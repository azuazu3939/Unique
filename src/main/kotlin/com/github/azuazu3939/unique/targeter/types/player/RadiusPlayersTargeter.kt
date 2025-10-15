package com.github.azuazu3939.unique.targeter.types.player

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.distanceToAsync
import com.github.azuazu3939.unique.nms.getPlayersAsync
import com.github.azuazu3939.unique.targeter.TargetSortMode
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * 範囲内の全プレイヤーをターゲット
 */
class RadiusPlayersTargeter(
    id: String = "radius_players",
    private val range: Double = 16.0,
    filter: String? = null,
    private val sortMode: TargetSortMode = TargetSortMode.NONE,
    private val sortExpression: String? = null,
    private val offset: Int = 0,
    private val limit: Int? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val targets = world.getPlayersAsync()
            .filter {
                it.distanceToAsync(location) <= range
            }

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, targets)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusPlayers(range=$range, sort=$sortMode, offset=$offset, limit=$limit)",
            result.size
        )
        return result
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val targets = world.getPlayersAsync()
            .filter {
                it.distanceToAsync(location) <= range
            }

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, targets)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusPlayers(range=$range, sort=$sortMode, offset=$offset, limit=$limit) from PacketEntity",
            result.size
        )
        return result
    }

    override fun getDescription(): String {
        val parts = mutableListOf("Players within $range blocks")
        if (sortMode != TargetSortMode.NONE) parts.add("sort: $sortMode")
        if (sortExpression != null) parts.add("sortExpr: $sortExpression")
        if (offset > 0) parts.add("offset: $offset")
        if (limit != null) parts.add("limit: $limit")
        return parts.joinToString(", ")
    }
}
