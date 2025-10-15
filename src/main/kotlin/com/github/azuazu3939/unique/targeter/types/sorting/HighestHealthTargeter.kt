package com.github.azuazu3939.unique.targeter.types.sorting

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.maxHealth
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * HighestHealthTargeter - 最もHPが高いターゲットを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もHPが高いエンティティを選択します。
 * タンクキャラへの攻撃やボス優先攻撃などに有用です。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 *
 * 使用例:
 * ```yaml
 * targeter:
 *   type: HighestHealth
 *   baseTargeter:
 *     type: RadiusEntities
 *     range: 15.0
 * ```
 */
class HighestHealthTargeter(
    id: String = "highest_health",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (no candidates)", 0)
            return emptyList()
        }

        // LivingEntityのみを対象にし、最もHPが高いものを選択
        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (no living entities)", 0)
            return emptyList()
        }

        val highestHealth = livingCandidates.maxByOrNull { it.health } ?: return emptyList()
        val targets = listOf(highestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "HighestHealth (selected: ${highestHealth.name}, HP: ${highestHealth.health}/${highestHealth.maxHealth()})",
            filtered.size
        )

        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (PacketEntity, no living entities)", 0)
            return emptyList()
        }

        val highestHealth = livingCandidates.maxByOrNull { it.health } ?: return emptyList()
        val targets = listOf(highestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "HighestHealth from PacketEntity (selected: ${highestHealth.name}, HP: ${highestHealth.health})",
            filtered.size
        )

        return filtered
    }

    override fun getDescription(): String {
        return "Highest health from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "HighestHealthTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}
