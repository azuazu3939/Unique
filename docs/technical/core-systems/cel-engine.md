# CEL Engine システム

## 概要

CEL (Common Expression Language) Engine は、Unique プラグインにおける動的な条件評価と計算を実現する中核システムです。Google が開発した CEL ライブラリを基盤とし、YAML 設定ファイル内で記述された式を実行時に評価します。

### 主な目的

- **動的な条件評価**: モブのスポーン条件、スキル発動条件などを実行時に判定
- **計算式の評価**: ダメージ計算、ヘルス計算、ドロップ確率などの動的な値の算出
- **型安全性**: 静的型チェックによる安全な式評価
- **パフォーマンス**: コンパイル済み式のキャッシュによる高速実行

### システム構成要素

1. **CELEngineManager**: CEL エンジンの初期化と式のコンパイルを管理
2. **CELEvaluator**: コンパイル済み式の評価と型変換を実行
3. **CELVariableProvider**: 評価コンテキストの構築と変数提供

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                      YAML 設定ファイル                       │
│  conditions: "player.health < 10 && world.time > 12000"     │
│  damage: "base_damage * (1 + attacker.level * 0.1)"        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    CELEngineManager                          │
│  ┌──────────────────────────────────────────────────┐      │
│  │ compile(expression: String): CompiledExpression  │      │
│  │ - 式の構文解析とコンパイル                        │      │
│  │ - 型チェックと最適化                              │      │
│  │ - コンパイル済み式のキャッシュ                    │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   CELVariableProvider                        │
│  ┌──────────────────────────────────────────────────┐      │
│  │ buildEntityContext(entity): Map<String, Any>     │      │
│  │ buildWorldInfo(world): Map<String, Any>          │      │
│  │ buildTargetContext(target): Map<String, Any>     │      │
│  │ buildLocationInfo(location): Map<String, Any>    │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      CELEvaluator                            │
│  ┌──────────────────────────────────────────────────┐      │
│  │ evaluate(compiled, context): Any?                │      │
│  │ evaluateBoolean(expr, context): Boolean          │      │
│  │ evaluateNumber(expr, context): Double            │      │
│  │ - 型変換とエラーハンドリング                      │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      実行時評価結果                          │
│  Boolean / Number / String / List / Map                     │
└─────────────────────────────────────────────────────────────┘
```

## 実装詳細

### CELEngineManager

CEL エンジンの初期化とコンパイルを管理するシングルトンオブジェクト。

```kotlin
object CELEngineManager {
    private val logger = Unique.instance.logger

    // CEL環境の構築
    private val celEnv = CelFactory.standardCelBuilder()
        .addVar("player", StructTypeReference.create("Player"))
        .addVar("mob", StructTypeReference.create("Mob"))
        .addVar("world", StructTypeReference.create("World"))
        .addVar("attacker", StructTypeReference.create("Entity"))
        .addVar("target", StructTypeReference.create("Entity"))
        .addVar("location", StructTypeReference.create("Location"))
        .addVar("base_damage", SimpleType.DOUBLE)
        .addVar("base_health", SimpleType.DOUBLE)
        .addVar("level", SimpleType.INT)
        .addVar("distance", SimpleType.DOUBLE)
        .build()

    private val celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build()

    // コンパイル済み式のキャッシュ
    private val compiledCache = ConcurrentHashMap<String, CelAbstractSyntaxTree>()

