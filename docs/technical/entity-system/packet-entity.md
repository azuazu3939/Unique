# Packet Entity System

## 概要

Packet Entity System は、Minecraft のパケットを使用して実体のないエンティティを表現するシステムです。サーバー側でエンティティを管理せず、クライアント側にのみパケットで送信することで、高いパフォーマンスと柔軟性を実現します。

### 主な目的

- **軽量なエンティティ表現**: サーバー側の Entity オブジェクトを生成せず、パケットのみで表現
- **非同期操作**: コルーチンを使用した非同期処理でメインスレッドをブロックしない
- **Folia 対応**: リージョンベースのスレッド処理に対応
- **拡張性**: PacketMob、PacketDisplayEntity など用途に応じた継承

### システム構成要素

1. **PacketEntity**: パケットエンティティの基底クラス
2. **PacketEntityManager**: すべてのパケットエンティティのライフサイクル管理
3. **PacketDisplayEntity**: Display Entity (1.19.4+) 用の実装
4. **PacketMob**: AI、戦闘、物理を持つモブ用の実装（別ドキュメント参照）

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                  PacketEntityManager                         │
│  • エンティティ登録・削除                                     │
│  • エンティティID生成                                        │
│  • 定期更新タスク                                            │
│  • 範囲検索・フィルタリング                                   │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
         ┌────────────────┴────────────────┐
         │                                  │
         ▼                                  ▼
┌──────────────────────┐         ┌───────────────────────┐
│   PacketEntity       │         │  PacketDisplayEntity  │
│   (抽象基底クラス)    │         │  (Display Entity用)   │
├──────────────────────┤         ├───────────────────────┤
│ • spawn/despawn      │         │ • テキスト表示        │
│ • teleport/move      │         │ • アイテム表示        │
│ • damage/heal        │         │ • ブロック表示        │
│ • updateMetadata     │         │ • ビルボードモード    │
│ • playAnimation      │         │ • スケール調整        │
│ • viewer管理         │         └───────────────────────┘
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   PacketMob          │
│   (モブ実装)          │
├──────────────────────┤
│ • AI                 │
│ • 戦闘               │
│ • 物理               │
│ • スキル             │
└──────────────────────┘
```

## 実装詳細

### PacketEntity 基底クラス

すべてのパケットエンティティの基底となる抽象クラス。

```kotlin
/**
 * パケットエンティティ基底クラス
 *
 * 実体を持たないエンティティをパケットで表現
 * 非同期操作が可能で、サーバー負荷が軽い
 */
