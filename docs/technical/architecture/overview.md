# アーキテクチャ概要

## プロジェクト概要

**Unique** は、Minecraft Paper サーバー向けの高性能カスタムMobプラグインです。パケットベースのエンティティシステムを採用し、バニラエンティティの制限を超えた柔軟なMob作成を実現しています。

## 設計思想

### 1. パフォーマンス重視
- パケットベースの実装によりサーバー負荷を最小化
- 非同期処理（Kotlin Coroutines）による並行実行
- CEL式のキャッシュによる高速評価

### 2. 宣言的な設定
- YAMLによる直感的なMob定義
- CEL式による動的な値の計算
- コンパクトな構文による可読性

### 3. 拡張性
- ファクトリーパターンによる新機能追加の容易さ
- カスタムイベントによるプラグイン間連携
- モジュラー設計による保守性

## システムアーキテクチャ

```
┌─────────────────────────────────────────────────────────┐
│                    Unique Plugin                        │
├─────────────────────────────────────────────────────────┤
│  Bootstrap (Paper Plugin Lifecycle)                     │
│  ├─ Commands (Brigadier)                                │
│  └─ Lifecycle Events                                    │
├─────────────────────────────────────────────────────────┤
│  Main Plugin Class (SuspendingJavaPlugin)              │
│  ├─ Initialization Flow                                 │
│  ├─ Component Management                                │
│  └─ Shutdown Flow                                       │
├─────────────────────────────────────────────────────────┤
│  Core Systems                                           │
│  ├─ CEL Engine (Expression Evaluation)                  │
│  ├─ Config Manager (YAML Configuration)                 │
│  └─ Event System (Custom Events)                        │
├─────────────────────────────────────────────────────────┤
│  Entity System                                          │
│  ├─ PacketEntityManager (Entity Lifecycle)              │
│  ├─ PacketMob (Custom Mob)                              │
│  │   ├─ PacketMobAI (Behavior Logic)                    │
│  │   ├─ PacketMobPhysics (Movement & Collision)         │
│  │   └─ PacketMobCombat (Damage & Health)               │
│  └─ PacketDisplayEntity (Cosmetic Entity)               │
├─────────────────────────────────────────────────────────┤
│  Skill System                                           │
│  ├─ SkillExecutor (Execution Engine)                    │
│  ├─ SkillFactory (Skill Creation)                       │
│  └─ Skill Types                                         │
│      ├─ BasicSkill                                      │
│      ├─ ProjectileSkill                                 │
│      ├─ BeamSkill                                       │
│      ├─ AuraSkill                                       │
│      └─ SummonSkill                                     │
├─────────────────────────────────────────────────────────┤
│  Effect System                                          │
│  ├─ EffectFactory (Effect Creation)                     │
│  └─ Effect Types (15+ types)                            │
├─────────────────────────────────────────────────────────┤
│  Targeter System                                        │
│  ├─ TargeterFactory (Targeter Creation)                 │
│  └─ Targeter Types (10+ types)                          │
├─────────────────────────────────────────────────────────┤
│  Mob System                                             │
│  ├─ MobManager (Definition Management)                  │
│  ├─ MobDefinition (Mob Configuration)                   │
│  └─ SpawnManager (Auto Spawn System)                    │
├─────────────────────────────────────────────────────────┤
│  Player System                                          │
│  └─ PlayerDataManager (Player State)                    │
├─────────────────────────────────────────────────────────┤
│  Utility                                                │
│  ├─ CompactSyntaxParser (Syntax Parsing)                │
│  ├─ ResourceKeyResolver (Resource Keys)                 │
│  ├─ TimeParser (Time Units)                             │
│  └─ DebugLogger (Logging)                               │
└─────────────────────────────────────────────────────────┘
           ↓                    ↓                  ↓
   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
   │ PacketEvents │   │  Paper API   │   │ Google CEL   │
   └──────────────┘   └──────────────┘   └──────────────┘
```

## レイヤーアーキテクチャ

### Layer 1: Bootstrap & Lifecycle
**責務**: プラグインの初期化とライフサイクル管理
- `Bootstrap.kt` - Paper Plugin Bootstrap
- `Unique.kt` - メインプラグインクラス
- コマンド登録（Brigadier）
- イベント登録

### Layer 2: Core Systems
**責務**: プラグインの基盤機能
- **CEL Engine**: 動的式の評価とキャッシュ
- **Config Manager**: YAML設定の読み込みと検証
- **Event System**: カスタムイベントの管理

### Layer 3: Entity System
**責務**: カスタムエンティティの管理と制御
- **PacketEntityManager**: エンティティのライフサイクル管理
- **PacketMob**: カスタムMobの実装
  - AI: ターゲット検索、追跡、攻撃
  - Physics: 重力、衝突、移動
  - Combat: ダメージ計算、体力管理

### Layer 4: Skill System
**責務**: スキルの実行と管理
- **SkillExecutor**: 非同期スキル実行
- **SkillFactory**: スキルインスタンスの生成
- **Skill Types**: 各種スキルの実装

### Layer 5: Effect & Targeter System
**責務**: エフェクトの適用とターゲット選択
- **EffectFactory**: エフェクトインスタンスの生成
- **TargeterFactory**: ターゲッターインスタンスの生成

