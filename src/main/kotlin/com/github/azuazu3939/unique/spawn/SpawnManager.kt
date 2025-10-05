package com.github.azuazu3939.unique.spawn

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.event.PacketMobSkillEvent
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.TimeParser
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * スポーン管理クラス
 *
 * スポーン定義の読み込みとMobの自動スポーン
 */
class SpawnManager(private val plugin: Unique) {

    /**
     * 読み込まれたスポーン定義
     */
    private val spawnDefinitions = ConcurrentHashMap<String, SpawnDefinition>()

    /**
     * アクティブなスポーンタスク
     */
    private val spawnTasks = ConcurrentHashMap<String, Job>()

    /**
     * スポーンしたMobのカウント（定義名 -> カウント）
     */
    private val spawnCounts = ConcurrentHashMap<String, Int>()

    /**
     * 初期化
     */
    fun initialize() {
        DebugLogger.info("Initializing SpawnManager...")

        loadSpawnDefinitions()

        if (plugin.configManager.mainConfig.spawn.enabled) {
            startSpawnTasks()
        } else {
            DebugLogger.info("Spawn system is disabled in config")
        }

        DebugLogger.info("SpawnManager initialized (${spawnDefinitions.size} spawn definitions loaded)")
    }

    /**
     * シャットダウン
     */
    suspend fun shutdown() {
        DebugLogger.info("Shutting down SpawnManager...")

        // すべてのスポーンタスクを停止
        spawnTasks.values.forEach { it.cancel() }
        spawnTasks.clear()

        DebugLogger.info("SpawnManager shut down")
    }

    /**
     * スポーン定義を読み込み
     */
    fun loadSpawnDefinitions() {
        spawnDefinitions.clear()

        val spawnsFolder = plugin.spawnsFolder
        if (!spawnsFolder.exists() || !spawnsFolder.isDirectory) {
            DebugLogger.warn("Spawns folder not found: ${spawnsFolder.path}")
            return
        }

        val yamlFiles = spawnsFolder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: return

        DebugLogger.debug("Loading spawn definitions from ${yamlFiles.size} files...")

        for (file in yamlFiles) {
            try {
                val yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)

                val spawnNames = yaml.getKeys(false)

                for (spawnName in spawnNames) {
                    val section = yaml.getConfigurationSection(spawnName) ?: continue

                    try {
                        val spawnDef = parseSpawnDefinition(section)
                        spawnDefinitions[spawnName] = spawnDef

                        DebugLogger.debug("  ✓ Loaded spawn: $spawnName (Mob: ${spawnDef.mob})")
                    } catch (e: Exception) {
                        DebugLogger.error("  ✗ Failed to parse spawn: $spawnName", e)
                    }
                }

            } catch (e: Exception) {
                DebugLogger.error("Failed to load spawn file: ${file.name}", e)
            }
        }

