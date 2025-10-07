# SpawnManager - 自動スポーンシステム

## 概要

`SpawnManager` は、Mobの自動スポーン機能を提供するクラスです。時間、天候、プレイヤー数などの条件に基づいて、Mobを自動的にスポーンさせます。

## クラス構造

```kotlin
class SpawnManager(private val plugin: Unique) {
    // スポーン定義（定義名 -> 定義）
    private val spawnDefinitions = ConcurrentHashMap<String, SpawnDefinition>()

    // アクティブなスポーンタスク（定義名 -> Job）
    private val spawnTasks = ConcurrentHashMap<String, Job>()

    // スポーン数カウント（定義名 -> カウント）
    private val spawnCounts = ConcurrentHashMap<String, Int>()
}
```

## 主要機能

### 1. スポーン定義の読み込み

```kotlin
fun loadSpawnDefinitions() {
    spawnDefinitions.clear()

    val spawnsFolder = plugin.spawnsFolder
    val yamlFiles = spawnsFolder.listFiles { file ->
        file.extension == "yml" || file.extension == "yaml"
    }

    for (file in yamlFiles) {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val spawnNames = yaml.getKeys(false)

        for (spawnName in spawnNames) {
            val section = yaml.getConfigurationSection(spawnName)
            val spawnDef = parseSpawnDefinition(section)
            spawnDefinitions[spawnName] = spawnDef
        }
    }
}
```

**読み込み元**:
- `plugins/Unique/spawns/*.yml`
- すべてのYAMLファイルを自動検出

### 2. スポーン定義の解析

```kotlin
private fun parseSpawnDefinition(section: ConfigurationSection): SpawnDefinition {
    return SpawnDefinition(
        mob = section.getString("mob") ?: section.getString("Mob"),
        conditions = section.getStringList("conditions"),
        spawnRate = section.getInt("spawnRate", 20),
        maxNearby = section.getInt("maxNearbyMobs", 5),
        chunkRadius = section.getInt("spawnRadius", 3),
        region = parseRegion(section.getConfigurationSection("region")),
        location = parseLocation(section.getConfigurationSection("location")),
        advancedConditions = parseAdvancedConditions(section.getConfigurationSection("advancedConditions")),
        onSpawn = parseOnSpawnSkills(section.getConfigurationSection("onSpawn"))
    )
}
```

### 3. スポーンタスクの実行

```kotlin
private suspend fun runSpawnTask(name: String, definition: SpawnDefinition) {
    while (true) {
        try {
            // スポーンレートに基づいて待機
            delay(definition.spawnRate * 50L)  // tickをミリ秒に変換

            // 軽量なチェック
            val world = getSpawnWorld(definition) ?: continue
            val players = world.players
            if (players.isEmpty()) continue

            val targetPlayer = players.random()

            // 実際のスポーン処理は region dispatcher で実行
            plugin.launch(plugin.regionDispatcher(targetPlayer.location)) {
                attemptSpawnForPlayer(name, definition, targetPlayer)
            }

        } catch (e: CancellationException) {
            throw e  // 正常なキャンセル
        } catch (e: Exception) {
            DebugLogger.error("Error in spawn task: $name", e)
        }
    }
}
```

**スポーンタスクの流れ**:
1. `spawnRate` 間隔で待機
2. ワールドにプレイヤーがいるかチェック
3. ランダムにプレイヤーを選択
4. プレイヤーの region dispatcher でスポーン試行

### 4. スポーン試行

