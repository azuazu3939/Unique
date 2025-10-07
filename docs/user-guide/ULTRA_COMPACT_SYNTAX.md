# 超コンパクト構文ガイド

Uniqueプラグインの最もコンパクトな記法です。1行でスキル定義、ターゲッター、トリガーをすべて記述できます。

## 基本構文

```
スキル定義 @ターゲッター ~トリガー
```

### 例

```yaml
MyMob:
  type: ZOMBIE
  Skills:
    - projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=22}]} @NearestPlayer{r=30.0} ~onTimer:30t
    - FireballAttack @TL ~onDamaged
    - damage{amount=15} @Self ~onAttack
```

---

## 構文要素

### 1. スキル定義

**インライン構文**または**スキル名参照**が使えます。

#### インライン構文
```yaml
projectile{type=FIREBALL;speed=2.5;maxRange=40;onHit=[damage{amount=22}]}
```

#### スキル名参照
```yaml
FireballAttack
```

### 2. ターゲッター（@記号）

ターゲットを指定します。`@TargetType{param=value}` の形式です。

#### 基本形
```yaml
@NearestPlayer{r=30.0}
@Self
@TargetLocation
```

#### 省略形

| 省略形 | 正式名 |
|--------|--------|
| `@TL` | @TargetLocation |
| `@NP` | @NearestPlayer |
| `@RP` | @RadiusPlayers |
| `@RND` | @Random |
| `@LH` | @LowestHealth |
| `@HH` | @HighestHealth |
| `@AREA` | @Area |
| `@CHAIN` | @Chain |

#### パラメータ

| パラメータ | 省略形 | 説明 |
|-----------|--------|------|
| `range` | `r` | 範囲 |
| `count` | `c` | ターゲット数 |
| `condition` | `cond` | 条件 |
| `maxDistance` | `maxDist` | 最大距離 |

**例:**
```yaml
@NP{r=30.0}                           # NearestPlayer、範囲30
@RP{r=20.0;c=5}                       # RadiusPlayers、範囲20、最大5体
@NP{r=40.0;cond=target.health < 50}   # 条件付き
```

### 3. トリガー（~記号）

スキル発動タイミングを指定します。

| トリガー | 説明 |
|---------|------|
| `~onTimer:interval` | 一定間隔で発動（intervalは時間） |
| `~onDamaged` | ダメージを受けた時 |
| `~onDeath` | 死亡時 |
| `~onSpawn` | スポーン時 |
| `~onAttack` | 攻撃時 |

**例:**
```yaml
~onTimer:30t      # 30tick毎（1.5秒）
~onTimer:3s       # 3秒毎
~onTimer:500ms    # 500ミリ秒毎
~onDamaged        # ダメージを受けた時
```

---

## 時間単位

`~onTimer` で使える時間単位です。

| 単位 | 説明 | 例 |
|------|------|-----|
| `ns` | ナノ秒 | `100ns` |
| `us` / `μs` | マイクロ秒 | `100us` |
| `ms` | ミリ秒 | `500ms` |
| `s` | 秒 | `3s` |
| `m` | 分 | `5m` |
| `h` | 時間 | `2h` |
| `d` | 日 | `1d` |
| `t` / `tick` | **tick（1t=50ms、推奨）** | `30t` |
| *単位なし* | **tick（デフォルト、MythicMobs互換）** | `30` → 30tick |

---

## 実例

### 基本的な例

```yaml
FireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "150"
  Skills:
    - projectile{type=FIREBALL;speed=2.5;maxRange=40;hr=1.8;hom=0.15;onHit=[damage{amount=22},explosion{amount=12;radius=4.0;kb=1.5;fire=true}]} @NP{r=30.0} ~onTimer:30t
    - explosion{amount=20;radius=6.0;kb=2.5} @Self ~onDamaged
    - heal{amount=10} @Self ~onTimer:5s
```

### 全トリガー使用例

```yaml
AllTriggersMob:
  type: WITHER_SKELETON
  Skills:
    - projectile{type=WITHER_SKULL;speed=2.0;onHit=[damage{amount=25}]} @NP{r=35.0} ~onTimer:60t
    - explosion{amount=20;radius=6.0} @Self ~onDamaged
    - explosion{amount=50;radius=10.0;fire=true} @Self ~onDeath
    - particle{type=SOUL;amount=200} @Self ~onSpawn
    - damage{amount=15} @TL ~onAttack
```

