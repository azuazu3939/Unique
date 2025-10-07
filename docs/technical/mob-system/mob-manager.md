# MobManager - Mob管理システム

## 概要

`MobManager` は、Mob定義の読み込み、解析、インスタンス化を管理するクラスです。YAMLファイルからMob定義を読み込み、PacketMobとして生成します。

## クラス構造

```kotlin
class MobManager(private val plugin: Unique) {
    // Mob定義（Mob名 -> 定義）
    private val mobDefinitions = ConcurrentHashMap<String, MobDefinition>()

    // アクティブなMobインスタンス（UUID -> インスタンス）
    private val activeMobs = ConcurrentHashMap<String, MobInstance>()
}
```

## 主要機能

### 1. Mob定義の読み込み

```kotlin
fun loadMobDefinitions() {
    mobDefinitions.clear()

    val mobsFolder = plugin.mobsFolder
    val yamlFiles = mobsFolder.listFiles { file ->
        file.extension == "yml" || file.extension == "yaml"
    }

    for (file in yamlFiles) {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val mobNames = yaml.getKeys(false)

        for (mobName in mobNames) {
            val section = yaml.getConfigurationSection(mobName)
            val mobDef = parseMobDefinition(section)
            mobDefinitions[mobName] = mobDef
        }
    }
}
```

**読み込み元**:
- `plugins/Unique/mobs/*.yml`
- すべてのYAMLファイルを自動検出
- ファイル名は任意（内容の Mob名 がキー）

### 2. Mob定義の解析

```kotlin
private fun parseMobDefinition(section: ConfigurationSection): MobDefinition {
    return MobDefinition(
        type = section.getString("Type") ?: "ZOMBIE",
        display = section.getString("Display"),
        health = section.getDouble("Health", -1.0).takeIf { it >= 0 }.toString(),
        damage = section.getDouble("Damage", -1.0).takeIf { it >= 0 }.toString(),
        armor = section.getDouble("Armor", 0.0).toString(),
        armorToughness = section.getDouble("ArmorToughness", 0.0).toString(),
        damageFormula = section.getString("DamageFormula"),
        ai = parseAI(section.getConfigurationSection("AI")),
        appearance = parseAppearance(section.getConfigurationSection("Appearance")),
        skills = parseSkills(section.getConfigurationSection("Skills")),
        drops = parseDrops(section.getConfigurationSection("Drops")),
        options = parseOptions(section.getConfigurationSection("Options"))
    )
}
```

**Mob定義の要素**:
- 基本プロパティ（Type, Display, Health, Damage等）
- AI設定（MovementSpeed, FollowRange等）
- 外観設定（CustomNameVisible, Glowing等）
- スキル設定（OnTimer, OnDamaged等）
- ドロップ設定（アイテム、確率等）
- オプション設定（無敵、サウンド等）

### 3. Mobのスポーン

```kotlin
suspend fun spawnMob(mobName: String, location: Location): PacketMob? {
    val definition = mobDefinitions[mobName] ?: return null

    // CEL評価用コンテキスト構築
    val context = buildMobSpawnContext(location)

    // 動的評価
    val evaluatedHealth = evaluateHealth(definition.health, context)
    val evaluatedDamage = evaluateDamage(definition.damage, context)
    val evaluatedArmor = evaluateArmor(definition.armor, context)
    val evaluatedArmorToughness = evaluateArmorToughness(definition.armorToughness, context)

    // PacketMob生成
    val mob = PacketMob.builder(...)
        .health(evaluatedHealth)
        .damage(evaluatedDamage)
        .armor(evaluatedArmor)
        .armorToughness(evaluatedArmorToughness)
        .build()

    // スポーンイベント発火
    if (EventUtil.callEvent(PacketMobSpawnEvent(mob, location, mobName))) {
        return null  // キャンセルされた
    }

    // エンティティマネージャーに登録
    plugin.packetEntityManager.registerEntity(mob)

    // MobInstance作成
    val instance = MobInstance(mobName, mob, definition)
    activeMobs[uuid.toString()] = instance

    // OnSpawnスキル実行
    executeSkillTriggers(mob, definition.skills.onSpawn, SkillTriggerType.ON_SPAWN)

    return mob
}
```

