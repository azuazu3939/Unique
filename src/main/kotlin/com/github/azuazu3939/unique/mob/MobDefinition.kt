package com.github.azuazu3939.unique.mob

import org.bukkit.entity.EntityType

/**
 * Mob定義
 *
 * YAMLから読み込まれるMobの設定
 * Hopliteで型安全に読み込まれる
 */
data class MobDefinition(
    // 基本設定
    val type: String,  // EntityTypeの文字列
    val display: String? = null,
    val health: Double? = null,
    val damage: Double? = null,

    // AI設定
    val ai: MobAI = MobAI(),

    // 外観設定
    val appearance: MobAppearance = MobAppearance(),

    // スキル設定
    val skills: MobSkills = MobSkills(),

    // ドロップ設定
    val drops: List<DropDefinition> = emptyList()
) {
    /**
     * EntityTypeを取得
     */
    fun getEntityType(): EntityType {
        return try {
            EntityType.valueOf(type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid entity type: $type")
        }
    }

    /**
     * 表示名を取得（デフォルト値対応）
     */
    fun getDisplayName(): String {
        return display ?: type
    }

    /**
     * HP を取得（デフォルト値対応）
     */
    fun getHealth(): Double {
        return health ?: 20.0
    }

    /**
     * ダメージを取得（デフォルト値対応）
     */
    fun getDamage(): Double {
        return damage ?: 5.0
    }
}

/**
 * Mob AI設定
 */
data class MobAI(
    val movementSpeed: Double = 0.25,
    val followRange: Double = 16.0,
    val knockbackResistance: Double = 0.0,
    val hasAI: Boolean = true,
    val hasGravity: Boolean = true
)

/**
 * Mob外観設定
 */
data class MobAppearance(
    val customNameVisible: Boolean = true,
    val glowing: Boolean = false,
    val invisible: Boolean = false
)

/**
 * Mobスキル設定
 */
data class MobSkills(
    val onTimer: List<SkillTrigger> = emptyList(),
    val onDamaged: List<SkillTrigger> = emptyList(),
    val onDeath: List<SkillTrigger> = emptyList(),
    val onSpawn: List<SkillTrigger> = emptyList(),
    val onAttack: List<SkillTrigger> = emptyList()
)

/**
 * スキルトリガー
 */
data class SkillTrigger(
    val name: String,
    val interval: Int? = null,  // OnTimer用（tick）
    val condition: String = "true",
    val targeter: TargeterDefinition,
    val meta: SkillMetaDefinition = SkillMetaDefinition(),
    val skills: List<SkillReference>
)

/**
 * ターゲッター定義
 */
data class TargeterDefinition(
    val type: String,
    val range: Double = 16.0,
    val maxDistance: Double = 50.0,
    val count: Int = 1,
    val condition: String = "true"
)

/**
 * スキルメタ定義
 */
data class SkillMetaDefinition(
    val sync: Boolean = false,
    val executeDelay: String = "0ms",
    val effectDelay: String = "0ms",
    val cancelOnDeath: Boolean = true,
    val interruptible: Boolean = false
)

/**
 * スキル参照
 */
data class SkillReference(
    val skill: String,
    val meta: SkillMetaDefinition = SkillMetaDefinition(),
    val effects: List<EffectDefinition> = emptyList()
)

/**
 * エフェクト定義
 */
data class EffectDefinition(
    val type: String,
    val amount: Double = 0.0,
    val strength: Double = 1.0,
    val duration: String = "0ms",
    val amplifier: Int = 0,
    val particle: String? = null,
    val sound: String? = null,
    val count: Int = 10,
    val message: String? = null,
    val command: String? = null,
    val asOp: Boolean = false,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f,
    val velocityX: Double = 0.0,
    val velocityY: Double = 0.0,
    val velocityZ: Double = 0.0,
    val offsetX: Double = 0.5,
    val offsetY: Double = 0.5,
    val offsetZ: Double = 0.5,
    val meta: SkillMetaDefinition = SkillMetaDefinition()
)

/**
 * ドロップ定義
 */
data class DropDefinition(
    val item: String,
    val amount: String = "1",  // "1" or "1-3" 形式
    val chance: Double = 1.0,
    val condition: String = "true"
) {
    /**
     * ドロップ個数を取得（範囲対応）
     */
    fun getAmount(): Int {
        return if (amount.contains("-")) {
            val parts = amount.split("-")
            val min = parts[0].toIntOrNull() ?: 1
            val max = parts[1].toIntOrNull() ?: min
            (min..max).random()
        } else {
            amount.toIntOrNull() ?: 1
        }
    }
}