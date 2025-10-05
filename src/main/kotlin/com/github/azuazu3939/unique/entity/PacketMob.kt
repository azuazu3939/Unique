package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.entity.EntityAnimation.DAMAGE
import com.github.azuazu3939.unique.entity.EntityAnimation.DEATH
import com.github.azuazu3939.unique.entity.packet.PacketSender
import com.github.azuazu3939.unique.event.PacketMobAttackEvent
import com.github.azuazu3939.unique.event.PacketMobSkillEvent
import com.github.azuazu3939.unique.event.PacketMobTargetEvent
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.EventUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.min

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

    // ========================================
    // AI関連フィールド
    // ========================================

    /**
     * AI状態
     */
    var aiState: AIState = AIState.IDLE

    /**
     * 現在のターゲット
     */
    var currentTarget: Entity? = null

    /**
     * 最後にターゲットをチェックしたTick
     */
    var lastTargetCheckTick: Int = 0

    /**
     * 移動速度
     */
    var movementSpeed: Double = 0.25

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
     * 最後に攻撃したTick
     */
    var lastAttackTick: Int = 0

    /**
     * ターゲット検索間隔（tick）
     */
    var targetSearchInterval: Int = 20

    /**
     * ノックバック耐性
     */
    var knockbackResistance: Double = 0.0

    /**
     * 徘徊位置
     */
    private var wanderTarget: Location? = null

    /**
     * 最後に徘徊位置を更新したTick
     */
    private var lastWanderUpdateTick: Int = 0

    /**
     * 徘徊更新間隔（tick）
     */
    private val wanderUpdateInterval: Int = 100

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
    override suspend fun despawn(player: Player) {
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
    override suspend fun playAnimation(animation: EntityAnimation) {
        if (isDead && animation != DEATH) return

        viewers.forEach { player ->
            Bukkit.getPlayer(player)?.let {
                PacketSender.sendAnimationPacket(it, entityId, animation)
            }
        }
        DebugLogger.verbose("Playing animation $animation for $mobName")
    }

    /**
     * ダメージを受ける（オーバーライド）
     */
    override suspend fun damage(amount: Double) {
        // Armor/ArmorToughnessによるダメージ軽減計算
        val reducedDamage = calculateArmorReduction(amount)

        DebugLogger.debug("$mobName took $reducedDamage damage (original: $amount, armor: $armor, toughness: $armorToughness) (${health}/${maxHealth} HP)")
        super.damage(reducedDamage)

        // ダメージアニメーション再生
        playAnimation(DAMAGE)
    }

    /**
     * Minecraftのダメージ軽減計算（Armor + ArmorToughness）
     *
     * 計算式：
     * 1. defensePoints = armor値（0-30）
     * 2. toughness = armorToughness値（0-20）
     * 3. damageReduction = defensePoints - (damage / (2 + toughness / 4))
     * 4. finalDamage = damage * (1 - min(20, max(damageReduction / 5, damageReduction - damage / (2 + toughness / 4))) / 25)
     *
     * 簡易版: damage * (1 - min(20, armor) / 25)
     */
    private fun calculateArmorReduction(damage: Double): Double {
        if (armor <= 0.0) return damage

        // Minecraftの防具軽減計算（簡易版）
        // armor値による軽減率: 1ポイントあたり4%（最大20ポイント = 80%軽減）
        val armorReduction = min(20.0, armor) / 25.0  // 最大80%軽減

        // ArmorToughnessによる追加軽減（高ダメージに対してより効果的）
        val toughnessBonus = if (damage > 10.0 && armorToughness > 0.0) {
            val excessDamage = damage - 10.0
            val toughnessEffect = armorToughness / 20.0  // 0.0-1.0
            min(0.2, excessDamage / 100.0 * toughnessEffect)  // 最大20%追加軽減
        } else {
            0.0
        }

        val totalReduction = (armorReduction + toughnessBonus).coerceIn(0.0, 0.8)  // 最大80%軽減
        return damage * (1.0 - totalReduction)
    }

    /**
     * 殺す（オーバーライド）
     */
    override suspend fun kill() {
        DebugLogger.info("$mobName was killed")

        // デスアニメーション再生
        playAnimation(DEATH)

        super.kill()
    }

    /**
     * 更新処理
     */
    override suspend fun tick() {
        super.tick()

        // AI処理
        if (hasAI && !isDead) {
            tickAI()
        }
    }

    /**
     * AI処理
     */
    private suspend fun tickAI() {
        // ターゲット検索
        if (ticksLived - lastTargetCheckTick >= targetSearchInterval) {
            searchTarget()
            lastTargetCheckTick = ticksLived
        }

        // ターゲットの有効性チェック
        if (currentTarget != null) {
            if (currentTarget!!.isDead || !isInRange(currentTarget!!, followRange)) {
                currentTarget = null
                aiState = AIState.IDLE
            }
        }

        // 状態に応じた処理
        when (aiState) {
            AIState.IDLE -> tickIdle()
            AIState.TARGET -> tickTarget()
            AIState.ATTACK -> tickAttack()
            AIState.WANDER -> tickWander()
        }
    }

    /**
     * ターゲット検索
     */
    private fun searchTarget() {
        val world = location.world ?: return

        // 範囲内のプレイヤーを検索
        val nearbyPlayers = world.getNearbyEntities(
            location,
            followRange,
            followRange,
            followRange
        ).filterIsInstance<Player>()
            .filter { !it.isDead && it.gameMode != org.bukkit.GameMode.SPECTATOR && it.gameMode != org.bukkit.GameMode.CREATIVE }

        if (nearbyPlayers.isNotEmpty()) {
            val newTarget = nearbyPlayers.minByOrNull { it.location.distance(location) }

            // ターゲット変更イベント発火＆キャンセルチェック
            val targetEvent = EventUtil.callEventOrNull(
                PacketMobTargetEvent(this, currentTarget, newTarget, PacketMobTargetEvent.TargetReason.CLOSEST_PLAYER)
            ) ?: return

            currentTarget = targetEvent.newTarget
            if (currentTarget != null) {
                aiState = AIState.TARGET
                DebugLogger.verbose("$mobName found target: ${(currentTarget as? Player)?.name}")
            }
        }
    }

    /**
     * 待機状態の処理
     */
    private suspend fun tickIdle() {
        // 一定確率で徘徊モードに移行
        if (Math.random() < 0.01) {
            aiState = AIState.WANDER
            wanderTarget = getRandomWanderLocation()
        }
    }

    /**
     * ターゲット追跡状態の処理
     */
    private suspend fun tickTarget() {
        val target = currentTarget ?: return

        val distance = location.distance(target.location)

        // 攻撃範囲内なら攻撃モードに移行
        if (distance <= attackRange) {
            aiState = AIState.ATTACK
            return
        }

        // ターゲットに向かって移動
        moveTowards(target.location)
    }

    /**
     * 攻撃状態の処理
     */
    private suspend fun tickAttack() {
        val target = currentTarget ?: run {
            aiState = AIState.IDLE
            return
        }

        val distance = location.distance(target.location)

        // 攻撃範囲外なら追跡モードに戻る
        if (distance > attackRange) {
            aiState = AIState.TARGET
            return
        }

        // 攻撃クールダウンチェック
        if (ticksLived - lastAttackTick >= attackCooldown) {
            performAttack(target)
            lastAttackTick = ticksLived
        }

        // ターゲットの方を向く
        lookAt(target.location)
    }

    /**
     * 徘徊状態の処理
     */
    private suspend fun tickWander() {
        val wander = wanderTarget

        // 徘徊先がない、または到達した場合
        if (wander == null || location.distance(wander) < 1.0) {
            aiState = AIState.IDLE
            wanderTarget = null
            return
        }

        // 徘徊先に向かって移動
        moveTowards(wander)
    }

    /**
     * 指定位置に向かって移動
     */
    private suspend fun moveTowards(target: Location) {
        val direction = target.toVector().subtract(location.toVector()).normalize()
        val distance = min(movementSpeed, location.distance(target))

        val deltaX = direction.x * distance
        val deltaY = 0.0  // Y軸は重力に任せる
        val deltaZ = direction.z * distance

        move(deltaX, deltaY, deltaZ)
    }

    /**
     * 指定位置を向く
     */
    private fun lookAt(target: Location) {
        val direction = target.toVector().subtract(location.toVector())
        location.direction = direction
    }

    /**
     * 攻撃実行
     */
    private suspend fun performAttack(target: Entity) {
        DebugLogger.debug("$mobName attacks ${(target as? Player)?.name ?: target.type.name}")

        // MobInstance取得
        val instance = com.github.azuazu3939.unique.Unique.instance.mobManager.getMobInstance(this)
        val damage = instance?.definition?.damage?.toDouble() ?: 5.0

        // 攻撃イベント発火＆キャンセルチェック
        val attackEvent = EventUtil.callEventOrNull(PacketMobAttackEvent(this, target, damage)) ?: run {
            DebugLogger.verbose("$mobName attack cancelled by event")
            return
        }

        // ダメージを与える
        if (target is LivingEntity) {
            target.damage(attackEvent.damage)
        }

        // 攻撃アニメーション再生
        playAnimation(EntityAnimation.SWING_MAIN_HAND)

        // OnAttackスキルトリガー実行
        instance?.let {
            com.github.azuazu3939.unique.Unique.instance.mobManager.executeSkillTriggers(
                this,
                it.definition.skills.onAttack,
                PacketMobSkillEvent.SkillTriggerType.ON_ATTACK
            )
        }
    }

    /**
     * ランダムな徘徊位置を取得
     */
    private fun getRandomWanderLocation(): Location {
        val randomX = (Math.random() - 0.5) * 10
        val randomZ = (Math.random() - 0.5) * 10
        return location.clone().add(randomX, 0.0, randomZ)
    }

    /**
     * 範囲内かチェック
     */
    private fun isInRange(entity: Entity, range: Double): Boolean {
        return location.distance(entity.location) <= range
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
        private var customNameVisible: Boolean = true
        private var hasAI: Boolean = true
        private var hasGravity: Boolean = true
        private var isGlowing: Boolean = false
        private var isInvisible: Boolean = false

        // AI関連
        private var movementSpeed: Double = 0.25
        private var followRange: Double = 16.0
        private var attackRange: Double = 2.0
        private var attackCooldown: Int = 20
        private var targetSearchInterval: Int = 20
        private var knockbackResistance: Double = 0.0

        fun health(health: Double) = apply { this.health = health }
        fun maxHealth(maxHealth: Double) = apply { this.maxHealth = maxHealth }
        fun armor(armor: Double) = apply { this.armor = armor.coerceIn(0.0, 30.0) }
        fun armorToughness(toughness: Double) = apply { this.armorToughness = toughness.coerceIn(0.0, 20.0) }
        fun customNameVisible(visible: Boolean) = apply { this.customNameVisible = visible }
        fun hasAI(ai: Boolean) = apply { this.hasAI = ai }
        fun hasGravity(gravity: Boolean) = apply { this.hasGravity = gravity }
        fun glowing(glowing: Boolean) = apply { this.isGlowing = glowing }
        fun invisible(invisible: Boolean) = apply { this.isInvisible = invisible }

        // AI設定
        fun movementSpeed(speed: Double) = apply { this.movementSpeed = speed }
        fun followRange(range: Double) = apply { this.followRange = range }
        fun attackRange(range: Double) = apply { this.attackRange = range }
        fun attackCooldown(cooldown: Int) = apply { this.attackCooldown = cooldown }
        fun targetSearchInterval(interval: Int) = apply { this.targetSearchInterval = interval }
        fun knockbackResistance(resistance: Double) = apply { this.knockbackResistance = resistance }

        fun build(): PacketMob {
            val mob = PacketMob(entityId, uuid, entityType, location, mobName)
            mob.health = health
            mob.maxHealth = maxHealth
            mob.armor = armor
            mob.armorToughness = armorToughness
            mob.customNameVisible = customNameVisible
            mob.hasAI = hasAI
            mob.hasGravity = hasGravity
            mob.isGlowing = isGlowing
            mob.isInvisible = isInvisible

            // AI設定
            mob.movementSpeed = movementSpeed
            mob.followRange = followRange
            mob.attackRange = attackRange
            mob.attackCooldown = attackCooldown
            mob.targetSearchInterval = targetSearchInterval
            mob.knockbackResistance = knockbackResistance

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
