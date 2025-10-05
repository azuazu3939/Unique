# Unique - AI-Friendly Mob Creation Plugin

MythicMobsより実装が容易で、AIが書きやすいMob/Item作成プラグイン

## 🎯 特徴

### 1. **AIフレンドリー設計**
- YAMLベースの構造化された設定
- CEL（Common Expression Language）で条件や効果をラッピング
- デフォルト値により冗長性を排除
- 明確で予測可能なAPI

### 2. **高パフォーマンス**
- **Folia + Paper** 完全対応
- **パケットエンティティ** による軽量なMob実装
- **MCコルーチン** による効率的な非同期処理
- Paper UserDev フル活用

### 3. **高カスタマイズ性**
- ターゲッター、スキル、コンディションをYAMLで定義可能
- プラグイン本体の改変不要
- 全ての処理ロジックをYAMLで記述可能

## 📁 ディレクトリ構造

```
plugins/Unique/
├── config.yml          # プラグイン設定
├── mobs/              # Mob定義YAML
├── spawns/            # スポーン管理YAML
├── skills/            # スキル定義YAML
├── conditions/        # 条件定義YAML
├── targeters/         # ターゲッター定義YAML
├── effects/           # エフェクト定義YAML
└── sample/            # サンプルファイル（開発時）
```

## 🚀 クイックスタート

### 基本的なMob定義

```yaml
# mobs/custom_zombie.yml
CustomZombie:
  Type: ZOMBIE
  Display: '&cカスタムゾンビ'
  Health: 100

  Skills:
    OnTimer:
      - name: FireballAttack
        interval: 100
        targeter:
          type: NearestPlayer
          range: 20
        skills:
          - skill: FireballSkill
```

### 時間指定の例

```yaml
skills:
  - skill: DelayedSkill
    meta:
      executeDelay: 1s      # 1秒後に実行
      effectDelay: 500ms    # さらに500ms後に効果
      sync: false           # 非同期実行
```

## 🔧 技術スタック

- **言語**: Kotlin 1.9.22
- **サーバー**: Paper API 1.21.8
- **非同期**: MCコルーチン 2.15.0
- **条件エンジン**: CEL-Java 0.4.4
- **設定**: Hoplite 2.7.5（型安全なYAML読込）
- **エンティティ**: PacketEvents（パケットエンティティ）

## 📝 開発状況

### Phase 1: 基礎ユーティリティ ✅
- [x] TimeParser - 時間単位変換
- [x] LocationUtil - 座標計算
- [x] DebugLogger - デバッグログ
- [x] 基本的なディレクトリ構造

### Phase 2: CEL統合 ✅
- [x] CELEngineManager - CELエンジン管理とキャッシュ
- [x] CELVariableProvider - 変数提供システム
- [x] CELEvaluator - 高レベル評価API

### Phase 3: 設定システム ✅
- [x] ConfigManager - Hoplite統合設定管理
- [x] MainConfig - 型安全な設定データクラス
- [x] YamlLoader - 軽量YAML読み込みヘルパー

### Phase 4: パケットエンティティ ✅
- [x] PacketEntity - パケットエンティティ基底クラス
- [x] PacketEntityManager - エンティティライフサイクル管理
- [x] PacketMob - Mob実装
- [x] PacketSender - パケット送信ヘルパー（スタブ）

### Phase 5: コアシステム ✅
- [x] Condition - 条件システム（CEL統合）
- [x] Targeter - ターゲッターシステム
- [x] Effect - エフェクトシステム
- [x] Skill - スキルシステム基底
- [x] SkillExecutor - スキル実行エンジン
- [x] MobDefinition - Mob定義データクラス
- [x] MobManager - Mob管理とYAML読み込み

### Phase 6: スポーンシステム ✅
- [x] SpawnDefinition - スポーン定義データクラス
- [x] SpawnManager - スポーン管理と自動スポーン
- [x] 条件評価システム（CEL統合）
- [x] 範囲指定スポーン（円形・矩形）

### Phase 7: 統合とテスト
- [ ] イベントリスナー
- [ ] コマンドハンドラー
- [ ] 総合テスト

## 📖 ドキュメント

詳細なドキュメントは、docsフォルダー内のMarkDownファイルを参照してください。

## 🤝 コントリビューション

このプロジェクトは現在開発中です。

## 📄 ライセンス

MIT License

## 🔗 リンク

- GitHub: https://github.com/azuazu3939/Unique