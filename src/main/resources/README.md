# Unique Plugin - リソースフォルダー

このフォルダーには、Uniqueプラグインの設定ファイルとサンプルが含まれています。

## フォルダー構造

```
plugins/Unique/
├── config.yml          # プラグインのメイン設定
│
├── mobs/               # ★ユーザーが書く：Mob定義
│   ├── basic_mobs.yml      # 基本的なカスタムMob
│   └── boss_mobs.yml       # ボス級カスタムMob
│
├── skills/             # ★ユーザーが書く：スキル組み合わせ定義
│   ├── projectile_skills.yml  # 発射物スキル
│   ├── combat_skills.yml      # 戦闘スキル
│   └── aura_beam_skills.yml   # オーラ・ビームスキル
│
├── spawns/             # ★ユーザーが書く：スポーン設定
│   └── world_spawns.yml        # ワールドスポーン設定
│
├── libs/               # ライブラリ（基本的に触らない）
│   ├── Effects/            # エフェクト実装定義
│   │   ├── damage_effects.yml    # ダメージ系エフェクト
│   │   ├── support_effects.yml   # サポート系エフェクト
│   │   ├── visual_effects.yml    # 視覚専用エフェクト
│   │   └── area_effects.yml      # 範囲系エフェクト
│   ├── Targeters/          # カスタムターゲッター実装
│   │   └── custom_targeters.yml
│   └── Triggers/           # カスタムトリガー実装
│       └── custom_triggers.yml
│
└── sample/             # サンプル・参照用
    ├── complete_example.yml    # 完全な実装例
    ├── reload_event_example.kt # リロードイベント例
    └── README.md               # サンプル説明

```

## エイリアス機能

スキル、エフェクト、コンディションに短縮名（エイリアス）を付けることができます。これにより、長い名前や複雑な定義を簡潔に参照できます。

**設定方法:**

config.ymlまたは専用のaliases.ymlファイルでエイリアスを定義します。

```yaml
# config.yml または aliases.yml
aliases:
  # スキルのエイリアス
  skills:
    FB: BasicFireball              # FB → BasicFireball
    PB: PowerfulBlaze              # PB → PowerfulBlaze
    Heal: SelfHealingEffect        # Heal → SelfHealingEffect
    AOE: AreaOfEffectDamage        # AOE → AreaOfEffectDamage

  # コンディションのエイリアス
  conditions:
    LowHP: "entity.health < entity.maxHealth * 0.3"
    HighHP: "entity.health > entity.maxHealth * 0.7"
    NearPlayers: "nearbyPlayers.count > 0"
    CanStoreVars: "target.canHoldVariables"

  # ターゲッターのエイリアス
  targeters:
    # Nearest系（最も近い）
    NP: NearestPlayer              # 最も近いプレイヤー
    NL: NearestLiving              # 最も近いLivingEntity
    NE: NearestEntity              # 最も近いEntity
    NPM: NearestPacketMob          # 最も近いPacketMob

    # All系（範囲内全て）
    AP: AllPlayers                 # 範囲内の全プレイヤー
    AL: AllLiving                  # 範囲内の全LivingEntity
    AE: AllEntities                # 範囲内の全Entity
    APM: AllPacketMobs             # 範囲内の全PacketMob

    # その他
    Self: SelfTargeter             # 自分自身
    Los: LineOfSight               # 視線先
    Atk: AttackerTargeter          # 攻撃者
```

**使用例:**

```yaml
# エイリアス定義
aliases:
  skills:
    FB: BasicFireball
  conditions:
    LowHP: "entity.health < entity.maxHealth * 0.3"
  targeters:
    NP: Nearest

# Mob定義で使用
StrongZombie:
  Type: ZOMBIE
  Skills:
    # プレイヤーをターゲット
    - FB @NP{r=10} ~onAttack        # 最も近いプレイヤーに火球
    - Heal @Self{cond=LowHP} ~onDamaged  # 低HP時に回復

    # エンティティタイプ別ターゲティング
    - damage{amount=20} @NL{r=15} ~onAttack     # 近くのLivingEntityにダメージ
    - freeze{duration=60} @AE{r=10} ~onTimer:100  # 全Entityを凍結
    - heal{amount=10} @APM{r=20} ~onTimer:60    # PacketMobを回復

    # 複数のエイリアスを組み合わせ
    - PB @AP{r=20;cond=HighHP} ~onTimer:40  # HP高い時に全プレイヤーへ
```

**エイリアスのメリット:**

1. **可読性向上**: 長い名前を短縮して読みやすくする
2. **保守性向上**: エイリアスの定義を変更すれば、全ての参照先が一括で更新される
3. **入力効率**: 頻繁に使用するスキルやコンディションを短く記述できる
4. **統一性**: チーム開発で共通の短縮名を使用できる

**注意事項:**

- エイリアス名は短すぎると意味が分かりにくくなるため、2-4文字程度を推奨
- エイリアスと実際の定義名が重複しないように注意
- コンディションのエイリアスは複雑な式を簡潔にするのに特に有効

---

## 各フォルダーの役割

### mobs/ - ★ユーザーが書く：Mob定義
カスタムMobの定義を配置します。**超コンパクト構文**を使用してスキルやエフェクトを参照します。エイリアス機能を活用することで、さらに簡潔な記述が可能です。

```yaml
# mobs/basic_mobs.yml
StrongZombie:
  Type: ZOMBIE
  Display: "&c強化ゾンビ"
  Health: "60"
  Skills:
    # 超コンパクト構文: スキル @ターゲッター ~トリガー
    - MeleeEnhance @TL ~onAttack
    - Knockback @Self ~onDamaged
    # libs/Effects/から参照
    - HeavyDamageEffect @TL ~onAttack
```

### skills/ - ★ユーザーが書く：スキル組み合わせ定義
再利用可能なスキルの定義を配置します。**インライン形式**で定義し、Mob側から参照されます。
libs/Effects/のエフェクトを組み合わせて独自のスキルを作成できます。

```yaml
# skills/combat_skills.yml
# インライン形式、トリガーは含めない
MeleeEnhance:
  - damage{amount=25}
  - particle{type=SWEEP_ATTACK;count=15;audience=@RadiusPlayers{r=50}}
  - sound{type=ENTITY_PLAYER_ATTACK_STRONG;volume=1.0;pitch=0.9;audience=@RadiusPlayers{r=30}}
```

### spawns/ - ★ユーザーが書く：スポーン設定
Mobのスポーン条件や場所を定義します。

```yaml
# spawns/world_spawns.yml
OverworldCommonSpawns:
  mob: StrongZombie
  worlds:
    - world
  conditions:
    - type: Time
      value: NIGHT
  spawnRate: 0.05
```

### libs/ - ライブラリ（基本的に触らない）
プラグインが提供する基本実装定義です。ユーザーはここから参照するだけで、通常は編集不要です。

#### libs/Effects/ - エフェクト実装定義
見た目と効果を持つエフェクトの定義です。**オーディエンス機能**に対応しています。

- **damage_effects.yml**: ダメージ系エフェクト（HeavyDamageEffect, FlameDamageEffect等）
- **support_effects.yml**: サポート系エフェクト（GreatHealEffect, ShieldEffect等）
- **visual_effects.yml**: 視覚専用エフェクト（FlameVisualEffect, MagicVisualEffect等）
- **area_effects.yml**: 範囲系エフェクト（ExplosionEffect, LightningEffect等）
- **utility_effects.yml**: ユーティリティエフェクト（Delay, Wait, Teleport等）

**オーディエンス機能**: 効果や見た目を受け取る対象を指定できます
```yaml
# audience パラメータで受信者を指定
- particle{type=FLAME;count=30;audience=@RadiusPlayers{r=50}}
- sound{type=ENTITY_BLAZE_SHOOT;volume=1.0;audience=@RadiusPlayers{r=30}}
- damage{amount=50;audience=@TargetLocation}
```

**タイミング制御（delay/wait）**:
```yaml
# delayスキル: 並列実行（前のスキルを待たない）
Skills:
  - damage{amount=50}
  - delay 2s           # 並列で2秒遅延開始
  - particle{type=FLAME;count=30}  # 2秒後に実行

# waitスキル: 直列実行（前のスキルが全て完了するまで待つ）
Skills:
  - damage{amount=50}
  - wait 2s            # damageが完了してから2秒待機
  - particle{type=FLAME;count=30}  # damage完了+2秒後に実行

# エフェクトレベルのdelay: 個別エフェクトに遅延を設定
Skills:
  - damage{amount=50}
  - sound{type=ENTITY_BLAZE_SHOOT;volume=1.0;delay=1s}  # 1秒遅延
  - particle{type=FLAME;count=30;delay=2s}  # 2秒遅延

# CEL式による動的遅延
Skills:
  - damage{amount=50}
  - delay "current.tps.toInt"  # TPSに応じた遅延
  - sound{delay="nearbyPlayers.count * 5"}  # プレイヤー数×5tick遅延
```

#### libs/Targeters/ - カスタムターゲッター実装
カスタムターゲッターの定義です。将来的にYAMLで定義可能になる予定です。

#### libs/Triggers/ - カスタムトリガー実装
カスタムトリガーの定義です。将来的にYAMLで定義可能になる予定です。

### sample/ - サンプル・参照用
完全な実装例やコードサンプルが含まれています。学習や参照用に使用してください。

## 超コンパクト構文

Mob定義では以下の構文を使用します：

```
スキル定義 @ターゲッター ~トリガー
```

### 例

```yaml
Skills:
  # 基本例
  - BasicFireball @NearestPlayer{r=30} ~onTimer:60
  
  # インライン定義
  - damage{amount=20} @TargetLocation ~onAttack
  
  # 条件付き
  - SelfHeal @Self{cond=entity.health < entity.maxHealth * 0.3} ~onDamaged
  
  # 省略形
  - PowerfulFireball @NP{r=50} ~onTimer:40
```

### ターゲッター省略形

| 省略形 | 正式名 |
|--------|---------|
| `@TL` | @TargetLocation |
| `@NP` | @NearestPlayer |
| `@RP` | @RadiusPlayers |
| `@RND` | @Random |
| `@LH` | @LowestHealth |
| `@HH` | @HighestHealth |

### トリガー

| トリガー | 説明 |
|----------|------|
| `~onTimer:interval` | 一定間隔で発動 |
| `~onDamaged` | ダメージを受けた時 |
| `~onDeath` | 死亡時 |
| `~onSpawn` | スポーン時 |
| `~onAttack` | 攻撃時 |