```kotlin
private suspend fun attemptSpawnForPlayer(
    name: String,
    definition: SpawnDefinition,
    targetPlayer: Player
) {
    // 条件評価
    if (!evaluateSpawnConditions(definition, targetPlayer.world)) {
        return
    }

    // 最大数チェック
    val currentCount = spawnCounts.getOrDefault(name, 0)
    if (currentCount >= definition.maxNearby) {
        return
    }

    // スポーン位置決定（プレイヤーの周囲20-48ブロック）
    val minRadius = 20.0
    val maxRadius = 48.0
    val angle = Random.nextDouble() * 2 * PI
    val distance = Random.nextDouble(minRadius, maxRadius)
    val x = targetPlayer.location.x + distance * cos(angle)
    val z = targetPlayer.location.z + distance * sin(angle)

    val tempLocation = Location(targetPlayer.world, x, 64.0, z)

    // 実際のY座標を取得してスポーン
    withContext(plugin.regionDispatcher(tempLocation)) {
        val y = targetPlayer.world.getHighestBlockYAt(x.toInt(), z.toInt()).toDouble() + 1
        val spawnLocation = Location(targetPlayer.world, x, y, z)

        val mob = plugin.mobManager.spawnMob(definition.mob, spawnLocation)

        if (mob != null) {
            spawnCounts.compute(name) { _, count -> (count ?: 0) + 1 }
            executeOnSpawnSkills(mob, definition.onSpawn)
        }
    }
}
```

### 5. スポーン条件評価

```kotlin
private fun evaluateSpawnConditions(definition: SpawnDefinition, world: World): Boolean {
    val context = buildSpawnContext(definition, world)

    // CEL条件評価（短絡評価）
    for (condition in definition.conditions) {
        if (condition.trim() == "true") continue

        try {
            val result = plugin.celEngine.evaluateBoolean(condition, context)
            if (!result) return false
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate spawn condition: $condition", e)
            return false
        }
    }

    // 高度な条件評価
    if (!evaluateAdvancedConditions(definition.advancedConditions, world)) {
        return false
    }

    return true
}
```

### 6. スポーンコンテキスト構築

```kotlin
private fun buildSpawnContext(definition: SpawnDefinition, world: World): Map<String, Any> {
    val context = mutableMapOf<String, Any>()

    // ワールド情報
    context["world"] = CELVariableProvider.buildWorldInfo(world)

    // スポーン定義情報
    context["spawn"] = mapOf(
        "mob" to definition.mob,
        "maxNearby" to definition.maxNearby,
        "currentCount" to (spawnCounts[definition.mob] ?: 0)
    )

    // 近くのプレイヤー情報
    val nearbyPlayers = world.players.filter { /* ... */ }
    context["nearbyPlayers"] = mapOf(
        "count" to nearbyPlayers.size,
        "maxLevel" to (nearbyPlayers.maxOfOrNull { it.level } ?: 0),
        "avgLevel" to (nearbyPlayers.map { it.level }.average().takeIf { !it.isNaN() } ?: 0.0)
    )

    // 環境情報
    context["environment"] = mapOf(
        "moonPhase" to (world.fullTime / 24000 % 8).toInt(),
        "dayOfCycle" to (world.fullTime / 24000).toInt(),
        "tickOfDay" to (world.time % 24000).toInt()
    )

    return CELVariableProvider.buildFullContext(context)
}
```

## SpawnDefinition

```kotlin
data class SpawnDefinition(
    val mob: String,                           // スポーンするMob名
    val conditions: List<String> = listOf("true"),  // CEL式条件リスト
    val spawnRate: Int = 20,                   // スポーン試行間隔（tick）
    val maxNearby: Int = 5,                    // 最大同時存在数
    val chunkRadius: Int = 3,                  // スポーン範囲（チャンク）
    val region: SpawnRegion? = null,           // スポーン範囲（座標指定）
    val location: SpawnLocation? = null,       // 固定スポーン位置
    val advancedConditions: AdvancedConditions = AdvancedConditions(),
    val onSpawn: List<SpawnSkillReference> = emptyList()  // スポーン時スキル
)
```

## スポーン条件

### CEL式条件

```yaml
ZombieSpawn:
  mob: "StrongZombie"
  conditions:
    - "world.isNight"                          # 夜間のみ
    - "nearbyPlayers.count >= 2"               # プレイヤー2人以上
    - "nearbyPlayers.avgLevel >= 20"           # 平均レベル20以上
    - "environment.moonPhase == 0"             # 満月
  spawnRate: 20
  maxNearby: 5
```

### 高度な条件

