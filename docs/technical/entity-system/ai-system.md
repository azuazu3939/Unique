# PacketMobAI - AI システム

## 概要

`PacketMobAI` は、PacketMobの行動ロジックを管理するコンポーネントクラスです。ターゲット検索、追跡、攻撃、徘徊などの基本的なMobの行動を実装しています。

## アーキテクチャ

### クラス構造

```kotlin
class PacketMobAI(
    private val mob: PacketMob,
    private val physics: PacketMobPhysics
) {
    // ターゲット管理
    private var currentTarget: Entity? = null
    private var targetLostTick: Int = 0

    // 攻撃管理
    private var lastAttackTick: Int = 0

    // 徘徊管理
    private var wanderTarget: Location? = null
    private var wanderCooldown: Int = 0
}
```

**依存関係**:
- `PacketMob`: AI制御対象のMob本体
- `PacketMobPhysics`: 移動処理を委譲

## AI ステートマシン

AIは以下の状態を持ちます:

```
┌──────────┐
│  IDLE    │ ← 初期状態
│  (待機)   │
└────┬─────┘
     │
     │ ターゲット発見
     ▼
┌──────────┐
│ PURSUING │
│ (追跡)    │
└────┬─────┘
     │
     │ 攻撃範囲内
     ▼
┌──────────┐
│ATTACKING │
│ (攻撃)    │
└────┬─────┘
     │
     │ ターゲット喪失
     ▼
┌──────────┐
│WANDERING │
│ (徘徊)    │
└────┬─────┘
     │
     │ 元の位置に戻る / 新しいターゲット発見
     ▼
┌──────────┐
│  IDLE    │
└──────────┘
```

## 主要機能

### 1. ターゲット検索

```kotlin
private fun findTarget(): Entity? {
    val searchRange = mob.followRange
    val location = mob.location
    val world = location.world ?: return null

    // 範囲内のプレイヤーを検索
    val nearbyPlayers = world.getNearbyEntities(
        location,
        searchRange,
        searchRange,
        searchRange
    )
        .filterIsInstance<Player>()
        .filter { player ->
            player.isValid &&
            !player.isDead &&
            player.gameMode != GameMode.SPECTATOR &&
            player.gameMode != GameMode.CREATIVE
        }

    if (nearbyPlayers.isEmpty()) return null

    // 最も近いプレイヤーを選択
    return nearbyPlayers.minByOrNull {
        it.location.distanceSquared(location)
    }
}
```

**検索条件**:
- `followRange` 以内のプレイヤー
- 有効かつ生存しているプレイヤー
- スペクテイターモード・クリエイティブモード以外

**最適化**:
- `distanceSquared()` による平方根計算の回避
- 設定可能な検索間隔（デフォルト: 20tick = 1秒）

### 2. ターゲット追跡

```kotlin
private fun pursueTarget(target: Entity) {
    val targetLoc = target.location
    val mobLoc = mob.location

    // 距離チェック
    val distance = targetLoc.distance(mobLoc)

    if (distance > mob.followRange) {
        // 範囲外 - ターゲット喪失
        loseTarget()
        return
    }

    if (distance <= mob.attackRange) {
        // 攻撃範囲内 - 攻撃試行
        tryAttack(target)
    } else {
        // 追跡 - ターゲットに向かって移動
        physics.moveTowards(targetLoc, mob.movementSpeed)
    }

    // ターゲットの方を向く（設定可能）
    if (mob.lookAtMovementDirection) {
        lookAt(targetLoc)
    }
}
```

**移動アルゴリズム**:
1. ターゲットへの方向ベクトルを計算
2. 移動速度を適用
3. 物理システムに速度を設定
4. 次のtickで実際の移動を実行

**視線制御**:
```kotlin
private fun lookAt(target: Location) {
    val direction = target.toVector()
        .subtract(mob.location.toVector())
        .normalize()

    val yaw = Math.toDegrees(atan2(-direction.x, direction.z)).toFloat()
    val pitch = Math.toDegrees(asin(-direction.y)).toFloat()

    mob.location.yaw = yaw
    mob.location.pitch = pitch

    // ビューワーに回転パケット送信
    mob.viewers.forEach { uuid ->
        Bukkit.getPlayer(uuid)?.let { player ->
            PacketSender.sendRotationPacket(player, mob.entityId, yaw, pitch)
        }
    }
}
```

### 3. 攻撃処理

