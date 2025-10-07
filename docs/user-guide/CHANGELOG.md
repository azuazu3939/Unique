# 変更履歴 (Changelog)

Uniqueプラグインのバージョンごとの変更内容を記録します。

---

## Phase 2 完了 (2025-10-06)

### 🎉 新機能

#### スキルシステム
- ✅ **BeamSkill**: 直線状のレーザービーム攻撃
  - レイキャスト命中判定
  - 経路上の全エンティティにダメージ
  - 貫通/非貫通の選択可能
  - カスタマイズ可能なパーティクルとサウンド

- ✅ **AuraSkill**: 持続的な範囲効果
  - 範囲内のエンティティに継続的にエフェクト適用
  - カスタマイズ可能な更新間隔
  - 自分への影響の有無を選択可能
  - 最大ターゲット数の制限機能

#### エフェクトシステム
- ✅ **TeleportEffect**: テレポート効果
  - 3つのモード: TO_SOURCE, TO_TARGET, RANDOM
  - ランダムテレポート範囲指定可能

- ✅ **PullEffect**: 引き寄せ効果
  - CEL式対応の強度設定

- ✅ **PushEffect**: 押し出し効果
  - CEL式対応の強度設定

- ✅ **BlindEffect**: 盲目効果
  - CEL式対応の持続時間
  - 効果レベル設定可能

- ✅ **SetBlockEffect**: ブロック設置/破壊
  - 範囲設置機能
  - 一時的な設置機能（自動消滅）
  - CEL式対応

#### ターゲッターシステム
- ✅ **NearestTargeter**: 最も近いターゲットを選択
  - ベースターゲッター方式
  - フィルター機能

- ✅ **FarthestTargeter**: 最も遠いターゲットを選択
  - ベースターゲッター方式
  - フィルター機能

- ✅ **ThreatTargeter**: 脅威度ベースのターゲット選択
  - MMORPGスタイルの脅威度管理
  - 上位N体を選択可能
  - Entity Metadataで脅威度を管理

### 🔧 改善

- ✅ **EffectFactory**: エフェクト生成の統一化
- ✅ **TargeterFactory**: ターゲッター生成の統一化
- ✅ **SkillFactory**: スキル生成の統一化
- ✅ **MobDefinition**: 50+ 新フィールド追加（後方互換性維持）

### 🐛 バグ修正

- ✅ **CEL API**: `evaluateAsDouble()` → `evaluateNumber()` に修正
- ✅ **ThreatTargeter**: 重複フィルターメソッドを削除
- ✅ **BlindEffect/SetBlockEffect**: `applyEffect()` → `apply()` に修正

### 📚 ドキュメント

- ✅ **NEW_FEATURES_GUIDE.md**: Phase 2機能の完全ガイド
- ✅ **サンプルファイル**: 新機能のサンプル追加
  - `new_skills_examples.yml`
  - `new_effects_examples.yml`
  - `new_targeters_examples.yml`

---

## Phase 1 完了 (2025-10-05)

### 🎉 新機能

#### Factory/Parserシステム
- ✅ **EffectFactory**: YAML → Effect変換
  - 17種類のエフェクトをサポート
  - 型安全なパース処理
  - エラーハンドリング

- ✅ **TargeterFactory**: YAML → Targeter変換
  - 12種類のターゲッターをサポート
  - 再帰的なパース処理（ネストされたターゲッター対応）
  - エラーハンドリング

- ✅ **SkillFactory**: YAML → Skill変換
  - 6種類のスキルをサポート
  - MetaSkillのフェーズ対応
  - BranchSkillの分岐対応
  - エラーハンドリング

#### スキルシステム
- ✅ **BasicSkill**: 基本エフェクト
- ✅ **ProjectileSkill**: 発射体攻撃
  - 8種類の発射体タイプ
  - ホーミング機能
  - 重力/貫通設定
- ✅ **MetaSkill**: フェーズ制御
- ✅ **BranchSkill**: 条件分岐
- ✅ **SummonSkill**: Mob召喚

#### エフェクトシステム（基本17種）
- ✅ **DamageEffect**: ダメージ
- ✅ **HealEffect**: 回復
- ✅ **PotionEffectEffect**: ポーション効果
- ✅ **ParticleEffect**: パーティクル
- ✅ **SoundEffect**: サウンド
- ✅ **MessageEffect**: メッセージ
- ✅ **CommandEffect**: コマンド実行
- ✅ **VelocityEffect**: 速度変更
- ✅ **LightningEffect**: 雷
- ✅ **ExplosionEffect**: 爆発
- ✅ **FreezeEffect**: 凍結
- ✅ **ShieldEffect**: シールド

#### ターゲッターシステム（基本9種）
- ✅ **SelfTargeter**: 自分自身
- ✅ **NearestPlayerTargeter**: 最も近いプレイヤー
- ✅ **RadiusPlayersTargeter**: 範囲内の全プレイヤー
- ✅ **RadiusEntitiesTargeter**: 範囲内の全エンティティ
- ✅ **LowestHealthTargeter**: 最も体力が低いターゲット
- ✅ **RandomTargeter**: ランダムなターゲット
- ✅ **AreaTargeter**: 範囲ターゲット
- ✅ **ChainTargeter**: 連鎖ターゲット

