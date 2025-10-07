package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.*
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.event.PacketMobDamageEvent
import com.github.azuazu3939.unique.event.PacketMobSkillEvent
import com.github.azuazu3939.unique.event.PacketMobSpawnEvent
import com.github.azuazu3939.unique.nms.distanceToAsync
import com.github.azuazu3939.unique.nms.getPlayersAsync
import com.github.azuazu3939.unique.skill.BasicSkill
import com.github.azuazu3939.unique.skill.SkillMeta
import com.github.azuazu3939.unique.targeter.*
import com.github.azuazu3939.unique.util.*
import com.github.shynixn.mccoroutine.folia.asyncDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.*
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
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
    }

    /**
     * Mob定義を解析
     */
    private fun parseMobDefinition(section: ConfigurationSection): MobDefinition {
        return MobDefinition(
            type = section.getString("Type") ?: "ZOMBIE",
            display = section.getString("Display"),
            health = section.getDouble("Health", -1.0).takeIf { it >= 0 }.toString(),
            damage = section.getDouble("Damage", -1.0).takeIf { it >= 0 }.toString(),
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
     * スキル設定を解析（超コンパクト構文専用）
     */
    private fun parseSkills(section: ConfigurationSection?): MobSkills {
        if (section == null) return MobSkills.empty()

        // 超コンパクト構文: Skills が直接文字列リストの場合
        val compactSkills = section.getList("")
        if (compactSkills != null && compactSkills.isNotEmpty()) {
            return parseCompactSkills(compactSkills)
        }

        DebugLogger.warn("Skills section is empty or invalid format")
        return MobSkills.empty()
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
        var mob: PacketMob? = null
        var uuid: UUID? = null
        var definition: MobDefinition? = null
        withContext(plugin.asyncDispatcher) {

            definition = mobDefinitions[mobName]
            if (definition == null) {
                DebugLogger.error("Mob definition not found: $mobName")
                return@withContext
            }

            // CEL評価用のコンテキストを構築
            val context = buildMobSpawnContext(location)

            // Health, Damage, Armor, ArmorToughnessを動的に評価
            val evaluatedHealth = evaluateHealth(definition.health, context)
            val evaluatedDamage = evaluateDamage(definition.damage, context)
            val evaluatedArmor = evaluateArmor(definition.armor, context)
            val evaluatedArmorToughness = evaluateArmorToughness(definition.armorToughness, context)

            // PacketMobを生成
            val entityId = plugin.packetEntityManager.generateEntityId()
            uuid = plugin.packetEntityManager.generateUUID()

            mob = PacketMob.builder(
                entityId = entityId,
                uuid = uuid,
                entityType = definition.getEntityType(),
                location = location,
                mobName = definition.getDisplayName()
            )
                .health(evaluatedHealth)
                .maxHealth(evaluatedHealth)
                .damage(evaluatedDamage)
                .armor(evaluatedArmor)
                .armorToughness(evaluatedArmorToughness)
                .damageFormula(definition.damageFormula)
                .customNameVisible(definition.appearance.customNameVisible)
                .hasAI(definition.ai.hasAI)
                .hasGravity(definition.ai.hasGravity)
                .glowing(definition.appearance.glowing)
                .invisible(definition.appearance.invisible)
                .options(definition.options)
                .movementSpeed(definition.ai.movementSpeed)
                .followRange(definition.ai.followRange)
                .knockbackResistance(definition.ai.knockbackResistance)
                .lookAtMovementDirection(definition.ai.lookAtMovementDirection)
                .wallClimbHeight(definition.ai.wallClimbHeight)
                .build()
        }

        if (mob == null || uuid == null || definition == null) {
            return null
        }

        // スポーンイベント発火（キャンセルされた場合はnullを返す）
        if (EventUtil.callEvent(PacketMobSpawnEvent(mob, location, mobName))) {
            DebugLogger.debug("Mob spawn cancelled by event: $mobName")
            return null
        }

        withContext(plugin.asyncDispatcher) {
            plugin.packetEntityManager.registerEntity(mob)

            // MobInstanceを作成
            val instance = MobInstance(mobName, mob, definition)
            activeMobs[uuid.toString()] = instance
        }

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

                    // 条件チェック（onDamagedの場合はdamager情報も含める）
                    val conditionMet = if (triggerType == PacketMobSkillEvent.SkillTriggerType.ON_DAMAGED && mob.combat.lastDamager != null) {
                        evaluateCondition(trigger.condition, mob, mob.combat.lastDamager)
                    } else {
                        evaluateCondition(trigger.condition, mob)
                    }

                    if (!conditionMet) {
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
     * 条件を評価（攻撃者情報なし）
     */
    private fun evaluateCondition(condition: String, mob: PacketMob): Boolean {
        return condition == "true" || plugin.celEvaluator.evaluatePacketEntityCondition(condition, mob)
    }

    /**
     * 条件を評価（攻撃者情報あり）
     */
    private fun evaluateCondition(condition: String, mob: PacketMob, damager: Entity?): Boolean {
        if (condition == "true") return true
        if (damager == null) return plugin.celEvaluator.evaluatePacketEntityCondition(condition, mob)

        return plugin.celEvaluator.evaluatePacketEntityTargetCondition(condition, mob, damager)
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
                effects = buildEffects(skillRef.effects)
            )
            plugin.skillExecutor.executeSkill(skill, mob, targeter)
        }
    }

    /**
     * ターゲッターを構築
     */
    private fun buildTargeter(definition: TargeterDefinition): Targeter {
        // filterはCEL式文字列として直接使用
        val filter = if (definition.condition != "true") {
            definition.condition
        } else null

        return when (definition.type.lowercase()) {
            "self" -> SelfTargeter(filter = filter)
            "nearestplayer" -> NearestPlayerTargeter(
                range = definition.range,
                filter = filter
            )
            "radiusplayers" -> RadiusPlayersTargeter(
                range = definition.range,
                filter = filter
            )
            "radiusentities" -> RadiusEntitiesTargeter(
                range = definition.range,
                filter = filter
            )
            "lineofsight" -> LineOfSightTargeter(
                maxDistance = definition.maxDistance,
                filter = filter
            )
            else -> SelfTargeter(filter = filter)
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
                amount = def.amount.toString(),
                sync = sync
            )
            "heal" -> HealEffect(
                amount = def.amount.toString(),
                sync = sync
            )
            "knockback" -> KnockbackEffect(
                strength = def.strength,
                sync = sync
            )
            "particle" -> {
                val particle = ResourceKeyResolver.resolveParticle(
                    def.particle ?: "FLAME",
                    Particle.FLAME
                )
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
                    sync = sync,
                    category = SoundCategory.HOSTILE
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
    fun getMobInstance(uuid: UUID): MobInstance? {
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
     * MobInstanceを削除
     */
    fun removeMobInstance(uuid: UUID) {
        activeMobs.remove(uuid.toString())?.let {
            DebugLogger.debug("Removed MobInstance: ${it.definitionName}")
        }
    }

    /**
     * ドロップアイテムを計算（リストを返すのみ）
     *
     * @param definition Mob定義
     * @param killer キラー（プレイヤー）
     * @param location ドロップ位置
     * @return ドロップアイテムのリスト
     */
    fun calculateDropItems(
        definition: MobDefinition,
        killer: Player,
        location: Location
    ): List<ItemStack> {
        if (definition.drops.isEmpty()) return emptyList()

        val drops = mutableListOf<ItemStack>()
        val context = buildDropContext(killer, location)

        // 定義からのドロップ処理
        definition.drops.forEach { drop ->
            try {
                // 条件＆確率チェック
                if (!shouldDrop(drop, context)) return@forEach

                // アイテムスタック作成＆追加
                createDropItem(drop, context)?.let { drops.add(it) }

            } catch (e: Exception) {
                DebugLogger.error("Failed to calculate drop: ${drop.item}", e)
            }
        }

        return drops
    }

    /**
     * アイテムをワールドにドロップする
     * 注：この関数は既にregion dispatcherのコンテキスト内で呼ばれることを前提とする
     *
     * @param location ドロップ位置
     * @param items ドロップするアイテムのリスト
     */
    suspend fun dropItemsInWorld(
        location: Location,
        items: List<ItemStack>
    ) {
        if (items.isEmpty()) return

        val world = location.world ?: return

        // 既にregion dispatcherのコンテキスト内にいるので、直接dropItemNaturallyを呼ぶ
        items.forEach { itemStack ->
            world.dropItemNaturally(location, itemStack)
            DebugLogger.debug("Dropped ${itemStack.amount}x ${itemStack.type} at $location")
        }
    }

    /**
     * ドロップ処理（計算 + ワールドにドロップ）
     *
     * @param definition Mob定義
     * @param location ドロップ位置
     * @param killer キラー（プレイヤー）
     * @param eventDrops イベントで追加されたドロップアイテム
     */
    suspend fun processDrops(
        definition: MobDefinition,
        location: Location,
        killer: Player,
        eventDrops: MutableList<ItemStack>
    ) {
        withContext(plugin.asyncDispatcher) {
            val calculatedDrops = calculateDropItems(definition, killer, location)
            eventDrops.addAll(calculatedDrops)
        }

        // worldにドロップアイテムをspawnさせるためここはリージョンスケジューラを使用しなくてはならない
        withContext(plugin.regionDispatcher(location)) {
            // ワールドにドロップ
            dropItemsInWorld(location, eventDrops)
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

        // 確率チェック（CEL評価）
        val chanceValue = evaluateDropChance(drop.chance, context)
        if (Math.random() > chanceValue) {
            DebugLogger.verbose("Drop chance failed: ${drop.item} (${chanceValue})")
            return false
        }

        return true
    }

    /**
     * ドロップアイテムを作成
     */
    private fun createDropItem(drop: DropDefinition, context: Map<String, Any>): ItemStack? {
        val material = Material.getMaterial(drop.item.uppercase())
        if (material == null) {
            DebugLogger.error("Invalid drop material: ${drop.item}")
            return null
        }

        val amount = evaluateDropAmount(drop.amount, context)
        DebugLogger.debug("Added drop: ${amount}x ${drop.item}")
        return ItemStack(material, amount)
    }

    private fun buildDropContext(killer: Player, location: Location): Map<String, Any> {
        val world = location.world
        val searchRange = plugin.configManager.mainConfig.performance.contextSearchRange
        val searchRangeSquared = searchRange * searchRange
        val nearbyPlayers = world?.getPlayersAsync()
            ?.filter { it.world.name == world.name && it.distanceToAsync(location) <= searchRangeSquared }
            ?: emptyList()

        val baseContext = buildMap {
            put("killer", CELVariableProvider.buildEntityInfo(killer))
            location.world?.let { put("world", CELVariableProvider.buildWorldInfo(it)) }
            put("nearbyPlayers", mapOf(
                "count" to nearbyPlayers.size,
                "maxLevel" to (nearbyPlayers.maxOfOrNull { it.level } ?: 0),
                "minLevel" to (nearbyPlayers.minOfOrNull { it.level } ?: 0),
                "avgLevel" to (nearbyPlayers.map { it.level }.average().takeIf { !it.isNaN() } ?: 0.0)
            ))
        }
        return CELVariableProvider.buildFullContext(baseContext)
    }

    /**
     * Mob被ダメージ処理
     */
    suspend fun handleMobDamaged(mob: PacketMob, damager: Entity, damage: Double) {
        val instance = getMobInstance(mob) ?: return
        val definition = instance.definition

        DebugLogger.debug("Mob ${instance.definitionName} damaged by ${damager.type} ($damage damage)")

        // ダメージイベント発火＆キャンセルチェック
        val cause = if (damager is Player) {
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

        // 死亡チェック - PacketMob.kill()で既に処理されているので、ここでは何もしない
        // PacketEntityManagerが dead_entity_cleanup_ticks 後に cleanup() を呼び出す
    }

    /**
     * Mobスポーン時のCELコンテキストを構築
     * 注：この関数は既にregion dispatcherのコンテキスト内で呼ばれることを前提とする
     * 最適化：getNearbyEntitiesの代わりにworld.playersを使用
     */
    private fun buildMobSpawnContext(location: Location): Map<String, Any> {
        val world = location.world ?: return emptyMap()

        // 最適化：getNearbyEntitiesは重いので、world.playersから距離チェック
        val searchRange = plugin.configManager.mainConfig.performance.contextSearchRange
        val searchRangeSquared = searchRange * searchRange
        val nearbyPlayers = world.players
            .filter { it.location.world == world && it.location.distanceSquared(location) <= searchRangeSquared }

        val baseContext = buildMap {
            put("world", CELVariableProvider.buildWorldInfo(world))
            put("location", CELVariableProvider.buildLocationInfo(location))
            put("nearbyPlayers", mapOf(
                "count" to nearbyPlayers.size,
                "maxLevel" to (nearbyPlayers.maxOfOrNull { it.level } ?: 0),
                "minLevel" to (nearbyPlayers.minOfOrNull { it.level } ?: 0),
                "avgLevel" to (nearbyPlayers.map { it.level }.average().takeIf { !it.isNaN() } ?: 0.0)
            ))
            put("nearbyPlayerCount", nearbyPlayers.size)
        }

        return CELVariableProvider.buildFullContext(baseContext)
    }

    /**
     * Health値を評価（CEL式対応）
     */
    private fun evaluateHealth(healthExpression: String?, context: Map<String, Any>): Double {
        if (healthExpression == null || healthExpression.equals("null", ignoreCase = true)) return 20.0

        return try {
            // 固定値ならそのまま返す
            healthExpression.toDoubleOrNull() ?: run {
                // CEL式として評価
                plugin.celEvaluator.evaluateNumber(healthExpression, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate health: $healthExpression", e)
            20.0
        }
    }

    /**
     * Damage値を評価（CEL式対応）
     */
    private fun evaluateDamage(damageExpression: String?, context: Map<String, Any>): Double {
        if (damageExpression == null || damageExpression.equals("null", ignoreCase = true)) return 5.0

        return try {
            damageExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(damageExpression, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage: $damageExpression", e)
            5.0
        }
    }

    /**
     * Armor値を評価（CEL式対応）
     */
    private fun evaluateArmor(armorExpression: String?, context: Map<String, Any>): Double {
        if (armorExpression == null || armorExpression.equals("null", ignoreCase = true)) return 0.0

        return try {
            armorExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(armorExpression, context).coerceIn(0.0, 30.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate armor: $armorExpression", e)
            0.0
        }
    }

    /**
     * ArmorToughness値を評価（CEL式対応）
     */
    private fun evaluateArmorToughness(armorToughnessExpression: String?, context: Map<String, Any>): Double {
        if (armorToughnessExpression == null || armorToughnessExpression.equals("null", ignoreCase = true)) return 0.0

        return try {
            armorToughnessExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(armorToughnessExpression, context).coerceIn(0.0, 20.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate armor toughness: $armorToughnessExpression", e)
            0.0
        }
    }

    /**
     * ドロップ個数を評価（CEL式対応、範囲形式も対応）
     */
    private fun evaluateDropAmount(amountExpression: String, context: Map<String, Any>): Int {
        return try {
            // 範囲形式 "1-3" のチェック
            if (amountExpression.contains("-") && amountExpression.toIntOrNull() == null) {
                val parts = amountExpression.split("-")
                val min = parts[0].trim().toIntOrNull() ?: 1
                val max = parts[1].trim().toIntOrNull() ?: min
                return (min..max).random()
            }

            // 固定値または CEL式
            amountExpression.toIntOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(amountExpression, context).toInt().coerceAtLeast(1)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate drop amount: $amountExpression", e)
            1
        }
    }

    /**
     * ドロップ確率を評価（CEL式対応）
     */
    private fun evaluateDropChance(chanceExpression: String, context: Map<String, Any>): Double {
        return try {
            chanceExpression.toDoubleOrNull() ?: run {
                plugin.celEvaluator.evaluateNumber(chanceExpression, context).coerceIn(0.0, 1.0)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate drop chance: $chanceExpression", e)
            1.0
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
            DebugLogger.info("  $name: ${def.type} (HP: ${def.health})")
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