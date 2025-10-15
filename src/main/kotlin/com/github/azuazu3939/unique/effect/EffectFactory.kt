package com.github.azuazu3939.unique.effect

// Combat effects

// Movement effects

// Status effects

// World effects
import com.github.azuazu3939.unique.effect.types.combat.DamageEffect
import com.github.azuazu3939.unique.effect.types.combat.ExplosionEffect
import com.github.azuazu3939.unique.effect.types.combat.HealEffect
import com.github.azuazu3939.unique.effect.types.combat.LightningEffect
import com.github.azuazu3939.unique.effect.types.movement.PullEffect
import com.github.azuazu3939.unique.effect.types.movement.PushEffect
import com.github.azuazu3939.unique.effect.types.movement.TeleportEffect
import com.github.azuazu3939.unique.effect.types.movement.VelocityEffect
import com.github.azuazu3939.unique.effect.types.status.BlindEffect
import com.github.azuazu3939.unique.effect.types.status.FreezeEffect
import com.github.azuazu3939.unique.effect.types.status.PotionEffectEffect
import com.github.azuazu3939.unique.effect.types.status.ShieldEffect
import com.github.azuazu3939.unique.effect.types.world.*
import com.github.azuazu3939.unique.mob.EffectDefinition
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.ResourceKeyResolver
import com.github.azuazu3939.unique.util.getPotionEffectTypeName
import com.github.azuazu3939.unique.util.getSound
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.SoundCategory

/**
 * EffectFactory - Effect定義からEffectインスタンスを生成
 *
 * YAMLから読み込まれたEffectDefinitionを実際のEffectオブジェクトに変換します。
 */
object EffectFactory {

