package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * パケットエンティティマネージャー
 *
 * すべてのパケットエンティティのライフサイクルを管理
 */
class PacketEntityManager(private val plugin: Unique) {

    /**
     * エンティティID生成器
     */
    private val entityIdGenerator = AtomicInteger(1000000)

    /**
     * 全エンティティのマップ（UUID -> PacketEntity）
     */
    private val entities = ConcurrentHashMap<UUID, PacketEntity>()

    /**
     * エンティティIDからUUIDへのマッピング
     */
    private val entityIdToUuid = ConcurrentHashMap<Int, UUID>()

    /**
     * 更新タスク
     */
    private var updateTask: Job? = null

    /**
     * 初期化
     */
    fun initialize() {
        DebugLogger.info("Initializing PacketEntityManager...")

        // 更新タスクを開始
        startUpdateTask()

        DebugLogger.info("PacketEntityManager initialized")
    }

    /**
     * シャットダウン
     */
    suspend fun shutdown() {
        DebugLogger.info("Shutting down PacketEntityManager...")

        // 更新タスクを停止
        updateTask?.cancel()
        updateTask = null

        // すべてのエンティティをクリーンアップ
        val entityList = entities.values.toList()
        for (entity in entityList) {
            entity.cleanup()
        }

        entities.clear()
        entityIdToUuid.clear()

        DebugLogger.info("PacketEntityManager shut down (${entityList.size} entities cleaned)")
    }

    /**
     * エンティティを登録
     */
    fun registerEntity(entity: PacketEntity) {
        entities[entity.uuid] = entity
        entityIdToUuid[entity.entityId] = entity.uuid

        DebugLogger.debug("Registered entity: ${entity.entityId} (${entity.entityType})")
    }

    /**
     * エンティティの登録を解除
     */
    suspend fun unregisterEntity(uuid: UUID) {
        val entity = entities.remove(uuid) ?: return
        entityIdToUuid.remove(entity.entityId)

        entity.cleanup()

        DebugLogger.debug("Unregistered entity: ${entity.entityId}")
    }

    /**
     * エンティティを取得（UUID）
     */
    fun getEntity(uuid: UUID): PacketEntity? {
        return entities[uuid]
    }

    /**
     * エンティティを取得（エンティティID）
     */
    fun getEntityByEntityId(entityId: Int): PacketEntity? {
        val uuid = entityIdToUuid[entityId] ?: return null
        return entities[uuid]
    }

    /**
     * すべてのエンティティを取得
     */
    fun getAllEntities(): Collection<PacketEntity> {
        return entities.values
    }

    /**
     * エンティティ数を取得
     */
    fun getEntityCount(): Int {
        return entities.size
    }

    /**
     * 次のエンティティIDを生成
     */
    fun generateEntityId(): Int {
        return entityIdGenerator.incrementAndGet()
    }

    /**
     * 新しいUUIDを生成
     */
    fun generateUUID(): UUID {
        return UUID.randomUUID()
    }

    /**
     * 範囲内のエンティティを取得
     */
    fun getEntitiesInRange(location: Location, range: Double): List<PacketEntity> {
        val rangeSquared = range * range
        return entities.values.filter { entity ->
            entity.location.world == location.world &&
                    entity.location.distanceSquared(location) <= rangeSquared
        }
    }

    /**
     * プレイヤーの視界内のエンティティを取得
     */
    fun getEntitiesInView(player: Player, range: Double): List<PacketEntity> {
        return getEntitiesInRange(player.location, range).filter { entity ->
            entity.isViewer(player)
        }
    }

    /**
     * 特定タイプのエンティティを取得
     */
    fun getEntitiesByType(type: EntityType): List<PacketEntity> {
        return entities.values.filter { it.entityType == type }
    }

    /**
     * 生存しているエンティティを取得
     */
    fun getAliveEntities(): List<PacketEntity> {
        return entities.values.filter { !it.isDead }
    }

    /**
     * 死亡しているエンティティを取得
     */
    fun getDeadEntities(): List<PacketEntity> {
        return entities.values.filter { it.isDead }
    }

    /**
     * プレイヤーがビューワーのエンティティを取得
     */
    fun getEntitiesViewedBy(player: Player): List<PacketEntity> {
        return entities.values.filter { it.isViewer(player) }
    }

    /**
     * 更新タスクを開始
     */
    private fun startUpdateTask() {
        val updateInterval = plugin.configManager.mainConfig.performance.packetEntityUpdateInterval

        updateTask = plugin.launch {
            while (true) {
                delay(updateInterval * 50L)  // tickをミリ秒に変換

                try {
                    updateEntities()
                } catch (e: Exception) {
                    DebugLogger.error("Error during entity update", e)
                }
            }
        }

        DebugLogger.debug("Update task started (interval: $updateInterval ticks)")
    }

    /**
     * すべてのエンティティを更新
     */
    private suspend fun updateEntities() {
        val startTime = System.nanoTime()

        val entityList = entities.values.toList()
        for (entity in entityList) {
            try {
                entity.tick()
            } catch (e: Exception) {
                DebugLogger.error("Error updating entity ${entity.entityId}", e)
            }
        }

        // 死亡して一定時間経過したエンティティを削除
        val toRemove = entityList.filter { entity ->
            entity.isDead && entity.ticksLived > 100  // 5秒後に削除
        }

        for (entity in toRemove) {
            unregisterEntity(entity.uuid)
        }

        val duration = (System.nanoTime() - startTime) / 1_000_000L
        DebugLogger.timing("Entity update (${entityList.size} entities)", duration)
    }

    /**
     * プレイヤー周辺のエンティティを自動スポーン
     */
    suspend fun autoSpawnForPlayer(player: Player, range: Double) {
        val nearbyEntities = getEntitiesInRange(player.location, range)

        for (entity in nearbyEntities) {
            if (!entity.isViewer(player) && !entity.isDead) {
                entity.spawn(player)
            }
        }
    }

    /**
     * プレイヤーから離れたエンティティを自動デスポーン
     */
    suspend fun autoDespawnForPlayer(player: Player, range: Double) {
        val viewedEntities = getEntitiesViewedBy(player)

        for (entity in viewedEntities) {
            if (entity.location.world != player.world ||
                entity.location.distanceSquared(player.location) > range * range) {
                entity.despawn(player)
            }
        }
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("PacketEntity Debug Info")
        DebugLogger.info("Total entities: ${entities.size}")
        DebugLogger.info("Alive entities: ${getAliveEntities().size}")
        DebugLogger.info("Dead entities: ${getDeadEntities().size}")

        val typeCount = entities.values.groupBy { it.entityType }.mapValues { it.value.size }
        DebugLogger.info("Entities by type:")
        typeCount.forEach { (type, count) ->
            DebugLogger.info("  $type: $count")
        }

        DebugLogger.separator()
    }
}