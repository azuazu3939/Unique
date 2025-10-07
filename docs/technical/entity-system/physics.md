# PacketMobPhysics - 物理システム

## 概要

`PacketMobPhysics` は、PacketMobの物理演算を管理するコンポーネントクラスです。重力、速度、衝突判定などの物理的な挙動を実装しています。

## クラス構造

```kotlin
class PacketMobPhysics(private val mob: PacketMob) {
    // 速度ベクトル
    private var velocityX: Double = 0.0
    private var velocityY: Double = 0.0
    private var velocityZ: Double = 0.0

    // 物理定数
    private val gravity = -0.08           // 重力加速度
    private val terminalVelocity = -3.92  // 終端速度
    private val groundFriction = 0.6      // 地面摩擦
    private val airResistance = 0.98      // 空気抵抗
}
```

## 主要機能

### 1. 重力処理

```kotlin
suspend fun applyGravity() {
    if (mob.isDead || !mob.hasGravity) return

    val location = mob.location
    val world = location.world ?: return

    // Y座標が0未満の場合は強制削除
    if (location.y < 0) {
        mob.kill()
        return
    }

    // 地面判定
    val blockBelow = world.getBlockAt(
        location.blockX,
        (location.y - 0.1).toInt(),
        location.blockZ
    )

    if (blockBelow.type.isSolid) {
        // 地面に接地している
        velocityY = 0.0

        // 地面摩擦を適用
        velocityX *= groundFriction
        velocityZ *= groundFriction
    } else {
        // 空中にいる - 重力を適用
        velocityY = (velocityY + gravity).coerceAtLeast(terminalVelocity)

        // 空気抵抗を適用
        velocityX *= airResistance
        velocityZ *= airResistance
    }
}
```

**物理定数の説明**:
- `gravity = -0.08`: 重力加速度（負の値 = 下向き）
- `terminalVelocity = -3.92`: 最大落下速度
- `groundFriction = 0.6`: 地面での減速率（60%に減速）
- `airResistance = 0.98`: 空気中での減速率（2%減速）

### 2. 速度の適用

```kotlin
suspend fun applyVelocity() {
    if (mob.isDead) return

    // 速度がほぼゼロの場合はスキップ
    if (abs(velocityX) < 0.001 && abs(velocityY) < 0.001 && abs(velocityZ) < 0.001) {
        return
    }

    val oldLocation = mob.location.clone()
    val newLocation = oldLocation.clone().add(velocityX, velocityY, velocityZ)

    // 衝突判定
    if (checkCollision(newLocation)) {
        // 衝突した場合は速度をリセット
        velocityX = 0.0
        velocityY = 0.0
        velocityZ = 0.0
        return
    }

    // 段差を登る処理
    val climbHeight = mob.wallClimbHeight
    if (climbHeight > 0 && velocityY == 0.0) {
        val climbLocation = tryClimbStep(newLocation, climbHeight)
        if (climbLocation != null) {
            newLocation.y = climbLocation.y
        }
    }

    // 移動実行
    val deltaX = newLocation.x - oldLocation.x
    val deltaY = newLocation.y - oldLocation.y
    val deltaZ = newLocation.z - oldLocation.z

    // パケット送信（距離に応じて選択）
    val distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
    if (distanceSquared > 64.0) {  // 8ブロック以上
        mob.teleport(newLocation)
    } else {
        mob.move(deltaX, deltaY, deltaZ)
    }
}
```

### 3. 移動処理

```kotlin
fun moveTowards(target: Location, speed: Double) {
    val direction = target.toVector()
        .subtract(mob.location.toVector())
        .normalize()

    // 速度を設定
    velocityX = direction.x * speed
    velocityZ = direction.z * speed

    // Y軸は重力に任せる（空中移動でない限り）
    // velocityY は重力処理で設定される
}

fun setVelocity(x: Double, y: Double, z: Double) {
    velocityX = x
    velocityY = y
    velocityZ = z
}

fun addVelocity(x: Double, y: Double, z: Double) {
    velocityX += x
    velocityY += y
    velocityZ += z
}

fun getVelocity(): Vector {
    return Vector(velocityX, velocityY, velocityZ)
}
```

### 4. 衝突判定

```kotlin
private fun checkCollision(location: Location): Boolean {
    val world = location.world ?: return false

    // エンティティのヒットボックス
    val width = mob.getEntityHitboxWidth() / 2.0
    val height = mob.getEntityHitboxHeight()

    // チェックする座標リスト
    val checkPoints = listOf(
        // 底面の4隅
        Location(world, location.x - width, location.y, location.z - width),
        Location(world, location.x + width, location.y, location.z - width),
        Location(world, location.x - width, location.y, location.z + width),
        Location(world, location.x + width, location.y, location.z + width),

        // 上面の4隅
        Location(world, location.x - width, location.y + height, location.z - width),
        Location(world, location.x + width, location.y + height, location.z - width),
        Location(world, location.x - width, location.y + height, location.z + width),
        Location(world, location.x + width, location.y + height, location.z + width),

        // 中央
        Location(world, location.x, location.y + height / 2, location.z)
    )

    // いずれかの座標が固体ブロック内にある場合は衝突
    return checkPoints.any { point ->
        world.getBlockAt(point.blockX, point.blockY, point.blockZ).type.isSolid
    }
}
```

**衝突判定のポイント**:
- ヒットボックスの8隅と中心をチェック
- 固体ブロックとの衝突を検出
- 液体（水、溶岩）は通過可能

### 5. 段差登り

