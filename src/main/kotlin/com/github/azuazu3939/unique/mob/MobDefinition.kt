package com.github.azuazu3939.unique.mob

import org.bukkit.entity.EntityType

/**
 * Mob定義
 *
 * YAMLから読み込まれるMobの設定
 * Hopliteで型安全に読み込まれる
 *
 * health, damageはCEL式をサポート
 * 例: health="100", health="100 + (nearbyPlayers.count * 50)"
 *     damage="10", damage="10 + (nearbyPlayers.avgLevel * 0.5)"
 *
 * damageFormulaはPacketMobがダメージを受けた時の計算式
 * 例: damageFormula="damage * (1 - min(20, armor) / 25)"  # Minecraftの標準式
 *     damageFormula="damage * (1 - armor / 100)"  # シンプルな%軽減
 */
data class MobDefinition(
    // 基本設定
    val type: String,  // EntityTypeの文字列
    val display: String? = null,
    val health: String? = null,  // CEL式対応
    val damage: String? = null,  // CEL式対応
    val armor: String? = null,  // CEL式対応 - 防具値
    val armorToughness: String? = null,  // CEL式対応 - 防具強度

    // ダメージ計算式（PacketMob専用）
    val damageFormula: String? = null,  // CEL式対応 - ダメージ計算式

    // AI設定
    val ai: MobAI = MobAI(),

    // 外観設定
    val appearance: MobAppearance = MobAppearance(),

    // オプション設定
    val options: MobOptions = MobOptions(),

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
}

/**
 * Mob AI設定
 */
