# Unique プラグイン 完全リファレンス

このドキュメントでは、Uniqueプラグインの全機能、全パラメータを網羅的に解説します。

## 目次

- [スキル一覧](#スキル一覧)
- [エフェクト一覧](#エフェクト一覧)
- [ターゲッター一覧](#ターゲッター一覧)
- [CEL式リファレンス](#cel式リファレンス)
- [全パラメータ表](#全パラメータ表)

---

## スキル一覧

### BasicSkill

最も基本的なスキル。即座にエフェクトを適用します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "basic" | - | スキルタイプ |
| `effects` | List&lt;Effect&gt; | [] | - | 適用するエフェクトのリスト |
| `targeter` | Targeter | - | - | ターゲット選択 |
| `condition` | String | null | ✅ | 実行条件（CEL式） |
| `meta.sync` | Boolean | false | - | 同期実行するか |

**YAML例:**
```yaml
skills:
  - skill: basic_attack
    type: basic
    effects:
      - type: damage
        damageAmount: 10.0
      - type: particle
        particle: CRIT
```

---

### ProjectileSkill

発射体を発射し、命中時にエフェクトを適用します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "projectile" | - | スキルタイプ |
| `projectileType` | String | "ARROW" | - | 発射体の種類 |
| `speed` | String | "1.0" | ✅ | 発射速度 |
| `gravity` | Boolean | false | - | 重力の影響を受けるか |
| `maxDistance` | String | "30.0" | ✅ | 最大飛距離 |
| `hitRadius` | String | "1.0" | ✅ | 命中判定半径 |
| `pierce` | Boolean | false | - | 貫通するか |
| `homing` | String | "0.0" | ✅ | ホーミング強度（0.0-1.0） |
| `particle` | String | null | - | 軌道パーティクル |
| `particleDensity` | Double | 0.5 | - | パーティクル密度 |
| `fireSound` | String | null | - | 発射音 |
| `hitSound` | String | null | - | 命中音 |
| `hitEffects` | List&lt;Effect&gt; | [] | - | 命中時のエフェクト |

**発射体タイプ:**
- `ARROW`: 矢
- `FIREBALL`: ファイアボール
- `SMALL_FIREBALL`: 小さいファイアボール
- `WITHER_SKULL`: ウィザースカル
- `SNOWBALL`: 雪玉
- `EGG`: 卵
- `ENDER_PEARL`: エンダーパール

**YAML例:**
```yaml
skills:
  - skill: fireball
    type: projectile
    projectileType: FIREBALL
    speed: '2.0'
    gravity: false
    maxDistance: '40.0'
    hitRadius: '1.5'
    pierce: false
    homing: '0.1'
    particle: FLAME
    particleDensity: 0.8
    fireSound: entity_blaze_shoot
    hitSound: entity_generic_explode
    hitEffects:
      - type: damage
        damageAmount: 15.0
      - type: explosion
        explosionRadius: '3.0'
```

---

### MetaSkill

複数のフェーズを順次実行するスキル。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "meta" | - | スキルタイプ |
| `phases` | List&lt;Phase&gt; | [] | - | 実行フェーズのリスト |

**Phase パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `delay` | Long | 0 | - | 遅延時間（ms） |
| `skills` | List&lt;Skill&gt; | [] | - | 実行するスキル |

**YAML例:**
```yaml
skills:
  - skill: multi_phase
    type: meta
    phases:
      - delay: 0
        skills:
          - skill: phase1
            type: basic
            effects:
              - type: message
                message: 'フェーズ1'
      - delay: 1000
        skills:
          - skill: phase2
            type: basic
            effects:
              - type: damage
                damageAmount: 10.0
```

---

### BranchSkill

条件に応じて異なるスキルを実行します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "branch" | - | スキルタイプ |
| `branches` | List&lt;Branch&gt; | [] | - | 分岐のリスト |

**Branch パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `condition` | String | null | ✅ | 実行条件（CEL式） |
| `skills` | List&lt;Skill&gt; | [] | - | 実行するスキル |

**YAML例:**
```yaml
skills:
  - skill: conditional_attack
    type: branch
    branches:
      - condition: 'entity.health < entity.maxHealth * 0.3'
        skills:
          - skill: desperate_attack
            type: basic
            effects:
              - type: damage
                damageAmount: 30.0
      - condition: 'nearbyPlayers.count > 5'
        skills:
          - skill: aoe_attack
            type: basic
            effects:
              - type: explosion
                explosionRadius: '5.0'
      - condition: 'true'  # デフォルト
        skills:
          - skill: normal_attack
            type: basic
            effects:
              - type: damage
                damageAmount: 10.0
```

---

### BeamSkill

直線状にレーザービームを発射するスキル。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "beam" | - | スキルタイプ |
| `beamRange` | String | "20.0" | ✅ | ビームの射程距離 |
| `beamWidth` | String | "0.5" | ✅ | ビームの幅（命中判定半径） |
| `beamParticle` | String | "FLAME" | - | ビームのパーティクル |
| `beamParticleDensity` | Double | 0.3 | - | パーティクルの密度 |
| `beamDuration` | String | "1000" | ✅ | ビーム持続時間（ms） |
| `beamTickInterval` | String | "50" | ✅ | ビーム更新間隔（ms） |
| `beamPiercing` | Boolean | true | - | 貫通するか |
| `beamFireSound` | String | null | - | 発射時のサウンド |
| `hitSound` | String | null | - | 命中時のサウンド |
| `effects` | List&lt;Effect&gt; | [] | - | 命中時のエフェクト |

**YAML例:**
```yaml
skills:
  - skill: laser_beam
    type: beam
    beamRange: '25.0'
    beamWidth: '0.8'
    beamParticle: FLAME
    beamParticleDensity: 0.5
    beamDuration: '2000'
    beamTickInterval: '50'
    beamPiercing: true
    beamFireSound: block_beacon_activate
    hitSound: entity_generic_burn
    effects:
      - type: damage
        damageAmount: 15.0
      - type: blind
        blindDuration: '3000'
        blindAmplifier: 1
```

---

### AuraSkill

持続的な範囲効果を生成するスキル。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "aura" | - | スキルタイプ |
| `auraRadius` | String | "5.0" | ✅ | オーラの半径 |
| `auraDuration` | String | "10000" | ✅ | オーラの持続時間（ms） |
| `auraTickInterval` | String | "1000" | ✅ | エフェクト適用間隔（ms） |
| `auraParticle` | String | "ENCHANT" | - | オーラのパーティクル |
| `auraParticleCount` | Int | 10 | - | パーティクルの数 |
| `auraParticleSpeed` | Double | 0.1 | - | パーティクルの速度 |
| `auraSelfAffect` | Boolean | false | - | 自分にも効果を適用するか |
| `auraMaxTargets` | Int | 0 | - | 最大ターゲット数（0=無制限） |
| `auraStartSound` | String | null | - | 開始時のサウンド |
| `auraTickSound` | String | null | - | 効果適用時のサウンド |
| `auraEndSound` | String | null | - | 終了時のサウンド |
| `effects` | List&lt;Effect&gt; | [] | - | オーラ内に適用するエフェクト |

**YAML例:**
```yaml
skills:
  - skill: healing_aura
    type: aura
    auraRadius: '8.0'
    auraDuration: '60000'
    auraTickInterval: '2000'
    auraParticle: HEART
    auraParticleCount: 15
    auraParticleSpeed: 0.2
    auraSelfAffect: true
    auraMaxTargets: 5
    auraStartSound: block_beacon_activate
    auraTickSound: entity_player_levelup
    auraEndSound: block_beacon_deactivate
    effects:
      - type: heal
        healAmount: 3.0
      - type: potioneffect
        potionType: regeneration
        potionDuration: '3000'
        potionAmplifier: 0
```

---

### SummonSkill

他のMobを召喚するスキル。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "summon" | - | スキルタイプ |
| `summonMobId` | String | - | - | 召喚するMob ID |
| `summonCount` | String | "1" | ✅ | 召喚数 |
| `summonRadius` | String | "3.0" | ✅ | 召喚範囲半径 |

**YAML例:**
```yaml
skills:
  - skill: summon_minions
    type: summon
    summonMobId: WeakZombie
    summonCount: '3'
    summonRadius: '5.0'
```

---

## エフェクト一覧

### DamageEffect

ダメージを与えるエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "damage" | - | エフェクトタイプ |
| `damageAmount` | String | "0.0" | ✅ | ダメージ量 |

**YAML例:**
```yaml
effects:
  - type: damage
    damageAmount: 15.0
  - type: damage
    damageAmount: '10 + (entity.level * 2)'  # CEL式
```

---

### HealEffect

体力を回復するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "heal" | - | エフェクトタイプ |
| `healAmount` | String | "0.0" | ✅ | 回復量 |

**YAML例:**
```yaml
effects:
  - type: heal
    healAmount: 10.0
```

---

### PotionEffectEffect

ポーション効果を付与するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "potioneffect" | - | エフェクトタイプ |
| `potionType` | String | - | - | ポーション効果タイプ |
| `potionDuration` | String | "1000" | ✅ | 持続時間（ms） |
| `potionAmplifier` | Int | 0 | - | 効果レベル（0-255） |

**ポーション効果タイプ:**
`speed`, `slowness`, `haste`, `mining_fatigue`, `strength`, `instant_health`, `instant_damage`, `jump_boost`, `nausea`, `regeneration`, `resistance`, `fire_resistance`, `water_breathing`, `invisibility`, `blindness`, `night_vision`, `hunger`, `weakness`, `poison`, `wither`, `health_boost`, `absorption`, `saturation`, `glowing`, `levitation`, `luck`, `unluck`, `slow_falling`, `conduit_power`, `dolphins_grace`, `bad_omen`, `hero_of_the_village`

**YAML例:**
```yaml
effects:
  - type: potioneffect
    potionType: strength
    potionDuration: '5000'
    potionAmplifier: 2
```

---

### ParticleEffect

パーティクルを表示するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "particle" | - | エフェクトタイプ |
| `particle` | String | "FLAME" | - | パーティクルタイプ |
| `particleCount` | Int | 10 | - | パーティクル数 |
| `particleSpeed` | Double | 0.1 | - | パーティクル速度 |
| `particleOffsetX` | Double | 0.5 | - | X軸オフセット |
| `particleOffsetY` | Double | 0.5 | - | Y軸オフセット |
| `particleOffsetZ` | Double | 0.5 | - | Z軸オフセット |

**主なパーティクルタイプ:**
`FLAME`, `SMOKE`, `EXPLOSION`, `CRIT`, `HEART`, `ENCHANT`, `PORTAL`, `DRAGON_BREATH`, `END_ROD`, `SWEEP_ATTACK`, `CLOUD`, `REDSTONE`, `SNOWFLAKE`, `BUBBLE`, `DRIP_WATER`, `DRIP_LAVA`, `VILLAGER_ANGRY`, `VILLAGER_HAPPY`, `NOTE`

**YAML例:**
```yaml
effects:
  - type: particle
    particle: CRIT
    particleCount: 30
    particleSpeed: 0.2
```

---

### SoundEffect

サウンドを再生するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "sound" | - | エフェクトタイプ |
| `sound` | String | - | - | サウンド名 |
| `soundVolume` | Float | 1.0 | - | 音量 |
| `soundPitch` | Float | 1.0 | - | ピッチ |

**主なサウンド:**
`entity_player_attack_crit`, `entity_generic_explode`, `entity_blaze_shoot`, `entity_wither_shoot`, `block_beacon_activate`, `entity_player_levelup`, `entity_lightning_bolt_thunder`, `entity_ender_dragon_growl`

**YAML例:**
```yaml
effects:
  - type: sound
    sound: entity_player_attack_crit
    soundVolume: 1.0
    soundPitch: 1.2
```

---

### MessageEffect

メッセージを送信するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "message" | - | エフェクトタイプ |
| `message` | String | "" | - | 送信するメッセージ |

**YAML例:**
```yaml
effects:
  - type: message
    message: '&c強力な攻撃を受けた！'
```

---

### CommandEffect

コマンドを実行するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "command" | - | エフェクトタイプ |
| `command` | String | "" | - | 実行するコマンド |
| `commandAsConsole` | Boolean | false | - | コンソールから実行するか |

**プレースホルダー:**
- `{player}`: プレイヤー名
- `{x}`, `{y}`, `{z}`: 座標
- `{world}`: ワールド名

**YAML例:**
```yaml
effects:
  - type: command
    command: 'give {player} diamond 1'
    commandAsConsole: true
```

---

### VelocityEffect

速度を設定するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "velocity" | - | エフェクトタイプ |
| `velocityX` | String | "0.0" | ✅ | X方向の速度 |
| `velocityY` | String | "0.0" | ✅ | Y方向の速度 |
| `velocityZ` | String | "0.0" | ✅ | Z方向の速度 |

**YAML例:**
```yaml
effects:
  - type: velocity
    velocityX: '0.0'
    velocityY: '2.0'
    velocityZ: '0.0'
```

---

### LightningEffect

雷を落とすエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "lightning" | - | エフェクトタイプ |
| `lightningDamage` | String | "0.0" | ✅ | ダメージ量 |
| `lightningSetFire` | Boolean | false | - | 着火するか |
| `lightningVisualOnly` | Boolean | false | - | 見た目のみか |

**YAML例:**
```yaml
effects:
  - type: lightning
    lightningDamage: 20.0
    lightningSetFire: true
    lightningVisualOnly: false
```

---

### ExplosionEffect

爆発を発生させるエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "explosion" | - | エフェクトタイプ |
| `explosionDamage` | String | "0.0" | ✅ | ダメージ量 |
| `explosionRadius` | String | "3.0" | ✅ | 爆発半径 |
| `explosionKnockback` | String | "1.0" | ✅ | ノックバック強度 |
| `explosionSetFire` | Boolean | false | - | 着火するか |
| `explosionBreakBlocks` | Boolean | false | - | ブロックを破壊するか |

**YAML例:**
```yaml
effects:
  - type: explosion
    explosionDamage: 15.0
    explosionRadius: '4.0'
    explosionKnockback: '1.5'
    explosionSetFire: true
    explosionBreakBlocks: false
```

---

### FreezeEffect

エンティティを凍結させるエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "freeze" | - | エフェクトタイプ |
| `freezeDuration` | String | "3000" | ✅ | 凍結時間（ms） |
| `freezeTicks` | Int | 140 | - | 凍結Tick数 |

**YAML例:**
```yaml
effects:
  - type: freeze
    freezeDuration: '5000'
    freezeTicks: 280
```

---

### ShieldEffect

シールドを付与するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "shield" | - | エフェクトタイプ |
| `shieldAmount` | String | "10.0" | ✅ | シールド量 |
| `shieldDuration` | String | "5000" | ✅ | 持続時間（ms） |

**YAML例:**
```yaml
effects:
  - type: shield
    shieldAmount: 20.0
    shieldDuration: '10000'
```

---

### TeleportEffect

テレポートさせるエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "teleport" | - | エフェクトタイプ |
| `teleportMode` | String | "TO_SOURCE" | - | テレポートモード |
| `teleportRange` | String | "5.0" | ✅ | ランダム範囲（RANDOMモード用） |

**テレポートモード:**
- `TO_SOURCE`: ソースの位置へテレポート
- `TO_TARGET`: ターゲットの位置へテレポート
- `RANDOM`: ランダムな位置へテレポート

**YAML例:**
```yaml
effects:
  - type: teleport
    teleportMode: TO_SOURCE
  - type: teleport
    teleportMode: RANDOM
    teleportRange: '10.0'
```

---

### PullEffect

引き寄せるエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "pull" | - | エフェクトタイプ |
| `pullStrength` | String | "1.0" | ✅ | 引き寄せ強度 |

**YAML例:**
```yaml
effects:
  - type: pull
    pullStrength: '2.0'
  - type: pull
    pullStrength: '1.0 + (target.level * 0.1)'  # CEL式
```

---

### PushEffect

押し出すエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "push" | - | エフェクトタイプ |
| `pushStrength` | String | "1.5" | ✅ | 押し出し強度 |

**YAML例:**
```yaml
effects:
  - type: push
    pushStrength: '3.0'
```

---

### BlindEffect

盲目効果を付与するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "blind" | - | エフェクトタイプ |
| `blindDuration` | String | "3000" | ✅ | 持続時間（ms） |
| `blindAmplifier` | Int | 0 | - | 効果レベル（0-255） |

**YAML例:**
```yaml
effects:
  - type: blind
    blindDuration: '5000'
    blindAmplifier: 2
```

---

### SetBlockEffect

ブロックを設置/破壊するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "setblock" | - | エフェクトタイプ |
| `blockType` | String | "AIR" | - | ブロックタイプ |
| `blockRadius` | String | "0.0" | ✅ | 設置範囲半径 |
| `blockTemporary` | Boolean | false | - | 一時的な設置か |
| `blockTemporaryDuration` | String | "5000" | ✅ | 一時設置の持続時間（ms） |

**YAML例:**
```yaml
effects:
  - type: setblock
    blockType: STONE
    blockRadius: '0'
  - type: setblock
    blockType: BARRIER
    blockRadius: '2.0'
    blockTemporary: true
    blockTemporaryDuration: '10000'
```

---

## ターゲッター一覧

### SelfTargeter

自分自身をターゲットにします。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "self" | - | ターゲッタータイプ |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: self
```

---

### NearestPlayerTargeter

最も近いプレイヤーをターゲットにします。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "nearestplayer" | - | ターゲッタータイプ |
| `range` | String | "10.0" | ✅ | 検索範囲 |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: nearestplayer
  range: 15.0
  filter: 'target.health < target.maxHealth * 0.5'
```

---

### RadiusPlayersTargeter

範囲内の全プレイヤーをターゲットにします。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "radiusplayers" | - | ターゲッタータイプ |
| `range` | String | "10.0" | ✅ | 検索範囲 |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: radiusplayers
  range: 20.0
```

---

### RadiusEntitiesTargeter

範囲内の全エンティティをターゲットにします。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "radiusentities" | - | ターゲッタータイプ |
| `range` | String | "10.0" | ✅ | 検索範囲 |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: radiusentities
  range: 15.0
  filter: 'target.type == "ZOMBIE"'
```

---

### LowestHealthTargeter

最も体力が低いターゲットを選択します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "lowesthealth" | - | ターゲッタータイプ |
| `baseTargeter` | Targeter | - | - | ベースとなるターゲッター |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: lowesthealth
  baseTargeter:
    type: radiusplayers
    range: 20.0
```

---

### RandomTargeter

ランダムにターゲットを選択します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "random" | - | ターゲッタータイプ |
| `count` | Int | 1 | - | 選択数 |
| `baseTargeter` | Targeter | - | - | ベースとなるターゲッター |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: random
  count: 3
  baseTargeter:
    type: radiusplayers
    range: 25.0
```

---

### AreaTargeter

範囲内のターゲットを選択します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "area" | - | ターゲッタータイプ |
| `areaRange` | String | "5.0" | ✅ | 範囲 |
| `baseTargeter` | Targeter | - | - | ベースとなるターゲッター |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: area
  areaRange: 10.0
  baseTargeter:
    type: nearestplayer
    range: 30.0
```

---

### ChainTargeter

連鎖的にターゲットを選択します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "chain" | - | ターゲッタータイプ |
| `chainCount` | Int | 3 | - | 連鎖数 |
| `chainRange` | String | "5.0" | ✅ | 連鎖範囲 |
| `baseTargeter` | Targeter | - | - | ベースとなるターゲッター |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: chain
  chainCount: 5
  chainRange: 8.0
  baseTargeter:
    type: nearestplayer
    range: 20.0
```

---

### NearestTargeter

最も近いターゲットを選択します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "nearest" | - | ターゲッタータイプ |
| `baseTargeter` | Targeter | - | - | ベースとなるターゲッター |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: nearest
  baseTargeter:
    type: radiusplayers
    range: 15.0
```

---

### FarthestTargeter

最も遠いターゲットを選択します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "farthest" | - | ターゲッタータイプ |
| `baseTargeter` | Targeter | - | - | ベースとなるターゲッター |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: farthest
  baseTargeter:
    type: radiusplayers
    range: 30.0
```

---

### ThreatTargeter

脅威度に基づいてターゲットを選択します。

**パラメータ:**

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | "threat" | - | ターゲッタータイプ |
| `count` | Int | 1 | - | 選択する上位数 |
| `baseTargeter` | Targeter | - | - | ベースとなるターゲッター |
| `filter` | String | null | ✅ | フィルター条件（CEL式） |

**YAML例:**
```yaml
targeter:
  type: threat
  count: 3
  baseTargeter:
    type: radiusplayers
    range: 25.0
```

---

## CEL式リファレンス

### 利用可能な変数

#### エンティティ変数

| 変数 | 型 | 説明 |
|------|-----|------|
| `entity.health` | Double | エンティティの体力 |
| `entity.maxHealth` | Double | エンティティの最大体力 |
| `entity.level` | Int | エンティティのレベル |
| `entity.type` | String | エンティティタイプ |
| `entity.name` | String | エンティティ名 |
| `entity.world` | String | ワールド名 |
| `entity.x` | Double | X座標 |
| `entity.y` | Double | Y座標 |
| `entity.z` | Double | Z座標 |

#### ターゲット変数

| 変数 | 型 | 説明 |
|------|-----|------|
| `target.health` | Double | ターゲットの体力 |
| `target.maxHealth` | Double | ターゲットの最大体力 |
| `target.level` | Int | ターゲットのレベル |
| `target.type` | String | ターゲットタイプ |
| `target.distance` | Double | ターゲットとの距離 |
| `target.x` | Double | X座標 |
| `target.y` | Double | Y座標 |
| `target.z` | Double | Z座標 |

#### ワールド変数

| 変数 | 型 | 説明 |
|------|-----|------|
| `world.time` | Long | ワールド時刻（0-24000） |
| `world.name` | String | ワールド名 |
| `world.difficulty` | String | 難易度 |
| `world.isThundering` | Boolean | 雷雨かどうか |

#### プレイヤー変数

| 変数 | 型 | 説明 |
|------|-----|------|
| `nearbyPlayers.count` | Int | 周囲のプレイヤー数 |
| `nearbyPlayers.list` | List | 周囲のプレイヤーリスト |

#### ダメージ変数（onDamagedトリガー用）

| 変数 | 型 | 説明 |
|------|-----|------|
| `damage.amount` | Double | ダメージ量 |
| `damage.cause` | String | ダメージ原因 |
| `damage.isCritical` | Boolean | クリティカルかどうか |

### 演算子

#### 算術演算子

| 演算子 | 説明 | 例 |
|--------|------|-----|
| `+` | 加算 | `10 + 5` → 15 |
| `-` | 減算 | `10 - 5` → 5 |
| `*` | 乗算 | `10 * 5` → 50 |
| `/` | 除算 | `10 / 5` → 2 |
| `%` | 剰余 | `10 % 3` → 1 |

#### 比較演算子

| 演算子 | 説明 | 例 |
|--------|------|-----|
| `==` | 等しい | `entity.health == 100` |
| `!=` | 等しくない | `entity.health != 100` |
| `<` | 小なり | `entity.health < 50` |
| `<=` | 小なりイコール | `entity.health <= 50` |
| `>` | 大なり | `entity.health > 50` |
| `>=` | 大なりイコール | `entity.health >= 50` |

#### 論理演算子

| 演算子 | 説明 | 例 |
|--------|------|-----|
| `&&` | AND | `entity.health < 50 && nearbyPlayers.count > 3` |
| `\|\|` | OR | `entity.health < 50 \|\| target.distance < 5` |
| `!` | NOT | `!world.isThundering` |

#### 三項演算子

| 演算子 | 説明 | 例 |
|--------|------|-----|
| `?:` | 条件演算子 | `entity.health < 50 ? 20 : 10` |

### 関数

#### 数学関数

| 関数 | 説明 | 例 |
|------|------|-----|
| `min(a, b)` | 最小値 | `min(entity.health, 100)` |
| `max(a, b)` | 最大値 | `max(entity.health, 10)` |
| `abs(a)` | 絶対値 | `abs(-5)` → 5 |
| `floor(a)` | 切り捨て | `floor(4.7)` → 4 |
| `ceil(a)` | 切り上げ | `ceil(4.2)` → 5 |
| `round(a)` | 四捨五入 | `round(4.5)` → 5 |

#### 文字列関数

| 関数 | 説明 | 例 |
|------|------|-----|
| `contains(str, substr)` | 部分文字列を含むか | `contains(entity.name, "Boss")` |
| `startsWith(str, prefix)` | 接頭辞で始まるか | `startsWith(entity.name, "Elite")` |
| `endsWith(str, suffix)` | 接尾辞で終わるか | `endsWith(world.name, "_nether")` |

### 使用例

#### 基本的な条件式

```yaml
# 体力が50%未満の場合
condition: 'entity.health < entity.maxHealth * 0.5'

# 夜間の場合（13000-24000）
condition: 'world.time > 13000'

# プレイヤーが5人以上いる場合
condition: 'nearbyPlayers.count >= 5'

# ターゲットとの距離が10未満の場合
condition: 'target.distance < 10'
```

#### 複雑な条件式

```yaml
# 体力が30%未満かつプレイヤーが3人以上
condition: 'entity.health < entity.maxHealth * 0.3 && nearbyPlayers.count >= 3'

# 昼間またはプレイヤーが1人以下
condition: 'world.time < 13000 || nearbyPlayers.count <= 1'

# ボス名を含むかつ体力が50%以上
condition: 'contains(entity.name, "Boss") && entity.health >= entity.maxHealth * 0.5'
```

#### 動的な値の計算

```yaml
# プレイヤー数に応じたダメージ
damageAmount: '10 + (nearbyPlayers.count * 2)'

# 体力割合に応じた回復量
healAmount: '(entity.maxHealth - entity.health) * 0.5'

# 距離に応じた速度
speed: 'target.distance * 0.1'

# 時間帯による強度変化
strength: 'world.time > 13000 ? 2.0 : 1.0'
```

---

## 全パラメータ表

### Mob定義の基本パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `type` | String | ZOMBIE | - | エンティティタイプ |
| `display` | String | - | - | 表示名 |
| `health` | String | "20" | ✅ | 体力 |
| `damage` | String | "1" | ✅ | 攻撃力 |
| `armor` | String | "0" | ✅ | 防御力 |
| `armorToughness` | String | "0" | ✅ | 防具強度 |

### AI設定

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `ai.movementSpeed` | Double | 0.25 | - | 移動速度 |
| `ai.followRange` | Double | 16.0 | - | 追跡範囲 |
| `ai.knockbackResistance` | Double | 0.0 | - | ノックバック耐性 |

### 見た目設定

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `appearance.glowing` | Boolean | false | - | 発光するか |
| `appearance.customNameVisible` | Boolean | true | - | 名前を表示するか |
| `appearance.baby` | Boolean | false | - | 子供サイズか |

### スキルトリガー

| トリガー | 説明 |
|---------|------|
| `onSpawn` | スポーン時 |
| `onTimer` | タイマー（定期実行） |
| `onAttack` | 攻撃時 |
| `onDamaged` | ダメージを受けた時 |
| `onDeath` | 死亡時 |

### スキル共通パラメータ

| パラメータ | 型 | デフォルト | CEL対応 | 説明 |
|-----------|-----|----------|---------|------|
| `name` | String | - | - | スキル名 |
| `interval` | Long | - | - | 実行間隔（tick） |
| `condition` | String | null | ✅ | 実行条件 |
| `targeter` | Targeter | - | - | ターゲッター |
| `skills` | List&lt;Skill&gt; | [] | - | 実行するスキル |
| `meta.sync` | Boolean | false | - | 同期実行するか |

---

## パフォーマンスに関する注意

### 重い処理

以下の機能は計算負荷が高いため、適切な間隔を設定してください：

1. **BeamSkill**: レイキャスト計算が重い
   - 推奨間隔: 100tick以上（5秒以上）
   - `beamTickInterval`: 50ms以上

2. **AuraSkill**: 範囲内のエンティティを定期的にチェック
   - 推奨間隔: 200tick以上（10秒以上）
   - `auraTickInterval`: 1000ms以上（1秒以上）

3. **RadiusEntitiesTargeter**: 全エンティティを検索
   - 範囲を必要最小限に設定
   - フィルターを活用

4. **SetBlockEffect**: ブロック操作は同期処理が必要
   - `meta.sync: true` を推奨
   - `blockRadius` を大きくしすぎない

### 推奨設定

```yaml
# BeamSkillの推奨設定
skills:
  - skill: beam
    type: beam
    beamDuration: '1000'  # 1秒以内
    beamTickInterval: '50'  # 50ms以上

# AuraSkillの推奨設定
skills:
  - skill: aura
    type: aura
    auraDuration: '30000'  # 30秒以内
    auraTickInterval: '1000'  # 1秒以上
    auraMaxTargets: 10  # 最大ターゲット数を制限
```

---

## 変更履歴

- **Phase 2 (2025-10-06)**: BeamSkill, AuraSkill, 5つの新エフェクト, 3つの新ターゲッター追加
- **Phase 1**: Factory/Parserシステム、基本スキル・エフェクト・ターゲッター実装
