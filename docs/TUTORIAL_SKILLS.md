# スキルシステム入門

このチュートリアルでは、Uniqueのスキルシステムを詳しく解説します。

---

## 📚 スキルシステムの概要

Uniqueのスキルシステムは、**トリガー**、**ターゲッター**、**エフェクト**の3つで構成されます。

```
トリガー（いつ） → ターゲッター（誰に） → エフェクト（何を）
```

### スキルトリガーの種類

| トリガー | 発動タイミング |
|---------|---------------|
| **OnTimer** | 一定間隔で発動 |
| **OnDamaged** | ダメージを受けた時 |
| **OnDeath** | 死亡時 |
| **OnSpawn** | スポーン時 |
| **OnAttack** | 攻撃時 |

---

## ⏰ OnTimer - タイマースキル

一定間隔で自動発動するスキルです。

### 基本例

```yaml
FireZombie:
  Type: ZOMBIE
  Display: '&6ファイアゾンビ'
  Health: 150

  Skills:
    OnTimer:
      - name: FireballAttack
        interval: 100  # 5秒ごと（20tick = 1秒）
        targeter:
          type: NearestPlayer
          range: 20
        skills:
          - skill: Fireball
```

### 複数のタイマースキル

```yaml
MultiSkillZombie:
  Type: ZOMBIE
  Display: '&eマルチスキルゾンビ'
  Health: 200

  Skills:
    OnTimer:
      # スキル1: 3秒ごとに炎
      - name: FireAttack
        interval: 60
        targeter:
          type: NearestPlayer
          range: 15
        skills:
          - skill: Fireball

      # スキル2: 10秒ごとに範囲攻撃
      - name: AreaAttack
        interval: 200
        targeter:
          type: RadiusPlayers
          range: 10.0
        skills:
          - skill: AreaDamage
```

### 条件付き発動

```yaml
ConditionalZombie:
  Type: ZOMBIE
  Display: '&c条件付きゾンビ'
  Health: 200

  Skills:
    OnTimer:
      # HP50%以下でのみ発動
      - name: DesperationAttack
        interval: 80
        condition: "entity.health < entity.maxHealth * 0.5"
        targeter:
          type: NearestPlayer
          range: 20
        skills:
          - skill: StrongFireball
```

---

## 🛡️ OnDamaged - ダメージ受信スキル

ダメージを受けた時に発動するスキルです。

### 基本例（カウンターアタック）

```yaml
CounterZombie:
  Type: ZOMBIE
  Display: '&9カウンターゾンビ'
  Health: 180

  Skills:
    OnDamaged:
      - name: Counter
        targeter:
          type: Attacker  # 攻撃者をターゲット
        skills:
          - skill: CounterDamage
```

**スキル定義**:
```yaml
CounterDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "15"
    - type: Knockback
      strength: 2.0
    - type: Particle
      particle: CRIT
      count: 30
```

### HP比例の反撃

```yaml
DefensiveZombie:
  Type: ZOMBIE
  Display: '&aディフェンシブゾンビ'
  Health: 250

  Skills:
    OnDamaged:
      # HP低いほど強い反撃
      - name: DesperationCounter
        condition: "entity.health < entity.maxHealth * 0.3"
        targeter:
          type: Attacker
        skills:
          - skill: StrongCounter
```

**スキル定義**:
```yaml
StrongCounter:
  type: Damage
  effects:
    - type: Damage
      amount: "30"  # 高ダメージ
    - type: PotionEffect
      effect: WEAKNESS
      duration: "200"  # 10秒
      amplifier: "2"
```

### ダメージを受けた時に回復

```yaml
RegenerativeZombie:
  Type: ZOMBIE
  Display: '&2リジェネゾンビ'
  Health: 200

  Skills:
    OnDamaged:
      # 20%の確率で回復
      - name: RegenerationChance
        condition: "random.chance(0.2)"
        targeter:
          type: Self
        skills:
          - skill: Regenerate
```