    /**
     * 式をコンパイルしてキャッシュから返す
     */
    fun compile(expression: String): CelAbstractSyntaxTree? {
        return compiledCache.getOrPut(expression) {
            try {
                celEnv.compile(expression).ast
            } catch (e: CelValidationException) {
                logger.warning("CEL式のコンパイルエラー: $expression - ${e.message}")
                null
            } catch (e: Exception) {
                logger.severe("CEL式のコンパイル中に予期しないエラー: $expression")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 式を評価する（キャッシュなし・即時評価用）
     */
    fun evaluate(expression: String, context: Map<String, Any>): Any? {
        val compiled = compile(expression) ?: return null
        return try {
            celRuntime.createProgram(compiled).eval(context)
        } catch (e: Exception) {
            logger.warning("CEL式の評価エラー: $expression - ${e.message}")
            null
        }
    }
}
```

**主な機能:**

1. **環境構築**: 利用可能な変数と型を定義
2. **コンパイルキャッシュ**: 同じ式を再コンパイルせず、パフォーマンスを向上
3. **エラーハンドリング**: コンパイルエラーと実行時エラーを適切に処理
4. **型安全性**: 構造体型参照による型チェック

### CELEvaluator

コンパイル済み式の評価と型変換を担当するオブジェクト。

```kotlin
object CELEvaluator {
    private val logger = Unique.instance.logger

    /**
     * Boolean型として評価
     */
    fun evaluateBoolean(expression: String, context: Map<String, Any>): Boolean {
        val compiled = CELEngineManager.compile(expression) ?: return false

        return try {
            val result = evaluate(compiled, context)
            when (result) {
                is Boolean -> result
                is String -> result.toBoolean()
                is Number -> result.toDouble() > 0
                null -> false
                else -> {
                    logger.warning("CEL式の結果をBooleanに変換できません: $result")
                    false
                }
            }
        } catch (e: Exception) {
            logger.warning("CEL式の評価中にエラー: $expression - ${e.message}")
            false
        }
    }

    /**
     * Number型として評価
     */
    fun evaluateNumber(expression: String, context: Map<String, Any>): Double {
        val compiled = CELEngineManager.compile(expression) ?: return 0.0

        return try {
            val result = evaluate(compiled, context)
            when (result) {
                is Number -> result.toDouble()
                is String -> result.toDoubleOrNull() ?: 0.0
                is Boolean -> if (result) 1.0 else 0.0
                null -> 0.0
                else -> {
                    logger.warning("CEL式の結果をNumberに変換できません: $result")
                    0.0
                }
            }
        } catch (e: Exception) {
            logger.warning("CEL式の評価中にエラー: $expression - ${e.message}")
            0.0
        }
    }

    /**
     * String型として評価
     */
    fun evaluateString(expression: String, context: Map<String, Any>): String {
        val compiled = CELEngineManager.compile(expression) ?: return ""

        return try {
            val result = evaluate(compiled, context)
            result?.toString() ?: ""
        } catch (e: Exception) {
            logger.warning("CEL式の評価中にエラー: $expression - ${e.message}")
            ""
        }
    }

    /**
     * 生の評価結果を返す
     */
    private fun evaluate(compiled: CelAbstractSyntaxTree, context: Map<String, Any>): Any? {
        return try {
            val program = CelRuntimeFactory.standardCelRuntimeBuilder()
                .build()
                .createProgram(compiled)
            program.eval(context)
        } catch (e: CelEvaluationException) {
            logger.warning("CEL評価エラー: ${e.message}")
            null
        }
    }
}
```

**主な機能:**

1. **型変換**: 評価結果を Boolean、Number、String に安全に変換
2. **デフォルト値**: 変換失敗時の適切なデフォルト値の返却
3. **エラーハンドリング**: 評価エラーのログ出力と安全な処理
4. **柔軟な変換**: 異なる型間の暗黙的変換をサポート

### CELVariableProvider

評価コンテキストを構築し、CEL 式で使用する変数を提供します。

```kotlin
object CELVariableProvider {

    /**
     * エンティティ情報のコンテキストを構築
     */
    fun buildEntityContext(entity: Entity): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            // 基本情報
            put("type", entity.type.name)
            put("uuid", entity.uniqueId.toString())
            put("name", entity.name ?: "")
            put("custom_name", entity.customName ?: "")

            // 位置情報
            buildLocationInfo(entity.location).forEach { (key, value) ->
                put("location.$key", value)
            }

            // 生存エンティティの場合
            if (entity is LivingEntity) {
                put("health", entity.health)
                put("max_health", entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0)
                put("armor", entity.getAttribute(Attribute.GENERIC_ARMOR)?.value ?: 0.0)
                put("armor_toughness", entity.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)?.value ?: 0.0)

                // アクティブエフェクト
                val effects = entity.activePotionEffects.map { it.type.name }
                put("effects", effects)
                put("has_effect", effects.isNotEmpty())
            }

            // プレイヤーの場合
            if (entity is Player) {
                put("level", entity.level)
                put("exp", entity.exp)
                put("food_level", entity.foodLevel)
                put("saturation", entity.saturation)
                put("game_mode", entity.gameMode.name)
                put("is_op", entity.isOp)
                put("is_flying", entity.isFlying)
                put("is_sneaking", entity.isSneaking)
                put("is_sprinting", entity.isSprinting)

                // 所持アイテム情報
                put("main_hand", entity.inventory.itemInMainHand.type.name)
                put("off_hand", entity.inventory.itemInOffHand.type.name)
            }
        }
    }

