package com.github.azuazu3939.unique.effect

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.maxHealth
import com.github.azuazu3939.unique.util.name
import com.github.azuazu3939.unique.util.soundName
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.time.Duration

/**
 * エフェクト基底クラス
 *
 * エンティティに対する効果を適用
 */
abstract class Effect(
    val id: String,
    val sync: Boolean = false
) {

    /**
     * エフェクトを適用
     *
     * @param source ソースエンティティ
     * @param target ターゲットエンティティ
     */
    abstract suspend fun apply(source: Entity, target: Entity)

    /**
     * エフェクトを適用（PacketEntityソース）
     *
     * @param source ソースPacketEntity
     * @param target ターゲットエンティティ
     */
    abstract suspend fun apply(source: PacketEntity, target: Entity)

    /**
     * エフェクトの説明
     */
    open fun getDescription(): String {
        return "Effect: $id"
    }

    /**
     * デバッグ情報
     */
    open fun debugInfo(): String {
        return "Effect[id=$id, sync=$sync]"
    }
}

/**
 * ダメージエフェクト
 *
 * amountパラメータはCEL式をサポート
 * 例: "10", "target.health * 0.3", "20 * (1 - distance.horizontal(source.location, target.location) / 30)"
 */
class DamageEffect(
    id: String = "damage",
    private val amount: String,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val damageValue = evaluateDamage(source, target)
        target.damage(damageValue, source)
        DebugLogger.effect("Damage($damageValue)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val damageValue = evaluateDamageFromPacket(source, target)
        target.damage(damageValue)
        DebugLogger.effect("Damage($damageValue) from PacketEntity", target.name)
    }

    private fun evaluateDamage(source: Entity, target: Entity): Double {
        return try {
            // 固定値ならそのまま返す
            amount.toDoubleOrNull() ?: run {
                // CEL式として評価
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
                evaluator.evaluateNumber(amount, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage amount: $amount", e)
            0.0
        }
    }

    private fun evaluateDamageFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            amount.toDoubleOrNull() ?: run {
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
                evaluator.evaluateNumber(amount, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage amount: $amount", e)
            0.0
        }
    }

    override fun getDescription(): String = "Damage: $amount"
}

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
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
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
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
                evaluator.evaluateNumber(amount, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate heal amount: $amount", e)
            0.0
        }
    }

    override fun getDescription(): String = "Heal: $amount"
}

/**
 * ノックバックエフェクト
 */
