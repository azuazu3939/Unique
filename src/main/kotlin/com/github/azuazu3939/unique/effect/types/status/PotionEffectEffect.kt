package com.github.azuazu3939.unique.effect.types.status

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * ポーション効果
 *
 * duration, amplifierパラメータはCEL式をサポート
 * 例: duration="200", duration="world.isNight ? 600 : 300"
 *     amplifier="1", amplifier="math.floor((1 - entity.health / entity.maxHealth) * 3)"
 */
class PotionEffectEffect(
    id: String = "potion_effect",
    private val effectType: PotionEffectType,
    private val duration: String,  // tick単位のCEL式
    private val amplifier: String = "0",
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val durationTicks = evaluateDuration(source, target)
        val amplifierLevel = evaluateAmplifier(source, target)

        val potionEffect = PotionEffect(
            effectType,
            durationTicks,
            amplifierLevel
        )

        target.addPotionEffect(potionEffect)

        DebugLogger.effect("PotionEffect(${effectType.key}, amp=$amplifierLevel, dur=$durationTicks)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val durationTicks = evaluateDurationFromPacket(source, target)
        val amplifierLevel = evaluateAmplifierFromPacket(source, target)

        val potionEffect = PotionEffect(
            effectType,
            durationTicks,
            amplifierLevel
        )

        target.addPotionEffect(potionEffect)

        DebugLogger.effect("PotionEffect(${effectType.key}, amp=$amplifierLevel, dur=$durationTicks) from PacketEntity", target.name)
    }

    private fun evaluateDuration(source: Entity, target: Entity): Int {
        return try {
            duration.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(duration, context).toInt()
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate potion duration: $duration", e)
            200  // デフォルト10秒
        }
    }

    private fun evaluateDurationFromPacket(source: PacketEntity, target: Entity): Int {
        return try {
            duration.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(duration, context).toInt()
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate potion duration: $duration", e)
            200
        }
    }

    private fun evaluateAmplifier(source: Entity, target: Entity): Int {
        return try {
            amplifier.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(amplifier, context).toInt()
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate potion amplifier: $amplifier", e)
            0
        }
    }

    private fun evaluateAmplifierFromPacket(source: PacketEntity, target: Entity): Int {
        return try {
            amplifier.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(amplifier, context).toInt()
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate potion amplifier: $amplifier", e)
            0
        }
    }

    override fun getDescription(): String {
        return "Potion Effect: ${effectType.key} (dur=$duration, amp=$amplifier)"
    }
}
