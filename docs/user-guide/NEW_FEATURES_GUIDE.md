# 新機能ガイド (Phase 2)

このドキュメントでは、Phase 2で追加された新機能を説明します。

## 目次
- [新しいスキル](#新しいスキル)
  - [BeamSkill](#beamskill)
  - [AuraSkill](#auraskill)
- [新しいエフェクト](#新しいエフェクト)
  - [TeleportEffect](#teleporteffect)
  - [PullEffect](#pulleffect)
  - [PushEffect](#pusheffect)
  - [BlindEffect](#blindeffect)
  - [SetBlockEffect](#setblockeffect)
- [新しいターゲッター](#新しいターゲッター)
  - [NearestTargeter](#nearesttargeter)
  - [FarthestTargeter](#farthesttargeter)
  - [ThreatTargeter](#threattargeter)

---

## 新しいスキル

### BeamSkill

直線状にレーザービームを発射し、経路上の全エンティティにダメージやエフェクトを与えるスキル。

**特徴:**
- レイキャストによる正確な命中判定
- 経路上の全エンティティに継続的にダメージ
- カスタマイズ可能なパーティクルとサウンド
- 貫通/非貫通の選択可能

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `beamRange` | String (CEL) | "20.0" | ビームの射程距離 |
| `beamWidth` | String (CEL) | "0.5" | ビームの幅（命中判定半径） |
| `beamParticle` | String | FLAME | ビームのパーティクル |
| `beamParticleDensity` | Double | 0.3 | パーティクルの密度 |
| `beamDuration` | String | "1000" | ビーム持続時間（ms） |
| `beamTickInterval` | String | "50" | ビーム更新間隔（ms） |
| `beamPiercing` | Boolean | true | 貫通するか |
| `beamFireSound` | String | null | 発射時のサウンド |
| `hitSound` | String | null | 命中時のサウンド |
| `effects` | List | [] | 命中時のエフェクト |

**使用例:**
```yaml
skills:
  - skill: laser_beam
    type: BEAM

    beamRange: '25.0'
    beamWidth: '0.8'
    beamParticle: FLAME
    beamParticleDensity: 0.5
    beamDuration: '2000'  # 2秒間持続
    beamTickInterval: '50'
    beamPiercing: true

    effects:
      - type: damage
        damageAmount: 15.0
      - type: blind
        blindDuration: '3000'
        blindAmplifier: 1
```

---

### AuraSkill

ソースの周囲に持続的なオーラを生成し、範囲内のエンティティに継続的にエフェクトを適用するスキル。

**特徴:**
- 持続的な範囲効果
- カスタマイズ可能な更新間隔
- 自分への影響の有無を選択可能
- 最大ターゲット数の制限可能

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `auraRadius` | String (CEL) | "5.0" | オーラの半径 |
| `auraDuration` | String | "10000" | オーラの持続時間（ms） |
| `auraTickInterval` | String | "1000" | エフェクト適用間隔（ms） |
| `auraParticle` | String | ENCHANT | オーラのパーティクル |
| `auraParticleCount` | Int | 10 | パーティクルの数 |
| `auraParticleSpeed` | Double | 0.1 | パーティクルの速度 |
| `auraSelfAffect` | Boolean | false | 自分にも効果を適用するか |
| `auraMaxTargets` | Int | 0 | 最大ターゲット数（0=無制限） |
| `auraStartSound` | String | null | 開始時のサウンド |
| `auraTickSound` | String | null | 効果適用時のサウンド |
| `auraEndSound` | String | null | 終了時のサウンド |
| `effects` | List | [] | オーラ内に適用するエフェクト |

**使用例:**
```yaml
skills:
  - skill: healing_aura
    type: AURA

    auraRadius: '8.0'
    auraDuration: '60000'  # 60秒
    auraTickInterval: '2000'  # 2秒ごと
    auraParticle: HEART
    auraParticleCount: 15
    auraSelfAffect: true
    auraMaxTargets: 5

    effects:
      - type: heal
        healAmount: 3.0
      - type: potioneffect
        potionType: regeneration
        potionDuration: '3000'
        potionAmplifier: 0
```

---

## 新しいエフェクト

### TeleportEffect

ターゲットを指定位置またはソースの位置にテレポートさせるエフェクト。

**モード:**
- `TO_SOURCE`: ソースの位置へテレポート
- `TO_TARGET`: ターゲットの位置へテレポート（既にいる場所）
- `RANDOM`: ランダムな位置へテレポート

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `teleportMode` | String | "TO_SOURCE" | テレポートモード |
| `teleportRange` | String (CEL) | "5.0" | ランダム範囲（RANDOMモード用） |

**使用例:**
```yaml
effects:
  # プレイヤーをMobの位置に引き寄せる
  - type: teleport
    teleportMode: TO_SOURCE

  # ランダムな位置にテレポート
  - type: teleport
    teleportMode: RANDOM
    teleportRange: '10.0'
```

---

### PullEffect

ターゲットをソースの方向に引き寄せるエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `pullStrength` | String (CEL) | "1.0" | 引き寄せ強度 |

**使用例:**
```yaml
effects:
  - type: pull
    pullStrength: '2.0'
```

---

### PushEffect

ターゲットをソースから遠ざける方向に押し出すエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `pushStrength` | String (CEL) | "1.5" | 押し出し強度 |

**使用例:**
```yaml
effects:
  - type: push
    pushStrength: '3.0'
```

---

### BlindEffect

ターゲットに盲目効果を付与するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `blindDuration` | String (CEL) | "3000" | 持続時間（ms） |
| `blindAmplifier` | Int | 0 | 効果レベル（0-255） |

**使用例:**
```yaml
effects:
  - type: blind
    blindDuration: '5000'  # 5秒
    blindAmplifier: 2
```

---

### SetBlockEffect

ターゲット位置にブロックを設置または破壊するエフェクト。

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `blockType` | String | "AIR" | ブロックタイプ |
| `blockRadius` | String (CEL) | "0.0" | 設置範囲半径 |
| `blockTemporary` | Boolean | false | 一時的な設置か |
| `blockTemporaryDuration` | String | "5000" | 一時設置の持続時間（ms） |

**使用例:**
```yaml
effects:
  # 単一ブロック設置
  - type: setblock
    blockType: STONE
    blockRadius: '0'

  # 範囲ブロック設置（一時的）
  - type: setblock
    blockType: BARRIER
    blockRadius: '2.0'
    blockTemporary: true
    blockTemporaryDuration: '10000'  # 10秒後に消える
```

---

## 新しいターゲッター

### NearestTargeter

ベースターゲッターから取得したターゲットの中から、最もソースに近いエンティティを選択するターゲッター。

**パラメータ:**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `baseTargeter` | TargeterDefinition | ベースとなるターゲッター |
| `filter` | String (CEL) | フィルター条件 |

**使用例:**
```yaml
targeter:
  type: nearest
  baseTargeter:
    type: radiusplayers
    range: 15.0
```

---

### FarthestTargeter

ベースターゲッターから取得したターゲットの中から、最もソースから遠いエンティティを選択するターゲッター。

**パラメータ:**

| パラメータ | 型 | 説明 |
|-----------|-----|------|
| `baseTargeter` | TargeterDefinition | ベースとなるターゲッター |
| `filter` | String (CEL) | フィルター条件 |

**使用例:**
```yaml
targeter:
  type: farthest
  baseTargeter:
    type: radiusplayers
    range: 30.0
```

---

### ThreatTargeter

脅威度（Threat/Aggro）に基づいてターゲットを選択するターゲッター。

**特徴:**
- MMORPGスタイルの脅威度管理
- 上位N体を選択可能
- Entity Metadataで脅威度を管理

**パラメータ:**

| パラメータ | 型 | デフォルト | 説明 |
|-----------|-----|----------|------|
| `baseTargeter` | TargeterDefinition | - | ベースとなるターゲッター |
| `count` | Int | 1 | 選択する上位数 |
| `filter` | String (CEL) | null | フィルター条件 |

**使用例:**
```yaml
targeter:
  type: threat
  count: 3  # 脅威度上位3体
  baseTargeter:
    type: radiusplayers
    range: 25.0
```

**脅威度の管理:**

脅威度は自動的には管理されません。スキルやエフェクトから手動で追加する必要があります。

```kotlin
// Kotlinコードから脅威度を追加
ThreatTargeter.addThreat(mobEntity, playerEntity, 10.0)

// 脅威度を取得
val threat = ThreatTargeter.getThreat(mobEntity, playerEntity)

// 脅威度をクリア
ThreatTargeter.clearThreat(mobEntity, playerEntity)
```

---

## 複合例

新機能を組み合わせた高度な例：

```yaml
UltimateRaidBoss:
  type: WITHER
  display: '&5&l&k||&r &4&lレイドボス &5&l&k||'
  health: '2000'
  damage: '30'
  armor: '30'

  skills:
    # スキル1: 脅威度上位3体にビーム攻撃
    onTimer:
      - name: threat_beam
        interval: 200

        targeter:
          type: threat
          count: 3
          baseTargeter:
            type: radiusplayers
            range: 40.0

        skills:
          - skill: multi_beam
            type: BEAM
            beamRange: '35.0'
            beamWidth: '1.0'
            beamDuration: '3000'
            beamPiercing: true
            effects:
              - type: damage
                damageAmount: 15.0
              - type: blind
                blindDuration: '5000'

    # スキル2: ヒーリングオーラを展開
    onDamaged:
      - name: self_heal_aura
        condition: 'entity.health < entity.maxHealth * 0.3'

        targeter:
          type: self

        skills:
          - skill: emergency_aura
            type: AURA
            auraRadius: '5.0'
            auraDuration: '15000'
            auraTickInterval: '1000'
            auraSelfAffect: true
            effects:
              - type: heal
                healAmount: 20.0

    # スキル3: 最も近い敵をテレポート→押し出し
    onAttack:
      - name: teleport_push_combo

        targeter:
          type: nearest
          baseTargeter:
            type: radiusplayers
            range: 20.0

        skills:
          - skill: combo
            type: basic
            effects:
              - type: teleport
                teleportMode: TO_SOURCE
              - type: push
                pushStrength: '3.0'
              - type: setblock
                blockType: FIRE
                blockRadius: '2.0'
                blockTemporary: true
```

---

## CEL式サポート

新機能のほとんどのパラメータはCEL式をサポートしています。

**例:**
```yaml
effects:
  # プレイヤーのレベルに応じた引き寄せ強度
  - type: pull
    pullStrength: '1.0 + (target.level * 0.1)'

  # 距離に応じたビーム範囲
skills:
  - skill: dynamic_beam
    type: BEAM
    beamRange: 'target.distance * 1.5'
    beamWidth: '0.5 + (entity.health / entity.maxHealth)'
```

---

## サンプルファイル

より詳しい例は以下のサンプルファイルを参照してください：

- `sample/new_skills_examples.yml` - BeamSkillとAuraSkillの例
- `sample/new_effects_examples.yml` - 新しいエフェクトの例
- `sample/new_targeters_examples.yml` - 新しいターゲッターの例

---

## 注意事項

1. **同期処理**: ブロック操作やテレポートは `sync: true` を推奨
2. **パフォーマンス**: BeamSkillとAuraSkillは計算負荷が高いため、適切な間隔を設定
3. **脅威度管理**: ThreatTargeterを使用する場合は、脅威度を手動で管理する必要があります
4. **ブロック設置**: SetBlockEffectは適切に使用しないとワールドを破壊する可能性があります

---

## 既知の問題

- PacketEntity版の脅威度管理は未実装（将来実装予定）
- 一部のエフェクトはFolia環境でのテストが不十分

---

## 今後の拡張予定

Phase 3では以下のパフォーマンス最適化が予定されています：
- ProjectileSkillのパーティクル描画最適化
- Targeterのキャッシング機能
- より高度な脅威度管理システム