```kotlin
private fun tryAttack(target: Entity) {
    // クールダウンチェック
    val ticksSinceLastAttack = mob.ticksLived - lastAttackTick
    if (ticksSinceLastAttack < mob.attackCooldown) {
        return
    }

    // 攻撃範囲チェック
    val distance = target.location.distance(mob.location)
    if (distance > mob.attackRange) {
        return
    }

    // 攻撃実行
    performAttack(target)
    lastAttackTick = mob.ticksLived
}

private suspend fun performAttack(target: Entity) {
    val damage = calculateDamage()

    // 攻撃イベント発火
    val attackEvent = PacketMobAttackEvent(mob, target, damage)
    if (EventUtil.callEvent(attackEvent)) {
        DebugLogger.verbose("Attack cancelled by event")
        return
    }

    val finalDamage = attackEvent.damage

    // ダメージ適用
    when (target) {
        is Player -> {
            target.damage(finalDamage, mob.location.world?.spawnEntity(
                mob.location,
                EntityType.AREA_EFFECT_CLOUD
            ))
        }
        is LivingEntity -> {
            target.damage(finalDamage)
        }
    }

    // 攻撃アニメーション
    mob.playAnimation(EntityAnimation.SWING_MAIN_ARM)

    // OnAttackスキルトリガー実行
    val mobInstance = Unique.instance.mobManager.getMobInstance(mob)
    mobInstance?.let { instance ->
        Unique.instance.mobManager.executeSkillTriggers(
            mob,
            instance.definition.skills.onAttack,
            PacketMobSkillEvent.SkillTriggerType.ON_ATTACK
        )
    }

    DebugLogger.debug("${mob.mobName} attacked ${target.name} for $finalDamage damage")
}
```

**ダメージ計算**:
```kotlin
private fun calculateDamage(): Double {
    val baseDamage = mob.damage

    // ダメージ式がある場合はCEL評価
    val formula = mob.damageFormula
    if (formula != null) {
        try {
            val context = CELVariableProvider.buildPacketEntityContext(mob)
            return Unique.instance.celEvaluator.evaluateNumber(formula, context)
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage formula: $formula", e)
        }
    }

    return baseDamage
}
```

**攻撃プロパティ**:
- `damage`: 基本ダメージ
- `damageFormula`: CEL式によるダメージ計算（オプション）
- `attackRange`: 攻撃範囲（デフォルト: 2.0ブロック）
- `attackCooldown`: 攻撃間隔（デフォルト: 20tick = 1秒）

### 4. 徘徊処理

ターゲットがいない場合、Mobは徘徊行動を取ります。

```kotlin
private fun wander() {
    // クールダウン中はスキップ
    if (wanderCooldown > 0) {
        wanderCooldown--
        return
    }

    // 徘徊目標が設定されていない、または到達した場合
    if (wanderTarget == null || hasReachedWanderTarget()) {
        // 新しい徘徊目標を設定
        wanderTarget = generateWanderTarget()
        wanderCooldown = 60 + Random.nextInt(120)  // 3-9秒
    }

    // 徘徊目標に向かって移動
    wanderTarget?.let { target ->
        physics.moveTowards(target, mob.movementSpeed * 0.5)  // 通常の半分の速度
    }
}

private fun generateWanderTarget(): Location {
    val currentLoc = mob.location
    val range = 8.0  // 8ブロック範囲内で徘徊

    val offsetX = (Random.nextDouble() - 0.5) * range * 2
    val offsetZ = (Random.nextDouble() - 0.5) * range * 2

    val targetX = currentLoc.x + offsetX
    val targetZ = currentLoc.z + offsetZ

    val world = currentLoc.world ?: return currentLoc
    val targetY = world.getHighestBlockYAt(targetX.toInt(), targetZ.toInt()).toDouble() + 1

    return Location(world, targetX, targetY, targetZ)
}

private fun hasReachedWanderTarget(): Boolean {
    val target = wanderTarget ?: return true
    val distance = mob.location.distance(target)
    return distance < 1.0  // 1ブロック以内で到達とみなす
}
```

**徘徊アルゴリズム**:
1. ランダムな方向に徘徊目標を生成（8ブロック範囲内）
2. 徘徊目標に向かって移動（通常の半分の速度）
3. 目標到達後、3-9秒のクールダウン
4. クールダウン後、新しい徘徊目標を生成

### 5. ターゲット喪失処理

