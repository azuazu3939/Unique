package com.github.azuazu3939.unique.targeter

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.maxHealth
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.*

/**
 * エンティティタイプフィルター
 */
enum class EntityTypeFilter {
    PLAYER,      // プレイヤーのみ
    PACKET_MOB,  // PacketMobのみ
    LIVING,      // LivingEntityのみ（デフォルト）
    ENTITY       // 全てのEntity
}

/**
 * ターゲットソートモード
 */
enum class TargetSortMode {
    NONE,            // ソートなし
    NEAREST,         // 距離が近い順
    FARTHEST,        // 距離が遠い順
    LOWEST_HEALTH,   // HP低い順
    HIGHEST_HEALTH,  // HP高い順
    THREAT,          // ヘイト値高い順
    RANDOM,          // ランダム
    CUSTOM           // CEL式でカスタムソート（sortExpressionが必要）
}

/**
 * ターゲッター基底クラス
 *
 * エンティティからターゲットを選択
 * CEL式によるフィルタリングをサポート
 */
abstract class Targeter(
    val id: String,
    val filter: String? = null,  // CEL式でのフィルター
    val entityType: EntityTypeFilter = EntityTypeFilter.LIVING  // エンティティタイプフィルター
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
     * エンティティタイプでフィルタリング
     */
    protected fun filterByEntityType(entities: Collection<Entity>): List<Entity> {
        return when (entityType) {
            EntityTypeFilter.PLAYER -> entities.filterIsInstance<Player>()
            EntityTypeFilter.PACKET_MOB -> {
                // PacketMobは実体を持たないため、通常のエンティティフィルタリングでは取得できない
                // PacketMob専用の処理が必要な場合は別途実装
                emptyList()
            }
            EntityTypeFilter.LIVING -> {
                // LivingEntityのみ
                entities.filterIsInstance<LivingEntity>()
            }
            EntityTypeFilter.ENTITY -> entities.toList()
        }
    }

    /**
     * ターゲットをソートして制限
     *
     * @param source ソースエンティティ
     * @param targets ターゲットリスト
     * @param sortMode ソートモード
     * @param sortExpression カスタムソート用のCEL式（sortMode=CUSTOMの場合に使用）
     * @param offset スキップする件数（デフォルト: 0）
     * @param limit 取得する最大件数（nullの場合は制限なし）
     * @return ソート・制限されたターゲットリスト
     */
    protected fun sortAndLimitTargets(
        source: Entity,
        targets: List<Entity>,
        sortMode: TargetSortMode,
        sortExpression: String? = null,
        offset: Int = 0,
        limit: Int? = null
    ): List<Entity> {
        if (targets.isEmpty()) return targets

        // ソート
        val sorted = when (sortMode) {
            TargetSortMode.NONE -> targets
            TargetSortMode.NEAREST -> targets.sortedBy {
                it.location.distanceSquared(source.location)
            }
            TargetSortMode.FARTHEST -> targets.sortedByDescending {
                it.location.distanceSquared(source.location)
            }
            TargetSortMode.LOWEST_HEALTH -> targets.filterIsInstance<LivingEntity>()
                .sortedBy { it.health }
            TargetSortMode.HIGHEST_HEALTH -> targets.filterIsInstance<LivingEntity>()
                .sortedByDescending { it.health }
            TargetSortMode.THREAT -> targets.sortedByDescending {
                ThreatTargeter.getThreat(source, it)
            }
            TargetSortMode.RANDOM -> targets.shuffled()
            TargetSortMode.CUSTOM -> {
                if (sortExpression == null) {
                    DebugLogger.error("CUSTOM sort mode requires sortExpression parameter")
                    targets
                } else {
                    sortByCustomExpression(source, targets, sortExpression)
                }
            }
        }

        // オフセットと制限を適用
        return applyOffsetAndLimit(sorted, offset, limit)
    }

    /**
     * CEL式を使用してターゲットをソート
     */
    private fun sortByCustomExpression(source: Entity, targets: List<Entity>, expression: String): List<Entity> {
        val evaluator = Unique.instance.celEvaluator

        return try {
            targets.sortedBy { target ->
                try {
                    val context = CELVariableProvider.buildTargetContext(source, target)
                    val result = evaluator.evaluate(expression, context)

                    // 数値に変換
                    when (result) {
                        is Number -> result.toDouble()
                        is Boolean -> if (result) 1.0 else 0.0
                        is String -> result.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                } catch (e: Exception) {
                    DebugLogger.error("Failed to evaluate sort expression for target: $expression", e)
                    0.0
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to sort targets by expression: $expression", e)
            targets
        }
    }

    /**
     * オフセットと制限を適用
     */
    private fun applyOffsetAndLimit(targets: List<Entity>, offset: Int, limit: Int?): List<Entity> {
        val validOffset = offset.coerceAtLeast(0)

        // オフセット適用
        val afterOffset = if (validOffset > 0) {
            targets.drop(validOffset)
        } else {
            targets
        }

        // 制限適用
        return if (limit != null && limit > 0) {
            afterOffset.take(limit)
        } else {
            afterOffset
        }
    }

    /**
     * ターゲットをソートして制限（PacketEntityソース）
     *
     * @param source ソースPacketEntity
     * @param targets ターゲットリスト
     * @param sortMode ソートモード
     * @param sortExpression カスタムソート用のCEL式（sortMode=CUSTOMの場合に使用）
     * @param offset スキップする件数（デフォルト: 0）
     * @param limit 取得する最大件数（nullの場合は制限なし）
     * @return ソート・制限されたターゲットリスト
     */
    protected fun sortAndLimitTargets(
        source: PacketEntity,
        targets: List<Entity>,
        sortMode: TargetSortMode,
        sortExpression: String? = null,
        offset: Int = 0,
        limit: Int? = null
    ): List<Entity> {
        if (targets.isEmpty()) return targets

        // ソート
        val sorted = when (sortMode) {
            TargetSortMode.NONE -> targets
            TargetSortMode.NEAREST -> targets.sortedBy {
                it.location.distanceSquared(source.location)
            }
            TargetSortMode.FARTHEST -> targets.sortedByDescending {
                it.location.distanceSquared(source.location)
            }
            TargetSortMode.LOWEST_HEALTH -> targets.filterIsInstance<LivingEntity>()
                .sortedBy { it.health }
            TargetSortMode.HIGHEST_HEALTH -> targets.filterIsInstance<LivingEntity>()
                .sortedByDescending { it.health }
            TargetSortMode.THREAT -> targets.sortedByDescending {
                // PacketEntity用のthreat取得ロジック
                if (it.hasMetadata("${ThreatTargeter.THREAT_METADATA_KEY}_packetentity_${source.entityId}")) {
                    it.getMetadata("${ThreatTargeter.THREAT_METADATA_KEY}_packetentity_${source.entityId}")
                        .firstOrNull()?.asDouble() ?: 0.0
                } else {
                    0.0
                }
            }
            TargetSortMode.RANDOM -> targets.shuffled()
            TargetSortMode.CUSTOM -> {
                if (sortExpression == null) {
                    DebugLogger.error("CUSTOM sort mode requires sortExpression parameter")
                    targets
                } else {
                    sortByCustomExpressionFromPacket(source, targets, sortExpression)
                }
            }
        }

        // オフセットと制限を適用
        return applyOffsetAndLimit(sorted, offset, limit)
    }

    /**
     * CEL式を使用してターゲットをソート（PacketEntityソース）
     */
    private fun sortByCustomExpressionFromPacket(source: PacketEntity, targets: List<Entity>, expression: String): List<Entity> {
        val evaluator = Unique.instance.celEvaluator

        return try {
            targets.sortedBy { target ->
                try {
                    val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                    val result = evaluator.evaluate(expression, context)

                    // 数値に変換
                    when (result) {
                        is Number -> result.toDouble()
                        is Boolean -> if (result) 1.0 else 0.0
                        is String -> result.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                } catch (e: Exception) {
                    DebugLogger.error("Failed to evaluate sort expression for target (PacketEntity): $expression", e)
                    0.0
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to sort targets by expression (PacketEntity): $expression", e)
            targets
        }
    }

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
    filter: String? = null,
    private val sortMode: TargetSortMode = TargetSortMode.NONE,
    private val sortExpression: String? = null,
    private val offset: Int = 0,
    private val limit: Int? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, targets)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusPlayers(range=$range, sort=$sortMode, offset=$offset, limit=$limit)",
            result.size
        )
        return result
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        val targets = world.getNearbyEntities(location, range, range, range)
            .filterIsInstance<Player>()
            .filter { it.isValid && !it.isDead }

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, targets)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusPlayers(range=$range, sort=$sortMode, offset=$offset, limit=$limit) from PacketEntity",
            result.size
        )
        return result
    }

    override fun getDescription(): String {
        val parts = mutableListOf("Players within $range blocks")
        if (sortMode != TargetSortMode.NONE) parts.add("sort: $sortMode")
        if (sortExpression != null) parts.add("sortExpr: $sortExpression")
        if (offset > 0) parts.add("offset: $offset")
        if (limit != null) parts.add("limit: $limit")
        return parts.joinToString(", ")
    }
}

/**
 * 範囲内の全エンティティをターゲット
 */
class RadiusEntitiesTargeter(
    id: String = "radius_entities",
    private val range: Double = 16.0,
    filter: String? = null,
    entityType: EntityTypeFilter = EntityTypeFilter.LIVING,
    private val sortMode: TargetSortMode = TargetSortMode.NONE,
    private val sortExpression: String? = null,
    private val offset: Int = 0,
    private val limit: Int? = null
) : Targeter(id, filter, entityType) {

    override fun getTargets(source: Entity): List<Entity> {
        val world = source.world
        val location = source.location

        // 全てのエンティティを取得
        val allEntities = world.getNearbyEntities(location, range, range, range)
            .filter { it != source && it.isValid && !it.isDead }

        // エンティティタイプでフィルタリング
        val typeFiltered = filterByEntityType(allEntities)

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, typeFiltered)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusEntities(range=$range, type=$entityType, sort=$sortMode, offset=$offset, limit=$limit)",
            result.size
        )
        return result
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val world = source.location.world ?: return emptyList()
        val location = source.location

        // 全てのエンティティを取得
        val allEntities = world.getNearbyEntities(location, range, range, range)
            .filter { it.isValid && !it.isDead }

        // エンティティタイプでフィルタリング
        val typeFiltered = filterByEntityType(allEntities)

        // CEL式でフィルタリング
        val celFiltered = filterByFilter(source, typeFiltered)

        // ソートと制限
        val result = sortAndLimitTargets(source, celFiltered, sortMode, sortExpression, offset, limit)

        DebugLogger.targeter(
            "RadiusEntities(range=$range, type=$entityType, sort=$sortMode, offset=$offset, limit=$limit) from PacketEntity",
            result.size
        )
        return result
    }

    override fun getDescription(): String {
        val parts = mutableListOf("Entities within $range blocks")
        parts.add("type: $entityType")
        if (sortMode != TargetSortMode.NONE) parts.add("sort: $sortMode")
        if (sortExpression != null) parts.add("sortExpr: $sortExpression")
        if (offset > 0) parts.add("offset: $offset")
        if (limit != null) parts.add("limit: $limit")
        return parts.joinToString(", ")
    }
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

