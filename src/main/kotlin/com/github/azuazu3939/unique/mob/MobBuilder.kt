package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.effect.types.combat.DamageEffect
import com.github.azuazu3939.unique.effect.types.combat.HealEffect
import com.github.azuazu3939.unique.effect.types.movement.KnockbackEffect
import com.github.azuazu3939.unique.effect.types.status.IgniteEffect
import com.github.azuazu3939.unique.effect.types.world.MessageEffect
import com.github.azuazu3939.unique.effect.types.world.ParticleEffect
import com.github.azuazu3939.unique.effect.types.world.SoundEffect
import com.github.azuazu3939.unique.skill.SkillMeta
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.targeter.types.basic.SelfTargeter
import com.github.azuazu3939.unique.targeter.types.entity.LineOfSightTargeter
import com.github.azuazu3939.unique.targeter.types.entity.RadiusEntitiesTargeter
import com.github.azuazu3939.unique.targeter.types.player.NearestPlayerTargeter
import com.github.azuazu3939.unique.targeter.types.player.RadiusPlayersTargeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.ResourceKeyResolver
import com.github.azuazu3939.unique.util.TimeParser
import com.github.azuazu3939.unique.util.getSound
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory

/**
 * Targeter、Effect、SkillMetaのビルダー
 */
object MobBuilder {

    /**
     * ターゲッターを構築
     */
    fun buildTargeter(definition: TargeterDefinition): Targeter {
        // filterはCEL式文字列として直接使用
        val filter = if (definition.condition != "true") {
            definition.condition
        } else null

        return when (definition.type.lowercase()) {
            "self" -> SelfTargeter(filter = filter)
            "nearestplayer" -> NearestPlayerTargeter(
                range = definition.range,
                filter = filter
            )
            "radiusplayers" -> RadiusPlayersTargeter(
                range = definition.range,
                filter = filter
            )
            "radiusentities" -> RadiusEntitiesTargeter(
                range = definition.range,
                filter = filter
            )
            "lineofsight" -> LineOfSightTargeter(
                maxDistance = definition.maxDistance,
                filter = filter
            )
            "targetlocation" -> {
                // @TL - 現在の攻撃ターゲット（MythicMobs互換）
                // PacketMobのcombat.targetをターゲットとして返す
                // 実装はCurrentAttackTargetTargeterで行うべきだが、暫定的にNearestPlayerを使用
                NearestPlayerTargeter(
                    range = definition.range,
                    filter = filter
                )
            }
            else -> {
                DebugLogger.warn("Unknown targeter type: ${definition.type}, defaulting to Self")
                SelfTargeter(filter = filter)
            }
        }
    }

    /**
     * エフェクトリストを構築
     */
    fun buildEffects(plugin: Unique, definitions: List<EffectDefinition>): List<Effect> {
        return definitions.mapNotNull { def ->
            try {
                buildEffect(plugin, def)
            } catch (e: Exception) {
                DebugLogger.error("Failed to build effect: ${def.type}", e)
                null
            }
        }
    }

    /**
     * 単一エフェクトを構築
     */
    private fun buildEffect(plugin: Unique, def: EffectDefinition): Effect {
        val sync = def.meta.sync

        return when (def.type.lowercase()) {
            "damage" -> DamageEffect(
                amount = def.amount.toString(),
                sync = sync
            )
            "heal" -> HealEffect(
                amount = def.amount.toString(),
                sync = sync
            )
            "knockback" -> KnockbackEffect(
                strength = def.strength,
                sync = sync
            )
            "particle" -> {
                val particle = ResourceKeyResolver.resolveParticle(
                    def.particle ?: "FLAME",
                    Particle.FLAME
                )
                ParticleEffect(
                    particle = particle,
                    count = def.count,
                    offsetX = def.offsetX,
                    offsetY = def.offsetY,
                    offsetZ = def.offsetZ,
                    sync = sync
                )
            }
            "sound" -> {
                val sound = try {
                    val key = NamespacedKey.minecraft(def.sound ?: "entity.generic.explode")
                    getSound(key)
                } catch (e: IllegalArgumentException) {
                    Sound.ENTITY_GENERIC_EXPLODE
                }
                SoundEffect(
                    sound = sound,
                    volume = def.volume,
                    pitch = def.pitch,
                    sync = sync,
                    category = SoundCategory.HOSTILE
                )
            }
            "message" -> MessageEffect(
                message = def.message ?: "",
                sync = sync
            )
            "ignite" -> {
                val duration = TimeParser.parse(def.duration)
                IgniteEffect(
                    duration = duration,
                    sync = sync
                )
            }
            else -> throw IllegalArgumentException("Unknown effect type: ${def.type}")
        }
    }

    /**
     * SkillMetaを構築
     */
    fun buildSkillMeta(def: SkillMetaDefinition): SkillMeta {
        val executeDelay = TimeParser.parse(def.executeDelay)
        val effectDelay = TimeParser.parse(def.effectDelay)

        return SkillMeta(
            sync = def.sync,
            executeDelay = executeDelay,
            effectDelay = effectDelay,
            cancelOnDeath = def.cancelOnDeath,
            interruptible = def.interruptible
        )
    }
}
