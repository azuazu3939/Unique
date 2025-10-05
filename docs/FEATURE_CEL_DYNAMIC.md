# CEL動的機能ガイド（Phase 1実装内容）

Phase 1で実装されたCEL動的機能の詳細ガイドです。

---

## 🚀 Phase 1実装内容

Phase 1では、以下の3つの主要機能が実装されました：

1. **Effect動的パラメータ** - Damage, Heal, PotionEffectがCEL式に対応
2. **MobDefinition動的ステータス** - Health, DamageがCEL式に対応
3. **Drop完全CEL化** - amount, chanceがCEL式に対応

これにより、プレイヤー数、レベル、時間帯、環境など、様々な要因に応じてMobの挙動を動的に変更できるようになりました。

---

## 💥 1. Effect動的パラメータ

### 1.1 DamageEffect - 動的ダメージ

**対応パラメータ**: `amount`

#### 基本例（固定値）

```yaml
FixedDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "20"  # 固定20ダメージ
```

#### ターゲットのHPに比例

```yaml
PercentDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "target.maxHealth * 0.3"  # 最大HPの30%
```

#### 現在HPに比例

```yaml
CurrentHPDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "target.health * 0.5"  # 現在HPの50%
```

#### 距離減衰ダメージ

```yaml
DistanceDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "30 * math.max(0, 1 - distance.horizontal(source.location, target.location) / 20.0)"
      # 20ブロックで0、0ブロックで30
```

#### プレイヤー数でスケール

```yaml
ScalingDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "10 + (nearbyPlayers.count * 5)"  # 1人につき+5ダメージ
```

---

### 1.2 HealEffect - 動的回復

**対応パラメータ**: `amount`

#### 失ったHP分を回復

```yaml
FullRestore:
  type: Heal
  effects:
    - type: Heal
      amount: "entity.maxHealth - entity.health"
```

#### 最大HPの割合で回復

```yaml
PercentHeal:
  type: Heal
  effects:
    - type: Heal
      amount: "entity.maxHealth * 0.5"  # 最大HPの50%回復
```

#### HP比例回復（低いほど多く回復）

```yaml
AdaptiveHeal:
  type: Heal
  effects:
    - type: Heal
      amount: "(entity.maxHealth - entity.health) * 0.8"  # 失ったHPの80%
```

---

### 1.3 PotionEffectEffect - 動的ポーション効果

**対応パラメータ**: `duration`, `amplifier`

#### HP比例のデバフ

```yaml
HPBasedDebuff:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: WEAKNESS
      duration: "200"  # 固定10秒
      amplifier: "math.floor((1 - entity.health / entity.maxHealth) * 3)"
      # HP低いほど強いデバフ（0-3）
```

#### 時間帯で変化

```yaml
TimeBasedBuff:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: STRENGTH
      duration: "world.isNight ? 200 : 0"  # 夜のみ10秒
      amplifier: "world.isNight ? 2 : 0"  # 夜のみレベル3
```

#### プレイヤー数でデバフ強化

```yaml
ScalingDebuff:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: SLOWNESS
      duration: "100 + (nearbyPlayers.count * 20)"  # 1人につき+1秒
      amplifier: "math.min(3, nearbyPlayers.count - 1)"  # 最大レベル4
```

#### 距離で効果時間変化

```yaml
DistanceBasedDebuff:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: POISON
      duration: "math.ceil(200 * (1 - distance.horizontal(source.location, target.location) / 30.0))"
      # 近いほど長い（0-10秒）
      amplifier: "2"
```

---

## 🧟 2. MobDefinition動的ステータス

### 2.1 Health - 動的HP

**対応パラメータ**: `Health`

#### プレイヤー数でスケール

```yaml
PlayerScalingBoss:
  Type: WITHER_SKELETON
  Display: '&4&lScaling Boss'
  Health: "100 + (nearbyPlayers.count * 50)"  # 1人につき+50HP
  Damage: 10
```

**結果**:
- プレイヤー1人: HP 150
- プレイヤー3人: HP 250
- プレイヤー5人: HP 350

#### 平均レベルでスケール

```yaml
LevelScalingBoss:
  Type: IRON_GOLEM
  Display: '&fLevel Scaling Golem'
  Health: "50 + (nearbyPlayers.avgLevel * 10)"  # レベル1につき+10HP
  Damage: 5
```

**結果**:
- 平均レベル10: HP 150
- 平均レベル20: HP 250
- 平均レベル30: HP 350

#### 時間帯で変化

