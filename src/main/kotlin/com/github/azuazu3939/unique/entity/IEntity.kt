package com.github.azuazu3939.unique.entity

import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Pose
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Entity abstraction interface
 * Supports both Bukkit LivingEntity and EntityLib packet-based entities
 */
interface IEntity {
    /**
     * Get entity UUID
     */
    fun getUniqueId(): UUID

    /**
     * Get entity location
     */
    fun getLoc(): Location

    /**
     * Get entity type
     */
    fun getEntityType(): EntityType

    /**
     * Get current health
     */
    fun getHealth(): Double

    /**
     * Get maximum health
     */
    fun getMaxHealth(): Double

    /**
     * Get entity velocity
     */
    fun getVelocity(): Vector

    /**
     * Get entity pose/stance
     */
    fun getPose(): Pose

    /**
     * Check if entity has AI
     */
    fun hasAI(): Boolean

    /**
     * Check if entity has gravity
     */
    fun hasGravity(): Boolean

    /**
     * Check if entity is on ground
     */
    fun isOnGround(): Boolean

    /**
     * Check if entity is in water
     */
    fun isInWater(): Boolean

    /**
     * Check if entity is gliding
     */
    fun isGliding(): Boolean

    /**
     * Check if entity is swimming
     */
    fun isSwimming(): Boolean

    /**
     * Check if entity is inside vehicle
     */
    fun isInsideVehicle(): Boolean

    /**
     * Get fire ticks
     */
    fun getFireTicks(): Int

    /**
     * Get last damage cause
     */
    fun getLastDamageCause(): String?

    /**
     * Get last damage time
     */
    fun getLastDamageTime(): Long

    /**
     * Get no damage ticks
     */
    fun getNoDamageTicks(): Int

    /**
     * Check if has line of sight to another entity
     */
    fun hasLineOfSight(other: IEntity): Boolean

    /**
     * Get the underlying Bukkit entity if available
     * Returns null for packet-based entities
     */
    fun getBukkitEntity(): org.bukkit.entity.LivingEntity?

    /**
     * Check if this is a player
     */
    fun isPlayer(): Boolean

    /**
     * Get as player if this is a player
     */
    fun asPlayer(): org.bukkit.entity.Player?

    /**
     * Get potion effect from potion effect type
     */
    fun getPotionEffect(effect: PotionEffectType): PotionEffect?

    /**
     * Add potion effect the target
     */
    fun addPotionEffect(effect: PotionEffect)

    /**
     * ダメージを対象に与える。
     */
    fun damage(damage: Double, attacker: IEntity?)
}
