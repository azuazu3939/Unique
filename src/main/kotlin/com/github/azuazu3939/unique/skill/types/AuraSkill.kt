package com.github.azuazu3939.unique.skill.types

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.skill.Skill
import com.github.azuazu3939.unique.skill.SkillMeta
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.Plugin
import kotlin.math.cos
import kotlin.math.sin

/**
 * AuraSkill - オーラスキル
 *
 * ソースの周囲に持続的なオーラを生成し、範囲内のエンティティに継続的にエフェクトを適用します。
 * ヒーリングオーラ、ダメージオーラ、バフオーラなど様々な用途に使用できます。
 *
 * 使用例:
 * ```yaml
 * skills:
 *   - skill: healing_aura
 *     type: AURA
 *     radius: "5.0"
 *     duration: "10000"  # 10秒間持続
 *     tickInterval: "1000"  # 1秒ごとに効果適用
 *     particle: HEART
 *     particleCount: 5
 *     effects:
 *       - type: HEAL
 *         healAmount: 2.0
 *       - type: POTION
 *         potionType: REGENERATION
 *         potionDuration: "2000"
 *         potionAmplifier: 0
 * ```
 *
 * @param id スキルID
 * @param meta スキルメタ設定
 * @param condition 実行条件
 * @param radius オーラの半径（CEL式対応）
 * @param duration オーラの持続時間（ms）
 * @param tickInterval エフェクト適用間隔（ms）
 * @param particle オーラのパーティクル
 * @param particleCount パーティクルの数
 * @param particleSpeed パーティクルの速度
 * @param effects オーラ内のエンティティに適用するエフェクト
 * @param selfAffect ソース自身にも効果を適用するか
 * @param maxTargets 一度に影響を与える最大エンティティ数（0=無制限）
 * @param startSound オーラ開始時のサウンド
 * @param tickSound オーラ効果適用時のサウンド
 * @param endSound オーラ終了時のサウンド
 */
class AuraSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val radius: String = "5.0",
    private val duration: Long = 10000,  // オーラ持続時間（ms）
    private val tickInterval: Long = 1000,  // エフェクト適用間隔（ms）
    private val particle: Particle = Particle.ENCHANT,
    private val particleCount: Int = 10,
    private val particleSpeed: Double = 0.1,
    private val effects: List<Effect> = emptyList(),
    private val selfAffect: Boolean = false,
    private val maxTargets: Int = 0,  // 0 = 無制限
    private val startSound: Sound? = Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
    private val tickSound: Sound? = null,
    private val endSound: Sound? = Sound.ENTITY_EXPERIENCE_ORB_PICKUP
) : Skill(id, meta, condition) {

    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.debug("AuraSkill condition not met")
            return
        }

        // 実行遅延
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildEntityContext(source)
        val radiusValue = evaluateCelDouble(radius, context, evaluator, 5.0)

        // 開始音（同期処理）
        startSound?.let { sound ->
            withContext(Unique.instance.regionDispatcher(source.location)) {
                source.world.playSound(source.location, sound, 1.0f, 1.0f)
            }
        }

        // オーラを実行
        executeAura(plugin, source, radiusValue)

        DebugLogger.debug("AuraSkill executed: radius=$radiusValue, duration=${duration}ms")
    }

    override suspend fun execute(plugin: Plugin, source: PacketEntity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.debug("AuraSkill condition not met")
            return
        }

        // 実行遅延
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildPacketEntityContext(source)
        val radiusValue = evaluateCelDouble(radius, context, evaluator, 5.0)

        // 開始音（同期処理）
        startSound?.let { sound ->
            withContext(Unique.instance.regionDispatcher(source.location)) {
                source.location.world?.playSound(source.location, sound, 1.0f, 1.0f)
            }
        }

        // オーラを実行（PacketEntity版）
        executeAuraFromPacketEntity(plugin, source, radiusValue)

        DebugLogger.debug("AuraSkill executed from PacketEntity: radius=$radiusValue, duration=${duration}ms")
    }

    /**
     * オーラ実行（Entity source）
     */
    private suspend fun executeAura(
        plugin: Plugin,
        source: Entity,
        radiusValue: Double
    ) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + duration
        var lastTickTime = startTime
        val affectedEntities = mutableSetOf<Entity>()

        while (System.currentTimeMillis() < endTime) {
            // 死亡チェック
            if (meta.cancelOnDeath && source.isDead) {
                DebugLogger.debug("AuraSkill cancelled (source died)")
                break
            }

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTickTime >= tickInterval) {
                lastTickTime = currentTime

                // パーティクル表示
                displayAuraParticles(source.location, radiusValue)

                // 範囲内のエンティティを取得
                val nearbyEntities = source.location.world?.getNearbyEntitiesAsync(
                    source.location,
                    radiusValue,
                    radiusValue,
                    radiusValue
                )?.filter { it is LivingEntity && (selfAffect || it != source) } ?: emptyList()

                // maxTargets制限を適用
                val targets = if (maxTargets > 0) {
                    nearbyEntities.take(maxTargets)
                } else {
                    nearbyEntities
                }

                // エフェクトを適用
                for (entity in targets) {
                    affectedEntities.add(entity)

                    for (effect in effects) {
                        if (meta.effectDelay.inWholeMilliseconds > 0) {
                            delay(meta.effectDelay.inWholeMilliseconds)
                        }
                        effect.apply(source, entity)
                    }
                }

                // Tick音（同期処理）
                if (targets.isNotEmpty()) {
                    tickSound?.let { sound ->
                        withContext(Unique.instance.regionDispatcher(source.location)) {
                            source.world.playSound(source.location, sound, 0.5f, 1.0f)
                        }
                    }
                }

                DebugLogger.debug("AuraSkill tick: affected ${targets.size} entities")
            }

            // 短い待機
            delay(50)
        }

        // 終了音（同期処理）
        endSound?.let { sound ->
            withContext(Unique.instance.regionDispatcher(source.location)) {
                source.world.playSound(source.location, sound, 1.0f, 1.0f)
            }
        }

        DebugLogger.debug("AuraSkill completed: total affected ${affectedEntities.size} entities")
    }

    /**
     * オーラ実行（PacketEntity source）
     */
    private suspend fun executeAuraFromPacketEntity(
        plugin: Plugin,
        source: PacketEntity,
        radiusValue: Double
    ) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + duration
        var lastTickTime = startTime
        val affectedEntities = mutableSetOf<Entity>()

        while (System.currentTimeMillis() < endTime) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTickTime >= tickInterval) {
                lastTickTime = currentTime

                // パーティクル表示
                displayAuraParticles(source.location, radiusValue)

                // 範囲内のエンティティを取得
                val nearbyEntities = source.location.world?.getNearbyEntitiesAsync(
                    source.location,
                    radiusValue,
                    radiusValue,
                    radiusValue
                )?.filter { it is LivingEntity } ?: emptyList()

                // maxTargets制限を適用
                val targets = if (maxTargets > 0) {
                    nearbyEntities.take(maxTargets)
                } else {
                    nearbyEntities
                }

                // エフェクトを適用
                for (entity in targets) {
                    affectedEntities.add(entity)

                    for (effect in effects) {
                        if (meta.effectDelay.inWholeMilliseconds > 0) {
                            delay(meta.effectDelay.inWholeMilliseconds)
                        }
                        effect.apply(source, entity)
                    }
                }

                // Tick音（同期処理）
                if (targets.isNotEmpty()) {
                    tickSound?.let { sound ->
                        withContext(Unique.instance.regionDispatcher(source.location)) {
                            source.location.world?.playSound(source.location, sound, 0.5f, 1.0f)
                        }
                    }
                }

                DebugLogger.debug("AuraSkill tick: affected ${targets.size} entities")
            }

            delay(50)
        }

        // 終了音（同期処理）
        endSound?.let { sound ->
            withContext(Unique.instance.regionDispatcher(source.location)) {
                source.location.world?.playSound(source.location, sound, 1.0f, 1.0f)
            }
        }

        DebugLogger.debug("AuraSkill completed: total affected ${affectedEntities.size} entities")
    }

    /**
     * オーラのパーティクルを表示（同期処理）
     */
    private suspend fun displayAuraParticles(center: Location, radiusValue: Double) {
        val world = center.world ?: return

        withContext(Unique.instance.regionDispatcher(center)) {
            // 円形にパーティクルを配置
            val angleStep = 360.0 / particleCount
            for (i in 0 until particleCount) {
                val angle = Math.toRadians(angleStep * i)
                val x = center.x + radiusValue * cos(angle)
                val z = center.z + radiusValue * sin(angle)
                val y = center.y + (Math.random() * 2.0 - 1.0)  // ランダムなY offset

                val particleLoc = Location(world, x, y, z)
                world.spawnParticle(
                    particle,
                    particleLoc,
                    1,
                    0.1, 0.1, 0.1,
                    particleSpeed
                )
            }

            // 中心にもパーティクル
            world.spawnParticle(
                particle,
                center.clone().add(0.0, 1.0, 0.0),
                3,
                0.3, 0.5, 0.3,
                particleSpeed
            )
        }
    }

    /**
     * CEL式をDouble値として評価
     */
    private fun evaluateCelDouble(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Double
    ): Double {
        return try {
            expression.toDoubleOrNull() ?: evaluator.evaluateNumber(expression, context)
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    override fun getDescription(): String = "Aura skill with radius $radius for ${duration}ms"

    override fun debugInfo(): String {
        return "AuraSkill[id=$id, radius=$radius, duration=${duration}ms, tickInterval=${tickInterval}ms, effects=${effects.size}]"
    }
}