### 時間単位

| 単位 | 説明 |
|------|------|
| `30` | 30tick（デフォルト、1.5秒） |
| `30t` | 30tick（明示的） |
| `3s` | 3秒 |
| `500ms` | 500ミリ秒 |
| `5m` | 5分 |

## クイックスタート

1. **サンプルをコピー**
   ```bash
   cp sample/complete_example.yml mobs/my_first_mob.yml
   ```

2. **ファイルを編集**
   - スキル定義を `skills/` に分離（または `libs/Effects/` から参照）
   - Mob定義を調整
   - 必要に応じて `spawns/` にスポーン設定を追加

3. **libs/Effects/ から参照する（推奨）**
   ```yaml
   # mobs/my_first_mob.yml
   Skills:
     - HeavyDamageEffect @TL ~onAttack
     - FlameVisualEffect @Self ~onSpawn
   ```

4. **リロード**
   ```
   /unique reload
   ```

5. **スポーン**
   ```
   /unique spawn ExampleBoss
   ```

## ベストプラクティス

### ✅ 推奨

- **スキルは skills/ にインライン形式で定義**し、Mob定義から参照
  ```yaml
  # skills/
  MeleeEnhance:
    - damage{amount=25}
    - particle{type=SWEEP_ATTACK;count=15;audience=@RadiusPlayers{r=50}}
  ```

- **スキル定義にはトリガーを含めない**（Mob側で指定）
  ```yaml
  # mobs/
  Skills:
    - MeleeEnhance @TL ~onAttack  # ← トリガーはここ
  ```

- **libs/Effects/ のエフェクトを活用**して独自のスキルを作成
  ```yaml
  # skills/
  CustomAttack:
    - HeavyDamageEffect  # libs/Effects/から参照
    - FlameVisualEffect
  ```

- **オーディエンス機能を活用**して視覚効果の受信者を制御
  ```yaml
  - particle{type=FLAME;count=30;audience=@RadiusPlayers{r=50}}
  - sound{type=ENTITY_BLAZE_SHOOT;audience=@NearestPlayer{r=30}}
  ```

- **超コンパクト構文**を使用してMobを定義
- **CEL式**を活用して動的なMobを作成
- **適切なフォルダー**に定義を配置（mobs/, skills/, spawns/）

### ❌ 非推奨

- スキル定義に type: Basic, type: Projectile などを使用（旧形式）
- スキル定義にトリガーを含める（Mob側で指定すべき）
- すべてを1ファイルにまとめる（管理が困難）
- libs/ フォルダーを直接編集する（参照のみ推奨）

## サポート

詳細なドキュメント：
- [クイックスタート](../../../docs/QUICKSTART.md)
- [完全ガイド](../../../docs/GUIDE.md)
- [リファレンス](../../../docs/REFERENCE.md)
- [超コンパクト構文](../../../docs/ULTRA_COMPACT_SYNTAX.md)

問題が発生した場合：
- [トラブルシューティング](../../../docs/TROUBLESHOOTING.md)
- [開発ガイド](../../../docs/DEVELOPMENT.md)

## ライセンス

このプラグインはGNU General Public License v3.0の下で公開されています。

---

## 実装メモ（開発者向け）

### オーディエンス機能の実装

**effectのクラス設計:**
- `audience` パラメータ: エフェクトや視覚を受け取る対象を指定
- `hasEffect` パラメータ: サーバー/プレイヤーに効果があるか（デフォルト: true）
- パーティクルやサウンド等の視覚系は `hasEffect=false` 相当

**設計例:**
```kotlin
// Effectベースクラス
abstract class Effect {
    var audience: Targeter? = null  // 受信者指定
    var hasEffect: Boolean = true   // 効果の有無

    abstract fun apply(target: Entity)
    open fun shouldApplyTo(entity: Entity): Boolean {
        return audience?.matches(entity) ?: true
    }
}

// 視覚専用エフェクト（パーティクル、サウンド）
class VisualEffect : Effect() {
    init {
        hasEffect = false  // 視覚のみ、ゲーム効果なし
    }
}

// ダメージエフェクト
class DamageEffect : Effect() {
    init {
        hasEffect = true  // デフォルトで効果あり
    }
}
```

**audienceの動作:**
- `audience=@RadiusPlayers{r=50}`: 半径50以内のプレイヤーに視覚効果を表示
- `audience=@TargetLocation`: ターゲット位置のエンティティに効果を適用
- `audience` 未指定: デフォルトの対象（スキル実行時のターゲット）に適用

---

### タイミング制御の実装

**3種類のタイミング制御:**

1. **delayスキル（並列実行）**
   - 前のスキルを待たずに遅延を開始（トリガー時刻から計算）
   - 記法: `- delay 2s`, `- delay 5`, `- delay "current.tps.toInt"`

2. **waitスキル（直列実行）**
   - 前のスキル完了を待ってから遅延を開始
   - 記法: `- wait 2s`, `- wait 5`, `- wait "current.tps.toInt"`

3. **エフェクトレベルのdelayパラメータ**
   - トリガー発火後、個別エフェクトの実行を遅延
   - 記法: `sound{delay=20}`, `sound{delay="2s"}`, `sound{delay="current.tps.toInt"}`
   - **重要**: `wait` パラメータは実装しない（delayのみ）

**実装設計:**

```kotlin
// Effectベースクラス
abstract class Effect {
    var delay: String = "0"  // CEL式、時間単位、またはtick数
    var audience: Targeter? = null
    var hasEffect: Boolean = true

    // baseTime: 前のエフェクトの完了時刻（tick）
    // 戻り値: このエフェクトの完了時刻（tick）
    open fun execute(target: Entity, baseTime: Long): Long {
        val delayTicks = parseTime(delay)  // CEL評価 + 時間単位変換
        val executeTime = baseTime + delayTicks

        if (delayTicks > 0) {
            scheduleAt(executeTime) {
                apply(target)
            }
        } else {
            apply(target)
        }

        // 完了時刻 = 実行時刻 + 実行時間
        return executeTime + getDuration()
    }

    abstract fun apply(target: Entity)
    open fun getDuration(): Long = 0  // 大抵のエフェクトは即座に完了

    // 時間パース: "2s" -> 40, "5" -> 5, "current.tps.toInt" -> CEL評価
    fun parseTime(time: String): Long {
        return when {
            time.contains('"') -> {
                // CEL式: "current.tps.toInt"
                val expr = time.trim('"')
                celEngine.evaluate(expr).toLong()
            }
            time.endsWith("s") -> time.removeSuffix("s").toLong() * 20
            time.endsWith("ms") -> time.removeSuffix("ms").toLong() / 50
            time.endsWith("m") -> time.removeSuffix("m").toLong() * 1200
            else -> time.toLong()  // tick数
        }
    }
}

// DelayEffect: 並列実行
class DelayEffect(val time: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val delayTicks = parseTime(time)
        // 並列: トリガー時刻（0）からdelayTicks後に完了
        return delayTicks
    }

    override fun apply(target: Entity) {
        // 何もしない（時間経過のみ）
    }
}

// WaitEffect: 直列実行
class WaitEffect(val time: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val waitTicks = parseTime(time)
        // 直列: 前のエフェクト完了からwaitTicks後に完了
        return baseTime + waitTicks
    }

    override fun apply(target: Entity) {
        // 何もしない（時間経過のみ）
    }
}

// スキル実行エンジン
class SkillExecutor {
    fun executeSkill(effects: List<Effect>, target: Entity) {
        var currentCompletionTime = 0L  // 現在の完了時刻

        for (effect in effects) {
            val completionTime = effect.execute(target, currentCompletionTime)
            currentCompletionTime = completionTime
        }
    }
}
```

**動作検証:**

```yaml
# ケース1: エフェクトレベルdelay + wait
Skills:
  - sound{delay="20"}     # 20tick後に実行、完了時刻=20
  - wait 2s               # 20完了後さらに40tick、完了時刻=60
  - particle{...}         # 60tick後に実行

# ケース2: delayスキル（並列）
Skills:
  - projectile{...}       # 0tick開始、40tick後に着弾完了
  - delay 2s              # 並列: 0tickから40tick後に完了（projectileの完了を待たない）
  - particle{...}         # 40tick後に実行（projectileと同時）

# ケース3: waitスキル（直列）
Skills:
  - projectile{...}       # 0tick開始、40tick後に完了
  - wait 2s               # 直列: 40tick完了後さらに40tick、完了時刻=80
  - particle{...}         # 80tick後に実行

# ケース4: CEL式による動的遅延
Skills:
  - damage{amount=50}
  - delay "current.tps.toInt"  # 現在のTPSに応じた遅延
  - sound{delay="nearbyPlayers.count * 5"}  # プレイヤー数に応じた遅延
```

**実装のポイント:**
- `delay`スキル: `execute()`で常にトリガー時刻（0）からの完了時刻を返す
- `wait`スキル: `execute()`でbaseTimeに加算した完了時刻を返す
- エフェクトレベルdelay: baseTimeに加算するため、waitと同じ動作
- 全ての時間指定でCEL式を使用可能

---

### 変数保存機能

スキル実行中に計算結果を一時保存し、後続のエフェクトで再利用できます。

**記法:**
```yaml
# 変数に値を保存
- set 変数名 "CEL式"

# 保存した変数を参照
- damage{amount="vars.変数名"}
```

**使用例:**

```yaml
# HP割合を計算して保存
Skills:
  - set hpPercent "entity.health / entity.maxHealth * 100"
  - damage{amount="vars.hpPercent * 2"}
  - particle{count="cast.toInt(vars.hpPercent)"}
  - sound{volume="vars.hpPercent / 100"}

# 複数の変数を保存
Skills:
  - set playerCount "nearbyPlayers.count"
  - set avgLevel "nearbyPlayers.avgLevel"
  - set scaledDamage "vars.playerCount * vars.avgLevel * 0.5"
  - damage{amount="vars.scaledDamage"}
  - delay "vars.playerCount * 10"

# 条件分岐で変数を使用
Skills:
  - set hpPercent "entity.health / entity.maxHealth * 100"
  - set damage "vars.hpPercent < 50 ? 100 : 50"
  - damage{amount="vars.damage"}

# TPS基準の動的調整
Skills:
  - set tpsAdjust "20 - average.tps5m"
  - delay "cast.toInt(vars.tpsAdjust * 5)"
  - damage{amount="50 + vars.tpsAdjust * 2"}
```

**実装設計:**