```kotlin
private fun loseTarget() {
    if (currentTarget == null) return

    val oldTarget = currentTarget
    currentTarget = null
    targetLostTick = mob.ticksLived

    // ターゲット喪失イベント発火
    val event = PacketMobTargetEvent(
        mob,
        oldTarget,
        null,
        PacketMobTargetEvent.TargetReason.TARGET_LOST
    )
    EventUtil.callEvent(event)

    DebugLogger.verbose("${mob.mobName} lost target: ${oldTarget?.name}")
}
```

**喪失条件**:
- ターゲットが死亡
- ターゲットが無効化
- ターゲットが `followRange` を超えて離れた
- ターゲットがスペクテイターモードに変更

## Tick処理フロー

```kotlin
suspend fun tick() {
    // ターゲット検索（間隔制御）
    if (mob.ticksLived % mob.targetSearchInterval == 0) {
        if (currentTarget == null || !isValidTarget(currentTarget)) {
            currentTarget = findTarget()

            if (currentTarget != null) {
                // ターゲット獲得イベント
                val event = PacketMobTargetEvent(
                    mob,
                    null,
                    currentTarget,
                    PacketMobTargetEvent.TargetReason.CLOSEST_PLAYER
                )
                if (EventUtil.callEvent(event)) {
                    currentTarget = null
                } else {
                    currentTarget = event.newTarget
                    DebugLogger.verbose("${mob.mobName} found target: ${currentTarget?.name}")
                }
            }
        }
    }

    // AI行動
    when {
        currentTarget != null -> pursueTarget(currentTarget!!)
        else -> wander()
    }
}
```

**処理順序**:
1. ターゲット検索（設定間隔ごと）
2. ターゲットの有効性チェック
3. ターゲット追跡 または 徘徊
4. 視線更新（設定に応じて）

## AIプロパティ

### 移動関連

```yaml
AI:
  MovementSpeed: 0.25        # 移動速度（ブロック/tick）
  FollowRange: 16.0          # 追跡範囲（ブロック）
  LookAtMovementDirection: true  # 移動方向を向くか
  WallClimbHeight: 1.0       # 登れる段差の高さ
```

**MovementSpeed**:
- 0.1: 非常に遅い（スライムなど）
- 0.25: 標準（ゾンビ、スケルトンなど）
- 0.35: 速い（スパイダーなど）
- 0.5+: 非常に速い（ボスMobなど）

**FollowRange**:
- プレイヤー検索範囲
- この範囲を出るとターゲット喪失
- デフォルト: 16.0ブロック

**LookAtMovementDirection**:
- `true`: 移動方向を常に向く（推奨）
- `false`: 移動中も現在の向きを維持

**WallClimbHeight**:
- 登れる段差の高さ
- 0.0: 段差を登れない
- 1.0: 1ブロックの段差まで登れる（標準）
- 2.0+: 高い段差も登れる

### 戦闘関連

```yaml
AttackDamage: 10.0           # 攻撃ダメージ
DamageFormula: "damage * (1 + nearbyPlayers.count * 0.1)"  # ダメージ式（オプション）
AttackRange: 2.0             # 攻撃範囲（ブロック）
AttackCooldown: 20           # 攻撃間隔（tick）
```

**AttackRange**:
- 近接攻撃: 1.5-3.0
- 中距離攻撃: 5.0-10.0
- 遠距離攻撃: スキルシステムを使用

**AttackCooldown**:
- 攻撃間隔（tick）
- 20tick = 1秒（標準）
- 10tick = 0.5秒（速攻）
- 40tick = 2秒（強力な攻撃）

### ターゲット検索

```yaml
TargetSearchInterval: 20     # ターゲット検索間隔（tick）
```

**TargetSearchInterval**:
- ターゲット検索の頻度
- 20tick = 1秒（標準）
- 10tick = 0.5秒（レスポンシブ）
- 40tick = 2秒（省エネ）

**パフォーマンス影響**:
- 短い間隔: 反応が速いが CPU 負荷増
- 長い間隔: CPU 負荷減だが反応が遅い

## イベント

### PacketMobTargetEvent

ターゲット変更時に発火します。

