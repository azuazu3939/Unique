package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.entity.packet.PacketSender
import com.github.azuazu3939.unique.entity.physics.AABB
import com.github.azuazu3939.unique.entity.physics.CollisionDetector
import com.github.azuazu3939.unique.nms.getPlayerByUUID
import org.bukkit.util.Vector
import kotlin.math.*

/**
 * PacketMobの物理演算を担当するクラス
 *
 * BukkitのBoundingBoxとAABBを使用した正確な衝突判定
 * - 重力の適用
 * - 速度の管理と適用
 * - 移動時の衝突判定
 * - 地面埋まり防止
 */
class PacketMobPhysics(private val mob: PacketMob) {

    // 速度（ノックバックなど）
    var velocityX: Double = 0.0
        private set
    var velocityY: Double = 0.0
        private set
    var velocityZ: Double = 0.0
        private set

    // 接地フラグ
    var isOnGround: Boolean = false
        private set

    // 前回の位置（デバッグ用）
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0
    private var lastZ: Double = 0.0

    /**
     * 速度を追加（ノックバックなど）
     * ノックバック耐性を考慮
     */
    fun addVelocity(vx: Double, vy: Double, vz: Double) {
        val resistance = mob.knockbackResistance.coerceIn(0.0, 1.0)
        val multiplier = 1.0 - resistance

        velocityX += vx * multiplier
        velocityY += vy * multiplier
        velocityZ += vz * multiplier
    }

    /**
     * 速度を設定（上書き）
     * ノックバック耐性を考慮
     */
    fun setVelocity(vx: Double, vy: Double, vz: Double) {
        val resistance = mob.knockbackResistance.coerceIn(0.0, 1.0)
        val multiplier = 1.0 - resistance

        velocityX = vx * multiplier
        velocityY = vy * multiplier
        velocityZ = vz * multiplier
    }

    /**
     * ジャンプ処理
     *
     * 地面にいる場合のみジャンプ可能
     * jumpStrengthの値を使用（デフォルト0.42、バニラと同じ）
     */
    fun jump() {
        if (isOnGround && mob.hasGravity) {
            velocityY = mob.jumpStrength
            isOnGround = false
        }
    }

    /**
     * AI移動速度を設定（水平方向のみ）
     * 外部速度（ノックバックなど）がある場合は上書きしない
     * 現在の速度に目標速度へ近づける加速度を適用（滑らかな移動）
     */
    fun setAIVelocity(vx: Double, vz: Double) {
        // 外部速度が大きい場合は上書きしない
        if (hasExternalVelocity()) {
            return
        }

        // 目標速度へ徐々に加速（バニラライク）
        val acceleration = 0.6  // 加速度（1.0 = 即座に目標速度、0.5 = 半分ずつ近づく）

        velocityX += (vx - velocityX) * acceleration
        velocityZ += (vz - velocityZ) * acceleration
    }

    /**
     * 現在のAABBを取得
     */
    private fun getEntityAABB(): AABB {
        return getEntityAABB(mob.location.x, mob.location.y, mob.location.z)
    }

    /**
     * 指定位置でのAABBを取得
     */
    private fun getEntityAABB(x: Double, y: Double, z: Double): AABB {
        val width = mob.getEntityHitboxWidth()
        val height = mob.getEntityHitboxHeight()

        val tempLocation = mob.location.clone()
        tempLocation.x = x
        tempLocation.y = y
        tempLocation.z = z

        return AABB.fromLocation(tempLocation, width, height)
    }

    /**
     * 物理演算のメインtick処理
     */
    fun tick() {
        mob.location.world ?: return

        // 前回の位置を保存
        lastX = mob.location.x
        lastY = mob.location.y
        lastZ = mob.location.z

        // 摩擦・空気抵抗と重力を先に適用（移動前）
        applyFrictionAndGravity()

        // 速度を適用（衝突判定付き）
        if (hasVelocity()) {
            applyVelocityWithCollision()
        }
    }

