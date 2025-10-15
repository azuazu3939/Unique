package com.github.azuazu3939.unique.targeter.types.advanced

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * ランダムターゲット
 */
class RandomTargeter(
    id: String = "random",
    private val baseTargeter: Targeter,
    private val count: Int = 1,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val allTargets = baseTargeter.getTargets(source)

        if (allTargets.isEmpty() || count <= 0) {
            return emptyList()
        }

        val shuffled = allTargets.shuffled()
        val selected = shuffled.take(count.coerceAtMost(allTargets.size))
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Random(count=$count)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val allTargets = baseTargeter.getTargets(source)

        if (allTargets.isEmpty() || count <= 0) {
            return emptyList()
        }

        val shuffled = allTargets.shuffled()
        val selected = shuffled.take(count.coerceAtMost(allTargets.size))
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Random(count=$count) from PacketEntity", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Random $count from (${baseTargeter.getDescription()})"
    }
}
