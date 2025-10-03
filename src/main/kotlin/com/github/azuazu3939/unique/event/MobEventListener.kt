package com.github.azuazu3939.unique.event

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.mob.UniqueMob
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * Listens for Bukkit events and triggers mob skills accordingly
 *
 * Note: This listener handles REAL Bukkit entities only.
 * EntityLib packet-based fake entities are handled by EntityLibEventHandler instead,
 * since they don't trigger Bukkit events.
 *
 * This listener is used when UniqueMobs are backed by real server-side entities
 * (for hybrid or future implementations).
 */
class MobEventListener(private val plugin: Unique) : Listener {

    companion object {
        // Persistent data key for storing UniqueMob UUID on real entities
        private val MOB_UUID_KEY = NamespacedKey("unique", "mob_uuid")
    }

    /**
     * Handle entity damage events
     * Triggers ON_DAMAGED skills when a UniqueMob is damaged
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity as? LivingEntity ?: return

        // Find UniqueMob by entity
        val mob = findUniqueMobByEntity(entity) ?: return

        // Get damager entity if applicable
        val damager = if (event is EntityDamageByEntityEvent) {
            event.damager as? LivingEntity
        } else {
            null
        }

        // Trigger ON_DAMAGED via the mob's onDamaged method
        mob.onDamaged(damager, event.damage)
    }

    /**
     * Handle entity death events
     * Triggers ON_DEATH skills when a UniqueMob dies
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity

        // Find UniqueMob by entity
        val mob = findUniqueMobByEntity(entity) ?: return

        // Create trigger event
        val triggerEvent = TriggerEvent(
            trigger = SkillTrigger.ON_DEATH,
            triggerEntity = entity,
            target = entity
        )

        // Trigger skills
        mob.onTrigger(triggerEvent)

        // Clean up the entity link
        unlinkEntityFromMob(entity)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? LivingEntity ?: return

        // Find UniqueMob that is the attacker
        val attackerMob = findUniqueMobByEntity(attacker) ?: return

        // Get victim
        val victim = event.entity as? LivingEntity ?: return

        // Trigger ON_ATTACK skills
        attackerMob.onAttack(victim, attacker)
    }

    /**
     * Handle entity target events
     * Triggers ON_TARGET skills when a custom mob targets an entity
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityTarget(event: EntityTargetEvent) {
        val entity = event.entity as? LivingEntity ?: return

        // Find UniqueMob
        val mob = findUniqueMobByEntity(entity) ?: return

        // Get target
        val target = event.target as? LivingEntity ?: return

        // Create trigger event
        val triggerEvent = TriggerEvent(
            trigger = SkillTrigger.ON_TARGET,
            triggerEntity = null,
            target = target
        )

        // Trigger skills
        mob.onTrigger(triggerEvent)
    }

    /**
     * Find a UniqueMob by its Bukkit entity
     *
     * Uses persistent data container to link real Bukkit entities to UniqueMobs.
     * This only works for UniqueMobs that are backed by real server-side entities.
     *
     * For EntityLib packet-based fake entities, use EntityLibEventHandler instead.
     *
     * @return The UniqueMob if found and active, null otherwise
     */
    private fun findUniqueMobByEntity(entity: LivingEntity): UniqueMob? {
        // Try to get UniqueMob UUID from persistent data
        val mobUuidString = entity.persistentDataContainer.get(MOB_UUID_KEY, PersistentDataType.STRING)
            ?: return null

        // Parse UUID
        val mobUuid = try {
            UUID.fromString(mobUuidString)
        } catch (e: IllegalArgumentException) {
            plugin.debugLogger.warning("Invalid UniqueMob UUID in entity persistent data: $mobUuidString")
            return null
        }

        // Get UniqueMob from manager
        return plugin.mobManager.getMob(mobUuid)
    }

    /**
     * Link a real Bukkit entity to a UniqueMob
     *
     * This should be called when creating a UniqueMob that is backed by a real entity.
     * Stores the UniqueMob UUID in the entity's persistent data container.
     */
    fun linkEntityToMob(entity: LivingEntity, mobUuid: UUID) {
        entity.persistentDataContainer.set(MOB_UUID_KEY, PersistentDataType.STRING, mobUuid.toString())
        plugin.debugLogger.debug("Linked entity ${entity.uniqueId} to UniqueMob $mobUuid")
    }

    /**
     * Unlink a real Bukkit entity from a UniqueMob
     */
    fun unlinkEntityFromMob(entity: LivingEntity) {
        entity.persistentDataContainer.remove(MOB_UUID_KEY)
    }
}
