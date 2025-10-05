# ドロップシステム (CEL最新版)

## 概要

カスタムMobからアイテムをドロップするシステムの実装ドキュメントです。**CELベースの最新方針**に従い、柔軟な設定形式と動的なドロップ制御をサポートしています。

---

## DropDefinition 構造

```kotlin
data class DropDefinition(
    val item: String,              // ドロップアイテム（Material名）
    val amount: String = "1",      // ドロップ個数（"1" or "1-3" or CEL式）
    val chance: Double = 1.0,      // ドロップ確率（0.0-1.0 or CEL式）
    val condition: String = "true" // ドロップ条件（CEL式）
)
```

### メソッド

- **`getAmount(): Int`** - ドロップ個数を取得（範囲指定やCEL式を評価）

---

## サポートされているYAML形式

### 1. リスト形式（シンプル）

最もシンプルな形式。アイテム名のみを指定します。

```yaml
Drops:
  items:
    - "DIAMOND"
    - "GOLD_INGOT"
    - "EMERALD"
```

**結果:**
- 各アイテムが100%の確率で1個ずつドロップ

---

### 2. リスト形式（詳細 + CEL式対応）

個数、確率、条件を細かく制御できる形式です。**CEL式による動的計算に対応**。

```yaml
Drops:
  items:
    # 固定値
    - item: "DIAMOND"
      amount: "1-3"
      chance: 0.5
      condition: "killer.level >= 10"

    # CEL式でドロップ個数を計算
    - item: "GOLD_INGOT"
      amount: "math.min(nearbyPlayers.count, 10)"  # プレイヤー数に応じて
      chance: 0.8
      condition: "world.isNight"

    # CEL式でドロップ確率を計算
    - item: "NETHERITE_SCRAP"
      amount: "1"
      chance: "0.1 + (killer.level * 0.01)"  # レベルで確率UP
      condition: "nearbyPlayers.count > 3"

    # CEL式による複雑な条件
    - item: "ENDER_PEARL"
      amount: "random.int(1, 3)"
      chance: 0.6
      condition: "world.isNight && environment.moonPhase == 0"  # 満月の夜
```

**フィールド説明:**
- `item`: ドロップアイテム（必須）
- `amount`: 個数、範囲("1-5")、またはCEL式
- `chance`: 確率（0.0-1.0）またはCEL式
- `condition`: CEL条件式（デフォルト: "true"）

---

### 3. セクション形式

名前付きドロップを定義する形式です。

```yaml
Drops:
  rare_drop:
    item: "DIAMOND"
    amount: "math.ceil(killer.level / 10)"  # レベル10ごとに1個
    chance: 0.3
    condition: "killer.level >= 15"

  common_drop:
    item: "IRON_INGOT"
    amount: "5"
    chance: 1.0

  scaling_drop:
    item: "EMERALD"
    amount: "random.int(1, nearbyPlayers.maxLevel / 5)"
    chance: "math.min(0.9, nearbyPlayers.count * 0.1)"
    condition: "nearbyPlayers.count > 0"

  night_bonus:
    item: "ENDER_PEARL"
    amount: "1-2"
    chance: 0.6
    condition: "world.isNight && world.difficulty == 'HARD'"
```

---

### 4. シンプルな直接指定

キーと値のペアで簡潔に指定する形式です。

```yaml
Drops:
  DIAMOND: "1-3"        # item: DIAMOND, amount: "1-3"
  GOLD_INGOT: 0.5       # item: GOLD_INGOT, chance: 0.5
  EMERALD: 1            # item: EMERALD (デフォルト設定)
```

---

## CEL式の活用例

### 最新のCEL変数・関数を使用

#### Math関数
```yaml
# プレイヤー数に応じた個数（最大10個）
amount: "math.min(nearbyPlayers.count, 10)"

# レベルに応じた確率（最大50%）
chance: "math.min(0.5, killer.level * 0.01)"

# 三角関数を使った複雑な計算
amount: "math.floor(math.sin(environment.tickOfDay * 0.01) * 5 + 5)"
```