```kotlin
// SetEffect: 変数を保存するエフェクト
class SetEffect(val varName: String, val expression: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        // CEL式を評価
        val value = celEngine.evaluate(expression, context)

        // コンテキストの vars に保存
        if (!context.containsKey("vars")) {
            context["vars"] = mutableMapOf<String, Any>()
        }
        (context["vars"] as MutableMap<String, Any>)[varName] = value

        // 即座に完了
        return baseTime
    }

    override fun apply(target: Entity) {
        // 何もしない（変数保存のみ）
    }
}

// スキル実行エンジン
class SkillExecutor {
    fun executeSkill(effects: List<Effect>, target: Entity) {
        // スキル実行用のコンテキストを構築
        val context = CELVariableProvider.buildEntityContext(target).toMutableMap()

        // vars を初期化
        context["vars"] = mutableMapOf<String, Any>()

        var currentCompletionTime = 0L

        for (effect in effects) {
            // コンテキストを各エフェクトに渡す
            effect.context = context
            val completionTime = effect.execute(target, currentCompletionTime)
            currentCompletionTime = completionTime
        }
    }
}
```

**変数のスコープ:**
- スキル実行中のみ有効（スキル完了後は破棄）
- 同じスキル内の後続エフェクトからのみアクセス可能
- トリガーごとに独立した変数スペース

**注意点:**
- 変数名は重複可能（上書きされる）
- `vars.` プレフィックスでアクセス
- 変数は常にスキル実行時に評価される
- **型指定は不要**（保存時は評価結果の型、使用時にキャスト）

---

### 繰り返し処理（foreach/repeat）

スキルの一部を複数回繰り返し実行できます。`wait`と組み合わせて時間差攻撃が可能です。

**記法:**
```yaml
# foreach形式: エフェクトのリストを繰り返し
- foreach{count=回数;effects=[エフェクト1,エフェクト2,...]}

# repeat形式: 単一エフェクトを繰り返し
- repeat{count=回数;delay=間隔;effect=エフェクト}
```

**使用例:**

```yaml
# 5回連続攻撃（wait併用）
Skills:
  - foreach{count=5;effects=[damage{amount=10},particle{type=FLAME;count=5},wait{time=1s}]}

# プレイヤー数分繰り返し
Skills:
  - foreach{count="nearbyPlayers.count";effects=[damage{amount=20},wait{time=500ms}]}

# 変数を使った繰り返し
Skills:
  - set loopCount "cast.toInt(entity.health / 10)"
  - foreach{count="vars.loopCount";effects=[damage{amount=5},wait{time=200ms}]}

# ループカウンタを利用
Skills:
  - foreach{count=10;effects=[
      damage{amount="10 + vars.index * 2"},
      particle{count="vars.iteration * 5"},
      wait{time="vars.index * 100"}
    ]}

# repeat形式でシンプルに
Skills:
  - repeat{count=5;delay=1s;effect=damage{amount=15}}

# TPS基準の繰り返し
Skills:
  - set repeatCount "cast.toInt(average.tps1m)"
  - foreach{count="vars.repeatCount";effects=[
      damage{amount=10},
      sound{type=ENTITY_BLAZE_SHOOT;delay="vars.index * 100"},
      wait{time=500ms}
    ]}

# 複雑な繰り返しパターン
Skills:
  - set hpPercent "entity.health / entity.maxHealth * 100"
  - set attackCount "vars.hpPercent < 50 ? 10 : 5"
  - foreach{count="vars.attackCount";effects=[
      damage{amount="20 + vars.index * 3"},
      particle{type=CRIT;count="10 + vars.iteration * 2"},
      wait{time="1s"}
    ]}
```

**ループ変数:**
- `vars.index`: 0から始まるループインデックス（0, 1, 2, ...）
- `vars.iteration`: 1から始まる反復回数（1, 2, 3, ...）

**実装設計:**

```kotlin
// ForeachEffect: エフェクトリストを繰り返し実行
class ForeachEffect(
    val countExpression: String,
    val effects: List<Effect>
) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        // 繰り返し回数を評価
        val loopCount = evaluateExpression(countExpression).toInt()
        var currentTime = baseTime

        for (i in 0 until loopCount) {
            // ループ変数を設定
            (context["vars"] as MutableMap<String, Any>)["index"] = i
            (context["vars"] as MutableMap<String, Any>)["iteration"] = i + 1

            // ボディのエフェクトを順次実行
            for (effect in effects) {
                effect.context = context
                currentTime = effect.execute(target, currentTime)
            }
        }

        return currentTime
    }

    override fun apply(target: Entity) {
        // 何もしない
    }
}

// RepeatEffect: 単一エフェクトを間隔をあけて繰り返し
class RepeatEffect(
    val countExpression: String,
    val delayExpression: String,
    val effect: Effect
) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val loopCount = evaluateExpression(countExpression).toInt()
        val delayTicks = parseTime(delayExpression)
        var currentTime = baseTime

        for (i in 0 until loopCount) {
            (context["vars"] as MutableMap<String, Any>)["index"] = i
            (context["vars"] as MutableMap<String, Any>)["iteration"] = i + 1

            currentTime = effect.execute(target, currentTime)
            currentTime += delayTicks  // 次の実行まで待機
        }

        return currentTime
    }

    override fun apply(target: Entity) {
        // 何もしない
    }
}
```

**実装のポイント:**
- `count` は動的に評価される（CEL式、変数、固定値）
- ループ内で `wait` を使用すると順次実行される
- ループ変数 `vars.index` と `vars.iteration` が自動的に設定される
- ネストしたループも可能（内側のループで `vars.index` が上書きされる）

**foreach内でのwait vs delay:**

foreachループ内でも`wait`と`delay`の違いは保たれます：

```yaml
# waitの場合: 前のエフェクト完了を待つ
Skills:
  - foreach{count=3;effects=[
      projectile{...},      # 2秒かかる発射物
      wait 1s,              # projectile完了（2秒）を待ってから1秒
      particle{type=FLAME}  # 合計3秒後に実行
    ]}
# 各ループ: 3秒間隔（0s, 3s, 6s）

# delayの場合: ループ開始時刻からの絶対時刻
Skills:
  - foreach{count=3;effects=[
      projectile{...},      # 2秒かかる発射物
      delay 1s,             # ループ開始+1秒（projectileの途中）
      particle{type=FLAME}  # 1秒後に実行（projectileより先）
    ]}
# 各ループ: 2秒間隔（0s, 2s, 4s、particleは0s+1s, 2s+1s, 4s+1s）
```

**動作の違い:**
- `wait 1s`: 前のエフェクト**完了後**1秒待機（累積的）
- `delay 1s`: ループ**開始時刻から**1秒後に実行（並列的）

**実装設計:**

```kotlin
class ForeachEffect(...) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        var currentTime = baseTime

        for (i in 0 until loopCount) {
            val loopStartTime = currentTime  // ループ開始時刻を記録

            // ループ開始時刻をコンテキストに保存
            (context["vars"] as MutableMap<String, Any>)["loopStartTime"] = loopStartTime

            for (effect in effects) {
                effect.context = context
                currentTime = effect.execute(target, currentTime)
            }
        }

        return currentTime
    }
}

class DelayEffect(val time: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val delayTicks = parseTime(time)

        // foreach内かどうかを判定
        if (context.containsKey("vars") &&
            (context["vars"] as Map<*, *>).containsKey("loopStartTime")) {
            // foreach内: ループ開始時刻からの遅延
            val loopStartTime = (context["vars"] as Map<*, *>)["loopStartTime"] as Long
            return loopStartTime + delayTicks
        } else {
            // 通常: トリガー時刻（0）からの遅延
            return delayTicks
        }
    }
}

class WaitEffect(val time: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val waitTicks = parseTime(time)
        // 常に前のエフェクト完了時刻からの遅延
        return baseTime + waitTicks
    }
}
```

**実用例:**

```yaml
# 連射攻撃（1秒間隔、前の発射を待たない）
Skills:
  - foreach{count=5;effects=[
      projectile{type=ARROW;speed=3.0},  # 発射
      delay 1s                            # 1秒後に次のループ
    ]}
# 結果: 0s, 1s, 2s, 3s, 4s で発射

# 確実な順次攻撃（発射完了を待つ）
Skills:
  - foreach{count=5;effects=[
      projectile{type=ARROW;speed=3.0},  # 発射（2秒で着弾）
      wait 1s                             # 着弾後1秒待機
    ]}
# 結果: 0s, 3s, 6s, 9s, 12s で発射
```

**注意点:**
- 大量のループは処理負荷が高い（目安：20回以内）
- `delay` なしで高速ループすると一瞬で全て実行される
- ループ変数は内側のループで上書きされる（ネスト時は注意）
- **foreach内のdelayはループ開始時刻基準**、通常のdelayはトリガー時刻基準

---

### foreachでコレクション操作

リストやマップをforeachで繰り返し処理できます。

**記法:**
```yaml
# リストを繰り返し
- foreach{in=リスト変数;effects=[...]}

# マップを繰り返し（キーのみ）
- foreach{in=マップ変数;mode=keys;effects=[...]}

# マップを繰り返し（値のみ）
- foreach{in=マップ変数;mode=values;effects=[...]}

# マップを繰り返し（キーと値の両方）
- foreach{in=マップ変数;mode=entries;effects=[...]}
```

**リスト/マップの定義:**
```yaml
# リストを定義
- set myList "[10, 20, 30, 40, 50]"

# マップを定義
- set myMap "{fire: 10, ice: 20, thunder: 30}"

# 複雑なリスト
- set damages "[
    cast.toInt(entity.health * 0.1),
    cast.toInt(nearbyPlayers.count * 5),
    cast.toInt(average.tps1m * 2)
  ]"
```

**使用例:**