abstract class PacketEntity(
    val entityId: Int,
    val uuid: UUID,
    val entityType: EntityType,
    initialLocation: Location
) {

    /**
     * 現在の座標
     */
    var location: Location = initialLocation.clone()
        protected set

    /**
     * 体力
     */
    var health: Double = 20.0

    /**
     * 最大体力
     */
    var maxHealth: Double = 20.0

    /**
     * 死亡フラグ
     */
    var isDead: Boolean = false

    /**
     * 生存時間（tick）
     */
    var ticksLived: Int = 0
        protected set

    /**
     * 死亡した時刻（tick）
     */
    var deathTick: Int = -1

    /**
     * このエンティティを見ているプレイヤー
     */
    val viewers = ConcurrentHashMap.newKeySet<UUID>()!!

    /**
     * アクティブなコルーチンジョブ
     */
    protected val activeJobs = ConcurrentHashMap<String, Job>()

    /**
     * カスタムメタデータ
     */
    protected val metadata = ConcurrentHashMap<String, Any>()

    /**
     * エンティティをスポーン（プレイヤーに表示）
     */
    abstract suspend fun spawn(player: Player)

    /**
     * エンティティを削除（プレイヤーから非表示）
     */
    abstract suspend fun despawn(player: Player)

    /**
     * エンティティを移動
     */
    abstract suspend fun teleport(newLocation: Location)

    /**
     * 相対移動
     */
    abstract suspend fun move(deltaX: Double, deltaY: Double, deltaZ: Double)

    /**
     * メタデータを更新
     */
    abstract suspend fun updateMetadata()

    /**
     * アニメーションを再生
     */
    abstract suspend fun playAnimation(animation: EntityAnimation)

    /**
     * ダメージを受ける
     */
    open suspend fun damage(amount: Double) {
        if (isDead) return

        health = (health - amount).coerceAtLeast(0.0)

        if (health <= 0.0) {
            kill()
        }

        updateMetadata()
    }

    /**
     * 回復
     */
    open suspend fun heal(amount: Double) {
        if (isDead) return

        health = (health + amount).coerceAtMost(maxHealth)
        updateMetadata()
    }

    /**
     * エンティティを殺す
     */
    open suspend fun kill() {
        if (isDead) return

        isDead = true
        health = 0.0
        deathTick = ticksLived

        // 死亡アニメーション
        playAnimation(EntityAnimation.DEATH)

        DebugLogger.debug("PacketEntity killed: $entityId ($entityType), deathTick=$deathTick")
    }

    /**
     * 更新処理（1tick毎に呼ばれる）
     */
    open suspend fun tick() {
        ticksLived++
        if (isDead) return
    }

    /**
     * クリーンアップ
     */
    open suspend fun cleanup() {
        DebugLogger.info("Cleaning up PacketEntity: $entityId")

        despawnForAll(viewers.toSet())

        // ジョブをキャンセル
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()

        viewers.clear()

        DebugLogger.info("PacketEntity cleaned up: $entityId")
    }
}
```

**主な機能:**

1. **エンティティ情報**: entityId、UUID、EntityType、位置
2. **ヘルス管理**: health、maxHealth、isDead
3. **ビューワー管理**: どのプレイヤーに表示されているか追跡
4. **非同期操作**: すべての主要メソッドが suspend 関数
5. **カスタムメタデータ**: 任意のデータを格納可能
6. **ジョブ管理**: 非同期タスクの追跡とキャンセル

### PacketEntityManager

すべてのパケットエンティティのライフサイクルを管理するマネージャークラス。

```kotlin
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
     * エンティティを登録
     */
    suspend fun registerEntity(entity: PacketEntity) {
        entities[entity.uuid] = entity
        entityIdToUuid[entity.entityId] = entity.uuid

        DebugLogger.debug("Registered entity: ${entity.entityId} (${entity.entityType})")

        // 登録時に近くのプレイヤーにスポーンパケットを送信
        val world = entity.location.world ?: return

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
     * 更新タスクを開始
     */
    private fun startUpdateTask() {
        val updateInterval = plugin.configManager.mainConfig.performance.packetEntityUpdateInterval

        updateTask = plugin.launch(plugin.globalRegionDispatcher) {
            while (true) {
                delay(updateInterval * 50L)

                try {
                    updateEntities()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    DebugLogger.error("Error during entity update", e)
                }
            }
        }

        DebugLogger.info("PacketEntity update task started (interval: $updateInterval ticks)")
    }

    /**
     * すべてのエンティティを更新
     */
    private suspend fun updateEntities() {
        val entityList = entities.values.toList()
        if (entityList.isEmpty()) return

        // エンティティをワールドごとにグループ化
        val entitiesByWorld = entityList.groupBy { it.location.world }

        val config = plugin.configManager.mainConfig.performance
        val toRemove = mutableListOf<PacketEntity>()
        val jobs = mutableListOf<Job>()

        for ((world, worldEntities) in entitiesByWorld) {
            if (world == null) continue

            // 各エンティティを並列で更新
            for (entity in worldEntities) {
                try {
                    val job = plugin.launch(plugin.regionDispatcher(entity.location)) {
                        try {
                            entity.tick()

                            // 死亡して一定時間経過したエンティティをマーク
                            if (entity.isDead && entity.deathTick >= 0) {
                                val ticksSinceDeath = entity.ticksLived - entity.deathTick
                                if (ticksSinceDeath > config.deadEntityCleanupTicks) {
                                    synchronized(toRemove) {
                                        toRemove.add(entity)
                                    }
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            DebugLogger.error("Error ticking entity ${entity.entityId}", e)
                        }
                    }
                    jobs.add(job)
                } catch (e: Exception) {
                    DebugLogger.error("Error launching entity update ${entity.entityId}", e)
                }
            }
        }

        // すべてのtick処理が完了するまで待機
        jobs.joinAll()

        // バッチで削除
        if (toRemove.isNotEmpty()) {
            delay(config.batchCleanupDelayMs)
            for (entity in toRemove) {
                try {
                    unregisterEntity(entity.uuid)
                } catch (e: Exception) {
                    DebugLogger.error("Error removing entity ${entity.entityId}", e)
                }
            }
        }
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
}
```

**主な機能:**

1. **エンティティ登録**: 新しいエンティティの登録と自動スポーン
2. **定期更新**: すべてのエンティティを定期的に更新
3. **並列処理**: Folia のリージョンディスパッチャーを使用した並列更新
4. **自動削除**: 死亡後一定時間経過したエンティティを自動削除
5. **範囲検索**: 特定の位置・範囲内のエンティティを取得

### PacketDisplayEntity

Display Entity (1.19.4+) 用の実装。ダメージを受けず、装飾専用です。

```kotlin
/**
 * Display Entity用パケットエンティティ
 *
 * ダメージを受けず、AIも持たない装飾専用エンティティ
 * 主にホログラム、看板、装飾などに使用
 */
class PacketDisplayEntity(
    entityId: Int,
    uuid: UUID,
    location: Location,
    val displayType: DisplayType,
    val displayName: String
) : PacketEntity(entityId, uuid, determineEntityType(displayType), location) {

    /**
     * 表示テキスト（TEXT_DISPLAY用）
     */
    var text: String = ""
        set(value) {
            field = value
            viewers.forEach { playerId ->
                Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * アイテム（ITEM_DISPLAY用）
     */
    var itemStack: ItemStack? = null
        set(value) {
            field = value
            viewers.forEach { playerId ->
                Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * ブロックデータ（BLOCK_DISPLAY用）
     */
    var blockData: BlockData? = null
        set(value) {
            field = value
            viewers.forEach { playerId ->
                Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * 拡大率
     */
    var scale: Float = 1.0f
        set(value) {
            field = value
            viewers.forEach { playerId ->
                Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * ビルボードモード
     */
    var billboardMode: BillboardMode = BillboardMode.FIXED

    /**
     * Display Entityはダメージを受けない
     */
    override fun canTakeDamage(): Boolean = false

    /**
     * スポーン処理
     */
    override suspend fun spawn(player: Player) {
        try {
            PacketSender.sendSpawnDisplayPacket(player, this)
            addViewer(player)
            DebugLogger.verbose("Spawned $displayType display for ${player.name}")
        } catch (e: Exception) {
            DebugLogger.error("Failed to spawn display entity for ${player.name}", e)
        }
    }

    /**
     * デスポーン処理
     */
    override suspend fun despawn(player: Player) {
        try {
            PacketSender.sendDespawnPacket(player, entityId)
            removeViewer(player)
        } catch (e: Exception) {
            DebugLogger.error("Failed to despawn display entity for ${player.name}", e)
        }
    }

    override suspend fun teleport(newLocation: Location) {
        location = newLocation.clone()
        viewers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                PacketSender.sendTeleportPacket(player, entityId, newLocation)
            }
        }
    }
}

/**
 * Display Entity種別
 */
enum class DisplayType {
    TEXT,       // テキスト表示
    ITEM,       // アイテム表示
    BLOCK       // ブロック表示
}

/**
 * ビルボードモード
 */
enum class BillboardMode {
    FIXED,          // 固定（回転しない）
    VERTICAL,       // 垂直軸のみ回転
    HORIZONTAL,     // 水平軸のみ回転
    CENTER          // 完全ビルボード（常にプレイヤーを向く）
}
```

**主な機能:**

1. **3種類の表示**: テキスト、アイテム、ブロック
2. **リアルタイム更新**: プロパティ変更時に自動的にパケット送信
3. **ビルボード機能**: プレイヤーの視点に応じた回転
4. **スケール調整**: 表示サイズの変更
5. **ダメージ無効**: 装飾専用のため、ダメージを受けない

## 使用例

### 基本的なエンティティの作成とスポーン

```kotlin
// PacketEntityManagerの取得
val entityManager = plugin.packetEntityManager

// エンティティIDとUUIDを生成
val entityId = entityManager.generateEntityId()
val uuid = entityManager.generateUUID()

// カスタムエンティティを作成（PacketEntityを継承）
class MyCustomEntity(
    entityId: Int,
    uuid: UUID,
    location: Location
) : PacketEntity(entityId, uuid, EntityType.ZOMBIE, location) {

    override suspend fun spawn(player: Player) {
        PacketSender.sendSpawnMobPacket(player, this)
        addViewer(player)
    }

    override suspend fun despawn(player: Player) {
        PacketSender.sendDespawnPacket(player, entityId)
        removeViewer(player)
    }

    override suspend fun teleport(newLocation: Location) {
        location = newLocation.clone()
        viewers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                PacketSender.sendTeleportPacket(player, entityId, newLocation)
            }
        }
    }

    override suspend fun move(deltaX: Double, deltaY: Double, deltaZ: Double) {
        location.add(deltaX, deltaY, deltaZ)
        viewers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                PacketSender.sendRelativeMovePacket(player, entityId, deltaX, deltaY, deltaZ)
            }
        }
    }

    override suspend fun updateMetadata() {
        viewers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                PacketSender.sendMetadataPacket(player, this)
            }
        }
    }

    override suspend fun playAnimation(animation: EntityAnimation) {
        viewers.forEach { playerId ->
            Bukkit.getPlayer(playerId)?.let { player ->
                PacketSender.sendAnimationPacket(player, entityId, animation)
            }
        }
    }
}

// エンティティを作成・登録
val entity = MyCustomEntity(entityId, uuid, location)
entityManager.registerEntity(entity)
```

### Display Entity の作成

```kotlin
// テキスト表示の作成
val textDisplay = PacketDisplayEntity.builder(
    entityId = entityManager.generateEntityId(),
    uuid = entityManager.generateUUID(),
    location = player.location.add(0.0, 2.0, 0.0),
    displayType = DisplayType.TEXT,
    displayName = "MyTextDisplay"
)
    .text("Hello, World!")
    .scale(2.0f)
    .billboardMode(BillboardMode.CENTER)
    .backgroundColor(0x80000000.toInt()) // 半透明の黒
    .build()

entityManager.registerEntity(textDisplay)

// アイテム表示の作成
val itemDisplay = PacketDisplayEntity.builder(
    entityId = entityManager.generateEntityId(),
    uuid = entityManager.generateUUID(),
    location = location,
    displayType = DisplayType.ITEM,
    displayName = "MyItemDisplay"
)
    .itemStack(ItemStack(Material.DIAMOND_SWORD))
    .scale(3.0f)
    .billboardMode(BillboardMode.CENTER)
    .build()

entityManager.registerEntity(itemDisplay)

// ブロック表示の作成
val blockDisplay = PacketDisplayEntity.builder(
    entityId = entityManager.generateEntityId(),
    uuid = entityManager.generateUUID(),
    location = location,
    displayType = DisplayType.BLOCK,
    displayName = "MyBlockDisplay"
)
    .blockData(Material.DIAMOND_BLOCK.createBlockData())
    .scale(1.5f)
    .build()

entityManager.registerEntity(blockDisplay)
```

### エンティティの動的な更新

```kotlin
// テキストを変更
textDisplay.text = "Updated Text!"

// スケールを変更
textDisplay.scale = 3.0f

// 位置を変更
textDisplay.teleport(newLocation)

// 相対移動
textDisplay.move(0.0, 0.1, 0.0)  // Y方向に0.1ブロック上昇
```

### エンティティの検索

```kotlin
// 範囲内のエンティティを取得
val nearbyEntities = entityManager.getEntitiesInRange(player.location, 20.0)

// 特定タイプのエンティティを取得
val zombies = entityManager.getEntitiesByType(EntityType.ZOMBIE)

// 生存しているエンティティを取得
val aliveEntities = entityManager.getAliveEntities()

// プレイヤーが見ているエンティティを取得
val viewedEntities = entityManager.getEntitiesViewedBy(player)
```

### カスタムメタデータの使用

```kotlin
// メタデータを設定
entity.setMetadata("owner", player.uniqueId)
entity.setMetadata("spawn_time", System.currentTimeMillis())
entity.setMetadata("custom_data", mapOf("key" to "value"))

// メタデータを取得
val owner: UUID? = entity.getMetadata("owner")
val spawnTime: Long? = entity.getMetadata("spawn_time")

// メタデータの存在確認
if (entity.hasMetadata("owner")) {
    println("Owner: ${entity.getMetadata<UUID>("owner")}")
}

// メタデータを削除
entity.removeMetadata("custom_data")
```

## パフォーマンス最適化

### ビューワー管理の最適化

不要なプレイヤーへのパケット送信を避けます。

```kotlin
// 距離に応じた自動スポーン/デスポーン
suspend fun updateViewers(entity: PacketEntity, viewDistance: Double) {
    val world = entity.location.world ?: return

    for (player in world.players) {
        val distance = player.location.distance(entity.location)

        if (distance <= viewDistance && !entity.isViewer(player)) {
            // スポーン
            entity.spawn(player)
        } else if (distance > viewDistance && entity.isViewer(player)) {
            // デスポーン
            entity.despawn(player)
        }
    }
}
```

### バッチ処理

複数のエンティティを一度に処理します。

```kotlin
// 複数エンティティの一括スポーン
suspend fun spawnMultiple(entities: List<PacketEntity>, player: Player) {
    entities.forEach { entity ->
        entity.spawn(player)
    }
}

// 複数エンティティの一括削除
suspend fun removeMultiple(entities: List<PacketEntity>) {
    entities.forEach { entity ->
        entityManager.unregisterEntity(entity.uuid)
    }
}
```

### 非同期処理

重い処理は非同期で実行します。

```kotlin
// 非同期でエンティティを更新
plugin.launch {
    entity.updateMetadata()
    delay(100)
    entity.playAnimation(EntityAnimation.SWING_MAIN_HAND)
}
```

## ベストプラクティス

### 1. 適切なエンティティIDの生成

```kotlin
// 良い例 - マネージャーを使用
val entityId = entityManager.generateEntityId()

// 悪い例 - ランダムな値を使用（衝突の可能性）
val entityId = Random.nextInt()
```

### 2. エンティティの適切なクリーンアップ

```kotlin
// 良い例 - マネージャーを使用して削除
entityManager.unregisterEntity(entity.uuid)

// 悪い例 - 直接削除（ビューワーに残る）
entities.remove(entity.uuid)
```

### 3. エラーハンドリング

```kotlin
suspend fun safeSpawn(entity: PacketEntity, player: Player) {
    try {
        entity.spawn(player)
    } catch (e: Exception) {
        DebugLogger.error("Failed to spawn entity for ${player.name}", e)
        // フォールバック処理
    }
}
```

### 4. メモリリークの防止

```kotlin
override suspend fun cleanup() {
    // ビューワーをクリア
    despawnForAll(viewers.toSet())
    viewers.clear()

    // ジョブをキャンセル
    activeJobs.values.forEach { it.cancel() }
    activeJobs.clear()

    // メタデータをクリア
    metadata.clear()

    // 親クラスのcleanup
    super.cleanup()
}
```

## 関連ドキュメント

- [Packet Mob](packet-mob.md) - PacketMob の実装詳細
- [AI System](ai-system.md) - PacketMob の AI システム
- [Physics](physics.md) - PacketMob の物理シミュレーション
- [Combat](combat.md) - PacketMob の戦闘システム

## まとめ

Packet Entity System は Unique プラグインの軽量なエンティティ表現を実現する重要なシステムです。以下の特徴を持ちます:

1. **軽量**: サーバー側の Entity オブジェクトを生成せず、パケットのみで表現
2. **非同期**: コルーチンを使用した非同期処理
3. **Folia 対応**: リージョンベースのスレッド処理に対応
4. **拡張性**: 用途に応じて継承して機能を追加可能
5. **高パフォーマンス**: ビューワー管理とバッチ処理による最適化

Packet Entity System を適切に活用することで、多数のカスタムエンティティを効率的に管理できます。