/**
 * LowestHealthTargeter - 最もHPが低いターゲットを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もHPが低いエンティティを選択します。
 * ヒーラーAIやフィニッシャー攻撃などに有用です。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 *
 * 使用例:
 * ```yaml
 * targeter:
 *   type: LowestHealth
 *   baseTargeter:
 *     type: RadiusPlayers
 *     range: 20.0
 * ```
 */
class LowestHealthTargeter(
    id: String = "lowest_health",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (no candidates)", 0)
            return emptyList()
        }

        // LivingEntityのみを対象にし、最もHPが低いものを選択
        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (no living entities)", 0)
            return emptyList()
        }

        val lowestHealth = livingCandidates.minByOrNull { it.health } ?: return emptyList()
        val targets = listOf(lowestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "LowestHealth (selected: ${lowestHealth.name}, HP: ${lowestHealth.health}/${lowestHealth.maxHealth()})",
            filtered.size
        )

        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("LowestHealth (PacketEntity, no living entities)", 0)
            return emptyList()
        }

        val lowestHealth = livingCandidates.minByOrNull { it.health } ?: return emptyList()
        val targets = listOf(lowestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "LowestHealth from PacketEntity (selected: ${lowestHealth.name}, HP: ${lowestHealth.health})",
            filtered.size
        )

        return filtered
    }

    override fun getDescription(): String {
        return "Lowest health from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "LowestHealthTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}