**スキル定義**:
```yaml
Regenerate:
  type: Heal
  effects:
    - type: Heal
      amount: "20"
    - type: Particle
      particle: HEART
      count: 10
```

---

## 💀 OnDeath - 死亡時スキル

死亡時に発動するスキルです。

### 基本例（爆発）

```yaml
ExplodingZombie:
  Type: ZOMBIE
  Display: '&c爆発ゾンビ'
  Health: 100

  Skills:
    OnDeath:
      - name: DeathExplosion
        targeter:
          type: RadiusPlayers
          range: 8.0
        skills:
          - skill: Explosion
```

**スキル定義**:
```yaml
Explosion:
  type: Damage
  effects:
    - type: Damage
      amount: "20"
    - type: Particle
      particle: EXPLOSION_HUGE
      count: 5
    - type: Sound
      sound: ENTITY_GENERIC_EXPLODE
      volume: 2.0
```

### 召喚スキル

```yaml
SummonerZombie:
  Type: ZOMBIE
  Display: '&5サモナーゾンビ'
  Health: 150

  Skills:
    OnDeath:
      # 死亡時に2体召喚
      - name: SummonMinions
        targeter:
          type: Self
        skills:
          - skill: SummonZombies
```

**スキル定義**:
```yaml
SummonZombies:
  type: Summon
  summon:
    entityType: ZOMBIE
    amount: 2
    radius: 3.0
```

### メッセージ表示

```yaml
MessengerZombie:
  Type: ZOMBIE
  Display: '&eメッセンジャーゾンビ'
  Health: 100

  Skills:
    OnDeath:
      - name: DeathMessage
        targeter:
          type: RadiusPlayers
          range: 50.0
        skills:
          - skill: BroadcastDeath
```

**スキル定義**:
```yaml
BroadcastDeath:
  type: Message
  effects:
    - type: Message
      message: "&c[!] &eメッセンジャーゾンビが倒されました！"
```

---

## 🐣 OnSpawn - スポーン時スキル

スポーン時に発動するスキルです。

### スポーン演出

```yaml
DramaticZombie:
  Type: ZOMBIE
  Display: '&dドラマチックゾンビ'
  Health: 200

  Skills:
    OnSpawn:
      - name: SpawnEffect
        targeter:
          type: Self
        skills:
          - skill: SpawnAnimation
```

**スキル定義**:
```yaml
SpawnAnimation:
  type: Particle
  effects:
    - type: Particle
      particle: PORTAL
      count: 100
    - type: Sound
      sound: ENTITY_ENDERMAN_TELEPORT
      volume: 2.0
```

### 周囲への警告

```yaml
WarningZombie:
  Type: WITHER_SKELETON
  Display: '&4&l危険なゾンビ'
  Health: 300

  Skills:
    OnSpawn:
      - name: SpawnWarning
        targeter:
          type: RadiusPlayers
          range: 50.0
        skills:
          - skill: WarningMessage
```

**スキル定義**:
```yaml
WarningMessage:
  type: Message
  effects:
    - type: Message
      message: "&c&l[警告] &e危険なボスがスポーンしました！"
    - type: Sound
      sound: ENTITY_WITHER_SPAWN
      volume: 2.0
```

---

## ⚔️ OnAttack - 攻撃時スキル

Mobが攻撃した時に発動するスキルです。

### 基本例（追加ダメージ）

```yaml
AssassinZombie:
  Type: ZOMBIE
  Display: '&8アサシンゾンビ'
  Health: 120

  Skills:
    OnAttack:
      - name: CriticalHit
        condition: "random.chance(0.3)"  # 30%の確率
        targeter:
          type: NearestPlayer
          range: 5
        skills:
          - skill: CriticalStrike
```

**スキル定義**:
```yaml
CriticalStrike:
  type: Damage
  effects:
    - type: Damage
      amount: "10"  # 追加ダメージ
    - type: Particle
      particle: CRIT_MAGIC
      count: 20
```