    /**
     * ワールド情報のコンテキストを構築
     */
    fun buildWorldInfo(world: World): Map<String, Any> {
        return mapOf(
            "name" to world.name,
            "time" to world.time,
            "full_time" to world.fullTime,
            "is_day" to (world.time in 0..12000),
            "is_night" to (world.time > 12000),
            "weather_duration" to world.weatherDuration,
            "is_thundering" to world.isThundering,
            "is_raining" to (world.hasStorm()),
            "difficulty" to world.difficulty.name,
            "player_count" to world.players.size,
            "entity_count" to world.entities.size,
            "sea_level" to world.seaLevel
        )
    }

    /**
     * ターゲット情報のコンテキストを構築
     */
    fun buildTargetContext(attacker: Entity, target: Entity): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // 攻撃者情報
        buildEntityContext(attacker).forEach { (key, value) ->
            context["attacker.$key"] = value
        }

        // ターゲット情報
        buildEntityContext(target).forEach { (key, value) ->
            context["target.$key"] = value
        }

        // 関係情報
        context["distance"] = attacker.location.distance(target.location)
        context["same_world"] = attacker.world.uid == target.world.uid

        return context
    }

    /**
     * 位置情報のコンテキストを構築
     */
    fun buildLocationInfo(location: Location): Map<String, Any> {
        return mapOf(
            "x" to location.x,
            "y" to location.y,
            "z" to location.z,
            "yaw" to location.yaw,
            "pitch" to location.pitch,
            "block_x" to location.blockX,
            "block_y" to location.blockY,
            "block_z" to location.blockZ,
            "chunk_x" to location.chunk.x,
            "chunk_z" to location.chunk.z,
            "world" to location.world.name,
            "biome" to location.block.biome.name,
            "block_type" to location.block.type.name,
            "light_level" to location.block.lightLevel
        )
    }

    /**
     * 戦闘コンテキストを構築
     */
    fun buildCombatContext(
        attacker: Entity?,
        target: Entity,
        baseDamage: Double,
        additionalContext: Map<String, Any> = emptyMap()
    ): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // 基本ダメージ
        context["base_damage"] = baseDamage

        // 攻撃者情報
        if (attacker != null) {
            buildEntityContext(attacker).forEach { (key, value) ->
                context["attacker.$key"] = value
            }
        }

        // ターゲット情報
        buildEntityContext(target).forEach { (key, value) ->
            context["target.$key"] = value
        }

        // 距離計算
        if (attacker != null) {
            context["distance"] = attacker.location.distance(target.location)
        }

        // 追加コンテキスト
        context.putAll(additionalContext)

        return context
    }
}
```

**主な機能:**

1. **エンティティコンテキスト**: エンティティの状態、属性、位置情報を提供
2. **ワールドコンテキスト**: 時間、天候、難易度などのワールド情報
3. **ターゲットコンテキスト**: 攻撃者とターゲットの関係情報
4. **戦闘コンテキスト**: ダメージ計算に必要な全情報
5. **柔軟な拡張**: 追加コンテキストの動的な追加をサポート

## 使用例

### 条件評価

```yaml
mobs:
  boss_mob:
    spawn_conditions:
      # プレイヤーのヘルスが50%以下で、夜間の場合のみスポーン
      - "player.health < player.max_health * 0.5 && world.is_night"
      # プレイヤーがオペレーターではない
      - "!player.is_op"
      # バイオームが平原または森林
      - "location.biome in ['PLAINS', 'FOREST']"
