package com.github.azuazu3939.unique.targeter

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * ターゲッター基底クラス
 *
 * エンティティからターゲットを選択
 */
abstract class Targeter(
    val id: String,
    val condition: Condition? = null
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
     * 条件でフィルタリング
     */
    protected fun filterByCondition(source: Entity, targets: List<Entity>): List<Entity> {
        if (condition == null) return targets

        return targets.filter { target ->
            // ターゲット用のコンテキストで評価
            val evaluator = Unique.instance.celEvaluator
            evaluator.evaluateTargetCondition(
                condition.expressions.joinToString(" && "),
                source,
                target
            )
        }
    }

    /**
     * 条件でフィルタリング（PacketEntityソース）
     */
    protected fun filterByCondition(source: PacketEntity, targets: List<Entity>): List<Entity> {
        if (condition == null) return targets

        return targets.filter { target ->
            // ターゲット用のコンテキストで評価
            val evaluator = Unique.instance.celEvaluator
            evaluator.evaluatePacketEntityTargetCondition(
                condition.expressions.joinToString(" && "),
                source,
                target
            )
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
        return "Targeter[id=$id, condition=${condition?.debugInfo() ?: "none"}]"
    }
}

/**
 * 自分自身をターゲット
 */
class SelfTargeter(
    id: String = "self",
    condition: Condition? = null
) : Targeter(id, condition) {

    override fun getTargets(source: Entity): List<Entity> {
        val targets = listOf(source)
        val filtered = filterByCondition(source, targets)

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
    condition: Condition? = null
) : Targeter(id, condition) {

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
        val filtered = filterByCondition(source, targets)

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
        val filtered = filterByCondition(source, targets)

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
    condition: Condition? = null
) : Targeter(id, condition) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        val filtered = filterByCondition(source, targets)

        DebugLogger.targeter("RadiusPlayers(range=$range)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        val filtered = filterByCondition(source, targets)

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
    condition: Condition? = null
) : Targeter(id, condition) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it != source && it.isValid && !it.isDead }

        val filtered = filterByCondition(source, targets)

        DebugLogger.targeter("RadiusEntities(range=$range)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it.isValid && !it.isDead }

        val filtered = filterByCondition(source, targets)

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
    condition: Condition? = null
) : Targeter(id, condition) {

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

        val filtered = filterByCondition(source, targets)

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
    condition: Condition? = null
) : Targeter(id, condition) {

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

        val filtered = filterByCondition(source, targets)

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

        val filtered = filterByCondition(source, targets)

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
    condition: Condition? = null
) : Targeter(id, condition) {

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
    condition: Condition? = null
) : Targeter(id, condition) {

    override fun getTargets(source: Entity): List<Entity> {
        val allTargets = baseTargeter.getTargets(source)

        if (allTargets.isEmpty() || count <= 0) {
            return emptyList()
        }

        val shuffled = allTargets.shuffled()
        val selected = shuffled.take(count.coerceAtMost(allTargets.size))
        val filtered = filterByCondition(source, selected)

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
        val filtered = filterByCondition(source, selected)

        DebugLogger.targeter("Random(count=$count) from PacketEntity", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Random $count from (${baseTargeter.getDescription()})"
    }
}