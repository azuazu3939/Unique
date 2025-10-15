package com.github.azuazu3939.unique.targeter.types.advanced

import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * 条件付きターゲッター
 *
 * ベースターゲッターの結果を条件でフィルタリング
 * 複数の条件を組み合わせて高度なターゲット選択が可能
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター
 * @param conditions 適用する条件リスト（AND条件）
 * @deprecated Use filter property on base Targeter instead
 */
// @Deprecated("Use filter property on base Targeter", ReplaceWith("baseTargeter with filter"))
class ConditionalTargeter(
    id: String = "conditional",
    private val baseTargeter: Targeter,
    private val conditions: List<Condition>,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        // ベースターゲッターでターゲットを取得
        val baseTargets = baseTargeter.getTargets(source)

        if (baseTargets.isEmpty() || conditions.isEmpty()) {
            DebugLogger.targeter("Conditional (no filtering)", baseTargets.size)
            return baseTargets
        }

        // 全ての条件を満たすターゲットのみを抽出
        val filtered = baseTargets.filter { target ->
            conditions.all { condition ->
                condition.evaluate(target)
            }
        }

        // さらに自身のフィルターでフィルタリング
        val finalFiltered = filterByFilter(source, filtered)

        DebugLogger.targeter(
            "Conditional(base=${baseTargeter.id}, conditions=${conditions.size})",
            "${baseTargets.size} -> ${filtered.size} -> ${finalFiltered.size}"
        )
        return finalFiltered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // ベースターゲッターでターゲットを取得
        val baseTargets = baseTargeter.getTargets(source)

        if (baseTargets.isEmpty() || conditions.isEmpty()) {
            DebugLogger.targeter("Conditional (PacketEntity, no filtering)", baseTargets.size)
            return baseTargets
        }

        // 全ての条件を満たすターゲットのみを抽出
        val filtered = baseTargets.filter { target ->
            conditions.all { condition ->
                condition.evaluate(target)
            }
        }

        // さらに自身のフィルターでフィルタリング
        val finalFiltered = filterByFilter(source, filtered)

        DebugLogger.targeter(
            "Conditional(PacketEntity, base=${baseTargeter.id}, conditions=${conditions.size})",
            baseTargets.size
        )
        return finalFiltered
    }

    override fun getDescription(): String {
        return "Conditional targeter (base: ${baseTargeter.getDescription()}, ${conditions.size} conditions)"
    }

    override fun debugInfo(): String {
        val conditionInfo = conditions.joinToString(", ") { it.debugInfo() }
        return "ConditionalTargeter[base=${baseTargeter.debugInfo()}, conditions=[$conditionInfo]]"
    }
}
