# PacketMob - カスタムMob実装

## 概要

`PacketMob` は、パケットベースのカスタムMobエンティティを実装するクラスです。バニラのエンティティシステムを使用せず、PacketEventsを通じてクライアントにエンティティ情報を送信することで、サーバー負荷を最小限に抑えながら高度にカスタマイズ可能なMobを実現します。

## アーキテクチャ

### コンポーネント分離設計

PacketMobは以下の3つのコンポーネントクラスに機能を分離しています:

```kotlin
class PacketMob(
    entityId: Int,
    uuid: UUID,
    entityType: EntityType,
    location: Location,
    val mobName: String
) : PacketEntity(entityId, uuid, entityType, location) {
    // コンポーネント
    private val physics = PacketMobPhysics(this)
    private val ai = PacketMobAI(this, physics)
    internal val combat = PacketMobCombat(this)
}
```

**設計原則**:
- **単一責任の原則**: 各コンポーネントは明確に定義された責務のみを持つ
- **依存性の注入**: コンポーネント間の依存関係を明示的に管理
- **疎結合**: インターフェースを通じた柔軟な拡張

### クラス図

```
┌─────────────────────────────────────────────────────────┐
│                      PacketEntity                        │
│  (抽象基底クラス)                                           │
├─────────────────────────────────────────────────────────┤
│  + entityId: Int                                        │
│  + uuid: UUID                                           │
│  + entityType: EntityType                               │
│  + location: Location                                   │
│  + health: Double                                       │
│  + maxHealth: Double                                    │
│  + isDead: Boolean                                      │
│  + viewers: Set<UUID>                                   │
├─────────────────────────────────────────────────────────┤
│  + abstract suspend spawn(player: Player)               │
│  + abstract suspend despawn(player: Player)             │
│  + abstract suspend tick()                              │
└─────────────────────────────────────────────────────────┘
                            ▲
                            │ 継承
                            │
┌─────────────────────────────────────────────────────────┐
│                       PacketMob                          │
│  (カスタムMob実装)                                          │
├─────────────────────────────────────────────────────────┤
│  コアプロパティ:                                            │
│  + mobName: String                                      │
│  + customName: String                                   │
│  + customNameVisible: Boolean                           │
│  + hasAI: Boolean                                       │
│  + hasGravity: Boolean                                  │
│  + isGlowing: Boolean                                   │
│  + isInvisible: Boolean                                 │
│                                                         │
│  戦闘プロパティ:                                            │
│  + damage: Double                                       │
│  + armor: Double (0-30)                                 │
│  + armorToughness: Double (0-20)                        │
│  + damageFormula: String?                               │
│                                                         │
│  AIプロパティ:                                             │
│  + movementSpeed: Double                                │
│  + followRange: Double                                  │
│  + attackRange: Double                                  │
│  + attackCooldown: Int                                  │
│  + targetSearchInterval: Int                            │
│  + knockbackResistance: Double                          │
│  + lookAtMovementDirection: Boolean                     │
│  + wallClimbHeight: Double                              │
│                                                         │
│  オプション:                                               │
│  + options: MobOptions                                  │
│                                                         │
│  変数ストレージ:                                            │
│  - variables: MutableMap<String, Any>                   │
├─────────────────────────────────────────────────────────┤
│  コンポーネント:                                            │
│  - physics: PacketMobPhysics                            │
│  - ai: PacketMobAI                                      │
│  - combat: PacketMobCombat                              │
├─────────────────────────────────────────────────────────┤
│  パケット送信メソッド:                                        │
│  + suspend spawn(player: Player)                        │
│  + suspend despawn(player: Player)                      │
│  + suspend teleport(newLocation: Location)              │
│  + suspend move(deltaX, deltaY, deltaZ: Double)         │
│  + suspend updateMetadata()                             │
│  + suspend playAnimation(animation: EntityAnimation)    │
│                                                         │
│  ダメージ関連:                                             │
│  + suspend damage(amount: Double, damager: Entity?)     │
│  + suspend kill()                                       │
│  + canTakeDamage(): Boolean                             │
│                                                         │
│  変数管理:                                                │
│  + setVariable(name: String, value: Any)                │
│  + getVariable(name: String): Any?                      │
│  + getVariables(): Map<String, Any>                     │
│  + clearVariables()                                     │
│                                                         │
│  ヒットボックス:                                            │
│  + getEntityHitboxWidth(): Double                       │
│  + getEntityHitboxHeight(): Double                      │
│                                                         │
│  ダメージランキング:                                         │
│  + getDamageRanking(limit: Int): List<Pair<UUID, Double>>│
│  + getPlayerDamage(player: Player): Double              │
│  + getPlayerRank(player: Player): Int                   │
│  + clearDamageRanking()                                 │
│  + broadcastDamageRanking(limit: Int)                   │
└─────────────────────────────────────────────────────────┘
           │                │                │
           │ 委譲           │ 委譲           │ 委譲
           ▼                ▼                ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
│ PacketMobPhysics │ │ PacketMobAI  │ │ PacketMobCombat  │
│                  │ │              │ │                  │
│ - 重力処理       │ │ - ターゲット │ │ - ダメージ計算   │
│ - 速度管理       │ │ - 移動処理   │ │ - 防御力計算     │
│ - 衝突判定       │ │ - 攻撃判定   │ │ - 死亡処理       │
│                  │ │ - 徘徊処理   │ │ - 攻撃処理       │
└──────────────────┘ └──────────────┘ └──────────────────┘
```

