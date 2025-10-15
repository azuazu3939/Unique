package com.github.azuazu3939.unique.effect.types.combat

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * 雷召喚エフェクト
 *
 * 指定位置に雷を召喚します。
 * ダメージあり/なし（ビジュアルのみ）を選択可能。
 *
 * CEL対応パラメータ:
 * - damage: ダメージ量
 */
class LightningEffect(
    id: String = "lightning",
    private val damage: String = "10",  // CEL式対応
    private val setFire: Boolean = true,
    private val visualOnly: Boolean = false,  // trueの場合、ダメージなし
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val location = target.location
        val world = location.world ?: return

        if (visualOnly) {
            // ビジュアルのみの雷
            world.strikeLightningEffect(location)
        } else {
            // 実際の雷（ダメージあり）
            world.strikeLightning(location)

            // CEL式でダメージ評価
            val damageValue = evaluateDamage(source, target)

            if (target is LivingEntity) {
                target.damage(damageValue)

                if (setFire) {
                    target.fireTicks = 100  // 5秒
                }
            }
        }

        DebugLogger.effect("Lightning(damage=$damage, visual=$visualOnly)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val location = target.location
        val world = location.world ?: return

        if (visualOnly) {
            world.strikeLightningEffect(location)
        } else {
            world.strikeLightning(location)

            val damageValue = evaluateDamageFromPacket(source, target)

            if (target is LivingEntity) {
                target.damage(damageValue)

                if (setFire) {
                    target.fireTicks = 100
                }
            }
        }

        DebugLogger.effect("Lightning(damage=$damage, visual=$visualOnly) from PacketEntity", target.name)
    }

    private fun evaluateDamage(source: Entity, target: Entity): Double {
        return try {
            damage.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(damage, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate lightning damage: $damage", e)
            10.0
        }
    }

    private fun evaluateDamageFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            damage.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(damage, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate lightning damage: $damage", e)
            10.0
        }
    }

    override fun getDescription(): String = "Summon lightning${if (visualOnly) " (visual only)" else " with $damage damage"}"
}
