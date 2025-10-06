package com.github.azuazu3939.unique.effect

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.*
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
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
     * エフェクトを適用（PacketEntity to PacketEntity）
     *
     * @param source ソースPacketEntity
     * @param target ターゲットPacketEntity
     */
    open suspend fun apply(source: PacketEntity, target: PacketEntity) {
        // デフォルト実装: PacketEntityに対するエフェクトは制限される
        DebugLogger.verbose("Effect $id: PacketEntity to PacketEntity not supported, skipping")
    }

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
 *
 * @param ignoreInvulnerability 無敵時間を無視するか（連続ヒット可能）
 * @param ignoreKnockbackResistance ノックバック耐性を無視するか（ノックバックを強制的に発生）
 * @param preventKnockback ノックバックを完全に封じるか（後処理で適用）
 */
class DamageEffect(
    id: String = "damage",
    private val amount: String,
    private val ignoreInvulnerability: Boolean = false,
    private val ignoreKnockbackResistance: Boolean = false,
    private val preventKnockback: Boolean = false,
    sync: Boolean = true
) : Effect(id, sync) {

    companion object {
        private val KNOCKBACK_RESISTANCE_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d")
    }

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val damageValue = evaluateDamage(source, target)

        // 無敵時間とノックバック耐性を一時的に操作
        applyDamageWithOptions(target, damageValue, source)

        DebugLogger.effect("Damage($damageValue, ignoreInv=$ignoreInvulnerability, ignoreKB=$ignoreKnockbackResistance, preventKB=$preventKnockback)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val damageValue = evaluateDamageFromPacket(source, target)

        // 無敵時間とノックバック耐性を一時的に操作
        applyDamageWithOptions(target, damageValue, null)

        DebugLogger.effect("Damage($damageValue, ignoreInv=$ignoreInvulnerability, ignoreKB=$ignoreKnockbackResistance, preventKB=$preventKnockback) from PacketEntity", target.name)
    }

    /**
     * 無敵時間とノックバック耐性を考慮してダメージを適用
     */
    private fun applyDamageWithOptions(target: LivingEntity, damage: Double, source: Entity?) {
        var originalNoDamageTicks: Int? = null
        var kbAttribute: org.bukkit.attribute.AttributeInstance? = null
        var originalKbValue: Double? = null
        var originalVelocity: org.bukkit.util.Vector? = null

        try {
            // 無敵時間を無視する場合
            if (ignoreInvulnerability) {
                originalNoDamageTicks = target.noDamageTicks
                target.noDamageTicks = 0
            }

            // ノックバック耐性を無視する場合（ノックバックを強制的に発生）
            if (ignoreKnockbackResistance) {
                kbAttribute = target.getAttribute(Attribute.KNOCKBACK_RESISTANCE)
                if (kbAttribute != null) {
                    originalKbValue = kbAttribute.baseValue
                    // 一時的にノックバック耐性を0に設定
                    kbAttribute.baseValue = 0.0
                }
            }

            // ノックバックを封じる場合は、ダメージ前のvelocityを保存
            if (preventKnockback) {
                originalVelocity = target.velocity.clone()
            }

            // ダメージを適用
            if (source != null) {
                target.damage(damage, source)
            } else {
                target.damage(damage)
            }

            // ノックバックを封じる場合は、元のvelocityを復元（後処理）
            if (preventKnockback && originalVelocity != null) {
                target.velocity = originalVelocity
            }

        } finally {
            // 元の値に戻す
            if (ignoreInvulnerability && originalNoDamageTicks != null) {
                target.noDamageTicks = originalNoDamageTicks
            }

            if (ignoreKnockbackResistance && kbAttribute != null && originalKbValue != null) {
                kbAttribute.baseValue = originalKbValue
            }
        }
    }

    private fun evaluateDamage(source: Entity, target: Entity): Double {
        return try {
            // 固定値ならそのまま返す
            amount.toDoubleOrNull() ?: run {
                // CEL式として評価
                val context = CELVariableProvider.buildTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
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
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
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

        DebugLogger.effect("PotionEffect(${effectType.name()}, amp=$amplifierLevel, dur=$durationTicks)", target.name)
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

        DebugLogger.effect("PotionEffect(${effectType.name()}, amp=$amplifierLevel, dur=$durationTicks) from PacketEntity", target.name)
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
        return "Potion Effect: ${effectType.name()} (dur=$duration, amp=$amplifier)"
    }
}

/**
 * テレポートエフェクト
 */
class TeleportToEffect(
    id: String = "teleport_to",
    private val location: Location,
    sync: Boolean = false
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        target.teleportAsync(location)
        DebugLogger.effect("TeleportTo", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        target.teleportAsync(location)
        DebugLogger.effect("TeleportTo PacketEntity", target.name)
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
    private val particle: Particle,
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
    sync: Boolean = false,
    category: SoundCategory
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

/**
 * 爆発エフェクト
 *
 * 指定位置で爆発を発生させます。
 * 範囲ダメージとノックバックを与えます。
 *
 * CEL対応パラメータ:
 * - damage: ダメージ量
 * - radius: 爆発半径
 * - knockback: ノックバック強度
 */
class ExplosionEffect(
    id: String = "explosion",
    private val damage: String = "20",  // CEL式対応
    private val radius: String = "3.0",  // CEL式対応
    private val knockback: String = "1.0",  // CEL式対応
    private val setFire: Boolean = false,
    private val breakBlocks: Boolean = false,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val location = target.location
        val world = location.world ?: return

        // CEL式を評価
        val damageValue = evaluateDamage(source, target)
        val radiusValue = evaluateRadius(source, target)
        val knockbackValue = evaluateKnockback(source, target)

        // 爆発エフェクト
        world.createExplosion(location, 0f, setFire, breakBlocks)

        // 範囲内のエンティティにダメージ
        world.getNearbyEntities(location, radiusValue, radiusValue, radiusValue)
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                // 距離減衰ダメージ
                val distance = entity.location.distance(location)
                val damageMultiplier = kotlin.math.max(0.0, 1.0 - distance / radiusValue)
                val finalDamage = damageValue * damageMultiplier

                entity.damage(finalDamage)

                // ノックバック
                val direction = entity.location.toVector().subtract(location.toVector()).normalize()
                entity.velocity = direction.multiply(knockbackValue * damageMultiplier)
            }

        DebugLogger.effect("Explosion(damage=$damageValue, radius=$radiusValue)", location.toString())
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val location = target.location
        val world = location.world ?: return

        val damageValue = evaluateDamageFromPacket(source, target)
        val radiusValue = evaluateRadiusFromPacket(source, target)
        val knockbackValue = evaluateKnockbackFromPacket(source, target)

        world.createExplosion(location, 0f, setFire, breakBlocks)

        world.getNearbyEntities(location, radiusValue, radiusValue, radiusValue)
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                val distance = entity.location.distance(location)
                val damageMultiplier = kotlin.math.max(0.0, 1.0 - distance / radiusValue)
                val finalDamage = damageValue * damageMultiplier

                entity.damage(finalDamage)

                val direction = entity.location.toVector().subtract(location.toVector()).normalize()
                entity.velocity = direction.multiply(knockbackValue * damageMultiplier)
            }

        DebugLogger.effect("Explosion(damage=$damageValue, radius=$radiusValue) from PacketEntity", location.toString())
    }

    private fun evaluateDamage(source: Entity, target: Entity): Double {
        return try {
            damage.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(damage, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion damage: $damage", e)
            20.0
        }
    }

    private fun evaluateRadius(source: Entity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion radius: $radius", e)
            3.0
        }
    }

    private fun evaluateKnockback(source: Entity, target: Entity): Double {
        return try {
            knockback.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(knockback, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion knockback: $knockback", e)
            1.0
        }
    }

    private fun evaluateDamageFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            damage.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(damage, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion damage: $damage", e)
            20.0
        }
    }

    private fun evaluateRadiusFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion radius: $radius", e)
            3.0
        }
    }

    private fun evaluateKnockbackFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            knockback.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(knockback, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion knockback: $knockback", e)
            1.0
        }
    }

    override fun getDescription(): String = "Explode with $damage damage (radius: $radius)"
}

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

