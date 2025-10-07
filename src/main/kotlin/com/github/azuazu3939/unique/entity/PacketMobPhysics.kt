package com.github.azuazu3939.unique.entity

/**
 * PacketMobの物理演算を担当するクラス
 *
 * - 重力の適用
 * - 速度の管理と適用
 * - 移動時の衝突判定
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

    /**
     * 速度を追加（ノックバックなど）
     */
    fun addVelocity(vx: Double, vy: Double, vz: Double) {
        velocityX += vx
        velocityY += vy
        velocityZ += vz
    }

    /**
     * 速度を設定（上書き）
     */
    fun setVelocity(vx: Double, vy: Double, vz: Double) {
        velocityX = vx
        velocityY = vy
        velocityZ = vz
    }

    /**
     * 速度を実際の移動に適用
     */
    suspend fun applyVelocity() {
        // 速度がほぼ0なら処理をスキップ
        if (kotlin.math.abs(velocityX) < 0.001 &&
            kotlin.math.abs(velocityY) < 0.001 &&
            kotlin.math.abs(velocityZ) < 0.001) {
            velocityX = 0.0
            velocityY = 0.0
            velocityZ = 0.0
            return
        }

        // 速度を移動に適用
        mob.move(velocityX, velocityY, velocityZ)

        // 摩擦・空気抵抗を適用
        if (isOnGround) {
            // 地面摩擦（強めに設定してノックバックをより早く減衰）
            velocityX *= 0.5
            velocityZ *= 0.5
        } else {
            // 空気抵抗
            velocityX *= 0.95
            velocityZ *= 0.95
        }

        // Y軸は空気抵抗のみ
        velocityY *= 0.98
    }

    /**
     * 重力を適用（Mobの当たり判定全体をチェック）
     */
    suspend fun applyGravity() {
        val world = mob.location.world ?: return

        // Mobの当たり判定を取得
        val hitboxRadius = mob.getEntityHitboxWidth() / 2.0
        val hitboxHeight = mob.getEntityHitboxHeight()

        // 当たり判定の範囲を計算
        val minX = (mob.location.x - hitboxRadius).toInt()
        val maxX = (mob.location.x + hitboxRadius).toInt()
        val minZ = (mob.location.z - hitboxRadius).toInt()
        val maxZ = (mob.location.z + hitboxRadius).toInt()

        val footY = mob.location.y
        val footBlockY = footY.toInt()

        // 当たり判定の範囲内で埋まりチェック（足元から頭まで）
        var isEmbedded = false
        var maxEmbeddedY = footBlockY

        for (checkX in minX..maxX) {
            for (checkZ in minZ..maxZ) {
                for (i in 0 until kotlin.math.ceil(hitboxHeight).toInt()) {
                    val checkY = footBlockY + i
                    val block = world.getBlockAt(checkX, checkY, checkZ)
                    if (block.type.isSolid) {
                        isEmbedded = true
                        if (checkY > maxEmbeddedY) {
                            maxEmbeddedY = checkY
                        }
                    }
                }
            }
        }

        // 埋まっている場合は押し上げる
        if (isEmbedded) {
            val groundY = maxEmbeddedY + 1.0
            mob.location.y = groundY
            isOnGround = true
            velocityY = 0.0
            return
        }

        // 当たり判定の範囲内で地面検出
        var hasGround = false
        var maxGroundY = footBlockY - 1

        for (checkX in minX..maxX) {
            for (checkZ in minZ..maxZ) {
                val blockBelow = world.getBlockAt(checkX, footBlockY - 1, checkZ)
                if (blockBelow.type.isSolid) {
                    hasGround = true
                    val groundBlockY = footBlockY - 1
                    if (groundBlockY > maxGroundY) {
                        maxGroundY = groundBlockY
                    }
                }
            }
        }

        if (hasGround) {
            val groundY = maxGroundY + 1.0
            val distanceToGround = footY - groundY

            // 地面に近い場合（0.15ブロック以内）
            if (distanceToGround < 0.15) {
                isOnGround = true
                // 下降中なら速度をリセット
                if (velocityY < 0) {
                    velocityY = 0.0
                }
                // 地面より下にいる場合は補正
                if (distanceToGround < 0.05) {
                    mob.location.y = groundY
                }
            } else {
                // まだ空中
                isOnGround = false
                velocityY -= 0.2
                if (velocityY < -2.0) {
                    velocityY = -2.0
                }
            }
        } else {
            // 空中（地面がない）
            isOnGround = false
            velocityY -= 0.2
            if (velocityY < -2.0) {
                velocityY = -2.0
            }
        }
    }

    /**
     * 指定された位置でMobが通れる空間があるかチェック
     *
     * @param world ワールド
     * @param centerX Mobの中心X座標
     * @param centerZ Mobの中心Z座標
     * @param footY Mobの足元Y座標
     * @return 通れる場合true、通れない場合false
     */
    fun checkClearance(world: org.bukkit.World, centerX: Double, centerZ: Double, footY: Double): Boolean {
        val hitboxRadius = mob.getEntityHitboxWidth() / 2.0
        val hitboxHeight = mob.getEntityHitboxHeight()

        // 当たり判定の範囲を計算
        val minX = (centerX - hitboxRadius).toInt()
        val maxX = (centerX + hitboxRadius).toInt()
        val minZ = (centerZ - hitboxRadius).toInt()
        val maxZ = (centerZ + hitboxRadius).toInt()

        val startY = footY.toInt()
        val requiredHeight = kotlin.math.ceil(hitboxHeight).toInt()

        // 当たり判定の範囲内の全ての位置で高さをチェック
        for (checkX in minX..maxX) {
            for (checkZ in minZ..maxZ) {
                // 足元から必要な高さ分のブロックをチェック
                for (i in 0 until requiredHeight) {
                    val checkY = startY + i
                    val block = world.getBlockAt(checkX, checkY, checkZ)

                    // 固体ブロックがある場合は通れない
                    if (block.type.isSolid) {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * 外部からの速度（ノックバックなど）が有効かチェック
     */
    fun hasExternalVelocity(): Boolean {
        val horizontalVelocity = kotlin.math.sqrt(velocityX * velocityX + velocityZ * velocityZ)
        return horizontalVelocity > 0.05
    }
}