#### CEL式サポート
- ✅ **CELEngineManager**: CELエンジン管理
- ✅ **CELEvaluator**: CEL式の評価
- ✅ **CELVariableProvider**: 変数提供
  - entity.* 変数
  - target.* 変数
  - world.* 変数
  - nearbyPlayers.* 変数
  - damage.* 変数

### 🔧 改善

- ✅ **MobDefinition**: 型安全なYAML定義
- ✅ **PacketEntity**: 軽量なパケットベースエンティティ
- ✅ **Folia対応**: MCコルーチンによる非同期処理

### 📚 ドキュメント

- ✅ **CEL_QUICK_START.md**: CEL式のクイックスタート
- ✅ **CEL_EXTENSIONS_GUIDE.md**: CEL拡張ガイド
- ✅ **COMMIT_GUIDE.md**: コミットガイド
- ✅ サンプルファイル多数

---

## 初期リリース (2025-10-01)

### 🎉 初期機能

#### 基本システム
- ✅ カスタムMob定義システム
- ✅ スキルトリガーシステム
  - onSpawn
  - onTimer
  - onAttack
  - onDamaged
  - onDeath
- ✅ PacketEventsによるパケット制御
- ✅ Hopliteによる設定管理
- ✅ Paper 1.21.8対応

#### AI・見た目
- ✅ AI設定（移動速度、追跡範囲など）
- ✅ 見た目設定（発光、名前表示など）
- ✅ 装備設定

### 📚 ドキュメント

- ✅ 基本的なREADME
- ✅ 簡易チュートリアル

---

## 🚀 今後の予定

### Phase 3: パフォーマンス最適化（予定）

#### 計画中の機能
- 🔜 **ProjectileSkillのパーティクル描画最適化**
  - バッチ処理による負荷軽減
  - 描画距離に応じた密度調整

- 🔜 **Targeterのキャッシング機能**
  - 同一フレーム内での結果キャッシュ
  - 不要な検索の削減

- 🔜 **高度な脅威度管理システム**
  - 自動脅威度減衰
  - タンク/ヒーラー/DPSの役割認識
  - PacketEntity対応

- 🔜 **範囲処理の最適化**
  - 空間分割による検索高速化
  - 不要なチェックの削減

#### 検討中の機能
- 🤔 **新しいスキルタイプ**
  - ShieldSkill: 防御バリア
  - TrapSkill: 罠設置
  - BuffSkill: バフ付与専用

- 🤔 **新しいエフェクト**
  - ChainLightningEffect: 連鎖雷
  - SlowfallEffect: 落下速度減少
  - InvisibilityEffect: 透明化

- 🤔 **新しいターゲッター**
  - HealthRangeTargeter: 体力範囲指定
  - TypeTargeter: エンティティタイプフィルター
  - TagTargeter: タグベースターゲット

- 🤔 **GUI設定エディター**
  - ゲーム内でのMob編集
  - ビジュアルスキルエディター

---

## 📊 統計情報

### Phase 2時点での実装状況

| カテゴリ | 数 | 説明 |
|---------|-----|------|
| スキルタイプ | 6 | Basic, Projectile, Meta, Branch, Beam, Aura |
| エフェクトタイプ | 17 | Damage, Heal, Potion, Particle, Sound, Message, Command, Velocity, Lightning, Explosion, Freeze, Shield, Teleport, Pull, Push, Blind, SetBlock |
| ターゲッタータイプ | 12 | Self, NearestPlayer, RadiusPlayers, RadiusEntities, LowestHealth, Random, Area, Chain, Nearest, Farthest, Threat, その他 |
| CEL変数グループ | 5 | entity, target, world, nearbyPlayers, damage |
| サンプルファイル | 10+ | 各種サンプルMob定義 |
| ドキュメントファイル | 15+ | ガイド、リファレンス、チュートリアル |

---

## 🔗 リンク

- **GitHub**: [リポジトリ](https://github.com/azuazu3939/unique)
- **Issues**: [バグ報告・要望](https://github.com/azuazu3939/unique/issues)
- **Wiki**: [コミュニティWiki](https://github.com/azuazu3939/unique/wiki)

---

## 📝 変更履歴のフォーマット

このファイルは以下のフォーマットに従います：

- 🎉 **新機能**: 新しく追加された機能
- 🔧 **改善**: 既存機能の改善・強化
- 🐛 **バグ修正**: 不具合の修正
- 📚 **ドキュメント**: ドキュメントの追加・更新
- ⚠️ **非互換性**: 破壊的変更
- 🔜 **計画中**: 実装予定の機能
- 🤔 **検討中**: 検討中の機能
- ✅ **完了**: 実装済み
- ❌ **中止**: 実装を中止

---

**最終更新**: 2025-10-06
