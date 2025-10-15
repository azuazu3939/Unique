package com.github.azuazu3939.unique.targeter.types.advanced

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.*

/**
 * エリア形状
 */
enum class AreaShape {
    CIRCLE,  // 円形
    CONE,    // 円錐
    BOX,     // 箱形
    DONUT    // ドーナツ形
}

/**
 * 方向
 */
enum class Direction {
    FORWARD,   // 前方
    BACKWARD,  // 後方
    LEFT,      // 左
    RIGHT,     // 右
    UP,        // 上
    DOWN       // 下
}

/**
 * エリアターゲッター
 *
 * 指定した形状のエリア内のエンティティをターゲット
 * CEL式による動的な範囲計算をサポート
 *
 * @param id ターゲッターID
 * @param shape エリア形状
 * @param radius 半径（CEL式対応）
 * @param innerRadius 内側半径（DONUT用、CEL式対応）
 * @param angle 角度（CONE用、度数法、CEL式対応）
 * @param width 幅（BOX用、CEL式対応）
 * @param height 高さ（BOX用、CEL式対応）
 * @param depth 奥行き（BOX用、CEL式対応）
 * @param direction 方向（CONE, BOX用）
 * @param targetPlayers プレイヤーをターゲットに含むか
 * @param targetMobs Mobをターゲットに含むか
 */
