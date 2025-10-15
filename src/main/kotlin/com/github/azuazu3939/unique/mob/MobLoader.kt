package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.util.CompactSyntaxParser
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Mob定義とスキルライブラリの読み込み
 *
 * MobManagerから分離されたYAML読み込みとパース処理を担当
 */
class MobLoader(private val plugin: Unique) {

    /**
     * スキルライブラリを読み込み
     *
     * @return スキル名をキーとし、インライン構文リストを値とするマップ
     */
    fun loadSkillLibrary(): ConcurrentHashMap<String, List<String>> {
        val skillLibrary = ConcurrentHashMap<String, List<String>>()

        val skillsFolder = File(plugin.dataFolder, "skills")
        if (!skillsFolder.exists() || !skillsFolder.isDirectory) {
            DebugLogger.debug("Skills folder not found: ${skillsFolder.path}")
            return skillLibrary
        }

        val yamlFiles = skillsFolder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: return skillLibrary

        DebugLogger.debug("Loading skill library from ${yamlFiles.size} files...")

        for (file in yamlFiles) {
            try {
                val yaml = YamlConfiguration.loadConfiguration(file)
                val skillNames = yaml.getKeys(false)

                for (skillName in skillNames) {
                    val inlineList = yaml.getStringList(skillName)
                    if (inlineList.isNotEmpty()) {
                        skillLibrary[skillName] = inlineList
                        DebugLogger.debug("  Loaded skill: $skillName (${inlineList.size} effects)")
                    }
                }
            } catch (e: Exception) {
                DebugLogger.error("Failed to load skill file: ${file.name}", e)
            }
        }

        DebugLogger.info("Loaded ${skillLibrary.size} skill definitions from skill library")
        return skillLibrary
    }

    /**
     * Mob定義を読み込み
     *
     * @return Mob名をキーとし、MobDefinitionを値とするマップ
     */
    fun loadMobDefinitions(): ConcurrentHashMap<String, MobDefinition> {
        val mobDefinitions = ConcurrentHashMap<String, MobDefinition>()

        val mobsFolder = plugin.mobsFolder
        if (!mobsFolder.exists() || !mobsFolder.isDirectory) {
            DebugLogger.warn("Mobs folder not found: ${mobsFolder.path}")
            return mobDefinitions
        }

        val yamlFiles = mobsFolder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: return mobDefinitions

        DebugLogger.debug("Loading mob definitions from ${yamlFiles.size} files...")

        for (file in yamlFiles) {
            try {
                // YamlConfigurationで読み込み
                val yaml = YamlConfiguration.loadConfiguration(file)

                // トップレベルのキーを取得（Mob名）
                val mobNames = yaml.getKeys(false)

                for (mobName in mobNames) {
                    val section = yaml.getConfigurationSection(mobName) ?: continue

                    try {
                        // Mob定義を構築
                        val mobDef = parseMobDefinition(section)
                        mobDefinitions[mobName] = mobDef
                    } catch (e: Exception) {
                        DebugLogger.error("  ✗ Failed to parse mob: $mobName", e)
                    }
                }

            } catch (e: Exception) {
                DebugLogger.error("Failed to load mob file: ${file.name}", e)
            }
        }

        DebugLogger.info("Loaded ${mobDefinitions.size} mob definitions")
        return mobDefinitions
    }

    /**
     * Mob定義を解析
     */
    private fun parseMobDefinition(section: ConfigurationSection): MobDefinition {
        return MobDefinition(
            type = section.getString("Type") ?: "ZOMBIE",
            display = section.getString("Display"),
            health = section.getString("Health") ?: section.getDouble("Health", -1.0).takeIf { it >= 0 }?.toString(),
            damage = section.getString("Damage") ?: section.getDouble("Damage", -1.0).takeIf { it >= 0 }?.toString(),
            armor = section.getString("Armor") ?: section.getDouble("Armor", -1.0).takeIf { it >= 0 }?.toString(),
            armorToughness = section.getString("ArmorToughness") ?: section.getDouble("ArmorToughness", -1.0).takeIf { it >= 0 }?.toString(),
            damageFormula = section.getString("DamageFormula"),
            ai = parseAI(section.getConfigurationSection("AI")),
            appearance = parseAppearance(section.getConfigurationSection("Appearance")),
            options = parseOptions(section.getConfigurationSection("Options")),
            skills = parseSkills(section.getList("Skills")),
            drops = parseDrops(section.getConfigurationSection("Drops"))
        )
    }

