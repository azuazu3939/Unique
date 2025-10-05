package com.github.azuazu3939.unique.util

import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.bukkit.event.Event

/**
 * イベントユーティリティ
 *
 * イベント発火の共通処理
 */
object EventUtil {

    /**
     * イベントを発火し、キャンセル状態を返す
     *
     * @param event 発火するイベント
     * @return イベントがキャンセルされた場合はtrue
     */
    inline fun <reified T> callEvent(event: T): Boolean where T : Event, T : Cancellable {
        Bukkit.getPluginManager().callEvent(event)
        return event.isCancelled
    }

    /**
     * キャンセル不可能なイベントを発火
     *
     * @param event 発火するイベント
     */
    fun callEvent(event: Event) {
        Bukkit.getPluginManager().callEvent(event)
    }

    /**
     * イベントを発火し、キャンセルされた場合はnullを返す
     *
     * @param event 発火するイベント
     * @return キャンセルされた場合はnull、それ以外はイベント
     */
    inline fun <reified T> callEventOrNull(event: T): T? where T : Event, T : Cancellable {
        Bukkit.getPluginManager().callEvent(event)
        return if (event.isCancelled) null else event
    }

    /**
     * イベントを発火し、結果に応じてアクションを実行
     *
     * @param event 発火するイベント
     * @param onSuccess キャンセルされなかった場合に実行
     * @param onCancel キャンセルされた場合に実行（オプション）
     */
    inline fun <reified T> callEventAndRun(
        event: T,
        onSuccess: (T) -> Unit,
        onCancel: ((T) -> Unit)
    ) where T : Event, T : Cancellable {
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) {
            onCancel.invoke(event)
        } else {
            onSuccess(event)
        }
    }
}