```yaml
# リストの各要素でダメージ
Skills:
  - set damages "[10, 20, 30, 40, 50]"
  - foreach{in="vars.damages";effects=[
      damage{amount="vars.item"},
      particle{count="vars.item"},
      wait{time=1s}
    ]}
# vars.item: 各要素の値
# vars.index: インデックス（0, 1, 2, 3, 4）

# マップのキーを使用
Skills:
  - set elements "{fire: 10, ice: 20, thunder: 30}"
  - foreach{in="vars.elements";mode=keys;effects=[
      message{text="Element: {vars.key}"},
      wait{time=500ms}
    ]}
# vars.key: "fire", "ice", "thunder"

# マップの値を使用
Skills:
  - set elements "{fire: 10, ice: 20, thunder: 30}"
  - foreach{in="vars.elements";mode=values;effects=[
      damage{amount="vars.value"},
      wait{time=500ms}
    ]}
# vars.value: 10, 20, 30

# マップのエントリ（キーと値）を使用
Skills:
  - set elements "{fire: 10, ice: 20, thunder: 30}"
  - foreach{in="vars.elements";mode=entries;effects=[
      damage{amount="vars.value"},
      particle{type="vars.key.toUpperCase()"},
      wait{time=500ms}
    ]}
# vars.key: "fire", "ice", "thunder"
# vars.value: 10, 20, 30

# 動的リスト生成
Skills:
  - set count "nearbyPlayers.count"
  - set damages "[]"
  - foreach{count="vars.count";effects=[
      set damages "vars.damages + [10 + vars.index * 5]"
    ]}
  - foreach{in="vars.damages";effects=[
      damage{amount="vars.item"},
      wait{time=500ms}
    ]}
```

**ループ変数（コレクション版）:**
- **リストの場合:**
  - `vars.item`: 現在の要素
  - `vars.index`: インデックス（0始まり）

- **マップの場合（mode=keys）:**
  - `vars.key`: 現在のキー
  - `vars.index`: インデックス（0始まり）

- **マップの場合（mode=values）:**
  - `vars.value`: 現在の値
  - `vars.index`: インデックス（0始まり）

- **マップの場合（mode=entries）:**
  - `vars.key`: 現在のキー
  - `vars.value`: 現在の値
  - `vars.index`: インデックス（0始まり）

---

### 変数スコープ（Mob個体ごと）

変数はMob個体ごとに独立して保存されます。

**実装設計:**
```kotlin
// PacketMobに変数ストレージを追加
class PacketMob(...) {
    // 個体ごとの変数ストレージ
    private val variables = mutableMapOf<String, Any>()

    fun setVariable(name: String, value: Any) {
        variables[name] = value
    }

    fun getVariable(name: String): Any? {
        return variables[name]
    }

    fun getVariables(): Map<String, Any> {
        return variables.toMap()
    }
}

// SetEffect
class SetEffect(val varName: String, val expression: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val value = celEngine.evaluate(expression, context)

        // PacketMobの場合は個体に保存
        if (target is PacketMob) {
            target.setVariable(varName, value)
            // コンテキストにも反映
            if (!context.containsKey("vars")) {
                context["vars"] = mutableMapOf<String, Any>()
            }
            (context["vars"] as MutableMap<String, Any>)[varName] = value
        }

        return baseTime
    }
}

// スキル実行時にMobの変数を読み込み
class SkillExecutor {
    fun executeSkill(effects: List<Effect>, source: PacketMob, target: Entity) {
        val context = CELVariableProvider.buildEntityContext(source).toMutableMap()

        // Mobの変数をロード
        context["vars"] = source.getVariables().toMutableMap()

        // スキル実行...
    }
}
```

**変数の永続性:**
- Mob個体ごとに保存
- Mobが存在する限り保持
- Mob消滅時に削除
- スキル実行間で共有される

**例:**
```yaml
# カウンターを保持
Skills:
  - set attackCount "vars.attackCount != null ? vars.attackCount + 1 : 1"
  - message{text="Attack #{vars.attackCount}"}
  - damage{amount="10 + vars.attackCount * 2"}
```

---

### 変数の書き込みスコープ

変数の書き込み先を `type` パラメータで指定できます。

**記法:**
```yaml
# 自分の変数を設定（デフォルト - type省略可能）
- set varName "CEL式"
- set{type=self} varName "CEL式"
- set{type=caster} varName "CEL式"  # スキル発動者

# ターゲットの変数を設定（明示的に指定が必要）
- set{type=target} varName "CEL式"

# トリガーエンティティの変数を設定（onDamagedなどで攻撃者）
- set{type=trigger} varName "CEL式"

# グローバル変数（全Mob/Player共有、再起動で消える）
- set{type=global} varName "CEL式"

# ワールド共有変数（同じワールド内で共有）
- set{type=world} varName "CEL式"

# 位置ベース変数（特定座標に紐付け）
- set{type=location;x=100;y=64;z=200} varName "CEL式"
```

**スコープの詳細:**

1. **`self` / `caster` （デフォルト）:**
   - スキルを実行しているエンティティ（Mob自身またはプレイヤー）
   - `type` を省略した場合は `self` が使用されます
   - 変数はエンティティの消滅/ログアウトまで保持されます

2. **`target` （ターゲッター選択対象）:**
   - **重要**: `target` はターゲッターで選択されたエンティティを指します
   - 例: スキル定義で `@Nearest{range=10}` が指定されている場合、10m以内の最も近いエンティティが `target` になります
   - ターゲッターの種類:
     - `@Nearest{range=N}`: 最も近いエンティティ
     - `@All{range=N}`: 範囲内の全エンティティ
     - `@Random{range=N;count=M}`: ランダムM体
     - `@Self`: 自分自身（この場合 `target` == `self`）
   - **エンティティタイプフィルタ**（詳細は後述）:
     - `entityType=PLAYER`: プレイヤーのみ
     - `entityType=PACKET_MOB`: PacketMobのみ
     - `entityType=LIVING`: LivingEntityのみ（デフォルト）
     - `entityType=ENTITY`: 全てのEntity（明示的に指定が必要）
   - **ソート・制限機能**:
     - `sort=NEAREST`: 距離が近い順
     - `sort=FARTHEST`: 距離が遠い順
     - `sort=LOWEST_HEALTH`: HP低い順
     - `sort=HIGHEST_HEALTH`: HP高い順
     - `sort=LOWEST_ARMOR`: 防御力低い順
     - `sort=HIGHEST_ARMOR`: 防御力高い順
     - `sort=THREAT`: ヘイト値高い順
     - `sort=RANDOM`: ランダム
     - `limit=N`: 上位N件のみ取得
   - 使用例:
     - `@AP{r=20;sort=NEAREST;limit=3}`: 最も近い3名のプレイヤー
     - `@AL{r=30;sort=HIGHEST_HEALTH;limit=10}`: HP高い10体
     - `@AL{r=25;sort=LOWEST_ARMOR;limit=5}`: 防御力低い5体
   - ターゲットが存在しない場合、変数操作は警告ログを出力してスキップされます

3. **`trigger` （トリガー元エンティティ）:**
   - イベントをトリガーしたエンティティ（例: onDamagedで攻撃してきたプレイヤー）
   - 主にリアクティブなスキルで使用します

4. **`global` （グローバル変数）:**
   - サーバー全体で共有される変数
   - 全てのMob、Playerからアクセス可能
   - サーバー再起動まで保持

5. **`world` （ワールド変数）:**
   - 同じワールド内で共有される変数
   - ワールドごとに独立したストレージ

6. **`location` （位置変数）:**
   - 特定の座標に紐付いた変数
   - 座標ベースのギミック等に使用

**使用例:**

```yaml
# ターゲットにマークを付ける
Skills:
  - set{type=target} marked "true"
  - set{type=target} markTime "world.time"

# マークされたターゲットに追加ダメージ
Skills:
  - damage{amount="target.vars.marked ? 50 : 25"}

# ターゲットのカウンターを増やす
Skills:
  - set{type=target} hitCount "target.vars.hitCount != null ? target.vars.hitCount + 1 : 1"
  - damage{amount="10 * target.vars.hitCount"}

# 変数の受け渡し
Skills:
  - set myPower "50"
  - set{type=target} receivedPower "vars.myPower"
  - damage{amount="target.vars.receivedPower"}

# グローバル変数で全体攻撃カウント
Skills:
  - set{type=global} totalAttacks "global.vars.totalAttacks + 1"
  - message{text="Total attacks: {global.vars.totalAttacks}"}

# トリガー（攻撃者）にカウンター
Skills:
  - set{type=trigger} damageDealt "trigger.vars.damageDealt + 25"
```

**typeパラメータの種類:**

| type | 説明 | 変数の永続性 |
|------|------|------------|
| `self` / `caster` | 自分（デフォルト） | Mob消滅まで / Playerログアウトまで |
| `target` | ターゲット | ターゲットの消滅まで / ログアウトまで |
| `trigger` | トリガーエンティティ | トリガーの消滅まで / ログアウトまで |
| `global` | グローバル（全体共有） | サーバー再起動まで |
| `world` | ワールド共有 | サーバー再起動まで |
| `location` | 座標ベース | サーバー再起動まで |

**変数保持可能なエンティティ:**
- **PacketMob**: 可能（消滅時に変数も削除）
- **Player**: 可能（ログアウト時に変数も削除）
- **その他のEntity**: 不可（警告ログ出力）

**変数の永続性の詳細:**

1. **PacketMob の変数:**
   - Mob個体ごとに独立した変数ストレージを持つ
   - despawn()時に自動的に全変数がクリアされる
   - 同じ定義から生成された別のMobでも変数は共有されない
   - 例: Boss1とBoss2は同じ定義でも、それぞれ独立した変数を持つ

2. **Player の変数:**
   - プレイヤーごとに独立した変数ストレージを持つ（UUID基準）
   - ログアウト時に全変数が削除される
   - 再ログイン時は変数は空の状態から開始
   - スレッドセーフ（ConcurrentHashMap使用）

3. **Global 変数:**
   - サーバー全体で共有される変数
   - サーバー再起動まで保持される
   - 全てのMob、Playerからアクセス可能
   - 用途: 全体カウンター、イベント進行状態など

4. **World 変数:**
   - ワールドごとに独立した変数ストレージ
   - サーバー再起動まで保持される
   - 同じワールド内のすべてのエンティティからアクセス可能
   - 用途: ワールド別のイベント状態、ワールド固有のカウンターなど

5. **Location 変数:**
   - 特定の座標に紐付いた変数
   - サーバー再起動まで保持される
   - 座標ベースの状態管理に使用
   - 用途: 特定地点の状態、リスポーン地点の管理など

6. **変数が存在しない場合:**
   - `vars.変数名` でアクセスした場合、存在しなければ `null` を返す
   - CEL式では `!= null` でチェックしてから使用を推奨
   - 例: `vars.count != null ? vars.count + 1 : 1`

7. **ターゲットが変数を保持できない場合:**
   - `set{type=target}` でターゲットがPlayer/PacketMobでない場合、警告ログが出力される
   - 変数は保存されず、後続のスキルで `target.vars.変数名` は `null` を返す
   - スキル実行は継続される（エラーにはならない）