## 主要機能

### 1. パケット通信

PacketMobは以下のパケットを使用してクライアントと通信します:

#### スポーンパケット
```kotlin
suspend fun spawn(player: Player) {
    if (isDead) return
    PacketSender.sendSpawnPacket(player, this)
    addViewer(player)
}
```

送信されるパケット:
- `SpawnEntity` - エンティティの生成
- `EntityMetadata` - 初期メタデータ（名前、発光など）

#### 位置更新パケット

```kotlin
// テレポート（大きな移動）
suspend fun teleport(newLocation: Location) {
    location = newLocation.clone()
    viewers.forEach { player ->
        Bukkit.getPlayer(player)?.let {
            PacketSender.sendTeleportPacket(it, entityId, newLocation)
        }
    }
}

// 相対移動（小さな移動）
suspend fun move(deltaX: Double, deltaY: Double, deltaZ: Double) {
    location.add(deltaX, deltaY, deltaZ)
    viewers.forEach { player ->
       Bukkit.getPlayer(player)?.let {
           PacketSender.sendMovePacket(it, entityId, deltaX, deltaY, deltaZ)
       }
    }
}
```

**パケット選択の基準**:
- 移動距離が8ブロック以上 → `TeleportPacket`
- 移動距離が8ブロック未満 → `MovePacket`（帯域幅節約）

#### メタデータ更新

```kotlin
var customName: String = mobName
    set(value) {
        field = value
        viewers.forEach { player ->
            sendMetadataPacket(Bukkit.getPlayer(player))
        }
    }

var isGlowing: Boolean = false
    set(value) {
        field = value
        viewers.forEach { player ->
            sendMetadataPacket(Bukkit.getPlayer(player))
        }
    }
```

更新可能なメタデータ:
- カスタム名と表示フラグ
- 発光エフェクト
- 透明化
- その他エンティティ固有のフラグ

### 2. ライフサイクル管理

#### 生成フロー

```kotlin
// 1. Builderパターンで生成
val mob = PacketMob.builder(
    entityId = entityId,
    uuid = uuid,
    entityType = EntityType.ZOMBIE,
    location = location,
    mobName = "CustomZombie"
)
    .health(100.0)
    .maxHealth(100.0)
    .damage(10.0)
    .armor(15.0)
    .movementSpeed(0.25)
    .followRange(32.0)
    .build()

// 2. エンティティマネージャーに登録
plugin.packetEntityManager.registerEntity(mob)

// 3. 周囲のプレイヤーにスポーン
// （PacketEntityManagerが自動的に処理）
```

