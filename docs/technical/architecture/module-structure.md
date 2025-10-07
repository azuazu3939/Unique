# モジュール構造

## ソースコード構成

```
src/main/kotlin/com/github/azuazu3939/unique/
├── Unique.kt                    # メインプラグインクラス
├── Bootstrap.kt                 # Paper Plugin Bootstrap
│
├── cel/                         # CEL式評価エンジン
│   ├── CELEngineManager.kt     # CELエンジン管理
│   ├── CELEvaluator.kt         # 式評価実行
│   └── CELVariableProvider.kt  # 変数プロバイダー
│
├── config/                      # 設定管理
│   ├── ConfigManager.kt        # 設定マネージャー
│   ├── MainConfig.kt           # メイン設定クラス
│   └── YamlLoader.kt           # YAML読み込み
│
├── entity/                      # エンティティシステム
│   ├── PacketEntity.kt         # 基底エンティティクラス
│   ├── PacketMob.kt            # Mobエンティティ
│   ├── PacketMobAI.kt          # AI処理
│   ├── PacketMobPhysics.kt     # 物理シミュレーション
│   ├── PacketMobCombat.kt      # 戦闘システム
│   ├── PacketDisplayEntity.kt  # ディスプレイエンティティ
│   ├── PacketEntityManager.kt  # エンティティマネージャー
│   ├── AIBehaviorType.kt       # AI行動タイプ
│   ├── AIMovementType.kt       # AI移動タイプ
│   ├── packet/
│   │   └── PacketSender.kt     # パケット送信ユーティリティ
│   └── physics/
│       ├── AABB.kt             # 衝突ボックス
│       └── CollisionDetector.kt # 衝突判定
│
├── skill/                       # スキルシステム
│   ├── Skill.kt                # スキルインターフェース
│   ├── SkillExecutor.kt        # スキル実行エンジン
│   ├── SkillFactory.kt         # スキルファクトリー
│   └── types/
│       ├── AuraSkill.kt        # オーラスキル
│       ├── BeamSkill.kt        # ビームスキル
│       ├── ProjectileSkill.kt  # 発射物スキル
│       └── SummonSkill.kt      # 召喚スキル
│
├── effect/                      # エフェクトシステム
│   ├── Effect.kt               # エフェクトインターフェース
│   └── EffectFactory.kt        # エフェクトファクトリー
│
├── targeter/                    # ターゲターシステム
│   ├── Targeter.kt             # ターゲッターインターフェース
│   └── TargeterFactory.kt      # ターゲッターファクトリー
│
├── condition/                   # 条件システム
│   └── Condition.kt            # 条件評価
│
├── mob/                         # Mob管理
│   ├── MobManager.kt           # Mob定義管理
│   └── MobDefinition.kt        # Mob定義クラス
│
├── spawn/                       # スポーンシステム
│   ├── SpawnManager.kt         # スポーン管理
│   └── SpawnDefinition.kt      # スポーン定義
│
├── player/                      # プレイヤーデータ
│   ├── PlayerDataManager.kt    # プレイヤーデータ管理
│   └── PlayerData.kt           # プレイヤーデータクラス
│
├── event/                       # カスタムイベント
│   ├── PacketMobEvent.kt       # Mobイベント
│   └── UniqueReloadEvent.kt    # リロードイベント
│
├── listener/                    # イベントリスナー
│   └── MobListener.kt          # Mobイベントリスナー
│
└── util/                        # ユーティリティ
    ├── CompactSyntaxParser.kt  # 構文パーサー
    ├── InlineSkillParser.kt    # インラインスキルパーサー
    ├── ResourceKeyResolver.kt  # リソースキー解決
    ├── TimeParser.kt           # 時間パース
    ├── LocationUtil.kt         # 位置計算
    ├── EventUtil.kt            # イベントユーティリティ
    ├── DebugLogger.kt          # デバッグログ
    └── Extentions.kt           # Kotlin拡張関数
```

## モジュール詳細

### 1. Core Module（コアモジュール）

#### Unique.kt
**役割**: プラグインのエントリーポイント
- SuspendingJavaPluginを継承
- 各マネージャーの初期化
- フォルダ構造の作成
- ライフサイクル管理

