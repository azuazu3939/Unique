package com.github.azuazu3939.unique.targeter

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.*

/**
 * ターゲッター基底クラス
 *
 * エンティティからターゲットを選択
 * CEL式によるフィルタリングをサポート
 */
abstract class Targeter(
    val id: String,
    val filter: String? = null  // CEL式でのフィルター
) {

    /**
     * ターゲットを取得
     *
     * @param source ソースエンティティ
     * @return ターゲットリスト
     */
    abstract fun getTargets(source: Entity): List<Entity>

    /**
     * ターゲットを取得（PacketEntityソース）
     *
     * @param source ソースPacketEntity
     * @return ターゲットリスト
     */
    abstract fun getTargets(source: PacketEntity): List<Entity>

    /**
     * フィルターでフィルタリング（CEL式）
     */
    protected fun filterByFilter(source: Entity, targets: List<Entity>): List<Entity> {
        if (filter == null) return targets
        val evaluator = Unique.instance.celEvaluator

        return targets.filter { target ->
            try {
                // ターゲット用のコンテキストを構築
                val context = CELVariableProvider.buildTargetContext(source, target)
                evaluator.evaluateBoolean(filter, context)
            } catch (e: Exception) {
                DebugLogger.error("Filter evaluation failed: $filter", e)
                false
            }
        }
    }

    /**
     * フィルターでフィルタリング（PacketEntityソース）
     */
    protected fun filterByFilter(source: PacketEntity, targets: List<Entity>): List<Entity> {
        if (filter == null) return targets
        val evaluator = Unique.instance.celEvaluator

        return targets.filter { target ->
            try {
                // PacketEntityとターゲット用のコンテキストを構築
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                evaluator.evaluateBoolean(filter, context)
            } catch (e: Exception) {
                DebugLogger.error("Filter evaluation failed: $filter", e)
                false
            }
        }
    }

    /**
     * ターゲッターの説明
     */
    open fun getDescription(): String {
        return "Targeter: $id"
    }

    /**
     * デバッグ情報
     */
    open fun debugInfo(): String {
        return "Targeter[id=$id, filter=${filter ?: "none"}]"
    }
}

/**
 * 自分自身をターゲット
 */
class SelfTargeter(
    id: String = "self",
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val targets = listOf(source)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Self", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // PacketEntityは実体がないため、自分自身をターゲットにできない
        DebugLogger.targeter("Self (PacketEntity)", 0)
        return emptyList()
    }

    override fun getDescription(): String = "Self"
}

/**
 * 最も近いプレイヤーをターゲット
 */
class NearestPlayerTargeter(
    id: String = "nearest_player",
    private val range: Double = 16.0,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val nearbyPlayers = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        if (nearbyPlayers.isEmpty()) {
            DebugLogger.targeter("NearestPlayer", 0)
            return emptyList()
        }

        val nearest = nearbyPlayers.minByOrNull { it.location.distanceSquared(location) }
        val targets = if (nearest != null) listOf(nearest) else emptyList()
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("NearestPlayer(range=$range)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val nearbyPlayers = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        if (nearbyPlayers.isEmpty()) {
            DebugLogger.targeter("NearestPlayer (PacketEntity)", 0)
            return emptyList()
        }

        val nearest = nearbyPlayers.minByOrNull { it.location.distanceSquared(location) }
        val targets = if (nearest != null) listOf(nearest) else emptyList()
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("NearestPlayer(range=$range) from PacketEntity", filtered.size)
        return filtered
    }

    override fun getDescription(): String = "Nearest player within $range blocks"
}

/**
 * 範囲内の全プレイヤーをターゲット
 */
class RadiusPlayersTargeter(
    id: String = "radius_players",
    private val range: Double = 16.0,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("RadiusPlayers(range=$range)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("RadiusPlayers(range=$range) from PacketEntity", filtered.size)
        return filtered
    }

    override fun getDescription(): String = "All players within $range blocks"
}

/**
 * 範囲内の全エンティティをターゲット
 */
