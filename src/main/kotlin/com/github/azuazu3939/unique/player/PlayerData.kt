package com.github.azuazu3939.unique.player

import java.util.*

/**
 * プレイヤーデータ
 *
 * マナ、スタミナなどのカスタムデータを保持
 */
class PlayerData(
    val uuid: UUID,
    val name: String
) {
    /**
     * マナ（MP）
     */
    var mana: Double = 100.0
        private set

    /**
     * 最大マナ
     */
    var maxMana: Double = 100.0

    /**
     * マナ自然回復速度（tick毎）
     */
    var manaRegenRate: Double = 0.1

    /**
     * 最後にマナが回復したtick
     */
    private var lastManaRegenTick: Long = 0

    /**
     * カスタムデータストレージ（拡張用）
     */
    private val customData = mutableMapOf<String, Any>()

    /**
     * マナを消費
     *
     * @param amount 消費量
     * @return 消費に成功した場合true
     */
    fun consumeMana(amount: Double): Boolean {
        if (mana < amount) return false
        mana = (mana - amount).coerceAtLeast(0.0)
        return true
    }

    /**
     * マナを回復
     *
     * @param amount 回復量
     */
    fun restoreMana(amount: Double) {
        mana = (mana + amount).coerceAtMost(maxMana)
    }

    /**
     * マナを完全回復
     */
    fun fillMana() {
        mana = maxMana
    }

    /**
     * マナが足りているかチェック
     *
     * @param amount 必要量
     * @return 足りている場合true
     */
    fun hasMana(amount: Double): Boolean {
        return mana >= amount
    }

    /**
     * マナの割合を取得（0.0 - 1.0）
     */
    fun getManaRatio(): Double {
        return if (maxMana > 0) mana / maxMana else 0.0
    }

    /**
     * 自然回復処理
     *
     * @param currentTick 現在のtick
     */
    fun tickManaRegen(currentTick: Long) {
        if (currentTick - lastManaRegenTick >= 20) { // 1秒ごと
            restoreMana(manaRegenRate * 20)
            lastManaRegenTick = currentTick
        }
    }

    /**
     * カスタムデータを設定
     */
    fun setCustomData(key: String, value: Any) {
        customData[key] = value
    }

    /**
     * カスタムデータを取得
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCustomData(key: String): T? {
        return customData[key] as? T
    }

    /**
     * カスタムデータを削除
     */
    fun removeCustomData(key: String) {
        customData.remove(key)
    }

    /**
     * カスタムデータが存在するか
     */
    fun hasCustomData(key: String): Boolean {
        return customData.containsKey(key)
    }
}