**実装設計:**

**1. PacketMob の変数ストレージ（PacketMob.kt）:**
```kotlin
// PacketMobクラス内
private val variables = mutableMapOf<String, Any>()

fun setVariable(name: String, value: Any) {
    variables[name] = value
}

fun getVariable(name: String): Any? = variables[name]

fun getVariables(): Map<String, Any> = variables.toMap()

fun clearVariables() = variables.clear()

// despawn時に自動的にクリア
override suspend fun despawn(player: Player) {
    // ... despawn処理
    clearVariables()  // 変数をクリア
}
```

**2. Player の変数ストレージ（Extensions.kt）:**
```kotlin
// Player専用の変数ストレージ（ログアウト時にクリア）
private val playerVariables = ConcurrentHashMap<UUID, MutableMap<String, Any>>()

fun Entity.setVariable(name: String, value: Any) {
    when (this) {
        is Player -> {
            playerVariables.getOrPut(this.uniqueId) { mutableMapOf() }[name] = value
        }
        else -> {
            DebugLogger.warn("Entity ${this.type} cannot hold variables")
        }
    }
}

fun Entity.getVariable(name: String): Any? {
    return when (this) {
        is Player -> playerVariables[this.uniqueId]?.get(name)
        else -> null
    }
}

fun Player.clearVariablesOnLogout() {
    playerVariables.remove(this.uniqueId)
}
```

**3. ログアウト時のクリア（Listener）:**
```kotlin
@EventHandler
fun onPlayerQuit(event: PlayerQuitEvent) {
    event.player.clearVariablesOnLogout()
}
```

**変数アクセスまとめ:**
- `vars.変数名`: 自分の変数
- `target.vars.変数名`: ターゲットの変数
- `trigger.vars.変数名`: トリガーエンティティの変数
- `global.vars.変数名`: グローバル変数
- `world.vars.変数名`: ワールド共有変数
- `set varName "式"`: 自分の変数を設定
- `set{type=target} varName "式"`: ターゲットの変数を設定

---

### 変数格納可能性のチェック

エンティティが変数を保持できるかどうかを事前にチェックできます。これにより、変数操作が失敗する前に条件分岐が可能です。

**利用可能なCEL変数:**

```yaml
entity.canHoldVariables     # 自分が変数を保持できるか（PacketMob/Playerならtrue）
target.canHoldVariables     # ターゲットが変数を保持できるか
trigger.canHoldVariables    # トリガーエンティティが変数を保持できるか
```

**使用例:**

```yaml
# ターゲットが変数を保持できる場合のみ変数を設定
Skills:
  - condition: "target.canHoldVariables"
    effects:
      - set{type=target} marked "true"
      - set{type=target} markTime "world.time"

# 条件式での利用
Skills:
  - damage{amount="target.canHoldVariables && target.vars.marked ? 50 : 25"}

# 複数条件の組み合わせ
Skills:
  - condition: "target.canHoldVariables && entity.health > 50"
    effects:
      - inc{type=target} hitCount
      - damage{amount="10 * target.vars.hitCount"}

# 変数保持不可のターゲットへのフォールバック
Skills:
  - condition: "target.canHoldVariables"
    effects:
      - set{type=target} debuffLevel "3"
      - damage{amount="20 + target.vars.debuffLevel * 5"}

  - condition: "!target.canHoldVariables"
    effects:
      - damage{amount="35"}  # 固定ダメージ
```

**対応エンティティ:**
- `true`: PacketMob、Player
- `false`: その他のエンティティ（Zombie、Skeleton、Cow など）

**実装詳細:**
```kotlin
// Extensions.kt
fun Entity.canHoldVariables(): Boolean {
    return this is Player
}

fun PacketMob.canHoldVariables(): Boolean = true

// CELVariableProvider.kt
internal fun buildEntityInfo(entity: Entity): Map<String, Any> {
    val info = mutableMapOf<String, Any>()
    // ...
    info["canHoldVariables"] = entity.canHoldVariables()
    // ...
}
```

---

### ターゲッターのエンティティタイプフィルタ

ターゲッターに `entityType` パラメータを指定することで、対象とするエンティティのタイプを絞り込めます。

**記法:**
```yaml
@Nearest{range=10;entityType=PLAYER}         # プレイヤーのみ
@All{range=20;entityType=PACKET_MOB}         # PacketMobのみ
@Random{range=15;count=3;entityType=LIVING}  # LivingEntityのみ
```

**利用可能なエンティティタイプ:**

1. **`PLAYER`**: プレイヤーのみを対象
   ```yaml
   # プレイヤーのみに攻撃
   - damage{amount=20} @Nearest{range=10;entityType=PLAYER}
   ```

2. **`PACKET_MOB`**: PacketMob（カスタムMob）のみを対象
   ```yaml
   # PacketMobのみを対象にバフ
   - potion{type=SPEED;duration=100} @All{range=15;entityType=PACKET_MOB}
   ```

3. **`LIVING`**: LivingEntity（生物）のみを対象
   - プレイヤー、Mob、PacketMobなど全ての生物が含まれる
   - **デフォルト**: `entityType` を省略した場合は `LIVING` として扱われる
   ```yaml
   # 全ての生物に範囲ダメージ
   - damage{amount=15} @All{range=10;entityType=LIVING}

   # 省略形（同じ意味）
   - damage{amount=15} @All{range=10}
   ```

4. **`ENTITY`**: 全てのEntity（生物以外も含む）
   - アイテムスタンド、アーマースタンド、落ちているアイテムなども含む
   - LivingEntityでない場合のみ選択される（LivingEntityが優先）
   ```yaml
   # 全てのエンティティに適用（アイテムスタンドなども含む）
   - particle{type=FLAME;count=10} @All{range=20;entityType=ENTITY}
   ```

**フィルタリングの優先順位:**

`entityType` を省略した場合、以下の順序でチェックされます：

1. **LivingEntity** を優先的に検索
2. LivingEntityが見つからない場合、**Entity** を検索

```yaml
# entityType省略 → LivingEntityを優先
- damage{amount=20} @Nearest{range=10}
# → プレイヤー、Mob、PacketMobなどを優先的に選択
# → LivingEntityがいない場合のみ、アイテムスタンドなどを選択
```

**使用例:**

```yaml
# プレイヤーのみに通知
PlayerOnlySkill:
  - message{text="Player detected!"} @All{range=30;entityType=PLAYER}

# PacketMobのみを強化
PacketMobBuffSkill:
  - potion{type=STRENGTH;duration=200;amplifier=2} @All{range=20;entityType=PACKET_MOB}
  - particle{type=ANGRY_VILLAGER;count=10} @All{range=20;entityType=PACKET_MOB}

# プレイヤーとPacketMob両方に効果（複数スキル）
HybridSkill:
  - damage{amount=20} @Nearest{range=10;entityType=PLAYER}
  - damage{amount=15} @Nearest{range=10;entityType=PACKET_MOB}

# LivingEntity全体に範囲攻撃
AreaAttack:
  - damage{amount=10} @All{range=15;entityType=LIVING}
  - particle{type=EXPLOSION;count=30} @All{range=15}

# 変数を保持できるエンティティのみ（Player + PacketMob）
VariableTargetSkill:
  - condition: "target.canHoldVariables"
    effects:
      - inc{type=target} hitCount
      - damage{amount="10 * target.vars.hitCount"} @Nearest{range=10;entityType=PLAYER}

  - damage{amount=20} @Nearest{range=10;entityType=PACKET_MOB;cond=target.canHoldVariables}
```

**実装設計:**
```kotlin
// Targeter基底クラス
abstract class Targeter {
    enum class EntityTypeFilter {
        PLAYER,       // Playerのみ
        PACKET_MOB,   // PacketMobのみ
        LIVING,       // LivingEntityのみ（デフォルト）
        ENTITY        // 全てのEntity
    }

    var entityType: EntityTypeFilter = EntityTypeFilter.LIVING

    fun getTargets(source: Entity): List<Entity> {
        val candidates = getCandidates(source)

        // entityTypeでフィルタリング
        val filtered = candidates.filter { entity ->
            when (entityType) {
                EntityTypeFilter.PLAYER -> entity is Player
                EntityTypeFilter.PACKET_MOB -> entity is PacketMob
                EntityTypeFilter.LIVING -> entity is LivingEntity
                EntityTypeFilter.ENTITY -> {
                    // LivingEntityを優先、見つからない場合はEntity全般
                    if (candidates.any { it is LivingEntity }) {
                        entity is LivingEntity
                    } else {
                        true
                    }
                }
            }
        }

        return filtered
    }

    abstract fun getCandidates(source: Entity): List<Entity>
}
```

---

### ターゲッターのソート・制限機能

ターゲッターで複数のターゲットを取得する際、ソート順序と取得件数を制限できます。

**記法:**
```yaml
@Targeter{range=N;sort=MODE;limit=M}

# 例:
@AP{r=20;sort=NEAREST;limit=3}              # 最も近い3名のプレイヤー
@AL{r=30;sort=HIGHEST_HEALTH;limit=10}      # HP高い10体のLiving
@AL{r=25;sort=LOWEST_ARMOR;limit=5}         # 防御力低い5体
@AP{r=15;sort=LOWEST_HEALTH;limit=3}        # HP低い3名のプレイヤー
@AL{r=20;sort=THREAT;limit=5}               # ヘイト値上位5名
@AP{r=20;sort=RANDOM;limit=5}               # ランダム5名
```

**利用可能なソートモード:**

1. **`NONE`**: ソートなし（デフォルト）
2. **`NEAREST`**: 距離が近い順
3. **`FARTHEST`**: 距離が遠い順
4. **`LOWEST_HEALTH`**: HP低い順
5. **`HIGHEST_HEALTH`**: HP高い順
6. **`THREAT`**: ヘイト値高い順
7. **`RANDOM`**: ランダム
8. **`CUSTOM`**: CEL式でカスタムソート（最も拡張性が高い！）

**`CUSTOM`ソートモード（CEL式使用）:**
- `sortExpression` パラメータでソート基準を指定
- 計算結果が小さい順にソート（昇順）
- マイナス記号 `-` で降順にできる
- 利用可能な変数:
  - `target.health`, `target.maxHealth`
  - `target.armor`, `target.armorToughness`
  - `target.attackDamage`, `target.attackSpeed`
  - `target.knockbackResistance`, `target.movementSpeed`
  - その他全てのCEL変数