### 4. スキルトリガー実行

```kotlin
fun executeSkillTriggers(
    mob: PacketMob,
    triggers: List<SkillTrigger>,
    triggerType: SkillTriggerType
) {
    plugin.launch {
        triggers.forEach { trigger ->
            // スキルイベント発火
            if (EventUtil.callEvent(PacketMobSkillEvent(mob, trigger.name, triggerType))) {
                return@forEach  // キャンセルされた
            }

            // 条件チェック
            if (!evaluateCondition(trigger.condition, mob)) {
                return@forEach
            }

            // スキル実行
            executeSkillsForTrigger(mob, trigger)
        }
    }
}
```

**スキルトリガータイプ**:
- `ON_SPAWN`: スポーン時
- `ON_TIMER`: 定期実行
- `ON_DAMAGED`: ダメージを受けた時
- `ON_DEATH`: 死亡時
- `ON_ATTACK`: 攻撃時

### 5. ドロップ処理

```kotlin
suspend fun processDrops(
    definition: MobDefinition,
    location: Location,
    killer: Player,
    eventDrops: MutableList<ItemStack>
) {
    withContext(plugin.regionDispatcher(location)) {
        // 定義からドロップを計算
        val calculatedDrops = calculateDropItems(definition, killer, location)
        eventDrops.addAll(calculatedDrops)

        // ワールドにドロップ
        dropItemsInWorld(location, eventDrops)
    }
}

private fun shouldDrop(drop: DropDefinition, context: Map<String, Any>): Boolean {
    // 条件チェック
    if (drop.condition != "true") {
        val conditionMet = plugin.celEngine.evaluateBoolean(drop.condition, context)
        if (!conditionMet) return false
    }

    // 確率チェック（CEL評価）
    val chanceValue = evaluateDropChance(drop.chance, context)
    if (Math.random() > chanceValue) return false

    return true
}
```

**ドロップ設定の機能**:
- CEL式による条件評価
- CEL式による確率計算
- CEL式による個数計算
- 範囲指定（例: `amount: "1-3"`）

### 6. CEL式評価

```kotlin
private fun evaluateHealth(healthExpression: String?, context: Map<String, Any>): Double {
    if (healthExpression == null) return 20.0

    return try {
        // 固定値ならそのまま返す
        healthExpression.toDoubleOrNull() ?: run {
            // CEL式として評価
            plugin.celEvaluator.evaluateNumber(healthExpression, context)
        }
    } catch (e: Exception) {
        20.0
    }
}
```

**CEL式で評価可能なプロパティ**:
- `Health`: 体力
- `Damage`: 攻撃力
- `Armor`: 防御力
- `ArmorToughness`: 防御力強度
- `Drop.chance`: ドロップ確率
- `Drop.amount`: ドロップ個数

**使用可能なコンテキスト変数**:
```yaml
world:
  name: "world"
  time: 6000
  isDay: true
  players: 5

nearbyPlayers:
  count: 3
  maxLevel: 50
  avgLevel: 35.5

location:
  x: 100.5
  y: 64.0
  z: -50.3
```

## MobDefinition

```kotlin
data class MobDefinition(
    val type: String,                      // エンティティタイプ
    val display: String?,                  // 表示名
    val health: String?,                   // 体力（CEL式対応）
    val damage: String?,                   // 攻撃力（CEL式対応）
    val armor: String? = "0.0",            // 防御力（CEL式対応）
    val armorToughness: String? = "0.0",   // 防御力強度（CEL式対応）
    val damageFormula: String? = null,     // ダメージ計算式
    val ai: MobAI = MobAI(),               // AI設定
    val appearance: MobAppearance = MobAppearance(),  // 外観設定
    val skills: MobSkills = MobSkills.empty(),        // スキル設定
    val drops: List<DropDefinition> = emptyList(),    // ドロップ設定
    val options: MobOptions = MobOptions()            // オプション設定
)
```

## MobInstance

