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
    val viewDistance: Double = 50.0,  // パケットエンティティの可視距離
    val autoSpawnOnJoin: Boolean = true  // プレイヤー参加時の自動スポーン
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
 * 実験的機能設定
 */
data class ExperimentalConfig(
    val enableNewFeatures: Boolean = false
)