```yaml
# 基本的な使用
@AL{r=20;sort=CUSTOM;sortExpression="target.armor";limit=5}        # 防御力低い5体
@AL{r=20;sort=CUSTOM;sortExpression="-target.armor";limit=3}       # 防御力高い3体（降順）

# 複雑な計算
@AL{r=20;sort=CUSTOM;sortExpression="target.health / target.maxHealth";limit=5}  # HP割合低い5体
@AL{r=25;sort=CUSTOM;sortExpression="-(target.armor + target.health)";limit=3}   # タンク性能高い3体

# 距離との組み合わせ
@AL{r=30;sort=CUSTOM;sortExpression="distance.between(entity.location, target.location) * target.armor"}
```

**`offset`パラメータ:**
- ソート後の先頭からN件をスキップ
- デフォルト: 0（スキップなし）
- 例: `offset=1` で1番目をスキップ（2番目から取得）

**`limit`パラメータ:**
- ソート後の上位N件のみを取得
- 省略した場合は全てのターゲットを取得
- 例: `limit=3` で上位3件のみ

**`offset`と`limit`の組み合わせ:**
```yaml
@AP{r=20;sort=NEAREST;offset=1;limit=2}  # 2番目と3番目に近いプレイヤー
@AL{r=25;sort=LOWEST_HEALTH;offset=1;limit=3}  # HP低い順で2位～4位
@AP{r=20;sort=NEAREST;offset=1}  # 最も近いプレイヤーを除く全員
```

**実用例:**

```yaml
# ボススキル：防御力低い敵を狙う貫通攻撃（CEL式）
BossPiercingAttack:
  Skills:
    - damage{amount=50;ignoreInvulnerability=true} @AL{r=20;sort=CUSTOM;sortExpression="target.armor";limit=5}
  # 防御力が低い5体に貫通ダメージ

# ヒーラースキル：HP割合が低い味方を優先回復
HealerPriority:
  Skills:
    - heal{amount=20} @AP{r=15;sort=CUSTOM;sortExpression="target.health / target.maxHealth";limit=3}
  # HP割合が最も低い3名を回復

# AOEスキル：最も近い10体に範囲ダメージ
AOENearbyTargets:
  Skills:
    - damage{amount=15} @AL{r=12;sort=NEAREST;limit=10}
    - particle{type=EXPLOSION_LARGE;count=20}
  # 近くの10体に範囲ダメージ

# タンク優先攻撃：最もタンク性能が高い敵を攻撃
TankBuster:
  Skills:
    - damage{amount=40} @AL{r=15;sort=CUSTOM;sortExpression="-(target.armor + target.health)";limit=1}
  # 防御力+HPが最も高い敵に大ダメージ

# マルチターゲット：ランダムに選択
MultiTargetRandom:
  Skills:
    - foreach{
        targeter: @AP{r=20;sort=RANDOM;limit=5},
        effects: [
          damage{amount=10},
          particle{type=CRIT;count=5}
        ]
      }
  # ランダムに選んだ5名に連続攻撃

# 連鎖攻撃：1番目を除く2～4番目に近い敵
ChainAttackSkip:
  Skills:
    - damage{amount=30} @AL{r=25;sort=NEAREST;offset=1;limit=3}
  # 最も近い敵をスキップして、2～4番目に攻撃

# 攻撃力が高い順に3体にデバフ
AttackPowerDebuff:
  Skills:
    - potion{type=WEAKNESS;duration=200} @AL{r=20;sort=CUSTOM;sortExpression="-target.attackDamage";limit=3}
  # 攻撃力が高い3体に弱体化
```

**組み合わせ例:**

```yaml
# 条件 + ソート + 制限
LowHealthNearestThree:
  Skills:
    - heal{amount=30} @AL{r=20;cond=LowHP;sort=NEAREST;limit=3}
  # HP50%以下で、最も近い3体を回復

# エンティティタイプ + ソート + 制限
PlayerHighArmorTargets:
  Skills:
    - damage{amount=50} @AP{r=25;entityType=PLAYER;sort=HIGHEST_ARMOR;limit=3}
  # プレイヤーのみ、防御力高い3名にダメージ
```

**実装詳細:**
```kotlin
enum class TargetSortMode {
    NONE, NEAREST, FARTHEST,
    LOWEST_HEALTH, HIGHEST_HEALTH,
    LOWEST_ARMOR, HIGHEST_ARMOR,
    THREAT, RANDOM
}

class RadiusPlayersTargeter(
    private val range: Double,
    private val sortMode: TargetSortMode = TargetSortMode.NONE,
    private val limit: Int? = null
) : Targeter() {
    // ソート・制限処理
}
```

---

### ダメージエフェクトの高度なオプション

ダメージエフェクトに無敵時間とノックバック耐性を制御するオプションを追加できます。

**記法:**
```yaml
- damage{amount=20;ignoreInvulnerability=true}           # 無敵時間を無視
- damage{amount=30;ignoreKnockbackResistance=true}       # ノックバック耐性を無視（KB強制発生）
- damage{amount=25;preventKnockback=true}                # ノックバックを完全に封じる
- damage{amount=40;ignoreInvulnerability=true;ignoreKnockbackResistance=true}  # 両方無視
```

**オプション:**

1. **`ignoreInvulnerability`**: 無敵時間を無視
   - **デフォルト**: `false`
   - `true` の場合、ターゲットの無敵時間（NoDamageTicks）を一時的に0に設定してダメージを与える
   - ダメージ適用後、元の無敵時間に戻す

   ```yaml
   # 連続ヒット攻撃（無敵時間を無視）
   RapidAttack:
     - foreach{count=5;effects=[
         damage{amount=5;ignoreInvulnerability=true},
         particle{type=CRIT;count=10},
         wait{time=100ms}
       ]}
   ```

2. **`ignoreKnockbackResistance`**: ノックバック耐性を無視（ノックバックを強制的に発生）
   - **デフォルト**: `false`
   - `true` の場合、ターゲットのKNOCKBACK_RESISTANCE属性を一時的に0に設定
   - ダメージ適用後、元の値に戻す
   - ノックバックを強制的に発生させたい場合に使用

   ```yaml
   # 強制ノックバック攻撃
   ForceKnockback:
     - damage{amount=20;ignoreKnockbackResistance=true}
     - particle{type=SWEEP_ATTACK;count=20}
   ```

3. **`preventKnockback`**: ノックバックを完全に封じる
   - **デフォルト**: `false`
   - `true` の場合、ダメージ適用前のvelocity（速度）を保存し、ダメージ適用後に復元
   - **処理順序**: `ignoreKnockbackResistance` の後に処理されるため、両方を指定した場合は `preventKnockback` が優先される
   - ノックバックを発生させずにダメージだけを与えたい場合に使用

   ```yaml
   # ノックバックなしダメージ（スタン攻撃など）
   StunAttack:
     - damage{amount=15;preventKnockback=true}
     - potion{type=SLOW;duration=60;amplifier=2}
   ```

4. **両方を組み合わせ:**
   ```yaml
   # 完全無視の強力な攻撃
   TrueStrike:
     - damage{amount=50;ignoreInvulnerability=true;ignoreKnockbackResistance=true}
     - sound{type=ENTITY_LIGHTNING_BOLT_IMPACT;volume=2.0}
   ```

**使用例:**

```yaml
# 連続ヒットコンボ
ComboAttack:
  - damage{amount=10;ignoreInvulnerability=true}
  - wait{time=50ms}
  - damage{amount=15;ignoreInvulnerability=true}
  - wait{time=50ms}
  - damage{amount=20;ignoreInvulnerability=true}
  - particle{type=SWEEP_ATTACK;count=30}

# ボス用の貫通攻撃（防具無視＋無敵時間無視）
BossPiercingAttack:
  - damage{amount=100;ignoreInvulnerability=true;ignoreKnockbackResistance=true}
  - particle{type=EXPLOSION;count=50}
  - sound{type=ENTITY_ENDER_DRAGON_HURT;volume=2.0}

# ノックバック重視の攻撃
KnockbackSlam:
  - damage{amount=15;ignoreKnockbackResistance=true}
  - particle{type=CLOUD;count=40}
  - sound{type=ENTITY_IRON_GOLEM_ATTACK}

# 高速連打攻撃（DPS重視）
RapidFire:
  - foreach{count=10;effects=[
      damage{amount=3;ignoreInvulnerability=true},
      particle{type=FLAME;count=5},
      wait{time=50ms}
    ]}
```

**実装設計:**
```kotlin
class DamageEffect(
    val amount: String,
    val ignoreInvulnerability: Boolean = false,
    val ignoreKnockbackResistance: Boolean = false
) : Effect() {

    override fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        // ダメージ量を評価
        val damageAmount = evaluateDamage(amount, context)

        // 無敵時間を一時的に無視
        val originalNoDamageTicks = if (ignoreInvulnerability) {
            val ndt = target.noDamageTicks
            target.noDamageTicks = 0
            ndt
        } else null

        // ノックバック耐性を一時的に無視
        val originalKBResistance = if (ignoreKnockbackResistance) {
            val attr = target.getAttribute(Attribute.KNOCKBACK_RESISTANCE)
            val original = attr?.value ?: 0.0
            attr?.baseValue = 0.0
            original
        } else null

        try {
            // ダメージを適用
            target.damage(damageAmount, source)
        } finally {
            // 元の値に戻す
            originalNoDamageTicks?.let { target.noDamageTicks = it }
            originalKBResistance?.let {
                target.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = it
            }
        }
    }
}
```

**注意事項:**

1. **無敵時間の無視**:
   - 連続してダメージを与えられるため、DPSが大幅に上昇します
   - バランス調整が重要です

2. **ノックバック耐性の無視**:
   - エンダードラゴンやウィザーなど、通常ノックバックしない敵も吹き飛ばせます
   - ボス戦のギミックとして有効です

3. **パフォーマンス**:
   - アトリビュート変更は一時的なため、パフォーマンスへの影響は最小限です
   - `finally` ブロックで確実に元に戻すため、エラーが発生しても安全です

---

### 変数の編集操作（簡略記法）

変数の値を簡単に編集するための専用エフェクトを提供しています。`set` を使った冗長な記述を簡略化できます。

**記法:**
```yaml
# 増減操作
- inc 変数名           # 変数を1増やす（存在しない場合は1に設定）
- dec 変数名           # 変数を1減らす（存在しない場合は-1に設定）

# 四則演算
- add 変数名 値       # 変数に値を加算
- sub 変数名 値       # 変数から値を減算
- mul 変数名 値       # 変数に値を乗算
- div 変数名 値       # 変数を値で除算
- mod 変数名 値       # 変数を値で剰余

# スコープ指定（set と同様）
- inc{type=target} 変数名
- add{type=global} 変数名 値
- mul{type=trigger} 変数名 値
```

