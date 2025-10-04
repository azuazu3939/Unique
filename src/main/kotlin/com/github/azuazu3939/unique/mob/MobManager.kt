package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.condition.CelCondition
import com.github.azuazu3939.unique.effect.*
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.event.*
import com.github.azuazu3939.unique.skill.BasicSkill
import com.github.azuazu3939.unique.skill.SkillMeta
import com.github.azuazu3939.unique.targeter.*
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.EventUtil
import com.github.azuazu3939.unique.util.TimeParser
import com.github.azuazu3939.unique.util.getSound
import com.github.shynixn.mccoroutine.folia.launch
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection
import java.util.concurrent.ConcurrentHashMap

/**
 * Mob管理クラス
 *
 * Mob定義の読み込みとインスタンス生成
 */
class MobManager(private val plugin: Unique) {

    /**
     * 読み込まれたMob定義
     */
    private val mobDefinitions = ConcurrentHashMap<String, MobDefinition>()

    /**
     * アクティブなMobインスタンス
     */
    private val activeMobs = ConcurrentHashMap<String, MobInstance>()

    /**
     * 初期化
     */
    fun initialize() {
        DebugLogger.info("Initializing MobManager...")

        loadMobDefinitions()

        DebugLogger.info("MobManager initialized (${mobDefinitions.size} mob types loaded)")
    }

    /**
     * Mob定義を読み込み
     */
    fun loadMobDefinitions() {
        mobDefinitions.clear()

        val mobsFolder = plugin.mobsFolder
        if (!mobsFolder.exists() || !mobsFolder.isDirectory) {
            DebugLogger.warn("Mobs folder not found: ${mobsFolder.path}")
            return
        }

        val yamlFiles = mobsFolder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: return

        DebugLogger.debug("Loading mob definitions from ${yamlFiles.size} files...")

        for (file in yamlFiles) {
            try {
                // YamlConfigurationで読み込み
                val yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)

                // トップレベルのキーを取得（Mob名）
                val mobNames = yaml.getKeys(false)

                for (mobName in mobNames) {
                    val section = yaml.getConfigurationSection(mobName) ?: continue

                    try {
                        // Mob定義を構築
                        val mobDef = parseMobDefinition(section)
                        mobDefinitions[mobName] = mobDef

                        DebugLogger.debug("  ✓ Loaded mob: $mobName (${mobDef.type})")
                    } catch (e: Exception) {
                        DebugLogger.error("  ✗ Failed to parse mob: $mobName", e)
                    }
                }

            } catch (e: Exception) {
                DebugLogger.error("Failed to load mob file: ${file.name}", e)
            }
        }

