package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.nms.distanceToAsync
import com.github.azuazu3939.unique.nms.getPlayersAsync
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.asyncDispatcher
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Mobドロップの計算と処理
 */
class MobDropHandler(private val plugin: Unique) {

    /**
     * ドロップアイテムを計算（リストを返すのみ）
     *
     * @param definition Mob定義
     * @param killer キラー（プレイヤー）
     * @param location ドロップ位置
     * @return ドロップアイテムのリスト
     */
    fun calculateDropItems(
        definition: MobDefinition,
        killer: Player,
        location: Location
    ): List<ItemStack> {
        if (definition.drops.isEmpty()) return emptyList()

        val drops = mutableListOf<ItemStack>()
        val context = buildDropContext(killer, location)

        // 定義からのドロップ処理
        definition.drops.forEach { drop ->
            try {
                // 条件＆確率チェック
                if (!shouldDrop(drop, context)) return@forEach

                // アイテムスタック作成＆追加
                createDropItem(drop, context)?.let { drops.add(it) }

            } catch (e: Exception) {
                DebugLogger.error("Failed to calculate drop: ${drop.item}", e)
            }
        }

        return drops
    }

    /**
     * アイテムをワールドにドロップする
     * 注：この関数は既にregion dispatcherのコンテキスト内で呼ばれることを前提とする
     *
     * @param location ドロップ位置
     * @param items ドロップするアイテムのリスト
     */
    suspend fun dropItemsInWorld(
        location: Location,
        items: List<ItemStack>
    ) {
        if (items.isEmpty()) return

        val world = location.world ?: return

        // 既にregion dispatcherのコンテキスト内にいるので、直接dropItemNaturallyを呼ぶ
        items.forEach { itemStack ->
            world.dropItemNaturally(location, itemStack)
            DebugLogger.debug("Dropped ${itemStack.amount}x ${itemStack.type} at $location")
        }
    }

    /**
     * ドロップ処理（計算 + ワールドにドロップ）
     *
     * @param definition Mob定義
     * @param location ドロップ位置
     * @param killer キラー（プレイヤー）
     * @param eventDrops イベントで追加されたドロップアイテム
     */
    suspend fun processDrops(
        definition: MobDefinition,
        location: Location,
        killer: Player,
        eventDrops: MutableList<ItemStack>
    ) {
        withContext(plugin.asyncDispatcher) {
            val calculatedDrops = calculateDropItems(definition, killer, location)
            eventDrops.addAll(calculatedDrops)
        }

        // worldにドロップアイテムをspawnさせるためここはリージョンスケジューラを使用しなくてはならない
        withContext(plugin.regionDispatcher(location)) {
            // ワールドにドロップ
            dropItemsInWorld(location, eventDrops)
        }
    }

    /**
     * ドロップするかどうかを判定
     */
    private fun shouldDrop(drop: DropDefinition, context: Map<String, Any>): Boolean {
        // 条件チェック
        if (drop.condition != "true") {
            val conditionMet = plugin.celEngine.evaluateBoolean(drop.condition, context)
            if (!conditionMet) {
                DebugLogger.verbose("Drop condition not met: ${drop.item}")
                return false
            }
        }

        // 確率チェック（CEL評価）
        val chanceValue = evaluateDropChance(drop.chance, context)
        if (Math.random() > chanceValue) {
            DebugLogger.verbose("Drop chance failed: ${drop.item} (${chanceValue})")
            return false
        }

        return true
    }

    /**
     * ドロップアイテムを作成
     */
    private fun createDropItem(drop: DropDefinition, context: Map<String, Any>): ItemStack? {
        val material = Material.getMaterial(drop.item.uppercase())
        if (material == null) {
            DebugLogger.error("Invalid drop material: ${drop.item}")
            return null
        }

        val amount = evaluateDropAmount(drop.amount, context)
        DebugLogger.debug("Added drop: ${amount}x ${drop.item}")
        return ItemStack(material, amount)
    }

    /**
     * ドロップコンテキストを構築
     */
    private fun buildDropContext(killer: Player, location: Location): Map<String, Any> {
        val world = location.world
        val searchRange = plugin.configManager.mainConfig.performance.contextSearchRange
        val searchRangeSquared = searchRange * searchRange
        val nearbyPlayers = world?.getPlayersAsync()
            ?.filter { it.world.name == world.name && it.distanceToAsync(location) <= searchRangeSquared }
            ?: emptyList()

        val baseContext = buildMap {
            put("killer", CELVariableProvider.buildEntityInfo(killer))
            location.world?.let { put("world", CELVariableProvider.buildWorldInfo(it)) }
            put("nearbyPlayers", mapOf(
                "count" to nearbyPlayers.size,
                "maxLevel" to (nearbyPlayers.maxOfOrNull { it.level } ?: 0),
                "minLevel" to (nearbyPlayers.minOfOrNull { it.level } ?: 0),
                "avgLevel" to (nearbyPlayers.map { it.level }.average().takeIf { !it.isNaN() } ?: 0.0)
            ))
        }
        return CELVariableProvider.buildFullContext(baseContext)
    }

    /**
     * ドロップ個数を評価（CEL式対応、範囲形式も対応）
     */
    private fun evaluateDropAmount(amountExpression: String, context: Map<String, Any>): Int {
        return try {
            // 範囲形式 "1-3" のチェック
            if (amountExpression.contains("-") && amountExpression.toIntOrNull() == null) {
                val parts = amountExpression.split("-")
                val min = parts[0].trim().toIntOrNull() ?: 1
                val max = parts[1].trim().toIntOrNull() ?: min
                return (min..max).random()
            }

            // 固定値または CEL式
            amountExpression.toIntOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(amountExpression, context).toInt().coerceAtLeast(1)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate drop amount: $amountExpression", e)
            1
        }
    }

    /**
     * ドロップ確率を評価（CEL式対応）
     */
    private fun evaluateDropChance(chanceExpression: String, context: Map<String, Any>): Double {
        return try {
            chanceExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(chanceExpression, context).coerceIn(0.0, 1.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate drop chance: $chanceExpression", e)
            1.0
        }
    }
}