/**
 * HighestHealthTargeter - 最もHPが高いターゲットを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もHPが高いエンティティを選択します。
 * タンクキャラへの攻撃やボス優先攻撃などに有用です。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 *
 * 使用例:
 * ```yaml
 * targeter:
 *   type: HighestHealth
 *   baseTargeter:
 *     type: RadiusEntities
 *     range: 15.0
 * ```
 */
class HighestHealthTargeter(
    id: String = "highest_health",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (no candidates)", 0)
            return emptyList()
        }

        // LivingEntityのみを対象にし、最もHPが高いものを選択
        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (no living entities)", 0)
            return emptyList()
        }

        val highestHealth = livingCandidates.maxByOrNull { it.health } ?: return emptyList()
        val targets = listOf(highestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "HighestHealth (selected: ${highestHealth.name}, HP: ${highestHealth.health}/${highestHealth.maxHealth()})",
            filtered.size
        )

        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val livingCandidates = candidates.filterIsInstance<LivingEntity>()

        if (livingCandidates.isEmpty()) {
            DebugLogger.targeter("HighestHealth (PacketEntity, no living entities)", 0)
            return emptyList()
        }

        val highestHealth = livingCandidates.maxByOrNull { it.health } ?: return emptyList()
        val targets = listOf(highestHealth as Entity)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter(
            "HighestHealth from PacketEntity (selected: ${highestHealth.name}, HP: ${highestHealth.health})",
            filtered.size
        )

        return filtered
    }

    override fun getDescription(): String {
        return "Highest health from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "HighestHealthTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}

