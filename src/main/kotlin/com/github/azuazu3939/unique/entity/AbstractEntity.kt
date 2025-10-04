package com.github.azuazu3939.unique.entity

import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Abstract base class for all entities in the Unique plugin
 * Supports both virtual (packet-based) and Bukkit entities
 */
abstract class AbstractEntity {

    // ============= Core Identity =============

    /**
     * Unique identifier for this entity
     */
    abstract val uuid: UUID

    /**
     * Minecraft entity ID (network ID)
     */
    abstract val entityId: Int

    /**
     * Bukkit entity type
     */
    abstract val entityType: EntityType

    // ============= Location & Movement =============

    /**
     * Get current location
     */
    abstract fun getLocation(): Location

    /**
     * Get current velocity
     */
    abstract fun getVelocity(): Vector

    /**
     * Get current pose/stance
     */
    abstract fun getPose(): Pose

    // ============= Health & Combat =============

    /**
     * Get current health
     */
    abstract fun getHealth(): Double

    /**
     * Get maximum health
     */
    abstract fun getMaxHealth(): Double

    /**
     * Take damage from attacker
     */
    abstract fun damage(amount: Double, attacker: AbstractEntity?)

    /**
     * Heal the entity
     */
    abstract fun heal(amount: Double)

    // ============= State Management =============

    /**
     * Get current entity state
     */
    abstract fun getState(): EntityState

    /**
     * Set entity state (triggers state transitions)
     */
    abstract fun setState(state: EntityState)

    /**
     * Check if entity has AI
     */
    abstract fun hasAI(): Boolean

    /**
     * Check if entity has gravity
     */
    abstract fun hasGravity(): Boolean

    /**
     * Check if entity is on ground
     */
    abstract fun isOnGround(): Boolean

    /**
     * Check if entity is in water
     */
    abstract fun isInWater(): Boolean

    /**
     * Check if entity is gliding
     */
    abstract fun isGliding(): Boolean

    /**
     * Check if entity is swimming
     */
    abstract fun isSwimming(): Boolean

    /**
     * Check if entity is inside vehicle
     */
    abstract fun isInsideVehicle(): Boolean

    /**
     * Get fire ticks
     */
    abstract fun getFireTicks(): Int

    /**
     * Get last damage cause
     */
    abstract fun getLastDamageCause(): String?

    /**
     * Get last damage time
     */
    abstract fun getLastDamageTime(): Long

    /**
     * Get no damage ticks (invulnerability)
     */
    abstract fun getNoDamageTicks(): Int

    // ============= Lifecycle =============

    /**
     * Spawn entity at location for viewers
     */
    abstract fun spawn(location: Location, viewers: Collection<Player>)

    /**
     * Despawn entity from all viewers
     */
    abstract fun despawn()

    /**
     * Teleport entity to new location
     */
    abstract fun teleport(location: Location)

    /**
     * Move entity by delta vector
     */
    abstract fun move(delta: Vector)

    // ============= Potion Effects =============

    /**
     * Get active potion effect
     */
    abstract fun getPotionEffect(effect: PotionEffectType): PotionEffect?

    /**
     * Add potion effect to entity
     */
    abstract fun addPotionEffect(effect: PotionEffect)

    /**
     * Remove potion effect from entity
     */
    abstract fun removePotionEffect(effectType: PotionEffectType)

    /**
     * Check if entity has potion effect
     */
    abstract fun hasPotionEffect(effectType: PotionEffectType): Boolean

    /**
     * Clear all potion effects
     */
    abstract fun clearPotionEffects()

    // ============= Line of Sight =============

    /**
     * Check if this entity has line of sight to another entity
     */
    abstract fun hasLineOfSight(other: AbstractEntity): Boolean

    // ============= Player Interaction =============

    /**
     * Check if this entity is a player
     */
    abstract fun isPlayer(): Boolean

    /**
     * Get as player if this is a player
     */
    abstract fun asPlayer(): Player?

    // ============= Bukkit Integration =============

    /**
     * Get the underlying Bukkit entity if available
     * Returns null for virtual entities
     */
    abstract fun getBukkitEntity(): org.bukkit.entity.LivingEntity?

    // ============= Metadata =============

    /**
     * Update entity metadata
     */
    abstract fun updateMetadata(key: String, value: Any)

    /**
     * Get entity metadata
     */
    abstract fun getMetadata(key: String): Any?

    /**
     * Check if entity has metadata key
     */
    abstract fun hasMetadata(key: String): Boolean

    // ============= Viewer Management =============

    /**
     * Add viewer to this entity
     */
    abstract fun addViewer(player: Player)

    /**
     * Remove viewer from this entity
     */
    abstract fun removeViewer(player: Player)

    /**
     * Get all current viewers
     */
    abstract fun getViewers(): Collection<Player>

    /**
     * Check if player is viewing this entity
     */
    abstract fun isViewing(player: Player): Boolean
}
