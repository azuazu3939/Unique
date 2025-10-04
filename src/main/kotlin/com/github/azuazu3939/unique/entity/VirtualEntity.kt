package com.github.azuazu3939.unique.entity

import com.github.retrooper.packetevents.protocol.entity.type.EntityType as PacketEntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Virtual entity implementation using only PacketEvents
 * Completely packet-based, no server-side entity
 */
class VirtualEntity(
    override val uuid: UUID = UUID.randomUUID(),
    override val entityId: Int = Random.nextInt(100000, 999999),
    override val entityType: EntityType,
    private val customName: Component? = null,
    initialHealth: Double = 20.0,
    private val maxHealth: Double = 20.0
) : AbstractEntity() {

    // Core state
    private var location: Location = Location(null, 0.0, 0.0, 0.0)
    private var velocity: Vector = Vector(0, 0, 0)
    private var health: Double = initialHealth
    private var lastDamageTime: Long = 0
    private var lastDamageCause: String? = null

    // Managers
    private val viewerManager = EntityViewerManager()
    private val stateManager = EntityStateManager(entityId, viewerManager)
    private val packetHelper = EntityPacketHelper

    // Potion effects
    private val activePotionEffects = ConcurrentHashMap<PotionEffectType, Pair<PotionEffect, Long>>()

    // PacketEvents entity type
    private val packetEntityType: PacketEntityType = EntityTypes.getByName(entityType.name)
        ?: EntityTypes.ZOMBIE

    init {
        // Set initial custom name if provided
        if (customName != null) {
            stateManager.setCustomName(customName, true)
        }
        stateManager.setHealth(health.toFloat())
    }

    // ============= Location & Movement =============

    override fun getLocation(): Location {
        return location.clone()
    }

    override fun getVelocity(): Vector {
        return velocity.clone()
    }

    override fun getPose(): Pose {
        // Map EntityState to Bukkit Pose
        return when (stateManager.getState()) {
            EntityState.SNEAKING -> Pose.SNEAKING
            EntityState.SWIMMING -> Pose.SWIMMING
            EntityState.GLIDING -> Pose.FALL_FLYING
            EntityState.DEAD -> Pose.DYING
            else -> Pose.STANDING
        }
    }

    // ============= Health & Combat =============

    override fun getHealth(): Double {
        return health
    }

    override fun getMaxHealth(): Double {
        return maxHealth
    }

    override fun damage(amount: Double, attacker: AbstractEntity?) {
        if (health <= 0) return

        health = (health - amount).coerceAtLeast(0.0)
        lastDamageTime = System.currentTimeMillis()
        lastDamageCause = "ENTITY_ATTACK"

        // Update health metadata
        stateManager.setHealth(health.toFloat())

        // Play damage animation
        stateManager.setState(EntityState.DAMAGED)

        // Check for death
        if (health <= 0) {
            stateManager.setState(EntityState.DEAD)
        } else {
            // Return to previous state after damage animation
            stateManager.setState(EntityState.SPAWNED)
        }
    }

    override fun heal(amount: Double) {
        health = (health + amount).coerceAtMost(maxHealth)
        stateManager.setHealth(health.toFloat())
    }

    // ============= State Management =============

    override fun getState(): EntityState {
        return stateManager.getState()
    }

    override fun setState(state: EntityState) {
        stateManager.setState(state)
    }

    override fun hasAI(): Boolean {
        return false // Virtual entities don't have AI
    }

    override fun hasGravity(): Boolean {
        return false // Virtual entities don't have gravity by default
    }

    override fun isOnGround(): Boolean {
        return location.clone().subtract(0.0, 0.1, 0.0).block.type.isSolid
    }

    override fun isInWater(): Boolean {
        return location.block.isLiquid
    }

    override fun isGliding(): Boolean {
        return stateManager.getState() == EntityState.GLIDING
    }

    override fun isSwimming(): Boolean {
        return stateManager.getState() == EntityState.SWIMMING
    }

    override fun isInsideVehicle(): Boolean {
        return stateManager.getState() == EntityState.IN_VEHICLE
    }

    override fun getFireTicks(): Int {
        return if (stateManager.getState() == EntityState.BURNING) 100 else 0
    }

    override fun getLastDamageCause(): String? {
        return lastDamageCause
    }

    override fun getLastDamageTime(): Long {
        return lastDamageTime
    }

    override fun getNoDamageTicks(): Int {
        return 0 // Virtual entities don't have invulnerability
    }

    // ============= Lifecycle =============

    override fun spawn(location: Location, viewers: Collection<Player>) {
        this.location = location.clone()

        // Add viewers
        viewerManager.addViewers(viewers)

        // Send spawn packet to all viewers
        viewers.forEach { player ->
            packetHelper.sendSpawnPacket(
                player,
                entityId,
                uuid,
                packetEntityType,
                location
            )

            // Send initial metadata
            val metadata = EntityMetadata.createBasic(
                customName = customName,
                customNameVisible = customName != null,
                health = health.toFloat()
            )
            packetHelper.sendMetadataPacket(player, entityId, metadata)
        }

        stateManager.setState(EntityState.SPAWNED)
    }

    override fun despawn() {
        val viewers = viewerManager.getViewers()

        // Send destroy packet to all viewers
        viewers.forEach { player ->
            packetHelper.sendDestroyPacket(player, entityId)
        }

        viewerManager.clearViewers()
        stateManager.setState(EntityState.DESPAWNED)
    }

    override fun teleport(location: Location) {
        this.location = location.clone()

        val viewers = viewerManager.getViewers()
        viewers.forEach { player ->
            packetHelper.sendTeleportPacket(player, entityId, location)
        }

        stateManager.setState(EntityState.TELEPORTING)
    }

    override fun move(delta: Vector) {
        location.add(delta)
        velocity = delta.clone()

        val viewers = viewerManager.getViewers()
        viewers.forEach { player ->
            packetHelper.sendRelativeMoveAndRotationPacket(
                player,
                entityId,
                delta,
                location.yaw,
                location.pitch,
                isOnGround()
            )
        }

        stateManager.setState(EntityState.MOVING)
    }

    /**
     * Move to target location smoothly
     */
    fun moveTo(targetLocation: Location, speed: Double) {
        val direction = targetLocation.toVector().subtract(location.toVector()).normalize()
        val delta = direction.multiply(speed)
        move(delta)
    }

    /**
     * Set velocity and send velocity packet
     */
    fun setVelocity(velocity: Vector) {
        this.velocity = velocity.clone()

        val viewers = viewerManager.getViewers()
        viewers.forEach { player ->
            packetHelper.sendVelocityPacket(player, entityId, velocity)
        }
    }

    // ============= Potion Effects =============

    override fun getPotionEffect(effect: PotionEffectType): PotionEffect? {
        cleanupExpiredEffects()
        return activePotionEffects[effect]?.first
    }

    override fun addPotionEffect(effect: PotionEffect) {
        val duration = effect.duration
        val expirationTime = if (duration == Int.MAX_VALUE || duration < 0) {
            Long.MAX_VALUE
        } else {
            System.currentTimeMillis() + (duration * 50L)
        }

        activePotionEffects[effect.type] = Pair(effect, expirationTime)
    }

    override fun removePotionEffect(effectType: PotionEffectType) {
        activePotionEffects.remove(effectType)
    }

    override fun hasPotionEffect(effectType: PotionEffectType): Boolean {
        cleanupExpiredEffects()
        return activePotionEffects.containsKey(effectType)
    }

    override fun clearPotionEffects() {
        activePotionEffects.clear()
    }

    private fun cleanupExpiredEffects() {
        val currentTime = System.currentTimeMillis()
        val expiredEffects = activePotionEffects.entries
            .filter { (_, pair) -> pair.second < currentTime }
            .map { it.key }
        expiredEffects.forEach { activePotionEffects.remove(it) }
    }

    // ============= Line of Sight =============

    override fun hasLineOfSight(other: AbstractEntity): Boolean {
        val otherLoc = other.getLocation()
        val distance = location.distance(otherLoc)
        if (distance > 100) return false

        val direction = otherLoc.toVector().subtract(location.toVector()).normalize()
        var checkLoc = location.clone()
        val step = 0.5

        for (i in 0 until (distance / step).toInt()) {
            checkLoc = checkLoc.add(direction.clone().multiply(step))
            if (checkLoc.block.type.isSolid) {
                return false
            }
        }

        return true
    }

    // ============= Player Interaction =============

    override fun isPlayer(): Boolean {
        return false
    }

    override fun asPlayer(): Player? {
        return null
    }

    // ============= Bukkit Integration =============

    override fun getBukkitEntity(): org.bukkit.entity.LivingEntity? {
        return null // Virtual entities have no Bukkit entity
    }

    // ============= Metadata =============

    override fun updateMetadata(key: String, value: Any) {
        stateManager.setMetadata(key, value)
    }

    override fun getMetadata(key: String): Any? {
        return stateManager.getMetadata(key)
    }

    override fun hasMetadata(key: String): Boolean {
        return stateManager.hasMetadata(key)
    }

    // ============= Viewer Management =============

    override fun addViewer(player: Player) {
        if (viewerManager.isViewing(player)) return

        viewerManager.addViewer(player)

        // Send spawn packet
        packetHelper.sendSpawnPacket(
            player,
            entityId,
            uuid,
            packetEntityType,
            location
        )

        // Send metadata
        val metadata = EntityMetadata.createBasic(
            customName = customName,
            customNameVisible = customName != null,
            health = health.toFloat()
        )
        packetHelper.sendMetadataPacket(player, entityId, metadata)
    }

    override fun removeViewer(player: Player) {
        if (!viewerManager.isViewing(player)) return

        viewerManager.removeViewer(player)

        // Send destroy packet
        packetHelper.sendDestroyPacket(player, entityId)
    }

    override fun getViewers(): Collection<Player> {
        return viewerManager.getViewers()
    }

    override fun isViewing(player: Player): Boolean {
        return viewerManager.isViewing(player)
    }

    // ============= Additional Utility Methods =============

    /**
     * Update custom name
     */
    fun setCustomName(name: Component?, visible: Boolean = true) {
        stateManager.setCustomName(name, visible)
    }

    /**
     * Set on fire
     */
    fun setOnFire(onFire: Boolean) {
        stateManager.setOnFire(onFire)
        if (onFire) {
            stateManager.setState(EntityState.BURNING)
        }
    }

    /**
     * Set invisible
     */
    fun setInvisible(invisible: Boolean) {
        stateManager.setInvisible(invisible)
        if (invisible) {
            stateManager.setState(EntityState.INVISIBLE)
        }
    }

    /**
     * Set glowing
     */
    fun setGlowing(glowing: Boolean) {
        stateManager.setGlowing(glowing)
        if (glowing) {
            stateManager.setState(EntityState.GLOWING)
        }
    }

    /**
     * Play swing animation
     */
    fun swingArm() {
        stateManager.setState(EntityState.ATTACKING)
    }
}