/**
 * TeleportEffect - ターゲットをテレポート
 *
 * ターゲットを指定位置またはソースの位置にテレポートします。
 * 相対座標やランダム範囲もサポート。
 *
 * パラメータ:
 * - mode: テレポートモード（TO_SOURCE, TO_TARGET, TO_LOCATION, RANDOM）
 * - range: ランダム範囲（RANDOM mode用、CEL式対応）
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Teleport
 *     mode: TO_SOURCE  # ソースの位置にテレポート
 * ```
 */
class TeleportEffect(
    id: String = "teleport",
    private val mode: TeleportMode = TeleportMode.TO_SOURCE,
    private val range: String = "5.0",  // RANDOM用
    sync: Boolean = true
) : Effect(id, sync) {

    enum class TeleportMode {
        TO_SOURCE,    // ソースの位置へ
        TO_TARGET,    // ターゲットの位置へ（既にいる場所）
        RANDOM        // ランダムな位置へ
    }

    override suspend fun apply(source: Entity, target: Entity) {
        val destination = when (mode) {
            TeleportMode.TO_SOURCE -> source.location
            TeleportMode.TO_TARGET -> target.location
            TeleportMode.RANDOM -> {
                val rangeValue = evaluateRange(source, target)
                val randomOffset = org.bukkit.util.Vector(
                    (Math.random() - 0.5) * rangeValue * 2,
                    Math.random() * rangeValue,
                    (Math.random() - 0.5) * rangeValue * 2
                )
                target.location.clone().add(randomOffset)
            }
        }

        target.teleport(destination)

        // テレポートエフェクト
        target.world.spawnParticle(
            Particle.PORTAL,
            target.location,
            50,
            0.5, 1.0, 0.5,
            0.5
        )
        target.world.playSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)

        DebugLogger.effect("Teleport(mode=$mode)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val destination = when (mode) {
            TeleportMode.TO_SOURCE -> source.location
            TeleportMode.TO_TARGET -> target.location
            TeleportMode.RANDOM -> {
                val rangeValue = evaluateRangeFromPacket(source, target)
                val randomOffset = org.bukkit.util.Vector(
                    (Math.random() - 0.5) * rangeValue * 2,
                    Math.random() * rangeValue,
                    (Math.random() - 0.5) * rangeValue * 2
                )
                target.location.clone().add(randomOffset)
            }
        }

        target.teleport(destination)

        target.world.spawnParticle(
            Particle.PORTAL,
            target.location,
            50,
            0.5, 1.0, 0.5,
            0.5
        )
        target.world.playSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)

        DebugLogger.effect("Teleport(mode=$mode) from PacketEntity", target.name)
    }

    private fun evaluateRange(source: Entity, target: Entity): Double {
        return try {
            range.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(range, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate teleport range: $range", e)
            5.0
        }
    }

    private fun evaluateRangeFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            range.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(range, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate teleport range: $range", e)
            5.0
        }
    }

    override fun getDescription(): String = "Teleport (mode: $mode)"
}