```kotlin
data class MobInstance(
    val definitionName: String,  // 定義名
    val entity: PacketMob,       // PacketMobエンティティ
    val definition: MobDefinition // Mob定義
)
```

**用途**:
- 定義名からMobインスタンスを取得
- UUIDからMobインスタンスを取得
- EntityIdからMobインスタンスを取得

## スキル設定の解析

### 超コンパクト構文

```yaml
Skills:
  - "projectile{damage=10.0, speed=1.5} @NearestPlayer{range=30.0} ~onTimer:30t"
  - "aura{radius=8.0, duration=5s} @self ~onDamaged{condition='health < maxHealth * 0.5'}"
```

**解析プロセス**:
1. トリガータイプを抽出（`~onTimer`, `~onDamaged` 等）
2. スキル本体をパース（`projectile{...}`）
3. ターゲッターをパース（`@NearestPlayer{...}`）
4. トリガーオプションをパース（`{condition=...}`）

### 詳細構文（従来）

```yaml
Skills:
  OnTimer:
    - name: "FireballAttack"
      interval: "30t"
      condition: "true"
      skill:
        type: projectile
        damage: 10.0
      targeter:
        type: nearestPlayer
        range: 30.0
```

## ドロップ設定の解析

### シンプル形式

```yaml
Drops:
  items:
    - DIAMOND
    - EMERALD
```

### 詳細形式

```yaml
Drops:
  items:
    - item: DIAMOND
      amount: "1-3"                              # 範囲指定
      chance: "0.1 + killer.level * 0.01"       # CEL式
      condition: "killer.level >= 10"           # CEL式
```

### マップ形式

```yaml
Drops:
  diamond:
    amount: "1-3"
    chance: 0.5
  emerald:
    amount: 1
    chance: 0.2
    condition: "nearbyPlayers.count >= 3"
```

## パフォーマンス最適化

### コンテキスト構築の最適化

```kotlin
// 最適化前: getNearbyEntities（重い）
val nearbyPlayers = world.getNearbyEntities(location, range, range, range)
    .filterIsInstance<Player>()

// 最適化後: world.players + 距離チェック
val nearbyPlayers = world.players
    .filter { it.location.distanceSquared(location) <= rangeSquared }
```

### ドロップ計算の非同期化

```kotlin
// ドロップ計算は region dispatcher で実行
withContext(plugin.regionDispatcher(location)) {
    val drops = calculateDropItems(definition, killer, location)
    dropItemsInWorld(location, drops)
}
```

## イベント

### UniqueReloadEvent

リロード前後に発火します。

```kotlin
@EventHandler
fun onReloadBefore(event: UniqueReloadBeforeEvent) {
    // リロード前の処理
}

@EventHandler
fun onReloadAfter(event: UniqueReloadAfterEvent) {
    // リロード後の処理
    // 新しいMob定義を取得
    val newDefinitions = event.plugin.mobManager.getAllMobDefinitions()
}
```

## トラブルシューティング

### Mob定義が読み込まれない

**原因**:
- YAMLファイルの構文エラー
- ファイル名が正しくない（.yml または .yaml）
- mobs フォルダが存在しない

**解決方法**:
```yaml
debug:
  enabled: true
```

ログで読み込みエラーを確認。

### CEL式が評価されない

**原因**:
- 構文エラー
- 変数名が間違っている
- コンテキストに変数が含まれていない

**解決方法**:
```kotlin
// CEL式をテスト
val result = plugin.celEngine.evaluate("100 + nearbyPlayers.count * 50", context)
println("Result: $result")
```

### ドロップアイテムが出ない

**原因**:
- 確率が低い
- 条件が満たされていない
- アイテム名が間違っている

**解決方法**:
```yaml
Drops:
  items:
    - item: DIAMOND
      amount: 1
      chance: 1.0  # 100%ドロップ
      condition: "true"  # 常に条件を満たす
```

---

**関連ドキュメント**:
- [PacketMob](../entity-system/packet-mob.md) - Mob本体の実装
- [Spawn System](spawn-system.md) - 自動スポーンシステム
- [CEL System](../core-systems/cel-engine.md) - CEL式評価