```kotlin
@EventHandler
fun onMobTarget(event: PacketMobTargetEvent) {
    val mob = event.mob
    val oldTarget = event.oldTarget
    val newTarget = event.newTarget
    val reason = event.reason

    when (reason) {
        TargetReason.CLOSEST_PLAYER -> {
            // 最も近いプレイヤーをターゲット
        }
        TargetReason.ATTACKED_BY -> {
            // 攻撃を受けてターゲット変更
        }
        TargetReason.TARGET_LOST -> {
            // ターゲット喪失
        }
        TargetReason.CUSTOM -> {
            // カスタムロジックによるターゲット変更
        }
    }

    // ターゲット変更をキャンセル
    if (someCondition) {
        event.isCancelled = true
    }

    // ターゲットを変更
    event.newTarget = differentTarget
}
```

### PacketMobAttackEvent

攻撃実行前に発火します。

```kotlin
@EventHandler
fun onMobAttack(event: PacketMobAttackEvent) {
    val mob = event.mob
    val target = event.target
    var damage = event.damage

    // ダメージ量を変更
    event.damage = damage * 1.5

    // 攻撃をキャンセル
    if (target is Player && target.isBlocking) {
        event.isCancelled = true
    }
}
```

## カスタムAI

### AI無効化

```yaml
CustomMob:
  Type: ZOMBIE
  AI:
    HasAI: false  # AI完全無効化
```

AIを無効化すると:
- ターゲット検索なし
- 移動なし（重力のみ適用）
- 攻撃なし

**用途**:
- 装飾用エンティティ
- スキルによって制御されるMob
- カスタムAIロジックを実装する場合

### カスタムターゲティング

イベントを使用してターゲット選択をカスタマイズできます:

```kotlin
@EventHandler
fun onMobTarget(event: PacketMobTargetEvent) {
    val mob = event.mob

    // 体力が最も低いプレイヤーをターゲット
    val players = mob.location.world?.players
        ?.filter { it.location.distance(mob.location) < mob.followRange }
        ?.sortedBy { it.health }

    event.newTarget = players?.firstOrNull()
}
```

### カスタム移動パターン

物理システムに直接速度を設定することでカスタム移動パターンを実装できます:

```kotlin
// 円形移動
val angle = (mob.ticksLived * 0.1) % (2 * Math.PI)
val radius = 5.0
val centerX = spawnLocation.x
val centerZ = spawnLocation.z

val targetX = centerX + radius * cos(angle)
val targetZ = centerZ + radius * sin(angle)

mob.physics.setVelocity(
    (targetX - mob.location.x) * 0.1,
    0.0,
    (targetZ - mob.location.z) * 0.1
)
```

## パフォーマンス最適化

### AI間引き

```yaml
performance:
  ai_tick_interval: 1  # AI処理の間隔（tick）
                       # 1 = 毎tick実行（デフォルト）
                       # 2 = 1tick おきに実行
```

### ビューワーチェック

```yaml
performance:
  skip_ai_when_no_viewers: true  # ビューワーがいない場合AIをスキップ
```

プレイヤーが周囲にいない場合、AI処理をスキップしてCPU負荷を削減。

### ターゲット検索最適化

```yaml
TargetSearchInterval: 40  # 検索頻度を下げる（2秒に1回）
```

頻繁にターゲット検索する必要がない場合、間隔を長くしてCPU負荷を削減。

## トラブルシューティング

### Mobが動かない

**原因**:
- `HasAI: false` に設定されている
- ビューワーがいないため AI がスキップされている
- 物理システムが無効化されている

**解決方法**:
```yaml
AI:
  HasAI: true

performance:
  skip_ai_when_no_viewers: false
```

### Mobが攻撃しない

**原因**:
- `AttackRange` が小さすぎる
- `AttackCooldown` が長すぎる
- ターゲットを検出できていない

**解決方法**:
```yaml
AttackRange: 3.0           # 攻撃範囲を広げる
AttackCooldown: 20         # 攻撃間隔を短縮
FollowRange: 32.0          # 追跡範囲を広げる
```

デバッグログを有効化して確認:
```yaml
debug:
  enabled: true
  verbose: true
```

### Mobが壁にぶつかる

**原因**:
- 衝突判定が不完全
- 経路探索が未実装

**解決方法**:
- `WallClimbHeight` を調整して段差を登れるようにする
- より高度なAIが必要な場合はカスタムAIロジックを実装

### ターゲットの切り替えが遅い

**原因**:
- `TargetSearchInterval` が長い

**解決方法**:
```yaml
TargetSearchInterval: 10  # 0.5秒ごとに検索
```

---

**関連ドキュメント**:
- [PacketMob](packet-mob.md) - Mob本体の実装
- [Physics System](physics.md) - 物理演算と移動
- [Combat System](combat.md) - 戦闘システム
