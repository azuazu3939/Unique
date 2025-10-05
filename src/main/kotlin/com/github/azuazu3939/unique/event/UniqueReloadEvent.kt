package com.github.azuazu3939.unique.event

import org.bukkit.command.CommandSender
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Uniqueプラグインのリロードイベント基底クラス
 */
abstract class UniqueReloadEvent(
    val sender: CommandSender
) : Event() {

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }

    override fun getHandlers(): HandlerList {
        return UniqueReloadEvent.handlers
    }
}

/**
 * リロード前イベント
 *
 * プラグインのリロードが開始される直前に発火されます。
 * このイベントをキャンセルすることで、リロードを中止できます。
 *
 * 使用例：
 * ```kotlin
 * @EventHandler
 * fun onReloadBefore(event: UniqueReloadBeforeEvent) {
 *     // リロード前の処理
 *     if (someCondition) {
 *         event.isCancelled = true
 *         event.sender.sendMessage("リロードは現在実行できません")
 *     }
 * }
 * ```
 */
class UniqueReloadBeforeEvent(
    sender: CommandSender
) : UniqueReloadEvent(sender) {

    /**
     * リロードをキャンセルするフラグ
     */
    var isCancelled: Boolean = false

    /**
     * キャンセル理由（オプション）
     */
    var cancelReason: String? = null
}

/**
 * リロード後イベント
 *
 * プラグインのリロードが完了した直後に発火されます。
 * リロードが成功したか、失敗したかを判定できます。
 *
 * 使用例：
 * ```kotlin
 * @EventHandler
 * fun onReloadAfter(event: UniqueReloadAfterEvent) {
 *     if (event.success) {
 *         // リロード成功時の処理
 *         event.sender.sendMessage("リロードが完了しました")
 *     } else {
 *         // リロード失敗時の処理
 *         event.sender.sendMessage("リロードに失敗しました: ${event.errorMessage}")
 *     }
 * }
 * ```
 */
class UniqueReloadAfterEvent(
    sender: CommandSender,
    val success: Boolean,
    val errorMessage: String? = null
) : UniqueReloadEvent(sender) {

    /**
     * リロードされたMob定義数
     */
    var mobCount: Int = 0

    /**
     * リロードされたスポーン定義数
     */
    var spawnCount: Int = 0

    /**
     * リロード所要時間（ミリ秒）
     */
    var duration: Long = 0
}
