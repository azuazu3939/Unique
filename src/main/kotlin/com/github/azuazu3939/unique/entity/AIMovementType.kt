package com.github.azuazu3939.unique.entity

/**
 * AI移動タイプ
 *
 * Mobの移動方法を定義
 */
enum class AIMovementType {
    /**
     * 地上移動（デフォルト）
     * 地面を歩いて移動し、障害物を登る
     */
    GROUND,

    /**
     * 飛行移動
     * 空中を自由に移動し、重力の影響を受けない
     */
    FLYING,

    /**
     * ジャンプ移動
     * 地上を移動するが、障害物に対してジャンプで飛び越える
     */
    JUMPING,

    /**
     * テレポート移動
     * 瞬間移動でターゲットに近づく
     */
    TELEPORT,

    /**
     * 固定
     * 移動しない（その場に留まる）
     */
    STATIONARY,

    /**
     * カスタム
     * YMLで定義されたカスタム移動パターンを使用
     */
    CUSTOM;

    companion object {
        /**
         * デフォルトの移動タイプ
         */
        val DEFAULT = GROUND

        /**
         * 文字列からAIMovementTypeを取得
         */
        fun fromString(name: String?): AIMovementType {
            if (name == null) return DEFAULT
            return try {
                valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}
