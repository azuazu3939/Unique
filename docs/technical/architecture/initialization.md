# 初期化フロー

## プラグイン起動シーケンス

### 1. Bootstrap Phase

```
Paper Server起動
    ↓
PluginBootstrap.bootstrap()  [Bootstrap.kt]
    ├─ LifecycleEvents.COMMANDS 登録
    │   └─ UniqueCommand (Brigadier) 登録
    │       ├─ /unique
    │       ├─ /unique reload
    │       ├─ /unique spawn <mob>
    │       ├─ /unique list <type>
    │       ├─ /unique debug <type>
    │       └─ /unique info
    └─ 他のライフサイクルイベント登録
```

### 2. Load Phase

```
Plugin.onLoad()
    ↓
Unique.onLoadAsync()  [Unique.kt:90-94]
    ├─ instance = this (シングルトン設定)
    ├─ PacketEvents API構築
    │   └─ SpigotPacketEventsBuilder.build(instance)
    └─ PacketEvents.getAPI().load()
```

### 3. Enable Phase

```
Plugin.onEnable()
    ↓
Unique.onEnable()  [Unique.kt:99-136]
    └─ launch (Kotlin Coroutine起動)
        └─ withContext(globalRegionDispatcher)
            ├─ PacketEvents.getAPI().init()  ← パケットイベント初期化
            ├─ initializeFolders()           ← フォルダ構造作成
            ├─ initializeConfig()            ← 設定読み込み
            ├─ initializeCEL()               ← CELエンジン起動
            ├─ initializeSkillExecutor()     ← スキル実行エンジン起動
            ├─ initializeMobManager()        ← Mob管理初期化
            ├─ initializeSpawnManager()      ← スポーン管理初期化
            ├─ initializePacketEntityManager() ← エンティティ管理初期化
            ├─ initializePlayerDataManager() ← プレイヤーデータ管理初期化
            └─ registerEvents(MobListener)   ← イベントリスナー登録
```

## 詳細初期化フロー

### initializeFolders() [Unique.kt:163-254]

```
フォルダ構造作成
    ├─ dataFolder/ (plugins/Unique/)
    │   ├─ mobs/                ← Mob定義
    │   │   ├─ basic_mobs.yml
    │   │   ├─ boss_mobs.yml
    │   │   ├─ advanced_features_showcase.yml
    │   │   ├─ options_and_display_examples.yml
    │   │   └─ resource_key_examples.yml
    │   ├─ spawns/              ← スポーン定義
    │   │   └─ world_spawns.yml
    │   ├─ skills/              ← スキルライブラリ
    │   │   ├─ combat_skills.yml
    │   │   ├─ aura_beam_skills.yml
    │   │   └─ projectile_skills.yml
    │   ├─ conditions/          ← 条件ライブラリ
    │   ├─ targeters/           ← ターゲッターライブラリ
    │   ├─ effects/             ← エフェクトライブラリ
    │   ├─ libs/                ← 共有ライブラリ
    │   │   ├─ Effects/
    │   │   │   ├─ area_effects.yml
    │   │   │   ├─ damage_effects.yml
    │   │   │   ├─ support_effects.yml
    │   │   │   ├─ utility_effects.yml
    │   │   │   └─ visual_effects.yml
    │   │   ├─ Targeters/
    │   │   │   └─ custom_targeters.yml
    │   │   └─ Triggers/
    │   │       └─ custom_triggers.yml
    │   ├─ ai/                  ← AI設定
    │   │   ├─ behaviors/
    │   │   └─ movements/
    │   └─ sample/              ← サンプルファイル
    │       ├─ README.md
    │       ├─ ai_behavior_examples.yml
    │       ├─ complete_example.yml
    │       └─ ...
    └─ サンプルファイルの自動生成（初回のみ）
```

**処理詳細**:
1. 各フォルダの存在チェック
2. 存在しない場合はフォルダ作成
3. 初回起動時のみサンプルファイルをリソースからコピー
4. DebugLoggerでログ出力

### initializeConfig() [Unique.kt:281-294]

```
設定管理の初期化
    ├─ ConfigManager生成
    ├─ ConfigManager.loadConfigs()
    │   ├─ config.yml読み込み
    │   │   ├─ debug設定
    │   │   ├─ performance設定
    │   │   ├─ cel設定
    │   │   └─ spawn設定
    │   └─ MainConfigオブジェクト生成
    └─ ConfigManager.validateConfigs()
        └─ 設定値の妥当性チェック
```

