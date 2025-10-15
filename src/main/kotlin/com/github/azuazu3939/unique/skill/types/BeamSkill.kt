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
import org.bukkit.util.Vector

/**
 * BeamSkill - ビームスキル
 *
 * ソースから直線状にレーザービームを発射し、経路上の全エンティティにダメージやエフェクトを与えます。
 * レイキャストを使用して正確な命中判定を行い、ビジュアルエフェクトを表示します。
 *
 * 使用例:
 * ```yaml
 * skills:
 *   - skill: laser_beam
 *     type: BEAM
 *     range: "20.0"
 *     width: "0.5"
 *     particle: FLAME
 *     particleDensity: 0.3
 *     duration: "1000"  # ビーム持続時間
 *     effects:
 *       - type: DAMAGE
 *         damageAmount: 5.0
 *       - type: PARTICLE
 *         particle: EXPLOSION
 * ```
 *
 * @param id スキルID
 * @param meta スキルメタ設定
 * @param condition 実行条件
 * @param range ビームの射程距離（CEL式対応）
 * @param width ビームの幅（命中判定半径、CEL式対応）
 * @param particle ビームのパーティクル
 * @param particleDensity パーティクルの密度（ブロックあたりのステップ数）
 * @param duration ビーム持続時間（ms）
 * @param tickInterval ビーム更新間隔（ms）
 * @param piercing 貫通するか（falseの場合は最初の命中で停止）
 * @param effects ビームに命中したエンティティに適用するエフェクト
 * @param hitSound 命中時のサウンド
 * @param fireSound 発射時のサウンド
 */
class BeamSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val range: String = "20.0",
    private val width: String = "0.5",
    private val particle: Particle = Particle.FLAME,
    private val particleDensity: Double = 0.3,  // ブロックあたりのステップ数
    private val duration: Long = 1000,  // ビーム持続時間（ms）
    private val tickInterval: Long = 50,  // 更新間隔（ms）
    private val piercing: Boolean = true,
    private val effects: List<Effect> = emptyList(),
    private val hitSound: Sound? = Sound.ENTITY_GENERIC_EXPLODE,
    private val fireSound: Sound? = Sound.BLOCK_BEACON_ACTIVATE
) : Skill(id, meta, condition) {

    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.debug("BeamSkill condition not met")
            return
        }

        // 実行遅延
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildEntityContext(source)
        val rangeValue = evaluateCelDouble(range, context, evaluator, 20.0)
        val widthValue = evaluateCelDouble(width, context, evaluator, 0.5)

        // 発射音（同期処理）
        fireSound?.let { sound ->
            withContext(Unique.instance.regionDispatcher(source.location)) {
                source.world.playSound(source.location, sound, 1.0f, 1.0f)
            }
        }

        // ビーム方向を決定
        val direction = if (source is LivingEntity) {
            source.eyeLocation.direction
        } else {
            source.location.direction
        }

        val startLoc = if (source is LivingEntity) {
            source.eyeLocation
        } else {
            source.location.add(0.0, 1.0, 0.0)
        }

        // ビームを発射
        fireBeam(plugin, source, startLoc, direction, rangeValue, widthValue)

        DebugLogger.debug("BeamSkill executed: range=$rangeValue, width=$widthValue")
    }

    override suspend fun execute(plugin: Plugin, source: PacketEntity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.debug("BeamSkill condition not met")
            return
        }

        // 実行遅延
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildPacketEntityContext(source)
        val rangeValue = evaluateCelDouble(range, context, evaluator, 20.0)
        val widthValue = evaluateCelDouble(width, context, evaluator, 0.5)

        // 発射音（同期処理）
        fireSound?.let { sound ->
            withContext(Unique.instance.regionDispatcher(source.location)) {
                source.location.world?.playSound(source.location, sound, 1.0f, 1.0f)
            }
        }

        // PacketEntityの向きを取得
        val direction = source.location.direction
        val startLoc = source.location.clone().add(0.0, 1.5, 0.0)

        // ビームを発射（PacketEntity版）
        fireBeamFromPacketEntity(plugin, source, startLoc, direction, rangeValue, widthValue)

        DebugLogger.debug("BeamSkill executed from PacketEntity: range=$rangeValue, width=$widthValue")
    }

    /**
     * ビーム発射（Entity source）
     */
    private suspend fun fireBeam(
        plugin: Plugin,
        source: Entity,
        startLoc: Location,
        direction: Vector,
        rangeValue: Double,
        widthValue: Double
    ) {
        val world = startLoc.world ?: return
        val hitEntities = mutableSetOf<Entity>()
        val ticks = (duration / tickInterval).toInt()

        // ビーム持続期間中、繰り返し処理
        for (tick in 0..ticks) {
            if (meta.cancelOnDeath && source.isDead) {
                DebugLogger.debug("BeamSkill cancelled (source died)")
                break
            }

            // レイキャストでビーム経路を計算
            val rayTrace = world.rayTrace(
                startLoc,
                direction,
                rangeValue,
                org.bukkit.FluidCollisionMode.NEVER,
                true,
                widthValue
            ) { it != source && it is LivingEntity }

            val endLoc = rayTrace?.hitPosition?.toLocation(world)
                ?: startLoc.clone().add(direction.clone().multiply(rangeValue))

            // パーティクルを表示
            displayBeamParticles(startLoc, endLoc)

            // ヒットしたエンティティを処理
            val currentHits = findEntitiesAlongBeam(startLoc, endLoc, widthValue)
                .filter { it != source && it is LivingEntity }

            for (entity in currentHits) {
                if (!hitEntities.contains(entity)) {
                    // 初めてヒットした場合
                    hitEntities.add(entity)

                    // エフェクトを適用
                    for (effect in effects) {
                        if (meta.effectDelay.inWholeMilliseconds > 0) {
                            delay(meta.effectDelay.inWholeMilliseconds)
                        }
                        effect.apply(source, entity)
                    }

                    // ヒット音（同期処理）
                    hitSound?.let { sound ->
                        withContext(Unique.instance.regionDispatcher(entity.location)) {
                            entity.world.playSound(entity.location, sound, 0.5f, 1.2f)
                        }
                    }

                    DebugLogger.debug("BeamSkill hit entity: ${entity.name}")
                }
            }

            // 貫通しない場合、最初のヒットで停止
            if (!piercing && hitEntities.isNotEmpty()) {
                break
            }

            // 次のtickまで待機
            if (tick < ticks) {
                delay(tickInterval)
            }
        }

        DebugLogger.debug("BeamSkill completed: hit ${hitEntities.size} entities")
    }

    /**
     * ビーム発射（PacketEntity source）
     */
    private suspend fun fireBeamFromPacketEntity(
        plugin: Plugin,
        source: PacketEntity,
        startLoc: Location,
        direction: Vector,
        rangeValue: Double,
        widthValue: Double
    ) {
        val world = startLoc.world ?: return
        val hitEntities = mutableSetOf<Entity>()
        val ticks = (duration / tickInterval).toInt()

        for (tick in 0..ticks) {
            // レイキャスト
            val rayTrace = world.rayTrace(
                startLoc,
                direction,
                rangeValue,
                org.bukkit.FluidCollisionMode.NEVER,
                true,
                widthValue
            ) { it is LivingEntity }

            val endLoc = rayTrace?.hitPosition?.toLocation(world)
                ?: startLoc.clone().add(direction.clone().multiply(rangeValue))

            // パーティクル表示
            displayBeamParticles(startLoc, endLoc)

            // ヒットエンティティ処理
            val currentHits = findEntitiesAlongBeam(startLoc, endLoc, widthValue)
                .filter { it is LivingEntity }

            for (entity in currentHits) {
                if (!hitEntities.contains(entity)) {
                    hitEntities.add(entity)

                    // エフェクト適用
                    for (effect in effects) {
                        if (meta.effectDelay.inWholeMilliseconds > 0) {
                            delay(meta.effectDelay.inWholeMilliseconds)
                        }
                        effect.apply(source, entity)
                    }

                    // ヒット音（同期処理）
                    hitSound?.let { sound ->
                        withContext(Unique.instance.regionDispatcher(entity.location)) {
                            entity.world.playSound(entity.location, sound, 0.5f, 1.2f)
                        }
                    }

                    DebugLogger.debug("BeamSkill hit entity: ${entity.name}")
                }
            }

            if (!piercing && hitEntities.isNotEmpty()) {
                break
            }

            if (tick < ticks) {
                delay(tickInterval)
            }
        }

        DebugLogger.debug("BeamSkill completed: hit ${hitEntities.size} entities")
    }

    /**
     * ビーム経路上のパーティクルを表示
     */
    private suspend fun displayBeamParticles(start: Location, end: Location) {
        val distance = start.distance(end)
        val steps = (distance / particleDensity).toInt().coerceAtLeast(1)
        val direction = end.toVector().subtract(start.toVector()).normalize()

        withContext(Unique.instance.regionDispatcher(start)) {
            for (i in 0..steps) {
                val progress = i.toDouble() / steps
                val particleLoc = start.clone().add(direction.clone().multiply(distance * progress))

                particleLoc.world?.spawnParticle(
                    particle,
                    particleLoc,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
                )
            }
        }
    }

    /**
     * ビーム経路上のエンティティを検索
     */
    private fun findEntitiesAlongBeam(start: Location, end: Location, width: Double): List<Entity> {
        val world = start.world ?: return emptyList()
        val direction = end.toVector().subtract(start.toVector())
        val distance = direction.length()
        direction.normalize()

        // ビーム経路上のすべてのエンティティを取得
        val nearbyEntities = world.getNearbyEntitiesAsync(
            start,
            distance + width,
            distance + width,
            distance + width
        )

        return nearbyEntities.filter { entity ->
            // エンティティとビーム経路の最短距離を計算
            val toEntity = entity.location.toVector().subtract(start.toVector())
            val projection = toEntity.dot(direction)

            // 投影が範囲内か確認
            if (projection !in 0.0..distance) {
                return@filter false
            }

            // 最短距離を計算
            val closestPoint = start.toVector().add(direction.clone().multiply(projection))
            val distanceToBeam = entity.location.toVector().distance(closestPoint)

            distanceToBeam <= width
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

    override fun getDescription(): String = "Beam skill with range $range"

    override fun debugInfo(): String {
        return "BeamSkill[id=$id, range=$range, width=$width, particle=$particle, piercing=$piercing]"
    }
}
