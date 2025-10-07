# Effect System - エフェクトシステム概要

## 概要

エフェクトシステムは、ターゲットに対する様々な作用を提供します。ダメージ、回復、ノックバック、パーティクル、サウンドなど15種類以上のエフェクトタイプがあります。

## エフェクトの基本構造

```kotlin
interface Effect {
    suspend fun apply(source: Entity, target: Entity)
    suspend fun apply(source: PacketEntity, target: Entity)
}
```

## エフェクトタイプ一覧

### ダメージ系

#### DamageEffect
ターゲットにダメージを与えます。

```yaml
effects:
  - type: damage
    amount: "10.0 + source.level * 2"  # CEL式対応
    sync: true
```

#### PercentDamageEffect
ターゲットの最大体力に対する割合ダメージ。

```yaml
effects:
  - type: percentDamage
    percent: 0.2  # 20%ダメージ
```

### 回復系

#### HealEffect
ターゲットを回復します。

```yaml
effects:
  - type: heal
    amount: "5.0 + nearbyPlayers.count"  # CEL式対応
```

### ノックバック系

#### KnockbackEffect
ターゲットをノックバックさせます。

```yaml
effects:
  - type: knockback
    strength: 1.5
    vertical: 0.5  # 上向きの力
```

### パーティクル系

#### ParticleEffect
パーティクルを表示します。

```yaml
effects:
  - type: particle
    particle: FLAME
    count: 10
    offsetX: 0.5
    offsetY: 0.5
    offsetZ: 0.5
    speed: 0.1
```

#### ParticleCircleEffect
円形パーティクルを表示します。

```yaml
effects:
  - type: particleCircle
    particle: FLAME
    radius: 5.0
    points: 20
    height: 1.0
```

#### ParticleSphereEffect
球形パーティクルを表示します。

```yaml
effects:
  - type: particleSphere
    particle: PORTAL
    radius: 3.0
    points: 50
```

### サウンド系

#### SoundEffect
サウンドを再生します。

```yaml
effects:
  - type: sound
    sound: entity.generic.explode
    volume: 1.0
    pitch: 1.0
    category: HOSTILE
```

### ステータス効果系

#### PotionEffect
ポーション効果を付与します。

```yaml
effects:
  - type: potion
    potion: SPEED
    duration: 5s
    amplifier: 1
```

#### IgniteEffect
ターゲットを発火させます。

```yaml
effects:
  - type: ignite
    duration: 5s
```

### メッセージ系

#### MessageEffect
メッセージを送信します。

```yaml
effects:
  - type: message
    message: "You have been hit!"
```

#### TitleEffect
タイトルを表示します。

```yaml
effects:
  - type: title
    title: "&cWarning!"
    subtitle: "&eYou are in danger"
    fadeIn: 10
    stay: 40
    fadeOut: 10
```

### テレポート系

#### TeleportEffect
ターゲットをテレポートさせます。

```yaml
effects:
  - type: teleport
    x: 0
    y: 100
    z: 0
    world: world
```

#### PullEffect
ターゲットをソースに引き寄せます。

```yaml
effects:
  - type: pull
    strength: 1.0
```

#### LaunchEffect
ターゲットを打ち上げます。

```yaml
effects:
  - type: launch
    velocityX: 0.0
    velocityY: 2.0
    velocityZ: 0.0
```

### コマンド実行系

#### CommandEffect
コマンドを実行します。

```yaml
effects:
  - type: command
    command: "give {player} diamond 1"
    asOp: false
```

## CEL式の活用

多くのエフェクトはCEL式をサポートしており、動的な値の計算が可能です。

### ダメージ計算

```yaml
effects:
  - type: damage
    amount: "10.0 * (1 + nearbyPlayers.count * 0.1)"  # プレイヤー数に応じて増加
```

### 条件付きエフェクト

```yaml
effects:
  - type: damage
    amount: "target.health < target.maxHealth * 0.5 ? 20.0 : 10.0"  # 体力50%以下で2倍ダメージ
```

### 複雑な計算

```yaml
effects:
  - type: heal
    amount: "min(target.maxHealth - target.health, 10.0 + source.level)"  # 不足分または最大値
```

## エフェクトの組み合わせ

複数のエフェクトを同時に適用できます:

```yaml
Skills:
  - "projectile{
      damage=15.0,
      knockback=1.0,
      particle=FLAME,
      sound=entity.blaze.shoot,
      ignite=3s
    } @direction ~onTimer:30t"
```

これは以下と同等です:

```yaml
effects:
  - type: damage
    amount: 15.0
  - type: knockback
    strength: 1.0
  - type: particle
    particle: FLAME
  - type: sound
    sound: entity.blaze.shoot
  - type: ignite
    duration: 3s
```

## 同期/非同期実行

```yaml
effects:
  - type: damage
    amount: 10.0
    sync: true  # メインスレッドで実行（必要な場合のみ）
```

**デフォルト**: `sync: false`（非同期実行）

**同期実行が必要な場合**:
- ワールド操作（ブロック変更など）
- エンティティ生成
- プレイヤーインベントリ操作

## カスタムエフェクトの実装

```kotlin
// 1. Effectインターフェースを実装
class CustomEffect(
    private val customParam: String
) : Effect {
    override suspend fun apply(source: Entity, target: Entity) {
        // カスタムロジック
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        // PacketEntity用の実装
    }
}

// 2. EffectFactoryに登録
EffectFactory.registerEffectType("custom") { params ->
    CustomEffect(
        customParam = params["customParam"] as String
    )
}
```

## パフォーマンス最適化

### エフェクト遅延

```yaml
Skills:
  - "projectile{...} @direction ~onTimer:30t{effectDelay=100ms}"
```

エフェクト適用前に遅延を入れることで、アニメーションとの同期やタイミング調整が可能。

### バッチ処理

複数のターゲットに同じエフェクトを適用する場合、内部的にバッチ処理されます。

```kotlin
targets.forEach { target ->
    effects.forEach { effect ->
        effect.apply(source, target)
    }
}
```

## トラブルシューティング

### エフェクトが適用されない

**原因**:
- ターゲットが見つからない
- エフェクトパラメータが不正
- 権限不足（CommandEffect）

**解決方法**:
```yaml
debug:
  enabled: true
  verbose: true
```

### パーティクルが表示されない

**原因**:
- クライアント設定でパーティクルが無効
- パーティクル数が多すぎて描画されない

**解決方法**:
```yaml
effects:
  - type: particle
    count: 1  # 数を減らす
```

---

**関連ドキュメント**:
- [Skill System](../skill-system/skill-executor.md) - スキルシステム
- [Targeter System](../targeter-system/targeter-overview.md) - ターゲッターシステム