        DebugLogger.info("Loaded ${mobDefinitions.size} mob definitions")
    }

    /**
     * Mob定義を解析
     */
    private fun parseMobDefinition(section: ConfigurationSection): MobDefinition {
        return MobDefinition(
            type = section.getString("Type") ?: "ZOMBIE",
            display = section.getString("Display"),
            health = section.getDouble("Health", -1.0).takeIf { it >= 0 },
            damage = section.getDouble("Damage", -1.0).takeIf { it >= 0 },
            ai = parseAI(section.getConfigurationSection("AI")),
            appearance = parseAppearance(section.getConfigurationSection("Appearance")),
            skills = parseSkills(section.getConfigurationSection("Skills")),
            drops = parseDrops(section.getConfigurationSection("Drops"))
        )
    }

    /**
     * AI設定を解析
     */
    private fun parseAI(section: ConfigurationSection?): MobAI {
        if (section == null) return MobAI()

        return MobAI(
            movementSpeed = section.getDouble("MovementSpeed", 0.25),
            followRange = section.getDouble("FollowRange", 16.0),
            knockbackResistance = section.getDouble("KnockbackResistance", 0.0),
            hasAI = section.getBoolean("HasAI", true),
            hasGravity = section.getBoolean("HasGravity", true)
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
     * スキル設定を解析
     */
    private fun parseSkills(section: ConfigurationSection?): MobSkills {
        if (section == null) return MobSkills()

        return MobSkills(
            onTimer = parseSkillTriggers(section.getConfigurationSection("OnTimer")),
            onDamaged = parseSkillTriggers(section.getConfigurationSection("OnDamaged")),
            onDeath = parseSkillTriggers(section.getConfigurationSection("OnDeath")),
            onSpawn = parseSkillTriggers(section.getConfigurationSection("OnSpawn")),
            onAttack = parseSkillTriggers(section.getConfigurationSection("OnAttack"))
        )
    }

    /**
     * スキルトリガーリストを解析
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseSkillTriggers(section: ConfigurationSection?): List<SkillTrigger> {
        if (section == null) return emptyList()

        // リスト形式で読み込み
        val triggers = mutableListOf<SkillTrigger>()

        // セクション直下がリストの場合
        if (section.isList("")) {
            val list = section.getList("") as? List<Map<String, Any>> ?: return emptyList()

            for (item in list) {
                triggers.add(parseSkillTrigger(item))
            }
        }

        return triggers
    }

    /**
     * 単一スキルトリガーを解析
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseSkillTrigger(map: Map<String, Any>): SkillTrigger {
        val targeterMap = map["targeter"] as? Map<String, Any> ?: mapOf("type" to "Self")
        val metaMap = map["meta"] as? Map<String, Any> ?: emptyMap()
        val skillsList = map["skills"] as? List<Map<String, Any>> ?: emptyList()

        return SkillTrigger(
            name = map["name"] as? String ?: "unnamed",
            interval = (map["interval"] as? Number)?.toInt(),
            condition = map["condition"] as? String ?: "true",
            targeter = parseTargeterDefinition(targeterMap),
            meta = parseSkillMetaDefinition(metaMap),
            skills = skillsList.map { parseSkillReference(it) }
        )
    }

    /**
     * ターゲッター定義を解析
     */
    private fun parseTargeterDefinition(map: Map<String, Any>): TargeterDefinition {
        return TargeterDefinition(
            type = map["type"] as? String ?: "Self",
            range = (map["range"] as? Number)?.toDouble() ?: 16.0,
            maxDistance = (map["maxDistance"] as? Number)?.toDouble() ?: 50.0,
            count = (map["count"] as? Number)?.toInt() ?: 1,
            condition = map["condition"] as? String ?: "true"
        )
    }

    /**
     * スキルメタ定義を解析
     */
    private fun parseSkillMetaDefinition(map: Map<String, Any>): SkillMetaDefinition {
        return SkillMetaDefinition(
            sync = map["sync"] as? Boolean ?: false,
            executeDelay = map["executeDelay"] as? String ?: "0ms",
            effectDelay = map["effectDelay"] as? String ?: "0ms",
            cancelOnDeath = map["cancelOnDeath"] as? Boolean ?: true,
            interruptible = map["interruptible"] as? Boolean ?: false
        )
    }

    /**
     * スキル参照を解析
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseSkillReference(map: Map<String, Any>): SkillReference {
        val metaMap = map["meta"] as? Map<String, Any> ?: emptyMap()
        val effectsList = map["effects"] as? List<Map<String, Any>> ?: emptyList()

        return SkillReference(
            skill = map["skill"] as? String ?: "unknown",
            meta = parseSkillMetaDefinition(metaMap),
            effects = effectsList.map { parseEffectDefinition(it) }
        )
    }

    /**
     * エフェクト定義を解析
     */
    private fun parseEffectDefinition(map: Map<String, Any>): EffectDefinition {
        return EffectDefinition(
            type = map["type"] as? String ?: "unknown",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            strength = (map["strength"] as? Number)?.toDouble() ?: 1.0,
            duration = map["duration"] as? String ?: "0ms",
            amplifier = (map["amplifier"] as? Number)?.toInt() ?: 0,
            particle = map["particle"] as? String,
            sound = map["sound"] as? String,
            count = (map["count"] as? Number)?.toInt() ?: 10,
            message = map["message"] as? String,
            command = map["command"] as? String,
            asOp = map["asOp"] as? Boolean ?: false,
            volume = (map["volume"] as? Number)?.toFloat() ?: 1.0f,
            pitch = (map["pitch"] as? Number)?.toFloat() ?: 1.0f
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

                        drops.add(DropDefinition(
                            item = item,
                            amount = amount,
                            chance = chance,
                            condition = condition
                        ))
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
                    val chance = dropSection.getDouble("chance", 1.0)
                    val condition = dropSection.getString("condition") ?: "true"

                    drops.add(DropDefinition(
                        item = item,
                        amount = amount,
                        chance = chance,
                        condition = condition
                    ))
                } else {
                    // 直接値の場合（キーがアイテム名）
                    when (val value = section.get(key)) {
                        is String -> {
                            // amount指定
                            drops.add(DropDefinition(item = key, amount = value))
                        }
                        is Number -> {
                            // chance指定
                            drops.add(DropDefinition(item = key, chance = value.toDouble()))
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

    /**
     * Mob定義を取得
     */
    fun getMobDefinition(name: String): MobDefinition? {
        return mobDefinitions[name]
    }

    /**
     * すべてのMob定義を取得
     */
    fun getAllMobDefinitions(): Map<String, MobDefinition> {
        return mobDefinitions.toMap()
    }

    /**
     * Mobをスポーン
     */
    suspend fun spawnMob(mobName: String, location: Location): PacketMob? {
        val definition = mobDefinitions[mobName]
        if (definition == null) {
            DebugLogger.error("Mob definition not found: $mobName")
            return null
        }

        // PacketMobを生成
        val entityId = plugin.packetEntityManager.generateEntityId()
        val uuid = plugin.packetEntityManager.generateUUID()

        val mob = PacketMob.builder(
            entityId = entityId,
            uuid = uuid,
            entityType = definition.getEntityType(),
            location = location,
            mobName = definition.getDisplayName()
        )
            .health(definition.getHealth())
            .maxHealth(definition.getHealth())
            .customNameVisible(definition.appearance.customNameVisible)
            .hasAI(definition.ai.hasAI)
            .hasGravity(definition.ai.hasGravity)
            .glowing(definition.appearance.glowing)
            .invisible(definition.appearance.invisible)
            .movementSpeed(definition.ai.movementSpeed)
            .followRange(definition.ai.followRange)
            .knockbackResistance(definition.ai.knockbackResistance)
            .build()

        // スポーンイベント発火（キャンセルされた場合はnullを返す）
        if (EventUtil.callEvent(PacketMobSpawnEvent(mob, location, mobName))) {
            DebugLogger.debug("Mob spawn cancelled by event: $mobName")
            return null
        }

        // エンティティマネージャーに登録
        plugin.packetEntityManager.registerEntity(mob)

        // MobInstanceを作成
        val instance = MobInstance(mobName, mob, definition)
        activeMobs[uuid.toString()] = instance

        // OnSpawnスキルを実行
        executeSkillTriggers(mob, definition.skills.onSpawn, PacketMobSkillEvent.SkillTriggerType.ON_SPAWN)

        DebugLogger.spawn(mobName, location.toString())

        return mob
    }

    /**
     * スキルトリガーを実行
     */
    fun executeSkillTriggers(
        mob: PacketMob,
        triggers: List<SkillTrigger>,
        triggerType: PacketMobSkillEvent.SkillTriggerType
    ) {
        if (triggers.isEmpty()) return

        plugin.launch {
            triggers.forEach { trigger ->
                try {
                    // スキルイベント発火＆キャンセルチェック
                    if (EventUtil.callEvent(PacketMobSkillEvent(mob, trigger.name, triggerType))) {
                        DebugLogger.verbose("Skill ${trigger.name} cancelled by event")
                        return@forEach
                    }

                    // 条件チェック
                    if (!evaluateCondition(trigger.condition, mob)) {
                        DebugLogger.verbose("Skill trigger ${trigger.name} condition not met")
                        return@forEach
                    }

                    // スキル実行
                    executeSkillsForTrigger(mob, trigger)
                    DebugLogger.verbose("Executed skill trigger: ${trigger.name}")

                } catch (e: Exception) {
                    DebugLogger.error("Failed to execute skill trigger: ${trigger.name}", e)
                }
            }
        }
    }

    /**
     * 条件を評価
     */
    private fun evaluateCondition(condition: String, mob: PacketMob): Boolean {
        return condition == "true" || plugin.celEvaluator.evaluatePacketEntityCondition(condition, mob)
    }

    /**
     * トリガーに対してスキルを実行
     */
    private fun executeSkillsForTrigger(mob: PacketMob, trigger: SkillTrigger) {
        val targeter = buildTargeter(trigger.targeter)

        trigger.skills.forEach { skillRef ->
            val skill = BasicSkill(
                id = skillRef.skill,
                meta = buildSkillMeta(skillRef.meta),
                condition = null,
                effects = buildEffects(skillRef.effects)
            )
            plugin.skillExecutor.executeSkill(skill, mob, targeter)
        }
    }

    /**
     * ターゲッターを構築
     */
    private fun buildTargeter(definition: TargeterDefinition): Targeter {
        val condition = if (definition.condition != "true") {
            CelCondition(
                id = "targeter_condition",
                expressions = listOf(definition.condition)
            )
        } else null

        return when (definition.type.lowercase()) {
            "self" -> SelfTargeter(condition = condition)
            "nearestplayer" -> NearestPlayerTargeter(
                range = definition.range,
                condition = condition
            )
            "radiusplayers" -> RadiusPlayersTargeter(
                range = definition.range,
                condition = condition
            )
            "radiusentities" -> RadiusEntitiesTargeter(
                range = definition.range,
                condition = condition
            )
            "lineofsight" -> LineOfSightTargeter(
                maxDistance = definition.maxDistance,
                condition = condition
            )
            else -> SelfTargeter(condition = condition)
        }
    }

    /**
     * エフェクトリストを構築
     */
    private fun buildEffects(definitions: List<EffectDefinition>): List<Effect> {
        return definitions.mapNotNull { def ->
            try {
                buildEffect(def)
            } catch (e: Exception) {
                DebugLogger.error("Failed to build effect: ${def.type}", e)
                null
            }
        }
    }

    /**
     * 単一エフェクトを構築
     */
    private fun buildEffect(def: EffectDefinition): Effect {
        val sync = def.meta.sync

        return when (def.type.lowercase()) {
            "damage" -> DamageEffect(
                amount = def.amount,
                sync = sync
            )
            "heal" -> HealEffect(
                amount = def.amount,
                sync = sync
            )
            "knockback" -> KnockbackEffect(
                strength = def.strength,
                sync = sync
            )
            "particle" -> {
                val particle = try {
                    Particle.valueOf(def.particle ?: "FLAME")
                } catch (e: IllegalArgumentException) {
                    Particle.FLAME
                }
                ParticleEffect(
                    particle = particle,
                    count = def.count,
                    offsetX = def.offsetX,
                    offsetY = def.offsetY,
                    offsetZ = def.offsetZ,
                    sync = sync
                )
            }
            "sound" -> {
                val sound = try {
                    val key = NamespacedKey.minecraft(def.sound ?: "entity.generic.explode")
                    getSound(key)
                } catch (e: IllegalArgumentException) {
                    Sound.ENTITY_GENERIC_EXPLODE
                }
                SoundEffect(
                    sound = sound,
                    volume = def.volume,
                    pitch = def.pitch,
                    sync = sync
                )
            }
            "message" -> MessageEffect(
                message = def.message ?: "",
                sync = sync
            )
            "ignite" -> {
                val duration = TimeParser.parse(def.duration)
                IgniteEffect(
                    duration = duration,
                    sync = sync
                )
            }
            else -> throw IllegalArgumentException("Unknown effect type: ${def.type}")
        }
    }

    /**
     * SkillMetaを構築
     */
    private fun buildSkillMeta(def: SkillMetaDefinition): SkillMeta {
        val executeDelay = TimeParser.parse(def.executeDelay)
        val effectDelay = TimeParser.parse(def.effectDelay)

        return SkillMeta(
            sync = def.sync,
            executeDelay = executeDelay,
            effectDelay = effectDelay,
            cancelOnDeath = def.cancelOnDeath,
            interruptible = def.interruptible
        )
    }

    /**
     * UUIDからMobInstanceを取得
     */
    fun getMobInstance(uuid: java.util.UUID): MobInstance? {
        return activeMobs[uuid.toString()]
    }

    /**
     * EntityIdからMobInstanceを取得
     */
    fun getMobInstance(entityId: Int): MobInstance? {
        val entity = plugin.packetEntityManager.getEntityByEntityId(entityId) as? PacketMob
            ?: return null
        return activeMobs[entity.uuid.toString()]
    }

    /**
     * PacketMobからMobInstanceを取得
     */
    fun getMobInstance(mob: PacketMob): MobInstance? {
        return activeMobs[mob.uuid.toString()]
    }

    /**
     * Mob死亡処理
     */
    suspend fun handleMobDeath(mob: PacketMob, killer: org.bukkit.entity.Player?) {
        val instance = getMobInstance(mob) ?: return
        val definition = instance.definition

        DebugLogger.info("Handling death for mob: ${instance.definitionName}")

        // 死亡イベント発火
        val deathEvent = PacketMobDeathEvent(mob, killer, mutableListOf())
        EventUtil.callEvent(deathEvent)

        // OnDeathスキルトリガー実行
        executeSkillTriggers(mob, definition.skills.onDeath, PacketMobSkillEvent.SkillTriggerType.ON_DEATH)

        // ドロップ処理
        if (killer != null) {
            processDrops(definition, mob.location, killer, deathEvent.drops)
        }

        // MobInstanceを削除
        activeMobs.remove(mob.uuid.toString())

        // 削除イベント発火
        EventUtil.callEvent(PacketMobRemoveEvent(mob, PacketMobRemoveEvent.RemoveReason.DEATH))

        // PacketEntityをアンレジスター（一定時間後）
        kotlinx.coroutines.delay(5000)  // 5秒後にクリーンアップ
        plugin.packetEntityManager.unregisterEntity(mob.uuid)

        DebugLogger.debug("Mob ${instance.definitionName} cleaned up")
    }

    /**
     * ドロップ処理
     */
    private fun processDrops(
        definition: MobDefinition,
        location: Location,
        killer: org.bukkit.entity.Player,
        eventDrops: MutableList<org.bukkit.inventory.ItemStack>
    ) {
        if (definition.drops.isEmpty() && eventDrops.isEmpty()) return

        val world = location.world ?: return

        val context = buildDropContext(killer, location)

        // 定義からのドロップ処理
        definition.drops.forEach { drop ->
            try {
                // 条件＆確率チェック
                if (!shouldDrop(drop, context)) return@forEach

                // アイテムスタック作成＆追加
                createDropItem(drop)?.let { eventDrops.add(it) }

            } catch (e: Exception) {
                DebugLogger.error("Failed to process drop: ${drop.item}", e)
            }
        }

        // 全てドロップ
        eventDrops.forEach { itemStack ->
            world.dropItemNaturally(location, itemStack)
            DebugLogger.debug("Dropped ${itemStack.amount}x ${itemStack.type} at $location")
        }
    }

    /**
     * ドロップするかどうかを判定
     */
    private fun shouldDrop(drop: DropDefinition, context: Map<String, Any>): Boolean {
        // 条件チェック
        if (drop.condition != "true") {
            val conditionMet = plugin.celEngine.evaluateBoolean(drop.condition, context)
            if (!conditionMet) {
                DebugLogger.verbose("Drop condition not met: ${drop.item}")
                return false
            }
        }

        // 確率チェック
        if (Math.random() > drop.chance) {
            DebugLogger.verbose("Drop chance failed: ${drop.item} (${drop.chance})")
            return false
        }

        return true
    }

    /**
     * ドロップアイテムを作成
     */
    private fun createDropItem(drop: DropDefinition): org.bukkit.inventory.ItemStack? {
        val material = org.bukkit.Material.getMaterial(drop.item.uppercase())
        if (material == null) {
            DebugLogger.error("Invalid drop material: ${drop.item}")
            return null
        }

        val amount = drop.getAmount()
        DebugLogger.debug("Added drop: ${amount}x ${drop.item}")
        return org.bukkit.inventory.ItemStack(material, amount)
    }

    /**
     * ドロップコンテキストを構築
     */
    private fun buildDropContext(killer: org.bukkit.entity.Player, location: Location): Map<String, Any> {
        val nearbyPlayers = location.world?.getNearbyEntities(location, 50.0, 50.0, 50.0)
            ?.filterIsInstance<org.bukkit.entity.Player>() ?: emptyList()

        return buildMap {
            put("killer", com.github.azuazu3939.unique.cel.CELVariableProvider.fromPlayer(killer))
            location.world?.let { put("world", com.github.azuazu3939.unique.cel.CELVariableProvider.fromWorld(it)) }
            put("nearbyPlayers", mapOf(
                "count" to nearbyPlayers.size,
                "maxLevel" to (nearbyPlayers.maxOfOrNull { it.level } ?: 0),
                "minLevel" to (nearbyPlayers.minOfOrNull { it.level } ?: 0),
                "avgLevel" to (nearbyPlayers.map { it.level }.average().takeIf { !it.isNaN() } ?: 0.0)
            ))
            put("math", com.github.azuazu3939.unique.cel.CELVariableProvider.getMathFunctions())
            put("string", com.github.azuazu3939.unique.cel.CELVariableProvider.getStringFunctions())
        }
    }

    /**
     * Mob被ダメージ処理
     */
    suspend fun handleMobDamaged(mob: PacketMob, damager: org.bukkit.entity.Entity, damage: Double) {
        val instance = getMobInstance(mob) ?: return
        val definition = instance.definition

        DebugLogger.debug("Mob ${instance.definitionName} damaged by ${damager.type} ($damage damage)")

        // ダメージイベント発火＆キャンセルチェック
        val cause = if (damager is org.bukkit.entity.Player) {
            PacketMobDamageEvent.DamageCause.PLAYER_ATTACK
        } else {
            PacketMobDamageEvent.DamageCause.ENTITY_ATTACK
        }

        val damageEvent = EventUtil.callEventOrNull(PacketMobDamageEvent(mob, damager, damage, cause)) ?: run {
            DebugLogger.verbose("Damage to ${instance.definitionName} cancelled by event")
            return
        }

        val finalDamage = damageEvent.damage

        // ダメージを適用
        mob.damage(finalDamage)

        // OnDamagedスキルトリガー実行
        executeSkillTriggers(mob, definition.skills.onDamaged, PacketMobSkillEvent.SkillTriggerType.ON_DAMAGED)

        // 死亡チェック
        if (mob.isDead) {
            val killer = damager as? org.bukkit.entity.Player
            handleMobDeath(mob, killer)
        }
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("MobManager Debug Info")
        DebugLogger.info("Loaded mob definitions: ${mobDefinitions.size}")
        DebugLogger.info("Active mob instances: ${activeMobs.size}")

        mobDefinitions.forEach { (name, def) ->
            DebugLogger.info("  $name: ${def.type} (HP: ${def.getHealth()})")
        }

        DebugLogger.separator()
    }
}

/**
 * Mobインスタンス
 */
data class MobInstance(
    val definitionName: String,
    val entity: PacketMob,
    val definition: MobDefinition
)