package com.example.myplugin

import com.github.azuazu3939.unique.event.UniqueReloadAfterEvent
import com.github.azuazu3939.unique.event.UniqueReloadBeforeEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

/**
 * UniqueReloadEventの使用例
 *
 * このクラスは、Uniqueプラグインのリロードイベントをリスンする例です。
 * 他のプラグインでUniqueのリロードを検知し、連携処理を行うことができます。
 */
class UniqueReloadListener : Listener {

    /**
     * 例1: リロード前の検証
     *
     * リロード前に条件をチェックし、必要に応じてリロードをキャンセルできます。
     */
    @EventHandler
    fun onReloadBefore(event: UniqueReloadBeforeEvent) {
        // 例: 戦闘中のプレイヤーがいる場合、リロードを中止
        val inCombat = checkPlayersInCombat()

        if (inCombat) {
            event.isCancelled = true
            event.cancelReason = "プレイヤーが戦闘中のため、リロードできません"
            event.sender.sendMessage("§c[Warning] §7Players are in combat. Reload cancelled.")
            return
        }

        // 例: リロード前の処理
        event.sender.sendMessage("§e[Info] §7Preparing for Unique reload...")

        // データベース接続チェック
        if (!checkDatabaseConnection()) {
            event.isCancelled = true
            event.cancelReason = "データベース接続が失われています"
        }
    }

    /**
     * 例2: リロード後の処理
     *
     * リロード完了後に、連携データを更新したり、通知を送信したりできます。
     */
    @EventHandler
    fun onReloadAfter(event: UniqueReloadAfterEvent) {
        if (event.success) {
            // リロード成功時の処理
            event.sender.sendMessage("§a[Success] §7Unique reloaded successfully!")
            event.sender.sendMessage("§7  - Mobs: §e${event.mobCount}")
            event.sender.sendMessage("§7  - Spawns: §e${event.spawnCount}")
            event.sender.sendMessage("§7  - Duration: §e${event.duration}ms")

            // 他のプラグインのキャッシュをクリア
            clearMyPluginCache()

            // 連携データを再読み込み
            reloadIntegrationData()

            // 全プレイヤーに通知
            notifyAllPlayers("Uniqueがリロードされました")

        } else {
            // リロード失敗時の処理
            event.sender.sendMessage("§c[Error] §7Unique reload failed!")
            event.sender.sendMessage("§7  - Error: §c${event.errorMessage}")

            // エラーログを記録
            logReloadError(event.errorMessage)

            // 管理者に通知
            notifyAdmins("Uniqueのリロードに失敗しました: ${event.errorMessage}")
        }
    }

    /**
     * 例3: リロード統計の記録
     *
     * リロード情報を記録して、パフォーマンス分析に使用します。
     */
    @EventHandler
    fun onReloadStatistics(event: UniqueReloadAfterEvent) {
        // 統計情報を記録
        val stats = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "sender" to event.sender.name,
            "success" to event.success,
            "mobCount" to event.mobCount,
            "spawnCount" to event.spawnCount,
            "duration" to event.duration
        )

        // データベースに保存
        saveReloadStatistics(stats)

        // パフォーマンスアラート
        if (event.duration > 5000) {
            event.sender.sendMessage("§e[Warning] §7Reload took ${event.duration}ms (>5s)")
        }
    }

    /**
     * 例4: リロード前のバックアップ
     *
     * リロード前に設定をバックアップします。
     */
    @EventHandler
    fun onReloadBackup(event: UniqueReloadBeforeEvent) {
        try {
            // 現在の設定をバックアップ
            backupCurrentConfiguration()

            event.sender.sendMessage("§a[Backup] §7Configuration backed up successfully")

        } catch (e: Exception) {
            event.sender.sendMessage("§c[Error] §7Failed to backup configuration: ${e.message}")

            // バックアップ失敗時はリロードを中止
            event.isCancelled = true
            event.cancelReason = "設定のバックアップに失敗しました"
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    private fun checkPlayersInCombat(): Boolean {
        // 実装例: 戦闘中のプレイヤーをチェック
        return false
    }

    private fun checkDatabaseConnection(): Boolean {
        // 実装例: データベース接続チェック
        return true
    }

    private fun clearMyPluginCache() {
        // 実装例: キャッシュクリア
    }

    private fun reloadIntegrationData() {
        // 実装例: 連携データ再読み込み
    }

    private fun notifyAllPlayers(message: String) {
        // 実装例: 全プレイヤーに通知
    }

    private fun notifyAdmins(message: String) {
        // 実装例: 管理者に通知
    }

    private fun logReloadError(error: String?) {
        // 実装例: エラーログ記録
    }

    private fun saveReloadStatistics(stats: Map<String, Any>) {
        // 実装例: 統計情報保存
    }

    private fun backupCurrentConfiguration() {
        // 実装例: 設定バックアップ
    }
}

/**
 * プラグインメインクラスでのリスナー登録例
 */
class MyPlugin : JavaPlugin() {

    override fun onEnable() {
        // UniqueReloadListenerを登録
        server.pluginManager.registerEvents(UniqueReloadListener(), this)

        logger.info("MyPlugin enabled with Unique integration")
    }
}