class AreaTargeter(
    id: String = "area",
    private val shape: AreaShape,
    private val radius: String = "10.0",
    private val innerRadius: String = "0.0",
    private val angle: String = "45.0",
    private val width: String = "10.0",
    private val height: String = "10.0",
    private val depth: String = "10.0",
    private val direction: Direction = Direction.FORWARD,
    private val targetPlayers: Boolean = true,
    private val targetMobs: Boolean = true,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val location = source.location
        val world = source.world

        // CEL式を評価して数値を取得
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildEntityContext(source)

        val radiusValue = evaluateCelExpression(radius, context, evaluator, 10.0)
        val innerRadiusValue = evaluateCelExpression(innerRadius, context, evaluator, 0.0)
        val angleValue = evaluateCelExpression(angle, context, evaluator, 45.0)
        val widthValue = evaluateCelExpression(width, context, evaluator, 10.0)
        val heightValue = evaluateCelExpression(height, context, evaluator, 10.0)
        val depthValue = evaluateCelExpression(depth, context, evaluator, 10.0)

        // 周囲のエンティティを取得
        val nearbyEntities = world.getNearbyEntitiesAsync(
            location,
            radiusValue + 10.0,  // 余裕を持たせて取得
            heightValue.coerceAtLeast(radiusValue),
            radiusValue + 10.0
        ).filter { entity ->
            entity.isValid && !entity.isDead && entity != source &&
            ((targetPlayers && entity is Player) || (targetMobs && entity is LivingEntity && entity !is Player))
        }

        // 形状に応じてフィルタリング
        val targets = when (shape) {
            AreaShape.CIRCLE -> filterCircle(nearbyEntities, location, radiusValue)
            AreaShape.CONE -> filterCone(nearbyEntities, source, location, radiusValue, angleValue, direction)
            AreaShape.BOX -> filterBox(nearbyEntities, source, location, widthValue, heightValue, depthValue, direction)
            AreaShape.DONUT -> filterDonut(nearbyEntities, location, innerRadiusValue, radiusValue)
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Area($shape, radius=$radiusValue)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val location = source.location
        val world = location.world ?: return emptyList()

        // CEL式を評価して数値を取得
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildPacketEntityContext(source)

        val radiusValue = evaluateCelExpression(radius, context, evaluator, 10.0)
        val innerRadiusValue = evaluateCelExpression(innerRadius, context, evaluator, 0.0)
        val angleValue = evaluateCelExpression(angle, context, evaluator, 45.0)
        val widthValue = evaluateCelExpression(width, context, evaluator, 10.0)
        val heightValue = evaluateCelExpression(height, context, evaluator, 10.0)
        val depthValue = evaluateCelExpression(depth, context, evaluator, 10.0)

        // 周囲のエンティティを取得
        val nearbyEntities = world.getNearbyEntitiesAsync(
            location,
            radiusValue + 10.0,
            heightValue.coerceAtLeast(radiusValue),
            radiusValue + 10.0
        ).filter { entity ->
            entity.isValid && !entity.isDead &&
            ((targetPlayers && entity is Player) || (targetMobs && entity is LivingEntity && entity !is Player))
        }

        // 形状に応じてフィルタリング
        val targets = when (shape) {
            AreaShape.CIRCLE -> filterCircle(nearbyEntities, location, radiusValue)
            AreaShape.CONE -> filterConeFromLocation(nearbyEntities, location, radiusValue, angleValue, direction)
            AreaShape.BOX -> filterBoxFromLocation(nearbyEntities, location, widthValue, heightValue, depthValue, direction)
            AreaShape.DONUT -> filterDonut(nearbyEntities, location, innerRadiusValue, radiusValue)
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Area($shape, radius=$radiusValue) from PacketEntity", filtered.size)
        return filtered
    }

    /**
     * CEL式を評価して数値を取得
     */
    private fun evaluateCelExpression(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Double
    ): Double {
        return try {
            // 数値として直接パース可能か試す
            expression.toDoubleOrNull() ?: run {
                // CEL式として評価
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toDouble()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    /**
     * 円形フィルター
     */
    private fun filterCircle(
        entities: Collection<Entity>,
        center: Location,
        radius: Double
    ): List<Entity> {
        return entities.filter { entity ->
            val dx = entity.location.x - center.x
            val dz = entity.location.z - center.z
            sqrt(dx * dx + dz * dz) <= radius
        }
    }

    /**
     * 円錐フィルター（エンティティソース）
     */
    private fun filterCone(
        entities: Collection<Entity>,
        source: Entity,
        center: Location,
        radius: Double,
        angleDegrees: Double,
        direction: Direction
    ): List<Entity> {
        if (source !is LivingEntity) {
            // LivingEntityでない場合はFORWARD方向として処理
            return filterConeFromLocation(entities, center, radius, angleDegrees, direction)
        }

        val directionVector = getDirectionVector(source, direction)
        val angleRadians = Math.toRadians(angleDegrees)

        return entities.filter { entity ->
            val toTarget = entity.location.toVector().subtract(center.toVector())
            val distance = toTarget.length()

            if (distance > radius) return@filter false

            // 角度チェック
            val angle = directionVector.angle(toTarget)
            angle <= angleRadians
        }
    }

    /**
     * 円錐フィルター（位置ソース）
     */
    private fun filterConeFromLocation(
        entities: Collection<Entity>,
        center: Location,
        radius: Double,
        angleDegrees: Double,
        direction: Direction
    ): List<Entity> {
        val directionVector = getDirectionVectorFromLocation(center, direction)
        val angleRadians = Math.toRadians(angleDegrees)

        return entities.filter { entity ->
            val toTarget = entity.location.toVector().subtract(center.toVector())
            val distance = toTarget.length()

            if (distance > radius) return@filter false

            // 角度チェック
            val angle = directionVector.angle(toTarget)
            angle <= angleRadians
        }
    }

    /**
     * 箱形フィルター（エンティティソース）
     */
    private fun filterBox(
        entities: Collection<Entity>,
        source: Entity,
        center: Location,
        width: Double,
        height: Double,
        depth: Double,
        direction: Direction
    ): List<Entity> {
        if (source !is LivingEntity) {
            return filterBoxFromLocation(entities, center, width, height, depth, direction)
        }

        val directionVector = getDirectionVector(source, direction)
        val yaw = atan2(directionVector.z, directionVector.x)

        return entities.filter { entity ->
            val relative = entity.location.clone().subtract(center)

            // 回転変換
            val rotatedX = relative.x * cos(-yaw) - relative.z * sin(-yaw)
            val rotatedZ = relative.x * sin(-yaw) + relative.z * cos(-yaw)

            abs(rotatedX) <= width / 2 &&
            abs(relative.y) <= height / 2 &&
            rotatedZ >= 0 && rotatedZ <= depth
        }
    }

    /**
     * 箱形フィルター（位置ソース）
     */
    private fun filterBoxFromLocation(
        entities: Collection<Entity>,
        center: Location,
        width: Double,
        height: Double,
        depth: Double,
        direction: Direction
    ): List<Entity> {
        val directionVector = getDirectionVectorFromLocation(center, direction)
        val yaw = atan2(directionVector.z, directionVector.x)

        return entities.filter { entity ->
            val relative = entity.location.clone().subtract(center)

            // 回転変換
            val rotatedX = relative.x * cos(-yaw) - relative.z * sin(-yaw)
            val rotatedZ = relative.x * sin(-yaw) + relative.z * cos(-yaw)

            abs(rotatedX) <= width / 2 &&
            abs(relative.y) <= height / 2 &&
            rotatedZ >= 0 && rotatedZ <= depth
        }
    }

    /**
     * ドーナツ形フィルター
     */
    private fun filterDonut(
        entities: Collection<Entity>,
        center: Location,
        innerRadius: Double,
        outerRadius: Double
    ): List<Entity> {
        return entities.filter { entity ->
            val dx = entity.location.x - center.x
            val dz = entity.location.z - center.z
            val distance = sqrt(dx * dx + dz * dz)
            distance in innerRadius..outerRadius
        }
    }

    /**
     * エンティティから方向ベクトルを取得
     */
    private fun getDirectionVector(entity: Entity, direction: Direction): Vector {
        val location = entity.location
        return when (direction) {
            Direction.FORWARD -> location.direction
            Direction.BACKWARD -> location.direction.multiply(-1)
            Direction.LEFT -> location.direction.rotateAroundY(Math.toRadians(90.0))
            Direction.RIGHT -> location.direction.rotateAroundY(Math.toRadians(-90.0))
            Direction.UP -> Vector(0, 1, 0)
            Direction.DOWN -> Vector(0, -1, 0)
        }
    }

    /**
     * 位置から方向ベクトルを取得
     */
    private fun getDirectionVectorFromLocation(location: Location, direction: Direction): Vector {
        return when (direction) {
            Direction.FORWARD -> location.direction
            Direction.BACKWARD -> location.direction.multiply(-1)
            Direction.LEFT -> location.direction.rotateAroundY(Math.toRadians(90.0))
            Direction.RIGHT -> location.direction.rotateAroundY(Math.toRadians(-90.0))
            Direction.UP -> Vector(0, 1, 0)
            Direction.DOWN -> Vector(0, -1, 0)
        }
    }

    override fun getDescription(): String {
        return when (shape) {
            AreaShape.CIRCLE -> "Circle area (radius: $radius)"
            AreaShape.CONE -> "Cone area (radius: $radius, angle: $angle°, direction: $direction)"
            AreaShape.BOX -> "Box area (width: $width, height: $height, depth: $depth, direction: $direction)"
            AreaShape.DONUT -> "Donut area (inner: $innerRadius, outer: $radius)"
        }
    }

    override fun debugInfo(): String {
        return "AreaTargeter[shape=$shape, radius=$radius, angle=$angle, direction=$direction]"
    }
}
