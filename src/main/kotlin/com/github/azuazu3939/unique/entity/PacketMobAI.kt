package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.entity.physics.AABB
import com.github.azuazu3939.unique.entity.physics.CollisionDetector
import com.github.azuazu3939.unique.event.PacketMobTargetEvent
import com.github.azuazu3939.unique.nms.distanceTo
import com.github.azuazu3939.unique.nms.distanceToAsync
import com.github.azuazu3939.unique.nms.getLocationAsync
import com.github.azuazu3939.unique.nms.getPlayersAsync
import com.github.azuazu3939.unique.util.EventUtil
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * PacketMobのAIを担当するクラス
 *
 * - ターゲット検索
 * - 移動処理
 * - 攻撃判定
 * - 徘徊処理
 */
class PacketMobAI(private val mob: PacketMob, private val physics: PacketMobPhysics) {

    // AI状態
    var aiState: AIState = AIState.IDLE

    // 現在のターゲット
    var currentTarget: Entity? = null

    // 最後にターゲットをチェックしたTick
    var lastTargetCheckTick: Int = 0

    // 最後に攻撃したTick
    var lastAttackTick: Int = 0

    // 徘徊位置
    private var wanderTarget: Location? = null

    // 最後に徘徊位置を更新したTick
    private var lastWanderUpdateTick: Int = 0

    // 最後にbodyの向きを同期したTick
    private var lastBodySyncTick: Int = 0

    // 徘徊更新間隔（tick）
    private val wanderUpdateInterval: Int = 100

    // body同期間隔（tick）
    private val bodySyncInterval: Int = 10

    /**
     * AI処理
     */
    fun tick() {
        if ((mob.ticksLived - lastTargetCheckTick) % mob.targetSearchInterval == 0) {
            searchTarget()
            lastTargetCheckTick = mob.ticksLived
        }

        // ターゲットの有効性チェック
        if (currentTarget != null) {
            if (currentTarget!!.isDead || !isInRange(currentTarget!!, mob.followRange)) {
                currentTarget = null
                aiState = AIState.IDLE
                resetPitch()
            }
        }

        // 状態に応じた処理
        when (aiState) {
            AIState.IDLE -> tickIdle()
            AIState.TARGET -> tickTarget()
            AIState.ATTACK -> tickAttack()
            AIState.WANDER -> tickWander()
        }

        // 定期的にbodyの向きをheadに合わせる
        if (mob.ticksLived - lastBodySyncTick >= bodySyncInterval) {
            syncBodyRotation()
            lastBodySyncTick = mob.ticksLived
        }
    }

    /**
     * ターゲット検索
     */
    private fun searchTarget() {
        val world = mob.location.world ?: return

        val followRange = mob.followRange
        val nearbyPlayers = world.getPlayersAsync()
            .filter { player ->
                        player.world.name == world.name &&
                        player.distanceToAsync(mob.location) <= followRange
            }

        if (nearbyPlayers.isNotEmpty()) {
            val newTarget = nearbyPlayers.minByOrNull { it.distanceToAsync(mob.location) }
            EventUtil.callEventOrNull(
                PacketMobTargetEvent(mob, PacketMobTargetEvent.TargetReason.CLOSEST_PLAYER
                )
            ) ?: return

            currentTarget = newTarget
            if (currentTarget != null) {
                aiState = AIState.TARGET
            }
        }
    }

    /**
     * 待機状態の処理
     */
    private fun tickIdle() {
        if (Math.random() < 0.01) {
            aiState = AIState.WANDER
            wanderTarget = getRandomWanderLocation()
        }
    }

    /**
     * ターゲット追跡状態の処理
     */
    private fun tickTarget() {
        val target = currentTarget ?: return

        val distance = mob.location.distanceTo(target.getLocationAsync())

        if (distance <= mob.attackRange) {
            aiState = AIState.ATTACK
            return
        }

        moveTowards(target.getLocationAsync())
    }