/**
 * PullEffect - ターゲットを引き寄せる
 *
 * ターゲットをソースの方向に引き寄せます。
 * 速度とノックバック抵抗を考慮します。
 *
 * パラメータ:
 * - strength: 引き寄せ強度（CEL式対応）
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Pull
 *     strength: "2.0"  # 強度2.0で引き寄せ
 * ```
 */
class PullEffect(
    id: String = "pull",
    private val strength: String = "1.0",
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val strengthValue = evaluateStrength(source, target)

        // ソースへの方向ベクトルを計算
        val direction = source.location.toVector().subtract(target.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        // 視覚効果
        target.world.spawnParticle(
            Particle.SWEEP_ATTACK,
            target.location.add(0.0, 1.0, 0.0),
            3,
            0.3, 0.3, 0.3,
            0.0
        )

        DebugLogger.effect("Pull(strength=$strengthValue)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val strengthValue = evaluateStrengthFromPacket(source, target)

        val direction = source.location.toVector().subtract(target.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        target.world.spawnParticle(
            Particle.SWEEP_ATTACK,
            target.location.add(0.0, 1.0, 0.0),
            3,
            0.3, 0.3, 0.3,
            0.0
        )

        DebugLogger.effect("Pull(strength=$strengthValue) from PacketEntity", target.name)
    }

    private fun evaluateStrength(source: Entity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate pull strength: $strength", e)
            1.0
        }
    }

    private fun evaluateStrengthFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate pull strength: $strength", e)
            1.0
        }
    }

    override fun getDescription(): String = "Pull with strength $strength"
}

/**
 * PushEffect - ターゲットを押し出す
 *
 * ターゲットをソースから遠ざける方向に押し出します。
 *
 * パラメータ:
 * - strength: 押し出し強度（CEL式対応）
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Push
 *     strength: "3.0"  # 強度3.0で押し出す
 * ```
 */
