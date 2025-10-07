package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.EntityAnimation.DEATH
import com.github.azuazu3939.unique.entity.packet.PacketSender
import com.github.azuazu3939.unique.mob.MobOptions
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*

/**
 * AI状態
 */
enum class AIState {
    IDLE,       // 待機中
    TARGET,     // ターゲット追跡中
    ATTACK,     // 攻撃中
    WANDER      // 徘徊中
}

/**
 * Mob用パケットエンティティ
 *
 * カスタムMobの実装
 * PacketEventsを使用してパケットを送信
 */
class PacketMob(
    entityId: Int,
    uuid: UUID,
    entityType: EntityType,
    location: Location,
    val mobName: String
) : PacketEntity(entityId, uuid, entityType, location) {

    // Component classes
    private val physics = PacketMobPhysics(this)
    private val ai = PacketMobAI(this, physics)
    internal val combat = PacketMobCombat(this)

    /**
     * カスタム名表示フラグ
     */
    var customNameVisible: Boolean = true

    /**
     * カスタム名
     */
    var customName: String = mobName
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { player ->
                sendMetadataPacket(Bukkit.getPlayer(player))
            }
        }

    /**
     * AI有効フラグ
     */
    var hasAI: Boolean = true

    /**
     * 重力フラグ
     */
    var hasGravity: Boolean = true

    /**
     * 発光フラグ
     */
    var isGlowing: Boolean = false
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { player ->
                sendMetadataPacket(Bukkit.getPlayer(player))
            }
        }

    /**
     * 不可視フラグ
     */
    var isInvisible: Boolean = false
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { player ->
                sendMetadataPacket(Bukkit.getPlayer(player))
            }
        }

    /**
     * 防具値（0-30）
     * Minecraftのダメージ軽減計算に使用される
     */
    var armor: Double = 0.0

    /**
     * 防具強度（0-20）
     * 高ダメージに対する追加軽減
     */
    var armorToughness: Double = 0.0

    /**
     * 攻撃力
     * 攻撃時に与えるダメージ量
     */
    var damage: Double = 1.0

    /**
     * ダメージ計算式（CEL式）
     * nullの場合はデフォルトの計算式を使用
     * 例: "damage * (1 - min(20, armor) / 25)"
     */
    var damageFormula: String? = null

    /**
     * Mobオプション設定
     */
    var options: MobOptions = MobOptions()

    /**
     * 変数ストレージ（Mob個体ごと）
     * スキル実行時に使用される一時変数を保存
     */
    private val variables = mutableMapOf<String, Any>()

    /**
     * 変数を設定
     */
    fun setVariable(name: String, value: Any) {
        variables[name] = value
    }

    /**
     * 変数を取得
     */
    fun getVariable(name: String): Any? {
        return variables[name]
    }

    /**
     * すべての変数を取得
     */
    fun getVariables(): Map<String, Any> {
        return variables.toMap()
    }

    /**
     * 変数をクリア
     */
    fun clearVariables() {
        variables.clear()
    }

    /**
     * Physicsコンポーネントを取得
     */
    fun getPhysicsComponent(): PacketMobPhysics {
        return physics
    }

    /**
     * AIコンポーネントを取得
     */
    fun getAIComponent(): PacketMobAI {
        return ai
    }

    /**
     * Combatコンポーネントを取得
     */
    fun getCombatComponent(): PacketMobCombat {
        return combat
    }

    // ========================================
    // AI関連フィールド
    // ========================================

    /**
     * 移動速度（ブロック/tick）
     * 0.1 = 2ブロック/秒（1秒=20tick）
     */
    var movementSpeed: Double = 0.1

    /**
     * 追跡範囲
     */
    var followRange: Double = 16.0

    /**
     * 攻撃範囲
     */
    var attackRange: Double = 2.0

    /**
     * 攻撃クールダウン（tick）
     */
    var attackCooldown: Int = 20

    /**
     * ターゲット検索間隔（tick）
     */
    var targetSearchInterval: Int = 20

    /**
     * ノックバック耐性
     */
    var knockbackResistance: Double = 0.0

    /**
     * 移動方向に視点を向けるか
     */
    var lookAtMovementDirection: Boolean = true

    /**
     * Head（頭）のYaw（水平回転角度）
     * AIの視線方向を表す
     */
    var headYaw: Float = 0f

    /**
     * Body（胴体）のYaw（水平回転角度）
     * エンティティ全体の向きを表す
     */
    var bodyYaw: Float = 0f

    /**
     * 乗り越えられる壁の高さ（ブロック数）
     * 旧名: wallClimbHeight
     */
    var stepHeight: Double = 1.0

    /**
     * エンティティをスポーン
     */
    override suspend fun spawn(player: Player) {
        if (isDead) return
        PacketSender.sendSpawnPacket(player, this)
        addViewer(player)
    }

    /**
     * エンティティをデスポーン
     */
    override fun despawn(player: Player) {
        PacketSender.sendDespawnPacket(player, entityId)
        removeViewer(player)
    }

    /**
     * テレポート
     */
    override suspend fun teleport(newLocation: Location) {
        if (isDead) return

        val oldLocation = location.clone()
        location = newLocation.clone()

        // 全ビューワーに送信
        viewers.forEach { player ->
            Bukkit.getPlayer(player)?.let {
                PacketSender.sendTeleportPacket(it, entityId, newLocation)
            }
        }

        DebugLogger.verbose("Teleported $mobName from $oldLocation to $newLocation")
    }

    /**
     * 相対移動
     */
    override suspend fun move(deltaX: Double, deltaY: Double, deltaZ: Double) {
        if (isDead) return

        location.add(deltaX, deltaY, deltaZ)

        // 全ビューワーに送信
        viewers.forEach { player ->
           Bukkit.getPlayer(player)?.let {
               PacketSender.sendMovePacket(it, entityId, deltaX, deltaY, deltaZ)
           }
        }

        DebugLogger.verbose("Moved $mobName by ($deltaX, $deltaY, $deltaZ)")
    }

    /**
     * メタデータ更新
     */
    override suspend fun updateMetadata() {
        if (isDead) return

        viewers.forEach { player ->
            sendMetadataPacket(Bukkit.getPlayer(player))
        }

        DebugLogger.verbose("Updated metadata for $mobName")
    }

    /**
     * メタデータパケット送信
     */
    private fun sendMetadataPacket(player: Player?) {
        if (player == null) return
        PacketSender.sendMetadataPacket(player, this)
    }

    /**
     * アニメーション再生
     */
    override fun playAnimation(animation: EntityAnimation) {
        // 死亡時はDEATHアニメーションのみ再生可能
        if (isDead && animation != DEATH) {
            DebugLogger.verbose("Skipping animation $animation for $mobName (isDead=true)")
            return
        }

        viewers.forEach { player ->
            Bukkit.getPlayer(player)?.let {
                PacketSender.sendAnimationPacket(it, entityId, animation)
            }
        }
        DebugLogger.verbose("Playing animation $animation for $mobName")
    }

    /**
     * ダメージを受けられるかどうか
     * optionsで設定可能
     */
    override fun canTakeDamage(): Boolean {
        return options.canTakeDamage && !options.invincible
    }

    /**
     * ダメージを受ける（オーバーライド）
     */
    /**
     * ダメージを与える（攻撃者なし）
     */
    override suspend fun damage(amount: Double) {
        combat.damage(amount, null)
    }

    /**
     * ダメージを与える（攻撃者あり）
     *
     * @param amount ダメージ量
     * @param damager ダメージを与えたエンティティ（Entity または PacketMob）
     */
    suspend fun damage(amount: Double, damager: Entity?) {
        combat.damage(amount, damager)
    }

    /**
     * 殺す（オーバーライド）
     */
    override suspend fun kill() {
        combat.kill()
    }

    /**
     * 更新処理
     * 最適化：AI処理を間引き、ビューワーがいない場合はスキップ
     */
    override fun tick() {
        super.tick()

        val config = Unique.instance.configManager.mainConfig.performance

        // 最後のダメージャー情報をクリア（設定可能な時間経過後）
        if (combat.shouldClearDamageMemory(ticksLived, config.damageMemoryTicks.toInt())) {
            combat.clearDamageMemory()
        }

        // 物理処理（重力、速度、埋まりチェック、摩擦を一括処理）
        if (!isDead) {
            physics.tick()
        }

        // AI処理（最適化：周囲にプレイヤーがいない場合はスキップ）
        if (hasAI && !isDead) {
            // ビューワー（周囲のプレイヤー）がいる場合のみAI処理（設定で変更可能）
            if (!config.skipAiWhenNoViewers || viewers.isNotEmpty()) {
                ai.tick()
            }
        }
    }

    /**
     * 速度を追加（ノックバックなど）
     */
    fun addVelocity(vx: Double, vy: Double, vz: Double) {
        physics.addVelocity(vx, vy, vz)
    }

    /**
     * エンティティタイプに応じたヒットボックスの幅を取得
     */
    internal fun getEntityHitboxWidth(): Double {
        return when (entityType) {
            // 人型 (0.6)
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.PLAYER,
            EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
            EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.CREEPER,
            EntityType.ENDERMAN, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.WITCH -> 0.6

            // 小型 (0.3-0.5)
            EntityType.SILVERFISH, EntityType.ENDERMITE -> 0.4
            EntityType.CAVE_SPIDER -> 0.7
            EntityType.BEE -> 0.7
            EntityType.VEX -> 0.4
            EntityType.CHICKEN -> 0.4
            EntityType.BAT -> 0.5

            // 中型 (0.9-1.0)
            EntityType.PIG, EntityType.SHEEP -> 0.9
            EntityType.COW -> 0.9
            EntityType.WOLF -> 0.6
            EntityType.CAT, EntityType.OCELOT -> 0.6
            EntityType.FOX -> 0.6
            EntityType.PANDA -> 1.3
            EntityType.POLAR_BEAR -> 1.4
            EntityType.GOAT -> 0.9

            // 大型 (1.0-2.0)
            EntityType.SPIDER -> 1.4
            EntityType.IRON_GOLEM -> 1.4
            EntityType.VILLAGER, EntityType.WANDERING_TRADER -> 0.6
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE -> 1.39
            EntityType.LLAMA, EntityType.TRADER_LLAMA -> 0.9
            EntityType.HOGLIN, EntityType.ZOGLIN -> 1.39
            EntityType.RAVAGER -> 1.95
            EntityType.STRIDER -> 0.9
            EntityType.CAMEL -> 1.7

            // ボス級
            EntityType.GIANT -> 3.6
            EntityType.WITHER -> 0.9
            EntityType.ENDER_DRAGON -> 16.0
            EntityType.WARDEN -> 0.9

            // 飛行系
            EntityType.GHAST -> 4.0
            EntityType.PHANTOM -> 0.9
            EntityType.BLAZE -> 0.6
            EntityType.PARROT -> 0.5

            // 水棲
            EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN -> 0.85
            EntityType.SQUID, EntityType.GLOW_SQUID -> 0.8
            EntityType.DOLPHIN -> 0.9
            EntityType.TURTLE -> 1.2
            EntityType.COD, EntityType.SALMON -> 0.5
            EntityType.PUFFERFISH -> 0.7
            EntityType.TROPICAL_FISH -> 0.5
            EntityType.AXOLOTL -> 0.75

            // スライム系（サイズ1の場合）
            EntityType.SLIME, EntityType.MAGMA_CUBE -> 2.04

            // その他
            EntityType.SHULKER -> 1.0
            EntityType.SNOW_GOLEM -> 0.7
            EntityType.EVOKER, EntityType.ILLUSIONER, EntityType.VINDICATOR, EntityType.PILLAGER -> 0.6
            EntityType.ALLAY -> 0.35
            EntityType.FROG -> 0.5
            EntityType.TADPOLE -> 0.4
            EntityType.SNIFFER -> 1.9

            else -> 0.6  // デフォルト
        }
    }

    /**
     * エンティティタイプに応じたヒットボックスの高さを取得
     */
    internal fun getEntityHitboxHeight(): Double {
        return when (entityType) {
            // 人型 (1.8-2.0)
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.PLAYER,
            EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
            EntityType.STRAY, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.ZOMBIFIED_PIGLIN -> 1.95
            EntityType.WITHER_SKELETON -> 2.4
            EntityType.CREEPER -> 1.7
            EntityType.ENDERMAN -> 2.9
            EntityType.WITCH -> 1.95

            // 小型 (0.3-0.7)
            EntityType.SILVERFISH -> 0.3
            EntityType.ENDERMITE -> 0.3
            EntityType.CAVE_SPIDER -> 0.5
            EntityType.BEE -> 0.6
            EntityType.VEX -> 0.8
            EntityType.CHICKEN -> 0.7
            EntityType.BAT -> 0.9

            // 中型 (0.9-1.4)
            EntityType.PIG -> 0.9
            EntityType.SHEEP -> 1.3
            EntityType.COW -> 1.4
            EntityType.WOLF -> 0.85
            EntityType.CAT, EntityType.OCELOT -> 0.7
            EntityType.FOX -> 0.7
            EntityType.PANDA -> 1.25
            EntityType.POLAR_BEAR -> 1.4
            EntityType.GOAT -> 1.3

            // 大型 (0.9-2.2)
            EntityType.SPIDER -> 0.9
            EntityType.IRON_GOLEM -> 2.7
            EntityType.VILLAGER, EntityType.WANDERING_TRADER -> 1.95
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE -> 1.6
            EntityType.LLAMA, EntityType.TRADER_LLAMA -> 1.87
            EntityType.HOGLIN, EntityType.ZOGLIN -> 1.4
            EntityType.RAVAGER -> 2.2
            EntityType.STRIDER -> 1.7
            EntityType.CAMEL -> 2.375

            // ボス級
            EntityType.GIANT -> 12.0
            EntityType.WITHER -> 3.5
            EntityType.ENDER_DRAGON -> 8.0
            EntityType.WARDEN -> 2.9

            // 飛行系
            EntityType.GHAST -> 4.0
            EntityType.PHANTOM -> 0.5
            EntityType.BLAZE -> 1.8
            EntityType.PARROT -> 0.9

            // 水棲
            EntityType.GUARDIAN -> 0.85
            EntityType.ELDER_GUARDIAN -> 1.99
            EntityType.SQUID, EntityType.GLOW_SQUID -> 0.8
            EntityType.DOLPHIN -> 0.6
            EntityType.TURTLE -> 0.4
            EntityType.COD -> 0.4
            EntityType.SALMON -> 0.4
            EntityType.PUFFERFISH -> 0.7
            EntityType.TROPICAL_FISH -> 0.4
            EntityType.AXOLOTL -> 0.42

            // スライム系（サイズ1の場合）
            EntityType.SLIME, EntityType.MAGMA_CUBE -> 2.04

            // その他
            EntityType.SHULKER -> 1.0
            EntityType.SNOW_GOLEM -> 1.9
            EntityType.EVOKER, EntityType.ILLUSIONER, EntityType.VINDICATOR, EntityType.PILLAGER -> 1.95
            EntityType.ALLAY -> 0.6
            EntityType.FROG -> 0.5
            EntityType.TADPOLE -> 0.3
            EntityType.SNIFFER -> 1.75

            else -> 1.8  // デフォルト
        }
    }

    // ========================================
    // Internal helper methods for component classes
    // ========================================

    /**
     * Internal method for combat class to apply damage without triggering events
     */
    internal suspend fun applyDamage(amount: Double) {
        super.damage(amount)
    }

    /**
     * Internal method for AI class to perform attacks
     */
    internal suspend fun performAttack(target: Entity) {
        combat.performAttack(target)
    }

    /**
     * Internal method for AI to update rotation
     *
     * Head（頭）の向きを更新し、全viewerにパケットを送信
     */
    internal fun updateRotation(yaw: Float, pitch: Float) {
        // Head（頭）の向きを更新
        headYaw = yaw
        location.yaw = yaw
        location.pitch = pitch

        // Send head rotation packet to all viewers
        viewers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                PacketSender.sendHeadRotationPacket(player, entityId, yaw)
            }
        }
    }

    /**
     * Internal method for AI to sync body rotation
     *
     * Body（胴体）の向きをHead（頭）の向きに同期させる
     * 定期的に呼ばれることで、頭と胴体の向きのズレを修正
     */
    internal fun syncBodyRotation() {
        // BodyYawをHeadYawに同期
        bodyYaw = headYaw
        location.yaw = bodyYaw

        // Send body rotation packet to all viewers
        viewers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                PacketSender.sendBodyRotationPacket(player, entityId, bodyYaw)
            }
        }
    }

    /**
     * ダメージサウンドを再生
     */
    internal fun playHurtSound() {
        val sound = getHurtSound() ?: return
        val world = location.world ?: return

        // サウンドを再生（音量1.0、ピッチ1.0）
        world.playSound(location, sound, 1.0f, 1.0f)
    }

    /**
     * 死亡サウンドを再生
     */
    internal fun playDeathSound() {
        val sound = getDeathSound() ?: return
        val world = location.world ?: return

        // サウンドを再生（音量1.0、ピッチ1.0）
        world.playSound(location, sound, 1.0f, 1.0f)
    }

    /**
     * エンティティタイプに応じたダメージサウンドを取得
     */
    private fun getHurtSound(): org.bukkit.Sound? {
        return when (entityType) {
            EntityType.ZOMBIE -> org.bukkit.Sound.ENTITY_ZOMBIE_HURT
            EntityType.SKELETON -> org.bukkit.Sound.ENTITY_SKELETON_HURT
            EntityType.SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_HURT
            EntityType.CREEPER -> org.bukkit.Sound.ENTITY_CREEPER_HURT
            EntityType.ENDERMAN -> org.bukkit.Sound.ENTITY_ENDERMAN_HURT
            EntityType.BLAZE -> org.bukkit.Sound.ENTITY_BLAZE_HURT
            EntityType.WITCH -> org.bukkit.Sound.ENTITY_WITCH_HURT
            EntityType.PIGLIN -> org.bukkit.Sound.ENTITY_PIGLIN_HURT
            EntityType.WITHER_SKELETON -> org.bukkit.Sound.ENTITY_WITHER_SKELETON_HURT
            EntityType.PHANTOM -> org.bukkit.Sound.ENTITY_PHANTOM_HURT
            EntityType.HOGLIN -> org.bukkit.Sound.ENTITY_HOGLIN_HURT
            EntityType.PIGLIN_BRUTE -> org.bukkit.Sound.ENTITY_PIGLIN_BRUTE_HURT
            EntityType.IRON_GOLEM -> org.bukkit.Sound.ENTITY_IRON_GOLEM_HURT
            EntityType.VILLAGER -> org.bukkit.Sound.ENTITY_VILLAGER_HURT
            EntityType.ENDER_DRAGON -> org.bukkit.Sound.ENTITY_ENDER_DRAGON_HURT
            EntityType.WITHER -> org.bukkit.Sound.ENTITY_WITHER_HURT
            EntityType.GHAST -> org.bukkit.Sound.ENTITY_GHAST_HURT
            EntityType.SLIME -> org.bukkit.Sound.ENTITY_SLIME_HURT
            EntityType.MAGMA_CUBE -> org.bukkit.Sound.ENTITY_MAGMA_CUBE_HURT
            EntityType.SILVERFISH -> org.bukkit.Sound.ENTITY_SILVERFISH_HURT
            EntityType.CAVE_SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_HURT
            EntityType.GUARDIAN -> org.bukkit.Sound.ENTITY_GUARDIAN_HURT
            EntityType.ELDER_GUARDIAN -> org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_HURT
            EntityType.SHULKER -> org.bukkit.Sound.ENTITY_SHULKER_HURT
            EntityType.VEX -> org.bukkit.Sound.ENTITY_VEX_HURT
            EntityType.VINDICATOR -> org.bukkit.Sound.ENTITY_VINDICATOR_HURT
            EntityType.EVOKER -> org.bukkit.Sound.ENTITY_EVOKER_HURT
            EntityType.RAVAGER -> org.bukkit.Sound.ENTITY_RAVAGER_HURT
            EntityType.PILLAGER -> org.bukkit.Sound.ENTITY_PILLAGER_HURT
            EntityType.DROWNED -> org.bukkit.Sound.ENTITY_DROWNED_HURT
            EntityType.HUSK -> org.bukkit.Sound.ENTITY_HUSK_HURT
            EntityType.STRAY -> org.bukkit.Sound.ENTITY_STRAY_HURT
            EntityType.ZOMBIE_VILLAGER -> org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_HURT
            EntityType.ENDERMITE -> org.bukkit.Sound.ENTITY_ENDERMITE_HURT
            EntityType.WARDEN -> org.bukkit.Sound.ENTITY_WARDEN_HURT
            EntityType.COW -> org.bukkit.Sound.ENTITY_COW_HURT
            EntityType.PIG -> org.bukkit.Sound.ENTITY_PIG_HURT
            EntityType.SHEEP -> org.bukkit.Sound.ENTITY_SHEEP_HURT
            EntityType.CHICKEN -> org.bukkit.Sound.ENTITY_CHICKEN_HURT
            EntityType.WOLF -> org.bukkit.Sound.ENTITY_WOLF_HURT
            EntityType.CAT -> org.bukkit.Sound.ENTITY_CAT_HURT
            EntityType.HORSE -> org.bukkit.Sound.ENTITY_HORSE_HURT
            else -> null  // サウンドが定義されていないエンティティタイプ
        }
    }

    /**
     * エンティティタイプに応じた死亡サウンドを取得
     */
    private fun getDeathSound(): org.bukkit.Sound? {
        return when (entityType) {
            EntityType.ZOMBIE -> org.bukkit.Sound.ENTITY_ZOMBIE_DEATH
            EntityType.SKELETON -> org.bukkit.Sound.ENTITY_SKELETON_DEATH
            EntityType.SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_DEATH
            EntityType.CREEPER -> org.bukkit.Sound.ENTITY_CREEPER_DEATH
            EntityType.ENDERMAN -> org.bukkit.Sound.ENTITY_ENDERMAN_DEATH
            EntityType.BLAZE -> org.bukkit.Sound.ENTITY_BLAZE_DEATH
            EntityType.WITCH -> org.bukkit.Sound.ENTITY_WITCH_DEATH
            EntityType.PIGLIN -> org.bukkit.Sound.ENTITY_PIGLIN_DEATH
            EntityType.WITHER_SKELETON -> org.bukkit.Sound.ENTITY_WITHER_SKELETON_DEATH
            EntityType.PHANTOM -> org.bukkit.Sound.ENTITY_PHANTOM_DEATH
            EntityType.HOGLIN -> org.bukkit.Sound.ENTITY_HOGLIN_DEATH
            EntityType.PIGLIN_BRUTE -> org.bukkit.Sound.ENTITY_PIGLIN_BRUTE_DEATH
            EntityType.IRON_GOLEM -> org.bukkit.Sound.ENTITY_IRON_GOLEM_DEATH
            EntityType.VILLAGER -> org.bukkit.Sound.ENTITY_VILLAGER_DEATH
            EntityType.ENDER_DRAGON -> org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH
            EntityType.WITHER -> org.bukkit.Sound.ENTITY_WITHER_DEATH
            EntityType.GHAST -> org.bukkit.Sound.ENTITY_GHAST_DEATH
            EntityType.SLIME -> org.bukkit.Sound.ENTITY_SLIME_DEATH
            EntityType.MAGMA_CUBE -> org.bukkit.Sound.ENTITY_MAGMA_CUBE_DEATH
            EntityType.SILVERFISH -> org.bukkit.Sound.ENTITY_SILVERFISH_DEATH
            EntityType.CAVE_SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_DEATH
            EntityType.GUARDIAN -> org.bukkit.Sound.ENTITY_GUARDIAN_DEATH
            EntityType.ELDER_GUARDIAN -> org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_DEATH
            EntityType.SHULKER -> org.bukkit.Sound.ENTITY_SHULKER_DEATH
            EntityType.VEX -> org.bukkit.Sound.ENTITY_VEX_DEATH
            EntityType.VINDICATOR -> org.bukkit.Sound.ENTITY_VINDICATOR_DEATH
            EntityType.EVOKER -> org.bukkit.Sound.ENTITY_EVOKER_DEATH
            EntityType.RAVAGER -> org.bukkit.Sound.ENTITY_RAVAGER_DEATH
            EntityType.PILLAGER -> org.bukkit.Sound.ENTITY_PILLAGER_DEATH
            EntityType.DROWNED -> org.bukkit.Sound.ENTITY_DROWNED_DEATH
            EntityType.HUSK -> org.bukkit.Sound.ENTITY_HUSK_DEATH
            EntityType.STRAY -> org.bukkit.Sound.ENTITY_STRAY_DEATH
            EntityType.ZOMBIE_VILLAGER -> org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_DEATH
            EntityType.ENDERMITE -> org.bukkit.Sound.ENTITY_ENDERMITE_DEATH
            EntityType.WARDEN -> org.bukkit.Sound.ENTITY_WARDEN_DEATH
            EntityType.COW -> org.bukkit.Sound.ENTITY_COW_DEATH
            EntityType.PIG -> org.bukkit.Sound.ENTITY_PIG_DEATH
            EntityType.SHEEP -> org.bukkit.Sound.ENTITY_SHEEP_DEATH
            EntityType.CHICKEN -> org.bukkit.Sound.ENTITY_CHICKEN_DEATH
            EntityType.WOLF -> org.bukkit.Sound.ENTITY_WOLF_DEATH
            EntityType.CAT -> org.bukkit.Sound.ENTITY_CAT_DEATH
            EntityType.HORSE -> org.bukkit.Sound.ENTITY_HORSE_DEATH
            else -> null  // サウンドが定義されていないエンティティタイプ
        }
    }

    /**
     * ダメージランキングを取得
     *
     * @param limit 取得する順位数（デフォルト: 10位まで）
     * @return ランキング（UUID と累計ダメージのペアリスト）
     */
    fun getDamageRanking(limit: Int = 10): List<Pair<UUID, Double>> {
        return combat.getDamageRanking(limit)
    }

    /**
     * プレイヤーの累計ダメージを取得
     *
     * @param player プレイヤー
     * @return 累計ダメージ
     */
    fun getPlayerDamage(player: Player): Double {
        return combat.getPlayerDamage(player)
    }

    /**
     * プレイヤーのランキング順位を取得
     *
     * @param player プレイヤー
     * @return 順位（1位から）、ランキング外の場合-1
     */
    fun getPlayerRank(player: Player): Int {
        return combat.getPlayerRank(player)
    }

    /**
     * ダメージランキングをクリア
     */
    fun clearDamageRanking() {
        combat.clearDamageRanking()
    }

    /**
     * ダメージランキングを表示（チャット）
     *
     * @param limit 表示する順位数
     */
    fun broadcastDamageRanking(limit: Int = 10) {
        combat.broadcastDamageRanking(limit)
    }

    /**
     * ビルダー
     */
    class Builder(
        private val entityId: Int,
        private val uuid: UUID,
        private val entityType: EntityType,
        private val location: Location,
        private val mobName: String
    ) {
        private var health: Double = 20.0
        private var maxHealth: Double = 20.0
        private var armor: Double = 0.0
        private var armorToughness: Double = 0.0
        private var damage: Double = 1.0
        private var damageFormula: String? = null
        private var customNameVisible: Boolean = true
        private var hasAI: Boolean = true
        private var hasGravity: Boolean = true
        private var isGlowing: Boolean = false
        private var isInvisible: Boolean = false

        // オプション設定
        private var options: MobOptions = MobOptions()

        // AI関連
        private var movementSpeed: Double = 0.1  // 2ブロック/秒（0.1 * 20tick/秒）
        private var followRange: Double = 16.0
        private var attackRange: Double = 2.0
        private var attackCooldown: Int = 20
        private var targetSearchInterval: Int = 20
        private var knockbackResistance: Double = 0.0
        private var lookAtMovementDirection: Boolean = true
        private var wallClimbHeight: Double = 1.0

        fun health(health: Double) = apply { this.health = health }
        fun maxHealth(maxHealth: Double) = apply { this.maxHealth = maxHealth }
        fun armor(armor: Double) = apply { this.armor = armor.coerceIn(0.0, 30.0) }
        fun armorToughness(toughness: Double) = apply { this.armorToughness = toughness.coerceIn(0.0, 20.0) }
        fun damage(damage: Double) = apply { this.damage = damage.coerceAtLeast(0.0) }
        fun damageFormula(formula: String?) = apply { this.damageFormula = formula }
        fun customNameVisible(visible: Boolean) = apply { this.customNameVisible = visible }
        fun hasAI(ai: Boolean) = apply { this.hasAI = ai }
        fun hasGravity(gravity: Boolean) = apply { this.hasGravity = gravity }
        fun glowing(glowing: Boolean) = apply { this.isGlowing = glowing }
        fun invisible(invisible: Boolean) = apply { this.isInvisible = invisible }

        // オプション設定
        fun options(options: MobOptions) = apply { this.options = options }

        // AI設定
        fun movementSpeed(speed: Double) = apply { this.movementSpeed = speed }
        fun followRange(range: Double) = apply { this.followRange = range }
        fun attackRange(range: Double) = apply { this.attackRange = range }
        fun attackCooldown(cooldown: Int) = apply { this.attackCooldown = cooldown }
        fun targetSearchInterval(interval: Int) = apply { this.targetSearchInterval = interval }
        fun knockbackResistance(resistance: Double) = apply { this.knockbackResistance = resistance }
        fun lookAtMovementDirection(enabled: Boolean) = apply { this.lookAtMovementDirection = enabled }
        fun wallClimbHeight(height: Double) = apply { this.wallClimbHeight = height.coerceAtLeast(0.0) }

        fun build(): PacketMob {
            val mob = PacketMob(entityId, uuid, entityType, location, mobName)
            mob.health = health
            mob.maxHealth = maxHealth
            mob.armor = armor
            mob.armorToughness = armorToughness
            mob.damage = damage
            mob.damageFormula = damageFormula
            mob.customNameVisible = customNameVisible
            mob.hasAI = hasAI
            mob.hasGravity = hasGravity
            mob.isGlowing = isGlowing
            mob.isInvisible = isInvisible

            // オプション設定
            mob.options = options

            // AI設定
            mob.movementSpeed = movementSpeed
            mob.followRange = followRange
            mob.attackRange = attackRange
            mob.attackCooldown = attackCooldown
            mob.targetSearchInterval = targetSearchInterval
            mob.knockbackResistance = knockbackResistance
            mob.lookAtMovementDirection = lookAtMovementDirection
            mob.stepHeight = wallClimbHeight

            // Rotation初期化（locationのyawを使用）
            mob.headYaw = location.yaw
            mob.bodyYaw = location.yaw

            return mob
        }
    }


    companion object {
        /**
         * ビルダーを作成
         */
        fun builder(
            entityId: Int,
            uuid: UUID,
            entityType: EntityType,
            location: Location,
            mobName: String
        ): Builder {
            return Builder(entityId, uuid, entityType, location, mobName)
        }
    }
}
