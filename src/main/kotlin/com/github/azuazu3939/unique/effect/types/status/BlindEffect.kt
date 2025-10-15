package com.github.azuazu3939.unique.effect.types.status

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * BlindEffect - ターゲットに盲目効果を付与
 *
 * 盲目ポーション効果を適用し、視界を制限します。
 * duration, amplifierはCEL式対応
 *
 * 使用例:
 * ```yaml
 * - type: BLIND
 *   duration: "5000"  # 5秒
 *   amplifier: 1
 * ```
 */
class BlindEffect(
    id: String = "blind",
    private val duration: String = "3000",  // CEL式対応 (ms)
    private val amplifier: Int = 0,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) {
            DebugLogger.debug("BlindEffect: target is not LivingEntity")
            return
        }

        val durationTicks = evaluateDuration(source, target)
        val actualAmplifier = amplifier.coerceIn(0, 255)

        // BLINDNESS効果を付与
        target.addPotionEffect(
            PotionEffect(
                PotionEffectType.BLINDNESS,
                durationTicks,
                actualAmplifier,
                false,  // ambient
                true,   // particles
                true    // icon
            )
        )

        // パーティクル効果
        target.world.spawnParticle(
            Particle.SMOKE,
            target.eyeLocation,
            20,
            0.3, 0.3, 0.3,
            0.05
        )

        // サウンド効果
        target.world.playSound(
            target.location,
            Sound.ENTITY_ENDER_DRAGON_FLAP,
            0.5f,
            0.5f
        )

        DebugLogger.effect("Blind(duration=${durationTicks}t, amp=$actualAmplifier)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) {
            DebugLogger.debug("BlindEffect: target is not LivingEntity")
            return
        }

        val durationTicks = evaluateDuration(source, target)
        val actualAmplifier = amplifier.coerceIn(0, 255)

        // BLINDNESS効果を付与
        target.addPotionEffect(
            PotionEffect(
                PotionEffectType.BLINDNESS,
                durationTicks,
                actualAmplifier,
                false,
                true,
                true
            )
        )

        // パーティクル効果
        target.world.spawnParticle(
            Particle.SMOKE,
            target.eyeLocation,
            20,
            0.3, 0.3, 0.3,
            0.05
        )

        // サウンド効果
        target.world.playSound(
            target.location,
            Sound.ENTITY_ENDER_DRAGON_FLAP,
            0.5f,
            0.5f
        )

        DebugLogger.effect("Blind(duration=${durationTicks}t, amp=$actualAmplifier) from PacketEntity", target.name)
    }

    /**
     * Duration評価（Entity source）
     */
    private fun evaluateDuration(source: Entity, target: Entity): Int {
        return try {
            val durationMs = duration.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(duration, context)
            }
            (durationMs / 50).toInt().coerceAtLeast(1)  // ms → ticks
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate blind duration: $duration", e)
            60  // デフォルト3秒
        }
    }

    /**
     * Duration評価（PacketEntity source）
     */
    private fun evaluateDuration(source: PacketEntity, target: Entity): Int {
        return try {
            val durationMs = duration.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(duration, context)
            }
            (durationMs / 50).toInt().coerceAtLeast(1)
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate blind duration: $duration", e)
            60
        }
    }

    override fun getDescription(): String = "Blind for ${duration}ms with amplifier $amplifier"
}