class RadiusEntitiesTargeter(
    id: String = "radius_entities",
    private val range: Double = 16.0,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it != source && it.isValid && !it.isDead }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("RadiusEntities(range=$range)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it.isValid && !it.isDead }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("RadiusEntities(range=$range) from PacketEntity", filtered.size)
        return filtered
    }

    override fun getDescription(): String = "All entities within $range blocks"
}

/**
 * 視線先のターゲット
 */
class LineOfSightTargeter(
    id: String = "line_of_sight",
    private val maxDistance: Double = 50.0,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        if (source !is LivingEntity) {
            return emptyList()
        }

        val target = source.getTargetEntity(maxDistance.toInt())
        val targets = if (target != null && target.isValid && !target.isDead) {
            listOf(target)
        } else {
            emptyList()
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("LineOfSight(maxDistance=$maxDistance)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // PacketEntityには視線がないため、空リストを返す
        DebugLogger.targeter("LineOfSight (PacketEntity)", 0)
        return emptyList()
    }

    override fun getDescription(): String = "Entity in line of sight (max $maxDistance blocks)"
}

/**
 * 攻撃者をターゲット（ダメージイベント時）
 */
class AttackerTargeter(
    id: String = "attacker",
    filter: String? = null
) : Targeter(id, filter) {

    // 攻撃者はコンテキストから取得する必要がある
    private var lastAttacker: Entity? = null

    fun setAttacker(attacker: Entity?) {
        lastAttacker = attacker
    }

    override fun getTargets(source: Entity): List<Entity> {
        val attacker = lastAttacker
        val targets = if (attacker != null && attacker.isValid && !attacker.isDead) {
            listOf(attacker)
        } else {
            emptyList()
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Attacker", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val attacker = lastAttacker
        val targets = if (attacker != null && attacker.isValid && !attacker.isDead) {
            listOf(attacker)
        } else {
            emptyList()
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Attacker (PacketEntity)", filtered.size)
        return filtered
    }

    override fun getDescription(): String = "Attacker"
}

/**
 * 座標ベースのターゲット
 */
class LocationTargeter(
    id: String = "location",
    private val location: Location,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        // 座標にはエンティティがいないので空リストを返す
        // 実際の使用ではスキルがこの座標を使用する
        DebugLogger.targeter("Location", 0)
        return emptyList()
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // 座標にはエンティティがいないので空リストを返す
        DebugLogger.targeter("Location (PacketEntity)", 0)
        return emptyList()
    }

    fun getLocation(): Location {
        return location.clone()
    }

    override fun getDescription(): String {
        return "Location(${location.blockX}, ${location.blockY}, ${location.blockZ})"
    }
}

/**
 * ランダムターゲット
 */
class RandomTargeter(
    id: String = "random",
    private val baseTargeter: Targeter,
    private val count: Int = 1,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val allTargets = baseTargeter.getTargets(source)

        if (allTargets.isEmpty() || count <= 0) {
            return emptyList()
        }

        val shuffled = allTargets.shuffled()
        val selected = shuffled.take(count.coerceAtMost(allTargets.size))
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Random(count=$count)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val allTargets = baseTargeter.getTargets(source)

        if (allTargets.isEmpty() || count <= 0) {
            return emptyList()
        }

        val shuffled = allTargets.shuffled()
        val selected = shuffled.take(count.coerceAtMost(allTargets.size))
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Random(count=$count) from PacketEntity", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Random $count from (${baseTargeter.getDescription()})"
    }
}

/**
 * 条件付きターゲッター
 *
 * ベースターゲッターの結果を条件でフィルタリング
 * 複数の条件を組み合わせて高度なターゲット選択が可能
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター
 * @param conditions 適用する条件リスト（AND条件）
 * @deprecated Use filter property on base Targeter instead
 */
// @Deprecated("Use filter property on base Targeter", ReplaceWith("baseTargeter with filter"))
class ConditionalTargeter(
    id: String = "conditional",
    private val baseTargeter: Targeter,
    private val conditions: List<Condition>,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        // ベースターゲッターでターゲットを取得
        val baseTargets = baseTargeter.getTargets(source)

        if (baseTargets.isEmpty() || conditions.isEmpty()) {
            DebugLogger.targeter("Conditional (no filtering)", baseTargets.size)
            return baseTargets
        }

        // 全ての条件を満たすターゲットのみを抽出
        val filtered = baseTargets.filter { target ->
            conditions.all { condition ->
                condition.evaluate(target)
            }
        }

        // さらに自身のフィルターでフィルタリング
        val finalFiltered = filterByFilter(source, filtered)

        DebugLogger.targeter(
            "Conditional(base=${baseTargeter.id}, conditions=${conditions.size})",
            "${baseTargets.size} -> ${filtered.size} -> ${finalFiltered.size}"
        )
        return finalFiltered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // ベースターゲッターでターゲットを取得
        val baseTargets = baseTargeter.getTargets(source)

        if (baseTargets.isEmpty() || conditions.isEmpty()) {
            DebugLogger.targeter("Conditional (PacketEntity, no filtering)", baseTargets.size)
            return baseTargets
        }

        // 全ての条件を満たすターゲットのみを抽出
        val filtered = baseTargets.filter { target ->
            conditions.all { condition ->
                condition.evaluate(target)
            }
        }

        // さらに自身のフィルターでフィルタリング
        val finalFiltered = filterByFilter(source, filtered)

        DebugLogger.targeter(
            "Conditional(PacketEntity, base=${baseTargeter.id}, conditions=${conditions.size})",
            baseTargets.size
        )
        return finalFiltered
    }

    override fun getDescription(): String {
        return "Conditional targeter (base: ${baseTargeter.getDescription()}, ${conditions.size} conditions)"
    }

    override fun debugInfo(): String {
        val conditionInfo = conditions.joinToString(", ") { it.debugInfo() }
        return "ConditionalTargeter[base=${baseTargeter.debugInfo()}, conditions=[$conditionInfo]]"
    }
}

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
        val nearbyEntities = world.getNearbyEntities(
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
        val nearbyEntities = world.getNearbyEntities(
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

/**
 * チェーンターゲッター
 *
 * 初期ターゲットから連鎖的にターゲットを拡散
 * 稲妻攻撃などに使用
 *
 * @param id ターゲッターID
 * @param initialTargeter 初期ターゲッター
 * @param maxChains 最大連鎖数（CEL式対応）
 * @param chainRange 連鎖範囲（CEL式対応）
 * @param chainCondition 連鎖条件（CEL式、chainIndex変数が利用可能）
 */
class ChainTargeter(
    id: String = "chain",
    private val initialTargeter: Targeter,
    private val maxChains: String = "5",
    private val chainRange: String = "5.0",
    private val chainCondition: String? = null,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val evaluator = Unique.instance.celEvaluator
        val baseContext = CELVariableProvider.buildEntityContext(source)

        // CEL式を評価
        val maxChainsValue = evaluateCelInt(maxChains, baseContext, evaluator, 5)
        val chainRangeValue = evaluateCelDouble(chainRange, baseContext, evaluator, 5.0)

        // 初期ターゲットを取得
        val initialTargets = initialTargeter.getTargets(source)
        if (initialTargets.isEmpty()) {
            DebugLogger.targeter("Chain (no initial target)", 0)
            return emptyList()
        }

        // 連鎖処理を実行
        val allTargets = performChainTargeting(
            initialTargets = initialTargets,
            maxChains = maxChainsValue,
            chainRange = chainRangeValue,
            baseContext = baseContext,
            evaluator = evaluator,
            excludeEntity = source
        )

        val filtered = filterByFilter(source, allTargets)

        DebugLogger.targeter(
            "Chain(max=$maxChainsValue, range=$chainRangeValue)",
            "${allTargets.size} targets (${filtered.size} after filter)"
        )
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val evaluator = Unique.instance.celEvaluator
        val baseContext = CELVariableProvider.buildPacketEntityContext(source)

        // CEL式を評価
        val maxChainsValue = evaluateCelInt(maxChains, baseContext, evaluator, 5)
        val chainRangeValue = evaluateCelDouble(chainRange, baseContext, evaluator, 5.0)

        // 初期ターゲットを取得
        val initialTargets = initialTargeter.getTargets(source)
        if (initialTargets.isEmpty()) {
            DebugLogger.targeter("Chain (PacketEntity, no initial target)", 0)
            return emptyList()
        }

        // 連鎖処理を実行（PacketEntityは実体がないため除外なし）
        val allTargets = performChainTargeting(
            initialTargets = initialTargets,
            maxChains = maxChainsValue,
            chainRange = chainRangeValue,
            baseContext = baseContext,
            evaluator = evaluator,
            excludeEntity = null
        )

        val filtered = filterByFilter(source, allTargets)

        DebugLogger.targeter(
            "Chain(PacketEntity, max=$maxChainsValue, range=$chainRangeValue)",
            "${allTargets.size} targets (${filtered.size} after filter)"
        )
        return filtered
    }

    /**
     * 連鎖ターゲティングを実行
     */
    private fun performChainTargeting(
        initialTargets: List<Entity>,
        maxChains: Int,
        chainRange: Double,
        baseContext: Map<String, Any>,
        evaluator: CELEvaluator,
        excludeEntity: Entity?
    ): List<Entity> {
        val allTargets = mutableListOf<Entity>()
        val processed = mutableSetOf<Entity>()

        // 最初のターゲット
        var currentTarget = initialTargets.first()
        allTargets.add(currentTarget)
        processed.add(currentTarget)

        // 連鎖処理
        repeat(maxChains - 1) { index ->
            val chainIndex = index + 1

            // 現在のターゲットから範囲内のエンティティを取得
            val nearbyEntities = currentTarget.world.getNearbyEntities(
                currentTarget.location,
                chainRange,
                chainRange,
                chainRange
            ).filterIsInstance<LivingEntity>()
                .filter {
                    it.isValid && !it.isDead && it !in processed &&
                    (excludeEntity == null || it != excludeEntity)
                }

            if (nearbyEntities.isEmpty()) {
                return@repeat
            }

            // 連鎖条件でフィルタリング
            val candidates = if (chainCondition != null) {
                nearbyEntities.filter { candidate ->
                    val context = baseContext.toMutableMap()
                    context["target"] = CELVariableProvider.buildEntityInfo(candidate)
                    context["chainIndex"] = chainIndex
                    context["currentTarget"] = CELVariableProvider.buildEntityInfo(currentTarget)

                    try {
                        evaluator.evaluateBoolean(chainCondition, context)
                    } catch (e: Exception) {
                        DebugLogger.error("Chain condition evaluation failed", e)
                        false
                    }
                }
            } else {
                nearbyEntities
            }

            if (candidates.isEmpty()) {
                return@repeat
            }

            // 最も近いターゲットを選択
            val nextTarget = candidates.minByOrNull {
                it.location.distanceSquared(currentTarget.location)
            } ?: return@repeat

            allTargets.add(nextTarget)
            processed.add(nextTarget)
            currentTarget = nextTarget
        }

        return allTargets
    }

    /**
     * CEL式を評価してInt値を取得
     */
    private fun evaluateCelInt(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Int
    ): Int {
        return try {
            expression.toIntOrNull() ?: run {
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toInt()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    /**
     * CEL式を評価してDouble値を取得
     */
    private fun evaluateCelDouble(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Double
    ): Double {
        return try {
            expression.toDoubleOrNull() ?: run {
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

    override fun getDescription(): String {
        return "Chain targeter (initial: ${initialTargeter.getDescription()}, max chains: $maxChains, range: $chainRange)"
    }

    override fun debugInfo(): String {
        return "ChainTargeter[initial=${initialTargeter.debugInfo()}, maxChains=$maxChains, chainRange=$chainRange, condition=${chainCondition ?: "none"}]"
    }
}