```yaml
TimeAdaptiveMob:
  Type: ZOMBIE
  Display: '&8&lNight Terror'
  Health: "world.isNight ? 150.0 : 80.0"  # 夜間強化
  Damage: 10
```

#### ランダムHP

```yaml
RandomMob:
  Type: CREEPER
  Display: '&aRandom Creeper'
  Health: "50 + random.range(0, 100)"  # 50-150
  Damage: 5
```

---

### 2.2 Damage - 動的ダメージ

**対応パラメータ**: `Damage`

#### プレイヤーレベルで変化

```yaml
LevelAdaptiveMob:
  Type: SKELETON
  Display: '&7Level Adaptive Skeleton'
  Health: 100
  Damage: "5 + (nearbyPlayers.avgLevel * 0.5)"  # レベルでダメージ増加
```

#### 天候で変化

```yaml
WeatherMob:
  Type: WITCH
  Display: '&5Weather Witch'
  Health: 100
  Damage: "world.hasStorm ? 15.0 : 8.0"  # 雨天時強化
```

#### プレイヤー数と時間帯の組み合わせ

```yaml
ComplexBoss:
  Type: WITHER_SKELETON
  Display: '&4&lComplex Boss'
  Health: "100 + (nearbyPlayers.count * 50)"
  Damage: "(world.isNight ? 15.0 : 10.0) + (nearbyPlayers.avgLevel * 0.3)"
  # 夜間+レベルで増加
```

---

## 💎 3. Drop完全CEL化

### 3.1 amount - 動的ドロップ数

**対応パラメータ**: `amount`

#### プレイヤーレベルでスケール

```yaml
drops:
  - item: DIAMOND
    amount: "math.max(1, nearbyPlayers.maxLevel / 10)"  # レベル10につき1個
    chance: "1.0"
```

#### プレイヤー数で増加

```yaml
drops:
  - item: GOLD_INGOT
    amount: "math.ceil(nearbyPlayers.count / 2)"  # 2人につき1個
    chance: "1.0"
```

#### HP比例（低いほど多くドロップ）

```yaml
drops:
  - item: DIAMOND
    amount: "math.ceil((1 - entity.health / entity.maxHealth) * 10)"
    # 死亡時のHPで0-10個
    chance: "1.0"
```

#### 範囲形式との互換性

```yaml
drops:
  - item: EMERALD
    amount: "1-3"  # 従来の範囲形式も引き続きサポート
    chance: "1.0"
```

---

### 3.2 chance - 動的ドロップ確率

**対応パラメータ**: `chance`

#### プレイヤー数で確率上昇

```yaml
drops:
  - item: NETHER_STAR
    amount: "1"
    chance: "0.1 + (nearbyPlayers.count * 0.05)"  # 1人につき+5%
```

#### 満月でレアドロップ

```yaml
drops:
  - item: NETHER_STAR
    amount: "1"
    chance: "environment.moonPhase == 0 ? 0.5 : 0.05"
    # 満月50%, 他5%
```

#### プレイヤーレベルで確率上昇

```yaml
drops:
  - item: ENCHANTED_GOLDEN_APPLE
    amount: "1"
    chance: "0.01 + (killer.level * 0.001)"  # レベル1につき+0.1%
```

#### 天候で確率変化

```yaml
drops:
  - item: DIAMOND
    amount: "1"
    chance: "world.isThundering ? 0.8 : 0.2"  # 雷雨時80%
```

#### ゲームモード条件

```yaml
drops:
  - item: DIAMOND
    amount: "3"
    chance: "1.0"
    condition: "killer.gameMode == 'SURVIVAL'"  # サバイバルのみ
```

---

## 🎨 実践例

### 例1: プレイヤー数スケーリングボス

```yaml
PlayerScalingBoss:
  Type: WITHER_SKELETON
  Display: '&4&lScaling Boss'
  Health: "100 + (nearbyPlayers.count * 50)"
  Damage: "10 + (nearbyPlayers.avgLevel * 0.5)"

  Skills:
    OnTimer:
      - name: ScalingAttack
        interval: 100
        targeter:
          type: NearestPlayer
          range: 20
        skills:
          - skill: DynamicDamage

  drops:
    - item: DIAMOND
      amount: "math.max(1, nearbyPlayers.maxLevel / 10)"
      chance: "0.2 + (nearbyPlayers.count * 0.1)"
      condition: "killer.gameMode == 'SURVIVAL'"
```

```yaml
DynamicDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "target.maxHealth * 0.3"
```

---

### 例2: 時間帯適応Mob

