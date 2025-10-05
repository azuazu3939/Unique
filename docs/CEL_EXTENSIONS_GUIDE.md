# CEL拡張機能ガイド

## 🎯 設計思想

**Javaクラスを増やさず、CEL式とYAMLで全ての機能を実現**

### 基本原則
1. **Java/Kotlinの役割**: データの取得と書き込みのみ
2. **CELの役割**: 条件判定、計算、フィルタリング
3. **YAMLの役割**: 設定とロジックの定義

---

## 📦 必要なCEL変数の追加

### 1. 数学関数 (math)

```yaml
# 既存の変数に追加が必要な関数
math.toRadians(degrees)     # 度数→ラジアン変換
math.toDegrees(radians)     # ラジアン→度数変換
math.acos(value)            # 逆余弦
math.asin(value)            # 逆正弦
math.atan(value)            # 逆正接
math.atan2(y, x)            # 2引数逆正接
math.cos(radians)           # 余弦
math.sin(radians)           # 正弦
math.tan(radians)           # 正接
```

**実装例** (`CELVariableProvider.kt`):
```kotlin
// math名前空間に関数を追加
env = env.extend(
    Decls.newVar("math", Decls.newMapType(Decls.String, Decls.Dyn))
)

// 評価時にmath関数を提供
val mathFunctions = mapOf(
    "toRadians" to { degrees: Double -> Math.toRadians(degrees) },
    "toDegrees" to { radians: Double -> Math.toDegrees(radians) },
    "acos" to { value: Double -> Math.acos(value) },
    "cos" to { radians: Double -> Math.cos(radians) },
    "sin" to { radians: Double -> Math.sin(radians) },
    // ... 他の関数
)

context["math"] = mathFunctions
```

### 2. ランダム関数 (random)

```yaml
random.range(min, max)      # min〜maxのランダム値
random.chance(probability)  # 確率判定（0.0〜1.0）
random.int(min, max)        # 整数のランダム値
random.boolean()            # ランダムboolean
```

**実装例**:
```kotlin
val randomFunctions = mapOf(
    "range" to { min: Double, max: Double -> 
        min + (max - min) * Math.random()
    },
    "chance" to { probability: Double ->
        Math.random() < probability
    },
    "int" to { min: Int, max: Int ->
        min + (Math.random() * (max - min + 1)).toInt()
    },
    "boolean" to { ->
        Math.random() < 0.5
    }
)

context["random"] = randomFunctions
```

### 3. 距離計算 (distance)

```yaml
distance.between(pos1, pos2)      # 2点間の距離
distance.horizontal(pos1, pos2)   # 水平距離（Y軸無視）
distance.squared(pos1, pos2)      # 距離の2乗（高速）
```

**実装例**:
```kotlin
val distanceFunctions = mapOf(
    "between" to { pos1: Map<String, Double>, pos2: Map<String, Double> ->
        val dx = pos2["x"]!! - pos1["x"]!!
        val dy = pos2["y"]!! - pos1["y"]!!
        val dz = pos2["z"]!! - pos1["z"]!!
        Math.sqrt(dx * dx + dy * dy + dz * dz)
    },
    "horizontal" to { pos1: Map<String, Double>, pos2: Map<String, Double> ->
        val dx = pos2["x"]!! - pos1["x"]!!
        val dz = pos2["z"]!! - pos1["z"]!!
        Math.sqrt(dx * dx + dz * dz)
    }
)

context["distance"] = distanceFunctions
```

### 4. 周囲のエンティティ情報

```yaml
nearbyPlayerCount           # 周囲のプレイヤー数
nearbyMobCount              # 周囲のMob数
nearbyPlayers.avgLevel      # 平均レベル
nearbyPlayers.maxLevel      # 最大レベル
nearbyPlayers.minLevel      # 最小レベル
chainIndex                  # 連鎖攻撃の現在のインデックス
```

**実装方法**:
スキル実行時に周囲をスキャンして変数に追加

```kotlin
fun buildSkillContext(source: Entity, target: Entity?): Map<String, Any> {
    val context = mutableMapOf<String, Any>()
    
    // 周囲のプレイヤー情報
    val nearbyPlayers = source.world.getNearbyEntities(
        source.location, 20.0, 20.0, 20.0
    ).filterIsInstance<Player>()
    
    context["nearbyPlayerCount"] = nearbyPlayers.size
    context["nearbyPlayers"] = mapOf(
        "avgLevel" to nearbyPlayers.map { it.level }.average(),
        "maxLevel" to nearbyPlayers.maxOfOrNull { it.level } ?: 0,
        "minLevel" to nearbyPlayers.minOfOrNull { it.level } ?: 0
    )
    
    // 周囲のMob情報
    val nearbyMobs = source.world.getNearbyEntities(
        source.location, 20.0, 20.0, 20.0
    ).filterIsInstance<LivingEntity>()
        .filter { it !is Player }
    
    context["nearbyMobCount"] = nearbyMobs.size
    
    return context
}
```

### 5. 環境情報 (environment)

```yaml
environment.moonPhase       # 月相（0-7）
environment.dayOfCycle      # サイクル日数
environment.tickOfDay       # 1日のtick
environment.biome           # 現在のバイオーム
```

