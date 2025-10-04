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
 */
class DamageEffect(
    id: String = "damage",
    private val amount: Double,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        target.damage(amount, source)
        DebugLogger.effect("Damage($amount)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        target.damage(amount)
        DebugLogger.effect("Damage($amount) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Damage: $amount"
}

/**
 * 回復エフェクト
 */
class HealEffect(
    id: String = "heal",
    private val amount: Double,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val newHealth = (target.health + amount).coerceAtMost(target.maxHealth())
        target.health = newHealth

        DebugLogger.effect("Heal($amount)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val newHealth = (target.health + amount).coerceAtMost(target.maxHealth())
        target.health = newHealth

        DebugLogger.effect("Heal($amount) from PacketEntity", target.name)
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
 */
class PotionEffectEffect(
    id: String = "potion_effect",
    private val effectType: org.bukkit.potion.PotionEffectType,
    private val duration: Duration,
    private val amplifier: Int = 0,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val potionEffect = org.bukkit.potion.PotionEffect(
            effectType,
            (duration.inWholeMilliseconds / 50).toInt(), // ミリ秒→tick
            amplifier
        )

        target.addPotionEffect(potionEffect)

        DebugLogger.effect("PotionEffect(${effectType.name()})", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val potionEffect = org.bukkit.potion.PotionEffect(
            effectType,
            (duration.inWholeMilliseconds / 50).toInt(),
            amplifier
        )

        target.addPotionEffect(potionEffect)

        DebugLogger.effect("PotionEffect(${effectType.name()}) from PacketEntity", target.name)
    }

    override fun getDescription(): String {
        return "Potion Effect: ${effectType.name()} for ${duration.inWholeSeconds}s"
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