    /**
     * Effect定義からEffectインスタンスを生成
     *
     * @param definition Effect定義
     * @return Effectインスタンス、失敗時はnull
     */
    fun createEffect(definition: EffectDefinition): Effect? {
        return try {
            when (definition.type.lowercase()) {
                // ========== 基本Effect ==========
                "damage" -> createDamageEffect(definition)
                "heal" -> createHealEffect(definition)
                "potioneffect", "potion" -> createPotionEffect(definition)
                "particle" -> createParticleEffect(definition)
                "sound" -> createSoundEffect(definition)
                "message" -> createMessageEffect(definition)
                "command" -> createCommandEffect(definition)
                "velocity" -> createVelocityEffect(definition)

                // ========== 新Effect ==========
                "lightning" -> createLightningEffect(definition)
                "explosion" -> createExplosionEffect(definition)
                "freeze" -> createFreezeEffect(definition)
                "shield" -> createShieldEffect(definition)
                "teleport" -> createTeleportEffect(definition)
                "pull" -> createPullEffect(definition)
                "push" -> createPushEffect(definition)
                "blind" -> createBlindEffect(definition)
                "setblock", "set_block" -> createSetBlockEffect(definition)

                else -> {
                    DebugLogger.warn("Unknown effect type: ${definition.type}")
                    null
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to create effect: ${definition.type}", e)
            null
        }
    }

    /**
     * 複数のEffect定義からEffectインスタンスリストを生成
     */
    fun createEffects(definitions: List<EffectDefinition>): List<Effect> {
        return definitions.mapNotNull { createEffect(it) }
    }

    // ========================================
    // 基本Effect生成メソッド
    // ========================================

    private fun createDamageEffect(def: EffectDefinition): DamageEffect {
        // amountフィールドまたは専用フィールドから取得
        val amount = def.damageAmount?.toString() ?: def.amount.toString()

        return DamageEffect(
            amount = amount,
            sync = def.meta.sync
        )
    }

    private fun createHealEffect(def: EffectDefinition): HealEffect {
        val amount = def.healAmount?.toString() ?: def.amount.toString()

        return HealEffect(
            amount = amount,
            sync = def.meta.sync
        )
    }

    private fun createPotionEffect(def: EffectDefinition): PotionEffectEffect? {
        val potionTypeName = def.potionType ?: run {
            DebugLogger.error("PotionEffect requires potionType field")
            return null
        }

        val potionType = try {
            getPotionEffectTypeName(NamespacedKey.minecraft(potionTypeName.lowercase())).first()?.type
        } catch (e: Exception) {
            DebugLogger.error("Invalid potion type: $potionTypeName", e)
            return null
        }

        if (potionType == null) {
            DebugLogger.error("Unknown potion type: $potionTypeName")
            return null
        }

        return PotionEffectEffect(
            effectType = potionType,
            duration = def.potionDuration ?: def.duration,
            amplifier = def.potionAmplifier?.toString() ?: def.amplifier.toString(),
            sync = def.meta.sync
        )
    }

    private fun createParticleEffect(def: EffectDefinition): ParticleEffect? {
        val particleName = def.particle ?: run {
            DebugLogger.error("ParticleEffect requires particle field")
            return null
        }

        val particle = ResourceKeyResolver.resolveParticle(particleName, Particle.FLAME)

        return ParticleEffect(
            particle = particle,
            count = def.particleCount ?: def.count,
            offsetX = def.offsetX,
            offsetY = def.offsetY,
            offsetZ = def.offsetZ,
            sync = def.meta.sync
        )
    }

    private fun createSoundEffect(def: EffectDefinition): SoundEffect? {
        val soundName = def.sound ?: run {
            DebugLogger.error("SoundEffect requires sound field")
            return null
        }

        val sound = try {
            getSound(NamespacedKey.minecraft(soundName.lowercase()))
        } catch (e: IllegalArgumentException) {
            DebugLogger.error("Invalid sound type: $soundName", e)
            return null
        }

        return SoundEffect(
            sound = sound,
            category = SoundCategory.HOSTILE,
            volume = def.soundVolume ?: def.volume,
            pitch = def.soundPitch ?: def.pitch,
            sync = def.meta.sync
        )
    }

    private fun createMessageEffect(def: EffectDefinition): MessageEffect? {
        val message = def.message ?: run {
            DebugLogger.error("MessageEffect requires message field")
            return null
        }

        return MessageEffect(
            message = message,
            sync = def.meta.sync
        )
    }

    private fun createCommandEffect(def: EffectDefinition): CommandEffect? {
        val command = def.command ?: run {
            DebugLogger.error("CommandEffect requires command field")
            return null
        }

        return CommandEffect(
            command = command,
            asOp = def.asOp,
            sync = def.meta.sync
        )
    }

    private fun createVelocityEffect(def: EffectDefinition): VelocityEffect {
        return VelocityEffect(
            velocityX = def.velocityX,
            velocityY = def.velocityY,
            velocityZ = def.velocityZ,
            sync = def.meta.sync
        )
    }

    // ========================================
    // 新Effect生成メソッド
    // ========================================

    private fun createLightningEffect(def: EffectDefinition): LightningEffect {
        return LightningEffect(
            damage = def.lightningDamage?.toString() ?: def.damageAmount?.toString() ?: def.amount.toString(),
            setFire = def.lightningSetFire ?: def.setFire ?: true,
            visualOnly = def.lightningVisualOnly ?: false,
            sync = def.meta.sync
        )
    }

    private fun createExplosionEffect(def: EffectDefinition): ExplosionEffect {
        return ExplosionEffect(
            damage = def.explosionDamage?.toString() ?: def.damageAmount?.toString() ?: def.amount.toString(),
            radius = def.explosionRadius?.toString() ?: def.radius?.toString() ?: "3.0",
            knockback = def.explosionKnockback?.toString() ?: def.knockback?.toString() ?: "1.0",
            setFire = def.explosionSetFire ?: def.setFire ?: false,
            breakBlocks = def.explosionBreakBlocks ?: false,
            sync = def.meta.sync
        )
    }

    private fun createFreezeEffect(def: EffectDefinition): FreezeEffect {
        return FreezeEffect(
            duration = def.freezeDuration ?: def.potionDuration ?: def.duration,
            amplifier = def.freezeAmplifier?.toString() ?: def.potionAmplifier?.toString() ?: def.amplifier.toString(),
            sync = def.meta.sync
        )
    }

    private fun createShieldEffect(def: EffectDefinition): ShieldEffect {
        return ShieldEffect(
            amount = def.shieldAmount?.toString() ?: def.healAmount?.toString() ?: def.amount.toString(),
            duration = def.shieldDuration ?: def.duration,
            sync = def.meta.sync
        )
    }

    private fun createTeleportEffect(def: EffectDefinition): TeleportEffect? {
        val modeName = def.teleportMode ?: "TO_SOURCE"
        val mode = try {
            TeleportEffect.TeleportMode.valueOf(modeName.uppercase())
        } catch (e: IllegalArgumentException) {
            DebugLogger.error("Invalid teleport mode: $modeName", e)
            return null
        }

        return TeleportEffect(
            mode = mode,
            range = def.teleportRange?.toString() ?: def.radius?.toString() ?: "5.0",
            sync = def.meta.sync
        )
    }

    private fun createPullEffect(def: EffectDefinition): PullEffect {
        return PullEffect(
            strength = def.pullStrength?.toString() ?: def.strength.toString(),
            radius = def.pullRadius ?: def.radius ?: 0.0,
            sync = def.meta.sync
        )
    }

    private fun createPushEffect(def: EffectDefinition): PushEffect {
        return PushEffect(
            strength = def.pushStrength?.toString() ?: def.strength.toString(),
            radius = def.pushRadius ?: def.radius ?: 0.0,
            sync = def.meta.sync
        )
    }

    private fun createBlindEffect(def: EffectDefinition): BlindEffect {
        return BlindEffect(
            duration = def.blindDuration ?: def.potionDuration ?: def.duration,
            amplifier = def.blindAmplifier ?: def.potionAmplifier ?: def.amplifier,
            sync = def.meta.sync
        )
    }

    private fun createSetBlockEffect(def: EffectDefinition): SetBlockEffect {
        return SetBlockEffect(
            blockType = def.blockType ?: "AIR",
            radius = def.blockRadius?.toString() ?: def.radius?.toString() ?: "0.0",
            temporary = def.blockTemporary ?: false,
            temporaryDuration = def.blockTemporaryDuration ?: "5000",
            sync = def.meta.sync
        )
    }

    /**
     * Effectタイプの検証
     *
     * @param type Effectタイプ
     * @return 有効な場合true
     */
    fun isValidEffectType(type: String): Boolean {
        return type.lowercase() in listOf(
            // 基本
            "damage", "heal", "potioneffect", "potion", "particle",
            "sound", "message", "command", "velocity",
            // 新Effect
            "lightning", "explosion", "freeze", "shield",
            "teleport", "pull", "push", "blind", "setblock", "set_block"
        )
    }

    /**
     * 利用可能なEffectタイプ一覧を取得
     */
    fun getAvailableEffectTypes(): List<String> {
        return listOf(
            "damage", "heal", "potioneffect", "particle", "sound",
            "message", "command", "velocity",
            "lightning", "explosion", "freeze", "shield",
            "teleport", "pull", "push", "blind", "setblock"
        )
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.info("=== EffectFactory Debug Info ===")
        DebugLogger.info("Available effect types: ${getAvailableEffectTypes().joinToString(", ")}")
        DebugLogger.info("Total effect types: ${getAvailableEffectTypes().size}")
    }
}