    /**
     * 摩擦・空気抵抗と重力を適用（移動前）
     *
     * 参考実装に基づく処理順序
     */
    private fun applyFrictionAndGravity() {
        if (isOnGround) {
            // 地面にいる場合、地面摩擦を適用
            if (velocityX != 0.0) {
                velocityX *= 0.6
            }
            if (velocityZ != 0.0) {
                velocityZ *= 0.6
            }
        }

        if (mob.hasGravity) {
            velocityY = (velocityY - 0.08) * 0.98
        }

        // 微小な速度はゼロにする
        if (abs(velocityX) < 0.003) velocityX = 0.0
        if (abs(velocityY) < 0.003) velocityY = 0.0
        if (abs(velocityZ) < 0.003) velocityZ = 0.0
    }

    /**
     * 指定された速度ベクトルで移動を試みる（衝突判定付き）
     *
     * velocityを引数に取り、衝突判定を処理しながら移動を試みます。
     * 途中で衝突した場合は、その手前まで移動します。
     *
     * @param velocity 移動速度ベクトル
     * @return 衝突判定の結果（実際に移動した量と衝突情報）
     */
    fun move(velocity: Vector): CollisionDetector.CollisionResult {
        val world = mob.location.world ?: return CollisionDetector.CollisionResult(
            Vector(0.0, 0.0, 0.0),
            collidedX = false,
            collidedY = false,
            collidedZ = false,
            isOnGround = isOnGround
        )

        // 前回の位置を保存
        lastX = mob.location.x
        lastY = mob.location.y
        lastZ = mob.location.z

        // 現在のAABBを取得
        val entityAABB = getEntityAABB()

        // 衝突判定を実行（スウィープテスト）
        val result = CollisionDetector.sweepTest(world, entityAABB, velocity)

        // 実際に移動可能な量だけ移動（衝突した場合はその手前まで）
        mob.location.x += result.motion.x
        mob.location.y += result.motion.y
        mob.location.z += result.motion.z

        // 地面判定を更新
        isOnGround = result.isOnGround

        // 位置が変更された場合、テレポートパケット送信
        if (hasMoved()) {
            sendMovementPackets()
        }

        return result
    }

    /**
     * 内部速度を衝突判定付きで適用
     */
    private fun applyVelocityWithCollision() {
        // 移動ベクトルを作成
        val motion = Vector(velocityX, velocityY, velocityZ)

        // move関数を使用して移動
        val result = move(motion)

        // 水平方向に衝突し、地面にいる場合は段差登りを試みる
        if ((result.collidedX || result.collidedZ) && result.isOnGround && mob.stepHeight > 0.0) {
            tryStepUp(motion)
        } else {
            // 段差登りができない場合、衝突した軸の速度をリセット
            if (result.collidedX) {
                velocityX = 0.0
            }
            if (result.collidedY) {
                velocityY = 0.0
            }
            if (result.collidedZ) {
                velocityZ = 0.0
            }
        }
    }

    /**
     * 段差を登る処理（バニラ準拠）
     *
     * 水平方向に衝突した場合、stepHeight以下の段差を登れるかチェックし、
     * 登れる場合は段差の高さ分だけY座標を上げる
     */
    private fun tryStepUp(originalMotion: Vector) {
        val world = mob.location.world ?: return

        // 現在のAABBを取得
        val entityAABB = getEntityAABB()

        // 段差の高さを計算
        val stepHeight = CollisionDetector.calculateStepHeight(
            world,
            entityAABB,
            originalMotion,
            mob.stepHeight
        )

        if (stepHeight != null && stepHeight > 0.0) {
            // 段差を登る：Y座標を段差の高さ分だけ上げて、水平方向に移動
            val stepMotion = Vector(originalMotion.x, stepHeight, originalMotion.z)
            val stepResult = move(stepMotion)

            // 段差登りに成功した場合、速度をリセット（Y方向のみ、水平方向は維持）
            if (stepResult.collidedY) {
                velocityY = 0.0
            }
            if (stepResult.collidedX) {
                velocityX = 0.0
            }
            if (stepResult.collidedZ) {
                velocityZ = 0.0
            }
        } else {
            // 段差登りができない場合、水平方向の速度をリセット
            if (originalMotion.x != 0.0) {
                velocityX = 0.0
            }
            if (originalMotion.z != 0.0) {
                velocityZ = 0.0
            }
        }
    }

