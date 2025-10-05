# Unique - Extended Features Documentation

## 🎯 新機能概要

### Phase 7追加実装: 高度なターゲッターとスキル

このドキュメントは、Uniqueプラグインに追加された高度な機能を説明します。

---

## 🎯 拡張ターゲッター

### 1. ConditionalTargeter (条件付きターゲッター)

**概要**: ベースターゲッターの結果を複数の条件でフィルタリング

**使用例**:
```yaml
targeter:
  type: Conditional
  baseTargeter:
    type: NearestPlayer
    range: 30
  conditions:
    - "target.health > 50"
    - "target.gameMode == 'SURVIVAL'"
    - "!target.isFlying"
```

**特徴**:
- 複数の条件をAND結合で評価
- 既存のターゲッターを条件でフィルタリング
- CEL式で柔軟な条件指定

### 2. ChainTargeter (連鎖ターゲッター)

**概要**: 最初のターゲットから連鎖的に周囲のエンティティをターゲット

**使用例**:
```yaml
targeter:
  type: Chain
  initialTargeter:
    type: NearestPlayer
    range: 20
  maxChains: 5
  chainRange: 5.0
  allowDuplicates: false
```

**パラメータ**:
- `initialTargeter`: 最初のターゲットを選択するターゲッター
- `maxChains`: 最大連鎖数
- `chainRange`: 各連鎖の範囲（ブロック）
- `allowDuplicates`: 同じターゲットを複数回選択可能か

**使用シーン**:
- チェーンライトニング
- 感染エフェクト
- 連鎖ダメージ

### 3. AreaTargeter (エリアターゲッター)

様々な形状のエリア内のエンティティをターゲット

#### CircleAreaTargeter (円形)

```yaml
targeter:
  type: CircleArea
  radius: 10.0
  height: 3.0
```

#### BoxAreaTargeter (矩形)

```yaml
targeter:
  type: BoxArea
  width: 10.0
  height: 5.0
  depth: 10.0
```

#### ConeAreaTargeter (円錐)

```yaml
targeter:
  type: ConeArea
  range: 15.0
  angle: 45.0  # 度数
```

#### DonutAreaTargeter (ドーナツ型)

```yaml
targeter:
  type: DonutArea
  innerRadius: 5.0
  outerRadius: 15.0
  height: 3.0
```

---

## ⚡ 新スキルタイプ

### 1. SummonSkill (召喚スキル)

**概要**: バニラエンティティやカスタムMobを召喚

**バニラエンティティ召喚**:
```yaml
SummonZombies:
  type: Summon
  summonType: VANILLA
  entityType: ZOMBIE
  amount: 3
  spreadRange: 3.0
  duration: 30000  # 30秒後に消滅
  inheritTarget: true
```

**カスタムMob召喚**:
```yaml
SummonBoss:
  type: Summon
  summonType: CUSTOM
  customMobId: BossDragon
  amount: 1
  spreadRange: 0
```

**パラメータ**:
- `summonType`: `VANILLA` or `CUSTOM`
- `entityType`: バニラエンティティタイプ
- `customMobId`: カスタムMob ID
- `amount`: 召喚数
- `spreadRange`: 散らばり範囲
- `duration`: 持続時間（null = 永続）
- `inheritTarget`: 召喚者のターゲットを引き継ぐ

### 2. TeleportSkill (テレポートスキル)

**概要**: ターゲットを指定位置にテレポート

**タイプ別使用例**:

**OFFSET (相対オフセット)**:
```yaml
RandomTeleport:
  type: Teleport
  teleportType: OFFSET
  offset:
    x: 10
    y: 0
    z: -5
  playEffect: true
  playSound: true
```

**BEHIND (背後へ)**:
```yaml
TeleportBehind:
  type: Teleport
  teleportType: BEHIND
  behindDistance: 5.0
```

**SWAP (入れ替え)**:
```yaml
TeleportSwap:
  type: Teleport
  teleportType: SWAP
```

**パラメータ**:
- `teleportType`: `OFFSET`, `ABSOLUTE`, `BEHIND`, `TO_SOURCE`, `SWAP`
- `offset`: 相対オフセット（Vector）
- `behindDistance`: 背後への距離
- `playEffect`: エフェクト再生
- `playSound`: サウンド再生

### 3. BuffSkill (バフ/デバフスキル)

**概要**: ポーション効果や属性変更を適用

**バフ例**:
```yaml
StrengthBuff:
  type: Buff
  potionEffects:
    - type: STRENGTH
      amplifier: 1
      showIcon: true
    - type: SPEED
      amplifier: 0
      showIcon: true
  duration: 200  # 10秒
  showParticles: true
```