#### Random関数
```yaml
# 1-5個のランダムドロップ
amount: "random.int(1, 5)"

# 30%の確率判定
condition: "random.chance(0.3)"

# ランダムboolean（50/50）
condition: "random.boolean() && killer.level > 10"
```

#### Distance関数
```yaml
# 近い場所でのみドロップ
condition: "distance.horizontal(killer.location, entity.location) < 10.0"

# 距離に応じた個数
amount: "math.max(1, math.floor(10 - distance.between(killer.location, entity.location)))"
```

#### String関数
```yaml
# バイオームで条件分岐
condition: "string.contains(environment.biome, 'FOREST')"

# ワールド名チェック
condition: "string.startsWith(world.name, 'world_nether')"
```

#### Environment変数
```yaml
# 満月の夜のみドロップ
condition: "environment.moonPhase == 0 && world.isNight"

# 昼間は少なく、夜は多く
amount: "world.isDay ? 1 : 3"

# 時刻による変動
chance: "math.abs(math.sin(environment.tickOfDay * 0.0001)) * 0.5 + 0.3"
```

#### NearbyPlayers統計
```yaml
# プレイヤー数でスケール
amount: "nearbyPlayers.count"

# 平均レベルで確率UP
chance: "0.1 + (nearbyPlayers.avgLevel * 0.01)"

# 最大レベルが高ければレアドロップ
condition: "nearbyPlayers.maxLevel >= 30"

# 最小レベルが低ければ初心者用アイテム
condition: "nearbyPlayers.minLevel < 10"
```

---

## 実践的な例

### 動的スケーリングMob

```yaml
adaptive_boss:
  Type: "WITHER_SKELETON"
  Health: 500
  DisplayName: "&4適応型ボス"

  Drops:
    # プレイヤー数に応じたドロップ
    scaling_reward:
      item: "DIAMOND"
      amount: "math.ceil(nearbyPlayers.count / 2)"
      chance: 1.0

    # レベルに応じた個数増加
    level_bonus:
      item: "GOLD_INGOT"
      amount: "random.int(1, math.ceil(nearbyPlayers.avgLevel / 5))"
      chance: 0.8

    # 難易度が高いほどレアドロップ
    difficulty_rare:
      item: "NETHERITE_SCRAP"
      amount: "1"
      chance: "world.difficulty == 'HARD' ? 0.3 : 0.1"
      condition: "nearbyPlayers.maxLevel >= 20"

    # 環境ボーナス
    environment_bonus:
      item: "EMERALD"
      amount: "random.int(1, 3)"
      chance: 0.5
      condition: "world.isThundering || environment.moonPhase == 0"
```

### 時間帯変動ドロップ

```yaml
night_creature:
  Type: "PHANTOM"
  Health: 80
  DisplayName: "&9夜の生物"

  Drops:
    # 夜間専用アイテム
    night_essence:
      item: "ENDER_PEARL"
      amount: "world.isNight ? random.int(2, 4) : 1"
      chance: "world.isNight ? 0.8 : 0.2"

    # 月相ボーナス
    moon_crystal:
      item: "DIAMOND"
      amount: "math.ceil((8 - environment.moonPhase) / 4)"  # 新月に近いほど多い
      chance: 0.4
      condition: "world.isNight"

    # 時刻で変動
    time_reward:
      item: "GOLD_INGOT"
      amount: "math.floor(math.sin(environment.tickOfDay * 0.0003) * 3 + 3)"
      chance: 0.6
```

### 距離・位置依存ドロップ

```yaml
territorial_mob:
  Type: "ZOMBIE"
  Health: 60
  DisplayName: "&2縄張りMob"

  Drops:
    # 近距離ボーナス
    close_range_bonus:
      item: "IRON_INGOT"
      amount: "math.max(1, math.floor(10 - distance.horizontal(killer.location, entity.location)))"
      chance: 1.0
      condition: "distance.horizontal(killer.location, entity.location) < 10"

    # バイオーム限定
    biome_special:
      item: "EMERALD"
      amount: "1-2"
      chance: 0.7
      condition: "string.contains(environment.biome, 'JUNGLE')"

    # Y座標でレア度変化
    depth_treasure:
      item: "DIAMOND"
      amount: "1"
      chance: "math.max(0.1, (64 - entity.location.y) / 64)"
      condition: "entity.location.y < 40"
```

