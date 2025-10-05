# Effect一覧リファレンス

Uniqueで利用可能な全Effectの詳細リファレンスです。

---

## 📋 Effect一覧

| Effect | 説明 | CEL対応 |
|--------|-----|---------|
| **Damage** | ダメージを与える | ✅ amount |
| **Heal** | HPを回復する | ✅ amount |
| **PotionEffect** | ポーション効果を付与 | ✅ duration, amplifier |
| **Knockback** | ノックバックを与える | ❌ |
| **Teleport** | テレポートする | ❌ |
| **Particle** | パーティクルを表示 | ❌ |
| **Sound** | サウンドを再生 | ❌ |
| **Message** | メッセージを送信 | ❌ |
| **Command** | コマンドを実行 | ❌ |
| **Ignite** | 着火する | ❌ |
| **Velocity** | 速度を変更 | ❌ |

---

## 💥 DamageEffect

ターゲットにダメージを与えます。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Damage" |
| `amount` | String | "0" | ✅ | ダメージ量 |

### 基本例

```yaml
BasicDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "20"  # 固定20ダメージ
```

### CEL動的例

```yaml
# ターゲットのHPに比例
PercentDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "target.maxHealth * 0.3"  # 最大HPの30%

# 距離減衰ダメージ
DistanceDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "30 * math.max(0, 1 - distance.horizontal(source.location, target.location) / 20.0)"

# プレイヤー数でスケール
ScalingDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "10 + (nearbyPlayers.count * 5)"
```

### 利用可能なCEL変数

- `entity.*` - ソースエンティティ情報
- `target.*` - ターゲットエンティティ情報
- `source.*` - ソース位置情報
- `world.*` - ワールド情報
- `nearbyPlayers.*` - 周囲のプレイヤー情報
- `distance.*` - 距離計算関数
- `math.*` - 数学関数

---

## 💚 HealEffect

ターゲットのHPを回復します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Heal" |
| `amount` | String | "0" | ✅ | 回復量 |

### 基本例

```yaml
BasicHeal:
  type: Heal
  effects:
    - type: Heal
      amount: "20"  # 固定20回復
```

### CEL動的例

```yaml
# 失ったHP分を回復
FullRestore:
  type: Heal
  effects:
    - type: Heal
      amount: "entity.maxHealth - entity.health"

# HP比例回復
PercentHeal:
  type: Heal
  effects:
    - type: Heal
      amount: "entity.maxHealth * 0.5"  # 最大HPの50%回復

# プレイヤーレベルで回復量変化
LevelBasedHeal:
  type: Heal
  effects:
    - type: Heal
      amount: "10 + (target.level * 2)"
```

---

## 🧪 PotionEffectEffect

ポーション効果を付与します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "PotionEffect" |
| `effect` | String | - | ❌ | ポーション効果タイプ |
| `duration` | String | "200" | ✅ | 持続時間（tick） |
| `amplifier` | String | "0" | ✅ | 効果レベル（0=レベル1） |

### 利用可能なポーション効果

- SPEED, SLOWNESS, HASTE, MINING_FATIGUE
- STRENGTH, INSTANT_HEALTH, INSTANT_DAMAGE, JUMP_BOOST
- NAUSEA, REGENERATION, RESISTANCE, FIRE_RESISTANCE
- WATER_BREATHING, INVISIBILITY, BLINDNESS, NIGHT_VISION
- HUNGER, WEAKNESS, POISON, WITHER
- HEALTH_BOOST, ABSORPTION, SATURATION, GLOWING
- LEVITATION, LUCK, UNLUCK, SLOW_FALLING
- CONDUIT_POWER, DOLPHINS_GRACE, BAD_OMEN, HERO_OF_THE_VILLAGE

### 基本例

```yaml
Poison:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: POISON
      duration: "200"  # 10秒
      amplifier: "1"   # レベル2
```

### CEL動的例

```yaml
# HP比例のデバフ
HPBasedDebuff:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: WEAKNESS
      duration: "200"
      amplifier: "math.floor((1 - entity.health / entity.maxHealth) * 3)"  # 0-3

# 夜間のみ強化
NightBuff:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: STRENGTH
      duration: "world.isNight ? 200 : 0"  # 夜10秒、昼0秒
      amplifier: "world.isNight ? 2 : 0"

# プレイヤー数でデバフ強化
ScalingDebuff:
  type: PotionEffect
  effects:
    - type: PotionEffect
      effect: SLOWNESS
      duration: "100 + (nearbyPlayers.count * 20)"
      amplifier: "math.min(3, nearbyPlayers.count - 1)"
```

---

## 🌀 KnockbackEffect

ターゲットにノックバックを与えます。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Knockback" |
| `strength` | Double | 1.0 | ❌ | ノックバック強度 |

### 例

```yaml
StrongKnockback:
  type: Knockback
  effects:
    - type: Knockback
      strength: 3.0  # 強力なノックバック
```

---

## 🌟 TeleportEffect

ターゲットをテレポートします。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Teleport" |
| `x` | Double | 0.0 | ❌ | X座標 |
| `y` | Double | 0.0 | ❌ | Y座標 |
| `z` | Double | 0.0 | ❌ | Z座標 |

### 例

```yaml
TeleportToSpawn:
  type: Teleport
  effects:
    - type: Teleport
      x: 0.0
      y: 100.0
      z: 0.0
```

---

## ✨ ParticleEffect