**設定項目例**:
```yaml
debug:
  enabled: false
  logLevel: INFO

performance:
  maxConcurrentSkills: 100
  entityTickRate: 1

cel:
  cacheSize: 1000
  evaluationTimeout: 100

spawn:
  enabled: true
  checkRadius: 32.0
```

### initializeCEL() [Unique.kt:259-276]

```
CELエンジン初期化
    ├─ MainConfig.celから設定取得
    │   ├─ cacheSize (デフォルト: 1000)
    │   └─ evaluationTimeout (デフォルト: 100ms)
    ├─ CELEngineManager生成
    │   ├─ LRUCache<String, Program>初期化
    │   ├─ CEL環境構築
    │   │   ├─ 標準関数登録
    │   │   └─ カスタム関数登録
    │   └─ タイムアウトハンドラ設定
    └─ CELEvaluator生成
        └─ CELEngineManagerをラップ
```

**CEL評価フロー**:
```
式: "100 + (nearbyPlayers.count * 50)"
    ↓
1. キャッシュチェック
    ├─ ヒット → キャッシュから返却
    └─ ミス → 以下の処理
2. CELコンパイル
    └─ Program生成
3. 変数バインド
    └─ nearbyPlayers.count = 3
4. 評価実行
    └─ 結果: 250
5. キャッシュ保存
```

### initializeSkillExecutor() [Unique.kt:299-306]

```
スキル実行エンジン初期化
    └─ SkillExecutor生成
        ├─ activeJobs: ConcurrentHashMap<Long, Job>
        ├─ jobIdCounter: AtomicLong(0)
        └─ executionCount: AtomicLong(0)
```

**スキル実行時のフロー**:
```
SkillExecutor.executeSkill()
    ├─ jobId生成 (incrementAndGet)
    ├─ 最大同時実行数チェック
    └─ launch (Coroutine起動)
        ├─ executionCount++
        ├─ skill.execute()
        ├─ executionCount--
        └─ activeJobs.remove(jobId)
```

### initializeMobManager() [Unique.kt:311-320]

```
Mob管理の初期化
    ├─ MobManager生成
    └─ MobManager.loadMobDefinitions()
        ├─ mobsFolder内のYAMLファイル列挙
        ├─ 各YAMLファイルを読み込み
        │   ├─ Mob名をキーとして取得
        │   ├─ MobDefinition生成
        │   │   ├─ type: EntityType
        │   │   ├─ display: String
        │   │   ├─ health: String (CEL式対応)
        │   │   ├─ damage: String (CEL式対応)
        │   │   ├─ skills: List<SkillEntry>
        │   │   ├─ ai: AIOptions
        │   │   └─ appearance: AppearanceOptions
        │   └─ spawnDefinitionsマップに追加
        └─ ロード完了ログ出力 (Loaded X mobs)
```

**Mob定義読み込み例**:
```yaml
FireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "150"
  damage: "25"
  Skills:
    - projectile{...} @NP{r=30} ~onTimer:30
```

### initializeSpawnManager() [Unique.kt:325-335]

```
スポーン管理の初期化
    ├─ SpawnManager生成
    ├─ SpawnManager.loadSpawnDefinitions()
    │   └─ spawnsFolder内のYAMLファイル読み込み
    │       ├─ SpawnDefinition生成
    │       │   ├─ mob: String
    │       │   ├─ location: Location
    │       │   ├─ spawnRate: Long
    │       │   ├─ maxCount: Int
    │       │   └─ condition: String (CEL式)
    │       └─ spawnDefinitionsマップに追加
    └─ SpawnManager.startSpawnTasks()
        └─ 各スポーン定義ごとに定期タスク起動
            └─ launch (Coroutine)
                ├─ delay(spawnRate)
                ├─ 条件チェック (CEL式評価)
                ├─ スポーン数チェック
                └─ MobManager.spawnMob()
```

### initializePacketEntityManager() [Unique.kt:340-348]

```
パケットエンティティ管理の初期化
    ├─ PacketEntityManager生成
    └─ PacketEntityManager.initialize()
        ├─ entities: ConcurrentHashMap初期化
        ├─ entityIdCounter: AtomicInteger(1000)初期化
        └─ グローバルTickタスク起動
            └─ server.globalRegionScheduler.runAtFixedRate()
                └─ tickAllEntities() (毎tick実行)
                    └─ 全エンティティのtick()呼び出し
```

