package com.github.azuazu3939.unique.entity.physics

import org.bukkit.World
import org.bukkit.util.Vector
import kotlin.math.ceil
import kotlin.math.floor

/**
 * CollisionDetector - スウィープテストを使った連続的な衝突検出
 *
 * MinecraftのエンティティとBlockの衝突を正確に検出
 */
object CollisionDetector {

    /**
     * 移動による衝突を検出し、実際に移動可能な量を計算（スウィープテスト）
     * VanillaSource実装に基づく処理順序
     *
     * @param world ワールド
     * @param entityAABB エンティティの境界ボックス
     * @param motion 移動ベクトル
     * @return 実際に移動可能なベクトルと衝突情報
     */
    fun sweepTest(world: World, entityAABB: AABB, motion: Vector): CollisionResult {
        var deltaX = motion.x
        var deltaY = motion.y
        var deltaZ = motion.z

        // 移動範囲を含むAABBを作成
        val sweepAABB = entityAABB.expand(deltaX, deltaY, deltaZ)

        // 移動範囲内のブロックAABBを取得
        val blockAABBs = getBlockAABBsInRange(world, sweepAABB)
        var currentAABB = entityAABB

        // 衝突フラグ
        var collidedX = false
        var collidedY = false
        var collidedZ = false

        // Y軸の衝突を最初に計算（重力方向）
        if (deltaY != 0.0) {
            val originalDeltaY = deltaY
            for (blockAABB in blockAABBs) {
                if (kotlin.math.abs(deltaY) < 1.0E-7) break
                deltaY = blockAABB.calculateYOffset(currentAABB, deltaY)
            }
            if (originalDeltaY != deltaY) {
                collidedY = true
            }
            if (deltaY != 0.0) {
                currentAABB = currentAABB.offset(0.0, deltaY, 0.0)
            }
        }

        // VanillaSource方式: X軸とZ軸のうち、移動量が小さい方を先に処理
        val xSmaller = kotlin.math.abs(deltaX) < kotlin.math.abs(deltaZ)

        // 最初の水平軸を処理（小さい方）
        if (xSmaller && deltaZ != 0.0) {
            val originalDeltaZ = deltaZ
            for (blockAABB in blockAABBs) {
                if (kotlin.math.abs(deltaZ) < 1.0E-7) break
                deltaZ = blockAABB.calculateZOffset(currentAABB, deltaZ)
            }
            if (originalDeltaZ != deltaZ) {
                collidedZ = true
            }
            if (deltaZ != 0.0) {
                currentAABB = currentAABB.offset(0.0, 0.0, deltaZ)
            }
        }

        // X軸の処理
        if (deltaX != 0.0) {
            val originalDeltaX = deltaX
            for (blockAABB in blockAABBs) {
                if (kotlin.math.abs(deltaX) < 1.0E-7) break
                deltaX = blockAABB.calculateXOffset(currentAABB, deltaX)
            }
            if (originalDeltaX != deltaX) {
                collidedX = true
            }
            if (!xSmaller && deltaX != 0.0) {
                currentAABB = currentAABB.offset(deltaX, 0.0, 0.0)
            }
        }

        // 2番目の水平軸を処理（大きい方）
        if (!xSmaller && deltaZ != 0.0) {
            val originalDeltaZ = deltaZ
            for (blockAABB in blockAABBs) {
                if (kotlin.math.abs(deltaZ) < 1.0E-7) break
                deltaZ = blockAABB.calculateZOffset(currentAABB, deltaZ)
            }
            if (originalDeltaZ != deltaZ) {
                collidedZ = true
            }
        }

        // 地面判定
        val isOnGround = collidedY && motion.y < 0

        return CollisionResult(
            Vector(deltaX, deltaY, deltaZ),
            collidedX,
            collidedY,
            collidedZ,
            isOnGround
        )
    }

