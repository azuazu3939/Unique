# Unique Plugin ドキュメント

Uniqueは、Minecraftサーバー向けの強力なカスタムMobプラグインです。超コンパクトな構文で、複雑なMobとスキルを簡単に作成できます。

## 📚 ドキュメント構成

このドキュメントは、ユーザー向けガイドと開発者向け技術資料の2つのセクションに分かれています。

### 👤 ユーザーガイド → [user-guide/](user-guide/)

プラグインの使用方法、設定方法、機能リファレンスなど、ユーザー向けの情報。

**はじめに:**
- [クイックスタート](user-guide/QUICKSTART.md) - 5分で始める
- [使い方ガイド](user-guide/GUIDE.md) - 詳細な使い方
- [超コンパクト構文](user-guide/ULTRA_COMPACT_SYNTAX.md) - 完全な構文ガイド

**リファレンス:**
- [機能リファレンス](user-guide/REFERENCE.md) - 全機能の詳細
- [上級機能ガイド](user-guide/ADVANCED_FEATURES.md) - 高度な機能
- [クイックリファレンス（上級）](user-guide/QUICK_REFERENCE_ADVANCED.md) - 上級者向けチートシート
- [リソースキーシステム](user-guide/RESOURCE_KEY_SYSTEM.md) - 最新のリソースキー対応
- [新機能ガイド](user-guide/NEW_FEATURES_GUIDE.md) - 最新機能の紹介

**その他:**
- [トラブルシューティング](user-guide/TROUBLESHOOTING.md) - 問題解決
- [変更履歴](user-guide/CHANGELOG.md) - バージョン履歴
- [開発ガイド](user-guide/DEVELOPMENT.md) - プラグイン開発

### 🛠️ 技術資料 → [technical/](technical/)

プラグインの内部実装、アーキテクチャ、各システムの詳細など、開発者向けの技術情報。

**アーキテクチャ:**
- [アーキテクチャ概要](technical/architecture/overview.md)
- [モジュール構造](technical/architecture/module-structure.md)
- [初期化フロー](technical/architecture/initialization.md)

**コアシステム:**
- [CEL Engine](technical/core-systems/cel-engine.md)
- [Config Manager](technical/core-systems/config-manager.md)
- [PacketEntity Manager](technical/core-systems/packet-entity-manager.md)

**エンティティシステム:**
- [PacketMob](technical/entity-system/packet-mob.md)
- [AI System](technical/entity-system/ai-system.md)
- [Physics System](technical/entity-system/physics.md)
- [Combat System](technical/entity-system/combat.md)

**スキルシステム:**
- [Skill Executor](technical/skill-system/skill-executor.md)
- [Skill Types](technical/skill-system/skill-types.md)

**エフェクトシステム:**
- [Effect Overview](technical/effect-system/effect-overview.md)

**ターゲッターシステム:**
- [Targeter Overview](technical/targeter-system/targeter-overview.md)

**Mobシステム:**
- [Mob Manager](technical/mob-system/mob-manager.md)
- [Spawn System](technical/mob-system/spawn-system.md)

## 🚀 クイックスタート

### インストール

1. プラグインを `plugins/` フォルダーに配置
2. サーバーを起動（自動で設定ファイル生成）
3. `plugins/Unique/mobs/` にMob定義YAMLを作成
4. `/unique reload` でリロード

### 基本的なMob作成

```yaml
SimpleZombie:
  Type: ZOMBIE
  Display: "&c強化ゾンビ"
  Health: "50"
  Damage: "10"
  Skills:
    - "damage{amount=15} @TargetLocation ~onAttack"
```

### 超コンパクト構文

```yaml
FireMage:
  Type: BLAZE
  Display: "&c炎の魔法使い"
  Health: "150"
  Skills:
    - "projectile{damage=22, speed=2.5, particle=FLAME} @NearestPlayer{range=30.0} ~onTimer:30t"
```

## 🎯 主要機能

### 超コンパクト構文
たった1行でMobスキルを定義できます。

### MythicMobs互換
MythicMobsユーザーにとって馴染みやすい設計です。
- **時間単位**: tick（デフォルト）、s、m、h、d
- **省略形**: `@NP` = NearestPlayer、`@TL` = TargetLocation
- **インライン構文**: `{param=value;...}` 形式