data class MobAI(
    val movementSpeed: Double = 0.25,
    val followRange: Double = 16.0,
    val knockbackResistance: Double = 0.0,
    val hasAI: Boolean = true,
    val hasGravity: Boolean = true,
    val lookAtMovementDirection: Boolean = true,
    val wallClimbHeight: Double = 1.0  // 乗り越えられる壁の高さ（ブロック数）
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
 * Mobオプション設定
 *
 * MythicMobsのOptionsを参考にした拡張設定
 * https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/Mobs/Options
 */
data class MobOptions(
    // 基本オプション
    val alwaysShowName: Boolean = false,        // 常に名前を表示
    val collidable: Boolean = true,             // 衝突判定
    val despawnOnChunkUnload: Boolean = true,   // チャンクアンロード時にデスポーン
    val despawnOnLogout: Boolean = false,       // プレイヤーログアウト時にデスポーン
    val invincible: Boolean = false,            // 無敵
    val persistent: Boolean = false,            // 永続化（サーバー再起動後も残る）
    val silent: Boolean = false,                // サイレント（全ての音を出さない）
    val playHurtSound: Boolean = true,          // ダメージを受けた時にサウンドを再生するか

    // アイテム関連
    val preventItemPickup: Boolean = false,     // アイテム拾得防止
    val preventOtherDrops: Boolean = false,     // 通常ドロップ防止（カスタムドロップのみ）

    // テレポート関連
    val preventTeleporting: Boolean = false,    // テレポート防止（エンダーマン等）
    val preventSunburn: Boolean = false,        // 日光ダメージ防止

    // スライム関連
    val preventSlimeSplit: Boolean = false,     // スライム分裂防止

    // 名前変更
    val preventRenaming: Boolean = false,       // 名札での名前変更防止

    // リーシュ
    val preventLeashing: Boolean = false,       // リードで引っ張られるのを防止

    // ターゲティング
    val targetable: Boolean = true,             // 他のMobからターゲットされるか

    // スポーン
    val spawnInvulnerableTicks: Int = 0,        // スポーン後の無敵時間（tick）

    // デスポーン
    val despawnDistance: Double = 128.0,        // プレイヤーからこの距離以上離れるとデスポーン
    val preventDespawn: Boolean = false,        // 通常のデスポーン防止

    // カスタムオプション
    val canTakeDamage: Boolean = true,          // ダメージを受けられるか（Display Entity用）
    val immuneToFire: Boolean = false,          // 火炎免疫
    val immuneToFall: Boolean = false,          // 落下ダメージ免疫
    val immuneToDrowning: Boolean = false,      // 溺死免疫
    val immuneToExplosions: Boolean = false,    // 爆発免疫

    // 追加の動作設定
    val noClip: Boolean = false,                // ブロックをすり抜ける
    val noPhysics: Boolean = false,             // 物理演算を無効化
    val followLeashSpeed: Double? = null,       // リードで引っ張られる速度

    // プレイヤーキル設定
    val setAsKiller: Boolean = true,            // プレイヤーを殺した時にキラーとして設定するか

    // アニメーション設定
    val showDamageAnimation: Boolean = true,    // ダメージを受けた時に赤くなるアニメーションを表示するか

    // ランキング表示設定
    val showDamageRanking: Boolean = false      // 死亡時にダメージランキングを表示するか
)

/**
 * Mobスキル設定
 *
 * 超コンパクト構文専用: ["スキル @ターゲッター ~トリガー", ...]
 */
data class MobSkills(
    val onTimer: List<SkillTrigger> = emptyList(),
    val onDamaged: List<SkillTrigger> = emptyList(),
    val onDeath: List<SkillTrigger> = emptyList(),
    val onSpawn: List<SkillTrigger> = emptyList(),
    val onAttack: List<SkillTrigger> = emptyList()
) {
    companion object {
        /**
         * 空のスキル設定
         */
        fun empty() = MobSkills()
    }
}

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
    val condition: String = "true",
    val filter: String? = null,  // CEL式フィルター

    // ========== 複合Targeter用 ==========
    val baseTargeter: TargeterDefinition? = null,  // LowestHealth, HighestHealth, Random等で使用

    // ========== AreaTargeter用 ==========
    val shape: String? = null,  // CIRCLE, CONE, BOX, DONUT
    val radius: String? = null,  // CEL式対応
    val innerRadius: String? = null,  // DONUT用
    val angle: String? = null,  // CONE用
    val width: String? = null,  // BOX用
    val height: String? = null,  // BOX用
    val depth: String? = null,  // BOX用
    val direction: String? = null,  // FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN
    val targetPlayers: Boolean = true,
    val targetMobs: Boolean = true,

    // ========== ChainTargeter用 ==========
    val initialTargeter: TargeterDefinition? = null,
    val maxChains: String? = null,  // CEL式対応
    val chainRange: String? = null,  // CEL式対応
    val chainCondition: String? = null  // CEL式
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
    val type: String? = null,  // Skillタイプ（Basic, Projectile, Meta, Branch等）
    val meta: SkillMetaDefinition = SkillMetaDefinition(),
    val effects: List<EffectDefinition> = emptyList(),

    // ========== ProjectileSkill用 ==========
    val projectileType: String? = null,
    val speed: String? = null,
    val gravity: Boolean? = null,
    val hitEffects: List<EffectDefinition> = emptyList(),
    val tickEffects: List<EffectDefinition> = emptyList(),
    val maxDistance: String? = null,
    val hitRadius: String? = null,
    val pierce: Boolean? = null,
    val homing: String? = null,
    val hitSound: String? = null,
    val launchSound: String? = null,

    // ========== MetaSkill用 ==========
    val phases: List<SkillPhaseDefinition> = emptyList(),

    // ========== BranchSkill用 ==========
    val branches: List<SkillBranchDefinition> = emptyList(),

    // ========== BeamSkill用 ==========
    val beamRange: String? = null,
    val beamWidth: String? = null,
    val beamParticle: String? = null,
    val beamParticleDensity: Double? = null,
    val beamDuration: String? = null,
    val beamTickInterval: String? = null,
    val beamPiercing: Boolean? = null,
    val beamFireSound: String? = null,

    // ========== AuraSkill用 ==========
    val auraRadius: String? = null,
    val auraDuration: String? = null,
    val auraTickInterval: String? = null,
    val auraParticle: String? = null,
    val auraParticleCount: Int? = null,
    val auraParticleSpeed: Double? = null,
    val auraSelfAffect: Boolean? = null,
    val auraMaxTargets: Int? = null,
    val auraStartSound: String? = null,
    val auraTickSound: String? = null,
    val auraEndSound: String? = null
)

/**
 * スキルフェーズ定義（MetaSkill用）
 */
data class SkillPhaseDefinition(
    val name: String,
    val targeter: TargeterDefinition,
    val skills: List<SkillReference>,
    val parallel: Boolean = false,
    val executeDelay: String = "0ms",
    val sync: Boolean = false
)

/**
 * スキル分岐定義（BranchSkill用）
 */