class PushEffect(
    id: String = "push",
    private val strength: String = "1.5",
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val strengthValue = evaluateStrength(source, target)

        // ソースから離れる方向ベクトルを計算
        val direction = target.location.toVector().subtract(source.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        // 視覚効果
        target.world.spawnParticle(
            Particle.EXPLOSION,
            target.location.add(0.0, 0.5, 0.0),
            5,
            0.2, 0.2, 0.2,
            0.0
        )

        DebugLogger.effect("Push(strength=$strengthValue)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val strengthValue = evaluateStrengthFromPacket(source, target)

        val direction = target.location.toVector().subtract(source.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        target.world.spawnParticle(
            Particle.EXPLOSION,
            target.location.add(0.0, 0.5, 0.0),
            5,
            0.2, 0.2, 0.2,
            0.0
        )

        DebugLogger.effect("Push(strength=$strengthValue) from PacketEntity", target.name)
    }

    private fun evaluateStrength(source: Entity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate push strength: $strength", e)
            1.5
        }
    }

    private fun evaluateStrengthFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate push strength: $strength", e)
            1.5
        }
    }

    override fun getDescription(): String = "Push with strength $strength"
}

// ========================================
// BlindEffect - 盲目エフェクト
// ========================================

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

// ========================================
// SetBlockEffect - ブロック設置/破壊エフェクト
// ========================================

/**
 * SetBlockEffect - ターゲット位置にブロックを設置または破壊
 *
 * 指定されたブロックタイプを設置、または空気ブロックで破壊します。
 * radiusを指定すると複数ブロックを設置可能（CEL式対応）
 *
 * 使用例:
 * ```yaml
 * # 単一ブロック設置
 * - type: SET_BLOCK
 *   blockType: STONE
 *   radius: "0"
 *
 * # 範囲ブロック設置
 * - type: SET_BLOCK
 *   blockType: BARRIER
 *   radius: "2.0"
 *   temporary: true
 *   temporaryDuration: "5000"  # 5秒後に元に戻る
 * ```
 */
class SetBlockEffect(
    id: String = "set_block",
    private val blockType: String = "AIR",
    private val radius: String = "0.0",  // CEL式対応
    private val temporary: Boolean = false,
    private val temporaryDuration: String = "5000",  // 一時設置の持続時間 (ms)
    sync: Boolean = true
) : Effect(id, sync) {

    private val material: Material by lazy {
        ResourceKeyResolver.resolveMaterial(blockType, Material.AIR)
    }

    override suspend fun apply(source: Entity, target: Entity) {
        val radiusValue = evaluateRadius(source, target)
        setBlocks(target.location, radiusValue)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val radiusValue = evaluateRadius(source, target)
        setBlocks(target.location, radiusValue)
    }

    /**
     * ブロック設置処理
     */
    private fun setBlocks(location: Location, radiusValue: Double) {
        val world = location.world ?: return
        val centerBlock = location.block

        if (radiusValue <= 0.0) {
            // 単一ブロック設置
            setBlock(centerBlock)
        } else {
            // 範囲ブロック設置
            val radiusSquared = radiusValue * radiusValue
            val radiusInt = radiusValue.toInt()

            for (x in -radiusInt..radiusInt) {
                for (y in -radiusInt..radiusInt) {
                    for (z in -radiusInt..radiusInt) {
                        val block = world.getBlockAt(
                            centerBlock.x + x,
                            centerBlock.y + y,
                            centerBlock.z + z
                        )

                        val distanceSquared = (x * x + y * y + z * z).toDouble()
                        if (distanceSquared <= radiusSquared) {
                            setBlock(block)
                        }
                    }
                }
            }
        }

        DebugLogger.effect("SetBlock(${material.name}, radius=$radiusValue)", "at ${location.blockX},${location.blockY},${location.blockZ}")
    }

    /**
     * 単一ブロック設置
     */
    private fun setBlock(block: Block) {
        if (temporary) {
            // 元のブロック状態を保存
            val originalType = block.type
            val originalData = block.blockData.clone()

            // ブロックを設置
            block.type = material

            // 一定時間後に元に戻す
            val durationTicks = (evaluateTemporaryDuration() / 50).toInt().coerceAtLeast(1)
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("Unique")!!,
                Runnable {
                    block.type = originalType
                    block.blockData = originalData
                },
                durationTicks.toLong()
            )
        } else {
            // 永続的に設置
            block.type = material
        }
    }

    /**
     * Radius評価（Entity source）
     */
    private fun evaluateRadius(source: Entity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate radius: $radius", e)
            0.0
        }
    }

    /**
     * Radius評価（PacketEntity source）
     */
    private fun evaluateRadius(source: PacketEntity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate radius: $radius", e)
            0.0
        }
    }

    /**
     * Temporary duration評価
     */
    private fun evaluateTemporaryDuration(): Long {
        return try {
            temporaryDuration.toLongOrNull() ?: 5000L
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate temporary duration: $temporaryDuration", e)
            5000L
        }
    }

    override fun getDescription(): String = "Set $blockType in radius $radius" + if (temporary) " (temporary ${temporaryDuration}ms)" else ""
}