**主要メソッド**:
```kotlin
override suspend fun onLoadAsync()    // プラグイン読み込み
override fun onEnable()               // プラグイン有効化
override suspend fun onDisableAsync() // プラグイン無効化
private fun initializeFolders()       // フォルダ初期化
private fun initializeCEL()           // CELエンジン初期化
```

#### Bootstrap.kt
**役割**: Paper Plugin Bootstrapの実装
- Brigadierコマンド登録
- ライフサイクルイベント処理

**コマンド**:
- `/unique reload` - 設定リロード
- `/unique spawn <mob>` - Mobスポーン
- `/unique list <type>` - リスト表示
- `/unique debug <type>` - デバッグ情報
- `/unique info` - プラグイン情報

### 2. CEL Module（式評価モジュール）

#### CELEngineManager.kt
**役割**: CELエンジンの管理とキャッシュ
- 式のコンパイルとキャッシュ
- 評価タイムアウト管理
- パフォーマンス統計

**主要フィールド**:
```kotlin
private val compiledExpressions: LRUCache<String, Program>
private val cacheSize: Int = 1000
private val evaluationTimeout: Long = 100
```

#### CELVariableProvider.kt
**役割**: CEL式で使用可能な変数の提供
- エンティティ変数（health, maxHealth, etc.）
- ワールド変数（time, difficulty, etc.）
- プレイヤー変数（nearbyPlayers.count, etc.）
- ダメージ変数（damage.amount, etc.）

### 3. Config Module（設定モジュール）

#### ConfigManager.kt
**役割**: 設定ファイルの管理
- config.ymlの読み込み
- 設定の検証
- リロード処理

#### MainConfig.kt
**役割**: 設定値の保持
```kotlin
data class MainConfig(
    val debug: DebugConfig,
    val performance: PerformanceConfig,
    val cel: CELConfig,
    val spawn: SpawnConfig
)
```

### 4. Entity Module（エンティティモジュール）

#### PacketEntity.kt
**役割**: パケットエンティティの基底クラス
- エンティティIDとUUIDの管理
- 位置と回転の管理
- Viewerの管理（誰に見えるか）

**主要フィールド**:
```kotlin
val entityId: Int
val uuid: UUID
var location: Location
var health: Double
var maxHealth: Double
val viewers: MutableSet<UUID>
```

#### PacketMob.kt
**役割**: カスタムMobの実装
- AIコンポーネント
- Physicsコンポーネント
- Combatコンポーネント
- カスタム名、発光、不可視などのプロパティ

**コンポーネント構成**:
```kotlin
private val physics = PacketMobPhysics(this)
private val ai = PacketMobAI(this, physics)
internal val combat = PacketMobCombat(this)
```

#### PacketMobAI.kt
**役割**: Mobの行動ロジック
- ターゲット検索
- 追跡行動
- 攻撃判定
- 徘徊処理

**AI状態**:
```kotlin
enum class AIState {
    IDLE,    // 待機中
    TARGET,  // ターゲット追跡中
    ATTACK,  // 攻撃中
    WANDER   // 徘徊中
}
```

#### PacketMobPhysics.kt
**役割**: 物理シミュレーション
- 重力計算
- 衝突判定
- 移動処理
- 地形判定

#### PacketMobCombat.kt
**役割**: 戦闘システム
- ダメージ計算
- 防具値による軽減
- ノックバック
- 死亡処理

#### PacketEntityManager.kt
**役割**: エンティティのライフサイクル管理
- エンティティ生成・削除
- Tick処理の管理
- Viewer管理

**主要メソッド**:
```kotlin
fun createPacketMob(...): PacketMob
fun destroyEntity(entityId: Int)
suspend fun tickAllEntities()
```

### 5. Skill Module（スキルモジュール）

#### SkillExecutor.kt
**役割**: スキル実行エンジン
- 非同期スキル実行
- ジョブ管理
- 最大同時実行数制御

**主要メソッド**:
```kotlin
fun executeSkill(skill: Skill, source: Entity, targeter: Targeter): Long
fun cancelJob(jobId: Long)
fun getActiveJobCount(): Int
```

#### SkillFactory.kt
**役割**: スキルインスタンスの生成
- YAMLからのスキル生成
- インラインスキル構文のパース
- スキルタイプの登録