    /**
     * ノックバック効果を適用（バニラMinecraft準拠）
     *
     * バニラの実装:
     * - 水平方向: 方向ベクトル * 0.4、既存の速度が小さい場合のみ上書き
     * - 垂直方向: 0.4（地面にいる場合のみ）、既存の速度との最大値を取る
     *
     * @param sourceX ノックバック元のX位置
     * @param sourceZ ノックバック元のZ位置
     * @param strength 水平方向のノックバック強度（デフォルト: 0.4）
     * @param verticalStrength 垂直方向の強度（デフォルト: 0.4）
     */
    fun applyKnockback(sourceX: Double, sourceZ: Double, strength: Double = 0.4, verticalStrength: Double = 0.4) {
        // ノックバック方向を計算
        val dx = mob.location.x - sourceX
        val dz = mob.location.z - sourceZ
        val distance = sqrt(dx * dx + dz * dz)

        var kbX: Double
        var kbZ: Double

        if (distance < 0.001) {
            // 距離が0に近い場合はランダムな方向に
            val angle = Math.random() * 2 * Math.PI
            kbX = cos(angle) * strength
            kbZ = sin(angle) * strength
        } else {
            // 正規化してノックバック速度を計算
            kbX = (dx / distance) * strength
            kbZ = (dz / distance) * strength
        }

        // 水平方向: 既存の速度の大きさと比較し、大きい方を採用（バニラ準拠）
        val currentHorizontalSpeed = sqrt(velocityX * velocityX + velocityZ * velocityZ)
        val knockbackHorizontalSpeed = sqrt(kbX * kbX + kbZ * kbZ)

        if (knockbackHorizontalSpeed > currentHorizontalSpeed) {
            velocityX = kbX
            velocityZ = kbZ
        }

        // 垂直方向: 地面にいる場合のみ適用、既存の速度との最大値を取る（バニラ準拠）
        if (isOnGround) {
            velocityY = max(velocityY, verticalStrength)
            isOnGround = false
        }

        // ノックバック耐性を適用
        val resistance = mob.knockbackResistance.coerceIn(0.0, 1.0)
        if (resistance > 0.0) {
            val multiplier = 1.0 - resistance
            velocityX *= multiplier
            velocityY *= multiplier
            velocityZ *= multiplier
        }
    }

    /**
     * 速度があるかチェック
     */
    fun hasVelocity(): Boolean {
        return abs(velocityX) > 0.003 || abs(velocityY) > 0.003 || abs(velocityZ) > 0.003
    }

    /**
     * 外部からの速度（ノックバックなど）が有効かチェック
     */
    fun hasExternalVelocity(): Boolean {
        val horizontalVelocity = sqrt(velocityX * velocityX + velocityZ * velocityZ)
        return horizontalVelocity > 0.05
    }

    /**
     * 位置が移動したかチェック
     */
    private fun hasMoved(): Boolean {
        val dx = mob.location.x - lastX
        val dy = mob.location.y - lastY
        val dz = mob.location.z - lastZ
        return abs(dx) > 0.001 || abs(dy) > 0.001 || abs(dz) > 0.001
    }

    /**
     * 移動パケットを全viewerに送信
     */
    private fun sendMovementPackets() {
        mob.viewers.forEach { uuid ->
            getPlayerByUUID(uuid)?.let { player ->
                PacketSender.sendTeleportPacket(player, mob.entityId, mob.location)
            }
        }
    }

    /**
     * デバッグ情報を取得
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== PacketMobPhysics Debug ===")
            appendLine("Position: ${String.format("%.2f, %.2f, %.2f", mob.location.x, mob.location.y, mob.location.z)}")
            appendLine("Velocity: ${String.format("%.3f, %.3f, %.3f", velocityX, velocityY, velocityZ)}")
            appendLine("OnGround: $isOnGround")
            appendLine("HasVelocity: ${hasVelocity()}")
            appendLine("HasExternalVelocity: ${hasExternalVelocity()}")
            appendLine("KnockbackResistance: ${mob.knockbackResistance}")
            appendLine("StepHeight: ${mob.stepHeight}")
        }
    }
}
