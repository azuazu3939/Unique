# Unique プラグイン 完全ガイド

超コンパクト構文を使ったカスタムMob作成の完全ガイドです。

## 目次

1. [基本概念](#基本概念)
2. [超コンパクト構文](#超コンパクト構文)
3. [スキルとエフェクト](#スキルとエフェクト)
4. [ターゲッター](#ターゲッター)
5. [トリガー](#トリガー)
6. [CEL式の活用](#cel式の活用)
7. [実践例](#実践例)
8. [ベストプラクティス](#ベストプラクティス)

---

## 基本概念

### Uniqueの構造

```
Mob定義 → スキル定義 @ターゲッター ~トリガー → 実行
```

**例:**
```yaml
MyMob:
  type: ZOMBIE
  Skills:
    - damage{amount=20} @NearestPlayer{r=30} ~onTimer:60
```

### ファイル構成

```
plugins/Unique/
├── config.yml          # プラグイン設定
├── mobs/              # カスタムMob定義
├── skills/            # スキルライブラリ
└── spawns/            # スポーン設定
```

---

## 超コンパクト構文

### 構文の基本

```
スキル定義 @ターゲッター ~トリガー
```

- **スキル定義**: 何をするか（projectile, damage, heal など）
- **ターゲッター**: 誰を対象にするか（@NP, @Self, @TL など）
- **トリガー**: いつ発動するか（~onTimer, ~onDamaged など）

### シンプルな例

```yaml
SimpleZombie:
  type: ZOMBIE
  display: "&c強化ゾンビ"
  health: "50"
  Skills:
    - damage{amount=15} @TargetLocation ~onAttack
```

### 複雑な例

```yaml
FireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "120"
  Skills:
    # ファイヤーボール（3秒毎）
    - projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=22},explosion{amount=15;radius=5.0;fire=true}]} @NearestPlayer{r=30} ~onTimer:60

    # 反撃爆発（ダメージ時）
    - explosion{amount=20;radius=8.0;fire=true} @Self ~onDamaged

    # 回復（10秒毎）
    - heal{amount=15} @Self ~onTimer:200
```

---

## スキルとエフェクト

### スキルタイプ

#### 1. Projectile（発射物）

```yaml
# 基本
- projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=20}]} @NP{r=30} ~onTimer:60

# 高度
- projectile{type=ARROW;speed=4.0;pierce=true;hom=0.3;onHit=[damage{amount=15},potion{type=POISON;duration=5s}]} @NP{r=25} ~onTimer:40
```

**パラメータ:**
- `type`: FIREBALL, ARROW, SNOWBALL, WITHER_SKULL, DRAGON_FIREBALL
- `speed`: 弾速
- `pierce`: 貫通するか
- `hom` / `homing`: ホーミング強度（0.0-1.0）
- `maxRange`: 最大飛距離
- `hr` / `hitRadius`: ヒット判定半径
- `onHit`: ヒット時のエフェクト配列
- `onTick`: Tick毎のエフェクト配列

#### 2. Beam（ビーム）

```yaml
- beam{range=25;width=0.8;particle=FLAME;duration=2000;piercing=true} @NP{r=30} ~onTimer:100
```

#### 3. Aura（オーラ）

```yaml
- aura{radius=10;duration=15000;tickInterval=1000;particle=ENCHANT;particleCount=30} @Self ~onSpawn
```

### エフェクト

#### ダメージ系
```yaml
- damage{amount=20}
- explosion{amount=15;radius=5.0;kb=2.0;fire=true}
- lightning{damage=30;fire=true}
```

#### 状態異常系
```yaml
- potion{type=POISON;duration=5s;amplifier=2}
- freeze{duration=3s;amplifier=2}
- blind{duration=4s;amplifier=1}
```

#### サポート系
```yaml
- heal{amount=15}
- shield{amount=30;duration=5s}
```

#### 移動系
```yaml
- teleport{mode=RANDOM;range=15}
- pull{strength=1.5;radius=10}
- push{strength=2.0;radius=8}
```

#### 視覚効果
```yaml
- particle{type=FLAME;amount=100}
- sound{type=ENTITY_GENERIC_EXPLODE;volume=1.5;pitch=0.8}
```

---

## ターゲッター

### 基本ターゲッター

```yaml
@Self                          # 自分自身
@TargetLocation               # 攻撃対象の位置
@NearestPlayer{r=30}          # 最も近いプレイヤー
@RadiusPlayers{r=15;c=5}      # 範囲内のプレイヤー最大5人
```

### 省略形

| 省略形 | 正式名 |
|--------|--------|
| `@TL` | TargetLocation |
| `@NP` | NearestPlayer |
| `@RP` | RadiusPlayers |
| `@RND` | Random |
| `@LH` | LowestHealth |
| `@HH` | HighestHealth |

### パラメータ

```yaml
@NP{r=30}                      # 範囲30
@RP{r=20;c=5}                  # 範囲20、最大5人
@NP{r=40;cond=target.health < 50}  # 条件付き
```

| パラメータ | 省略形 | 説明 |
|-----------|--------|------|
| `range` | `r` | 範囲 |
| `count` | `c` | ターゲット数 |
| `condition` | `cond` | CEL式条件 |

---

## トリガー

### トリガータイプ

```yaml
~onTimer:60        # 60tick毎（3秒）
~onDamaged         # ダメージを受けた時
~onDeath           # 死亡時
~onSpawn           # スポーン時
~onAttack          # 攻撃時
```

### 時間単位

| 記法 | 意味 |
|------|------|
| `30` | 30tick（1.5秒）- デフォルト |
| `30t` | 30tick（明示的） |
| `3s` | 3秒 |
| `500ms` | 500ミリ秒 |
| `5m` | 5分 |
| `2h` | 2時間 |
| `1d` | 1日 |

**重要:** 単位なしの数値はtickとして扱われます（MythicMobs互換）

---

## CEL式の活用

### 基本的な使い方

```yaml
# プレイヤー数に応じて体力増加
MyMob:
  health: "100 + (nearbyPlayers.count * 50)"

# レベルに応じてダメージ増加
  damage: "15 + (nearbyPlayers.avgLevel * 0.5)"

# 条件分岐
  armor: "nearbyPlayers.count > 3 ? 15 : 10"
```

### 利用可能な変数

#### entity（Mob自身）
```yaml
entity.health          # 現在の体力
entity.maxHealth       # 最大体力
entity.level           # レベル
```

#### nearbyPlayers
```yaml
nearbyPlayers.count        # プレイヤー数
nearbyPlayers.avgLevel     # 平均レベル
nearbyPlayers.maxLevel     # 最大レベル
nearbyPlayers.minLevel     # 最小レベル
```

#### world
```yaml
world.time             # ワールド時間
world.difficulty       # 難易度
```

#### environment
```yaml
environment.moonPhase  # 月の満ち欠け（0-7）
environment.isRaining  # 雨が降っているか
```

### 実例

```yaml
# 夜間強化Mob
NightHunter:
  type: ZOMBIE
  display: "&0夜の狩人"
  health: "80 + (world.time > 13000 ? 40 : 0)"
  damage: "world.time > 13000 ? 25 : 15"
  Skills:
    - damage{amount=30} @NP{r=25} ~onAttack
    - heal{amount=20} @Self ~onTimer:100

# 満月強化Mob
LunarWolf:
  type: WOLF
  display: "&e月光の狼"
  health: "environment.moonPhase == 0 ? 150 : 100"
  damage: "environment.moonPhase == 0 ? 30 : 20"
  Skills:
    - damage{amount=35} @TL ~onAttack
    - howl{radius=15} @Self ~onTimer:200

# プレイヤー数適応ボス
ScalingBoss:
  type: WITHER_SKELETON
  display: "&5適応型ボス"
  health: "200 + (nearbyPlayers.count * 100)"
  damage: "20 + (nearbyPlayers.avgLevel * 0.8)"
  armor: "nearbyPlayers.count > 5 ? 20 : 15"
  Skills:
    - projectile{type=WITHER_SKULL;speed=2.0;onHit=[damage{amount=40}]} @NP{r=40} ~onTimer:60
    - explosion{amount=30;radius=10.0} @Self ~onTimer:100
```

---

## 実践例

### 初級: 強化ゾンビ

```yaml
StrongZombie:
  type: ZOMBIE
  display: "&c強化ゾンビ"
  health: "60"
  damage: "15"
  AI:
    movementSpeed: 0.28
    followRange: 20.0
  Skills:
    - damage{amount=25} @TL ~onAttack
    - push{strength=1.5} @TL ~onDamaged
```

### 中級: 炎の魔法使い

```yaml
FireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "120"
  Skills:
    - projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=22},explosion{amount=15;radius=5.0;fire=true}]} @NP{r=30} ~onTimer:60
    - explosion{amount=20;radius=8.0;fire=true} @Self ~onDamaged
    - heal{amount=15} @Self ~onTimer:200
```

### 上級: ドラゴンボス

```yaml
DragonBoss:
  type: ENDER_DRAGON
  display: "&d&l古龍"
  health: "800 + (nearbyPlayers.count * 100)"
  damage: "40"
  armor: "18"
  AI:
    movementSpeed: 0.28
    followRange: 60.0
    knockbackResistance: 1.0
  Appearance:
    glowing: true
  Skills:
    # ドラゴンブレス
    - projectile{type=DRAGON_FIREBALL;speed=1.8;pierce=true;onHit=[damage{amount=45},explosion{amount=35;radius=8.0;fire=true}]} @NP{r=55} ~onTimer:40

    # 範囲爆発
    - explosion{amount=40;radius=15.0;kb=3.5;fire=true} @Self ~onTimer:80

    # 雷撃（低体力時）
    - lightning{damage=50;fire=true} @RP{r=35;c=6} ~onTimer:100

    # オーラ
    - aura{radius=20;duration=120000;tickInterval=1000;particle=DRAGON_BREATH;particleCount=60} @Self ~onSpawn

    # 回復
    - heal{amount=60} @Self ~onTimer:160

    # 死亡時爆発
    - explosion{amount=100;radius=25.0;kb=5.0;fire=true} @Self ~onDeath
    - lightning{damage=80;fire=true} @RP{r=30} ~onDeath

  Drops:
    - item: DRAGON_HEAD
      amount: "1"
      chance: "1.0"
    - item: NETHERITE_INGOT
      amount: "5-15"
      chance: "1.0"
```

---

## ベストプラクティス

### 1. 改行で見やすく

```yaml
Skills:
  - >
    projectile{
      type=FIREBALL;
      speed=2.5;
      onHit=[
        damage{amount=22},
        explosion{radius=5.0;fire=true}
      ]
    }
    @NearestPlayer{r=30}
    ~onTimer:60
```

### 2. コメントで整理

```yaml
Skills:
  # メイン攻撃（3秒毎）
  - projectile{...} @NP{r=30} ~onTimer:60

  # 防御（ダメージ時）
  - shield{amount=30;duration=5s} @Self ~onDamaged

  # 回復（10秒毎）
  - heal{amount=20} @Self ~onTimer:200
```

### 3. 省略形を活用

```yaml
# 冗長
@NearestPlayer{range=30}

# 簡潔
@NP{r=30}
```

### 4. 時間単位を統一

プロジェクト全体でtick単位に統一することを推奨：

```yaml
~onTimer:30    # 1.5秒
~onTimer:60    # 3秒
~onTimer:100   # 5秒
~onTimer:200   # 10秒
```

### 5. バランス調整

```yaml
# 弱い雑魚Mob
- damage{amount=10} @TL ~onAttack

# 中級Mob
- damage{amount=20} @TL ~onAttack
- projectile{...} @NP{r=25} ~onTimer:80

# ボス
- damage{amount=40} @TL ~onAttack
- projectile{...} @NP{r=40} ~onTimer:40
- explosion{...} @Self ~onTimer:100
```

### 6. CEL式の活用

動的なMobを作成：

```yaml
DynamicBoss:
  health: "base_hp + (player_count * scaling_factor)"
  damage: "base_damage * difficulty_multiplier"
```

---

## まとめ

超コンパクト構文は：

- **シンプル**: 1行でスキル定義
- **強力**: 複雑な動作も可能
- **読みやすい**: 一目で理解できる
- **MythicMobs互換**: 慣れ親しんだ構文

詳細は以下を参照：
- [構文リファレンス](ULTRA_COMPACT_SYNTAX.md)
- [機能リファレンス](REFERENCE.md)
- [クイックスタート](QUICKSTART.md)

---

**Unique Plugin** - 超コンパクトなカスタムMobプラグイン