**使用例:**

```yaml
# カウンター（従来）
Skills:
  - set count "vars.count != null ? vars.count + 1 : 1"

# カウンター（簡略版）
Skills:
  - inc count

# ダメージ累積（従来）
Skills:
  - set totalDamage "vars.totalDamage != null ? vars.totalDamage + 25 : 25"

# ダメージ累積（簡略版）
Skills:
  - add totalDamage 25

# HP基準のスケーリング（従来）
Skills:
  - set scaledDamage "10"
  - set scaledDamage "vars.scaledDamage * (entity.health / entity.maxHealth)"

# HP基準のスケーリング（簡略版）
Skills:
  - set scaledDamage "10"
  - mul scaledDamage "entity.health / entity.maxHealth"

# ターゲットのヒットカウント
Skills:
  - inc{type=target} hitCount
  - damage{amount="target.vars.hitCount * 10"}

# グローバルカウンター
Skills:
  - inc{type=global} totalKills
  - message{text="Total kills: {global.vars.totalKills}"}

# 複雑な計算の組み合わせ
Skills:
  - set baseDamage "20"
  - mul baseDamage "nearbyPlayers.count"     # プレイヤー数で倍率
  - add baseDamage "toInt(entity.health)"    # HPを加算
  - div baseDamage "2"                        # 半分にする
  - damage{amount="vars.baseDamage"}
```

**動作仕様:**

1. **変数が存在しない場合:**
   - `inc`: 1に設定
   - `dec`: -1に設定
   - `add/sub/mul/div/mod`: 演算の単位元を仮定（add/subは0、mul/divは1）

2. **型の扱い:**
   - 数値演算は自動で型を維持（Int同士ならInt、DoubleならDouble）
   - 型が異なる場合は自動でDoubleに昇格
   - 必要に応じて `toInt()` などで明示的に変換

3. **値の部分にCEL式を使用可能:**
   - `add count "nearbyPlayers.count"`
   - `mul damage "entity.health / entity.maxHealth"`

4. **スコープ指定:**
   - `type` パラメータで変数の保存先を指定（`set` と同様）
   - 未指定の場合は `self`（自分）

5. **型の不一致による警告:**
   - **数値演算 (inc/dec/mul/div/mod)**: 変数が数値でない場合、警告ログを出力し、演算をスキップ
     - 例: `inc count` で `count` が `"text"` の場合 → 警告: `Cannot perform numeric operation on non-numeric value: text`

   - **String型への特別な動作 (add/sub)**:
     - `add`: 文字列連結として動作（警告なし）
       - `add message "神"` で `message` が `"hello"` → `"hello神"`
     - `sub`: 文字列から部分文字列を削除（警告なし）
       - `sub message "lo"` で `message` が `"hello"` → `"hel"`

   - **List型への特別な動作 (add/sub)**:
     - `add`: リストに要素を追加（警告なし）
       - `add items "sword"` で `items` が `["bow"]` → `["bow", "sword"]`
     - `sub`: リストから要素を削除（警告なし）
       - `sub items "bow"` で `items` が `["bow", "sword"]` → `["sword"]`

   - **Map型への特別な動作 (add/sub)**:
     - `add`: マップにエントリを追加（値はMapまたはキー:値ペア）
       - `add stats {power: 10}` で `stats` が `{hp: 100}` → `{hp: 100, power: 10}`
     - `sub`: マップからキーを削除
       - `sub stats "hp"` で `stats` が `{hp: 100, power: 10}` → `{power: 10}`

**実装設計:**
```kotlin
// IncEffect: 変数を1増やす
class IncEffect(val varName: String, val type: VariableType = VariableType.SELF) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val current = getVariable(target, type, varName) as? Number
        val newValue = (current?.toInt() ?: 0) + 1
        setVariable(target, type, varName, newValue)
        return baseTime
    }
}

// AddEffect: 変数に値を加算
class AddEffect(
    val varName: String,
    val expression: String,
    val type: VariableType = VariableType.SELF
) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val current = getVariable(target, type, varName) as? Number ?: 0
        val value = celEngine.evaluate(expression, context) as Number
        val newValue = current.toDouble() + value.toDouble()
        setVariable(target, type, varName, newValue)
        return baseTime
    }
}

// MulEffect, DivEffect なども同様
```

---

### 型付き変数操作（型安全記法）

変数名に型情報を含めることで、型安全な変数操作が可能です。パーサーが自動的に型を判定し、適切な操作を行います。

**記法:**
```yaml
# 基本形式（self省略）
- set "変数名.型" 値
- add "変数名.型" 値
- sub "変数名.型" 値

# 完全なスコープ指定
- set "self.vars.変数名.型" 値
- set "target.vars.変数名.型" 値
- set "global.vars.変数名.型" 値
```

**スコープの省略規則:**

1. **`self` スコープはデフォルト（省略可能）:**
   ```yaml
   # これらは同じ意味
   - set "test.toInt" "0"
   - set "self.vars.test.toInt" "0"

   # これらも同じ意味
   - inc count
   - inc "self.vars.count"
   ```

2. **`target` スコープは明示的に指定が必要:**
   ```yaml
   # ターゲットの変数を操作する場合
   - set "target.vars.hitCount.toInt" "0"
   - inc "target.vars.hitCount"

   # または type パラメータで指定
   - set{type=target} "hitCount.toInt" "0"
   - inc{type=target} hitCount
   ```

3. **`target` はターゲッターで選択された対象:**
   - `target` はスキル実行時にターゲッターで選択されたエンティティを指します
   - 例: `@Nearest{range=10}` で選択された最も近いエンティティ
   - ターゲットが存在しない場合、`target.vars.*` は `null` を返します

4. **その他のスコープも明示的に指定:**
   ```yaml
   - set "global.vars.totalKills.toInt" "0"    # グローバル変数
   - set "trigger.vars.damageDealt.toInt" "0"  # トリガーエンティティ
   - set "world.vars.eventPhase.toInt" "1"     # ワールド変数
   ```

**対応型:**
- `.toInt`: Int型として扱う
- `.toDouble`: Double型として扱う
- `.toString`: String型として扱う
- `.toList`: List型として扱う
- `.toMap`: Map型として扱う
- `.toBoolean`: Boolean型として扱う

**使用例:**

```yaml
# Int型変数
Skills:
  - set "self.vars.test.toInt" "0"
  - add "self.vars.test.toInt" "1"
  - inc "test.toInt"
  - damage{amount="vars.test"}  # → 2

# String型変数（文字列連結）
Skills:
  - set "self.vars.message.toString" "Hello"
  - add "self.vars.message.toString" " World"
  - add "message.toString" "神"
  - message{text="{vars.message}"}  # → "Hello World神"

# List型変数
Skills:
  - set "self.vars.items.toList" "[]"
  - add "self.vars.items.toList" "sword"
  - add "items.toList" "bow"
  - add "items.toList" "arrow"
  # vars.items → ["sword", "bow", "arrow"]

# Map型変数
Skills:
  - set "self.vars.stats.toMap" "{}"
  - add "self.vars.stats.toMap" "{hp: 100}"
  - add "stats.toMap" "{mp: 50}"
  - add "stats.toMap" "{power: 10}"
  # vars.stats → {hp: 100, mp: 50, power: 10}

# スコープ指定付き
Skills:
  - set "target.vars.debuff.toList" "[]"
  - add "target.vars.debuff.toList" "poison"
  - add "target.vars.debuff.toList" "weakness"
  # target.vars.debuff → ["poison", "weakness"]

# グローバル変数
Skills:
  - set "global.vars.ranking.toMap" "{}"
  - add "global.vars.ranking.toMap" "{player1: 1000}"
  - add "global.vars.ranking.toMap" "{player2: 950}"
  # global.vars.ranking → {player1: 1000, player2: 950}
```

**詳細な動作:**

1. **Int/Double型:**
   - `set`: 数値を設定
   - `add`: 加算
   - `sub`: 減算
   - `mul`: 乗算
   - `div`: 除算
   - `mod`: 剰余
   - `inc`: 1増加
   - `dec`: 1減少

2. **String型:**
   - `set`: 文字列を設定
   - `add`: 文字列連結（末尾に追加）
   - `sub`: 部分文字列を削除

3. **List型:**
   - `set`: リストを設定（`[]` または `[item1, item2, ...]`）
   - `add`: 要素を追加
   - `sub`: 要素を削除（最初に見つかった一致要素）

4. **Map型:**
   - `set`: マップを設定（`{}` または `{key: value, ...}`）
   - `add`: エントリを追加/更新（既存キーは上書き）
   - `sub`: キーを削除

**型推論:**

型指定がない場合、既存の変数の型を推論します。

```yaml
# 型推論例
Skills:
  - set count "0"              # 型推論: Int
  - inc count                  # OK: Int型として扱う

  - set message "hello"        # 型推論: String
  - add message " world"       # OK: String型として文字列連結

  - set items "[1, 2, 3]"      # 型推論: List
  - add items "4"              # OK: List型として要素追加
```

**型変換との組み合わせ:**

```yaml
# チェーン型変換と組み合わせ
Skills:
  - set damage "entity.health.toInt"
  - mul damage "2"
  - add damage "nearbyPlayers.count.toInt"
  - damage{amount="vars.damage"}

# 複雑な型操作
Skills:
  - set raw "entity.health / entity.maxHealth * 100"  # Double
  - set "percent.toInt" "vars.raw.toInt"              # Int型で保存
  - add "percent.toInt" "10"                           # Int加算
  - set "message.toString" "HP: {vars.percent}%"      # String型で保存
```

