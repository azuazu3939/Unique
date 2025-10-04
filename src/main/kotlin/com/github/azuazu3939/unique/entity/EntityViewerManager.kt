package com.github.azuazu3939.unique.entity

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages which players can see which entities
 * Thread-safe viewer tracking for virtual entities
 */
class EntityViewerManager {

    private val viewers = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Add a viewer to this entity
     */
    fun addViewer(player: Player) {
        viewers.add(player.uniqueId)
    }

    /**
     * Remove a viewer from this entity
     */
    fun removeViewer(player: Player) {
        viewers.remove(player.uniqueId)
    }

    /**
     * Check if player is viewing this entity
     */
    fun isViewing(player: Player): Boolean {
        return viewers.contains(player.uniqueId)
    }

    /**
     * Get all current viewers
     * Filters out offline players
     */
    fun getViewers(): Collection<Player> {
        return viewers.mapNotNull { uuid ->
            org.bukkit.Bukkit.getPlayer(uuid)
        }
    }

    /**
     * Get viewer count
     */
    fun getViewerCount(): Int {
        return viewers.size
    }

    /**
     * Clear all viewers
     */
    fun clearViewers() {
        viewers.clear()
    }

    /**
     * Check if has any viewers
     */
    fun hasViewers(): Boolean {
        return viewers.isNotEmpty()
    }

    /**
     * Add multiple viewers
     */
    fun addViewers(players: Collection<Player>) {
        players.forEach { addViewer(it) }
    }

    /**
     * Remove multiple viewers
     */
    fun removeViewers(players: Collection<Player>) {
        players.forEach { removeViewer(it) }
    }
}
