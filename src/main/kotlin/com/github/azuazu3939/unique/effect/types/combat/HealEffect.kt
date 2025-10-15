package com.github.azuazu3939.unique.effect.types.combat

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.maxHealth
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * 回復エフェクト
 *
 * amountパラメータはCEL式をサポート
 * 例: "10", "target.maxHealth * 0.2", "entity.maxHealth - entity.health"
 */
class HealEffect(
    id: String = "heal",
    private val amount: String,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val healValue = evaluateHeal(source, target)
        val newHealth = (target.health + healValue).coerceAtMost(target.maxHealth())
        target.health = newHealth

        DebugLogger.effect("Heal($healValue)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val healValue = evaluateHealFromPacket(source, target)
        val newHealth = (target.health + healValue).coerceAtMost(target.maxHealth())
        target.health = newHealth

        DebugLogger.effect("Heal($healValue) from PacketEntity", target.name)
    }

    private fun evaluateHeal(source: Entity, target: Entity): Double {
        return try {
            amount.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(amount, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate heal amount: $amount", e)
            0.0
        }
    }

    private fun evaluateHealFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            amount.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(amount, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate heal amount: $amount", e)
            0.0
        }
    }

    override fun getDescription(): String = "Heal: $amount"
}
