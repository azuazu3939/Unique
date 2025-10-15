package com.github.azuazu3939.unique.targeter.types.advanced

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * チェーンターゲッター
 *
 * 初期ターゲットから連鎖的にターゲットを拡散
 * 稲妻攻撃などに使用
 *
 * @param id ターゲッターID
 * @param initialTargeter 初期ターゲッター
 * @param maxChains 最大連鎖数（CEL式対応）
 * @param chainRange 連鎖範囲（CEL式対応）
 * @param chainCondition 連鎖条件（CEL式、chainIndex変数が利用可能）
 */
class ChainTargeter(
    id: String = "chain",
    private val initialTargeter: Targeter,
    private val maxChains: String = "5",
    private val chainRange: String = "5.0",
    private val chainCondition: String? = null,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val evaluator = Unique.instance.celEvaluator
        val baseContext = CELVariableProvider.buildEntityContext(source)

        // CEL式を評価
        val maxChainsValue = evaluateCelInt(maxChains, baseContext, evaluator, 5)
        val chainRangeValue = evaluateCelDouble(chainRange, baseContext, evaluator, 5.0)

        // 初期ターゲットを取得
        val initialTargets = initialTargeter.getTargets(source)
        if (initialTargets.isEmpty()) {
            DebugLogger.targeter("Chain (no initial target)", 0)
            return emptyList()
        }

        // 連鎖処理を実行
        val allTargets = performChainTargeting(
            initialTargets = initialTargets,
            maxChains = maxChainsValue,
            chainRange = chainRangeValue,
            baseContext = baseContext,
            evaluator = evaluator,
            excludeEntity = source
        )

        val filtered = filterByFilter(source, allTargets)

        DebugLogger.targeter(
            "Chain(max=$maxChainsValue, range=$chainRangeValue)",
            "${allTargets.size} targets (${filtered.size} after filter)"
        )
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val evaluator = Unique.instance.celEvaluator
        val baseContext = CELVariableProvider.buildPacketEntityContext(source)

        // CEL式を評価
        val maxChainsValue = evaluateCelInt(maxChains, baseContext, evaluator, 5)
        val chainRangeValue = evaluateCelDouble(chainRange, baseContext, evaluator, 5.0)

        // 初期ターゲットを取得
        val initialTargets = initialTargeter.getTargets(source)
        if (initialTargets.isEmpty()) {
            DebugLogger.targeter("Chain (PacketEntity, no initial target)", 0)
            return emptyList()
        }

        // 連鎖処理を実行（PacketEntityは実体がないため除外なし）
        val allTargets = performChainTargeting(
            initialTargets = initialTargets,
            maxChains = maxChainsValue,
            chainRange = chainRangeValue,
            baseContext = baseContext,
            evaluator = evaluator,
            excludeEntity = null
        )

        val filtered = filterByFilter(source, allTargets)

        DebugLogger.targeter(
            "Chain(PacketEntity, max=$maxChainsValue, range=$chainRangeValue)",
            "${allTargets.size} targets (${filtered.size} after filter)"
        )
        return filtered
    }

    /**
     * 連鎖ターゲティングを実行
     */
    private fun performChainTargeting(
        initialTargets: List<Entity>,
        maxChains: Int,
        chainRange: Double,
        baseContext: Map<String, Any>,
        evaluator: CELEvaluator,
        excludeEntity: Entity?
    ): List<Entity> {
        val allTargets = mutableListOf<Entity>()
        val processed = mutableSetOf<Entity>()

        // 最初のターゲット
        var currentTarget = initialTargets.first()
        allTargets.add(currentTarget)
        processed.add(currentTarget)

        // 連鎖処理
        repeat(maxChains - 1) { index ->
            val chainIndex = index + 1

            // 現在のターゲットから範囲内のエンティティを取得
            val nearbyEntities = currentTarget.world.getNearbyEntitiesAsync(
                currentTarget.location,
                chainRange,
                chainRange,
                chainRange
            ).filterIsInstance<LivingEntity>()
                .filter {
                    it.isValid && !it.isDead && it !in processed &&
                    (excludeEntity == null || it != excludeEntity)
                }

            if (nearbyEntities.isEmpty()) {
                return@repeat
            }

            // 連鎖条件でフィルタリング
            val candidates = if (chainCondition != null) {
                nearbyEntities.filter { candidate ->
                    val context = baseContext.toMutableMap()
                    context["target"] = CELVariableProvider.buildEntityInfo(candidate)
                    context["chainIndex"] = chainIndex
                    context["currentTarget"] = CELVariableProvider.buildEntityInfo(currentTarget)

                    try {
                        evaluator.evaluateBoolean(chainCondition, context)
                    } catch (e: Exception) {
                        DebugLogger.error("Chain condition evaluation failed", e)
                        false
                    }
                }
            } else {
                nearbyEntities
            }

            if (candidates.isEmpty()) {
                return@repeat
            }

            // 最も近いターゲットを選択
            val nextTarget = candidates.minByOrNull {
                it.location.distanceSquared(currentTarget.location)
            } ?: return@repeat

            allTargets.add(nextTarget)
            processed.add(nextTarget)
            currentTarget = nextTarget
        }

        return allTargets
    }

    /**
     * CEL式を評価してInt値を取得
     */
    private fun evaluateCelInt(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Int
    ): Int {
        return try {
            expression.toIntOrNull() ?: run {
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toInt()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    /**
     * CEL式を評価してDouble値を取得
     */
    private fun evaluateCelDouble(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Double
    ): Double {
        return try {
            expression.toDoubleOrNull() ?: run {
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toDouble()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    override fun getDescription(): String {
        return "Chain targeter (initial: ${initialTargeter.getDescription()}, max chains: $maxChains, range: $chainRange)"
    }

    override fun debugInfo(): String {
        return "ChainTargeter[initial=${initialTargeter.debugInfo()}, maxChains=$maxChains, chainRange=$chainRange, condition=${chainCondition ?: "none"}]"
    }
}