/**
 * NearestTargeter - 最も近いエンティティを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もソースに近いエンティティを選択します。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 */
class NearestTargeter(
    id: String = "nearest",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Nearest (no candidates)", 0)
            return emptyList()
        }

        val nearest = candidates.minByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(nearest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Nearest (selected: ${nearest.name})", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Nearest (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val nearest = candidates.minByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(nearest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Nearest from PacketEntity (selected: ${nearest.name})", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Nearest from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "NearestTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}

/**
 * FarthestTargeter - 最も遠いエンティティを選択
 *
 * ベースターゲッターから取得したターゲットの中から、
 * 最もソースから遠いエンティティを選択します。
 *
 * @param id ターゲッターID
 * @param baseTargeter ベースとなるターゲッター（候補を取得）
 * @param filter CEL式によるフィルター（オプション）
 */
class FarthestTargeter(
    id: String = "farthest",
    private val baseTargeter: Targeter,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Farthest (no candidates)", 0)
            return emptyList()
        }

        val farthest = candidates.maxByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(farthest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Farthest (selected: ${farthest.name})", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)

        if (candidates.isEmpty()) {
            DebugLogger.targeter("Farthest (PacketEntity, no candidates)", 0)
            return emptyList()
        }

        val farthest = candidates.maxByOrNull {
            it.location.distanceSquared(source.location)
        } ?: return emptyList()

        val targets = listOf(farthest)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Farthest from PacketEntity (selected: ${farthest.name})", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Farthest from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "FarthestTargeter[base=${baseTargeter.debugInfo()}, filter=${filter ?: "none"}]"
    }
}

// ========================================
// ThreatTargeter - 脅威度ベースのターゲッティング
// ========================================

/**
 * ThreatTargeter - 脅威度（Threat/Aggro）に基づいてターゲットを選択
 *
 * 基本Targeterからターゲット候補を取得し、脅威度が最も高いものを選択します。
 * 脅威度はEntity MetadataまたはPacketMobの内部状態に保存されます。
 *
 * 使用例:
 * ```yaml
 * targeter:
 *   type: THREAT
 *   baseTargeter:
 *     type: RADIUSPLAYERS
 *     range: 20.0
 *   count: 3  # 上位3体を選択
 * ```
 *
 * 脅威度の追加方法:
 * ```kotlin
 * // Damageを与えた時に脅威度を追加
 * ThreatTargeter.addThreat(mob, player, 10.0)
 * ```
 */
