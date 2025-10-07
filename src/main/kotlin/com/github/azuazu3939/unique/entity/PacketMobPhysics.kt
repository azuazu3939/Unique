package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.entity.packet.PacketSender
import com.github.azuazu3939.unique.entity.physics.AABB
import com.github.azuazu3939.unique.entity.physics.CollisionDetector
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import kotlin.math.abs

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
    suspend fun tick() {
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

        // 重力と空気抵抗を適用（地面でも空中でも）
        if (mob.hasGravity) {
            velocityY = (velocityY - 0.08) * 0.98
        } else if (velocityY != 0.0) {
            velocityY *= 0.98
        }

        // 終端速度を制限
        val terminalVelocity = 3.92
        if (velocityY < -terminalVelocity) {
            velocityY = -terminalVelocity
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
    private suspend fun applyVelocityWithCollision() {
        // 移動ベクトルを作成
        val motion = Vector(velocityX, velocityY, velocityZ)

        // move関数を使用して移動
        val result = move(motion)

        // 衝突した場合、その軸の速度をリセット
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

    /**
     * ノックバック効果を適用
     *
     * @param sourceX ノックバック元のX位置
     * @param sourceZ ノックバック元のZ位置
     * @param strength ノックバック強度
     * @param verticalStrength 垂直方向の強度
     */
    fun applyKnockback(sourceX: Double, sourceZ: Double, strength: Double, verticalStrength: Double = 0.4) {
        // ノックバック方向を計算
        val dx = mob.location.x - sourceX
        val dz = mob.location.z - sourceZ
        val distance = kotlin.math.sqrt(dx * dx + dz * dz)

        if (distance < 0.001) {
            // 距離が0に近い場合はランダムな方向に
            val angle = Math.random() * 2 * Math.PI
            addVelocity(
                kotlin.math.cos(angle) * strength,
                verticalStrength,
                kotlin.math.sin(angle) * strength
            )
        } else {
            // 正規化してノックバック
            val normalizedX = dx / distance
            val normalizedZ = dz / distance

            addVelocity(
                normalizedX * strength,
                verticalStrength,
                normalizedZ * strength
            )
        }

        // ノックバック時は接地フラグをリセット
        isOnGround = false
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
        val horizontalVelocity = kotlin.math.sqrt(velocityX * velocityX + velocityZ * velocityZ)
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
            Bukkit.getPlayer(uuid)?.let { player ->
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