data class SkillBranchDefinition(
    val condition: String? = null,
    val skills: List<SkillReference>,
    val targeter: TargeterDefinition,
    val isDefault: Boolean = false
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
    val meta: SkillMetaDefinition = SkillMetaDefinition(),

    // ========== 基本Effect専用フィールド ==========
    val damageAmount: Double? = null,  // DamageEffect用
    val healAmount: Double? = null,  // HealEffect用
    val potionType: String? = null,  // PotionEffect用
    val potionDuration: String? = null,  // PotionEffect用
    val potionAmplifier: Int? = null,  // PotionEffect用
    val particleCount: Int? = null,  // ParticleEffect用
    val soundVolume: Float? = null,  // SoundEffect用
    val soundPitch: Float? = null,  // SoundEffect用

    // ========== LightningEffect専用フィールド ==========
    val lightningDamage: Double? = null,
    val lightningSetFire: Boolean? = null,
    val lightningVisualOnly: Boolean? = null,

    // ========== ExplosionEffect専用フィールド ==========
    val explosionDamage: Double? = null,
    val explosionRadius: Double? = null,
    val explosionKnockback: Double? = null,
    val explosionSetFire: Boolean? = null,
    val explosionBreakBlocks: Boolean? = null,

    // ========== FreezeEffect専用フィールド ==========
    val freezeDuration: String? = null,
    val freezeAmplifier: Int? = null,

    // ========== ShieldEffect専用フィールド ==========
    val shieldAmount: Double? = null,
    val shieldDuration: String? = null,

    // ========== TeleportEffect専用フィールド ==========
    val teleportMode: String? = null,  // TO_SOURCE, TO_TARGET, RANDOM
    val teleportRange: Double? = null,

    // ========== PullEffect専用フィールド ==========
    val pullStrength: Double? = null,

    // ========== PushEffect専用フィールド ==========
    val pushStrength: Double? = null,

    // ========== BlindEffect専用フィールド ==========
    val blindDuration: String? = null,
    val blindAmplifier: Int? = null,

    // ========== SetBlockEffect専用フィールド ==========
    val blockType: String? = null,
    val blockRadius: Double? = null,
    val blockTemporary: Boolean? = null,
    val blockTemporaryDuration: String? = null,

    // ========== 共通フィールド ==========
    val radius: Double? = null,  // Explosion, Pull, Push等で使用
    val knockback: Double? = null,  // Explosion, Push等で使用
    val setFire: Boolean? = null  // Lightning, Explosion等で使用
)

/**
 * ドロップ定義
 *
 * amount, chanceはCEL式をサポート
 * 例: amount="1", amount="math.max(1, nearbyPlayers.maxLevel / 10)"
 *     chance="1.0", chance="0.1 + (killer.level * 0.01)", chance="environment.moonPhase == 0 ? 0.5 : 0.1"
 */
data class DropDefinition(
    val item: String,
    val amount: String = "1",  // CEL式対応（範囲形式 "1-3" も互換性維持）
    val chance: String = "1.0",  // CEL式対応
    val condition: String = "true"
) {
    /**
     * ドロップ個数を取得（範囲対応、CEL評価は呼び出し側で行う）
     * @deprecated Use evaluateAmount() in MobManager instead
     */
    @Deprecated("Use evaluateAmount() in MobManager", ReplaceWith("amount"))
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

/**
 * Display Entity定義
 *
 * PacketMobとは異なり、装飾専用のエンティティ
 * ダメージを受けず、AIも持たない
 * ホログラム、看板、装飾などに使用
 */
data class DisplayDefinition(
    // 基本設定
    val type: String,  // TEXT, ITEM, BLOCK
    val display: String? = null,  // 表示名

    // TEXT_DISPLAY用
    val text: String? = null,  // 表示テキスト
    val backgroundColor: String? = null,  // 背景色（ARGB: "#40000000"形式）
    val textOpacity: Int? = null,  // テキスト不透明度（-1 = デフォルト）

    // ITEM_DISPLAY用
    val item: String? = null,  // アイテムタイプ（例: "DIAMOND_SWORD"）

    // BLOCK_DISPLAY用
    val block: String? = null,  // ブロックタイプ（例: "STONE"）

    // 共通設定
    val scale: Float = 1.0f,  // 拡大率
    val viewRange: Float = 1.0f,  // 表示範囲
    val shadowRadius: Float = 0.0f,  // 影の半径
    val shadowStrength: Float = 1.0f,  // 影の強度
    val billboardMode: String = "FIXED",  // FIXED, VERTICAL, HORIZONTAL, CENTER

    // スキル設定（Display Entityもスキルを持てる）
    val skills: MobSkills = MobSkills()
) {
    /**
     * DisplayTypeを取得
     */
    fun getDisplayType(): String {
        return type.uppercase()
    }

    /**
     * 表示名を取得
     */
    fun getDisplayName(): String {
        return display ?: type
    }
}