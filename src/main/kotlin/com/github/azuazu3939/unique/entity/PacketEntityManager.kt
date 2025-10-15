package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.nms.distanceTo
import com.github.azuazu3939.unique.nms.distanceToAsync
import com.github.azuazu3939.unique.nms.getLocationAsync
import com.github.azuazu3939.unique.nms.getPlayersAsync
import com.github.azuazu3939.unique.util.DebugLogger
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
        val nearbyPlayers = world.getPlayersAsync().filter { player ->
            // NMS拡張関数を使用してメインスレッド外でも安全に距離を計算
            player.distanceToAsync(entity.location) <= viewDistance
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

        // SpawnManagerからスポーンされたPacketMobの場合、スポーンカウントをデクリメント
        if (entity is PacketMob && entity.spawnDefinitionName != null) {
            plugin.spawnManager.decrementSpawnCount(entity.spawnDefinitionName!!)
        }

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
        return entities.values.filter { entity ->
            entity.location.world == location.world &&
                    entity.location.distanceTo(location) <= range
        }
    }

    /**
     * プレイヤーの視界内のエンティティを取得
     */
    fun getEntitiesInView(player: Player, range: Double): List<PacketEntity> {
        return getEntitiesInRange(player.getLocationAsync(), range).filter { entity ->
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
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                updateEntities()
            }, 50L, 50L, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            DebugLogger.error("Failed in startUpdateTask", e)
            e.printStackTrace()
        }
    }

    private fun updateEntities() {
        val entityList = entities.values.toList()

        if (entityList.isEmpty()) return

        // 死亡エンティティのリスト
        val toRemove = mutableListOf<PacketEntity>()

        // メインスレッドで全エンティティを更新
        for (entity in entityList) {
            try {
                entity.tick()
                if (entity.isDead) {
                    toRemove.add(entity)
                }
            } catch (e: Exception) {
                DebugLogger.error("Error ticking entity ${entity.entityId}", e)
            }
        }

        Bukkit.getAsyncScheduler().runDelayed(plugin, {
            // 死亡エンティティを削除
            for (entity in toRemove) {
                try {
                    unregisterEntity(entity.uuid)
                } catch (e: Exception) {
                    DebugLogger.error("Error removing entity ${entity.entityId}", e)
                }
            }
        }, 20 * 50, TimeUnit.MILLISECONDS)
    }

    /**
     * プレイヤー周辺のエンティティを自動スポーン
     */
    suspend fun autoSpawnForPlayer(player: Player, range: Double) {
        val nearbyEntities = getEntitiesInRange(player.getLocationAsync(), range)

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
                entity.location.distanceTo(player.getLocationAsync()) > range) {
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