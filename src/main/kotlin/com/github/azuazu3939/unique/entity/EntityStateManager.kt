package com.github.azuazu3939.unique.entity

import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import net.kyori.adventure.text.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages entity state transitions and metadata updates
 * Handles state machine logic and visual effects
 */
class EntityStateManager(
    private val entityId: Int,
    private val viewerManager: EntityViewerManager,
    private val packetHelper: EntityPacketHelper = EntityPacketHelper
) {

    private var currentState: EntityState = EntityState.UNSPAWNED
    private val stateHistory = mutableListOf<EntityState>()
    private var lastStateChangeTime: Long = System.currentTimeMillis()

    // Metadata cache
    private val metadataCache = ConcurrentHashMap<String, Any>()

    // State flags
    private var onFire: Boolean = false
    private var crouching: Boolean = false
    private var sprinting: Boolean = false
    private var swimming: Boolean = false
    private var invisible: Boolean = false
    private var glowing: Boolean = false
    private var elytraFlying: Boolean = false

    /**
     * Get current state
     */
    fun getState(): EntityState {
        return currentState
    }

    /**
     * Set new state (triggers state transition)
     */
    fun setState(newState: EntityState) {
        if (currentState == newState) return

        val oldState = currentState
        currentState = newState
        lastStateChangeTime = System.currentTimeMillis()

        // Add to history (keep last 10)
        stateHistory.add(oldState)
        if (stateHistory.size > 10) {
            stateHistory.removeAt(0)
        }

        // Handle state transitions
        onStateTransition(oldState, newState)
    }

    /**
     * Handle state transitions
     */
    private fun onStateTransition(from: EntityState, to: EntityState) {
        when (to) {
            EntityState.BURNING -> setOnFire(true)
            EntityState.INVISIBLE -> setInvisible(true)
            EntityState.GLOWING -> setGlowing(true)
            EntityState.SNEAKING -> setCrouching(true)
            EntityState.SPRINTING -> setSprinting(true)
            EntityState.SWIMMING -> setSwimming(true)
            EntityState.GLIDING -> setElytraFlying(true)
            EntityState.DEAD -> onDeath()
            EntityState.DAMAGED -> playDamageAnimation()
            EntityState.ATTACKING -> playAttackAnimation()
            else -> {
                // Reset flags for normal states
                if (from == EntityState.BURNING) setOnFire(false)
                if (from == EntityState.INVISIBLE) setInvisible(false)
                if (from == EntityState.GLOWING) setGlowing(false)
                if (from == EntityState.SNEAKING) setCrouching(false)
                if (from == EntityState.SPRINTING) setSprinting(false)
                if (from == EntityState.SWIMMING) setSwimming(false)
                if (from == EntityState.GLIDING) setElytraFlying(false)
            }
        }
    }

    /**
     * Set on fire state
     */
    fun setOnFire(value: Boolean) {
        if (onFire == value) return
        onFire = value
        updateFlags()
    }

    /**
     * Set crouching state
     */
    fun setCrouching(value: Boolean) {
        if (crouching == value) return
        crouching = value
        updateFlags()
        updatePose(if (value) EntityPose.CROUCHING else EntityPose.STANDING)
    }

    /**
     * Set sprinting state
     */
    fun setSprinting(value: Boolean) {
        if (sprinting == value) return
        sprinting = value
        updateFlags()
    }

    /**
     * Set swimming state
     */
    fun setSwimming(value: Boolean) {
        if (swimming == value) return
        swimming = value
        updateFlags()
        updatePose(if (value) EntityPose.SWIMMING else EntityPose.STANDING)
    }

    /**
     * Set invisible state
     */
    fun setInvisible(value: Boolean) {
        if (invisible == value) return
        invisible = value
        updateFlags()
    }

    /**
     * Set glowing state
     */
    fun setGlowing(value: Boolean) {
        if (glowing == value) return
        glowing = value
        updateFlags()
    }

    /**
     * Set elytra flying state
     */
    fun setElytraFlying(value: Boolean) {
        if (elytraFlying == value) return
        elytraFlying = value
        updateFlags()
        updatePose(if (value) EntityPose.FALL_FLYING else EntityPose.STANDING)
    }

    /**
     * Update entity flags metadata
     */
    private fun updateFlags() {
        val metadata = EntityMetadata.create()
            .setFlags(
                onFire = onFire,
                crouching = crouching,
                sprinting = sprinting,
                swimming = swimming,
                invisible = invisible,
                glowing = glowing,
                elytraFlying = elytraFlying
            )
            .build()

        broadcastMetadata(metadata)
    }

    /**
     * Update entity pose
     */
    private fun updatePose(pose: EntityPose) {
        val metadata = EntityMetadata.create()
            .setPose(pose)
            .build()

        broadcastMetadata(metadata)
    }

    /**
     * Update custom name
     */
    fun setCustomName(name: Component?, visible: Boolean = true) {
        val metadata = EntityMetadata.create()
            .setCustomName(name)
            .setCustomNameVisible(visible)
            .build()

        broadcastMetadata(metadata)
    }

    /**
     * Update health
     */
    fun setHealth(health: Float) {
        val metadata = EntityMetadata.create()
            .setHealth(health)
            .build()

        broadcastMetadata(metadata)
    }

    /**
     * Play damage animation
     */
    private fun playDamageAnimation() {
        val viewers = viewerManager.getViewers()
        viewers.forEach { player ->
            packetHelper.sendAnimationPacket(
                player,
                entityId,
                WrapperPlayServerEntityAnimation.EntityAnimationType.HURT
            )
        }
    }

    /**
     * Play attack animation
     */
    private fun playAttackAnimation() {
        val viewers = viewerManager.getViewers()
        viewers.forEach { player ->
            packetHelper.sendAnimationPacket(
                player,
                entityId,
                WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
            )
        }
    }

    /**
     * Handle death
     */
    private fun onDeath() {
        val viewers = viewerManager.getViewers()
        viewers.forEach { player ->
            // Death status (3 = death animation)
            packetHelper.sendStatusPacket(player, entityId, 3)
        }
    }

    /**
     * Broadcast metadata to all viewers
     */
    private fun broadcastMetadata(metadata: List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<*>>) {
        val viewers = viewerManager.getViewers()
        viewers.forEach { player ->
            packetHelper.sendMetadataPacket(player, entityId, metadata)
        }
    }

    /**
     * Get state history
     */
    fun getStateHistory(): List<EntityState> {
        return stateHistory.toList()
    }

    /**
     * Get time since last state change (milliseconds)
     */
    fun getTimeSinceStateChange(): Long {
        return System.currentTimeMillis() - lastStateChangeTime
    }

    /**
     * Store custom metadata
     */
    fun setMetadata(key: String, value: Any) {
        metadataCache[key] = value
    }

    /**
     * Get custom metadata
     */
    fun getMetadata(key: String): Any? {
        return metadataCache[key]
    }

    /**
     * Check if has metadata
     */
    fun hasMetadata(key: String): Boolean {
        return metadataCache.containsKey(key)
    }

    /**
     * Clear all metadata
     */
    fun clearMetadata() {
        metadataCache.clear()
    }
}