### Layer 6: Mob System
**責務**: Mob定義の管理とスポーン
- **MobManager**: Mob定義の読み込みとインスタンス化
- **SpawnManager**: 自動スポーンシステム

### Layer 7: Utility
**責務**: 共通機能の提供
- パース処理
- リソースキー解決
- 時間単位変換
- ログ出力

## データフロー

### Mob スポーンフロー
```
1. MobManager.loadMobDefinitions()
   └─> YAML読み込み → MobDefinition生成

2. MobManager.spawnMob(mobName, location)
   └─> PacketEntityManager.createPacketMob()
       └─> PacketMob生成
           ├─> PacketMobAI初期化
           ├─> PacketMobPhysics初期化
           └─> PacketMobCombat初期化

3. PacketEntityManager.registerEntity()
   └─> エンティティ登録 → Tick開始
```

### スキル実行フロー
```
1. Mobのスキルトリガー発動（例: onTimer）
   └─> SkillExecutor.executeSkill()

2. Targeterによるターゲット選択
   └─> @NearestPlayer{r=30} など

3. Skillの実行
   ├─> ProjectileSkill: 発射体生成 → 軌道計算 → 衝突判定
   ├─> BeamSkill: レイキャスト → 範囲判定 → エフェクト適用
   └─> AuraSkill: 範囲検索 → 定期エフェクト適用

4. Effectの適用
   └─> DamageEffect, ParticleEffect, SoundEffect など
```

### CEL式評価フロー
```
1. YAML読み込み時
   └─> "100 + (nearbyPlayers.count * 50)"

2. CELEngineManager.evaluate()
   ├─> キャッシュチェック
   │   ├─> ヒット: キャッシュから返却
   │   └─> ミス: 以下の処理
   └─> CELコンパイル → 変数バインド → 評価 → キャッシュ保存
```

## 並行処理モデル

### Kotlin Coroutines
```kotlin
// 非同期スキル実行
plugin.launch {
    skill.execute(plugin, source, targeter)
}

// 地域別実行（Folia対応）
plugin.launch(plugin.regionDispatcher(location)) {
    // 特定地域での処理
}

// グローバル実行
plugin.launch(plugin.globalRegionDispatcher) {
    // ワールド横断処理
}
```

### 並行性の管理
- **ConcurrentHashMap**: スレッドセーフなコレクション
- **AtomicLong**: アトミックカウンター
- **Job管理**: アクティブなコルーチンの追跡とキャンセル

## パケット通信

### PacketEvents統合
```
Client (Player)  ←────→  Server (Unique Plugin)
                 Packets

送信パケット:
├─ SpawnEntity (エンティティ生成)
├─ EntityMetadata (メタデータ更新)
├─ EntityTeleport (位置移動)
├─ EntityRotation (回転)
├─ EntityAnimation (アニメーション)
└─ DestroyEntities (エンティティ削除)
```

### パケット最適化
- バッチ送信による帯域幅削減
- 視界範囲外のエンティティはスキップ
- 更新頻度の調整（tick間隔）

## 設定管理

### 階層構造
```
config.yml (メイン設定)
├─ debug (デバッグ設定)
├─ performance (パフォーマンス設定)
├─ cel (CEL設定)
└─ spawn (スポーン設定)

mobs/*.yml (Mob定義)
├─ basic_mobs.yml
├─ boss_mobs.yml
└─ ...

spawns/*.yml (スポーン定義)
└─ world_spawns.yml

libs/Effects/*.yml (エフェクトライブラリ)
└─ ...
```

## 拡張ポイント

### 1. カスタムスキル追加
```kotlin
// 1. Skillインターフェースを実装
class CustomSkill : Skill {
    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        // 実装
    }
}

// 2. SkillFactoryに登録
SkillFactory.register("custom") { params -> CustomSkill() }
```

### 2. カスタムエフェクト追加
```kotlin
// 1. Effectインターフェースを実装
class CustomEffect : Effect {
    override suspend fun apply(source: Entity, target: Entity) {
        // 実装
    }
}

// 2. EffectFactoryに登録
EffectFactory.register("custom") { params -> CustomEffect() }
```

### 3. カスタムイベントリスナー
```kotlin
@EventHandler
fun onReload(event: UniqueReloadAfterEvent) {
    // リロード後の処理
}
```

## パフォーマンス考慮事項

### 最適化手法
1. **CEL式キャッシュ**: 同じ式の再評価を回避
2. **非同期処理**: ブロッキング処理を回避
3. **パケットバッチング**: 複数パケットをまとめて送信
4. **空間分割**: 地域別の処理（Folia対応）

### メモリ管理
- WeakReference による不要オブジェクトの自動解放
- ConcurrentHashMap によるスレッドセーフなキャッシュ
- 定期的なクリーンアップタスク

## セキュリティ

### CEL式の安全性
- サンドボックス化された式評価
- タイムアウト設定（デフォルト100ms）
- 危険な操作の制限

### 権限管理
- `unique.command` - コマンド実行
- `unique.reload` - リロード実行
- `unique.spawn` - Mobスポーン
- `unique.debug` - デバッグ情報表示

## 今後の展開

### Phase 3（計画中）
- アイテムシステム
- GUIエディター
- データベース連携
- マルチワールド対応強化

---

**関連ドキュメント**:
- [モジュール構造](module-structure.md)
- [初期化フロー](initialization.md)
