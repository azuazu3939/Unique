# Unique Plugin ドキュメント

Uniqueは、Minecraftサーバー向けの強力なカスタムMobプラグインです。超コンパクトな構文で、複雑なMobとスキルを簡単に作成できます。

## 📚 ドキュメント一覧

### 基本ガイド
- **[クイックスタート](QUICKSTART.md)** - 5分で始める
- **[使い方ガイド](GUIDE.md)** - 詳細な使い方
- **[構文リファレンス](ULTRA_COMPACT_SYNTAX.md)** - 完全な構文ガイド

### リファレンス
- **[機能リファレンス](REFERENCE.md)** - 全機能の詳細
- **[新機能ガイド](NEW_FEATURES_GUIDE.md)** - 最新機能の紹介

### 開発者向け
- **[開発ガイド](DEVELOPMENT.md)** - プラグイン開発
- **[変更履歴](CHANGELOG.md)** - バージョン履歴
- **[トラブルシューティング](TROUBLESHOOTING.md)** - 問題解決

## 🚀 特徴

### 超コンパクト構文
たった1行でMobスキルを定義できます。

```yaml
FireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "150"
  Skills:
    - projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=22},explosion{radius=4.0}]} @NearestPlayer{r=30} ~onTimer:30
```

### MythicMobs互換
MythicMobsユーザーにとって馴染みやすい設計です。

- **時間単位**: tick（デフォルト）、s、m、h、d
- **省略形**: `@NP` = NearestPlayer、`@TL` = TargetLocation
- **インライン構文**: `{param=value;...}` 形式

### CEL式サポート
Google CEL式による動的な値の計算が可能です。

```yaml
BossMob:
  health: "100 + (nearbyPlayers.count * 50)"  # プレイヤー数に応じて体力増加
  damage: "10 + (nearbyPlayers.avgLevel * 0.5)"  # レベルに応じてダメージ増加
```

### 豊富なスキルタイプ
- **Projectile（発射物）**: 矢、火球、ウィザースカルなど
- **Beam（ビーム）**: レーザービーム攻撃
- **Aura（オーラ）**: 範囲バフ・デバフ
- **Basic（基本）**: カスタムエフェクト組み合わせ

### 多彩なエフェクト
ダメージ、爆発、雷、パーティクル、サウンド、ポーション効果、回復、凍結、シールド、テレポート、引き寄せ、押し出し、盲目など

## 📖 基本構文

### Mob定義

```yaml
MobName:
  type: ENTITY_TYPE        # エンティティタイプ（必須）
  display: "&cボス名"      # 表示名
  health: "200"            # 体力（CEL式対応）
  damage: "15"             # 攻撃力（CEL式対応）
  armor: "10"              # 防具値（CEL式対応）

  AI:
    movementSpeed: 0.3
    followRange: 20.0

  Appearance:
    customNameVisible: true
    glowing: true

  Skills:
    - スキル定義 @ターゲッター ~トリガー

  Drops:
    - item: DIAMOND
      amount: "1-3"
      chance: "0.5"
```

### スキル構文

```
スキル定義 @ターゲッター ~トリガー
```

**例:**
```yaml
- projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=20}]} @NP{r=30} ~onTimer:30
```

## 🎯 クイックリファレンス

### ターゲッター省略形

| 省略形 | 正式名 |
|--------|--------|
| `@TL` | TargetLocation |
| `@NP` | NearestPlayer |
| `@RP` | RadiusPlayers |
| `@RND` | Random |
| `@LH` | LowestHealth |
| `@HH` | HighestHealth |

### 時間単位

| 単位 | 説明 |
|------|------|
| `30` | 30tick（デフォルト、1.5秒） |
| `30t` | 30tick（明示的） |
| `3s` | 3秒 |
| `500ms` | 500ミリ秒 |
| `5m` | 5分 |
| `2h` | 2時間 |

### トリガー

| トリガー | 説明 |
|---------|------|
| `~onTimer:interval` | 一定間隔で発動 |
| `~onDamaged` | ダメージを受けた時 |
| `~onDeath` | 死亡時 |
| `~onSpawn` | スポーン時 |
| `~onAttack` | 攻撃時 |

## 📝 サンプル

### シンプルなMob

```yaml
SimpleZombie:
  type: ZOMBIE
  display: "&c強化ゾンビ"
  health: "50"
  Skills:
    - damage{amount=15} @TL ~onAttack
```

### 中級Mob

```yaml
IceMage:
  type: STRAY
  display: "&b氷の魔法使い"
  health: "120"
  Skills:
    - projectile{type=SNOWBALL;speed=3.0;onHit=[damage{amount=12},freeze{duration=3s}]} @NP{r=25} ~onTimer:40
    - explosion{amount=15;radius=5.0} @Self ~onDamaged
    - heal{amount=10} @Self ~onTimer:5s
```

### 上級ボス

```yaml
DragonBoss:
  type: ENDER_DRAGON
  display: "&d&l古龍"
  health: "500 + (nearbyPlayers.count * 100)"
  damage: "25"
  armor: "15"
  Skills:
    - projectile{type=DRAGON_FIREBALL;speed=1.5;pierce=true;onHit=[damage{amount=40},explosion{amount=30;radius=8.0;fire=true}]} @NP{r=50} ~onTimer:60
    - lightning{damage=35;fire=true} @NP{r=40;cond=target.health < target.maxHealth * 0.3} ~onTimer:100
    - aura{radius=15;duration=10000;particle=DRAGON_BREATH} @Self ~onTimer:200
    - explosion{amount=80;radius=15.0;fire=true} @Self ~onDeath
```

## 🔧 セットアップ

1. プラグインを `plugins/` フォルダーに配置
2. サーバーを起動（自動で設定ファイル生成）
3. `plugins/Unique/mobs/` にMob定義YAMLを作成
4. `/unique reload` でリロード

## 💡 Tips

### 改行で見やすく

```yaml
Skills:
  - >
    projectile{
      type=FIREBALL;
      speed=2.5;
      onHit=[
        damage{amount=22},
        explosion{radius=4.0}
      ]
    }
    @NearestPlayer{r=30}
    ~onTimer:30
```

### コメントで整理

```yaml
Skills:
  # メイン攻撃（30tick毎）
  - projectile{...} @NP{r=30} ~onTimer:30

  # 防御スキル（ダメージ時）
  - explosion{...} @Self ~onDamaged
```

## 📦 サンプルファイル

プラグインには豊富なサンプルファイルが含まれています：

- `mobs/*.yml` - 様々なMobの例
- `skills/*.yml` - スキルライブラリ
- `sample/*.yml` - 学習用サンプル

## 🆘 サポート

問題が発生した場合：

1. [トラブルシューティング](TROUBLESHOOTING.md)を確認
2. ログファイルを確認（`plugins/Unique/logs/`）
3. `/unique debug` でデバッグモードを有効化
4. GitHubでIssueを作成

## 📄 ライセンス

このプラグインはGNU General Public License v3.0の下で公開されています。

---

**Unique Plugin** - 超コンパクトなカスタムMobプラグイン
