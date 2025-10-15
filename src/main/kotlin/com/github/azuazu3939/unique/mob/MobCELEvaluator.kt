package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location

/**
 * Mob用のCEL評価ヘルパー
 */
object MobCELEvaluator {

    /**
     * Mobスポーン時のCELコンテキストを構築
     * 注：この関数は既にregion dispatcherのコンテキスト内で呼ばれることを前提とする
     * 最適化：getNearbyEntitiesの代わりにworld.playersを使用
     */
    fun buildMobSpawnContext(plugin: Unique, location: Location): Map<String, Any> {
        val world = location.world ?: return emptyMap()

        // 最適化：getNearbyEntitiesは重いので、world.playersから距離チェック
        val searchRange = plugin.configManager.mainConfig.performance.contextSearchRange
        val searchRangeSquared = searchRange * searchRange
        val nearbyPlayers = world.players
            .filter { it.location.world == world && it.location.distanceSquared(location) <= searchRangeSquared }

        val baseContext = buildMap {
            put("world", CELVariableProvider.buildWorldInfo(world))
            put("location", CELVariableProvider.buildLocationInfo(location))
            put("nearbyPlayers", mapOf(
                "count" to nearbyPlayers.size,
                "maxLevel" to (nearbyPlayers.maxOfOrNull { it.level } ?: 0),
                "minLevel" to (nearbyPlayers.minOfOrNull { it.level } ?: 0),
                "avgLevel" to (nearbyPlayers.map { it.level }.average().takeIf { !it.isNaN() } ?: 0.0)
            ))
            put("nearbyPlayerCount", nearbyPlayers.size)
        }

        return CELVariableProvider.buildFullContext(baseContext)
    }

    /**
     * Health値を評価（CEL式対応）
     */
    fun evaluateHealth(plugin: Unique, healthExpression: String?, context: Map<String, Any>): Double {
        if (healthExpression == null || healthExpression.equals("null", ignoreCase = true)) return 20.0

        return try {
            // 固定値ならそのまま返す
            healthExpression.toDoubleOrNull() ?: run {
                // CEL式として評価
                plugin.celEvaluator.evaluateNumber(healthExpression, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate health: $healthExpression", e)
            20.0
        }
    }

    /**
     * Damage値を評価（CEL式対応）
     */
    fun evaluateDamage(plugin: Unique, damageExpression: String?, context: Map<String, Any>): Double {
        if (damageExpression == null || damageExpression.equals("null", ignoreCase = true)) return 5.0

        return try {
            damageExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(damageExpression, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage: $damageExpression", e)
            5.0
        }
    }

    /**
     * Armor値を評価（CEL式対応）
     */
    fun evaluateArmor(plugin: Unique, armorExpression: String?, context: Map<String, Any>): Double {
        if (armorExpression == null || armorExpression.equals("null", ignoreCase = true)) return 0.0

        return try {
            armorExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(armorExpression, context).coerceIn(0.0, 30.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate armor: $armorExpression", e)
            0.0
        }
    }

    /**
     * ArmorToughness値を評価（CEL式対応）
     */
    fun evaluateArmorToughness(plugin: Unique, armorToughnessExpression: String?, context: Map<String, Any>): Double {
        if (armorToughnessExpression == null || armorToughnessExpression.equals("null", ignoreCase = true)) return 0.0

        return try {
            armorToughnessExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(armorToughnessExpression, context).coerceIn(0.0, 20.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate armor toughness: $armorToughnessExpression", e)
            0.0
        }
    }
}
