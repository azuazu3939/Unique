package com.github.azuazu3939.unique.targeter.types.sorting

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.maxHealth
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * LowestHealthTargeter - 最もHPが低いターゲットを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もHPが低いエンティティを選択します。
 * ヒーラーAIやフィニッシャー攻撃などに有用です。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 *
 * 使用例:
 * ```yaml
 * targeter:
 *   type: LowestHealth
 *   baseTargeter:
 *     type: RadiusPlayers
 *     range: 20.0
 * ```
 */
class LowestHealthTargeter(
    id: String = "lowest_health",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (no candidates)", 0)
            return emptyList()
        }

        // LivingEntityのみを対象にし、最もHPが低いものを選択
        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (no living entities)", 0)
            return emptyList()
        }

        val lowestHealth = livingCandidates.minByOrNull { it.health } ?: return emptyList()
        val targets = listOf(lowestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "LowestHealth (selected: ${lowestHealth.name}, HP: ${lowestHealth.health}/${lowestHealth.maxHealth()})",
            filtered.size
        )

        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (PacketEntity, no living entities)", 0)
            return emptyList()
        }

        val lowestHealth = livingCandidates.minByOrNull { it.health } ?: return emptyList()
        val targets = listOf(lowestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "LowestHealth from PacketEntity (selected: ${lowestHealth.name}, HP: ${lowestHealth.health})",
            filtered.size
        )

        return filtered
    }

    override fun getDescription(): String {
        return "Lowest health from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "LowestHealthTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}
