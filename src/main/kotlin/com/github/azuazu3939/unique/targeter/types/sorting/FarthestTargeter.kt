package com.github.azuazu3939.unique.targeter.types.sorting

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * FarthestTargeter - 最も遠いエンティティを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もソースから遠いエンティティを選択します。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 */
class FarthestTargeter(
    id: String = "farthest",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Farthest (no candidates)", 0)
            return emptyList()
        }

        val farthest = candidates.maxByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(farthest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Farthest (selected: ${farthest.name})", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Farthest (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val farthest = candidates.maxByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(farthest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Farthest from PacketEntity (selected: ${farthest.name})", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Farthest from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "FarthestTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}