    /**
     * AI設定を解析
     */
    private fun parseAI(section: ConfigurationSection?): MobAI {
        if (section == null) return MobAI()

        return MobAI(
            behavior = section.getString("Behavior"),
            movement = section.getString("Movement"),
            movementSpeed = section.getDouble("MovementSpeed", 0.25),
            followRange = section.getDouble("FollowRange", 16.0),
            knockbackResistance = section.getDouble("KnockbackResistance", 0.0),
            hasAI = section.getBoolean("HasAI", true),
            hasGravity = section.getBoolean("HasGravity", true),
            lookAtMovementDirection = section.getBoolean("LookAtMovementDirection", true),
            wallClimbHeight = section.getDouble("WallClimbHeight", -1.0).takeIf { it >= 0 },
            jumpStrength = section.getDouble("JumpStrength", -1.0).takeIf { it >= 0 }
        )
    }

    /**
     * 外観設定を解析
     */
    private fun parseAppearance(section: ConfigurationSection?): MobAppearance {
        if (section == null) return MobAppearance()

        return MobAppearance(
            customNameVisible = section.getBoolean("CustomNameVisible", true),
            glowing = section.getBoolean("Glowing", false),
            invisible = section.getBoolean("Invisible", false)
        )
    }

    /**
     * オプション設定を解析
     */
    private fun parseOptions(section: ConfigurationSection?): MobOptions {
        if (section == null) return MobOptions()

        return MobOptions(
            alwaysShowName = section.getBoolean("AlwaysShowName", false),
            collidable = section.getBoolean("Collidable", true),
            despawnOnChunkUnload = section.getBoolean("DespawnOnChunkUnload", true),
            despawnOnLogout = section.getBoolean("DespawnOnLogout", false),
            invincible = section.getBoolean("Invincible", false),
            persistent = section.getBoolean("Persistent", false),
            silent = section.getBoolean("Silent", false),
            playHurtSound = section.getBoolean("PlayHurtSound", true),
            preventItemPickup = section.getBoolean("PreventItemPickup", false),
            preventOtherDrops = section.getBoolean("PreventOtherDrops", false),
            preventTeleporting = section.getBoolean("PreventTeleporting", false),
            preventSunburn = section.getBoolean("PreventSunburn", false),
            preventSlimeSplit = section.getBoolean("PreventSlimeSplit", false),
            preventRenaming = section.getBoolean("PreventRenaming", false),
            preventLeashing = section.getBoolean("PreventLeashing", false),
            targetable = section.getBoolean("Targetable", true),
            spawnInvulnerableTicks = section.getInt("SpawnInvulnerableTicks", 0),
            despawnDistance = section.getDouble("DespawnDistance", 128.0),
            preventDespawn = section.getBoolean("PreventDespawn", false),
            canTakeDamage = section.getBoolean("CanTakeDamage", true),
            immuneToFire = section.getBoolean("ImmuneToFire", false),
            immuneToFall = section.getBoolean("ImmuneToFall", false),
            immuneToDrowning = section.getBoolean("ImmuneToDrowning", false),
            immuneToExplosions = section.getBoolean("ImmuneToExplosions", false),
            noClip = section.getBoolean("NoClip", false),
            noPhysics = section.getBoolean("NoPhysics", false),
            followLeashSpeed = section.getDouble("FollowLeashSpeed", -1.0).takeIf { it >= 0 },
            setAsKiller = section.getBoolean("SetAsKiller", true),
            showDamageAnimation = section.getBoolean("ShowDamageAnimation", true),
            showDamageRanking = section.getBoolean("ShowDamageRanking", false)
        )
    }

    /**
     * スキル設定を解析（超コンパクト構文専用）
     */
    private fun parseSkills(skillsList: List<*>?): MobSkills {
        if (skillsList == null || skillsList.isEmpty()) {
            return MobSkills.empty()
        }

        // 超コンパクト構文: Skills が直接文字列リストの場合
        return parseCompactSkills(skillsList)
    }

