package com.github.azuazu3939.unique.skill.types

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.skill.Skill
import com.github.azuazu3939.unique.skill.SkillMeta
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.plugin.Plugin
import kotlin.math.cos
import kotlin.math.sin

/**
 * サモンスキル
 *
 * エンティティやCustomMobを召喚するスキル
 *
 * @param id スキルID
 * @param meta スキルメタ設定
 * @param condition 実行条件
 * @param summonType 召喚タイプ（vanilla / custom）
 * @param entityType バニラエンティティタイプ
 * @param customMobId カスタムMob ID
 * @param amount 召喚数（CEL式対応）
 * @param spreadRange 散らばり範囲（CEL式対応）
 * @param duration 召喚されたエンティティの持続時間（null = 永続、CEL式対応）
 * @param inheritTarget 召喚者のターゲットを引き継ぐか
 */
class SummonSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val summonType: SummonType = SummonType.VANILLA,
    private val entityType: EntityType? = null,
    private val customMobId: String? = null,
    private val amount: String = "1",
    private val spreadRange: String = "2.0",
    private val duration: String? = null,
    private val inheritTarget: Boolean = false
) : Skill(id, meta, condition) {

    enum class SummonType {
        VANILLA,  // バニラのエンティティ
        CUSTOM    // カスタムMob
    }

    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.skill("SummonSkill condition not met")
            return
        }

        // 実行遅延
        delay(meta.executeDelay)

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.skill("SummonSkill: No targets found")
            return
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildEntityContext(source)
        val amountValue = evaluateCelInt(amount, context, evaluator, 1)
        val spreadRangeValue = evaluateCelDouble(spreadRange, context, evaluator, 2.0)
        val durationValue = duration?.let { evaluateCelLong(it, context, evaluator, null) }

        // 各ターゲット位置で召喚
        for (target in targets) {
            summonEntities(plugin, target.location, source, amountValue, spreadRangeValue, durationValue)
        }
    }

    override suspend fun execute(plugin: Plugin, source: PacketEntity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.skill("SummonSkill condition not met (PacketEntity)")
            return
        }

        // 実行遅延
        delay(meta.executeDelay)

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.skill("SummonSkill: No targets found (PacketEntity)")
            return
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildPacketEntityContext(source)
        val amountValue = evaluateCelInt(amount, context, evaluator, 1)
        val spreadRangeValue = evaluateCelDouble(spreadRange, context, evaluator, 2.0)
        val durationValue = duration?.let { evaluateCelLong(it, context, evaluator, null) }

        // 各ターゲット位置で召喚
        for (target in targets) {
            summonEntities(plugin, target.location, null, amountValue, spreadRangeValue, durationValue)
        }
    }

    /**
     * エンティティを召喚
     */
    private suspend fun summonEntities(
        plugin: Plugin,
        location: Location,
        source: Entity?,
        amountValue: Int,
        spreadRangeValue: Double,
        durationValue: Long?
    ) {
        val world = location.world ?: return

        repeat(amountValue) { index ->
            // 散らばり位置を計算
            val offset = if (spreadRangeValue > 0 && amountValue > 1) {
                val angle = (2 * Math.PI * index) / amountValue
                val distance = Math.random() * spreadRangeValue
                Location(
                    world,
                    location.x + cos(angle) * distance,
                    location.y,
                    location.z + sin(angle) * distance
                )
            } else {
                location.clone()
            }

            when (summonType) {
                SummonType.VANILLA -> {
                    if (entityType == null) {
                        DebugLogger.error("SummonSkill: entityType is null for VANILLA summon")
                        return
                    }
                    summonVanillaEntity(plugin, offset, entityType, source, durationValue)
                }
                SummonType.CUSTOM -> {
                    if (customMobId == null) {
                        DebugLogger.error("SummonSkill: customMobId is null for CUSTOM summon")
                        return
                    }
                    summonCustomMob(plugin, offset, customMobId)
                }
            }
        }

        DebugLogger.skill("SummonSkill: Summoned $amountValue entities at ${location.blockX}, ${location.blockY}, ${location.blockZ}")
    }

    /**
     * バニラエンティティを召喚
     */
    private suspend fun summonVanillaEntity(
        plugin: Plugin,
        location: Location,
        type: EntityType,
        source: Entity?,
        durationValue: Long?
    ) {
        val world = location.world ?: return

        // asyncスレッドで実行
        withContext(plugin.globalRegionDispatcher) {
            val entity = world.spawnEntity(location, type)
            configureEntity(plugin, entity, source, durationValue)
        }
    }

    /**
     * カスタムMobを召喚
     */
    private suspend fun summonCustomMob(
        plugin: Plugin,
        location: Location,
        mobId: String
    ) {
        val unique = plugin as? Unique ?: return

        // MobManagerを使用してカスタムMobをスポーン
        unique.mobManager.spawnMob(mobId, location)

        DebugLogger.skill("SummonSkill: Spawned custom mob '$mobId'")
    }

    /**
     * 召喚されたエンティティを設定
     */
    private suspend fun configureEntity(
        plugin: Plugin,
        entity: Entity,
        source: Entity?,
        durationValue: Long?
    ) {
        // ターゲット継承
        if (inheritTarget && source is LivingEntity && entity is Mob) {
            entity.target = source
        }

        // 持続時間の設定
        if (durationValue != null) {
            plugin.launch(plugin.globalRegionDispatcher) {
                delay(durationValue)
                if (entity.isValid && !entity.isDead) {
                    entity.remove()
                    DebugLogger.skill("SummonSkill: Removed entity after ${durationValue}ms")
                }
            }
        }
    }

    /**
     * CEL式を評価してInt値を取得
     */
    private fun evaluateCelInt(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Int
    ): Int {
        return try {
            expression.toIntOrNull() ?: run {
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toInt()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    /**
     * CEL式を評価してDouble値を取得
     */
    private fun evaluateCelDouble(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Double
    ): Double {
        return try {
            expression.toDoubleOrNull() ?: run {
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toDouble()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    /**
     * CEL式を評価してLong値を取得
     */
    private fun evaluateCelLong(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Long?
    ): Long? {
        return try {
            expression.toLongOrNull() ?: run {
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toLong()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    override fun getDescription(): String {
        return when (summonType) {
            SummonType.VANILLA -> "Summon $amount x $entityType"
            SummonType.CUSTOM -> "Summon $amount x Custom Mob '$customMobId'"
        }
    }

    override fun debugInfo(): String {
        return "SummonSkill[type=$summonType, entity=$entityType, custom=$customMobId, amount=$amount, spread=$spreadRange, duration=$duration]"
    }
}