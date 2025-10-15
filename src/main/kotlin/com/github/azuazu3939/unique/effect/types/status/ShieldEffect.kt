package com.github.azuazu3939.unique.effect.types.status

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * ShieldEffect - 対象にシールド（吸収ハート）を付与
 *
 * 対象にABSORPTIONエフェクトを付与し、一時的なダメージ吸収を提供します。
 * バリアパーティクルで視覚的にシールドを表現します。
 *
 * パラメータ:
 * - amount: シールド量（ハート数）CEL式対応
 * - duration: シールド持続時間（tick）CEL式対応
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Shield
 *     amount: "10"   # 5ハート分のシールド
 *     duration: "200"  # 10秒間持続
 * ```
 */
class ShieldEffect(
    id: String = "shield",
    private val amount: String = "10",  // CEL式対応 (HP, ハート数x2)
    private val duration: String = "100",  // CEL式対応 (ticks, default 5秒)
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val amountValue = evaluateAmount(source, target)
        val durationValue = evaluateDuration(source, target)

        if (target is LivingEntity) {
            // ABSORPTIONエフェクトのamplifierは、追加されるハート数を決定
            // amplifier 0 = 1ハート追加、amplifier 1 = 1.5ハート追加...
            // amount (HP) を2で割ってハート数に変換し、さらにamplifierに変換
            val hearts = (amountValue / 2.0).toInt()
            val amplifier = kotlin.math.max(0, hearts - 1)

            // ABSORPTIONエフェクトを付与
            target.addPotionEffect(
                PotionEffect(
                    PotionEffectType.ABSORPTION,
                    durationValue,
                    amplifier.coerceIn(0, 255),
                    false,
                    true,
                    true
                )
            )

            // バリアパーティクルエフェクト
            val world = target.world
            val location = target.location.add(0.0, 1.0, 0.0)

            // バリアの壁を表現
            world.spawnParticle(
                Particle.BLOCK_MARKER,
                location,
                1,
                0.0, 0.0, 0.0,
                0.0,
                Material.BARRIER.createBlockData()
            )

            // 防御エンチャントのようなキラキラエフェクト
            world.spawnParticle(
                Particle.ENCHANT,
                location,
                40,
                0.5, 1.0, 0.5,
                0.5
            )

            // シールド付与サウンド
            world.playSound(
                location,
                Sound.ITEM_ARMOR_EQUIP_DIAMOND,
                SoundCategory.PLAYERS,
                1.0f,
                1.2f
            )

            DebugLogger.debug("Applied shield to ${target.name}: $amountValue HP for $durationValue ticks")
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val amountValue = evaluateAmountFromPacket(source, target)
        val durationValue = evaluateDurationFromPacket(source, target)

        if (target is LivingEntity) {
            val hearts = (amountValue / 2.0).toInt()
            val amplifier = kotlin.math.max(0, hearts - 1)

            target.addPotionEffect(
                PotionEffect(
                    PotionEffectType.ABSORPTION,
                    durationValue,
                    amplifier.coerceIn(0, 255),
                    false,
                    true,
                    true
                )
            )

            val world = target.world
            val location = target.location.add(0.0, 1.0, 0.0)

            world.spawnParticle(
                Particle.BLOCK_MARKER,
                location,
                1,
                0.0, 0.0, 0.0,
                0.0,
                Material.BARRIER.createBlockData()
            )

            world.spawnParticle(
                Particle.ENCHANT,
                location,
                40,
                0.5, 1.0, 0.5,
                0.5
            )

            world.playSound(
                location,
                Sound.ITEM_ARMOR_EQUIP_DIAMOND,
                SoundCategory.PLAYERS,
                1.0f,
                1.2f
            )

            DebugLogger.debug("Applied shield from PacketEntity to ${target.name}: $amountValue HP")
        }
    }

    private fun evaluateAmount(source: Entity, target: Entity): Double {
        return try {
            amount.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(amount, context).coerceIn(1.0, 2048.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate shield amount: $amount", e)
            10.0
        }
    }

    private fun evaluateAmountFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            amount.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(amount, context).coerceIn(1.0, 2048.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate shield amount from PacketEntity: $amount", e)
            10.0
        }
    }

    private fun evaluateDuration(source: Entity, target: Entity): Int {
        return try {
            duration.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(duration, context).toInt().coerceIn(1, 6000)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate shield duration: $duration", e)
            100
        }
    }

    private fun evaluateDurationFromPacket(source: PacketEntity, target: Entity): Int {
        return try {
            duration.toIntOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(duration, context).toInt().coerceIn(1, 6000)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate shield duration from PacketEntity: $duration", e)
            100
        }
    }

    override fun getDescription(): String = "Apply $amount HP shield for $duration ticks"
}
