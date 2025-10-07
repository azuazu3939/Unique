package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.event.PacketMobTargetEvent
import com.github.azuazu3939.unique.util.EventUtil
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Entity
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
    suspend fun tick() {
        // ターゲット検索
        if (mob.ticksLived - lastTargetCheckTick >= mob.targetSearchInterval) {
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

        val followRangeSquared = mob.followRange * mob.followRange
        val nearbyPlayers = world.players
            .filter { player ->
                !player.isDead &&
                        player.gameMode != GameMode.SPECTATOR &&
                        player.gameMode != GameMode.CREATIVE &&
                        player.location.world == world &&
                        player.location.distanceSquared(mob.location) <= followRangeSquared
            }

        if (nearbyPlayers.isNotEmpty()) {
            val newTarget = nearbyPlayers.minByOrNull { it.location.distance(mob.location) }

            val targetEvent = EventUtil.callEventOrNull(
                PacketMobTargetEvent(mob, currentTarget, newTarget, PacketMobTargetEvent.TargetReason.CLOSEST_PLAYER)
            ) ?: return

            currentTarget = targetEvent.newTarget
            if (currentTarget != null) {
                aiState = AIState.TARGET
            }
        }
    }

    /**
     * 待機状態の処理
     */
    private suspend fun tickIdle() {
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

        val distance = mob.location.distance(target.location)

        if (distance <= mob.attackRange) {
            aiState = AIState.ATTACK
            return
        }

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

        val distance = mob.location.distance(target.location)

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
    private suspend fun tickWander() {
        val wander = wanderTarget

        if (wander == null || mob.location.distance(wander) < 1.0) {
            aiState = AIState.IDLE
            wanderTarget = null
            return
        }

        moveTowards(wander)
    }

    /**
     * 指定位置に向かって移動
     */
    private suspend fun moveTowards(target: Location) {
        // 外部速度（ノックバック等）が大きい場合、AI移動を抑制
        if (physics.hasExternalVelocity()) {
            return
        }

        val direction = target.toVector().subtract(mob.location.toVector()).normalize()
        val deltaX = direction.x * mob.movementSpeed
        val deltaZ = direction.z * mob.movementSpeed

        // 移動方向に視点を向ける
        if (mob.lookAtMovementDirection) {
            val targetLookLocation = mob.location.clone().add(direction.multiply(1.0))
            lookAt(targetLookLocation)
        }

        val world = mob.location.world ?: return
        val currentY = mob.location.y

        // Mobの当たり判定の半径を取得
        val hitboxRadius = mob.getEntityHitboxWidth() / 2.0

        // 移動先の中心座標
        val nextCenterX = mob.location.x + deltaX
        val nextCenterZ = mob.location.z + deltaZ

        // 移動先のMobの当たり判定の範囲（端の座標）
        val minX = (nextCenterX - hitboxRadius).toInt()
        val maxX = (nextCenterX + hitboxRadius).toInt()
        val minZ = (nextCenterZ - hitboxRadius).toInt()
        val maxZ = (nextCenterZ + hitboxRadius).toInt()

        // 前方の壁をチェック（現在位置と異なる場合のみ）
        var canMoveHorizontally = true
        if (minX != mob.location.blockX || maxX != mob.location.blockX ||
            minZ != mob.location.blockZ || maxZ != mob.location.blockZ) {
            // 当たり判定の範囲内で最も高い壁をチェック
            var maxWallHeight = 0.0
            for (checkX in minX..maxX) {
                for (checkZ in minZ..maxZ) {
                    val wallHeight = checkWallHeight(world, checkX, checkZ, currentY)
                    if (wallHeight > maxWallHeight) {
                        maxWallHeight = wallHeight
                    }
                }
            }

            if (maxWallHeight > mob.wallClimbHeight) {
                canMoveHorizontally = false
            }
        }

        // 移動先の地面の高さをチェック（水平移動が可能な場合のみ）
        val currentFootY = currentY.toInt()
        var targetGroundY: Double? = null

        if (canMoveHorizontally) {
            // 当たり判定の範囲内で最も高い地面を探す（下方向に3ブロック、上方向に2ブロック）
            for (i in -3..2) {
                val checkY = currentFootY + i

                // 当たり判定の範囲内の全ブロックをチェック
                var hasGround = false
                for (checkX in minX..maxX) {
                    for (checkZ in minZ..maxZ) {
                        val block = world.getBlockAt(checkX, checkY, checkZ)
                        val blockAbove = world.getBlockAt(checkX, checkY + 1, checkZ)

                        if (block.type.isSolid && !blockAbove.type.isSolid) {
                            hasGround = true
                            break
                        }
                    }
                    if (hasGround) break
                }

                if (hasGround) {
                    targetGroundY = checkY + 1.0
                    break
                }
            }
        } else {
            // 水平移動できない場合は現在位置の地面を探す
            for (i in -3..2) {
                val checkY = currentFootY + i
                val block = world.getBlockAt(mob.location.blockX, checkY, mob.location.blockZ)
                val blockAbove = world.getBlockAt(mob.location.blockX, checkY + 1, mob.location.blockZ)

                if (block.type.isSolid && !blockAbove.type.isSolid) {
                    targetGroundY = checkY + 1.0
                    break
                }
            }
        }

        val deltaY = if (targetGroundY != null) {
            val diff = targetGroundY - currentY
            when {
                diff > 0.05 && diff <= 1.0 -> diff
                diff > 1.0 -> 0.3
                diff < -0.2 -> -0.3
                else -> 0.0
            }
        } else {
            -0.5
        }

        // 移動先で通れる空間があるかチェック（水平移動がある場合のみ）
        if (canMoveHorizontally && (deltaX != 0.0 || deltaZ != 0.0)) {
            val destinationY = currentY + deltaY
            if (!physics.checkClearance(world, nextCenterX, nextCenterZ, destinationY)) {
                return
            }
        }

        mob.move(deltaX, deltaY, deltaZ)
    }

    /**
     * 前方の壁の高さをチェック
     */
    private fun checkWallHeight(world: org.bukkit.World, x: Int, z: Int, currentY: Double): Double {
        val startY = currentY.toInt()
        val hitboxWidth = mob.getEntityHitboxWidth()
        val hitboxHeight = mob.getEntityHitboxHeight()
        val radius = (hitboxWidth / 2.0).toInt()

        var maxWallHeight = 0.0

        for (offsetX in -radius..radius) {
            for (offsetZ in -radius..radius) {
                val checkX = x + offsetX
                val checkZ = z + offsetZ
                var wallHeightAtPos = 0.0

                for (i in 0..hitboxHeight.toInt() + 2) {
                    val checkY = startY + i
                    val block = world.getBlockAt(checkX, checkY, checkZ)

                    if (block.type.isSolid) {
                        wallHeightAtPos = i + 1.0
                    } else {
                        break
                    }
                }

                if (wallHeightAtPos > maxWallHeight) {
                    maxWallHeight = wallHeightAtPos
                }
            }
        }

        return maxWallHeight
    }

    /**
     * 指定位置を向く
     */
    private suspend fun lookAt(target: Location) {
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
    private suspend fun resetPitch() {
        mob.updateRotation(mob.location.yaw, 0f)
    }

    /**
     * bodyの向きをheadに合わせる
     */
    private fun syncBodyRotation() {
        // 現在のheadのyawをbodyにも適用（パケット送信は不要、次回の移動で自動的に同期）
        // この処理は将来的にbodyRotationを独立管理する際に使用
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
        return mob.location.distance(entity.location) <= range
    }
}
