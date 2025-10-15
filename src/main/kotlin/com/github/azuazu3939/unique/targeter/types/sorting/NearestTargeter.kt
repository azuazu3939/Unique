package com.github.azuazu3939.unique.targeter.types.sorting

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * NearestTargeter - 最も近いエンティティを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もソースに近いエンティティを選択します。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 */
class NearestTargeter(
    id: String = "nearest",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Nearest (no candidates)", 0)
            return emptyList()
        }

        val nearest = candidates.minByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(nearest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Nearest (selected: ${nearest.name})", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Nearest (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val nearest = candidates.minByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(nearest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Nearest from PacketEntity (selected: ${nearest.name})", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Nearest from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "NearestTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}