パーティクルを表示します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Particle" |
| `particle` | String | - | ❌ | パーティクルタイプ |
| `count` | Int | 10 | ❌ | パーティクル数 |
| `offsetX` | Double | 0.5 | ❌ | X方向のオフセット |
| `offsetY` | Double | 0.5 | ❌ | Y方向のオフセット |
| `offsetZ` | Double | 0.5 | ❌ | Z方向のオフセット |

### 主なパーティクルタイプ

- **攻撃系**: CRIT, CRIT_MAGIC, SWEEP_ATTACK
- **炎系**: FLAME, SOUL_FIRE_FLAME, LAVA
- **爆発系**: EXPLOSION_NORMAL, EXPLOSION_LARGE, EXPLOSION_HUGE
- **魔法系**: ENCHANTMENT_TABLE, SPELL, PORTAL
- **エンダー系**: DRAGON_BREATH, END_ROD, REVERSE_PORTAL
- **環境系**: CLOUD, SMOKE_NORMAL, SMOKE_LARGE
- **その他**: HEART, NOTE, SLIME, TOTEM

### 例

```yaml
MagicEffect:
  type: Particle
  effects:
    - type: Particle
      particle: ENCHANTMENT_TABLE
      count: 50
      offsetX: 1.0
      offsetY: 1.0
      offsetZ: 1.0
```

---

## 🔊 SoundEffect

サウンドを再生します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Sound" |
| `sound` | String | - | ❌ | サウンド名 |
| `volume` | Float | 1.0 | ❌ | 音量 |
| `pitch` | Float | 1.0 | ❌ | ピッチ |

### 主なサウンド

- **攻撃**: ENTITY_PLAYER_ATTACK_STRONG, ENTITY_PLAYER_ATTACK_CRIT
- **爆発**: ENTITY_GENERIC_EXPLODE, ENTITY_DRAGON_FIREBALL_EXPLODE
- **魔法**: ENTITY_EVOKER_CAST_SPELL, ENTITY_ILLUSIONER_CAST_SPELL
- **テレポート**: ENTITY_ENDERMAN_TELEPORT, ENTITY_SHULKER_TELEPORT
- **ボス**: ENTITY_WITHER_SPAWN, ENTITY_ENDER_DRAGON_GROWL

### 例

```yaml
ExplosionSound:
  type: Sound
  effects:
    - type: Sound
      sound: ENTITY_GENERIC_EXPLODE
      volume: 2.0
      pitch: 0.8
```

---

## 💬 MessageEffect

プレイヤーにメッセージを送信します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Message" |
| `message` | String | - | ❌ | メッセージ内容 |

### 例

```yaml
WarningMessage:
  type: Message
  effects:
    - type: Message
      message: "&c&l[警告] &eボスが怒った！"
```

---

## ⌨️ CommandEffect

コマンドを実行します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Command" |
| `command` | String | - | ❌ | 実行するコマンド |
| `asOp` | Boolean | false | ❌ | OP権限で実行 |

### 例

```yaml
GiveReward:
  type: Command
  effects:
    - type: Command
      command: "give @p diamond 5"
      asOp: true
```

---

## 🔥 IgniteEffect

ターゲットに着火します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Ignite" |
| `duration` | String | "100" | ❌ | 着火時間（tick） |

### 例

```yaml
SetOnFire:
  type: Ignite
  effects:
    - type: Ignite
      duration: "100"  # 5秒
```

---

## 🚀 VelocityEffect

ターゲットの速度を変更します。

### パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|----------|---|----------|---------|-----|
| `type` | String | - | ❌ | "Velocity" |
| `velocityX` | Double | 0.0 | ❌ | X方向の速度 |
| `velocityY` | Double | 0.0 | ❌ | Y方向の速度 |
| `velocityZ` | Double | 0.0 | ❌ | Z方向の速度 |

### 例

```yaml
LaunchUpward:
  type: Velocity
  effects:
    - type: Velocity
      velocityX: 0.0
      velocityY: 2.0  # 上方向に打ち上げ
      velocityZ: 0.0
```

---

## 🎨 Effectの組み合わせ

複数のEffectを組み合わせて、より複雑な演出が可能です。

```yaml
ComplexAttack:
  type: Damage
  effects:
    # ダメージ
    - type: Damage
      amount: "25"

    # ノックバック
    - type: Knockback
      strength: 2.5

    # デバフ
    - type: PotionEffect
      effect: SLOWNESS
      duration: "100"
      amplifier: "2"

    # 着火
    - type: Ignite
      duration: "60"

    # パーティクル
    - type: Particle
      particle: FLAME
      count: 50

    # サウンド
    - type: Sound
      sound: ENTITY_BLAZE_SHOOT
      volume: 1.5

    # メッセージ
    - type: Message
      message: "&c炎の攻撃を受けた！"
```

---

## 📖 関連ドキュメント

- **[CEL動的機能ガイド](FEATURE_CEL_DYNAMIC.md)** - CEL式を使った動的パラメータ
- **[CELクイックスタート](CEL_QUICK_START.md)** - CEL式の基本
- **[Targeter一覧](REFERENCE_TARGETERS.md)** - ターゲットの選択方法
- **[スキルシステム入門](TUTORIAL_SKILLS.md)** - スキルの作り方

---

これで全Effectの使い方が理解できました！Effectを組み合わせて、独自のスキルを作成しましょう。