```yaml
TimeAdaptiveMob:
  Type: ZOMBIE
  Display: '&8&lNight Terror'
  Health: "world.isNight ? 150.0 : 80.0"
  Damage: "world.isNight ? 15.0 : 8.0"

  Skills:
    OnTimer:
      - name: NightAssault
        interval: 60
        condition: "world.isNight"
        targeter:
          type: RadiusPlayers
          range: 15.0
        skills:
          - skill: NightDamage

  drops:
    - item: NETHER_STAR
      amount: "1"
      chance: "environment.moonPhase == 0 ? 0.5 : 0.05"
```

```yaml
NightDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "world.isNight ? 20.0 : 10.0"
    - type: PotionEffect
      effect: WEAKNESS
      duration: "world.isNight ? 200 : 0"
      amplifier: "1"
```

---

### 例3: HP段階別ボス

```yaml
PhaseChangeMob:
  Type: ENDER_DRAGON
  Display: '&5&lPhase Dragon'
  Health: "500"

  Skills:
    OnTimer:
      # フェーズ1: HP70%以上
      - name: Phase1
        interval: 80
        condition: "entity.health > entity.maxHealth * 0.7"
        targeter:
          type: NearestPlayer
          range: 30
        skills:
          - skill: WeakAttack

      # フェーズ2: HP30-70%
      - name: Phase2
        interval: 60
        condition: "entity.health > entity.maxHealth * 0.3 && entity.health <= entity.maxHealth * 0.7"
        targeter:
          type: RadiusPlayers
          range: 20.0
        skills:
          - skill: MediumAttack

      # フェーズ3: HP30%以下
      - name: Phase3
        interval: 40
        condition: "entity.health <= entity.maxHealth * 0.3"
        targeter:
          type: Area
          shape: CIRCLE
          radius: "25.0"
        skills:
          - skill: StrongAttack

  drops:
    - item: DIAMOND
      amount: "math.ceil((1 - entity.health / entity.maxHealth) * 10)"
      chance: "1.0"
```

---

## 📊 利用可能なCEL変数

### エンティティ情報
- `entity.health`: Double - 現在HP
- `entity.maxHealth`: Double - 最大HP
- `entity.type`: String - エンティティタイプ
- `target.health`, `target.maxHealth`, `target.level` など

### ワールド情報
- `world.isDay`: Boolean - 昼間かどうか
- `world.isNight`: Boolean - 夜間かどうか
- `world.hasStorm`: Boolean - 雨が降っているか
- `world.isThundering`: Boolean - 雷雨かどうか
- `world.time`: Long - ワールド時刻
- `world.difficulty`: String - 難易度

### 環境情報
- `environment.moonPhase`: Int - 月の満ち欠け (0=満月, 4=新月)
- `environment.dayOfCycle`: Int - 経過日数
- `environment.tickOfDay`: Int - 1日のtick

### プレイヤー情報
- `nearbyPlayers.count`: Int - プレイヤー数
- `nearbyPlayers.maxLevel`: Int - 最大レベル
- `nearbyPlayers.minLevel`: Int - 最小レベル
- `nearbyPlayers.avgLevel`: Double - 平均レベル
- `nearbyPlayerCount`: Int - プレイヤー数（短縮形）

### キラー情報（ドロップ時）
- `killer.level`: Int - プレイヤーレベル
- `killer.gameMode`: String - ゲームモード

### 数学・ランダム関数
- `math.abs(x)`, `math.max(a,b)`, `math.min(a,b)`
- `math.floor(x)`, `math.ceil(x)`, `math.round(x)`
- `random.range(min, max)`, `random.int(min, max)`
- `random.chance(probability)`, `random.boolean()`

### 距離関数
- `distance.horizontal(pos1, pos2)`: Double - 水平距離
- `distance.between(pos1, pos2)`: Double - 3D距離
- `distance.squared(pos1, pos2)`: Double - 距離の二乗

---

## 📖 関連ドキュメント

- **[CELクイックスタート](CEL_QUICK_START.md)** - CEL式の基本
- **[CEL拡張機能ガイド](CEL_EXTENSIONS_GUIDE.md)** - 全変数・関数リファレンス
- **[Effect一覧](REFERENCE_EFFECTS.md)** - 全Effectリファレンス
- **[Mob定義リファレンス](REFERENCE_MOB_DEFINITION.md)** - Mob定義の詳細

---

これでCEL動的機能を使いこなせるようになりました！プレイヤー数、レベル、時間帯に応じて変化する、ダイナミックなMobを作成しましょう。