        DebugLogger.info("Loaded ${spawnDefinitions.size} spawn definitions")
    }

    /**
     * スポーン定義を解析
     */
    private fun parseSpawnDefinition(section: ConfigurationSection): SpawnDefinition {
        return SpawnDefinition(
            mob = section.getString("Mob") ?: throw IllegalArgumentException("Mob is required"),
            conditions = section.getStringList("Conditions").takeIf { it.isNotEmpty() } ?: listOf("true"),
            spawnRate = section.getInt("SpawnRate", 20),
            maxNearby = section.getInt("MaxNearby", 5),
            chunkRadius = section.getInt("ChunkRadius", 3),
            region = parseRegion(section.getConfigurationSection("Region")),
            location = parseLocation(section.getConfigurationSection("Location")),
            advancedConditions = parseAdvancedConditions(section.getConfigurationSection("AdvancedConditions")),
            onSpawn = parseOnSpawnSkills(section.getConfigurationSection("OnSpawn"))
        )
    }

    /**
     * スポーン範囲を解析
     */
    private fun parseRegion(section: ConfigurationSection?): SpawnRegion? {
        if (section == null) return null

        val type = section.getString("type") ?: return null

        return when (type.lowercase()) {
            "circle" -> {
                val centerSection = section.getConfigurationSection("center")
                SpawnRegion(
                    type = "circle",
                    center = if (centerSection != null) {
                        RegionCenter(
                            x = centerSection.getDouble("x", 0.0),
                            z = centerSection.getDouble("z", 0.0)
                        )
                    } else null,
                    radius = section.getDouble("radius", 100.0)
                )
            }
            "box" -> {
                val minSection = section.getConfigurationSection("min")
                val maxSection = section.getConfigurationSection("max")
                SpawnRegion(
                    type = "box",
                    min = if (minSection != null) {
                        RegionPoint(
                            x = minSection.getDouble("x", 0.0),
                            y = minSection.getDouble("y", 0.0),
                            z = minSection.getDouble("z", 0.0)
                        )
                    } else null,
                    max = if (maxSection != null) {
                        RegionPoint(
                            x = maxSection.getDouble("x", 0.0),
                            y = maxSection.getDouble("y", 0.0),
                            z = maxSection.getDouble("z", 0.0)
                        )
                    } else null
                )
            }
            else -> null
        }
    }

    /**
     * スポーン位置を解析
     */
    private fun parseLocation(section: ConfigurationSection?): SpawnLocation? {
        if (section == null) return null

        return SpawnLocation(
            world = section.getString("world") ?: "world",
            x = section.getDouble("x", 0.0),
            y = section.getDouble("y", 64.0),
            z = section.getDouble("z", 0.0),
            yaw = section.getDouble("yaw", 0.0).toFloat(),
            pitch = section.getDouble("pitch", 0.0).toFloat()
        )
    }

    /**
     * 高度な条件を解析
     */
    private fun parseAdvancedConditions(section: ConfigurationSection?): AdvancedConditions {
        if (section == null) return AdvancedConditions()

        return AdvancedConditions(
            moonPhase = section.getString("moonPhase"),
            weatherRequired = section.getString("weatherRequired"),
            playerLevelMin = section.getInt("playerLevelMin", -1).takeIf { it >= 0 },
            playerLevelMax = section.getInt("playerLevelMax", -1).takeIf { it >= 0 },
            nearbyPlayerCount = section.getInt("nearbyPlayerCount", -1).takeIf { it >= 0 }
        )
    }

    /**
     * OnSpawnスキルを解析
     */
    private fun parseOnSpawnSkills(section: ConfigurationSection?): List<SpawnSkillReference> {
        if (section == null) return emptyList()

        val skills = mutableListOf<SpawnSkillReference>()

        // スキルリストを取得
        val skillsList = section.getList("skills")
        if (skillsList != null) {
            // リスト形式の場合
            for (skillEntry in skillsList) {
                when (skillEntry) {
                    is String -> {
                        // シンプルな文字列形式: "skill_name"
                        skills.add(SpawnSkillReference(skill = skillEntry))
                    }
                    is Map<*, *> -> {
                        // マップ形式の場合
                        @Suppress("UNCHECKED_CAST")
                        val skillMap = skillEntry as Map<String, Any>

                        val skillName = skillMap["skill"] as? String
                            ?: skillMap["name"] as? String
                            ?: continue

                        val meta = parseSpawnSkillMeta(skillMap)
                        skills.add(SpawnSkillReference(skill = skillName, meta = meta))
                    }
                }
            }
        } else {
            // ConfigurationSection形式の場合
            for (key in section.getKeys(false)) {
                val skillSection = section.getConfigurationSection(key) ?: continue

                val skillName = skillSection.getString("skill") ?: key
                val meta = parseSpawnSkillMeta(skillSection)

                skills.add(SpawnSkillReference(skill = skillName, meta = meta))
            }
        }

        return skills
    }

    /**
     * スポーンスキルメタを解析
     */
    private fun parseSpawnSkillMeta(section: ConfigurationSection): SpawnSkillMeta {
        return SpawnSkillMeta(
            executeDelay = section.getString("executeDelay", "0ms") ?: "0ms",
            effectDelay = section.getString("effectDelay", "0ms") ?: "0ms",
            sync = section.getBoolean("sync", false)
        )
    }

    /**
     * スポーンスキルメタを解析（Map版）
     */
    private fun parseSpawnSkillMeta(map: Map<String, Any>): SpawnSkillMeta {
        return SpawnSkillMeta(
            executeDelay = map["executeDelay"] as? String ?: "0ms",
            effectDelay = map["effectDelay"] as? String ?: "0ms",
            sync = map["sync"] as? Boolean ?: false
        )
    }

    /**
     * スポーンタスクを開始
     */
    private fun startSpawnTasks() {
        DebugLogger.info("Starting spawn tasks...")

        for ((name, definition) in spawnDefinitions) {
            val job = plugin.launch {
                runSpawnTask(name, definition)
            }

            spawnTasks[name] = job
        }

        DebugLogger.info("Started ${spawnTasks.size} spawn tasks")
    }

    /**
     * スポーンタスクを実行
     */
    private suspend fun runSpawnTask(name: String, definition: SpawnDefinition) {
        while (true) {
            try {
                // スポーンレートに基づいて待機
                delay(definition.spawnRate * 50L)  // tickをミリ秒に変換

                // スポーン試行
                attemptSpawn(name, definition)

            } catch (e: Exception) {
                DebugLogger.error("Error in spawn task: $name", e)
            }
        }
    }

    /**
     * スポーンを試行
     */
    private suspend fun attemptSpawn(name: String, definition: SpawnDefinition) {
        // ワールド取得
        val world = getSpawnWorld(definition) ?: return

        // 条件評価
        if (!evaluateSpawnConditions(definition, world)) {
            return
        }

        // 最大数チェック
        val currentCount = spawnCounts.getOrDefault(name, 0)
        if (currentCount >= definition.maxNearby) {
            DebugLogger.verbose("Spawn $name: Max nearby reached ($currentCount/${definition.maxNearby})")
            return
        }

        // スポーン位置を決定
        val location = determineSpawnLocation(definition, world) ?: return

        // Mobをスポーン
        val mob = plugin.mobManager.spawnMob(definition.mob, location)

        if (mob != null) {
            // カウント増加
            spawnCounts.compute(name) { _, count -> (count ?: 0) + 1 }

            DebugLogger.debug("Spawned ${definition.mob} at ${location.blockX}, ${location.blockY}, ${location.blockZ} ($name)")

            // OnSpawnスキルを実行
            executeOnSpawnSkills(mob, definition.onSpawn)
        }
    }

    /**
     * OnSpawnスキルを実行
     */
    private suspend fun executeOnSpawnSkills(
        mob: com.github.azuazu3939.unique.entity.PacketMob,
        skillReferences: List<SpawnSkillReference>
    ) {
        if (skillReferences.isEmpty()) return

        for (skillRef in skillReferences) {
            try {
                // スキル取得
                val mobDef = plugin.mobManager.getMobDefinition(mob.mobName)
                if (mobDef == null) {
                    DebugLogger.warn("OnSpawn mob not found: ${mob.mobName}")
                    continue
                }

                val metaDef = mobDef.skills.onSpawn.filter { it.name == skillRef.skill }

                // 遅延処理
                val executeDelay = TimeParser.parse(skillRef.meta.executeDelay)
                if (executeDelay.inWholeMilliseconds > 0) {
                    delay(executeDelay.inWholeMilliseconds)
                }

                plugin.mobManager.executeSkillTriggers(mob, metaDef, PacketMobSkillEvent.SkillTriggerType.ON_SPAWN)
                DebugLogger.debug("Executed OnSpawn skill: ${skillRef.skill} for ${mob.mobName}")

            } catch (e: Exception) {
                DebugLogger.error("Failed to execute OnSpawn skill: ${skillRef.skill}", e)
            }
        }
    }

    /**
     * スポーンワールドを取得
     */
    private fun getSpawnWorld(definition: SpawnDefinition): World? {
        return if (definition.location != null) {
            Bukkit.getWorld(definition.location.world)
        } else {
            // デフォルトワールド
            Bukkit.getWorlds().firstOrNull()
        }
    }

    /**
     * スポーン条件を評価
     */
    private fun evaluateSpawnConditions(definition: SpawnDefinition, world: World): Boolean {
        // CEL条件評価用のコンテキストを構築
        val context = buildSpawnContext(definition, world)

        // 全てのCEL条件を評価（短絡評価）
        for ((index, condition) in definition.conditions.withIndex()) {
            // "true"はスキップ（常にtrue）
            if (condition.trim() == "true") continue

            try {
                val result = plugin.celEngine.evaluateBoolean(condition, context)

                if (!result) {
                    DebugLogger.verbose("Spawn condition #${index + 1} failed: $condition")
                    return false
                }

                DebugLogger.verbose("Spawn condition #${index + 1} passed: $condition")

            } catch (e: Exception) {
                DebugLogger.error("Failed to evaluate spawn condition #${index + 1}: $condition", e)
                DebugLogger.debug("Context: world=${world.name}, time=${world.time}, players=${world.players.size}")
                return false
            }
        }

        // 高度な条件評価（後方互換性のため維持）
        if (!evaluateAdvancedConditions(definition.advancedConditions, world)) {
            DebugLogger.verbose("Advanced conditions failed for spawn: ${definition.mob}")
            return false
        }

        DebugLogger.verbose("All spawn conditions passed for: ${definition.mob}")
        return true
    }

    /**
     * スポーンコンテキストを構築
     */
    private fun buildSpawnContext(definition: SpawnDefinition, world: World): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // ワールド情報（CELVariableProviderを使用）
        context["world"] = CELVariableProvider.buildWorldInfo(world)

        // スポーン定義情報
        context["spawn"] = mapOf(
            "mob" to definition.mob,
            "maxNearby" to definition.maxNearby,
            "chunkRadius" to definition.chunkRadius,
            "currentCount" to (spawnCounts[definition.mob] ?: 0)
        )

        // 近くのプレイヤー情報
        val nearbyPlayers = world.players.filter { player ->
            definition.location?.let { loc ->
                val spawnLoc = Location(world, loc.x, loc.y, loc.z)
                player.location.distance(spawnLoc) < (definition.chunkRadius * 16.0)
            } ?: true
        }

        context["nearbyPlayers"] = mapOf(
            "count" to nearbyPlayers.size,
            "maxLevel" to (nearbyPlayers.maxOfOrNull { it.level } ?: 0),
            "minLevel" to (nearbyPlayers.minOfOrNull { it.level } ?: 0),
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

    /**
     * 高度な条件を評価
     */
    private fun evaluateAdvancedConditions(conditions: AdvancedConditions, world: World): Boolean {
        // 天候条件
        if (conditions.weatherRequired != null) {
            val matches = when (conditions.weatherRequired.uppercase()) {
                "CLEAR" -> !world.hasStorm()
                "RAIN" -> world.hasStorm() && !world.isThundering
                "THUNDER" -> world.isThundering
                else -> true
            }
            if (!matches) return false
        }

        // 月の満ち欠け（簡易実装）
        if (conditions.moonPhase != null) {
            val phase = (world.fullTime / 24000 % 8).toInt()
            val matches = when (conditions.moonPhase.uppercase()) {
                "FULL_MOON" -> phase == 0
                "NEW_MOON" -> phase == 4
                else -> true
            }
            if (!matches) return false
        }

        return true
    }

    /**
     * スポーン位置を決定
     */
    private fun determineSpawnLocation(definition: SpawnDefinition, world: World): Location? {
        // 固定位置が指定されている場合
        if (definition.location != null) {
            val loc = definition.location
            return Location(world, loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
        }

        // 範囲が指定されている場合
        if (definition.region != null) {
            return generateLocationInRegion(definition.region, world)
        }

        // デフォルト: ワールドスポーン付近
        val spawn = world.spawnLocation
        return spawn.clone().add(
            Random.nextDouble(-50.0, 50.0),
            0.0,
            Random.nextDouble(-50.0, 50.0)
        )
    }

    /**
     * 範囲内でランダム位置を生成
     */
    private fun generateLocationInRegion(region: SpawnRegion, world: World): Location? {
        return when (region.type.lowercase()) {
            "circle" -> {
                val center = region.center ?: RegionCenter()
                val radius = region.radius ?: 100.0

                val angle = Random.nextDouble() * 2 * Math.PI
                val r = Random.nextDouble() * radius

                val x = center.x + r * cos(angle)
                val z = center.z + r * sin(angle)
                val y = world.getHighestBlockYAt(x.toInt(), z.toInt()).toDouble() + 1

                Location(world, x, y, z)
            }
            "box" -> {
                val min = region.min ?: RegionPoint(y = 64.0)
                val max = region.max ?: RegionPoint(100.0, 100.0, 100.0)

                val x = Random.nextDouble(min.x, max.x)
                val y = Random.nextDouble(min.y, max.y)
                val z = Random.nextDouble(min.z, max.z)

                Location(world, x, y, z)
            }
            else -> null
        }
    }

    /**
     * スポーン定義を取得
     */
    fun getSpawnDefinition(name: String): SpawnDefinition? {
        return spawnDefinitions[name]
    }

    /**
     * すべてのスポーン定義を取得
     */
    fun getAllSpawnDefinitions(): Map<String, SpawnDefinition> {
        return spawnDefinitions.toMap()
    }

    /**
     * スポーンタスクを再起動
     */
    fun restartSpawnTasks() {
        // 既存タスクを停止
        spawnTasks.values.forEach { it.cancel() }
        spawnTasks.clear()

        // 新しいタスクを開始
        if (plugin.configManager.mainConfig.spawn.enabled) {
            startSpawnTasks()
        }
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("SpawnManager Debug Info")
        DebugLogger.info("Loaded spawn definitions: ${spawnDefinitions.size}")
        DebugLogger.info("Active spawn tasks: ${spawnTasks.size}")
        DebugLogger.info("Spawn counts:")

        spawnCounts.forEach { (name, count) ->
            val definition = spawnDefinitions[name]
            DebugLogger.info("  $name: $count / ${definition?.maxNearby ?: 0}")
        }

        DebugLogger.separator()
    }
}