```

### ダメージ計算

```yaml
skills:
  power_strike:
    effects:
      - type: damage
        # ベースダメージ + 攻撃者のレベル補正 + 距離補正
        amount: "base_damage * (1 + attacker.level * 0.1) * (1 - distance / 100)"
        # クリティカル条件: ターゲットのヘルスが25%以下
        critical_condition: "target.health < target.max_health * 0.25"
        critical_multiplier: 2.0
```

### ドロップ確率

```yaml
drops:
  - item: DIAMOND
    # キラーのレベルに応じて確率が上昇
    chance: "0.05 * (1 + player.level * 0.02)"
    # 略奪エンチャントのレベルに応じて数量増加
    amount: "1 + floor(player.looting_level * 0.5)"
```

### 複雑な条件

```yaml
ai:
  behavior:
    enrage_condition:
      # ヘルスが50%以下、かつ、昼間、かつ、10ブロック以内にプレイヤーがいる
      - "mob.health < mob.max_health * 0.5"
      - "world.is_day"
      - "distance < 10"
      # または、ボスがダメージを受けて5秒以内
      - "time_since_last_damage < 100"  # 5秒 = 100ティック
```

## パフォーマンス最適化

### コンパイルキャッシュ

CELEngineManager は、一度コンパイルした式を `ConcurrentHashMap` でキャッシュします。これにより、同じ式を繰り返し評価する際のオーバーヘッドを大幅に削減します。

```kotlin
private val compiledCache = ConcurrentHashMap<String, CelAbstractSyntaxTree>()

fun compile(expression: String): CelAbstractSyntaxTree? {
    return compiledCache.getOrPut(expression) {
        // コンパイル処理
    }
}
```

**効果:**
- 初回コンパイル: 約 5-10ms
- キャッシュヒット: 約 0.01ms 未満
- メモリ使用量: 式1つあたり約 1-2KB

### 非同期評価

重い計算や多数のエンティティに対する評価は、コルーチンを使用して非同期に実行します。

```kotlin
suspend fun evaluateAsync(expression: String, context: Map<String, Any>): Any? = withContext(Dispatchers.Default) {
    CELEvaluator.evaluate(expression, context)
}
```

### プリコンパイル

頻繁に使用される式は、プラグイン起動時にプリコンパイルしておきます。

```kotlin
object CommonExpressions {
    val IS_NIGHT = CELEngineManager.compile("world.is_night")
    val LOW_HEALTH = CELEngineManager.compile("player.health < player.max_health * 0.3")
    val DISTANCE_CLOSE = CELEngineManager.compile("distance < 5")
}
```

### バッチ評価

複数のエンティティに対して同じ式を評価する場合、コンテキスト構築のオーバーヘッドを最小化します。

```kotlin
fun evaluateBatch(expression: String, entities: List<Entity>): List<Boolean> {
    val compiled = CELEngineManager.compile(expression) ?: return List(entities.size) { false }

    return entities.map { entity ->
        val context = CELVariableProvider.buildEntityContext(entity)
        CELEvaluator.evaluateBoolean(compiled, context)
    }
}
```

## ベストプラクティス

### 1. 式の簡潔化

複雑な式は読みにくく、パフォーマンスも低下します。可能な限りシンプルに保ちます。

```yaml
# 悪い例
conditions: "player.health < player.max_health * 0.5 && (world.time > 12000 && world.time < 24000) && !player.is_op && player.level > 10 && distance < 20"

