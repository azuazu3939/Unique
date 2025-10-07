package com.github.azuazu3939.unique.entity

/**
 * AI行動タイプ
 *
 * Mobの基本的な行動パターンを定義
 */
enum class AIBehaviorType {
    /**
     * 攻撃的（デフォルト）
     * 近くのプレイヤーを自動的に追跡して攻撃する
     */
    AGGRESSIVE,

    /**
     * 受動的
     * 何もせず、その場に留まる
     */
    PASSIVE,

    /**
     * 防御的
     * 攻撃されたときのみ反撃する
     */
    DEFENSIVE,

    /**
     * 徘徊
     * ランダムに歩き回るが、攻撃はしない
     */
    WANDER,

    /**
     * カスタム
     * YMLで定義されたカスタムAI行動を使用
     */
    CUSTOM;

    companion object {
        /**
         * デフォルトの行動タイプ
         */
        val DEFAULT = AGGRESSIVE

        /**
         * 文字列からAIBehaviorTypeを取得
         */
        fun fromString(name: String?): AIBehaviorType {
            if (name == null) return DEFAULT
            return try {
                valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}
