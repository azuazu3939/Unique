package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.mob.toHealth
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Adapter for Bukkit LivingEntity
 * Wraps a real server-side entity to conform to AbstractEntity interface
 */
class BukkitEntityAdapter(private val entity: LivingEntity) : AbstractEntity() {

    // Custom metadata storage
    private val customMetadata = ConcurrentHashMap<String, Any>()
    private var currentState: EntityState = EntityState.SPAWNED

    // ============= Core Identity =============

    override val uuid: UUID
        get() = entity.uniqueId

    override val entityId: Int
        get() = entity.entityId

    override val entityType: EntityType
        get() = entity.type

    // ============= Location & Movement =============

    override fun getLocation(): Location {
        return entity.location
    }

    override fun getVelocity(): Vector {
        return entity.velocity
    }

    override fun getPose(): Pose {
        return entity.pose
    }

    // ============= Health & Combat =============

    override fun getHealth(): Double {
        return entity.health
    }

    override fun getMaxHealth(): Double {
        return entity.toHealth()
    }

    override fun damage(amount: Double, attacker: AbstractEntity?) {
        val bukkitAttacker = attacker?.getBukkitEntity()
        if (bukkitAttacker != null) {
            entity.damage(amount, bukkitAttacker)
        } else {
            entity.damage(amount)
        }
        currentState = EntityState.DAMAGED
    }

    override fun heal(amount: Double) {
        entity.health = (entity.health + amount).coerceAtMost(getMaxHealth())
    }

    // ============= State Management =============

    override fun getState(): EntityState {
        // Derive state from Bukkit entity state
        return when {
            entity.isDead -> EntityState.DEAD
            entity.fireTicks > 0 -> EntityState.BURNING
            entity.isGliding -> EntityState.GLIDING
            entity.isSwimming -> EntityState.SWIMMING
            entity.isSneaking -> EntityState.SNEAKING
            (entity as? Player)?.isSprinting == true -> EntityState.SPRINTING
            entity.isInsideVehicle -> EntityState.IN_VEHICLE
            entity.isInvisible -> EntityState.INVISIBLE
            entity.isGlowing -> EntityState.GLOWING
            else -> currentState
        }
    }

    override fun setState(state: EntityState) {
        currentState = state
        // Apply state to Bukkit entity where possible
        when (state) {
            EntityState.BURNING -> entity.fireTicks = 100
            EntityState.INVISIBLE -> entity.isInvisible = true
            EntityState.GLOWING -> entity.isGlowing = true
            else -> {}
        }
    }

    override fun hasAI(): Boolean {
        return entity.hasAI()
    }

    override fun hasGravity(): Boolean {
        return entity.hasGravity()
    }

    override fun isOnGround(): Boolean {
        return entity.isOnGround
    }

    override fun isInWater(): Boolean {
        return entity.isInWater
    }

    override fun isGliding(): Boolean {
        return entity.isGliding
    }

    override fun isSwimming(): Boolean {
        return entity.isSwimming
    }

    override fun isInsideVehicle(): Boolean {
        return entity.isInsideVehicle
    }

    override fun getFireTicks(): Int {
        return entity.fireTicks
    }

    override fun getLastDamageCause(): String? {
        return entity.lastDamageCause?.cause?.name
    }

    override fun getLastDamageTime(): Long {
        // Bukkit doesn't track this, return 0
        return 0
    }

    override fun getNoDamageTicks(): Int {
        return entity.noDamageTicks
    }

    // ============= Lifecycle =============

    override fun spawn(location: Location, viewers: Collection<Player>) {
        // Bukkit entities are already spawned, just teleport
        entity.teleport(location)
        currentState = EntityState.SPAWNED
    }

    override fun despawn() {
        entity.remove()
        currentState = EntityState.DESPAWNED
    }

    override fun teleport(location: Location) {
        entity.teleport(location)
        currentState = EntityState.TELEPORTING
    }

    override fun move(delta: Vector) {
        entity.velocity = delta
        currentState = EntityState.MOVING
    }

    // ============= Potion Effects =============

    override fun getPotionEffect(effect: PotionEffectType): PotionEffect? {
        return entity.getPotionEffect(effect)
    }

    override fun addPotionEffect(effect: PotionEffect) {
        entity.addPotionEffect(effect)
    }

    override fun removePotionEffect(effectType: PotionEffectType) {
        entity.removePotionEffect(effectType)
    }

    override fun hasPotionEffect(effectType: PotionEffectType): Boolean {
        return entity.hasPotionEffect(effectType)
    }

    override fun clearPotionEffects() {
        entity.activePotionEffects.forEach { effect ->
            entity.removePotionEffect(effect.type)
        }
    }

    // ============= Line of Sight =============

    override fun hasLineOfSight(other: AbstractEntity): Boolean {
        val otherBukkit = other.getBukkitEntity() ?: return false
        return entity.hasLineOfSight(otherBukkit)
    }

    // ============= Player Interaction =============

    override fun isPlayer(): Boolean {
        return entity is Player
    }

    override fun asPlayer(): Player? {
        return entity as? Player
    }

    // ============= Bukkit Integration =============

    override fun getBukkitEntity(): LivingEntity {
        return entity
    }

    // ============= Metadata =============

    override fun updateMetadata(key: String, value: Any) {
        customMetadata[key] = value
    }

    override fun getMetadata(key: String): Any? {
        return customMetadata[key]
    }

    override fun hasMetadata(key: String): Boolean {
        return customMetadata.containsKey(key)
    }

    // ============= Viewer Management =============

    override fun addViewer(player: Player) {
        // Bukkit entities are visible to all players by default
        // No action needed
    }

    override fun removeViewer(player: Player) {
        // Bukkit entities are visible to all players by default
        // Cannot selectively hide without packets
    }

    override fun getViewers(): Collection<Player> {
        // Return all players in world (Bukkit entities visible to all)
        return entity.world.players
    }

    override fun isViewing(player: Player): Boolean {
        // Bukkit entities are visible to all players in same world
        return player.world == entity.world
    }
}