#### 更新フロー（Tick）

```kotlin
override suspend fun tick() {
    super.tick()  // 基底クラスのtick（ticksLivedインクリメント）

    val config = Unique.instance.configManager.mainConfig.performance

    // ダメージ記憶のクリア（設定可能）
    if (combat.shouldClearDamageMemory(ticksLived, config.damageMemoryTicks.toInt())) {
        combat.clearDamageMemory()
    }

    // 重力処理（死亡していない場合のみ）
    if (hasGravity && !isDead) {
        physics.applyGravity()
    }

    // AI処理（最適化：周囲にプレイヤーがいない場合はスキップ）
    if (hasAI && !isDead) {
        if (!config.skipAiWhenNoViewers || viewers.isNotEmpty()) {
            // AI処理を間引き（設定可能）
            if (ticksLived % config.aiTickInterval == 0) {
                ai.tick()
            }
        }
    }

    // 速度を実際の移動に適用
    if (!isDead) {
        physics.applyVelocity()
    }
}
```

**パフォーマンス最適化**:
- ビューワーがいない場合のAIスキップ（設定可能）
- AI処理の間引き（デフォルト: 毎tick、設定で変更可能）
- 死亡後の処理停止

#### 死亡フロー

```kotlin
suspend fun kill() {
    if (isDead) return

    // 1. 死亡フラグを立てる
    isDead = true
    health = 0.0
    deathTick = ticksLived

    // 2. 死亡イベント発火（ドロップ計算含む）
    val deathEvent = PacketMobDeathEvent(mob, killer)
    EventUtil.callEvent(deathEvent)

    // 3. ドロップアイテムをワールドに生成
    dropItemsInWorld(location, deathEvent.drops)

    // 4. OnDeathスキル実行
    mobManager.executeSkillTriggers(...)

    // 5. 死亡アニメーション＆サウンド
    playAnimation(DEATH)
    playDeathSound()

    // 6. MobInstance削除
    mobManager.removeMobInstance(uuid)
}
```

#### クリーンアップ

```kotlin
// PacketEntityManagerが自動的にクリーンアップ
// 設定: performance.dead_entity_cleanup_ticks（デフォルト: 200tick = 10秒）
```

### 3. 変数ストレージ

Mob個体ごとに変数を保存できます。スキル実行時の状態管理に使用されます。

```kotlin
// 変数の設定
mob.setVariable("phase", 2)
mob.setVariable("lastSkillTime", System.currentTimeMillis())

// 変数の取得
val phase = mob.getVariable("phase") as? Int ?: 1

// 全変数の取得
val allVars = mob.getVariables()

// 変数のクリア
mob.clearVariables()
```

**使用例**:
- ボスのフェーズ管理
- スキルのクールダウン
- 一時的なフラグ

### 4. ダメージランキングシステム

プレイヤーごとの累計ダメージを追跡し、ランキング表示が可能です。

```kotlin
// ランキング取得（上位10位まで）
val ranking = mob.getDamageRanking(limit = 10)
// List<Pair<UUID, Double>> - プレイヤーUUIDと累計ダメージ

// 特定プレイヤーの累計ダメージ
val damage = mob.getPlayerDamage(player)

// 特定プレイヤーの順位
val rank = mob.getPlayerRank(player)  // 1位から、ランキング外は-1

// ランキングをクリア
mob.clearDamageRanking()

// ランキングをブロードキャスト（範囲64ブロック以内のプレイヤーに送信）
mob.broadcastDamageRanking(limit = 10)
```

**出力例**:
```
========== CustomBoss Damage Ranking ==========
1. Player1: 1250.5 damage
2. Player2: 980.0 damage
3. Player3: 750.0 damage
...
================================================
```

