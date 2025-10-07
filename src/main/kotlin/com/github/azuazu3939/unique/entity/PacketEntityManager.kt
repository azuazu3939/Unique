package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.util.DebugLogger
import kotlinx.coroutines.CancellationException
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
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
     * 初期化
     */
    fun initialize() {
        DebugLogger.info("Initializing PacketEntityManager...")

        try {
            // 更新タスクを開始
            DebugLogger.info("Starting update task...")
            startUpdateTask()
            DebugLogger.info("Update task start completed")
        } catch (e: Exception) {
            DebugLogger.error("Failed to start update task", e)
            e.printStackTrace()
        }

        DebugLogger.info("PacketEntityManager initialized")
    }

    /**
     * シャットダウン
     */
    suspend fun shutdown() {
        DebugLogger.info("Shutting down PacketEntityManager...")

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
     * 注：この関数は既にregion dispatcherのコンテキスト内で呼ばれることを前提とする
     */
    suspend fun registerEntity(entity: PacketEntity) {
        entities[entity.uuid] = entity
        entityIdToUuid[entity.entityId] = entity.uuid

        DebugLogger.debug("Registered entity: ${entity.entityId} (${entity.entityType})")

        // 登録時に近くのプレイヤーにスポーンパケットを送信
        val world = entity.location.world
        if (world == null) {
            DebugLogger.error("Entity world is null! Cannot spawn for players")
            return
        }

        // 設定から可視距離を取得
        val viewDistance = plugin.configManager.mainConfig.performance.viewDistance
        val nearbyPlayers = world.players.filter { player ->
            player.location.distance(entity.location) <= viewDistance
        }

        for (player in nearbyPlayers) {
            try {
                entity.spawn(player)
            } catch (e: Exception) {
                DebugLogger.error("Failed to spawn entity for player ${player.name}", e)
            }
        }
    }

    /**
     * エンティティの登録を解除
     */
    fun unregisterEntity(uuid: UUID) {
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
        try {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, {
                updateEntities()
            }, 1, 1)
        } catch (e: Exception) {
            DebugLogger.error("Failed in startUpdateTask", e)
            e.printStackTrace()
        }
    }

    /**
     * すべてのエンティティを更新
     * 最適化：エンティティごとに並列処理、バッチ削除
     */
    private fun updateEntities() {
        val startTime = System.nanoTime()
        val entityList = entities.values.toList()

        if (entityList.isEmpty()) return

        // エンティティをワールドごとにグループ化して効率化
        val entitiesByWorld = entityList.groupBy { it.location.world }

        // 死亡エンティティのリスト
        val toRemove = ConcurrentHashMap.newKeySet<PacketEntity>()

        for ((world, worldEntities) in entitiesByWorld) {
            if (world == null) continue

            // 各エンティティを並列で更新（それぞれのregionで実行）
            for (entity in worldEntities) {
                try {
                    // 非同期で更新開始
                    Bukkit.getRegionScheduler().run(plugin, entity.location) {
                        try {
                            entity.tick()
                            // 死亡して一定時間経過したエンティティをマーク（設定可能）
                            if (entity.isDead) {
                                synchronized(toRemove) {
                                    toRemove.add(entity)

                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            DebugLogger.error("Error ticking entity ${entity.entityId}", e)
                        }
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        Bukkit.getAsyncScheduler().runDelayed(plugin, {
            if (toRemove.isNotEmpty()) {
                for (entity in toRemove) {
                    try {
                        unregisterEntity(entity.uuid)
                    } catch (e: CancellationException) {
                        // 正常なキャンセル - 再スロー
                        throw e
                    } catch (e: Exception) {
                        DebugLogger.error("Error removing entity ${entity.entityId}", e)
                    }
                }
            }
            val duration = (System.nanoTime() - startTime) / 1_000_000L
            DebugLogger.timing("Entity update batch (${entityList.size} entities, ${toRemove.size} removed)", duration)
        }, 1000, TimeUnit.MILLISECONDS)
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