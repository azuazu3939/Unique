package com.github.azuazu3939.unique.config

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.mob.ai.AIType
import com.github.azuazu3939.unique.mob.data.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import java.io.File

class ConfigLoader(private val plugin: Unique) {

    companion object {
        private const val CURRENT_CONFIG_VERSION = 1
    }

    private val aisFolder: File = File(plugin.dataFolder, "AIs")
    private val mobsFolder: File = File(plugin.dataFolder, "Mobs")
    private val skillsFolder: File = File(plugin.dataFolder, "Skills")
    private val configFile: File = File(plugin.dataFolder, "config.yml")

    fun loadAll() {
        loadConfig()
        loadAIs()
        loadSkills()
        loadMobs()
    }

    private fun loadConfig() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        // Save default config if not exists
        if (!configFile.exists()) {
            plugin.saveDefaultConfig()
        }

        // Reload config
        plugin.reloadConfig()

        // Check and update config version
        val config = plugin.config
        val configVersion = config.getInt("config-version", 0)

        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.debugLogger.important("Updating config from version $configVersion to $CURRENT_CONFIG_VERSION")
            updateConfig(configVersion)
            config.set("config-version", CURRENT_CONFIG_VERSION)
            plugin.saveConfig()
        }

        // Load debug level
        val debugLevel = config.getInt("debug-level", 1)
        plugin.debugLogger.setDebugLevel(debugLevel)
        plugin.debugLogger.critical("Debug level set to $debugLevel")

        // Load mob settings
        val tickRate = config.getLong("mobs.tick-rate", 1L)
        plugin.mobManager.setTickRate(tickRate)

        val maxDetectionRange = config.getDouble("mobs.max-detection-range", 64.0)
        plugin.mobManager.setMaxDetectionRange(maxDetectionRange)

        val enableAi = config.getBoolean("mobs.enable-ai", true)
        plugin.mobManager.setAiEnabled(enableAi)

        val enableDrops = config.getBoolean("mobs.enable-drops", true)
        plugin.mobManager.setDropsEnabled(enableDrops)

        // Load performance settings
        // Note: CEL caching is always enabled (no config needed)

        val maxMobsPerWorld = config.getInt("performance.max-mobs-per-world", 100)
        plugin.mobManager.setMaxMobsPerWorld(maxMobsPerWorld)
    }

    /**
     * Update config from old version to new version
     */
    private fun updateConfig(oldVersion: Int) {
        // Future config updates will be handled here
        // Example:
        // when (oldVersion) {
        //     0 -> {
        //         // Update from version 0 to 1
        //         plugin.config.set("new-setting", defaultValue)
        //     }
        // }
        plugin.debugLogger.detailed("No config changes required for version $oldVersion -> $CURRENT_CONFIG_VERSION")
    }

    /**
     * Copy default files from JAR resources to data folder
     */
    private fun copyDefaultFiles(folderName: String) {
        val resourceFiles = when (folderName) {
            "AIs" -> listOf(
                "aggressive_pursuit.yml",
                "charge_dasher.yml",
                "ranged_kiter.yml",
                "territorial_guardian.yml"
            )
            "Skills" -> listOf(
                "common_melee.yml",
                "fire_breath.yml",
                "ground_slam.yml",
                "message.yml",
                "meteor_strike.yml",
                "poison_strike.yml",
                "tail_swipe.yml",
                "test_case_skill.yml"
            )
            "Mobs" -> listOf(
                "condition_example_advanced.yml",
                "condition_example_basic.yml",
                "condition_example_complex.yml",
                "condition_example_target.yml",
                "targeter_example_aoe.yml",
                "targeter_example_mixed.yml",
                "targeter_example_origin.yml",
                "trigger_example_combat.yml",
                "trigger_example_mixed.yml",
                "trigger_example_timer.yml"
            )
            else -> emptyList()
        }

        var copiedCount = 0
        for (fileName in resourceFiles) {
            try {
                val resourcePath = "$folderName/$fileName"
                val inputStream = plugin.getResource(resourcePath)

                if (inputStream != null) {
                    val outputFile = File(plugin.dataFolder, resourcePath)
                    outputFile.parentFile.mkdirs()

                    inputStream.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    copiedCount++
                    plugin.debugLogger.debug("Copied default file: $resourcePath")
                } else {
                    plugin.debugLogger.warning("Default file not found in JAR: $resourcePath")
                }
            } catch (e: Exception) {
                plugin.debugLogger.error("Failed to copy default file $fileName: ${e.message}")
                if (plugin.debugLogger.getDebugLevel() >= 3) {
                    e.printStackTrace()
                }
            }
        }

        if (copiedCount > 0) {
            plugin.debugLogger.detailed("Copied $copiedCount default file(s) to $folderName folder")
        }
    }

    /**
     * Generic YAML loader for skills and mobs
     */
    private fun <T> loadYamlFiles(
        folder: File,
        folderName: String,
        sectionKey: String,
        clearAction: () -> Unit,
        parseAction: (ConfigurationSection) -> T,
        registerAction: (T) -> Unit,
        getIdAction: (T) -> String,
        itemTypeName: String
    ) {
        // Create folder if it doesn't exist
        if (!folder.exists()) {
            folder.mkdirs()
            plugin.debugLogger.detailed("Created $folderName folder")
        }

        // Copy default files from resources if folder is empty
        val existingFiles = folder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: emptyArray()

        if (existingFiles.isEmpty()) {
            copyDefaultFiles(folderName)
        }

        // Clear existing items before reload
        clearAction()

        val ymlFiles = folder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: emptyArray()

        val loadedItems = mutableListOf<String>()
        var loadedCount = 0

        for (file in ymlFiles) {
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                // Case-insensitive: supports lowercase/capitalized/uppercase
                val section = YamlParser.getSection(config, sectionKey) ?: continue

                val item = parseAction(section)
                registerAction(item)
                loadedItems.add("${getIdAction(item)} (${file.name})")
                loadedCount++
            } catch (e: Exception) {
                plugin.debugLogger.error("Failed to load $itemTypeName from ${file.name}: ${e.message}")
                if (plugin.debugLogger.getDebugLevel() >= 3) {
                    e.printStackTrace()
                }
            }
        }

        // Log reload with appropriate detail level
        plugin.debugLogger.reload(
            success = true,
            summary = "Loaded $loadedCount $itemTypeName(s) from ${ymlFiles.size} file(s)",
            details = loadedItems
        )
    }

    private fun loadAIs() {
        loadYamlFiles(
            folder = aisFolder,
            folderName = "AIs",
            sectionKey = "ai",
            clearAction = { plugin.aiManager.clear() },
            parseAction = { section -> parseAISection(section) },
            registerAction = { pair -> plugin.aiManager.registerAI(pair.first, pair.second) },
            getIdAction = { pair -> pair.first },
            itemTypeName = "AI"
        )
    }

    private fun loadSkills() {
        loadYamlFiles(
            folder = skillsFolder,
            folderName = "Skills",
            sectionKey = "skill",
            clearAction = { plugin.skillManager.clear() },
            parseAction = { section -> parseSkillSection(section) },
            registerAction = { skill -> plugin.skillManager.registerSkill(skill) },
            getIdAction = { skill -> skill.id },
            itemTypeName = "skill"
        )
    }

    private fun loadMobs() {
        loadYamlFiles(
            folder = mobsFolder,
            folderName = "Mobs",
            sectionKey = "mob",
            clearAction = { plugin.mobManager.clear() },
            parseAction = { section -> parseMobDefinition(section) },
            registerAction = { mob -> plugin.mobManager.registerMob(mob) },
            getIdAction = { mob -> mob.id },
            itemTypeName = "mob"
        )
    }

    private fun parseMobDefinition(section: ConfigurationSection): MobDefinition {
        val id = YamlParser.getString(section, "id") ?: throw IllegalArgumentException("Mob ID is required")
        val displayName = YamlParser.getString(section, "displayName") ?: id
        val entityType = YamlParser.getString(section, "entityType")?.let {
            EntityType.valueOf(it.uppercase())
        } ?: EntityType.ZOMBIE
        val health = YamlParser.getDouble(section, "health", 20.0)

        // Parse model
        val model = section.getConfigurationSection("model")?.let { modelSection ->
            ModelConfig(
                id = modelSection.getString("id") ?: "default",
                scale = modelSection.getDouble("scale", 1.0),
                viewDistance = modelSection.getDouble("viewDistance", 64.0)
            )
        }

        // Parse AI (inline or reference - legacy)
        val aiRef = YamlParser.getString(section, "AIRef")
        val ai = YamlParser.getSection(section, "AI")?.let { aiSection ->
            parseAIConfig(aiSection)
        }

        // Parse AI behaviors (recommended) - supports AIBehaviors (case-insensitive)
        val aiBehaviors = YamlParser.getList(section, "AIBehaviors")?.mapNotNull { item ->
            if (item == null) return@mapNotNull null
            parseAIBehavior(item)
        } ?: emptyList()

        // Parse AI switch mode - camelCase
        val aiSwitchMode = YamlParser.getString(section, "AISwitchMode")?.let {
            try {
                AISwitchMode.valueOf(it.uppercase())
            } catch (e: Exception) {
                AISwitchMode.PERIODIC
            }
        } ?: AISwitchMode.PERIODIC

        val aiSwitchInterval = YamlParser.getDouble(section, "AISwitchInterval", 3.0)

        // Parse inline skills (lowercase 'skills' for inline definitions)
        val skills = section.getMapList("skills").mapNotNull { skillMap ->
            parseSkill(skillMap)
        }

        // Parse Skills field - supports both simple references and compact notation
        // Examples:
        // - "skill_id" → simple reference
        // - "skill_id{opt=val} @target ~trigger <50% 0.5" → compact notation
        val skillsFromList = YamlParser.getStringList(section, "Skills")?.mapNotNull { skillString ->
            SkillInstanceParser.parse(skillString)
        } ?: emptyList()

        // Parse skill references (legacy - simple IDs only)
        val skillRefs = emptyList<String>()  // Deprecated: use Skills with SkillInstance format

        // Parse skill instances - supports SkillInstances (case-insensitive)
        // Merge with Skills field
        val skillInstances = (YamlParser.getList(section, "SkillInstances")?.mapNotNull { item ->
            if (item == null) return@mapNotNull null
            SkillInstanceParser.parse(item)
        } ?: emptyList()) + skillsFromList

        // Parse drops
        val drops = section.getMapList("drops").mapNotNull { dropMap ->
            parseDrop(dropMap)
        }

        // Parse custom attributes
        val attributes = section.getConfigurationSection("attributes")?.let { attrSection ->
            attrSection.getKeys(false).associateWith { key ->
                attrSection.get(key) ?: ""
            }
        } ?: emptyMap()

        return MobDefinition(
            id = id,
            displayName = displayName,
            entityType = entityType,
            health = health,
            model = model,
            ai = ai,
            aiRef = aiRef,
            aiBehaviors = aiBehaviors,
            aiSwitchMode = aiSwitchMode,
            aiSwitchInterval = aiSwitchInterval,
            skills = skills,
            skillRefs = skillRefs,
            skillInstances = skillInstances,
            drops = drops,
            attributes = attributes
        )
    }

    /**
     * Parse AI preset from AI folder
     * Returns Pair<id, AIConfig>
     */
    private fun parseAISection(section: ConfigurationSection): Pair<String, AIConfig> {
        val id = YamlParser.getString(section, "id") ?: throw IllegalArgumentException("AI ID is required")
        val aiConfig = parseAIConfig(section)
        return Pair(id, aiConfig)
    }

    /**
     * Parse AI behavior from map or section (camelCase fields)
     */
    private fun parseAIBehavior(item: Any): AIBehavior? {
        if (item !is Map<*, *>) return null

        val condition = item["condition"] as? String
        val priority = (item["priority"] as? Number)?.toInt() ?: 0
        val aiRef = item["AIRef"] as? String

        // Parse inline AI with compact notation support
        val ai = item["AI"]?.let { aiData ->
            val compactMap = CompactParser.parseCompactAI(aiData)
            if (compactMap != null) {
                CompactParser.createAIConfig(compactMap)
            } else if (aiData is Map<*, *>) {
                val expandedMap = CompactParser.expandAIMap(aiData)
                CompactParser.createAIConfig(expandedMap)
            } else {
                null
            }
        }

        return AIBehavior(
            condition = condition,
            priority = priority,
            aiRef = aiRef,
            ai = ai
        )
    }

    /**
     * Parse AIConfig from ConfigurationSection (camelCase fields)
     */
    private fun parseAIConfig(aiSection: ConfigurationSection): AIConfig {
        return AIConfig(
            type = AIType.fromString(YamlParser.getString(aiSection, "type") ?: "SIMPLE"),
            targetCondition = YamlParser.getString(aiSection, "targetCondition"),
            damageFormula = YamlParser.getString(aiSection, "damageFormula"),
            moveSpeed = YamlParser.getDouble(aiSection, "moveSpeed", 0.3),
            followRange = YamlParser.getDouble(aiSection, "followRange", 16.0),
            // Aggressive AI settings
            pursuitTime = YamlParser.getDouble(aiSection, "pursuitTime", 30.0),
            memoryDuration = YamlParser.getDouble(aiSection, "memoryDuration", 10.0),
            aggroRange = YamlParser.getDouble(aiSection, "aggroRange", 32.0),
            // Pathfinding settings
            usePathfinding = aiSection.getBoolean("usePathfinding", false),
            pathfindingUpdateInterval = aiSection.getInt("pathfindingUpdateInterval", 20),
            // Charge AI settings
            chargeDuration = YamlParser.getDouble(aiSection, "chargeDuration", 2.0),
            dashSpeed = YamlParser.getDouble(aiSection, "dashSpeed", 2.0),
            dashDistance = YamlParser.getDouble(aiSection, "dashDistance", 20.0),
            turnCooldown = YamlParser.getDouble(aiSection, "turnCooldown", 3.0),
            chargeParticle = YamlParser.getString(aiSection, "chargeParticle"),
            // Ranged AI settings
            preferredDistance = YamlParser.getDouble(aiSection, "preferredDistance", 10.0),
            minDistance = YamlParser.getDouble(aiSection, "minDistance", 5.0),
            maxDistance = YamlParser.getDouble(aiSection, "maxDistance", 20.0),
            // Territorial AI settings
            territoryRadius = YamlParser.getDouble(aiSection, "territoryRadius", 32.0),
            returnSpeed = YamlParser.getDouble(aiSection, "returnSpeed", 0.5),
            // Stationary AI settings
            rotateToTarget = aiSection.getBoolean("rotateToTarget", true)
        )
    }

    private fun parseSkillSection(section: ConfigurationSection): SkillConfig {
        val id = YamlParser.getString(section, "id") ?: throw IllegalArgumentException("Skill ID is required")
        val trigger = YamlParser.getString(section, "trigger") ?: throw IllegalArgumentException("Skill trigger is required")

        // Parse aliases (supports both list and single string)
        val aliases = YamlParser.getStringList(section, "aliases") ?: emptyList()

        val effects = section.getMapList("effects").mapNotNull { effectMap ->
            if (effectMap is Map<*, *>) YamlParser.parseEffect(effectMap) else null
        }

        val particle = YamlParser.getSection(section, "particle")?.let { YamlParser.parseParticleSection(it) }
        val sound = YamlParser.getSection(section, "sound")?.let { YamlParser.parseSoundSection(it) }

        return SkillConfig(
            id = id,
            aliases = aliases,
            trigger = trigger,
            damage = YamlParser.getString(section, "damage"),
            cooldown = YamlParser.getDouble(section, "cooldown", 0.0),
            effects = effects,
            particles = particle,
            sound = sound
        )
    }

    private fun parseSkill(map: Map<*, *>): SkillConfig? {
        val id = map["id"] as? String ?: return null
        val trigger = map["trigger"] as? String ?: return null

        val effects = YamlParser.parseEffects(map["effects"]) ?: emptyList()
        val particle = (map["particle"] as? Map<*, *>)?.let { YamlParser.parseParticle(it) }
        val sound = (map["sound"] as? Map<*, *>)?.let { YamlParser.parseSound(it) }

        return SkillConfig(
            id = id,
            trigger = trigger,
            damage = map["damage"] as? String,
            cooldown = (map["cooldown"] as? Number)?.toDouble() ?: 0.0,
            effects = effects,
            particles = particle,
            sound = sound
        )
    }

    private fun parseDrop(map: Map<*, *>): DropConfig? {
        val item = map["item"] as? String ?: return null
        return DropConfig(
            item = item,
            amount = (map["amount"] as? Number)?.toInt() ?: 1,
            chance = (map["chance"] as? Number)?.toDouble() ?: 1.0,
            condition = map["condition"] as? String
        )
    }

    fun reloadAll() {
        loadAll()
    }
}