**スキルタイプ**:
- `basic` - BasicSkill
- `projectile` - ProjectileSkill
- `beam` - BeamSkill
- `aura` - AuraSkill
- `summon` - SummonSkill

### 6. Effect Module（エフェクトモジュール）

#### EffectFactory.kt
**役割**: エフェクトインスタンスの生成
- YAMLからのエフェクト生成
- エフェクトタイプの登録

**エフェクトタイプ**（一部抜粋）:
- `damage` - ダメージ
- `heal` - 回復
- `explosion` - 爆発
- `lightning` - 雷
- `particle` - パーティクル
- `sound` - サウンド
- `potioneffect` - ポーション効果

### 7. Targeter Module（ターゲターモジュール）

#### TargeterFactory.kt
**役割**: ターゲッターインスタンスの生成
- YAMLからのターゲッター生成
- ターゲッタータイプの登録

**ターゲッタータイプ**（一部抜粋）:
- `self` - 自分
- `nearestplayer` - 最寄りプレイヤー
- `radiusplayers` - 範囲内プレイヤー
- `lowesthealth` - 最低体力
- `random` - ランダム

### 8. Mob Module（Mobモジュール）

#### MobManager.kt
**役割**: Mob定義の管理
- YAMLからのMob定義読み込み
- Mobのスポーン
- 定義の検索

**主要メソッド**:
```kotlin
fun loadMobDefinitions()
fun spawnMob(mobName: String, location: Location): PacketMob?
fun getMobDefinition(mobName: String): MobDefinition?
```

#### MobDefinition.kt
**役割**: Mob定義の保持
```kotlin
data class MobDefinition(
    val type: EntityType,
    val display: String?,
    val health: String,
    val damage: String,
    val skills: List<SkillEntry>,
    val ai: AIOptions,
    val appearance: AppearanceOptions,
    ...
)
```

### 9. Spawn Module（スポーンモジュール）

#### SpawnManager.kt
**役割**: 自動スポーンシステム
- スポーン定義の読み込み
- 定期的なスポーン処理
- スポーン条件の評価（CEL式）

**主要メソッド**:
```kotlin
fun loadSpawnDefinitions()
fun startSpawnTasks()
fun stopSpawnTasks()
```

### 10. Utility Module（ユーティリティモジュール）

#### CompactSyntaxParser.kt
**役割**: コンパクト構文のパース
```kotlin
// 例: projectile{type=FIREBALL;speed=2.5} @NP{r=30} ~onTimer:30
fun parseCompactSkillSyntax(line: String): ParsedSkillEntry
```

#### TimeParser.kt
**役割**: 時間単位のパース
```kotlin
fun parseTime(value: String): Long
// 30 -> 30 ticks
// 3s -> 60 ticks
// 5m -> 6000 ticks
```

#### ResourceKeyResolver.kt
**役割**: Minecraftリソースキーの解決
```kotlin
fun resolveEntityType(key: String): EntityType
fun resolveParticle(key: String): Particle
fun resolveSound(key: String): Sound
```

## モジュール間の依存関係

```
Unique (Main)
    ├─→ CELEngineManager
    ├─→ ConfigManager
    ├─→ PacketEntityManager
    │       ├─→ PacketMob
    │       │      ├─→ PacketMobAI
    │       │      ├─→ PacketMobPhysics
    │       │      └─→ PacketMobCombat
    │       └─→ PacketDisplayEntity
    ├─→ SkillExecutor
    │       └─→ SkillFactory
    │              ├─→ EffectFactory
    │              └─→ TargeterFactory
    ├─→ MobManager
    │       └─→ MobDefinition
    └─→ SpawnManager
            └─→ SpawnDefinition
```

## 拡張性の設計

### ファクトリーパターンによる拡張
新しいスキル/エフェクト/ターゲッターを追加する際、ファクトリーに登録するだけで利用可能になります。

```kotlin
// 例: カスタムスキルの登録
SkillFactory.register("myskill") { params ->
    MyCustomSkill(params)
}
```

### イベントシステムによる拡張
カスタムイベントをリッスンすることで、外部プラグインから機能を拡張できます。

```kotlin
@EventHandler
fun onMobSpawn(event: PacketMobSpawnEvent) {
    // カスタム処理
}
```

---

**関連ドキュメント**:
- [アーキテクチャ概要](overview.md)
- [初期化フロー](initialization.md)