### 5. ヒットボックス

エンティティタイプに応じた正確なヒットボックスを提供します。

```kotlin
val width = mob.getEntityHitboxWidth()   // 例: ZOMBIE = 0.6
val height = mob.getEntityHitboxHeight() // 例: ZOMBIE = 1.95
```

**対応エンティティタイプ**:
- 人型（Zombie, Skeleton等）: 0.6 x 1.95
- 小型（Silverfish等）: 0.4 x 0.3
- 中型（Pig, Cow等）: 0.9 x 0.9-1.4
- 大型（Iron Golem等）: 1.4 x 2.7
- ボス級（Giant, Wither等）: 3.6-16.0 x 3.5-12.0

### 6. サウンドシステム

エンティティタイプに応じたダメージ/死亡サウンドを自動再生します。

```kotlin
// ダメージサウンド（内部メソッド）
internal fun playHurtSound() {
    val sound = getHurtSound() ?: return
    location.world?.playSound(location, sound, 1.0f, 1.0f)
}

// 死亡サウンド（内部メソッド）
internal fun playDeathSound() {
    val sound = getDeathSound() ?: return
    location.world?.playSound(location, sound, 1.0f, 1.0f)
}
```

**対応サウンド**:
50種類以上のエンティティタイプに対応

**設定による制御**:
```yaml
options:
  silent: true          # すべてのサウンドを無効化
  playHurtSound: false  # ダメージサウンドのみ無効化
```

## MobOptions

Mobの詳細な挙動を制御するオプション設定です。

```kotlin
data class MobOptions(
    // ダメージ設定
    val canTakeDamage: Boolean = true,      // ダメージを受けるか
    val invincible: Boolean = false,         // 無敵フラグ

    // アニメーション設定
    val showDamageAnimation: Boolean = true, // ダメージアニメーション表示

    // サウンド設定
    val silent: Boolean = false,             // すべてのサウンドを無効化
    val playHurtSound: Boolean = true,       // ダメージサウンド再生

    // 死亡設定
    val setAsKiller: Boolean = true          // プレイヤーを殺した時にキラー設定
)
```

**YAML設定例**:
```yaml
CustomBoss:
  Type: ZOMBIE
  Health: 1000
  Options:
    canTakeDamage: true
    invincible: false
    showDamageAnimation: true
    silent: false
    playHurtSound: true
    setAsKiller: true
```

## Builderパターン

PacketMobの生成にはBuilderパターンを使用します。

```kotlin
val mob = PacketMob.builder(
    entityId = entityId,
    uuid = uuid,
    entityType = EntityType.ZOMBIE,
    location = location,
    mobName = "CustomZombie"
)
    // 体力
    .health(100.0)
    .maxHealth(100.0)

    // 戦闘プロパティ
    .damage(10.0)
    .armor(15.0)
    .armorToughness(5.0)
    .damageFormula("damage * (1 - min(20, armor) / 25)")

    // 外観
    .customNameVisible(true)
    .glowing(false)
    .invisible(false)

    // 基本設定
    .hasAI(true)
    .hasGravity(true)

    // AI設定
    .movementSpeed(0.25)
    .followRange(32.0)
    .attackRange(2.0)
    .attackCooldown(20)
    .targetSearchInterval(20)
    .knockbackResistance(0.5)
    .lookAtMovementDirection(true)
    .wallClimbHeight(1.0)

    // オプション
    .options(MobOptions(
        canTakeDamage = true,
        showDamageAnimation = true,
        silent = false
    ))

    .build()
```

## 最適化

### メモリ効率

- **コンポーネント分離**: 機能ごとに分離することでメモリレイアウトを最適化
- **遅延初期化**: 必要になるまでリソースを確保しない
- **ビューワー管理**: Set<UUID>による効率的なビューワー追跡

### CPU効率

