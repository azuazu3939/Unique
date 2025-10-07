package com.github.azuazu3939.unique.entity.physics

import org.bukkit.Location
import org.bukkit.util.Vector
import kotlin.math.max
import kotlin.math.min

/**
 * AABB (Axis-Aligned Bounding Box) - 軸に平行な境界ボックス
 *
 * Minecraftのエンティティとブロックの衝突判定に使用
 */
data class AABB(
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double
) {
    /**
     * 中心座標を取得
     */
    fun getCenter(): Vector {
        return Vector(
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0
        )
    }

    /**
     * サイズを取得
     */
    fun getSize(): Vector {
        return Vector(
            maxX - minX,
            maxY - minY,
            maxZ - minZ
        )
    }

    /**
     * 別のAABBと交差しているか判定
     */
    fun intersects(other: AABB): Boolean {
        return this.maxX > other.minX && this.minX < other.maxX &&
                this.maxY > other.minY && this.minY < other.maxY &&
                this.maxZ > other.minZ && this.minZ < other.maxZ
    }

    /**
     * 点が境界ボックス内にあるか判定
     */
    fun contains(x: Double, y: Double, z: Double): Boolean {
        return x in minX..maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ
    }

    /**
     * 点が境界ボックス内にあるか判定（Vector版）
     */
    fun contains(point: Vector): Boolean {
        return contains(point.x, point.y, point.z)
    }

    /**
     * 指定された方向に拡張した新しいAABBを返す
     */
    fun expand(x: Double, y: Double, z: Double): AABB {
        var newMinX = minX
        var newMaxX = maxX
        var newMinY = minY
        var newMaxY = maxY
        var newMinZ = minZ
        var newMaxZ = maxZ

        if (x < 0) newMinX += x else newMaxX += x
        if (y < 0) newMinY += y else newMaxY += y
        if (z < 0) newMinZ += z else newMaxZ += z

        return AABB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ)
    }

    /**
     * 指定された量だけ収縮した新しいAABBを返す
     */
    fun shrink(amount: Double): AABB {
        return AABB(
            minX + amount,
            minY + amount,
            minZ + amount,
            maxX - amount,
            maxY - amount,
            maxZ - amount
        )
    }

    /**
     * 指定されたオフセットで移動した新しいAABBを返す
     */
    fun offset(x: Double, y: Double, z: Double): AABB {
        return AABB(
            minX + x,
            minY + y,
            minZ + z,
            maxX + x,
            maxY + y,
            maxZ + z
        )
    }

    /**
     * 指定されたオフセットで移動した新しいAABBを返す（Vector版）
     */
    fun offset(offset: Vector): AABB {
        return offset(offset.x, offset.y, offset.z)
    }

    /**
     * Y軸方向の衝突を計算（スウィープテスト）
     * VanillaSource実装に基づく
     *
     * thisはブロック、entityAABBは移動中のエンティティ
     *
     * @param entityAABB エンティティのAABB
     * @param deltaY 移動量（Y軸）
     * @return 実際に移動可能な量
     */
    fun calculateYOffset(entityAABB: AABB, deltaY: Double): Double {
        // X軸とZ軸で重なっているかチェック
        if (entityAABB.maxX > this.minX && entityAABB.minX < this.maxX &&
            entityAABB.maxZ > this.minZ && entityAABB.minZ < this.maxZ) {

            // 上方向への移動（deltaY > 0）で、エンティティの上面がブロックの下面より下にある場合
            if (deltaY > 0.0 && entityAABB.maxY <= this.minY) {
                val collideDelta = this.minY - entityAABB.maxY
                if (collideDelta < deltaY) {
                    return collideDelta
                }
            }
            // 下方向への移動（deltaY < 0）で、エンティティの下面がブロックの上面より上にある場合
            else if (deltaY < 0.0 && entityAABB.minY >= this.maxY) {
                val collideDelta = this.maxY - entityAABB.minY
                if (collideDelta > deltaY) {
                    return collideDelta
                }
            }
        }
        return deltaY
    }

    /**
     * X軸方向の衝突を計算（スウィープテスト）
     * VanillaSource実装に基づく
     */
    fun calculateXOffset(entityAABB: AABB, deltaX: Double): Double {
        // Y軸とZ軸で重なっているかチェック
        if (entityAABB.maxY > this.minY && entityAABB.minY < this.maxY &&
            entityAABB.maxZ > this.minZ && entityAABB.minZ < this.maxZ) {

            // 正方向への移動（deltaX > 0）で、エンティティの右面がブロックの左面より左にある場合
            if (deltaX > 0.0 && entityAABB.maxX <= this.minX) {
                val collideDelta = this.minX - entityAABB.maxX
                if (collideDelta < deltaX) {
                    return collideDelta
                }
            }
            // 負方向への移動（deltaX < 0）で、エンティティの左面がブロックの右面より右にある場合
            else if (deltaX < 0.0 && entityAABB.minX >= this.maxX) {
                val collideDelta = this.maxX - entityAABB.minX
                if (collideDelta > deltaX) {
                    return collideDelta
                }
            }
        }
        return deltaX
    }

    /**
     * Z軸方向の衝突を計算（スウィープテスト）
     * VanillaSource実装に基づく
     */
    fun calculateZOffset(entityAABB: AABB, deltaZ: Double): Double {
        // X軸とY軸で重なっているかチェック
        if (entityAABB.maxX > this.minX && entityAABB.minX < this.maxX &&
            entityAABB.maxY > this.minY && entityAABB.minY < this.maxY) {

            // 正方向への移動（deltaZ > 0）で、エンティティの前面がブロックの後面より後ろにある場合
            if (deltaZ > 0.0 && entityAABB.maxZ <= this.minZ) {
                val collideDelta = this.minZ - entityAABB.maxZ
                if (collideDelta < deltaZ) {
                    return collideDelta
                }
            }
            // 負方向への移動（deltaZ < 0）で、エンティティの後面がブロックの前面より前にある場合
            else if (deltaZ < 0.0 && entityAABB.minZ >= this.maxZ) {
                val collideDelta = this.maxZ - entityAABB.minZ
                if (collideDelta > deltaZ) {
                    return collideDelta
                }
            }
        }
        return deltaZ
    }

    /**
     * 別のAABBとの最小距離を計算
     */
    fun distanceTo(other: AABB): Double {
        val dx = max(0.0, max(other.minX - this.maxX, this.minX - other.maxX))
        val dy = max(0.0, max(other.minY - this.maxY, this.minY - other.maxY))
        val dz = max(0.0, max(other.minZ - this.maxZ, this.minZ - other.maxZ))
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    companion object {
        /**
         * LocationとサイズからAABBを作成
         *
         * @param location 中心位置（足元）
         * @param width 幅
         * @param height 高さ
         * @return AABB
         */
        fun fromLocation(location: Location, width: Double, height: Double): AABB {
            val halfWidth = width / 2.0
            return AABB(
                location.x - halfWidth,
                location.y,
                location.z - halfWidth,
                location.x + halfWidth,
                location.y + height,
                location.z + halfWidth
            )
        }

        /**
         * ブロック座標からAABBを作成（1x1x1のブロック）
         */
        fun fromBlock(x: Int, y: Int, z: Int): AABB {
            return AABB(
                x.toDouble(),
                y.toDouble(),
                z.toDouble(),
                x + 1.0,
                y + 1.0,
                z + 1.0
            )
        }

        /**
         * 2つのAABBを含む最小のAABBを作成
         */
        fun union(a: AABB, b: AABB): AABB {
            return AABB(
                min(a.minX, b.minX),
                min(a.minY, b.minY),
                min(a.minZ, b.minZ),
                max(a.maxX, b.maxX),
                max(a.maxY, b.maxY),
                max(a.maxZ, b.maxZ)
            )
        }
    }
}
