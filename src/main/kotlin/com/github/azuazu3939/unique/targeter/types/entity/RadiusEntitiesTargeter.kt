package com.github.azuazu3939.unique.targeter.types.entity

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.targeter.EntityTypeFilter
import com.github.azuazu3939.unique.targeter.TargetSortMode
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * 範囲内の全エンティティをターゲット
 */
class RadiusEntitiesTargeter(
    id: String = "radius_entities",
    private val range: Double = 16.0,
    filter: String? = null,
    entityType: EntityTypeFilter = EntityTypeFilter.LIVING,
    private val sortMode: TargetSortMode = TargetSortMode.NONE,
    private val sortExpression: String? = null,
    private val offset: Int = 0,
    private val limit: Int? = null
) : Targeter(id, filter, entityType) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        // 全てのエンティティを取得
        val allEntities = world.getNearbyEntitiesAsync(location, range, range, range)
            .filter { it != source && it.isValid && !it.isDead }

        // エンティティタイプでフィルタリング
        val typeFiltered = filterByEntityType(allEntities)

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, typeFiltered)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusEntities(range=$range, type=$entityType, sort=$sortMode, offset=$offset, limit=$limit)",
            result.size
        )
        return result
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        // 全てのエンティティを取得
        val allEntities = world.getNearbyEntitiesAsync(location, range, range, range)
            .filter { it.isValid && !it.isDead }

        // エンティティタイプでフィルタリング
        val typeFiltered = filterByEntityType(allEntities)

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, typeFiltered)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusEntities(range=$range, type=$entityType, sort=$sortMode, offset=$offset, limit=$limit) from PacketEntity",
            result.size
        )
        return result
    }

    override fun getDescription(): String {
        val parts = mutableListOf("Entities within $range blocks")
        parts.add("type: $entityType")
        if (sortMode != TargetSortMode.NONE) parts.add("sort: $sortMode")
        if (sortExpression != null) parts.add("sortExpr: $sortExpression")
        if (offset > 0) parts.add("offset: $offset")
        if (limit != null) parts.add("limit: $limit")
        return parts.joinToString(", ")
    }
}