class ThreatTargeter(
    id: String = "threat",
    private val baseTargeter: Targeter,
    private val count: Int = 1,
    filter: String? = null
) : Targeter(id, filter) {

    companion object {
        const val THREAT_METADATA_KEY = "unique_threat_value"
        private const val THREAT_DECAY_INTERVAL = 100L  // 5秒ごとに減衰
        private const val THREAT_DECAY_RATE = 0.95  // 5%減衰

        /**
         * 脅威度を追加
         *
         * @param source 脅威の発生源（Mob等）
         * @param target 脅威を受けたエンティティ
         * @param amount 脅威度の量
         */
        fun addThreat(source: Entity, target: Entity, amount: Double) {
            val currentThreat = getThreat(source, target)
            setThreat(source, target, currentThreat + amount)
            DebugLogger.debug("Added threat: source=${source.name}, target=${target.name}, amount=$amount, total=${currentThreat + amount}")
        }

        /**
         * 脅威度を設定
         */
        fun setThreat(source: Entity, target: Entity, amount: Double) {
            target.setMetadata(
                getThreatKey(source),
                org.bukkit.metadata.FixedMetadataValue(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("Unique")!!,
                    amount
                )
            )
        }

        /**
         * 脅威度を取得
         */
        fun getThreat(source: Entity, target: Entity): Double {
            val key = getThreatKey(source)
            return if (target.hasMetadata(key)) {
                target.getMetadata(key).firstOrNull()?.asDouble() ?: 0.0
            } else {
                0.0
            }
        }

        /**
         * 脅威度をクリア
         */
        fun clearThreat(source: Entity, target: Entity) {
            target.removeMetadata(getThreatKey(source), org.bukkit.Bukkit.getPluginManager().getPlugin("Unique")!!)
        }

        /**
         * すべての脅威度をクリア
         */
        fun clearAllThreat(source: Entity) {
            // baseTargeterから取得した全ターゲットの脅威度をクリア
            // 実装は呼び出し側で個別に行う
        }

        /**
         * 脅威度のメタデータキーを生成
         */
        private fun getThreatKey(source: Entity): String {
            return "${THREAT_METADATA_KEY}_${source.uniqueId}"
        }
    }

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)
        if (candidates.isEmpty()) {
            DebugLogger.targeter("Threat (no candidates)", 0)
            return emptyList()
        }

        // 脅威度でソート（降順）
        val sorted = candidates.sortedByDescending { getThreat(source, it) }

        // 上位count個を返す
        val selected = sorted.take(count.coerceAtLeast(1))

        // フィルター適用
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Threat (selected top $count by threat)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // PacketEntityの場合はEntityに変換してから取得
        // PacketMobの場合は内部にthreatMapを持つことを想定
        val candidates = baseTargeter.getTargets(source)
        if (candidates.isEmpty()) {
            DebugLogger.targeter("Threat from PacketEntity (no candidates)", 0)
            return emptyList()
        }

        // PacketEntity用の脅威度取得は、将来的にPacketMobに実装
        // 現時点ではメタデータベースのフォールバック
        val sorted = candidates.sortedByDescending { target ->
            // PacketEntityのUUIDをキーとして使用
            if (target.hasMetadata("${THREAT_METADATA_KEY}_packetentity_${source.entityId}")) {
                target.getMetadata("${THREAT_METADATA_KEY}_packetentity_${source.entityId}")
                    .firstOrNull()?.asDouble() ?: 0.0
            } else {
                0.0
            }
        }

        val selected = sorted.take(count.coerceAtLeast(1))
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Threat from PacketEntity (selected top $count by threat)", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Threat-based (top $count) from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "ThreatTargeter[base=${baseTargeter.debugInfo()}, count=$count, filter=${filter ?: "none"}]"
    }
}