- **AI間引き**: 設定により処理頻度を調整可能（デフォルト: 毎tick）
- **ビューワーチェック**: プレイヤーがいない場合はAI処理をスキップ
- **距離判定の最適化**: `distanceSquared()`を使用して平方根計算を回避

### ネットワーク効率

- **パケットバッチング**: 複数の更新を1つのパケットにまとめる
- **差分更新**: 変更があった場合のみパケット送信
- **視界範囲チェック**: 視界外のプレイヤーにはパケット送信しない

## イベント

PacketMobは以下のカスタムイベントを発火します:

### PacketMobSpawnEvent
```kotlin
class PacketMobSpawnEvent(
    val mob: PacketMob,
    val location: Location,
    val mobName: String
) : Event(), Cancellable
```

スポーン前に発火。キャンセル可能。

### PacketMobDamageEvent
```kotlin
class PacketMobDamageEvent(
    val mob: PacketMob,
    val damager: Entity?,
    var damage: Double,
    val cause: DamageCause
) : Event(), Cancellable
```

ダメージを受ける前に発火。ダメージ量の変更やキャンセルが可能。

### PacketMobDeathEvent
```kotlin
class PacketMobDeathEvent(
    val mob: PacketMob,
    val killer: Player?,
    val drops: MutableList<ItemStack>
) : Event()
```

死亡時に発火。ドロップアイテムの追加/削除が可能。

### PacketMobAttackEvent
```kotlin
class PacketMobAttackEvent(
    val mob: PacketMob,
    val target: Entity,
    var damage: Double
) : Event(), Cancellable
```

攻撃実行前に発火。ダメージ量の変更やキャンセルが可能。

### PacketMobSkillEvent
```kotlin
class PacketMobSkillEvent(
    val mob: PacketMob,
    val skillName: String,
    val triggerType: SkillTriggerType
) : Event(), Cancellable
```

スキル実行前に発火。キャンセル可能。

### PacketMobRemoveEvent
```kotlin
class PacketMobRemoveEvent(
    val mob: PacketMob,
    val reason: RemoveReason
) : Event()
```

削除時に発火。理由（死亡、リロード等）を含む。

### PacketMobTargetEvent
```kotlin
class PacketMobTargetEvent(
    val mob: PacketMob,
    val oldTarget: Entity?,
    var newTarget: Entity?,
    val reason: TargetReason
) : Event(), Cancellable
```

ターゲット変更時に発火。新しいターゲットの変更やキャンセルが可能。

## トラブルシューティング

### Mobが表示されない

**原因**:
- プレイヤーがビューワーリストに追加されていない
- パケットが正しく送信されていない
- クライアントのレンダー距離外

**解決方法**:
```kotlin
// デバッグログを有効化
debug:
  enabled: true
  verbose: true
```

### Mobが動かない

**原因**:
- `hasAI: false` に設定されている
- `hasGravity: false` で空中に浮いている
- ビューワーがいないため AI がスキップされている

**解決方法**:
```yaml
AI:
  HasAI: true
  HasGravity: true

performance:
  skip_ai_when_no_viewers: false  # 常にAI実行
```

### ダメージランキングが記録されない

**原因**:
- ダメージがイベントでキャンセルされている
- ダメージソースがプレイヤーではない

**解決方法**:
イベントリスナーを確認し、`PacketMobDamageEvent` がキャンセルされていないことを確認。

### メモリリーク

**原因**:
- 死亡後のMobインスタンスが参照されている
- ビューワーリストが正しくクリアされていない

**解決方法**:
```yaml
performance:
  dead_entity_cleanup_ticks: 200  # 死亡後のクリーンアップ時間を短縮
```

---

**関連ドキュメント**:
- [AI System](ai-system.md) - AI処理の詳細
- [Physics System](physics.md) - 物理演算の詳細
- [Combat System](combat.md) - 戦闘システムの詳細
- [PacketEntity Manager](../core-systems/packet-entity-manager.md) - エンティティ管理
