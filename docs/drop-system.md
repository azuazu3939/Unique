# ドロップシステム

## 概要

カスタムMobからアイテムをドロップするシステムの実装ドキュメントです。柔軟な設定形式とCEL条件式による動的なドロップ制御をサポートしています。

## DropDefinition 構造

```kotlin
data class DropDefinition(
    val item: String,              // ドロップアイテム（Material名）
    val amount: String = "1",      // ドロップ個数（"1" or "1-3" 形式）
    val chance: Double = 1.0,      // ドロップ確率（0.0-1.0）
    val condition: String = "true" // ドロップ条件（CEL式）
)
```

### メソッド

- **`getAmount(): Int`** - ドロップ個数を取得（範囲指定の場合はランダム選択）

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

### 2. リスト形式（詳細）

個数、確率、条件を細かく制御できる形式です。

```yaml
Drops:
  items:
    - item: "DIAMOND"
      amount: "1-3"
      chance: 0.5
      condition: "killer.level >= 10"

    - item: "GOLD_INGOT"
      amount: "5-10"
      chance: 0.8
      condition: "world.isNight"

    - item: "NETHERITE_SCRAP"
      amount: "1"
      chance: 0.1
      condition: "nearbyPlayers.count > 3"
```

**フィールド説明:**
- `item`: ドロップアイテム（必須）
- `amount`: 個数または範囲（"1"、"1-5"など）
- `chance`: ドロップ確率（0.0-1.0、デフォルト: 1.0）
- `condition`: CEL条件式（デフォルト: "true"）

---

### 3. セクション形式

名前付きドロップを定義する形式です。

```yaml
Drops:
  rare_drop:
    item: "DIAMOND"
    amount: "1-3"
    chance: 0.3
    condition: "killer.level >= 15"

  common_drop:
    item: "IRON_INGOT"
    amount: "5"
    chance: 1.0
    condition: "true"

  night_only_drop:
    item: "ENDER_PEARL"
    amount: "1-2"
    chance: 0.6
    condition: "world.isNight && world.difficulty == 'HARD'"
```

**特徴:**
- ドロップに名前を付けられる
- デバッグやログで識別しやすい
- セクション名はアイテム名のフォールバックとして使用される

---

### 4. シンプルな直接指定

キーと値のペアで簡潔に指定する形式です。

```yaml
Drops:
  DIAMOND: "1-3"        # item: DIAMOND, amount: "1-3"
  GOLD_INGOT: 0.5       # item: GOLD_INGOT, chance: 0.5
  EMERALD: 1            # item: EMERALD (デフォルト値)
```

**動作:**
- 文字列値 → amount として解釈
- 数値（小数） → chance として解釈
- 数値（整数） → デフォルト設定

---

## CEL条件式の例

### プレイヤー条件

```yaml
# キラーのレベルが10以上
condition: "killer.level >= 10"

# キラーがサバイバルモード
condition: "killer.gameMode == 'SURVIVAL'"

# キラーの体力が50%以下
condition: "killer.health < killer.maxHealth * 0.5"

# キラーが特定のアイテムを持っている
condition: "killer.heldItemSlot == 0"
```

### 環境条件

```yaml
# 夜間のみ
condition: "world.isNight"

# 昼間のみ
condition: "world.isDay"

# 雷雨中のみ
condition: "world.isThundering"

# 特定の難易度
condition: "world.difficulty == 'HARD'"
```

### Mob条件

```yaml
# Mobの体力が50%以下
condition: "entity.health < entity.maxHealth * 0.5"

# Mobが特定の年齢以上
condition: "entity.age > 6000"

# Mobが死亡状態
condition: "entity.isDead"
```

### 複合条件

```yaml
# 夜間かつハードモード
condition: "world.isNight && world.difficulty == 'HARD'"

# レベル10以上またはサバイバルモード
condition: "killer.level >= 10 || killer.gameMode == 'SURVIVAL'"

# 近くにプレイヤーが3人以上いる場合
condition: "nearbyPlayers.count >= 3"

# 近くのプレイヤーの平均レベルが15以上
condition: "nearbyPlayers.avgLevel >= 15"
```

### Math関数を使用

```yaml
# ランダム確率
condition: "math.random() < 0.3"

# 距離計算
condition: "distance < 10.0"

# 角度計算
condition: "math.abs(angle) < 45.0"
```

---

## 実装例

### 基本的なMob定義

```yaml
my_zombie:
  Type: "ZOMBIE"
  Health: 40
  DisplayName: "&cカスタムゾンビ"

  Drops:
    items:
      - item: "ROTTEN_FLESH"
        amount: "1-3"
        chance: 1.0

      - item: "IRON_INGOT"
        amount: "1"
        chance: 0.3

      - item: "DIAMOND"
        amount: "1"
        chance: 0.05
        condition: "killer.level >= 20"
```

### レベル依存ドロップ