    /**
     * 指定範囲内のブロックAABBを取得
     */
    private fun getBlockAABBsInRange(world: World, aabb: AABB): List<AABB> {
        val blockAABBs = mutableListOf<AABB>()

        val minX = floor(aabb.minX).toInt()
        val minY = floor(aabb.minY).toInt()
        val minZ = floor(aabb.minZ).toInt()
        val maxX = ceil(aabb.maxX).toInt()
        val maxY = ceil(aabb.maxY).toInt()
        val maxZ = ceil(aabb.maxZ).toInt()

        for (x in minX until maxX) {
            for (y in minY until maxY) {
                for (z in minZ until maxZ) {
                    val block = world.getBlockAt(x, y, z)
                    if (block.type.isSolid) {
                        blockAABBs.add(AABB.fromBlock(x, y, z))
                    }
                }
            }
        }

        return blockAABBs
    }

    /**
     * エンティティが地面にいるか判定
     *
     * @param world ワールド
     * @param entityAABB エンティティの境界ボックス
     * @return 地面にいる場合true
     */
    fun isOnGround(world: World, entityAABB: AABB): Boolean {
        // 足元を少し下にずらしてチェック
        val testAABB = entityAABB.offset(0.0, -0.001, 0.0)
        val blockAABBs = getBlockAABBsInRange(world, testAABB)

        for (blockAABB in blockAABBs) {
            if (testAABB.intersects(blockAABB)) {
                return true
            }
        }

        return false
    }

    /**
     * エンティティがブロックに埋まっているか判定
     *
     * @param world ワールド
     * @param entityAABB エンティティの境界ボックス
     * @return 埋まっている場合、押し出すべきY座標
     */
    fun checkEmbedded(world: World, entityAABB: AABB): Double? {
        val blockAABBs = getBlockAABBsInRange(world, entityAABB)

        var maxEmbeddedY: Double? = null

        for (blockAABB in blockAABBs) {
            if (entityAABB.intersects(blockAABB)) {
                // 埋まっている場合、ブロックの上面を記録
                if (maxEmbeddedY == null || blockAABB.maxY > maxEmbeddedY) {
                    maxEmbeddedY = blockAABB.maxY
                }
            }
        }

        return maxEmbeddedY
    }

    /**
     * エンティティが壁を登れるか判定（段差判定）
     *
     * @param world ワールド
     * @param entityAABB エンティティの境界ボックス
     * @param motion 移動ベクトル（水平方向のみ）
     * @param maxStepHeight 登れる最大高さ
     * @return 登れる場合、登る高さ
     */
    fun calculateStepHeight(
        world: World,
        entityAABB: AABB,
        motion: Vector,
        maxStepHeight: Double
    ): Double? {
        // 水平方向のみの移動
        val horizontalMotion = Vector(motion.x, 0.0, motion.z)
        if (horizontalMotion.lengthSquared() < 0.001) return null

        // 通常の移動をテスト
        val normalResult = sweepTest(world, entityAABB, horizontalMotion)

        // 衝突していない場合は段差登りの必要なし
        if (!normalResult.collidedX && !normalResult.collidedZ) return null

        // 各段差の高さをテスト
        for (stepHeight in listOf(0.5, 1.0)) {
            if (stepHeight > maxStepHeight) break

            val stepUpMotion = Vector(motion.x, stepHeight, motion.z)
            val stepResult = sweepTest(world, entityAABB, stepUpMotion)

            // 登れる場合
            if (kotlin.math.abs(stepResult.motion.x - motion.x) < 0.001 &&
                kotlin.math.abs(stepResult.motion.z - motion.z) < 0.001
            ) {
                return stepHeight
            }
        }

        return null
    }

    /**
     * 衝突結果
     */
    data class CollisionResult(
        val motion: Vector,          // 実際に移動可能な量
        val collidedX: Boolean,      // X軸で衝突したか
        val collidedY: Boolean,      // Y軸で衝突したか
        val collidedZ: Boolean,      // Z軸で衝突したか
        val isOnGround: Boolean      // 地面にいるか
    )
}
