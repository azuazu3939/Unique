# Skill Types - スキルタイプ一覧

## 概要

Uniqueは複数のスキルタイプを提供しています。各スキルタイプは特定の動作パターンを実装しており、組み合わせることで複雑な攻撃パターンを実現できます。

## スキルタイプ一覧

### 1. BasicSkill

最も基本的なスキル。ターゲットに直接エフェクトを適用します。

```yaml
Skills:
  - "basic{damage=10.0} @NearestPlayer{range=16.0} ~onTimer:20t"
```

**実装**:
```kotlin
class BasicSkill(
    override val id: String,
    override val meta: SkillMeta,
    private val effects: List<Effect>
) : Skill {
    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        val targets = targeter.getTargets(source)
        targets.forEach { target ->
            effects.forEach { effect ->
                effect.apply(source, target)
            }
        }
    }
}
```

### 2. ProjectileSkill

発射体を生成し、軌道計算と衝突判定を行います。

```yaml
Skills:
  - "projectile{speed=1.5, gravity=-0.03, range=32.0} @direction ~onAttack"
```

**パラメータ**:
- `speed`: 発射速度（ブロック/tick）
- `gravity`: 重力加速度（デフォルト: -0.03）
- `range`: 最大射程（ブロック）
- `hitboxRadius`: 衝突判定の半径（デフォルト: 0.5）
- `penetration`: 貫通数（デフォルト: 0）
- `particle`: 軌跡パーティクル
- `tickInterval`: 更新間隔（デフォルト: 1tick）

**実装の特徴**:
- レイキャストによる高精度な衝突判定
- 重力シミュレーション
- 貫通処理
- パーティクル軌跡

### 3. BeamSkill

ビーム（光線）を発射し、範囲内のすべてのターゲットにエフェクトを適用します。

```yaml
Skills:
  - "beam{range=32.0, width=1.0} @direction ~onTimer:40t"
```

**パラメータ**:
- `range`: ビームの長さ（ブロック）
- `width`: ビームの幅（ブロック）
- `tickInterval`: 更新間隔
- `duration`: 持続時間
- `particle`: ビームパーティクル
- `particleDensity`: パーティクル密度

**実装の特徴**:
- 矩形範囲判定
- 連続ダメージ
- 視覚的なビームエフェクト

### 4. AuraSkill

ソースの周囲にオーラを展開し、範囲内のターゲットに定期的にエフェクトを適用します。

```yaml
Skills:
  - "aura{radius=8.0, duration=10s, tickInterval=20t} @RadiusEntities ~onSpawn"
```

**パラメータ**:
- `radius`: オーラの半径（ブロック）
- `duration`: 持続時間
- `tickInterval`: エフェクト適用間隔
- `particle`: オーラパーティクル
- `followSource`: ソースに追従するか

**実装の特徴**:
- 持続的なエフェクト
- 範囲内の全ターゲットに適用
- パーティクルエフェクト

### 5. SummonSkill

エンティティを召喚します。PacketMobまたはバニラエンティティを生成できます。

```yaml
Skills:
  - "summon{mob=CustomMinion, count=3, spread=5.0} @self ~onTimer:100t"
```

**パラメータ**:
- `mob`: 召喚するMob名（PacketMobの定義名）
- `count`: 召喚数
- `spread`: 召喚範囲（ブロック）
- `duration`: 召喚時間（オプション、指定時間後に自動削除）
- `offset`: 召喚位置のオフセット

**実装の特徴**:
- PacketMobの召喚
- 複数体の同時召喚
- 召喚位置の分散配置
- 時限削除

## スキルの組み合わせ

### 連続スキル

```yaml
Skills:
  - "projectile{...} @direction ~onAttack{executeDelay=0ms}"
  - "aura{...} @self ~onAttack{executeDelay=500ms}"
  - "beam{...} @direction ~onAttack{executeDelay=1000ms}"
```

### 条件付きスキル

```yaml
Skills:
  - "summon{...} @self ~onDamaged{condition='health < maxHealth * 0.5'}"
```

### 複合エフェクト

```yaml
Skills:
  - "projectile{damage=20.0, knockback=1.5, particle=FLAME, sound=entity.blaze.shoot} @direction ~onTimer:30t"
```

## カスタムスキルの実装

新しいスキルタイプを追加する方法:

```kotlin
// 1. Skillインターフェースを実装
class CustomSkill(
    override val id: String,
    override val meta: SkillMeta,
    private val customParam: String
) : Skill {
    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        // カスタムロジック
    }

    override suspend fun execute(plugin: Plugin, source: PacketEntity, targeter: Targeter) {
        // PacketEntity用の実装
    }
}

// 2. SkillFactoryに登録
SkillFactory.registerSkillType("custom") { params ->
    CustomSkill(
        id = params["id"] as String,
        meta = params["meta"] as SkillMeta,
        customParam = params["customParam"] as String
    )
}
```

---

**関連ドキュメント**:
- [Skill Executor](skill-executor.md) - スキル実行エンジン
- [Effect System](../effect-system/effect-overview.md) - エフェクトシステム
- [Targeter System](../targeter-system/targeter-overview.md) - ターゲッターシステム