```yaml
boss_mob:
  Type: "WITHER_SKELETON"
  Health: 200
  DisplayName: "&4ボスMob"

  Drops:
    beginner_reward:
      item: "IRON_INGOT"
      amount: "5-10"
      chance: 1.0
      condition: "killer.level < 10"

    intermediate_reward:
      item: "GOLD_INGOT"
      amount: "3-7"
      chance: 1.0
      condition: "killer.level >= 10 && killer.level < 20"

    expert_reward:
      item: "DIAMOND"
      amount: "1-3"
      chance: 1.0
      condition: "killer.level >= 20"

    rare_drop:
      item: "NETHERITE_SCRAP"
      amount: "1"
      chance: 0.1
      condition: "killer.level >= 30"
```

### 環境依存ドロップ

```yaml
night_creature:
  Type: "PHANTOM"
  Health: 60
  DisplayName: "&9夜の生物"

  Drops:
    night_essence:
      item: "ENDER_PEARL"
      amount: "1-2"
      chance: 0.8
      condition: "world.isNight"

    storm_crystal:
      item: "DIAMOND"
      amount: "1"
      chance: 0.5
      condition: "world.isThundering"

    moon_shard:
      item: "EMERALD"
      amount: "1"
      chance: 0.3
      condition: "environment.moonPhase == 0"  # 満月
```

### 複雑な条件の組み合わせ

```yaml
advanced_mob:
  Type: "SKELETON"
  Health: 80
  DisplayName: "&eアドバンスMob"

  Drops:
    items:
      # 基本ドロップ（常時）
      - item: "BONE"
        amount: "2-5"
        chance: 1.0

      # 夜間ボーナス
      - item: "ARROW"
        amount: "5-10"
        chance: 0.8
        condition: "world.isNight"

      # レベル＆難易度ボーナス
      - item: "ENCHANTED_BOOK"
        amount: "1"
        chance: 0.2
        condition: "killer.level >= 15 && world.difficulty == 'HARD'"

      # マルチプレイヤーボーナス
      - item: "DIAMOND"
        amount: "1"
        chance: 0.3
        condition: "nearbyPlayers.count >= 3 && nearbyPlayers.avgLevel >= 10"

      # ランダムレア
      - item: "NETHERITE_INGOT"
        amount: "1"
        chance: 0.05
        condition: "math.random() < 0.5"
```

---

## ドロップ処理フロー

1. **Mob死亡時**
   - ドロップ定義リストを取得

2. **各ドロップについて**
   - CEL条件式を評価（falseの場合スキップ）
   - ドロップ確率をチェック（ランダム判定）
   - 個数を決定（範囲の場合はランダム選択）
   - アイテムをドロップ

3. **コンテキスト変数**
   - `killer`: キルしたプレイヤー情報
   - `entity`: 死亡したMob情報
   - `world`: ワールド情報
   - `nearbyPlayers`: 近くのプレイヤー情報
   - `environment`: 環境情報（月相、時刻など）
   - `math`: Math関数
   - `string`: String関数

---

## 利用可能な変数

### killer（Player）
- `name`: プレイヤー名
- `level`: レベル
- `exp`: 経験値
- `health`: 体力
- `maxHealth`: 最大体力
- `gameMode`: ゲームモード
- `isFlying`: 飛行中か
- `isSneaking`: スニーク中か
- `location.x/y/z`: 座標

### entity（Mob）
- `type`: エンティティタイプ
- `health`: 体力
- `maxHealth`: 最大体力
- `age`: 生存時間
- `isDead`: 死亡状態
- `location.x/y/z`: 座標

### world
- `name`: ワールド名
- `time`: 時刻
- `isDay`: 昼間か
- `isNight`: 夜間か
- `hasStorm`: 雨か
- `isThundering`: 雷雨か
- `difficulty`: 難易度
- `playerCount`: プレイヤー数

### nearbyPlayers
- `count`: プレイヤー数
- `maxLevel`: 最大レベル
- `minLevel`: 最小レベル
- `avgLevel`: 平均レベル

### environment
- `moonPhase`: 月相（0-7）
- `dayOfCycle`: サイクル日数
- `tickOfDay`: 1日のtick

---

## パフォーマンス考慮事項

1. **条件式の最適化**
   - 軽い条件を先に評価
   - 複雑な計算は必要な場合のみ

2. **ドロップ確率**
   - chance = 0.0 のアイテムは削除推奨
   - 条件で制御する方が効率的

3. **大量ドロップの回避**
   - amountは適切な範囲に制限
   - 同時ドロップ数を考慮

---

## トラブルシューティング

### ドロップされない場合

1. **CEL条件式のエラー**
   ```
   条件式が正しいか確認
   DebugLogger でエラーログをチェック
   ```

2. **確率設定の確認**
   ```yaml
   chance: 1.0  # 必ず落ちる
   chance: 0.5  # 50%
   chance: 0.0  # 落ちない
   ```

3. **アイテム名の確認**
   ```yaml
   # 正しい Material 名を使用
   item: "DIAMOND"  # ✓
   item: "diamond"  # ✗（大文字小文字区別あり）
   ```

### デバッグ方法

```yaml
# テスト用に確率を100%に設定
Drops:
  items:
    - item: "DIAMOND"
      amount: "1"
      chance: 1.0
      condition: "true"  # 常にtrue
```

---

## まとめ

- 4つの柔軟な設定形式をサポート
- CEL式による動的な条件制御
- 範囲指定による個数のランダム化
- プレイヤー、Mob、環境の状態に応じたドロップ
- デバッグしやすい構造
