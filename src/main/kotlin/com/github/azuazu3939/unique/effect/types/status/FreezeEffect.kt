package com.github.azuazu3939.unique.effect.types.status

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * FreezeEffect - 対象を凍結させる
 *
 * 対象にSLOW/SLOW_DIGGINGエフェクトを付与し、氷のパーティクルを表示します。
 * 高いAmplifierで実質的な移動不可を実現できます。
 *
 * パラメータ:
 * - duration: 凍結時間（tick）CEL式対応
 * - amplifier: エフェクトレベル（0-255）CEL式対応
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Freeze
 *     duration: "100"  # 5秒間凍結
 *     amplifier: "3"   # レベル3のSlow
 * ```
 */
class FreezeEffect(
    id: String = "freeze",
    private val duration: String = "60",  // CEL式対応 (ticks, default 3秒)
    private val amplifier: String = "2",  // CEL式対応 (default レベル2)
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val durationValue = evaluateDuration(source, target)
        val amplifierValue = evaluateAmplifier(source, target)

        if (target is LivingEntity) {
            // SLOW（移動速度低下）を付与
            target.addPotionEffect(
                PotionEffect(
                    PotionEffectType.SLOWNESS,
                    durationValue,
                    amplifierValue,
                    false,
                    true,
                    true
                )
            )

            // SLOW_DIGGING（採掘速度低下）を追加で付与
            target.addPotionEffect(
                PotionEffect(
                    PotionEffectType.MINING_FATIGUE,
                    durationValue,
                    amplifierValue,
                    false,
                    true,
                    true
                )
            )

            // 氷のパーティクルエフェクト
            val world = target.world
            val location = target.location.add(0.0, 1.0, 0.0)

            // 氷の結晶エフェクト
            world.spawnParticle(
                Particle.SNOWFLAKE,
                location,
                30,
                0.5, 0.5, 0.5,
                0.01
            )

            // 冷気エフェクト
            world.spawnParticle(
                Particle.CLOUD,
                location,
                20,
                0.3, 0.5, 0.3,
                0.01
            )

            // 凍結サウンド
            world.playSound(
                location,
                Sound.BLOCK_GLASS_BREAK,
                SoundCategory.HOSTILE,
                0.5f,
                1.5f
            )

            DebugLogger.debug("Applied freeze to ${target.name} for $durationValue ticks (amplifier: $amplifierValue)")
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val durationValue = evaluateDurationFromPacket(source, target)
        val amplifierValue = evaluateAmplifierFromPacket(source, target)

        if (target is LivingEntity) {
            target.addPotionEffect(
                PotionEffect(
                    PotionEffectType.SLOWNESS,
                    durationValue,
                    amplifierValue,
                    false,
                    true,
                    true
                )
            )

            target.addPotionEffect(
                PotionEffect(
                    PotionEffectType.MINING_FATIGUE,
                    durationValue,
                    amplifierValue,
                    false,
                    true,
                    true
                )
            )

            val world = target.world
            val location = target.location.add(0.0, 1.0, 0.0)

            world.spawnParticle(
                Particle.SNOWFLAKE,
                location,
                30,
                0.5, 0.5, 0.5,
                0.01
            )

            world.spawnParticle(
                Particle.CLOUD,
                location,
                20,
                0.3, 0.5, 0.3,
                0.01
            )

            world.playSound(
                location,
                Sound.BLOCK_GLASS_BREAK,
                SoundCategory.HOSTILE,
                0.5f,
                1.5f
            )

            DebugLogger.debug("Applied freeze from PacketEntity to ${target.name} for $durationValue ticks")
        }
    }

    private fun evaluateDuration(source: Entity, target: Entity): Int {
        return try {
            duration.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(duration, context).toInt().coerceIn(1, 6000)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate freeze duration: $duration", e)
            60
        }
    }

    private fun evaluateDurationFromPacket(source: PacketEntity, target: Entity): Int {
        return try {
            duration.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(duration, context).toInt().coerceIn(1, 6000)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate freeze duration from PacketEntity: $duration", e)
            60
        }
    }

    private fun evaluateAmplifier(source: Entity, target: Entity): Int {
        return try {
            amplifier.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(amplifier, context).toInt().coerceIn(0, 255)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate freeze amplifier: $amplifier", e)
            2
        }
    }

    private fun evaluateAmplifierFromPacket(source: PacketEntity, target: Entity): Int {
        return try {
            amplifier.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(amplifier, context).toInt().coerceIn(0, 255)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate freeze amplifier from PacketEntity: $amplifier", e)
            2
        }
    }

    override fun getDescription(): String = "Freeze target for $duration ticks (amplifier: $amplifier)"
}