**実装設計:**
```kotlin
// SetEffect の拡張: 型付き変数名のパース
class SetEffect(val varPath: String, val expression: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        // 変数名から型情報を抽出
        // "self.vars.test.toInt" → scope=self, varName=test, type=Int
        val (scope, varName, type) = parseTypedVarPath(varPath)

        // CEL式を評価
        val rawValue = celEngine.evaluate(expression, context)

        // 型に応じて変換
        val typedValue = when (type) {
            "toInt" -> convertToInt(rawValue)
            "toDouble" -> convertToDouble(rawValue)
            "toString" -> rawValue.toString()
            "toList" -> convertToList(rawValue)
            "toMap" -> convertToMap(rawValue)
            else -> rawValue
        }

        // スコープに応じて保存
        setVariable(target, scope, varName, typedValue)

        return baseTime
    }
}

// AddEffect の拡張: 型に応じた加算
class AddEffect(val varPath: String, val expression: String) : Effect() {
    override fun execute(target: Entity, baseTime: Long): Long {
        val (scope, varName, type) = parseTypedVarPath(varPath)
        val current = getVariable(target, scope, varName)
        val value = celEngine.evaluate(expression, context)

        val newValue = when {
            current is Number && value is Number -> current.toDouble() + value.toDouble()
            current is String && value is String -> current + value
            current is List<*> -> (current as MutableList).apply { add(value) }
            current is Map<*, *> && value is Map<*, *> -> (current as MutableMap).apply { putAll(value) }
            else -> {
                DebugLogger.warn("Cannot add $value to $current")
                current
            }
        }

        setVariable(target, scope, varName, newValue)
        return baseTime
    }
}
```

---

### CEL変数リファレンス（完全版）

スキルやコンディションで使用できる全てのCEL変数の一覧です。

**1. エンティティ情報 (`entity`, `target`, `trigger`)**

```yaml
# 基本情報
entity.type                 # エンティティタイプ名（例: "ZOMBIE", "PLAYER"）
entity.uuid                 # UUID（文字列）
entity.isDead               # 死亡しているか（Boolean）
entity.age                  # 存在時間（tick）
entity.canHoldVariables     # 変数を保持できるか（Boolean: PacketMob/Playerのみtrue）

# 位置情報
entity.location.x           # X座標（Double）
entity.location.y           # Y座標（Double）
entity.location.z           # Z座標（Double）
entity.location.yaw         # 向き（0-360度、Double）
entity.location.pitch       # 仰角（-90～90度、Double）
entity.location.world       # ワールド名（String）

# LivingEntity固有（Mob、Playerなど）
entity.health               # 現在のHP（Double）
entity.maxHealth            # 最大HP（Double）
entity.armor                # 防御力（Double）
entity.armorToughness       # 防具強度（Double）
entity.attackDamage         # 攻撃力（Double）
entity.attackSpeed          # 攻撃速度（Double）
entity.knockbackResistance  # ノックバック耐性（Double）
entity.movementSpeed        # 移動速度（Double）

# Player固有（エンティティがPlayerの場合のみ）
entity.name                 # プレイヤー名（String）
entity.level                # レベル（Int）
entity.exp                  # 経験値（Float）
entity.foodLevel            # 食料レベル（Int）
entity.gameMode             # ゲームモード（String: "SURVIVAL", "CREATIVE"など）
entity.isFlying             # 飛行中か（Boolean）
entity.isSneaking           # スニーク中か（Boolean）
entity.isSprinting          # ダッシュ中か（Boolean）

# ターゲット・トリガー（同じ構造）
target.health               # ターゲットのHP
target.canHoldVariables     # ターゲットが変数を保持できるか
trigger.type                # トリガーエンティティのタイプ
```

**2. ワールド情報 (`world`)**

```yaml
world.name                  # ワールド名（String）
world.time                  # ワールド時刻（0-24000、Long）
world.fullTime              # フル時刻（Long）
world.isDay                 # 昼か（Boolean）
world.isNight               # 夜か（Boolean）
world.hasStorm              # 雨が降っているか（Boolean）
world.isThundering          # 雷雨か（Boolean）
world.difficulty            # 難易度（String: "PEACEFUL", "EASY", "NORMAL", "HARD"）
world.playerCount           # ワールド内のプレイヤー数（Int）
```

**3. 環境情報 (`environment`)**

```yaml
environment.moonPhase       # 月の満ち欠け（0-7、Int）
environment.dayOfCycle      # 日数（Int）
environment.tickOfDay       # 時刻（0-24000、Int）
environment.biome           # バイオーム名（String）
```

**4. 近くのプレイヤー情報 (`nearbyPlayers`)**

```yaml
nearbyPlayers.count         # 近くのプレイヤー数（Int）
nearbyPlayers.avgLevel      # 平均レベル（Double）
nearbyPlayers.maxLevel      # 最高レベル（Int）
nearbyPlayers.minLevel      # 最低レベル（Int）
```

**5. サーバー情報 (`current`, `average`)**

```yaml
# 現在のTPS
current.tps                 # 現在のTPS（Double、0.0-20.0）

# 平均TPS
average.tps                 # デフォルト平均TPS（Double、通常は1分平均）
average.tps1m               # 1分間の平均TPS（Double）
average.tps5m               # 5分間の平均TPS（Double）
average.tps15m              # 15分間の平均TPS（Double）
```

**6. 変数 (`vars`, `target.vars`, `global.vars`など)**

```yaml
vars.変数名                 # 自分の変数
target.vars.変数名          # ターゲットの変数
trigger.vars.変数名         # トリガーエンティティの変数
global.vars.変数名          # グローバル変数
world.vars.変数名           # ワールド変数
```

**使用例:**
```yaml
# TPSに基づく動的遅延
Skills:
  - delay "current.tps.toInt"           # 現在のTPS（20～0）
  - delay "average.tps1m.toInt"         # 1分平均TPS
  - wait "(20 - current.tps).toInt"     # TPS低下分だけ待機

# プレイヤー数に基づく遅延
Skills:
  - delay "nearbyPlayers.count * 10"    # プレイヤー数×10tick
  - wait "50 / (nearbyPlayers.count + 1)"  # プレイヤーが多いほど短く

# HP割合に基づく遅延
Skills:
  - delay "entity.health < entity.maxHealth * 0.5 ? 100 : 50"  # HP50%以下なら100tick、それ以外50tick
```

**型変換関数:**

CEL式内で型変換を行うための関数を3つの記法で提供しています。

**1. チェーン型変換形式（推奨・最も簡潔）:**

Kotlinの拡張関数のように、ドット記法で型変換をチェーンできます。

```yaml
# 単一の変換
- delay "current.tps.toInt"                    # Double → Int
- damage{amount="entity.health.toDouble"}       # 明示的にDouble化

# チェーン変換
- delay "current.tps.toInt.toDouble.toFloat.toString.toInt"
  # Double → Int → Double → Float → String → Int

# 複雑な式の変換
- particle{count="(entity.health / entity.maxHealth * 100).toInt"}
- damage{amount="nearbyPlayers.count.toDouble.mul(1.5).toInt"}

# 変数との組み合わせ
- set hpPercent "entity.health / entity.maxHealth * 100"
- delay "vars.hpPercent.toInt"
- damage{amount="vars.hpPercent.toDoubleClamped(0.0, 100.0)"}
```

**対応する変換メソッド:**
- `.toInt`: Int型に変換
- `.toLong`: Long型に変換
- `.toDouble`: Double型に変換
- `.toFloat`: Float型に変換
- `.toBoolean`: Boolean型に変換
- `.toString`: String型に変換
- `.toList`: List型に変換（カンマ区切り文字列やコレクションから）
- `.toMap`: Map型に変換（JSONライクな文字列やコレクションから）
- `.toIntOr(default)`: 安全なInt変換（失敗時はdefault）
- `.toDoubleClamped(min, max)`: 範囲制限付きDouble変換

**2. グローバル関数形式:**
```yaml
# 基本的な型変換
- delay "toInt(current.tps)"        # Double → Int
- delay "toDouble(nearbyPlayers.count)"  # Int → Double
- wait "toLong(entity.health)"      # Double → Long
- delay "toFloat(average.tps5m)"    # Double → Float

# 安全な変換（失敗時にデフォルト値）
- delay "toIntOr(someValue, 20)"    # 失敗時は20
- delay "toDoubleOr(someValue, 1.0)" # 失敗時は1.0

# 範囲制限付き変換
- delay "toIntClamped(current.tps, 1, 20)"     # 1～20に制限
- delay "toDoubleClamped(entity.health, 0.0, 100.0)"  # 0.0～100.0に制限

# リスト・マップ変換
- set items "toList('[1, 2, 3]')"
- set data "toMap('{hp: 100, mp: 50}')"
```

**3. 従来のcast形式（互換性のため残存）:**
```yaml
- delay "cast.toInt(current.tps)"
- delay "cast.toIntOr(someValue, 20)"
- delay "cast.toIntClamped(current.tps, 1, 20)"
```

**使用例:**
```yaml
# チェーン形式（推奨）
Skills:
  - delay "current.tps.toInt"
  - particle{count="(entity.health / entity.maxHealth * 100).toInt"}

# グローバル関数形式
Skills:
  - delay "toInt(current.tps)"
  - damage{amount="toDoubleClamped(entity.health / entity.maxHealth * 100, 0.0, 100.0)"}

# 複雑な変換チェーン
Skills:
  - set raw "entity.health.toString"           # "20.0"
  - set truncated "vars.raw.toDouble.toInt"    # 20
  - set list "vars.truncated.toString.toList"  # ["2", "0"]
```

**実装メモ:**
```kotlin
// CELEngineに変数を登録
class CELVariableProvider {
    fun getTpsVariables(): Map<String, Any> {
        val server = Bukkit.getServer()
        return mapOf(
            "current" to mapOf(
                "tps" to server.tps[0]  // 現在のTPS
            ),
            "average" to mapOf(
                "tps" to server.tps[1],      // デフォルトは1分平均
                "tps1m" to server.tps[0],    // 1分平均
                "tps5m" to server.tps[1],    // 5分平均
                "tps15m" to server.tps[2]    // 15分平均
            )
        )
    }

    fun buildCastFunctions(): Map<String, Any> {
        return mapOf(
            "toInt" to { value: Any -> /* 型変換ロジック */ },
            "toDouble" to { value: Any -> /* 型変換ロジック */ },
            "toLong" to { value: Any -> /* 型変換ロジック */ },
            "toFloat" to { value: Any -> /* 型変換ロジック */ },
            "toBoolean" to { value: Any -> /* 型変換ロジック */ },
            "toString" to { value: Any -> value.toString() },
            "toIntOr" to { value: Any, default: Int -> /* デフォルト値付き変換 */ },
            "toDoubleOr" to { value: Any, default: Double -> /* デフォルト値付き変換 */ },
            "toIntClamped" to { value: Any, min: Int, max: Int -> /* 範囲制限付き変換 */ },
            "toDoubleClamped" to { value: Any, min: Double, max: Double -> /* 範囲制限付き変換 */ }
        )
    }
}
```