    /**
     * 超コンパクト構文のスキルリストを解析
     *
     * 例: ["projectile{...} @NP{r=30.0} ~onTimer:30t", "FireballAttack @TL ~onDamaged"]
     */
    private fun parseCompactSkills(skills: List<*>): MobSkills {
        val onTimerList = mutableListOf<SkillTrigger>()
        val onDamagedList = mutableListOf<SkillTrigger>()
        val onDeathList = mutableListOf<SkillTrigger>()
        val onSpawnList = mutableListOf<SkillTrigger>()
        val onAttackList = mutableListOf<SkillTrigger>()

        for (item in skills) {
            val line = item?.toString() ?: continue

            // トリガータイプを取得
            val triggerType = CompactSyntaxParser.extractTriggerType(line)?.lowercase()

            // スキルトリガーをパース
            val trigger = CompactSyntaxParser.parseSkillTrigger(line) ?: continue

            // トリガータイプ別に振り分け
            when (triggerType) {
                "ontimer" -> onTimerList.add(trigger)
                "ondamaged" -> onDamagedList.add(trigger)
                "ondeath" -> onDeathList.add(trigger)
                "onspawn" -> onSpawnList.add(trigger)
                "onattack" -> onAttackList.add(trigger)
                else -> {
                    DebugLogger.warn("Unknown trigger type: $triggerType in line: $line")
                }
            }
        }

        return MobSkills(
            onTimer = onTimerList,
            onDamaged = onDamagedList,
            onDeath = onDeathList,
            onSpawn = onSpawnList,
            onAttack = onAttackList
        )
    }


    /**
     * ドロップ設定を解析
     */
    private fun parseDrops(section: ConfigurationSection?): List<DropDefinition> {
        if (section == null) return emptyList()

        val drops = mutableListOf<DropDefinition>()

        // リスト形式をチェック
        val dropsList = section.getList("items") ?: section.getList("list")
        if (dropsList != null) {
            // リスト形式の場合
            for (dropEntry in dropsList) {
                when (dropEntry) {
                    is String -> {
                        // シンプルな文字列形式: "DIAMOND"
                        drops.add(DropDefinition(item = dropEntry))
                    }

                    is Map<*, *> -> {
                        // マップ形式の場合
                        @Suppress("UNCHECKED_CAST")
                        val dropMap = dropEntry as Map<String, Any>

                        val item = dropMap["item"] as? String ?: continue
                        val amount = dropMap["amount"]?.toString() ?: "1"
                        val chance = (dropMap["chance"] as? Number)?.toDouble() ?: 1.0
                        val condition = dropMap["condition"] as? String ?: "true"

                        drops.add(
                            DropDefinition(
                                item = item,
                                amount = amount,
                                chance = chance.toString(),
                                condition = condition
                            )
                        )
                    }
                }
            }
        } else {
            // ConfigurationSection形式の場合
            for (key in section.getKeys(false)) {
                // "items"や"list"キーは既にチェック済みなのでスキップ
                if (key == "items" || key == "list") continue

                val dropSection = section.getConfigurationSection(key)
                if (dropSection != null) {
                    // セクション形式
                    val item = dropSection.getString("item") ?: key
                    val amount = dropSection.getString("amount") ?: "1"
                    // chanceもStringで取得（CEL式対応）
                    val chance = dropSection.getString("chance") ?: dropSection.getDouble("chance", 1.0).toString()
                    val condition = dropSection.getString("condition") ?: "true"

                    drops.add(
                        DropDefinition(
                            item = item,
                            amount = amount,
                            chance = chance,
                            condition = condition
                        )
                    )
                } else {
                    // 直接値の場合（キーがアイテム名）
                    when (val value = section.get(key)) {
                        is String -> {
                            // amount指定
                            drops.add(DropDefinition(item = key, amount = value))
                        }

                        is Number -> {
                            // chance指定
                            drops.add(DropDefinition(item = key, chance = value.toString()))
                        }

                        else -> {
                            // デフォルト
                            drops.add(DropDefinition(item = key))
                        }
                    }
                }
            }
        }

        DebugLogger.debug("Parsed ${drops.size} drop definitions")
        return drops
    }
}