    /**
     * 攻撃状態の処理
     */
    private fun tickAttack() {
        val target = currentTarget ?: run {
            aiState = AIState.IDLE
            return
        }

        val distance = mob.location.distanceTo(target.getLocationAsync())

        if (distance > mob.attackRange) {
            aiState = AIState.TARGET
            return
        }

        if (mob.ticksLived - lastAttackTick >= mob.attackCooldown) {
            mob.performAttack(target)
            lastAttackTick = mob.ticksLived
        }

        lookAt(target.location)
    }

    /**
     * 徘徊状態の処理
     */
    private fun tickWander() {
        val wander = wanderTarget

        if (wander == null || mob.location.distanceTo(wander) < 1.0) {
            aiState = AIState.IDLE
            wanderTarget = null
            return
        }

        moveTowards(wander)
    }

    /**
     * 指定位置に向かって移動（バニラ準拠・速度ベース）
     */
    private fun moveTowards(target: Location) {
        // 外部速度（ノックバック等）が大きい場合、AI移動を抑制
        if (physics.hasExternalVelocity()) {
            return
        }

        // 移動方向を計算
        val direction = target.toVector().subtract(mob.location.toVector())
        direction.y = 0.0  // 水平方向のみ

        if (direction.lengthSquared() < 0.0001) {
            return  // 目標に到達
        }

        direction.normalize()

        // 移動方向に視点を向ける
        if (mob.lookAtMovementDirection) {
            val targetLookLocation = mob.location.clone().add(direction.multiply(1.0))
            lookAt(targetLookLocation)
        }

        // AI移動速度を物理演算に追加
        var moveX = direction.x * mob.movementSpeed
        var moveZ = direction.z * mob.movementSpeed

        // ジャンプ判定（地面にいて、障害物があり、stepHeightより高い場合）
        if (mob.stepHeight >= 0.0 && physics.isOnGround && checkAndJump(Vector(moveX, 0.0, moveZ))) {
            moveX /= 2
            moveZ /= 2
        }

        // 現在の速度に上書き（AI移動は速度を直接設定）
        physics.setAIVelocity(moveX, moveZ)
    }

    /**
     * ジャンプが必要かチェックし、必要な場合はジャンプする
     */
    private fun checkAndJump(motion: Vector): Boolean {
        val world = mob.location.world ?: return false

        // 現在のAABBを取得
        val width = mob.getEntityHitboxWidth()
        val height = mob.getEntityHitboxHeight()
        val entityAABB = AABB.fromLocation(mob.location, width, height)

        // ジャンプが必要かチェック（stepHeightとjumpStrengthを渡す）
        if (CollisionDetector.shouldJump(world, entityAABB, motion, mob.stepHeight, mob.jumpStrength)) {
            physics.jump()
            return true
        }
        return false
    }

    /**
     * 指定位置を向く
     */
    private fun lookAt(target: Location) {
        val direction = target.toVector().subtract(mob.location.toVector()).normalize()
        mob.location.direction = direction

        // 視線変更パケットを全ビューワーに送信
        val yaw = Math.toDegrees(atan2(-direction.x, direction.z)).toFloat()
        val pitch = Math.toDegrees(-atan2(direction.y, sqrt(direction.x * direction.x + direction.z * direction.z))).toFloat()

        mob.updateRotation(yaw, pitch)
    }

    /**
     * ピッチをリセット（水平を向く）
     */
    private fun resetPitch() {
        mob.updateRotation(mob.location.yaw, 0f)
    }

    /**
     * bodyの向きをheadに合わせる
     *
     * 定期的に呼ばれることで、頭と胴体の向きのズレを修正
     */
    private fun syncBodyRotation() {
        mob.syncBodyRotation()
    }

    /**
     * ランダムな徘徊位置を取得
     */
    private fun getRandomWanderLocation(): Location {
        val randomX = (Math.random() - 0.5) * 10
        val randomZ = (Math.random() - 0.5) * 10
        return mob.location.clone().add(randomX, 0.0, randomZ)
    }

    /**
     * 範囲内判定
     */
    private fun isInRange(entity: Entity, range: Double): Boolean {
        return mob.location.distanceTo(entity.getLocationAsync()) <= range
    }
}