**実装例**:
```kotlin
val world = source.world
val environment = mapOf(
    "moonPhase" to (world.fullTime / 24000 % 8).toInt(),
    "dayOfCycle" to (world.fullTime / 24000).toInt(),
    "tickOfDay" to (world.time % 24000).toInt(),
    "biome" to source.location.block.biome.name
)

context["environment"] = environment
```

---

## 🔧 YAML構造の拡張

### 1. フィルター式をサポート

既存の`targeter`に`filter`プロパティを追加:

```yaml
targeter:
  type: RadiusPlayers
  range: 30
  filter: "target.health > 50 && target.gameMode == 'SURVIVAL'"
```

**実装** (`Targeter.kt`に追加):
```kotlin
// YAMLから読み込み
val filterExpression = section.getString("filter")

// ターゲット取得時にフィルタ適用
val targets = getBaseTargets(source)
val filtered = if (filterExpression != null) {
    targets.filter { target ->
        val context = buildTargetContext(source, target)
        celEvaluator.evaluateBoolean(filterExpression, context)
    }
} else {
    targets
}
```

### 2. 連鎖設定をサポート

```yaml
targeter:
  type: NearestPlayer
  range: 20
  chain:
    maxChains: 5
    chainRange: 5.0
    condition: "target.health > 0"
```

**実装**:
既存の`Targeter`を使い、`chain`セクションを読み込んで反復処理

```kotlin
fun getChainTargets(source: Entity, config: ChainConfig): List<Entity> {
    val allTargets = mutableListOf<Entity>()
    val processed = mutableSetOf<UUID>()
    
    var current = getInitialTarget(source) ?: return emptyList()
    allTargets.add(current)
    processed.add(current.uniqueId)
    
    repeat(config.maxChains) {
        val nearby = world.getNearbyEntities(
            current.location, 
            config.chainRange, 
            config.chainRange, 
            config.chainRange
        ).filter { it.uniqueId !in processed }
        
        val next = nearby.firstOrNull { target ->
            val context = buildTargetContext(source, target)
            context["chainIndex"] = allTargets.size
            celEvaluator.evaluateBoolean(config.condition, context)
        } ?: return@repeat
        
        allTargets.add(next)
        processed.add(next.uniqueId)
        current = next
    }
    
    return allTargets
}
```

### 3. CEL式で値を計算

YAMLの値としてCEL式を使用:

```yaml
# 文字列として定義
damage: "20 * (1 - distance.horizontal(source.location, target.location) / 10.0)"

# 読み込み時に評価
amount: "math.floor(nearbyPlayerCount / 2)"
```

**実装**:
YAML読み込み時に文字列を検出してCEL式として評価

```kotlin
fun parseValue(value: Any?, context: Map<String, Any>): Any {
    return when {
        value is String && isCelExpression(value) -> {
            celEvaluator.evaluate(value, context)
        }
        else -> value
    }
}

fun isCelExpression(str: String): Boolean {
    // CEL式の特徴を検出
    return str.contains("math.") || 
           str.contains("random.") ||
           str.contains("distance.") ||
           str.contains("entity.") ||
           str.contains("target.") ||
           str.matches(Regex(".*[+\\-*/()].*"))
}
```

### 4. 条件分岐をサポート

```yaml
branches:
  - condition: "nearbyPlayerCount >= 3"
    skills:
      - skill: CircleExplosion
  
  - condition: "target.health > 100"
    skills:
      - skill: StrongAttack
  
  - default: true
    skills:
      - skill: NormalAttack
```

**実装**:
既存の`MetaSkill`を拡張して分岐ロジックを追加

```kotlin
for (branch in branches) {
    val condition = branch.condition
    val isDefault = branch.isDefault
    
    if (isDefault || celEvaluator.evaluateBoolean(condition, context)) {
        executeBranchSkills(branch.skills, context)
        break  // 最初にマッチした分岐のみ実行
    }
}
```

---

## 📚 使用例

### 例1: 距離に応じたダメージ

```yaml
damage: "20 * math.max(0, 1 - distance.horizontal(source.location, target.location) / 10.0)"
```

### 例2: 時刻とバイオームで召喚数変更

```yaml
amount: >
  world.isNight && environment.biome.contains('DARK') ? 
  random.int(3, 5) : 
  random.int(1, 2)
```

### 例3: プレイヤーレベルに応じたバフ強度

```yaml
amplifier: "math.floor(nearbyPlayers.avgLevel / 10)"
```

---

## 🚀 実装優先順位

1. **math関数拡張** (高優先度)
    - 三角関数、角度計算に必須

2. **random関数** (高優先度)
    - ゲーム性に直結

3. **filter式サポート** (高優先度)
    - 柔軟なターゲット選択に必須

4. **nearbyエンティティ情報** (中優先度)
    - 高度なAI動作に必要

5. **chain設定** (中優先度)
    - 連鎖攻撃の実装に必要

6. **CEL式の値計算** (中優先度)
    - 動的な設定に便利

7. **branches分岐** (低優先度)
    - MetaSkillで代替可能

---

この設計により、**Javaクラスを一切追加せず**に、YAML + CELだけで高度な機能を実現できます！