### CEL式サポート
Google CEL式による動的な値の計算が可能です。

```yaml
BossMob:
  Health: "100 + (nearbyPlayers.count * 50)"  # プレイヤー数に応じて体力増加
  Damage: "10 + (nearbyPlayers.avgLevel * 0.5)"  # レベルに応じてダメージ増加
```

### 豊富なスキルタイプ
- **Projectile（発射物）**: 矢、火球、ウィザースカルなど
- **Beam（ビーム）**: レーザービーム攻撃
- **Aura（オーラ）**: 範囲バフ・デバフ
- **Summon（召喚）**: エンティティの召喚
- **Basic（基本）**: カスタムエフェクト組み合わせ

### 多彩なエフェクト
ダメージ、爆発、雷、パーティクル、サウンド、ポーション効果、回復、ノックバック、テレポート、引き寄せ、押し出しなど15種類以上

### 自動スポーンシステム
時間、天候、プレイヤー数などの条件に基づいた自動スポーン

### パケットベースの軽量実装
サーバー負荷を最小限に抑えた高性能な実装

## 📖 詳細情報

### 初めての方
1. [クイックスタート](user-guide/QUICKSTART.md)で基本を学ぶ
2. [使い方ガイド](user-guide/GUIDE.md)で詳細を理解する
3. [サンプルMob](../mobs/)を参考にカスタマイズする

### 既存ユーザー
- [新機能ガイド](user-guide/NEW_FEATURES_GUIDE.md) - 最新機能をチェック
- [上級機能](user-guide/ADVANCED_FEATURES.md) - 高度な機能を活用
- [変更履歴](user-guide/CHANGELOG.md) - アップデート内容を確認

### 開発者
- [技術資料](technical/) - 内部実装の詳細
- [アーキテクチャ概要](technical/architecture/overview.md) - システム全体の設計
- [開発ガイド](user-guide/DEVELOPMENT.md) - プラグイン開発の開始方法

## 🎓 学習リソース

### サンプルファイル
プラグインには豊富なサンプルファイルが含まれています：
- `mobs/*.yml` - 様々なMobの例
- `spawns/*.yml` - スポーン定義の例
- `libs/Effects/*.yml` - エフェクトライブラリ
- `sample/*.yml` - 学習用サンプル

### クイックリファレンス

#### ターゲッター省略形
| 省略形 | 正式名 |
|--------|--------|
| `@TL` | TargetLocation |
| `@NP` | NearestPlayer |
| `@RP` | RadiusPlayers |
| `@RND` | Random |
| `@LH` | LowestHealth |
| `@HH` | HighestHealth |

#### 時間単位
| 単位 | 説明 |
|------|------|
| `30` | 30tick（デフォルト、1.5秒） |
| `30t` | 30tick（明示的） |
| `3s` | 3秒 |
| `500ms` | 500ミリ秒 |
| `5m` | 5分 |
| `2h` | 2時間 |

#### トリガー
| トリガー | 説明 |
|---------|------|
| `~onTimer:interval` | 一定間隔で発動 |
| `~onDamaged` | ダメージを受けた時 |
| `~onDeath` | 死亡時 |
| `~onSpawn` | スポーン時 |
| `~onAttack` | 攻撃時 |

## 🆘 サポート

問題が発生した場合：

1. [トラブルシューティング](user-guide/TROUBLESHOOTING.md)を確認
2. ログファイルを確認（`plugins/Unique/logs/`）
3. `/unique debug` でデバッグモードを有効化
4. GitHubでIssueを作成

## 📝 要件

- **Minecraft**: 1.20.6+
- **サーバー**: Paper 1.20.6+ / Folia
- **Java**: 17+

## 🔗 関連リンク

- **GitHub Repository**: [azuazu3939/unique](https://github.com/azuazu3939/unique)
- **Issues**: バグ報告・機能要望
- **Discussions**: 質問・議論

## 📄 ライセンス

このプラグインはGNU General Public License v3.0の下で公開されています。

---

**Unique Plugin** - 超コンパクトなカスタムMobプラグイン
