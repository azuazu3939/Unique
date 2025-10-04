package com.github.azuazu3939.unique.spawn

/**
 * スポーン定義
 *
 * YAMLから読み込まれるスポーン設定
 */
data class SpawnDefinition(
    // スポーン対象Mob
    val mob: String,

    // スポーン条件（CEL式）
    val conditions: List<String> = listOf("true"),

    // スポーン設定
    val spawnRate: Int = 20,  // tick
    val maxNearby: Int = 5,
    val chunkRadius: Int = 3,

    // スポーン範囲
    val region: SpawnRegion? = null,

    // 固定スポーン位置
    val location: SpawnLocation? = null,

    // 高度な条件
    val advancedConditions: AdvancedConditions = AdvancedConditions(),

    // スポーン時のスキル
    val onSpawn: List<SpawnSkillReference> = emptyList()
)

/**
 * スポーン範囲定義
 */
data class SpawnRegion(
    val type: String,  // circle, box
    val center: RegionCenter? = null,
    val radius: Double? = null,
    val min: RegionPoint? = null,
    val max: RegionPoint? = null
)

/**
 * 範囲中心
 */
data class RegionCenter(
    val x: Double = 0.0,
    val z: Double = 0.0
)

/**
 * 範囲座標
 */
data class RegionPoint(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
)

/**
 * スポーン位置
 */
data class SpawnLocation(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f
)

/**
 * 高度な条件
 */
data class AdvancedConditions(
    val moonPhase: String? = null,  // FULL_MOON, NEW_MOON, etc.
    val weatherRequired: String? = null,  // CLEAR, RAIN, THUNDER
    val playerLevelMin: Int? = null,
    val playerLevelMax: Int? = null,
    val nearbyPlayerCount: Int? = null
)

/**
 * スポーン時スキル参照
 */
data class SpawnSkillReference(
    val skill: String,
    val meta: SpawnSkillMeta = SpawnSkillMeta()
)

/**
 * スポーン時スキルメタ
 */
data class SpawnSkillMeta(
    val executeDelay: String = "0ms",
    val effectDelay: String = "0ms",
    val sync: Boolean = false
)