---

## 利用可能なCEL変数（最新版）

### 基本変数

#### killer（Player）
- `name`, `uuid`, `level`, `exp`
- `health`, `maxHealth`
- `gameMode`, `isFlying`, `isSneaking`, `isSprinting`
- `location.x/y/z/world/yaw/pitch`

#### entity（Mob）
- `type`, `uuid`, `health`, `maxHealth`
- `age`, `isDead`
- `location.x/y/z/world`

#### world
- `name`, `time`, `fullTime`
- `isDay`, `isNight`
- `hasStorm`, `isThundering`
- `difficulty`, `playerCount`

### 拡張変数（CEL最新版）

#### nearbyPlayers（統計）
- `count`: プレイヤー数
- `avgLevel`: 平均レベル
- `maxLevel`: 最大レベル
- `minLevel`: 最小レベル

#### environment（環境情報）
- `moonPhase`: 月相（0-7）
- `biome`: バイオーム名
- `tickOfDay`: 1日のtick数（0-24000）

### CEL関数

#### math.*
```
基本: abs, max, min, floor, ceil, round
演算: sqrt, pow
三角関数: sin, cos, tan, asin, acos, atan, atan2
変換: toRadians, toDegrees
定数: PI, E
```

#### random.*
```
range(min, max): double型ランダム
int(min, max): int型ランダム
chance(probability): 確率判定
boolean(): ランダムboolean
```

#### distance.*
```
between(pos1, pos2): 3D距離
horizontal(pos1, pos2): 水平距離
squared(pos1, pos2): 距離の2乗
```

#### string.*
```
contains(str, substr): 部分文字列チェック
startsWith(str, prefix): 前方一致
endsWith(str, suffix): 後方一致
toLowerCase(str): 小文字変換
toUpperCase(str): 大文字変換
length(str): 文字列長
```

---

## ドロップ処理フロー

1. **Mob死亡時**
   - ドロップ定義リストを取得
   - コンテキスト変数を構築

2. **各ドロップについて**
   - CEL条件式を評価（falseならスキップ）
   - CEL式でドロップ確率を計算（必要に応じて）
   - ランダム判定で確率チェック
   - CEL式でドロップ個数を計算
   - アイテムをドロップ

3. **コンテキスト変数**
   - `killer`: キルしたプレイヤー
   - `entity`: 死亡したMob
   - `world`: ワールド
   - `nearbyPlayers`: 近くのプレイヤー統計
   - `environment`: 環境情報
   - `math`, `random`, `distance`, `string`: 関数

---

## トラブルシューティング

### CEL式のエラー

```yaml
# ❌ 構文エラー
amount: "math.min(nearbyPlayers.count 5)"  # カンマ忘れ

# ✅ 正しい
amount: "math.min(nearbyPlayers.count, 5)"
```

### 変数のタイプミス

```yaml
# ❌ 存在しない変数
condition: "nearbyPlayer.count > 3"  # Playersが正しい

# ✅ 正しい
condition: "nearbyPlayers.count > 3"
```

### デバッグ方法

```yaml
# テスト用に確率とCEL式を簡略化
test_drop:
  item: "DIAMOND"
  amount: "1"           # 固定値でテスト
  chance: 1.0           # 100%でテスト
  condition: "true"     # 常にtrueでテスト
```

---

## まとめ

- **4つの柔軟なYAML形式**をサポート
- **CEL式による動的計算** - 個数・確率・条件すべてに対応
- **最新CEL変数** - math, random, distance, string, environment, nearbyPlayers
- **プレイヤー・環境・時間に応じた動的ドロップ**
- **Javaコード不要** - YAMLとCELだけで高度な制御が可能

---

CELベースの設計思想により、柔軟で保守性の高いドロップシステムを実現しています！