### 改行を使った見やすい書き方

YAMLの複数行構文（`>`）を使うことで、ネストを表現できます。

```yaml
ReadableMob:
  type: ZOMBIE
  display: "&a読みやすいコンパクト構文"
  Skills:
    - >
      projectile{
        type=FIREBALL;
        speed=2.5;
        maxRange=40;
        hr=1.8;
        hom=0.15;
        onHit=[
          damage{amount=22},
          explosion{amount=12;radius=4.0;kb=1.5;fire=true}
        ];
        onTick=[
          particle{type=FLAME;amount=10}
        ]
      }
      @NearestPlayer{r=30.0}
      ~onTimer:30t
```

### 複雑な例（全パラメータ活用）

```yaml
DragonBoss:
  type: ENDER_DRAGON
  display: "&d&lドラゴンボス"
  health: "500"
  Skills:
    - projectile{type=DRAGON_FIREBALL;speed=1.5;gravity=false;pierce=true;maxRange=60;hr=2.5;hom=0.3;onHit=[damage{amount=40},explosion{amount=30;radius=8.0;kb=3.0;fire=true}];onTick=[particle{type=DRAGON_BREATH;amount=15}]} @NP{r=50.0} ~onTimer:80t
    - damage{amount=25} @AREA{shape=CIRCLE;radius=15.0} ~onTimer:100t
    - lightning{damage=35;fire=true} @NP{r=40.0;cond=target.health < target.maxHealth * 0.3} ~onTimer:120t
    - heal{amount=50} @Self ~onDamaged
```

---

## 構文の利点

### 超コンパクト構文（4行）

```yaml
FireMage:
  type: BLAZE
  Skills:
    - projectile{type=FIREBALL;speed=2.5;maxRange=40;onHit=[damage{amount=22}]} @NP{r=30.0} ~onTimer:30t
```

**特徴:**
- **簡潔**: 1行でスキル定義完結
- **読みやすい**: 一目で動作を理解
- **MythicMobs互換**: 慣れ親しんだ構文
- **強力**: 複雑なスキルも記述可能

**注意:** Uniqueプラグインは超コンパクト構文のみをサポートしています。従来のYAML形式（OnTimer:, OnDamaged: など）はサポートされていません。

---

## Tips

### 1. 改行で見やすく

```yaml
Skills:
  - >
    projectile{...}
    @NearestPlayer{r=30.0}
    ~onTimer:30t
```

### 2. 省略形を活用

```yaml
# 冗長
@NearestPlayer{range=30.0}

# シンプル
@NP{r=30.0}
```

### 3. 時間単位を統一

```yaml
# tick単位（Minecraft慣れしている人向け）
~onTimer:30t    # 1.5秒
~onTimer:60t    # 3秒

# 秒単位（わかりやすさ重視）
~onTimer:1.5s
~onTimer:3s
```

### 4. コメントで整理

```yaml
Skills:
  # メイン攻撃（30tick毎）
  - projectile{...} @NP{r=30.0} ~onTimer:30t

  # 防御スキル（ダメージ時）
  - explosion{...} @Self ~onDamaged

  # 回復（5秒毎）
  - heal{amount=10} @Self ~onTimer:5s
```

---

## まとめ

超コンパクト構文は、**最小限の記述で最大限の機能**を実現します。

### 利点
- **超簡潔**: 1行でスキル定義が完結
- **読みやすい**: スキルの全体像を一目で把握
- **MythicMobs風**: 既存ユーザーに馴染みやすい
- **強力**: 複雑なスキルも記述可能

### 適用シーン
- **プロトタイピング**: 素早くテストしたい時
- **シンプルなMob**: スキルが少ないMob
- **経験者向け**: MythicMobsやコンパクト構文に慣れている人

---

## クイックリファレンス

```yaml
# 基本構文
スキル定義 @ターゲッター ~トリガー

# 省略形
@TL    = @TargetLocation
@NP    = @NearestPlayer
@RP    = @RadiusPlayers
r=     = range=
c=     = count=
cond=  = condition=

# 時間単位
30t    = 30tick（1.5秒）
3s     = 3秒
500ms  = 500ミリ秒
5m     = 5分

# トリガー
~onTimer:interval   # 間隔発動
~onDamaged          # ダメージ時
~onDeath            # 死亡時
~onSpawn            # スポーン時
~onAttack           # 攻撃時
```
