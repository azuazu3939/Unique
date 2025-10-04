package com.github.azuazu3939.unique.util

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import kotlin.math.*

/**
 * 座標計算ユーティリティ
 *
 * Location操作、距離計算、円形配置などの幾何学計算を提供
 */
object LocationUtil {

    /**
     * 2点間の距離を計算（3D）
     */
    fun distance(loc1: Location, loc2: Location): Double {
        require(loc1.world == loc2.world) { "Locations must be in the same world" }
        return loc1.distance(loc2)
    }

    /**
     * 2点間の距離を計算（2D - Y軸を無視）
     */
    fun distance2D(loc1: Location, loc2: Location): Double {
        require(loc1.world == loc2.world) { "Locations must be in the same world" }
        val dx = loc1.x - loc2.x
        val dz = loc1.z - loc2.z
        return sqrt(dx * dx + dz * dz)
    }

    /**
     * 2点間の距離の二乗を計算（パフォーマンス最適化用）
     * sqrt()を省略するため高速
     */
    fun distanceSquared(loc1: Location, loc2: Location): Double {
        require(loc1.world == loc2.world) { "Locations must be in the same world" }
        return loc1.distanceSquared(loc2)
    }

    /**
     * 指定範囲内にあるかチェック
     */
    fun isInRange(loc1: Location, loc2: Location, range: Double): Boolean {
        return distanceSquared(loc1, loc2) <= range * range
    }

    /**
     * 中間点を取得
     */
    fun midpoint(loc1: Location, loc2: Location): Location {
        require(loc1.world == loc2.world) { "Locations must be in the same world" }
        return Location(
            loc1.world,
            (loc1.x + loc2.x) / 2,
            (loc1.y + loc2.y) / 2,
            (loc1.z + loc2.z) / 2
        )
    }

    /**
     * 方向ベクトルを取得
     */
    fun getDirection(from: Location, to: Location): Vector {
        return to.toVector().subtract(from.toVector()).normalize()
    }

    /**
     * 指定方向に移動したLocationを取得
     */
    fun advance(location: Location, direction: Vector, distance: Double): Location {
        val normalized = direction.clone().normalize()
        return location.clone().add(normalized.multiply(distance))
    }

    /**
     * エンティティの視線方向のLocationを取得
     */
    fun getTargetLocation(entity: Entity, distance: Double): Location {
        val direction = entity.location.direction
        return advance(entity.location, direction, distance)
    }

    /**
     * 円形配置でLocationリストを生成
     *
     * @param center 中心座標
     * @param radius 半径
     * @param points 配置する点の数
     * @param yOffset Y軸オフセット
     * @return 円形に配置されたLocationリスト
     */
    fun getCircleLocations(
        center: Location,
        radius: Double,
        points: Int,
        yOffset: Double = 0.0
    ): List<Location> {
        val locations = mutableListOf<Location>()
        val angleStep = 2 * PI / points

        for (i in 0 until points) {
            val angle = angleStep * i
            val x = center.x + radius * cos(angle)
            val z = center.z + radius * sin(angle)
            val y = center.y + yOffset

            locations.add(Location(center.world, x, y, z))
        }

        return locations
    }

    /**
     * 球形配置でLocationリストを生成
     *
     * @param center 中心座標
     * @param radius 半径
     * @param points 配置する点の数
     * @return 球形に配置されたLocationリスト
     */
    fun getSphereLocations(
        center: Location,
        radius: Double,
        points: Int
    ): List<Location> {
        val locations = mutableListOf<Location>()
        val goldenRatio = (1 + sqrt(5.0)) / 2

        for (i in 0 until points) {
            val theta = 2 * PI * i / goldenRatio
            val phi = acos(1 - 2 * (i + 0.5) / points)

            val x = center.x + radius * cos(theta) * sin(phi)
            val y = center.y + radius * cos(phi)
            val z = center.z + radius * sin(theta) * sin(phi)

            locations.add(Location(center.world, x, y, z))
        }

        return locations
    }

    /**
     * ランダムな円形範囲内のLocationを取得
     */
    fun getRandomCircleLocation(center: Location, radius: Double, yOffset: Double = 0.0): Location {
        val angle = Math.random() * 2 * PI
        val r = sqrt(Math.random()) * radius

        val x = center.x + r * cos(angle)
        val z = center.z + r * sin(angle)
        val y = center.y + yOffset

        return Location(center.world, x, y, z)
    }

    /**
     * 2点間の角度を取得（度数法）
     */
    fun getAngle(from: Location, to: Location): Double {
        val dx = to.x - from.x
        val dz = to.z - from.z
        return Math.toDegrees(atan2(dz, dx))
    }

    /**
     * Yaw（ヨー）を方向ベクトルに変換
     */
    fun yawToDirection(yaw: Float): Vector {
        val radians = Math.toRadians(yaw.toDouble())
        return Vector(
            -sin(radians),
            0.0,
            cos(radians)
        )
    }

    /**
     * LocationをYaw方向に回転
     */
    fun rotateAroundY(location: Location, center: Location, angle: Double): Location {
        val radians = Math.toRadians(angle)
        val cos = cos(radians)
        val sin = sin(radians)

        val dx = location.x - center.x
        val dz = location.z - center.z

        val newX = center.x + dx * cos - dz * sin
        val newZ = center.z + dx * sin + dz * cos

        return Location(location.world, newX, location.y, newZ)
    }

    /**
     * 矩形範囲内のランダムLocationを取得
     */
    fun getRandomLocationInBox(
        world: World,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double
    ): Location {
        val x = minX + Math.random() * (maxX - minX)
        val y = minY + Math.random() * (maxY - minY)
        val z = minZ + Math.random() * (maxZ - minZ)
        return Location(world, x, y, z)
    }

    /**
     * 視線が通っているかチェック（簡易版）
     */
    fun hasLineOfSight(from: Location, to: Location, maxDistance: Double = 100.0): Boolean {
        val distance = from.distance(to)
        if (distance > maxDistance) return false

        val world = from.world ?: return false
        return world.rayTraceBlocks(from, getDirection(from, to), distance)?.hitBlock == null
    }

    /**
     * Locationを文字列化（デバッグ用）
     */
    fun format(location: Location, precision: Int = 2): String {
        val format = "%.${precision}f"
        return "${location.world?.name}:(${format.format(location.x)}, ${format.format(location.y)}, ${format.format(location.z)})"
    }

    /**
     * Locationを簡潔に文字列化
     */
    fun formatShort(location: Location): String {
        return "(${location.blockX}, ${location.blockY}, ${location.blockZ})"
    }

    /**
     * 安全なLocation生成（ワールドがnullの場合の対処）
     */
    fun safeLocation(world: World?, x: Double, y: Double, z: Double): Location? {
        return world?.let { Location(it, x, y, z) }
    }

    /**
     * Locationのクローン（安全版）
     */
    fun safeClone(location: Location?): Location? {
        return location?.clone()
    }
}