**デバフ例**:
```yaml
WeaknessDebuff:
  type: Buff
  potionEffects:
    - type: WEAKNESS
      amplifier: 2
    - type: SLOWNESS
      amplifier: 1
  duration: 300  # 15秒
```

**属性変更**:
```yaml
HealthBoost:
  type: Buff
  potionEffects:
    - type: HEALTH_BOOST
      amplifier: 2
  attributeModifiers:
    - attribute: MAX_HEALTH
      amount: 20.0
      operation: ADD
  duration: 600
```

**パラメータ**:
- `potionEffects`: ポーション効果リスト
- `attributeModifiers`: 属性変更リスト
- `duration`: 持続時間（tick）
- `showParticles`: パーティクル表示
- `ambient`: アンビエント効果

**属性変更操作**:
- `ADD`: 加算
- `MULTIPLY`: 乗算
- `SET`: 設定

### 4. CommandSkill (コマンド実行スキル)

**概要**: 指定されたコマンドを実行

**コンソール実行**:
```yaml
BroadcastDefeat:
  type: Command
  executor: CONSOLE
  commands:
    - "say {target} has been defeated!"
    - "playsound minecraft:entity.ender_dragon.death master @a"
```

**プレイヤー実行**:
```yaml
PlayerAction:
  type: Command
  executor: TARGET
  commands:
    - "spawn"
```

**報酬配布**:
```yaml
GiveReward:
  type: Command
  executor: CONSOLE
  commands:
    - "give {target} diamond 5"
    - "give {target} emerald 10"
  commandDelay: 100  # コマンド間の遅延
```

**プレースホルダー**:
- `{source}`: ソースエンティティ名
- `{source_uuid}`: ソースUUID
- `{source_x/y/z}`: ソース座標
- `{source_world}`: ソースワールド
- `{target}`: ターゲット名
- `{target_uuid}`: ターゲットUUID
- `{target_x/y/z}`: ターゲット座標
- `{target_world}`: ターゲットワールド

**パラメータ**:
- `commands`: コマンドリスト
- `executor`: `CONSOLE`, `TARGET`, `SOURCE`
- `commandDelay`: コマンド間の遅延（ms）

---

## 📝 実装例

### 複雑なボスMob

```yaml
AncientDragon:
  Type: ENDER_DRAGON
  Display: '&5&l&k||&r &d&lAncient Dragon&r &5&l&k||'
  Health: 2000
  Damage: 50
  
  Skills:
    OnTimer:
      # フェーズ1: 通常攻撃
      - skill: ConditionalFireball
        interval: 3s
        condition: "entity.health > entity.maxHealth * 0.7"
        
      # フェーズ2: 連鎖攻撃
      - skill: ChainLightning
        interval: 5s
        condition: "entity.health <= entity.maxHealth * 0.7"
        
      # フェーズ3: 究極攻撃
      - skill: UltimateAttack
        interval: 10s
        condition: "entity.health <= entity.maxHealth * 0.3"
```

### 連鎖スキルコンボ

```yaml
UltimateAttack:
  type: Meta
  skills:
    - skill: StrengthBuff
      targeter:
        type: Self
      meta:
        executeDelay: 0ms
    
    - skill: TeleportBehind
      meta:
        executeDelay: 500ms
    
    - skill: CircleExplosion
      meta:
        executeDelay: 1s
    
    - skill: SummonMinions
      meta:
        executeDelay: 2s
```

---

## 🔧 実装状況

### ✅ 完了
- [x] ConditionalTargeter
- [x] ChainTargeter
- [x] AreaTargeter (Circle, Box, Cone, Donut)
- [x] SummonSkill
- [x] TeleportSkill
- [x] BuffSkill
- [x] CommandSkill
- [x] サンプルYAML作成
- [x] TargeterManager更新
- [x] SkillManager更新

### 📋 次のステップ
- [ ] イベントリスナー実装
- [ ] コマンドハンドラー実装
- [ ] 総合テスト
- [ ] パフォーマンス最適化

---

## 📖 参考資料

- [メインREADME](README.md)
- [プロジェクトナレッジ](cel_mob_plugin_knowledge.md)
- [サンプルファイル](src/main/resources/sample/)

---

## 🎮 使い方

1. **スキルを定義**: `plugins/Unique/skills/` にYAMLファイルを配置
2. **Mobを定義**: `plugins/Unique/mobs/` にYAMLファイルを配置
3. **スポーンを設定**: `plugins/Unique/spawns/` にYAMLファイルを配置
4. **リロード**: `/unique reload`

---

Made with ❤️ by azuazu3939