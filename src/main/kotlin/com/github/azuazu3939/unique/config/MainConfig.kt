package com.github.azuazu3939.unique.config

/**
 * メイン設定
 *
 * config.ymlに対応するデータクラス
 * Hopliteがデフォルト値を自動的に使用
 */
data class MainConfig(
    val debug: DebugConfig = DebugConfig(),
    val performance: PerformanceConfig = PerformanceConfig(),
    val spawn: SpawnConfig = SpawnConfig(),
    val cel: CelConfig = CelConfig(),
    val damage: DamageConfig = DamageConfig(),
    val resources: ResourceConfig = ResourceConfig(),
    val experimental: ExperimentalConfig = ExperimentalConfig()
)

/**
 * デバッグ設定
 */
data class DebugConfig(
    val enabled: Boolean = true,
    val verbose: Boolean = false,
    val logSkillExecution: Boolean = true,
    val logCelEvaluation: Boolean = false,
    val logTimings: Boolean = false
)

/**
 * パフォーマンス設定
 */
data class PerformanceConfig(
    val asyncByDefault: Boolean = true,
    val packetEntityUpdateInterval: Int = 1,
    val maxConcurrentSkills: Int = 100,
    val viewDistance: Double = 64.0,  // パケットエンティティの可視距離
    val autoSpawnOnJoin: Boolean = true,  // プレイヤー参加時の自動スポーン
    val aiTickInterval: Int = 1,  // MobのAI更新間隔（tick）
    val deadEntityCleanupTicks: Long = 40L,  // 死亡エンティティクリーンアップまでの時間（死亡アニメーション表示時間も兼ねる）
    val batchCleanupDelayMs: Long = 100L,  // バッチクリーンアップの遅延（ミリ秒）
    val skipAiWhenNoViewers: Boolean = true,  // 観察者がいない場合にAIをスキップ
    val damageMemoryTicks: Long = 200L,  // Mobがダメージを記憶する時間（tick）
    val contextSearchRange: Double = 50.0  // CELコンテキスト構築時の検索範囲
)

/**
 * スポーン設定
 */
data class SpawnConfig(
    val enabled: Boolean = true,
    val checkInterval: Int = 20,
    val maxTotalSpawns: Int = 200
)

/**
 * CEL設定
 */
data class CelConfig(
    val cacheSize: Int = 1000,
    val evaluationTimeout: Long = 100L
)

/**
 * ダメージ計算設定
 */
data class DamageConfig(
    /**
     * デフォルトのダメージ計算式（CEL式）
     * 個々のMobでdamageFormulaが指定されていない場合に使用される
     *
     * 例: "damage * (1 - min(20, armor) / 25)"  # Minecraft標準式
     *     "damage * (1 - armor / 100)"          # パーセント軽減
     *     null                                  # デフォルトの計算式を使用
     */
    val defaultFormula: String? = null
)

/**
 * リソースキー設定
 */
data class ResourceConfig(
    /**
     * カスタムリソースキーの名前空間
     *
     * 例: customNamespace = "myserver"
     *     → "myserver:custom_sound" を探し、なければ "minecraft:custom_sound"
     */
    val customNamespace: String = "custom",

    /**
     * リソースが見つからない場合にエラーログを出力するか
     */
    val logMissingResources: Boolean = false,

    /**
     * カスタムリソースキー機能を有効にするか
     */
    val enableCustomKeys: Boolean = true
)

/**
 * 実験的機能設定
 */
data class ExperimentalConfig(
    val enableNewFeatures: Boolean = false
)