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
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector

/**
 * ThrowSkill - ターゲットを投げ飛ばすスキル
 *
 * ターゲットに速度を与えて投げ飛ばします。
 * MythicMobsのThrowメカニクスと同等の機能を提供します。
 *
 * 使用例:
 * ```yaml
 * skills:
 *   - skill: throw_attack
 *     type: THROW
 *     velocity: 2.0
 *     velocityY: 1.5
 * ```
 *
 * @param id スキルID
 * @param meta スキルメタ設定
 * @param condition 実行条件
 * @param velocity 水平方向の速度（CEL式対応）
 * @param velocityY 垂直方向の速度（CEL式対応）
 * @param mode 投げる方向のモード（UP, FORWARD, BACKWARD, AWAY）
 */
class ThrowSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val velocity: String = "1.0",
    private val velocityY: String = "1.0",
    private val mode: ThrowMode = ThrowMode.UP
) : Skill(id, meta, condition) {

    enum class ThrowMode {
        UP,           // 真上に投げる
        FORWARD,      // ソースの向いている方向に投げる
        BACKWARD,     // ソースの後方に投げる
        AWAY          // ソースから遠ざかる方向に投げる
    }

    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.debug("ThrowSkill condition not met")
            return
        }

        // 実行遅延
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.debug("ThrowSkill: No targets found")
            return
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildEntityContext(source)
        val velocityValue = evaluateCelDouble(velocity, context, evaluator, 1.0)
        val velocityYValue = evaluateCelDouble(velocityY, context, evaluator, 1.0)

        // 各ターゲットを投げる
        for (target in targets) {
            if (target !is LivingEntity) continue

            throwTarget(plugin, source, target, velocityValue, velocityYValue)
        }

        DebugLogger.debug("ThrowSkill executed: velocity=$velocityValue, velocityY=$velocityYValue, mode=$mode")
    }

    override suspend fun execute(plugin: Plugin, source: PacketEntity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.debug("ThrowSkill condition not met (PacketEntity)")
            return
        }

        // 実行遅延
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.debug("ThrowSkill: No targets found (PacketEntity)")
            return
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildPacketEntityContext(source)
        val velocityValue = evaluateCelDouble(velocity, context, evaluator, 1.0)
        val velocityYValue = evaluateCelDouble(velocityY, context, evaluator, 1.0)

        // 各ターゲットを投げる
        for (target in targets) {
            if (target !is LivingEntity) continue

            throwTargetFromPacket(plugin, source, target, velocityValue, velocityYValue)
        }

        DebugLogger.debug("ThrowSkill executed from PacketEntity: velocity=$velocityValue, velocityY=$velocityYValue, mode=$mode")
    }

    /**
     * ターゲットを投げる（Entity source）
     */
    private suspend fun throwTarget(
        plugin: Plugin,
        source: Entity,
        target: LivingEntity,
        velocityValue: Double,
        velocityYValue: Double
    ) {
        val throwVector = calculateThrowVector(source, target, velocityValue, velocityYValue)

        // 同期処理でvelocity設定
        withContext(plugin.regionDispatcher(target.location)) {
            target.velocity = throwVector
        }

        DebugLogger.debug("Threw ${target.name} with velocity $throwVector")
    }

    /**
     * ターゲットを投げる（PacketEntity source）
     */
    private suspend fun throwTargetFromPacket(
        plugin: Plugin,
        source: PacketEntity,
        target: LivingEntity,
        velocityValue: Double,
        velocityYValue: Double
    ) {
        val throwVector = calculateThrowVectorFromPacket(source, target, velocityValue, velocityYValue)

        // 同期処理でvelocity設定
        withContext(plugin.regionDispatcher(target.location)) {
            target.velocity = throwVector
        }

        DebugLogger.debug("Threw ${target.name} from PacketEntity with velocity $throwVector")
    }

    /**
     * 投げる方向ベクトルを計算（Entity）
     */
    private fun calculateThrowVector(
        source: Entity,
        target: Entity,
        velocityValue: Double,
        velocityYValue: Double
    ): Vector {
        return when (mode) {
            ThrowMode.UP -> {
                // 真上に投げる
                Vector(0.0, velocityYValue, 0.0)
            }
            ThrowMode.FORWARD -> {
                // ソースの向いている方向に投げる
                val direction = source.location.direction.normalize()
                direction.multiply(velocityValue).setY(velocityYValue)
            }
            ThrowMode.BACKWARD -> {
                // ソースの後方に投げる
                val direction = source.location.direction.normalize().multiply(-1.0)
                direction.multiply(velocityValue).setY(velocityYValue)
            }
            ThrowMode.AWAY -> {
                // ソースから遠ざかる方向に投げる
                val direction = target.location.toVector()
                    .subtract(source.location.toVector())
                    .normalize()
                direction.multiply(velocityValue).setY(velocityYValue)
            }
        }
    }

    /**
     * 投げる方向ベクトルを計算（PacketEntity）
     */
    private fun calculateThrowVectorFromPacket(
        source: PacketEntity,
        target: Entity,
        velocityValue: Double,
        velocityYValue: Double
    ): Vector {
        return when (mode) {
            ThrowMode.UP -> {
                // 真上に投げる
                Vector(0.0, velocityYValue, 0.0)
            }
            ThrowMode.FORWARD -> {
                // ソースの向いている方向に投げる
                val direction = source.location.direction.normalize()
                direction.multiply(velocityValue).setY(velocityYValue)
            }
            ThrowMode.BACKWARD -> {
                // ソースの後方に投げる
                val direction = source.location.direction.normalize().multiply(-1.0)
                direction.multiply(velocityValue).setY(velocityYValue)
            }
            ThrowMode.AWAY -> {
                // ソースから遠ざかる方向に投げる
                val direction = target.location.toVector()
                    .subtract(source.location.toVector())
                    .normalize()
                direction.multiply(velocityValue).setY(velocityYValue)
            }
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

    override fun getDescription(): String = "Throw skill with velocity $velocity, velocityY $velocityY"

    override fun debugInfo(): String {
        return "ThrowSkill[id=$id, velocity=$velocity, velocityY=$velocityY, mode=$mode]"
    }
}
