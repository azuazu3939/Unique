package com.github.azuazu3939.unique.event

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.mob.UniqueMob
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import org.bukkit.entity.Player
import java.util.*

/**
 * Handles EntityLib packet-based events for fake entities
 * Since EntityLib entities are client-side only (packets), they don't trigger normal Bukkit events
 */
class EntityLibEventHandler(private val plugin: Unique) : PacketListener {

    /**
     * Track damage dealt to EntityLib entities
     * Maps entity ID -> (last damager UUID, damage amount, timestamp)
     */
    private val damageTracking = mutableMapOf<Int, DamageInfo>()

    data class DamageInfo(
        val damagerUuid: UUID,
        val damage: Double,
        val timestamp: Long
    )

    /**
     * Handle incoming packets from clients
     */
    override fun onPacketReceive(event: PacketReceiveEvent) {
        when (event.packetType) {
            PacketType.Play.Client.INTERACT_ENTITY -> handleInteract(event)
            else -> {}
        }
    }

    /**
     * Handle entity interaction packets
     * This includes attacks and right-clicks on EntityLib fake entities
     */
    private fun handleInteract(event: PacketReceiveEvent) {
        val packet = WrapperPlayClientInteractEntity(event)
        val player = event.getPlayer<Player>() ?: return
        val entityId = packet.entityId

        // Find mob by entity ID
        val mob = findMobByEntityId(entityId) ?: return

        when (packet.action) {
            WrapperPlayClientInteractEntity.InteractAction.ATTACK -> {
                // Player attacked the EntityLib fake entity
                handleAttack(mob, player)
            }

            WrapperPlayClientInteractEntity.InteractAction.INTERACT,
            WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT -> {
                // Player right-clicked the EntityLib fake entity
                handleRightClick(mob, player)
            }

            else -> {}
        }
    }

    /**
     * Handle player attacking EntityLib entity
     */
    private fun handleAttack(mob: UniqueMob, player: Player) {
        // Calculate damage (basic implementation - can be enhanced)
        val damage = calculatePlayerDamage(player)

        // Track damage for this entity
        damageTracking[mob.getEntityId()] = DamageInfo(
            damagerUuid = player.uniqueId,
            damage = damage,
            timestamp = System.currentTimeMillis()
        )

        // Trigger ON_DAMAGED on the mob
        mob.onDamaged(player, damage)

        plugin.debugLogger.debug("Player ${player.name} attacked mob ${mob.definition.id} for $damage damage")
    }

    /**
     * Handle player right-clicking EntityLib entity
     */
    private fun handleRightClick(mob: UniqueMob, player: Player) {
        // Trigger ON_INTERACT event
        val event = TriggerEvent(
            trigger = SkillTrigger.ON_INTERACT,
            triggerEntity = player,
            target = player
        )
        mob.onTrigger(event)

        plugin.debugLogger.debug("Player ${player.name} interacted with mob ${mob.definition.id}")
    }

    /**
     * Calculate damage dealt by player
     */
    private fun calculatePlayerDamage(player: Player): Double {
        // Basic damage calculation
        val itemInHand = player.inventory.itemInMainHand

        // Base damage from item
        val baseDamage = when (itemInHand.type.toString()) {
            "DIAMOND_SWORD" -> 7.0
            "IRON_SWORD" -> 6.0
            "STONE_SWORD" -> 5.0
            "WOODEN_SWORD", "GOLDEN_SWORD" -> 4.0
            "DIAMOND_AXE" -> 9.0
            "IRON_AXE" -> 9.0
            "STONE_AXE" -> 9.0
            "WOODEN_AXE", "GOLDEN_AXE" -> 7.0
            else -> 1.0 // Fist damage
        }

        // TODO: Add enchantment bonuses (Sharpness, etc.)
        // TODO: Add potion effect bonuses (Strength, etc.)
        // TODO: Add critical hit calculation

        return baseDamage
    }

    /**
     * Find a UniqueMob by EntityLib entity ID
     */
    private fun findMobByEntityId(entityId: Int): UniqueMob? {
        return plugin.mobManager.getActiveMobs().firstOrNull { mob ->
            mob.getEntityId() == entityId
        }
    }

    /**
     * Get recent damage info for an entity
     */
    fun getDamageInfo(entityId: Int): DamageInfo? {
        val info = damageTracking[entityId]
        // Clean up old entries (older than 5 seconds)
        if (info != null && System.currentTimeMillis() - info.timestamp > 5000) {
            damageTracking.remove(entityId)
            return null
        }
        return info
    }

    /**
     * Clear damage tracking for an entity
     */
    fun clearDamageTracking(entityId: Int) {
        damageTracking.remove(entityId)
    }
}