```yaml
BossSpawn:
  mob: "CustomBoss"
  conditions:
    - "world.time >= 18000 && world.time < 24000"  # 夜間（18:00-24:00）
  advancedConditions:
    weatherRequired: "CLEAR"                   # 晴天時のみ
    moonPhase: "FULL_MOON"                     # 満月
    playerLevelMin: 30                         # プレイヤーレベル30以上
    nearbyPlayerCount: 3                       # プレイヤー3人以上
```

## スポーン範囲

### プレイヤー周囲（デフォルト）

```yaml
ZombieSpawn:
  mob: "StrongZombie"
  spawnRate: 20
  maxNearby: 5
  chunkRadius: 3  # プレイヤーから3チャンク = 48ブロック以内
```

実際のスポーン範囲:
- 最小距離: 20ブロック
- 最大距離: 48ブロック（3チャンク）

### 固定位置

```yaml
BossSpawn:
  mob: "CustomBoss"
  location:
    world: "world"
    x: 0
    y: 100
    z: 0
```

### 範囲指定（円形）

```yaml
CircleSpawn:
  mob: "StrongZombie"
  region:
    type: circle
    center:
      x: 0
      z: 0
    radius: 100.0
```

### 範囲指定（箱形）

```yaml
BoxSpawn:
  mob: "StrongZombie"
  region:
    type: box
    min:
      x: -100
      y: 50
      z: -100
    max:
      x: 100
      y: 100
      z: 100
```

## OnSpawnスキル

スポーン直後にスキルを実行できます。

```yaml
ZombieSpawn:
  mob: "StrongZombie"
  onSpawn:
    skills:
      - skill: "SpawnEffect"
        executeDelay: "0ms"
      - skill: "RoarSound"
        executeDelay: "500ms"
```

## 設定

### グローバル設定

```yaml
# config.yml
spawn:
  enabled: true  # スポーンシステムの有効/無効
```

### スポーンレート

```yaml
spawnRate: 20  # 20tick = 1秒ごとにスポーン試行
```

**推奨値**:
- 20tick (1秒): 標準
- 40tick (2秒): 低頻度
- 10tick (0.5秒): 高頻度（負荷注意）

### 最大同時存在数

```yaml
maxNearby: 5  # 同じMobが最大5体まで
```

**動作**:
- この数を超えると新規スポーンをスキップ
- 死亡した場合はカウントが減少

## Folia対応

スポーンシステムはFoliaの地域分割に対応しています。

```kotlin
// Global region dispatcher でスポーンタスク管理
plugin.launch(plugin.globalRegionDispatcher) {
    runSpawnTask(name, definition)
}

// Region dispatcher でスポーン実行
plugin.launch(plugin.regionDispatcher(location)) {
    attemptSpawnForPlayer(name, definition, player)
}
```

**利点**:
- 地域ごとに並列実行
- デッドロック回避
- パフォーマンス向上

## トラブルシューティング

### Mobがスポーンしない

**原因**:
- `spawn.enabled: false`
- 条件が満たされていない
- `maxNearby` に到達している
- プレイヤーがいない

**解決方法**:
```yaml
debug:
  enabled: true
  verbose: true
```

ログで条件評価の結果を確認。

### スポーン頻度が高すぎる

**原因**:
- `spawnRate` が小さい
- `maxNearby` が大きい

**解決方法**:
```yaml
spawnRate: 40  # 2秒に1回
maxNearby: 3   # 最大3体まで
```

### 特定の場所にしかスポーンしない

**原因**:
- `location` で固定位置が指定されている
- `region` で範囲が限定されている

**解決方法**:
`location` と `region` を削除してプレイヤー周囲スポーンに変更。

### CEL式エラー

**原因**:
- 構文エラー
- 変数名が間違っている

**解決方法**:
```kotlin
// CEL式をテスト
val result = plugin.celEngine.evaluate("world.isNight && nearbyPlayers.count >= 2", context)
println("Result: $result")
```

---

**関連ドキュメント**:
- [MobManager](mob-manager.md) - Mob管理システム
- [PacketMob](../entity-system/packet-mob.md) - Mob本体の実装
- [CEL System](../core-systems/cel-engine.md) - CEL式評価