**エンティティTickフロー**:
```
PacketEntityManager.tickAllEntities()
    └─ for each entity in entities.values
        └─ entity.tick()
            ├─ ticksLived++
            ├─ PacketMobAI.tick()
            │   ├─ ターゲット検索
            │   ├─ 状態遷移 (IDLE/TARGET/ATTACK/WANDER)
            │   └─ 移動処理
            ├─ PacketMobPhysics.tick()
            │   ├─ 重力適用
            │   ├─ 速度計算
            │   ├─ 衝突判定
            │   └─ 位置更新
            └─ スキル実行チェック
                └─ onTimerトリガー処理
```

### initializePlayerDataManager() [Unique.kt:353-373]

```
プレイヤーデータ管理の初期化
    └─ PlayerDataManager生成
        └─ playerData: ConcurrentHashMap<UUID, PlayerData>
```

**将来の拡張（現在は無効化）**:
```
// マナ/MPシステム（計画中）
// server.globalRegionScheduler.runAtFixedRate()
//     └─ playerDataManager.tickAllPlayers() (毎tick)
//         └─ マナ自然回復処理
```

### イベントリスナー登録 [Unique.kt:129]

```
MobListener登録
    ├─ PlayerJoinEvent
    │   └─ 既存エンティティの可視化
    ├─ PlayerQuitEvent
    │   └─ Viewerリストからプレイヤー削除
    └─ EntityDamageByEntityEvent
        └─ PacketMobの被ダメージ処理
```

## Reload フロー

### /unique reload コマンド

```
UniqueCommand.handleReload()  [Bootstrap.kt:159-205]
    ├─ UniqueReloadBeforeEvent発火
    │   ├─ isCancelled チェック
    │   └─ キャンセル時は中断
    ├─ ConfigManager.reloadConfigs()
    │   └─ YAML再読み込み
    ├─ MobManager.loadMobDefinitions()
    │   └─ Mob定義再読み込み
    ├─ SpawnManager.loadSpawnDefinitions()
    │   └─ スポーン定義再読み込み
    ├─ SpawnManager.restartSpawnTasks()
    │   ├─ 既存タスクキャンセル
    │   └─ 新しいタスク起動
    └─ UniqueReloadAfterEvent発火
        ├─ success: Boolean
        ├─ errorMessage: String?
        ├─ mobCount: Int
        ├─ spawnCount: Int
        └─ duration: Long
```

**リロードイベントの活用例**:
```kotlin
@EventHandler
fun onReloadAfter(event: UniqueReloadAfterEvent) {
    if (event.success) {
        logger.info("リロード成功: ${event.mobCount}体のMob")
    } else {
        logger.warn("リロードエラー: ${event.errorMessage}")
    }
}
```

## Shutdown フロー

```
Plugin.onDisable()
    ↓
Unique.onDisableAsync()  [Unique.kt:141-158]
    ├─ PlayerDataManager.shutdown()
    │   └─ プレイヤーデータ保存
    ├─ PacketEntityManager.shutdown()
    │   ├─ 全エンティティのTickタスクキャンセル
    │   ├─ 全プレイヤーから全エンティティを非表示
    │   └─ entitiesマップクリア
    ├─ CELEngineManager.printDebugInfo()
    │   ├─ 評価回数統計
    │   ├─ キャッシュヒット率
    │   └─ 平均評価時間
    └─ DebugLogger.info("Unique plugin disabled.")
```

## パフォーマンス最適化

### 初期化時の最適化
1. **並行初期化**: 独立したコンポーネントは並行初期化
2. **遅延初期化**: 必要になるまで初期化を遅らせる
3. **キャッシュウォームアップ**: よく使う式を事前コンパイル

### Tick処理の最適化
1. **間引き**: 重い処理は間隔を空けて実行
2. **非同期化**: ブロッキング処理をCoroutineで非同期化
3. **バッチ処理**: 複数の操作をまとめて実行

## トラブルシューティング

### 初期化エラーの確認
```
plugins/Unique/logs/latest.log
```

### デバッグモード有効化
```yaml
# config.yml
debug:
  enabled: true
  logLevel: DEBUG
```

### 初期化時間の測定
各初期化メソッドで `System.currentTimeMillis()` を使用して時間を測定し、ログ出力しています。

```
[Unique] CEL engine initialized (15ms)
[Unique] Configuration loaded (8ms)
[Unique] SkillExecutor initialized (2ms)
[Unique] MobManager initialized with 10 mobs (45ms)
[Unique] SpawnManager initialized with 3 spawns (12ms)
[Unique] PacketEntityManager initialized (5ms)
```

---

**関連ドキュメント**:
- [アーキテクチャ概要](overview.md)
- [モジュール構造](module-structure.md)
