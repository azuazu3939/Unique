# Targeter System - ターゲッターシステム概要

## 概要

ターゲッターシステムは、スキルやエフェクトの対象となるエンティティを選択する機能を提供します。範囲検索、条件フィルタリング、ソート、制限など高度なターゲット選択が可能です。

## ターゲッターの基本構造

```kotlin
abstract class Targeter(
    val id: String,
    val filter: String? = null,      // CEL式フィルター
    val entityType: EntityTypeFilter = EntityTypeFilter.LIVING
) {
    abstract fun getTargets(source: Entity): List<Entity>
    abstract fun getTargets(source: PacketEntity): List<Entity>
}
```

## ターゲッタータイプ一覧

### 基本ターゲッター

#### SelfTargeter
自分自身をターゲット。

```yaml
targeter:
  type: self
```

#### NearestPlayerTargeter
最も近いプレイヤーをターゲット。

```yaml
targeter:
  type: nearestPlayer
  range: 16.0
```

#### RadiusPlayersTargeter
範囲内の全プレイヤーをターゲット。

```yaml
targeter:
  type: radiusPlayers
  range: 16.0
  sortMode: NEAREST  # オプション
  limit: 5           # オプション
```

#### RadiusEntitiesTargeter
範囲内の全エンティティをターゲット。

```yaml
targeter:
  type: radiusEntities
  range: 16.0
  entityType: LIVING
  filter: "target.health < 50.0"  # CEL式フィルター
```

#### LineOfSightTargeter
視線先のエンティティをターゲット。

```yaml
targeter:
  type: lineOfSight
  maxDistance: 50.0
```

### 条件ターゲッター

#### ConditionalTargeter
ベースターゲッターの結果を条件でフィルタリング。

```yaml
targeter:
  type: conditional
  baseTargeter:
    type: radiusPlayers
    range: 20.0
  filter: "target.health < target.maxHealth * 0.5"
```

#### LowestHealthTargeter
最もHPが低いエンティティを選択。

```yaml
targeter:
  type: lowestHealth
  baseTargeter:
    type: radiusPlayers
    range: 20.0
```

#### HighestHealthTargeter
最もHPが高いエンティティを選択。

```yaml
targeter:
  type: highestHealth
  baseTargeter:
    type: radiusEntities
    range: 15.0
```

#### NearestTargeter
最も近いエンティティを選択。

```yaml
targeter:
  type: nearest
  baseTargeter:
    type: radiusEntities
    range: 20.0
```

#### FarthestTargeter
最も遠いエンティティを選択。

```yaml
targeter:
  type: farthest
  baseTargeter:
    type: radiusEntities
    range: 20.0
```

### 特殊ターゲッター

#### AreaTargeter
特定の形状のエリア内のエンティティをターゲット。

```yaml
# 円形
targeter:
  type: area
  shape: CIRCLE
  radius: 10.0

# 円錐
targeter:
  type: area
  shape: CONE
  radius: 10.0
  angle: 45.0
  direction: FORWARD

# 箱形
targeter:
  type: area
  shape: BOX
  width: 10.0
  height: 5.0
  depth: 10.0
  direction: FORWARD

# ドーナツ形
targeter:
  type: area
  shape: DONUT
  innerRadius: 5.0
  radius: 10.0
```

#### ChainTargeter
連鎖的にターゲットを拡散。

```yaml
targeter:
  type: chain
  initialTargeter:
    type: nearestPlayer
    range: 20.0
  maxChains: 5
  chainRange: 5.0
  chainCondition: "target.health > 0"  # オプション
```

#### ThreatTargeter
脅威度（Threat/Aggro）に基づいてターゲットを選択。

```yaml
targeter:
  type: threat
  baseTargeter:
    type: radiusPlayers
    range: 20.0
  count: 3  # 上位3体を選択
```

#### RandomTargeter
ランダムにターゲットを選択。

```yaml
targeter:
  type: random
  baseTargeter:
    type: radiusPlayers
    range: 20.0
  count: 3
```

#### LocationTargeter
特定の座標をターゲット。

```yaml
targeter:
  type: location
  x: 0
  y: 100
  z: 0
  world: world
```

#### AttackerTargeter
攻撃者をターゲット（OnDamagedトリガー専用）。

```yaml
targeter:
  type: attacker
```

## エンティティタイプフィルター

