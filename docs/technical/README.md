# Unique Plugin 技術ドキュメント

このディレクトリには、Unique プラグインの技術的な実装詳細とアーキテクチャに関するドキュメントが含まれています。

## ドキュメント構造

### 📐 Architecture（アーキテクチャ）
プラグインの全体的な設計と構造に関するドキュメント

- [**overview.md**](architecture/overview.md) - プロジェクト全体の概要とアーキテクチャ
- [**module-structure.md**](architecture/module-structure.md) - モジュール構造と依存関係
- [**initialization.md**](architecture/initialization.md) - 初期化フローとライフサイクル

### ⚙️ Core Systems（コアシステム）
プラグインの基盤となるシステム

- [**cel-engine.md**](core-systems/cel-engine.md) - CEL（Common Expression Language）エンジン
- [**config-system.md**](core-systems/config-system.md) - 設定管理システム
- [**event-system.md**](core-systems/event-system.md) - イベントシステムとカスタムイベント

### 🤖 Entity System（エンティティシステム）
パケットベースのカスタムエンティティシステム

- [**packet-entity.md**](entity-system/packet-entity.md) - PacketEntityの基本実装
- [**packet-mob.md**](entity-system/packet-mob.md) - PacketMobの詳細実装
- [**ai-system.md**](entity-system/ai-system.md) - AI行動システム
- [**physics.md**](entity-system/physics.md) - 物理シミュレーションと衝突判定
- [**combat.md**](entity-system/combat.md) - 戦闘システムとダメージ計算

### ✨ Skill System（スキルシステム）
スキル実行とエフェクト管理

- [**skill-executor.md**](skill-system/skill-executor.md) - スキル実行エンジン
- [**skill-types.md**](skill-system/skill-types.md) - スキルタイプの実装詳細

### 💥 Effect System（エフェクトシステム）
エフェクトの実装と管理

- [**effect-overview.md**](effect-system/effect-overview.md) - エフェクトシステムの概要と実装

### 🎯 Targeter System（ターゲターシステム）
ターゲット選択システム

- [**targeter-overview.md**](targeter-system/targeter-overview.md) - ターゲターシステムの概要と実装

### 👾 Mob System（Mobシステム）
Mob管理とスポーンシステム

- [**mob-manager.md**](mob-system/mob-manager.md) - Mob定義の管理とインスタンス化
- [**spawn-system.md**](mob-system/spawn-system.md) - 自動スポーンシステム

## 技術スタック

### 言語・フレームワーク
- **Kotlin** 1.9.0+
- **Paper API** 1.21+
- **MCCoroutine** - コルーチンベースの非同期処理

### 主要ライブラリ
- **PacketEvents** - パケット操作
- **Google CEL** - 動的式評価
- **SnakeYAML** - YAML設定管理

### アーキテクチャパターン
- **コンポーネントベース設計** - PacketMob (AI + Physics + Combat)
- **ファクトリーパターン** - Skill, Effect, Targeter の生成
- **イベント駆動** - カスタムイベントによる拡張性
- **非同期処理** - Kotlin Coroutinesによる並行処理

## 開発の背景

### 設計目標
1. **パフォーマンス** - バニラエンティティに依存しないパケットベース実装
2. **柔軟性** - YAMLによる宣言的なMob定義
3. **拡張性** - プラグインAPIとイベントシステム
4. **安全性** - CEL式による安全な動的評価

### 主要な技術的課題と解決策

#### 1. パケットベースのエンティティ管理
**課題**: バニラエンティティはパフォーマンスとカスタマイズに制限がある

**解決策**:
- PacketEventsを使用した完全なパケット制御
- クライアント側でのみエンティティを表示
- サーバー側で独自の物理・AI処理

#### 2. 動的な値の計算
**課題**: プレイヤー数やMobの体力に応じた動的な値が必要

**解決策**:
- Google CELによる安全な式評価
- キャッシュ機構による高速化
- 事前コンパイルによる最適化

#### 3. 非同期処理とFolia対応
**課題**: Foliaの地域別スレッドモデルへの対応

**解決策**:
- MCCoroutineによるコルーチンベース実装
- regionDispatcherによる地域対応
- globalRegionDispatcherによるグローバル処理

#### 4. 衝突判定と物理シミュレーション
**課題**: パケットエンティティは自動的に衝突判定されない

**解決策**:
- AABB（Axis-Aligned Bounding Box）による衝突判定
- レイキャストによる地形判定
- 独自の重力・速度計算

## 実装状況

### 完了済み機能
✅ パケットエンティティシステム
✅ AI システム（ターゲット検索、追跡、攻撃、徘徊）
✅ 物理シミュレーション（重力、衝突、移動）
✅ スキルシステム（Basic, Projectile, Beam, Aura, Summon）
✅ エフェクトシステム（15種類以上のエフェクト）
✅ ターゲターシステム（10種類以上のターゲッター）
✅ CEL式評価エンジン
✅ 設定管理システム
✅ スポーンシステム
✅ カスタムイベント
✅ コマンドシステム（Brigadier）

### 開発中の機能
🚧 高度なAI行動パターン
🚧 パーティクルアニメーション
🚧 カスタムモデル対応

### 計画中の機能
📋 アイテムシステム
📋 クエストシステム
📋 GUIエディター

## パフォーマンス指標

### 目標値
- **エンティティ数**: サーバーあたり1000体以上
- **CEL評価**: 1ms以内（キャッシュヒット時）
- **スキル実行**: 100件/秒以上
- **メモリ使用量**: エンティティあたり1MB以下

### 最適化手法
- CEL式のキャッシュ
- 非同期処理の活用
- パケット送信の最適化
- データ構造の最適化（ConcurrentHashMap）

## コーディング規約

### Kotlinスタイル
- **命名**: camelCase (変数・関数), PascalCase (クラス)
- **可視性**: 必要最小限の公開（privateデフォルト）
- **Null安全**: Nullable型の明示的な使用
- **拡張関数**: 適切な場所での活用

### ドキュメント
- **KDoc**: public APIには必須
- **コメント**: 複雑なロジックには説明を追加
- **TODO/FIXME**: 課題の明確化

### テスト
- **単体テスト**: 各モジュールごと
- **統合テスト**: システム間の連携

## 参考資料

### 外部ドキュメント
- [Paper API Documentation](https://docs.papermc.io/)
- [PacketEvents Wiki](https://github.com/retrooper/packetevents/wiki)
- [Google CEL Specification](https://github.com/google/cel-spec)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

### 関連プロジェクト
- [MythicMobs](https://mythiccraft.io/index.php?resources/mythicmobs.1/) - インスピレーション元
- [ModelEngine](https://mythiccraft.io/index.php?resources/modelengine%E2%80%94ultimate-entity-model-manager-1-16-5-1-20-4.389/) - モデル管理の参考

## 貢献ガイド

このプロジェクトへの貢献を歓迎します。技術ドキュメントの改善や新機能の提案がある場合は、GitHubでIssueを作成してください。

---

**最終更新**: 2025-10-07
**バージョン**: 1.0.0
**ライセンス**: GNU General Public License v3.0