```kotlin
private fun tryClimbStep(location: Location, maxHeight: Double): Location? {
    val world = location.world ?: return null

    // 現在の高さから maxHeight まで試行
    var testHeight = 0.1
    while (testHeight <= maxHeight) {
        val testLocation = location.clone().add(0.0, testHeight, 0.0)

        // この高さで衝突しないかチェック
        if (!checkCollision(testLocation)) {
            // 登った位置の下に固体ブロックがあるかチェック
            val blockBelow = world.getBlockAt(
                testLocation.blockX,
                (testLocation.y - 0.1).toInt(),
                testLocation.blockZ
            )

            if (blockBelow.type.isSolid) {
                return testLocation
            }
        }

        testHeight += 0.1
    }

    return null
}
```

**段差登りの仕組み**:
1. 現在位置から `wallClimbHeight` までの高さを0.1ブロックずつチェック
2. 衝突しない位置を見つける
3. その位置の下に固体ブロックがあれば登れる

### 6. ノックバック

```kotlin
fun applyKnockback(
    source: Location,
    strength: Double
) {
    // ノックバック耐性を適用
    val effectiveStrength = strength * (1.0 - mob.knockbackResistance)
    if (effectiveStrength <= 0.0) return

    // ソースから離れる方向を計算
    val direction = mob.location.toVector()
        .subtract(source.toVector())
        .normalize()
        .multiply(effectiveStrength)

    // 速度を追加（上向きの成分も追加）
    addVelocity(
        direction.x,
        0.4 * effectiveStrength,  // 上向きの力
        direction.z
    )

    DebugLogger.verbose("Applied knockback to ${mob.mobName}: strength=$effectiveStrength")
}
```

**ノックバックの計算**:
```
実効強度 = 基本強度 × (1 - ノックバック耐性)
水平速度 = 方向 × 実効強度
垂直速度 = 0.4 × 実効強度
```

## 物理プロパティ

### 重力設定

```yaml
AI:
  HasGravity: true          # 重力を適用するか
```

- `true`: 通常のMob（地面に落ちる）
- `false`: 飛行Mob（空中に浮く）

### 移動速度

```yaml
AI:
  MovementSpeed: 0.25       # 移動速度（ブロック/tick）
```

**速度の目安**:
- 0.1: 非常に遅い
- 0.25: 標準（ゾンビ相当）
- 0.35: 速い（スパイダー相当）
- 0.5+: 非常に速い

### ノックバック耐性

```yaml
AI:
  KnockbackResistance: 0.5  # 0.0-1.0（0=耐性なし、1=完全耐性）
```

### 段差登り

```yaml
AI:
  WallClimbHeight: 1.0      # 登れる段差の高さ（ブロック）
```

- 0.0: 段差を登れない
- 1.0: 1ブロックの段差まで登れる（標準）
- 2.0+: 高い段差も登れる

## カスタム物理挙動

### 飛行

```kotlin
// 重力を無効化
mob.hasGravity = false

// 上下の速度を制御
mob.physics.setVelocity(
    directionX * speed,
    directionY * speed,  // Y方向にも移動
    directionZ * speed
)
```

### ジャンプ

```kotlin
// 上向きの速度を与える
mob.physics.addVelocity(0.0, 0.5, 0.0)  // ジャンプ力0.5
```

### テレポート

```kotlin
// 大きな移動はテレポートを使用
mob.teleport(targetLocation)

// 速度をリセット
mob.physics.setVelocity(0.0, 0.0, 0.0)
```

### 浮遊エフェクト

```kotlin
// Y軸の速度を固定
mob.physics.setVelocity(
    velocityX,
    0.02,  // ゆっくり浮上
    velocityZ
)
```

## パフォーマンス最適化

### 速度の閾値

微小な速度は無視してパケット送信を削減:

```kotlin
if (abs(velocityX) < 0.001 && abs(velocityY) < 0.001 && abs(velocityZ) < 0.001) {
    return  // パケット送信をスキップ
}
```

### 衝突判定の最適化

必要最小限のポイントのみをチェック:

```kotlin
// 底面の4隅と中心の5点のみチェック（軽量版）
val checkPoints = listOf(
    bottomLeft, bottomRight, topLeft, topRight, center
)
```

### 重力処理の間引き

設定により重力処理を間引くことが可能:

```yaml
performance:
  physics_tick_interval: 1  # 物理処理の間隔（tick）
```

## トラブルシューティング

### Mobが地面を貫通する

**原因**:
- 速度が大きすぎて衝突判定をすり抜ける
- ヒットボックスが正しく設定されていない

**解決方法**:
- 移動速度を下げる
- 衝突判定の頻度を上げる

### Mobが壁にめり込む

**原因**:
- 衝突判定のポイントが不足
- ヒットボックスが実際より小さい

**解決方法**:
- `checkCollision` のチェックポイントを増やす
- ヒットボックスサイズを調整

### Mobが段差を登れない

**原因**:
- `WallClimbHeight` が 0 または小さい
- 衝突判定が厳しすぎる

**解決方法**:
```yaml
AI:
  WallClimbHeight: 1.0  # 1ブロックの段差を登れる
```

### ノックバックが効かない

**原因**:
- `KnockbackResistance` が 1.0（完全耐性）
- 速度がすぐにリセットされている

**解決方法**:
```yaml
AI:
  KnockbackResistance: 0.5  # 50%の耐性
```

### Mobが浮き続ける

**原因**:
- `HasGravity: false` に設定されている
- Y速度が正の値で固定されている

**解決方法**:
```yaml
AI:
  HasGravity: true  # 重力を有効化
```

---

**関連ドキュメント**:
- [PacketMob](packet-mob.md) - Mob本体の実装
- [AI System](ai-system.md) - AI処理と移動制御
- [Combat System](combat.md) - ノックバック効果