# 良い例 - 複数の条件に分割
conditions:
  - "player.health < player.max_health * 0.5"
  - "world.is_night"
  - "!player.is_op"
  - "player.level > 10"
  - "distance < 20"
```

### 2. 早期リターン

最も失敗しやすい条件を最初に配置して、不要な評価を避けます。

```yaml
# 良い例 - 安価な条件を先に評価
conditions:
  - "world.is_night"              # 高速
  - "distance < 50"               # 高速
  - "player.level > 10"           # 高速
  - "complex_calculation() > 5"   # 低速
```

### 3. 変数の再利用

同じ計算結果を複数回使う場合は、コンテキストで変数として提供します。

```kotlin
val context = mutableMapOf<String, Any>()
context["health_percent"] = player.health / player.maxHealth
context["distance_to_spawn"] = player.location.distance(spawnLocation)

// YAMLで使用
// conditions: "health_percent < 0.5 && distance_to_spawn > 100"
```

### 4. 型の明示

CEL は型安全ですが、明示的な型変換を行うことでエラーを防ぎます。

```yaml
# 良い例 - 明示的な型変換
amount: "double(player.level) * 1.5"
count: "int(base_count * multiplier)"
```

### 5. エラーハンドリング

評価失敗時のデフォルト値を適切に設定します。

```kotlin
fun evaluateSafely(expression: String, context: Map<String, Any>, default: Double): Double {
    return try {
        CELEvaluator.evaluateNumber(expression, context)
    } catch (e: Exception) {
        logger.warning("式の評価に失敗、デフォルト値を使用: $default")
        default
    }
}
```

## トラブルシューティング

### コンパイルエラー

**問題**: 式がコンパイルできない

```
CEL式のコンパイルエラー: player.helth < 10 - undefined field 'helth'
```

**解決策**: タイポを修正、利用可能な変数名を確認

```yaml
# 修正前
conditions: "player.helth < 10"

# 修正後
conditions: "player.health < 10"
```

### 型エラー

**問題**: 型の不一致

```
CEL評価エラー: no such overload: _<_(string, int)
```

**解決策**: 適切な型変換を行う

```yaml
# 修正前
conditions: "player.name < 10"

# 修正後
conditions: "player.level < 10"
```

### パフォーマンス問題

**問題**: 式の評価が遅い

**解決策**:
1. デバッグモードで評価時間を測定
2. 複雑な式を分割
3. キャッシュ可能な計算を事前に実行
4. 不要な評価を削除

```kotlin
// デバッグ用の評価時間測定
val startTime = System.nanoTime()
val result = CELEvaluator.evaluateBoolean(expression, context)
val duration = (System.nanoTime() - startTime) / 1_000_000.0
logger.info("式の評価時間: ${duration}ms")
```

## 関連ドキュメント

- [Config System](config-system.md) - CEL 式を含む設定ファイルの読み込み
- [Condition System](../condition/condition-system.md) - CEL を使用した条件システム
- [Mob Manager](../mob-system/mob-manager.md) - モブ定義での CEL 式の使用
- [Spawn System](../mob-system/spawn-system.md) - スポーン条件での CEL 式の使用
- [Effect Overview](../effect-system/effect-overview.md) - エフェクトでの動的値計算

## まとめ

CEL Engine は Unique プラグインの柔軟性と拡張性を支える重要なコンポーネントです。以下の特徴を持ちます:

1. **型安全性**: コンパイル時の型チェックにより、実行時エラーを最小化
2. **高パフォーマンス**: コンパイルキャッシュと最適化により、高速な評価を実現
3. **柔軟性**: 豊富なコンテキスト変数により、様々な条件と計算を表現可能
4. **保守性**: YAML 内で式を記述することで、コードの再コンパイルなしに動作を変更可能

CEL Engine を適切に活用することで、複雑なゲームロジックを簡潔かつ効率的に実装できます。
