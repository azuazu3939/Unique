# Unique Plugin - 拡張機能ドキュメント (CELベース)

このドキュメントでは、**CELベースの最新実装方針**に従った高度な機能を解説します。
Javaクラスを増やさず、YAMLとCELで柔軟な機能を実現する設計です。

---

## 📋 目次

1. [CELベース設計思想](#celベース設計思想)
2. [高度なターゲッター](#高度なターゲッター)
3. [高度なスキル](#高度なスキル)
4. [CEL式の実践例](#cel式の実践例)
5. [実装例](#実装例)

---

## CELベース設計思想

### 原則

1. **条件判定はCELで記述** - Java側に条件ロジックを書かない
2. **動的計算はCELで実行** - ダメージ、範囲、個数などを式で計算
3. **Javaは実行環境のみ** - データ取得と適用だけを担当
4. **YAML中心の設定** - すべてのロジックをYAMLに記述

### 利用可能なCEL変数・関数

#### Math関数
```yaml
# 基本演算
amount: "math.max(nearbyPlayers.count, 5)"
damage: "math.ceil(entity.maxHealth * 0.1)"

# 三角関数
angle: "math.atan2(target.location.z - source.location.z, target.location.x - source.location.x)"
wave: "math.sin(environment.tickOfDay * 0.001) * 10 + 10"

# 定数
circleArea: "math.PI * math.pow(radius, 2)"
```

#### Random関数
```yaml
# ランダム整数
amount: "random.int(3, 8)"

# ランダム実数
velocity: "random.range(0.5, 2.0)"

# 確率判定
condition: "random.chance(0.3)"

# ランダムboolean
condition: "random.boolean() && entity.health < 50"
```

#### Distance関数
```yaml
# 3D距離
condition: "distance.between(source.location, target.location) < 10.0"

# 水平距離のみ
damage: "20 * (1 - distance.horizontal(source.location, target.location) / 15.0)"

# 距離の2乗（高速計算）
condition: "distance.squared(pos1, pos2) < 100.0"  # 10ブロック以内
```

#### String関数
```yaml
# バイオームチェック
condition: "string.contains(environment.biome, 'FOREST')"

# 大文字小文字変換
name: "string.toUpperCase(player.name)"

# 長さチェック
condition: "string.length(target.name) > 5"
```

#### Environment変数
```yaml
# 月相
condition: "environment.moonPhase == 0"  # 満月

# バイオーム
condition: "environment.biome == 'PLAINS'"

# 時刻（tick単位）
damage: "world.isNight ? 30 : 20"
```

#### NearbyPlayers統計
```yaml
# プレイヤー数
amount: "math.min(nearbyPlayers.count, 10)"

# 平均レベル
condition: "nearbyPlayers.avgLevel >= 15"

# 最大・最小レベル
damage: "10 + nearbyPlayers.maxLevel"
condition: "nearbyPlayers.minLevel < 5"
```

---

## 高度なターゲッター

### 1. ConditionalTargeter (条件付きフィルタリング)

**概要**: 基底ターゲッターの結果をCEL式でフィルタリング

```yaml
FilteredTargeter:
  type: Conditional
  baseTargeter:
    type: RadiusPlayers
    range: 30
  filter: "target.health > 50 && target.gameMode == 'SURVIVAL'"
```

**CEL式で動的フィルタ**:
```yaml
# プレイヤーレベルで条件分岐
filter: "target.level >= nearbyPlayers.avgLevel"

# 距離と体力の組み合わせ
filter: "distance.horizontal(source.location, target.location) < 15.0 && target.health > target.maxHealth * 0.5"

# ランダム選択
filter: "random.chance(0.5)"
```

---

### 2. ChainTargeter (連鎖ターゲティング)

**概要**: 最初のターゲットから連鎖的に拡散

**稲妻攻撃の例**:
```yaml
LightningChain:
  type: Chain
  initialTargeter:
    type: NearestPlayer
    range: 20
  maxChains: 5
  chainRange: 5.0
  condition: "target.health > 0"
```

**CEL式で連鎖制御**:
```yaml
# プレイヤー数で連鎖数を動的に
maxChains: "math.min(nearbyPlayers.count, 8)"

# 距離を式で計算
chainRange: "5.0 + random.range(0, 3.0)"

# 体力が高い対象のみ連鎖
condition: "target.health > target.maxHealth * 0.3"
```

---

### 3. AreaTargeter (エリア選択)

**概要**: Circle, Box, Cone, Donut形状でターゲット選択

#### Circle（円形）
```yaml
CircleArea:
  type: Area
  shape: CIRCLE
  radius: "10.0 + math.floor(nearbyPlayers.count / 2)"  # CEL式で半径計算
  center: SOURCE
```

#### Cone（円錐）
```yaml
ConeBlast:
  type: Area
  shape: CONE
  radius: "15.0"
  angle: "math.toRadians(45)"  # 度数法→ラジアン変換
  direction: FORWARD
```

#### Donut（ドーナツ）
```yaml
DonutZone:
  type: Area
  shape: DONUT
  innerRadius: "5.0"
  outerRadius: "distance.horizontal(source.location, target.location)"  # 動的範囲
  center: TARGET
```

**パラメータにCEL式を使用**:
```yaml
# 時間帯で範囲変動
radius: "world.isNight ? 15.0 : 10.0"

# プレイヤー数でスケール
radius: "math.min(30.0, 10.0 + nearbyPlayers.count * 2)"

# ランダム範囲
radius: "random.range(8.0, 15.0)"
```

---

## 高度なスキル

### 1. SummonSkill (召喚スキル)

**CEL式で召喚数を動的計算**:
```yaml
AdaptiveSummon:
  type: Summon
  mobType: "ZOMBIE"
  amount: "math.min(nearbyPlayers.count, 5)"  # プレイヤー数に応じて
  duration: "random.int(100, 300)"  # ランダム持続時間
  offset:
    x: "random.range(-3.0, 3.0)"
    y: 0
    z: "random.range(-3.0, 3.0)"
```

**カスタムMob召喚**:
```yaml
BossSummon:
  type: Summon
  customMob: "MiniBoss"
  amount: "math.ceil(nearbyPlayers.avgLevel / 10)"
  health: "entity.maxHealth * 0.5"
  duration: 600
```

---

### 2. TeleportSkill (テレポートスキル)

**CEL式で座標計算**:
```yaml
DynamicTeleport:
  type: Teleport
  mode: OFFSET
  offset:
    x: "math.cos(math.toRadians(source.location.yaw)) * 10"
    y: "2.0 + random.range(0, 3.0)"
    z: "math.sin(math.toRadians(source.location.yaw)) * 10"
  safeLocation: true
```

**背後にテレポート（距離を式で計算）**:
```yaml
BehindTeleport:
  type: Teleport
  mode: BEHIND
  distance: "5.0 + random.range(0, 2.0)"
```

---

### 3. BuffSkill (バフ/デバフスキル)

**CEL式で効果時間・強度を動的計算**:
```yaml
ScalingBuff:
  type: Buff
  potionEffects:
    - type: STRENGTH
      amplifier: "math.floor(nearbyPlayers.avgLevel / 10)"  # レベルで強度
      showIcon: true
    - type: SPEED
      amplifier: "random.int(0, 2)"
  duration: "100 + nearbyPlayers.count * 20"  # プレイヤー数で持続時間
```

**属性変更**:
```yaml
HealthBoost:
  type: Buff
  attributeModifiers:
    - attribute: MAX_HEALTH
      amount: "20.0 + nearbyPlayers.maxLevel"
      operation: ADD
  duration: "math.min(600, nearbyPlayers.count * 100)"
```

---

### 4. CommandSkill (コマンド実行スキル)

**プレースホルダー + CEL変数**:
```yaml
RewardCommand:
  type: Command
  executor: CONSOLE
  commands:
    - "give {target} diamond {amount}"  # {amount}はCEL式で計算
    - "say {target} received a reward!"
  amount: "math.ceil(nearbyPlayers.avgLevel / 5)"
```

---

## CEL式の実践例

### HP段階制ボス

```yaml
PhaseBasedBoss:
  Type: WITHER_SKELETON
  Health: 500
  Skills:
    OnTimer:
      # フェーズ1: HP > 70%
      - skill: NormalAttack
        interval: 3s
        condition: "entity.health > entity.maxHealth * 0.7"

      # フェーズ2: HP 30-70%
      - skill: PowerAttack
        interval: 2s
        condition: "entity.health > entity.maxHealth * 0.3 && entity.health <= entity.maxHealth * 0.7"

      # フェーズ3: HP < 30%
      - skill: BerserkMode
        interval: 1s
        condition: "entity.health <= entity.maxHealth * 0.3"
```

### 距離適応型Mob

```yaml
RangeAdaptiveMob:
  Type: SKELETON
  Health: 100
  Skills:
    OnTimer:
      # 近距離: 近接攻撃
      - skill: MeleeAttack
        interval: 1.5s
        condition: "distance.horizontal(entity.location, target.location) < 5.0"

      # 中距離: 矢
      - skill: ArrowShot
        interval: 2s
        condition: "distance.horizontal(entity.location, target.location) >= 5.0 && distance.horizontal(entity.location, target.location) < 15.0"

      # 遠距離: テレポート接近
      - skill: TeleportClose
        interval: 5s
        condition: "distance.horizontal(entity.location, target.location) >= 15.0"
```

### プレイヤー数スケーリング

```yaml
ScalingBoss:
  Type: ZOMBIE
  Health: "100 + nearbyPlayers.count * 50"  # プレイヤー数でHP増加
  Skills:
    OnTimer:
      - skill: SummonMinions
        interval: 10s
        targeter:
          type: Self
        amount: "math.min(nearbyPlayers.count, 8)"  # 最大8体

      - skill: AreaDamage
        interval: 5s
        targeter:
          type: Area
          shape: CIRCLE
          radius: "10.0 + nearbyPlayers.count * 2"  # プレイヤー数で範囲拡大
        damage: "15 + nearbyPlayers.avgLevel"
```

### 時間・環境反応型

```yaml
EnvironmentMob:
  Type: PHANTOM
  Health: 120
  Skills:
    OnTimer:
      # 夜間パワーアップ
      - skill: NightBoost
        interval: 1s
        condition: "world.isNight"
        damage: "30"

      # 昼間は弱体化
      - skill: DayAttack
        interval: 3s
        condition: "world.isDay"
        damage: "10"

      # 満月時特殊攻撃
      - skill: FullMoonBlast
        interval: 15s
        condition: "environment.moonPhase == 0 && world.isNight"
        damage: "50"

      # 雷雨時連鎖攻撃
      - skill: StormChain
        interval: 8s
        condition: "world.isThundering"
        targeter:
          type: Chain
          maxChains: "random.int(3, 7)"
```

---

## 実装例

### 複雑なボスMob (CEL統合版)

```yaml
AncientDragon:
  Type: ENDER_DRAGON
  Display: '&5&l&k||&r &d&lAncient Dragon&r &5&l&k||'
  Health: "1000 + nearbyPlayers.count * 200"  # プレイヤー数でスケール
  Damage: "30 + nearbyPlayers.maxLevel"

  Skills:
    OnTimer:
      # フェーズ1: HP > 70%
      - skill: FireballBarrage
        interval: 3s
        condition: "entity.health > entity.maxHealth * 0.7"
        targeter:
          type: RadiusPlayers
          range: 30
          filter: "target.gameMode == 'SURVIVAL'"
        amount: "random.int(3, 5)"

      # フェーズ2: HP 40-70%
      - skill: ChainLightning
        interval: 5s
        condition: "entity.health > entity.maxHealth * 0.4 && entity.health <= entity.maxHealth * 0.7"
        targeter:
          type: Chain
          maxChains: "math.min(nearbyPlayers.count, 8)"
          chainRange: "7.0 + random.range(0, 3.0)"

      # フェーズ3: HP < 40%
      - skill: SummonGuardians
        interval: 15s
        condition: "entity.health <= entity.maxHealth * 0.4"
        amount: "math.ceil(nearbyPlayers.count / 2)"

      - skill: AreaBlast
        interval: 8s
        condition: "entity.health <= entity.maxHealth * 0.4"
        targeter:
          type: Area
          shape: CIRCLE
          radius: "15.0 + nearbyPlayers.count"
        damage: "40 + random.int(0, 20)"
```

---

## 🔧 実装状況

### ✅ 完了
- [x] CEL変数大幅拡張 (math, random, distance, string, environment, nearbyPlayers)
- [x] ConditionalTargeter (CEL式フィルタ)
- [x] ChainTargeter (CEL式連鎖制御)
- [x] AreaTargeter (CEL式で範囲計算)
- [x] SummonSkill (CEL式で動的召喚)
- [x] TeleportSkill (CEL式で座標計算)
- [x] BuffSkill (CEL式で効果計算)
- [x] CommandSkill (プレースホルダー + CEL)
- [x] サンプルYAML作成（CEL式活用）
- [x] 包括的ドキュメント

### 📋 次のステップ
- [ ] イベントリスナー統合
- [ ] コマンドハンドラー
- [ ] 総合テスト
- [ ] パフォーマンス最適化

---

## 📖 関連ドキュメント

- [CELクイックスタート](docs/CEL_QUICK_START.md)
- [CEL変数・関数リファレンス](docs/CEL_EXTENSIONS_GUIDE.md)
- [CEL実装コミットガイド](docs/CEL_IMPLEMENTATION_COMMIT.md)
- [ドロップシステム](docs/drop-system.md)
- [プロジェクトナレッジ](cel_mob_plugin_knowledge.md)

---

## 🎮 使い方

1. **スキルを定義**: `plugins/Unique/skills/` にYAMLファイルを配置
2. **Mobを定義**: `plugins/Unique/mobs/` にYAMLファイルを配置（CEL式を活用）
3. **スポーンを設定**: `plugins/Unique/spawns/` にYAMLファイルを配置
4. **リロード**: `/unique reload`

**CEL式のテスト**: `/unique debug cel <expression>` で式の評価結果を確認

---

**設計思想**: Javaクラスを増やさず、YAMLとCELで柔軟に実現 ❤️