class KnockbackEffect(
    id: String = "knockback",
    private val strength: Double = 1.0,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val direction = target.location.toVector()
            .subtract(source.location.toVector())
            .normalize()
            .multiply(strength)

        target.velocity = target.velocity.add(direction)

        DebugLogger.effect("Knockback($strength)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val direction = target.location.toVector()
            .subtract(source.location.toVector())
            .normalize()
            .multiply(strength)

        target.velocity = target.velocity.add(direction)

        DebugLogger.effect("Knockback($strength) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Knockback: $strength"
}

/**
 * ポーション効果
 *
 * duration, amplifierパラメータはCEL式をサポート
 * 例: duration="200", duration="world.isNight ? 600 : 300"
 *     amplifier="1", amplifier="math.floor((1 - entity.health / entity.maxHealth) * 3)"
 */
class PotionEffectEffect(
    id: String = "potion_effect",
    private val effectType: org.bukkit.potion.PotionEffectType,
    private val duration: String,  // tick単位のCEL式
    private val amplifier: String = "0",
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val durationTicks = evaluateDuration(source, target)
        val amplifierLevel = evaluateAmplifier(source, target)

        val potionEffect = org.bukkit.potion.PotionEffect(
            effectType,
            durationTicks,
            amplifierLevel
        )

        target.addPotionEffect(potionEffect)

        DebugLogger.effect("PotionEffect(${effectType.name()}, amp=$amplifierLevel, dur=$durationTicks)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val durationTicks = evaluateDurationFromPacket(source, target)
        val amplifierLevel = evaluateAmplifierFromPacket(source, target)

        val potionEffect = org.bukkit.potion.PotionEffect(
            effectType,
            durationTicks,
            amplifierLevel
        )

        target.addPotionEffect(potionEffect)

        DebugLogger.effect("PotionEffect(${effectType.name()}, amp=$amplifierLevel, dur=$durationTicks) from PacketEntity", target.name)
    }

    private fun evaluateDuration(source: Entity, target: Entity): Int {
        return try {
            duration.toIntOrNull() ?: run {
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
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
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
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
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
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
                val context = com.github.azuazu3939.unique.cel.CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = com.github.azuazu3939.unique.Unique.instance.celEvaluator
                evaluator.evaluateNumber(amplifier, context).toInt()
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate potion amplifier: $amplifier", e)
            0
        }
    }

    override fun getDescription(): String {
        return "Potion Effect: ${effectType.name()} (dur=$duration, amp=$amplifier)"
    }
}

/**
 * テレポートエフェクト
 */
class TeleportEffect(
    id: String = "teleport",
    private val location: org.bukkit.Location,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        target.teleport(location)
        DebugLogger.effect("Teleport", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        target.teleport(location)
        DebugLogger.effect("Teleport from PacketEntity", target.name)
    }

    override fun getDescription(): String {
        return "Teleport to (${location.blockX}, ${location.blockY}, ${location.blockZ})"
    }
}

/**
 * パーティクルエフェクト
 */
class ParticleEffect(
    id: String = "particle",
    private val particle: org.bukkit.Particle,
    private val count: Int = 10,
    private val offsetX: Double = 0.5,
    private val offsetY: Double = 0.5,
    private val offsetZ: Double = 0.5,
    sync: Boolean = false
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val location = target.location
        location.world?.spawnParticle(
            particle,
            location,
            count,
            offsetX,
            offsetY,
            offsetZ
        )

        DebugLogger.effect("Particle(${particle.name})", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val location = target.location
        location.world?.spawnParticle(
            particle,
            location,
            count,
            offsetX,
            offsetY,
            offsetZ
        )

        DebugLogger.effect("Particle(${particle.name}) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Particle: ${particle.name} x$count"
}

/**
 * サウンドエフェクト
 */
class SoundEffect(
    id: String = "sound",
    private val sound: Sound,
    private val volume: Float = 1.0f,
    private val pitch: Float = 1.0f,
    sync: Boolean = false
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val location = target.location
        location.world?.playSound(location, sound, volume, pitch)

        DebugLogger.effect("Sound(${sound.soundName()})", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val location = target.location
        location.world?.playSound(location, sound, volume, pitch)

        DebugLogger.effect("Sound(${sound.soundName()}) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Sound: ${sound.soundName()}"
}

/**
 * メッセージエフェクト
 */
class MessageEffect(
    id: String = "message",
    private val message: String,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target is Player) {
            target.sendMessage(message)
            DebugLogger.effect("Message", target.name)
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target is Player) {
            target.sendMessage(message)
            DebugLogger.effect("Message from PacketEntity", target.name)
        }
    }

    override fun getDescription(): String = "Message: $message"
}

/**
 * コマンド実行エフェクト
 */
class CommandEffect(
    id: String = "command",
    private val command: String,
    private val asOp: Boolean = false,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target is Player) {
            val wasOp = target.isOp

            if (asOp && !wasOp) {
                target.isOp = true
            }

            target.performCommand(command)

            if (asOp && !wasOp) {
                target.isOp = false
            }

            DebugLogger.effect("Command($command)", target.name)
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target is Player) {
            val wasOp = target.isOp

            if (asOp && !wasOp) {
                target.isOp = true
            }

            target.performCommand(command)

            if (asOp && !wasOp) {
                target.isOp = false
            }

            DebugLogger.effect("Command($command) from PacketEntity", target.name)
        }
    }

    override fun getDescription(): String {
        return if (asOp) "Command (as OP): $command" else "Command: $command"
    }
}

/**
 * 発火エフェクト
 */
class IgniteEffect(
    id: String = "ignite",
    private val duration: Duration,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        target.fireTicks = (duration.inWholeMilliseconds / 50).toInt()
        DebugLogger.effect("Ignite(${duration.inWholeSeconds}s)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        target.fireTicks = (duration.inWholeMilliseconds / 50).toInt()
        DebugLogger.effect("Ignite(${duration.inWholeSeconds}s) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Ignite for ${duration.inWholeSeconds}s"
}

/**
 * 速度変更エフェクト
 */
class VelocityEffect(
    id: String = "velocity",
    private val velocityX: Double,
    private val velocityY: Double,
    private val velocityZ: Double,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val velocity = org.bukkit.util.Vector(velocityX, velocityY, velocityZ)
        target.velocity = velocity

        DebugLogger.effect("Velocity($velocityX, $velocityY, $velocityZ)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val velocity = org.bukkit.util.Vector(velocityX, velocityY, velocityZ)
        target.velocity = velocity

        DebugLogger.effect("Velocity($velocityX, $velocityY, $velocityZ) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Set velocity to ($velocityX, $velocityY, $velocityZ)"
}