```kotlin
enum class EntityTypeFilter {
    PLAYER,      // プレイヤーのみ
    PACKET_MOB,  // PacketMobのみ
    LIVING,      // LivingEntityのみ（デフォルト）
    ENTITY       // 全てのEntity
}
```

**使用例**:
```yaml
targeter:
  type: radiusEntities
  range: 20.0
  entityType: PLAYER  # プレイヤーのみ
```

## ソートモード

```kotlin
enum class TargetSortMode {
    NONE,            // ソートなし
    NEAREST,         // 距離が近い順
    FARTHEST,        // 距離が遠い順
    LOWEST_HEALTH,   // HP低い順
    HIGHEST_HEALTH,  // HP高い順
    THREAT,          // ヘイト値高い順
    RANDOM,          // ランダム
    CUSTOM           // CEL式でカスタムソート
}
```

**使用例**:
```yaml
targeter:
  type: radiusPlayers
  range: 20.0
  sortMode: LOWEST_HEALTH
  limit: 3  # HP最低の3人を選択
```

## CEL式フィルター

ターゲットをCEL式でフィルタリングできます。

### 基本的なフィルター

```yaml
# 体力50%以下
filter: "target.health < target.maxHealth * 0.5"

# レベル10以上
filter: "target.level >= 10"

# 特定のワールド
filter: "target.world.name == 'world_nether'"
```

### 複雑なフィルター

```yaml
# 複数条件
filter: "target.health < 50.0 && target.level > 5 && !target.isCreative"

# 距離条件
filter: "distance(source, target) < 10.0"

# タグチェック
filter: "target.hasTag('boss')"
```

### カスタムソート

```yaml
targeter:
  type: radiusPlayers
  range: 20.0
  sortMode: CUSTOM
  sortExpression: "target.health / target.maxHealth"  # HP割合でソート
```

## offset と limit

ターゲットリストの一部を取得できます。

```yaml
targeter:
  type: radiusPlayers
  range: 20.0
  sortMode: NEAREST
  offset: 1   # 最も近いプレイヤーをスキップ
  limit: 5    # 2-6番目に近いプレイヤーを選択
```

**使用例**:
- 「自分以外の最も近いプレイヤー」: `offset: 1, limit: 1`
- 「上位10位まで」: `limit: 10`
- 「11-20位」: `offset: 10, limit: 10`

## 方向指定

一部のターゲッターは方向を指定できます。

```kotlin
enum class Direction {
    FORWARD,   // 前方
    BACKWARD,  // 後方
    LEFT,      // 左
    RIGHT,     // 右
    UP,        // 上
    DOWN       // 下
}
```

**使用例**:
```yaml
targeter:
  type: area
  shape: CONE
  radius: 10.0
  angle: 45.0
  direction: FORWARD  # 前方45度の円錐
```

## ターゲッターの組み合わせ

### 条件付きランダム選択

```yaml
targeter:
  type: random
  baseTargeter:
    type: conditional
    baseTargeter:
      type: radiusPlayers
      range: 20.0
    filter: "target.level > 10"
  count: 3
```

### 連鎖攻撃

```yaml
targeter:
  type: chain
  initialTargeter:
    type: nearest
    baseTargeter:
      type: radiusPlayers
      range: 20.0
  maxChains: 5
  chainRange: 5.0
```

### 複雑なフィルタリング

```yaml
targeter:
  type: lowestHealth
  baseTargeter:
    type: radiusEntities
    range: 20.0
    entityType: LIVING
    filter: "target.health > 0 && !target.isDead"
```

## トラブルシューティング

### ターゲットが見つからない

**原因**:
- `range` が小さすぎる
- `filter` が厳しすぎる
- `entityType` が合っていない

**解決方法**:
```yaml
debug:
  enabled: true
  verbose: true
```

デバッグログで実際のターゲット数を確認。

### フィルターが機能しない

**原因**:
- CEL式の構文エラー
- 変数名が間違っている

**解決方法**:
CEL式をテストする:
```kotlin
val result = celEvaluator.evaluate("target.health < 50.0", context)
```

---

**関連ドキュメント**:
- [Skill System](../skill-system/skill-executor.md) - スキルシステム
- [Effect System](../effect-system/effect-overview.md) - エフェクトシステム
- [CEL System](../core-systems/cel-engine.md) - CEL式評価
