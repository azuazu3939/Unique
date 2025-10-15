package com.github.azuazu3939.unique.targeter.types.player

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.distanceToAsync
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.nms.getPlayersAsync
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * 最も近いプレイヤーをターゲット
 */
class NearestPlayerTargeter(
    id: String = "nearest_player",
    private val range: Double = 16.0,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val nearbyPlayers = world.getNearbyEntitiesAsync(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        if (nearbyPlayers.isEmpty()) {
            DebugLogger.targeter("NearestPlayer", 0)
            return emptyList()
        }

        val nearest = nearbyPlayers.minByOrNull { it.location.distanceSquared(location) }
        val targets = if (nearest != null) listOf(nearest) else emptyList()
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("NearestPlayer(range=$range)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val nearbyPlayers = world.getPlayersAsync()
            .filter {
                it.distanceToAsync(location) <= range
            }

        if (nearbyPlayers.isEmpty()) {
            DebugLogger.targeter("NearestPlayer (PacketEntity)", 0)
            return emptyList()
        }

        val nearest = nearbyPlayers.minByOrNull { it.distanceToAsync(location)  }
        val targets = if (nearest != null) listOf(nearest) else emptyList()
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("NearestPlayer(range=$range) from PacketEntity", filtered.size)
        return filtered
    }

    override fun getDescription(): String = "Nearest player within $range blocks"
}