### デバフ攻撃

```yaml
PoisonZombie:
  Type: ZOMBIE
  Display: '&2ポイズンゾンビ'
  Health: 130

  Skills:
    OnAttack:
      - name: PoisonAttack
        targeter:
          type: NearestPlayer
          range: 5
        skills:
          - skill: ApplyPoison
```

**スキル定義**:
```yaml
ApplyPoison:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: POISON
      duration: "200"  # 10秒
      amplifier: "1"
    - type: Particle
      particle: SLIME
      count: 15
```

---

## 🎯 スキルメタ設定

スキルの実行タイミングを細かく制御できます。

### 遅延実行

```yaml
DelayedSkillZombie:
  Type: ZOMBIE
  Display: '&b遅延スキルゾンビ'
  Health: 150

  Skills:
    OnTimer:
      - name: DelayedAttack
        interval: 100
        targeter:
          type: NearestPlayer
          range: 20
        skills:
          - skill: Fireball
        meta:
          executeDelay: 1s    # 1秒後に実行
          effectDelay: 500ms  # さらに500ms後にエフェクト
          sync: true          # メインスレッドで実行
```

### 死亡時キャンセル

```yaml
CancellableZombie:
  Type: ZOMBIE
  Display: '&7キャンセル可能ゾンビ'
  Health: 100

  Skills:
    OnTimer:
      - name: ChargeAttack
        interval: 150
        targeter:
          type: NearestPlayer
          range: 30
        skills:
          - skill: ChargedFireball
        meta:
          executeDelay: 3s        # 3秒チャージ
          cancelOnDeath: true     # 死亡時にキャンセル
          interruptible: true     # 中断可能
```

---

## 💡 実践例: フェーズ変化ボス

HP段階によってスキルが変わるボスの例：

```yaml
PhaseChangeBoss:
  Type: ENDER_DRAGON
  Display: '&5&lフェーズチェンジボス'
  Health: "500"

  Skills:
    OnTimer:
      # フェーズ1（HP70%以上）: 弱攻撃
      - name: Phase1Attack
        interval: 100
        condition: "entity.health > entity.maxHealth * 0.7"
        targeter:
          type: NearestPlayer
          range: 30
        skills:
          - skill: WeakAttack

      # フェーズ2（HP30-70%）: 中攻撃
      - name: Phase2Attack
        interval: 80
        condition: "entity.health > entity.maxHealth * 0.3 && entity.health <= entity.maxHealth * 0.7"
        targeter:
          type: RadiusPlayers
          range: 20.0
        skills:
          - skill: MediumAttack

      # フェーズ3（HP30%以下）: 強攻撃
      - name: Phase3Attack
        interval: 60
        condition: "entity.health <= entity.maxHealth * 0.3"
        targeter:
          type: Area
          shape: CIRCLE
          radius: "25.0"
        skills:
          - skill: StrongAttack

    OnDamaged:
      # HP50%で全体回復
      - name: HalfHPHeal
        condition: "entity.health <= entity.maxHealth * 0.5 && entity.health > entity.maxHealth * 0.4"
        targeter:
          type: Self
        skills:
          - skill: EmergencyHeal
```

---

## 📖 次のステップ

- **[ターゲッターシステム入門](TUTORIAL_TARGETERS.md)** - 11種類のTargeterを使いこなす
- **[Effect一覧](REFERENCE_EFFECTS.md)** - 全Effectの詳細
- **[CEL動的機能ガイド](FEATURE_CEL_DYNAMIC.md)** - CEL式を使った動的スキル
- **[高度なボス設計](ADVANCED_BOSS_DESIGN.md)** - 複雑なボス戦の作り方

---

これでスキルシステムの基本が理解できました！さまざまなトリガーを組み合わせて、独自のMobを作成しましょう。
