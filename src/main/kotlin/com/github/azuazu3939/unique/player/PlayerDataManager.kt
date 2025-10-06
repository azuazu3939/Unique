package com.github.azuazu3939.unique.player

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * プレイヤーデータマネージャー
 *
 * プレイヤーごとのカスタムデータ（マナなど）を管理
 */
class PlayerDataManager(private val plugin: Unique) {

    /**
     * プレイヤーデータキャッシュ
     */
    private val playerDataCache = ConcurrentHashMap<UUID, PlayerData>()

    /**
     * プレイヤーデータを取得（存在しない場合は作成）
     *
     * @param player プレイヤー
     * @return プレイヤーデータ
     */
    fun getPlayerData(player: Player): PlayerData {
        return playerDataCache.computeIfAbsent(player.uniqueId) {
            PlayerData(player.uniqueId, player.name).also {
                DebugLogger.debug("Created PlayerData for ${player.name}")
            }
        }
    }

    /**
     * プレイヤーデータを取得（UUID）
     *
     * @param uuid プレイヤーUUID
     * @return プレイヤーデータ（存在しない場合null）
     */
    fun getPlayerData(uuid: UUID): PlayerData? {
        return playerDataCache[uuid]
    }

    /**
     * プレイヤーデータを削除
     *
     * @param player プレイヤー
     */
    fun removePlayerData(player: Player) {
        playerDataCache.remove(player.uniqueId)?.let {
            DebugLogger.debug("Removed PlayerData for ${player.name}")
        }
    }

    /**
     * 全プレイヤーデータを更新（マナ自然回復など）
     */
    fun tickAllPlayers() {
        val currentTick = plugin.server.currentTick
        playerDataCache.values.forEach { data ->
            data.tickManaRegen(currentTick.toLong())
        }
    }

    /**
     * マナバーを表示（アクションバー）
     *
     * @param player プレイヤー
     */
    fun displayManaBar(player: Player) {
        //val data = getPlayerData(player)
        //val manaBar = buildManaBar(data)
        //player.sendActionBar(manaBar)
    }

    /**
     * マナバーを生成
     */
    private fun buildManaBar(data: PlayerData): String {
        val ratio = data.getManaRatio()
        val barLength = 20
        val filledBars = (barLength * ratio).toInt()
        val emptyBars = barLength - filledBars

        val bar = "§b" + "█".repeat(filledBars) + "§7" + "█".repeat(emptyBars)
        return "§bMP: $bar §f${data.mana.toInt()}/${data.maxMana.toInt()}"
    }

    /**
     * 全プレイヤーのマナバーを更新
     */
    fun updateAllManaBars() {
        plugin.server.onlinePlayers.forEach { player ->
            if (playerDataCache.containsKey(player.uniqueId)) {
                displayManaBar(player)
            }
        }
    }

    /**
     * シャットダウン処理
     */
    fun shutdown() {
        // TODO: 永続化が必要な場合はここでセーブ
        playerDataCache.clear()
        DebugLogger.info("PlayerDataManager shut down")
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("PlayerData Debug Info")
        DebugLogger.info("Total players: ${playerDataCache.size}")
        playerDataCache.values.forEach { data ->
            DebugLogger.info("  ${data.name}: MP=${data.mana.toInt()}/${data.maxMana.toInt()}")
        }
        DebugLogger.separator()
    }
}
