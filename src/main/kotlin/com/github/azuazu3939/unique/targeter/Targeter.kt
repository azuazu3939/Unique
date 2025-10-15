package com.github.azuazu3939.unique.targeter

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

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
            EntityTypeFilter.PLAYER -> entities.filterIsInstance<org.bukkit.entity.Player>()
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
                ThreatManager.getThreat(source, it)
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
                    // 数値に変換
                    when (val result = evaluator.evaluate(expression, context)) {
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
                if (it.hasMetadata("${ThreatManager.THREAT_METADATA_KEY}_packetentity_${source.entityId}")) {
                    it.getMetadata("${ThreatManager.THREAT_METADATA_KEY}_packetentity_${source.entityId}")
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
                    // 数値に変換
                    when (val result = evaluator.evaluate(expression, context)) {
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
 * 脅威度管理オブジェクト
 *
 * ThreatTargeterや他のターゲッターで使用される脅威度の管理機能を提供
 */
object ThreatManager {
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
     * 脅威度のメタデータキーを生成
     */
    private fun getThreatKey(source: Entity): String {
        return "${THREAT_METADATA_KEY}_${source.uniqueId}"
    }
}
