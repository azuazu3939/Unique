package com.github.azuazu3939.unique.mob

import com.destroystokyo.paper.ParticleBuilder
import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CelEvaluator
import com.github.azuazu3939.unique.entity.BukkitEntityWrapper
import com.github.azuazu3939.unique.entity.IEntity
import com.github.azuazu3939.unique.event.SkillTrigger
import com.github.azuazu3939.unique.event.TriggerEvent
import com.github.azuazu3939.unique.event.TriggerParser
import com.github.azuazu3939.unique.mob.ai.AIState
import com.github.azuazu3939.unique.mob.ai.AIType
import com.github.azuazu3939.unique.mob.condition.ConditionEvaluator
import com.github.azuazu3939.unique.mob.data.MobDefinition
import com.github.azuazu3939.unique.mob.targeting.TargetResolver
import com.github.azuazu3939.unique.nms.SchedulerTask
import com.github.azuazu3939.unique.skill.CooldownManager
import com.github.azuazu3939.unique.skill.SkillExecutor
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.shynixn.mccoroutine.folia.launch
import me.tofaa.entitylib.meta.types.LivingEntityMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import me.tofaa.entitylib.wrapper.WrapperLivingEntity
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*
import java.util.UUID.randomUUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.random.Random

/**
 * Manages custom mobs using EntityLib for Folia-compatible async entities
 */
class MobManager(private val plugin: Unique) {

    private val celEvaluator = CelEvaluator()
    private val mobDefinitions = ConcurrentHashMap<String, MobDefinition>()
    internal val activeMobs = ConcurrentHashMap<UUID, UniqueMob>()
    private var tickTask: SchedulerTask? = null
    private val platformScheduler = plugin.platformScheduler

    // Config settings
    private var tickRate: Long = 1L
    private var maxDetectionRange: Double = 64.0
    private var aiEnabled: Boolean = true
    private var dropsEnabled: Boolean = true
    private var maxMobsPerWorld: Int = 100

    init {
        startTicking()
    }

    /**
     * Set tick rate for mob AI updates
     */
    fun setTickRate(rate: Long) {
        if (tickRate != rate) {
            tickRate = rate
            stopTicking()
            startTicking()
            plugin.debugLogger.detailed("Tick rate set to $rate")
        }
    }

    /**
     * Set maximum detection range for mobs
     */
    fun setMaxDetectionRange(range: Double) {
        maxDetectionRange = range
        plugin.debugLogger.detailed("Max detection range set to $range")
    }

    /**
     * Enable or disable mob AI
     */
    fun setAiEnabled(enabled: Boolean) {
        aiEnabled = enabled
        plugin.debugLogger.important("Mob AI ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Enable or disable mob drops
     */
    fun setDropsEnabled(enabled: Boolean) {
        dropsEnabled = enabled
        plugin.debugLogger.important("Mob drops ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Set maximum mobs per world
     */
    fun setMaxMobsPerWorld(max: Int) {
        maxMobsPerWorld = max
        plugin.debugLogger.detailed("Max mobs per world set to $max")
    }

    /**
     * Check if AI is enabled
     */
    fun isAiEnabled(): Boolean = aiEnabled

    /**
     * Check if drops are enabled
     */
    fun isDropsEnabled(): Boolean = dropsEnabled

    /**
     * Start the mob ticking system using platform scheduler
     */
    private fun startTicking() {
        tickTask = platformScheduler.runTaskTimer(plugin, {
            tickAllMobs()
        }, 0L, tickRate)
        plugin.debugLogger.detailed("Mob ticking started (platform: ${if (platformScheduler.isFolia()) "Folia" else "Paper"})")
    }

    /**
     * Stop the mob ticking system
     */
    private fun stopTicking() {
        tickTask?.cancel()
        tickTask = null
        plugin.debugLogger.detailed("Mob ticking stopped")
    }

    /**
     * Tick all active mobs
     */
    private fun tickAllMobs() {
        if (!aiEnabled) return

        activeMobs.values.forEach { mob ->
            try {
                // Find nearby players within follow range
                val followRange = (mob.resolvedAI?.followRange ?: 16.0).coerceAtMost(maxDetectionRange)
                val nearbyPlayers = mob.getLocation().world?.getNearbyPlayers(
                    mob.getLocation(),
                    followRange
                ) ?: emptyList()

                mob.tick(nearbyPlayers.toList())
            } catch (e: Exception) {
                plugin.debugLogger.error("Error ticking mob ${mob.definition.id}: ${e.message}")
                if (plugin.debugLogger.getDebugLevel() >= 3) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Register a mob definition
     */
    fun registerMob(definition: MobDefinition) {
        mobDefinitions[definition.id] = definition
        plugin.debugLogger.detailed("Registered mob: ${definition.id}")
    }

    /**
     * Unregister a mob definition
     */
    fun unregisterMob(id: String) {
        mobDefinitions.remove(id)
    }

    /**
     * Get a mob definition by ID
     */
    fun getMobDefinition(id: String): MobDefinition? {
        return mobDefinitions[id]
    }

    /**
     * Get all registered mob definitions
     */
    fun getAllMobDefinitions(): Collection<MobDefinition> {
        return mobDefinitions.values
    }

    /**
     * Spawn a custom mob at a location
     */
    fun spawnMob(mobId: String, location: Location, viewers: Collection<Player> = location.world.players): UniqueMob? {
        val definition = mobDefinitions[mobId] ?: run {
            plugin.debugLogger.warning("Attempted to spawn unknown mob: $mobId")
            return null
        }

        // Check max mobs per world limit
        if (maxMobsPerWorld > 0) {
            val mobsInWorld = activeMobs.values.count { it.getLocation().world == location.world }
            if (mobsInWorld >= maxMobsPerWorld) {
                plugin.debugLogger.warning("Cannot spawn mob: Maximum mobs per world ($maxMobsPerWorld) reached")
                return null
            }
        }

        val customMob = UniqueMob(
            plugin = plugin,
            manager = this,
            definition = definition,
            location = location,
            celEvaluator = celEvaluator
        )

        activeMobs[customMob.uuid] = customMob
        customMob.spawn(viewers)

        return customMob
    }

    /**
     * Remove a custom mob
     */
    fun removeMob(uuid: UUID) {
        activeMobs.remove(uuid)?.let { mob ->
            mob.despawn()
            plugin.debugLogger.detailed("Removed mob: ${mob.definition.id}")
        }
    }

    /**
     * Get an active mob by UUID
     */
    fun getMob(uuid: UUID): UniqueMob? {
        return activeMobs[uuid]
    }

    /**
     * Get all active mobs
     */
    fun getActiveMobs(): Collection<UniqueMob> {
        return activeMobs.values
    }

    /**
     * Clear all mob definitions and despawn all active mobs
     */
    fun clear() {
        stopTicking()
        activeMobs.values.forEach { it.despawn() }
        activeMobs.clear()
        mobDefinitions.clear()
        celEvaluator.clearCache()
    }

    /**
     * Reload all mob definitions
     */
    fun reload() {
        // Stop ticking temporarily
        stopTicking()

        // Despawn all active mobs
        val despawnedCount = activeMobs.size
        activeMobs.values.forEach { it.despawn() }
        activeMobs.clear()

        // Clear CEL cache
        celEvaluator.clearCache()

        // Restart ticking
        startTicking()

        plugin.debugLogger.detailed("MobManager reloaded: ${mobDefinitions.size} definitions, $despawnedCount mobs despawned")
    }
}

/**
 * Represents a single custom mob instance
 * Implements IEntity to provide a unified interface for both packet and Bukkit entities
 */
class UniqueMob(
    private val plugin: Unique,
    private val manager: MobManager,
    val definition: MobDefinition,
    private var location: Location,
    private val celEvaluator: CelEvaluator
) : IEntity {
    val uuid: UUID = randomUUID()
    private var entity: WrapperEntity? = null
    private var health: Double = definition.health
    private val cooldownManager = CooldownManager()
    private val conditionEvaluator = ConditionEvaluator()
    private val targetResolver = TargetResolver(conditionEvaluator)
    private val skillExecutor = SkillExecutor(plugin, celEvaluator, conditionEvaluator, targetResolver, cooldownManager)
    private val id: Int = Random(uuid.leastSignificantBits).nextInt()

    // Potion effects management for packet entities
    // Map: PotionEffectType -> (PotionEffect, expirationTime in millis)
    private val activePotionEffects = ConcurrentHashMap<PotionEffectType, Pair<PotionEffect, Long>>()

    // Origin tracking for skills
    // Origin represents the location/entity where a skill was executed from
    // This can change during skill execution (e.g., projectile skills)
    private var skillOriginLocation: Location = location.clone()
    private var skillOriginEntity: org.bukkit.entity.LivingEntity? = null

    // Current target for skills
    var currentSkillTarget: org.bukkit.entity.LivingEntity? = null
    var currentTriggerEntity: org.bukkit.entity.LivingEntity? = null

    // Resolved AI behaviors (with references resolved)
    // Resolve AI behaviors
    private val resolvedAIBehaviors: List<com.github.azuazu3939.unique.mob.data.AIBehavior> =
        definition.aiBehaviors.map { behavior ->
            when {
                behavior.aiRef != null -> {
                    val ai = plugin.aiManager.getAI(behavior.aiRef)
                    if (ai != null) {
                        plugin.debugLogger.debug("Resolved AI behavior reference: ${behavior.aiRef} for mob ${definition.id}")
                        behavior.copy(ai = ai, aiRef = null)
                    } else {
                        plugin.debugLogger.warning("AI behavior reference not found: ${behavior.aiRef} for mob ${definition.id}")
                        behavior
                    }
                }
                else -> behavior
            }
        }

    // Current active AI configuration
    internal var resolvedAI: com.github.azuazu3939.unique.mob.data.AIConfig? = null
        private set

    // AI switch tracking
    private var lastAISwitch: Long = System.currentTimeMillis()
    private var lastSkillExecutionTime: Long = 0L

    // Resolved skills with overrides applied
    private val resolvedSkills: List<com.github.azuazu3939.unique.mob.data.SkillConfig>

    // Skill instance metadata (for cooldown groups, GCD, etc.)
    private val skillMetadata = ConcurrentHashMap<String, com.github.azuazu3939.unique.mob.data.SkillInstance>()

    // Trigger-based skill mapping
    private val skillsByTrigger = ConcurrentHashMap<SkillTrigger, MutableList<String>>()

    init {

        // Initialize AI (legacy support)
        resolvedAI = when {
            resolvedAIBehaviors.isNotEmpty() -> {
                // Use first behavior as initial AI
                selectAI()
            }
            definition.aiRef != null -> {
                val ai = plugin.aiManager.getAI(definition.aiRef)
                if (ai != null) {
                    plugin.debugLogger.debug("Resolved AI reference: ${definition.aiRef} for mob ${definition.id}")
                    ai
                } else {
                    plugin.debugLogger.warning("AI reference not found: ${definition.aiRef} for mob ${definition.id}")
                    null
                }
            }
            definition.ai != null -> definition.ai
            else -> null
        }

        // Resolve skill references and combine with inline skills
        val skills = mutableListOf<com.github.azuazu3939.unique.mob.data.SkillConfig>()

        // Add inline skills (legacy)
        skills.addAll(definition.skills)

        // Resolve skill references (legacy)
        definition.skillRefs.forEach { skillId ->
            val skill = plugin.skillManager.getSkill(skillId)
            if (skill != null) {
                skills.add(skill)
                plugin.debugLogger.debug("Resolved skill reference: $skillId for mob ${definition.id}")
            } else {
                plugin.debugLogger.warning("Skill reference not found: $skillId for mob ${definition.id}")
            }
        }

        // Resolve skill instances with overrides (recommended)
        definition.skillInstances.forEach { instance ->
            val baseSkill = plugin.skillManager.getSkill(instance.skillId)
            if (baseSkill != null) {
                val mergedSkill = instance.mergeWithBase(baseSkill)
                skills.add(mergedSkill)
                skillMetadata[mergedSkill.id] = instance
                plugin.debugLogger.debug("Resolved skill instance: ${instance.skillId} for mob ${definition.id}")
            } else {
                plugin.debugLogger.warning("Skill instance base not found: ${instance.skillId} for mob ${definition.id}")
            }
        }

        resolvedSkills = skills

        // Register triggers for all skills
        registerTriggers()
    }

    /**
     * Register all skill triggers
     * Parses trigger notation and registers with appropriate systems
     *
     * Trigger determination priority:
     * 1. SkillInstance.triggerOverride (if present)
     * 2. SkillConfig.trigger (base skill definition)
     * 3. Default to ALWAYS
     *
     * Trigger format:
     * - Trigger notation: "~onAttack", "~onTimer:20", "~onDamaged", etc.
     * - CEL expression (legacy): treated as ALWAYS trigger with condition
     */
    private fun registerTriggers() {
        resolvedSkills.forEach { skill ->
            val instance = skillMetadata[skill.id]

            // Get trigger string from instance override or base skill
            val triggerString = instance?.triggerOverride ?: skill.trigger

            // Parse trigger notation or treat as CEL expression
            val (trigger, interval) = if (triggerString.contains('~')) {
                // Trigger notation: ~onAttack, ~onTimer:20, etc.
                TriggerParser.parse(triggerString)
            } else {
                // CEL expression or legacy format, assume ALWAYS trigger
                // The CEL expression will be evaluated during skill execution
                Pair(SkillTrigger.ALWAYS, null)
            }

            // Add to trigger mapping
            skillsByTrigger.getOrPut(trigger) { mutableListOf() }.add(skill.id)

            // Register with TimerManager if ON_TIMER
            if (trigger == SkillTrigger.ON_TIMER && interval != null) {
                plugin.timerManager.registerTimerSkill(
                    mobUuid = uuid.toString(),
                    skillId = skill.id,
                    intervalTicks = interval
                )
                plugin.debugLogger.debug("Registered timer skill: ${skill.id} with interval $interval ticks for mob ${definition.id}")
            } else {
                plugin.debugLogger.debug("Registered skill: ${skill.id} with trigger $trigger for mob ${definition.id}")
            }
        }

        plugin.debugLogger.debug("Registered ${resolvedSkills.size} skills with triggers for mob ${definition.id}")
    }

    // AI state management
    private var currentTarget: Player? = null
    private var aiState: AIState = AIState.IDLE
    private var stateStartTime: Long = System.currentTimeMillis()
    private var lastTargetSeenTime: Long = 0L
    private var pursuitStartTime: Long = 0L

    // Charge AI state
    private var chargeStartTime: Long = 0L
    private var dashStartLocation: Location? = null
    private var dashDirection: Vector? = null

    // Pathfinding state
    private var currentPath: List<Location>? = null
    private var pathfindingTick: Int = 0

    // Spawn location for territorial AI
    private val spawnLocation: Location = location.clone()

    /**
     * Get the current location of the mob
     */
    fun getLocation(): Location = location.clone()

    /**
     * Get the EntityLib entity ID
     */
    fun getEntityId(): Int = id

    /**
     * Get the current AI state
     */
    fun getAIState(): AIState = aiState

    /**
     * Change AI state
     */
    private fun changeState(newState: AIState) {
        if (aiState != newState) {
            plugin.debugLogger.debug("${definition.id} state: $aiState -> $newState")
            aiState = newState
            stateStartTime = System.currentTimeMillis()
        }
    }

    /**
     * Spawn the mob for viewers
     */
    fun spawn(viewers: Collection<Player>) {
        try {
            // Create EntityLib entity based on entity type
            val wrapperEntity = createEntity(definition.entityType)

            if (wrapperEntity is WrapperLivingEntity) {
                // Configure entity metadata
                val meta = wrapperEntity.entityMeta as? LivingEntityMeta
                meta?.let {
                    // Set custom name
                    it.isCustomNameVisible = true
                    it.customName = Component.text(definition.displayName)

                    // Apply model scale if configured
                    definition.model?.scale?.let { scale ->
                        // Note: Scale requires additional metadata configuration
                        // This depends on the specific entity type
                    }
                }

                // Spawn entity for all viewers
                wrapperEntity.spawn(location.toPacket())
                viewers.forEach { player ->
                    wrapperEntity.addViewer(player.uniqueId)
                }

                entity = wrapperEntity

                // Log spawn with appropriate detail level
                val locationStr = "${location.world?.name} ${location.blockX}, ${location.blockY}, ${location.blockZ}"
                plugin.debugLogger.mobSpawn(definition.id, definition.displayName, locationStr, viewers.size)
            } else {
                plugin.debugLogger.warning("Failed to spawn ${definition.id}: Entity type ${definition.entityType} is not a living entity")
            }
        } catch (e: Exception) {
            plugin.debugLogger.error("Failed to spawn mob ${definition.id}: ${e.message}")
            if (plugin.debugLogger.getDebugLevel() >= 3) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Create an EntityLib entity based on the entity type
     */
    private fun createEntity(type: EntityType): WrapperEntity {
        return WrapperEntity(id, uuid, EntityTypes.getByName(type.name))
    }

    /**
     * Despawn the mob
     */
    fun despawn() {
        // Unregister from timer manager
        plugin.timerManager.unregisterMob(uuid.toString())

        entity?.remove()
        entity = null
    }

    /**
     * Select AI based on conditions and priority
     */
    private fun selectAI(): com.github.azuazu3939.unique.mob.data.AIConfig? {
        if (resolvedAIBehaviors.isEmpty()) return null

        // Find the highest priority behavior whose condition is met
        val selectedBehavior = resolvedAIBehaviors
            .filter { behavior ->
                if (behavior.condition == null) return@filter true

                // Evaluate condition
                val context = CelEvaluator.createContext(
                    player = currentTarget,
                    mobHealth = health,
                    mobMaxHealth = definition.health,
                    targetDistance = currentTarget?.location?.distance(location) ?: Double.MAX_VALUE
                )
                celEvaluator.evaluateBoolean(behavior.condition, context)
            }
            .maxByOrNull { it.priority }

        return selectedBehavior?.ai
    }

    /**
     * Check if AI should be switched
     */
    private fun shouldSwitchAI(): Boolean {
        return when (definition.aiSwitchMode) {
            com.github.azuazu3939.unique.mob.data.AISwitchMode.PERIODIC -> {
                val elapsed = (System.currentTimeMillis() - lastAISwitch) / 1000.0
                elapsed >= definition.aiSwitchInterval
            }
            com.github.azuazu3939.unique.mob.data.AISwitchMode.ON_SKILL_END -> {
                lastSkillExecutionTime > lastAISwitch
            }
            com.github.azuazu3939.unique.mob.data.AISwitchMode.NEVER -> false
        }
    }

    /**
     * Update mob AI tick
     */
    fun tick(nearbyPlayers: List<Player>) {
        if (entity == null) return

        // Switch AI if needed
        if (resolvedAIBehaviors.isNotEmpty() && shouldSwitchAI()) {
            val newAI = selectAI()
            if (newAI != null && newAI != resolvedAI) {
                resolvedAI = newAI
                lastAISwitch = System.currentTimeMillis()
                plugin.debugLogger.debug("${definition.id} switched AI to ${newAI.type}")
            }
        }

        val ai = resolvedAI ?: return

        // Update pathfinding tick counter
        pathfindingTick++

        // State machine based on AI type
        when (ai.type) {
            AIType.SIMPLE -> tickSimpleAI(nearbyPlayers, ai)
            AIType.AGGRESSIVE -> tickAggressiveAI(nearbyPlayers, ai)
            AIType.PATHFINDING -> tickPathfindingAI(nearbyPlayers, ai)
            AIType.CHARGE -> tickChargeAI(nearbyPlayers, ai)
            AIType.RANGED -> tickRangedAI(nearbyPlayers, ai)
            AIType.TERRITORIAL -> tickTerritorialAI(nearbyPlayers, ai)
            AIType.STATIONARY -> tickStationaryAI(nearbyPlayers, ai)
        }
    }

    /**
     * Simple AI - basic movement towards target
     */
    private fun tickSimpleAI(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig) {
        val target = findTarget(nearbyPlayers, ai)
        currentTarget = target

        if (target != null) {
            changeState(AIState.CHASING)
            moveTowards(target.location, ai.moveSpeed)
            executeSkills(target)
        } else {
            changeState(AIState.IDLE)
        }
    }

    /**
     * Aggressive AI - pursues target persistently
     */
    private fun tickAggressiveAI(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig) {
        val currentTime = System.currentTimeMillis()

        // Check if we have a remembered target
        if (currentTarget != null) {
            val target = currentTarget!!

            // Check if target is still valid
            if (!target.isOnline || target.isDead) {
                currentTarget = null
                changeState(AIState.IDLE)
                return
            }

            val distance = location.distance(target.location)

            // Update last seen time if in range
            if (distance <= ai.aggroRange) {
                lastTargetSeenTime = currentTime
            }

            // Check memory duration
            val timeSinceLastSeen = (currentTime - lastTargetSeenTime) / 1000.0
            if (timeSinceLastSeen > ai.memoryDuration) {
                plugin.debugLogger.debug("${definition.id} lost memory of ${target.name}")
                currentTarget = null
                changeState(AIState.IDLE)
                return
            }

            // Check pursuit time limit
            val timeSincePursuitStart = (currentTime - pursuitStartTime) / 1000.0
            if (timeSincePursuitStart > ai.pursuitTime) {
                plugin.debugLogger.detailed("${definition.id} gave up pursuing ${target.name}")
                currentTarget = null
                changeState(AIState.IDLE)
                return
            }

            // Chase target
            changeState(AIState.CHASING)
            moveTowards(target.location, ai.moveSpeed)
            executeSkills(target)
        } else {
            // Find new target
            val target = findTarget(nearbyPlayers, ai, ai.aggroRange)
            if (target != null) {
                currentTarget = target
                lastTargetSeenTime = currentTime
                pursuitStartTime = currentTime
                plugin.debugLogger.detailed("${definition.id} acquired target: ${target.name}")
                changeState(AIState.TARGETING)
            } else {
                changeState(AIState.IDLE)
            }
        }
    }

    /**
     * Charge AI - charges up, then dashes at high speed, turns and repeats
     */
    private fun tickChargeAI(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig) {
        val currentTime = System.currentTimeMillis()

        when (aiState) {
            AIState.IDLE, AIState.TARGETING -> {
                val target = findTarget(nearbyPlayers, ai)
                if (target != null) {
                    currentTarget = target
                    changeState(AIState.CHARGING)
                    chargeStartTime = currentTime
                    plugin.debugLogger.detailed("${definition.id} started charging")
                }
            }

            AIState.CHARGING -> {
                val chargeElapsed = (currentTime - chargeStartTime) / 1000.0

                // Spawn charge particles
                ai.chargeParticle?.let { particleName ->
                    try {
                        val particle = Particle.valueOf(particleName)
                        ParticleBuilder(particle)
                            .location(location)
                            .count(3)
                            .offset(0.5, 0.5, 0.5)
                            .spawn()
                    } catch (e: Exception) {
                        // Invalid particle name
                    }
                }

                // Rotate to face target
                currentTarget?.let { target ->
                    val dx = target.location.x - location.x
                    val dz = target.location.z - location.z
                    location.yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
                    entity?.teleport(location.toPacket())
                }

                // Check if charge is complete
                if (chargeElapsed >= ai.chargeDuration) {
                    changeState(AIState.DASHING)
                    dashStartLocation = location.clone()
                    // Calculate dash direction
                    currentTarget?.let { target ->
                        dashDirection = target.location.toVector().subtract(location.toVector()).normalize()
                    }
                    plugin.debugLogger.detailed("${definition.id} started dashing")
                }
            }

            AIState.DASHING -> {
                dashDirection?.let { direction ->
                    val dashStart = dashStartLocation ?: location
                    val distanceTraveled = location.distance(dashStart)

                    if (distanceTraveled >= ai.dashDistance) {
                        // Dash complete
                        changeState(AIState.COOLDOWN)
                        plugin.debugLogger.detailed("${definition.id} entered cooldown")
                    } else {
                        // Continue dashing
                        val movement = direction.clone().multiply(ai.dashSpeed)
                        val newLocation = location.clone().add(movement)
                        location = newLocation
                        entity?.teleport(location.toPacket())

                        // Damage players in path
                        currentTarget?.let { target ->
                            if (location.distance(target.location) < 2.0) {
                                val damage = ai.damageFormula?.let { formula ->
                                    val context = CelEvaluator.createContext(
                                        player = target,
                                        mobHealth = health,
                                        mobMaxHealth = definition.health,
                                        targetDistance = location.distance(target.location)
                                    )
                                    celEvaluator.evaluateDouble(formula, context)
                                } ?: 10.0
                                target.damage(damage)
                                plugin.debugLogger.detailed("${definition.id} dash hit ${target.name} for $damage damage")
                            }
                        }
                    }
                }
            }

            AIState.COOLDOWN -> {
                val cooldownElapsed = (currentTime - stateStartTime) / 1000.0
                if (cooldownElapsed >= ai.turnCooldown) {
                    changeState(AIState.IDLE)
                }
            }

            else -> {
                changeState(AIState.IDLE)
            }
        }

        // Execute skills if target is close
        currentTarget?.let { target ->
            if (location.distance(target.location) < 5.0) {
                executeSkills(target)
            }
        }
    }

    /**
     * Pathfinding AI - uses Minecraft's pathfinding
     */
    private fun tickPathfindingAI(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig) {
        val target = findTarget(nearbyPlayers, ai)
        currentTarget = target

        if (target != null) {
            changeState(AIState.CHASING)

            // Update path periodically
            if (pathfindingTick % ai.pathfindingUpdateInterval == 0 || currentPath == null) {
                currentPath = calculatePath(location, target.location)
            }

            // Follow path
            currentPath?.let { path ->
                if (path.isNotEmpty()) {
                    val nextPoint = path.first()
                    moveTowards(nextPoint, ai.moveSpeed)

                    // Remove point if reached
                    if (location.distance(nextPoint) < 1.0) {
                        currentPath = path.drop(1)
                    }
                }
            }

            executeSkills(target)
        } else {
            changeState(AIState.IDLE)
            currentPath = null
        }
    }

    /**
     * Ranged AI - maintains distance from target
     */
    private fun tickRangedAI(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig) {
        val target = findTarget(nearbyPlayers, ai)
        currentTarget = target

        if (target != null) {
            val distance = location.distance(target.location)

            when {
                distance < ai.minDistance -> {
                    // Too close, move away
                    changeState(AIState.CHASING)
                    val awayDirection = location.toVector().subtract(target.location.toVector()).normalize()
                    val newLocation = location.clone().add(awayDirection.multiply(ai.moveSpeed))
                    moveTowardsLocation(newLocation, ai.moveSpeed)
                }
                distance > ai.maxDistance -> {
                    // Too far, move closer
                    changeState(AIState.CHASING)
                    moveTowards(target.location, ai.moveSpeed)
                }
                else -> {
                    // In preferred range, stay still and attack
                    changeState(AIState.ATTACKING)
                    // Rotate to face target
                    val dx = target.location.x - location.x
                    val dz = target.location.z - location.z
                    location.yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
                    entity?.teleport(location.toPacket())
                }
            }

            executeSkills(target)
        } else {
            changeState(AIState.IDLE)
        }
    }

    /**
     * Territorial AI - stays within territory radius
     */
    private fun tickTerritorialAI(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig) {
        val distanceFromSpawn = location.distance(spawnLocation)

        if (distanceFromSpawn > ai.territoryRadius) {
            // Outside territory, return to spawn
            changeState(AIState.RETURNING)
            moveTowards(spawnLocation, ai.returnSpeed)
            currentTarget = null
        } else {
            // Inside territory, act normally
            val target = findTarget(nearbyPlayers, ai)
            currentTarget = target

            if (target != null) {
                val distanceToTarget = location.distance(target.location)
                // Check if pursuing target would take us outside territory
                if (distanceToTarget < ai.territoryRadius - distanceFromSpawn) {
                    changeState(AIState.CHASING)
                    moveTowards(target.location, ai.moveSpeed)
                    executeSkills(target)
                } else {
                    changeState(AIState.ATTACKING)
                    // Stay at edge and rotate to face
                    val dx = target.location.x - location.x
                    val dz = target.location.z - location.z
                    location.yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
                    entity?.teleport(location.toPacket())
                    executeSkills(target)
                }
            } else {
                changeState(AIState.IDLE)
            }
        }
    }

    /**
     * Stationary AI - doesn't move, only rotates
     */
    private fun tickStationaryAI(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig) {
        val target = findTarget(nearbyPlayers, ai)
        currentTarget = target

        if (target != null) {
            changeState(AIState.ATTACKING)

            // Rotate to face target if enabled
            if (ai.rotateToTarget) {
                val dx = target.location.x - location.x
                val dz = target.location.z - location.z
                location.yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
                entity?.teleport(location.toPacket())
            }

            executeSkills(target)
        } else {
            changeState(AIState.IDLE)
        }
    }

    /**
     * Find a target from nearby players
     */
    private fun findTarget(nearbyPlayers: List<Player>, ai: com.github.azuazu3939.unique.mob.data.AIConfig, maxRange: Double = ai.followRange): Player? {
        return if (ai.targetCondition != null) {
            nearbyPlayers.firstOrNull { player ->
                val distance = location.distance(player.location)
                if (distance > maxRange) return@firstOrNull false

                val context = CelEvaluator.createContext(
                    player = player,
                    mobHealth = health,
                    mobMaxHealth = definition.health,
                    targetDistance = distance
                )
                celEvaluator.evaluateBoolean(ai.targetCondition, context)
            }
        } else {
            // Default: target nearest player within range
            nearbyPlayers
                .filter { location.distance(it.location) <= maxRange }
                .minByOrNull { it.location.distance(location) }
        }
    }

    /**
     * Execute all skills on target sequentially using coroutines
     * Each skill must complete before the next one starts (including cast time and repeats)
     *
     * This is called by AI tick - triggers ON_ATTACK and ALWAYS skills
     */
    private fun executeSkills(target: Player) {
        // Trigger ON_ATTACK skills
        onAttack(target, currentTriggerEntity)

        // Also trigger ALWAYS skills (cooldown-controlled)
        val alwaysSkillIds = skillsByTrigger[SkillTrigger.ALWAYS] ?: emptyList()
        if (alwaysSkillIds.isEmpty()) return

        // Set origin to current location before executing skills
        skillOriginLocation = location.clone()
        skillOriginEntity = target
        currentSkillTarget = target
        currentTriggerEntity = target

        // Build skill list with metadata
        val alwaysSkills = alwaysSkillIds.mapNotNull { skillId ->
            val skill = resolvedSkills.find { it.id == skillId }
            if (skill != null) {
                skill to skillMetadata[skill.id]
            } else {
                null
            }
        }

        if (alwaysSkills.isEmpty()) return

        // Launch coroutine to execute skills sequentially using SkillExecutor
        val iPlayer = BukkitEntityWrapper(target)
        plugin.launch {
            skillExecutor.executeSkillChain(
                skills = alwaysSkills,
                mobLocation = location,
                mobHealth = health,
                mobMaxHealth = definition.health,
                target = iPlayer,
                trigger = iPlayer,
                originLocation = skillOriginLocation,
                skillMetadata = skillMetadata
            )

            // Track skill execution time for AI switching
            lastSkillExecutionTime = System.currentTimeMillis()
        }
    }

    /**
     * Handle trigger events from event listeners or AI
     * Executes skills that match the trigger type
     */
    fun onTrigger(event: TriggerEvent) {
        // Set origin to current location
        skillOriginLocation = location.clone()
        currentSkillTarget = event.target
        currentTriggerEntity = event.triggerEntity

        // Get skills for this trigger type
        val skillIds = skillsByTrigger[event.trigger] ?: emptyList()
        if (skillIds.isEmpty()) return

        // Build skill list with metadata
        val triggerSkills = skillIds.mapNotNull { skillId ->
            val skill = resolvedSkills.find { it.id == skillId }
            if (skill != null) {
                skill to skillMetadata[skill.id]
            } else {
                null
            }
        }

        if (triggerSkills.isEmpty()) return

        // Launch coroutine to execute triggered skills
        val iTarget = if (event.target == null) null else BukkitEntityWrapper(event.target)
        val iTrigger = if (event.triggerEntity == null) null else BukkitEntityWrapper(event.triggerEntity)
        plugin.launch {
            skillExecutor.executeSkillChain(
                skills = triggerSkills,
                mobLocation = location,
                mobHealth = health,
                mobMaxHealth = definition.health,
                target = iTarget,
                trigger = iTrigger,
                originLocation = skillOriginLocation,
                skillMetadata = skillMetadata
            )

            // Track skill execution time
            lastSkillExecutionTime = System.currentTimeMillis()
        }
    }

    /**
     * Trigger a specific skill (used by TimerManager)
     */
    fun onTriggerSpecificSkill(event: TriggerEvent, skillId: String) {
        // Set origin to current location
        skillOriginLocation = location.clone()
        currentSkillTarget = event.target
        currentTriggerEntity = event.triggerEntity

        // Find the specific skill
        val skill = resolvedSkills.find { it.id == skillId } ?: return
        val instance = skillMetadata[skillId]

        // Launch coroutine to execute the skill
        val iTarget = if (event.target == null) currentTarget?.let { BukkitEntityWrapper(it) } else BukkitEntityWrapper(event.target)
        val iTrigger = if (event.triggerEntity == null) null else BukkitEntityWrapper(event.triggerEntity)
        plugin.launch {
            skillExecutor.executeSkillChain(
                skills = listOf(skill to instance),
                mobLocation = location,
                mobHealth = health,
                mobMaxHealth = definition.health,
                target = iTarget, // Use current AI target if no specific target
                trigger = iTrigger,
                originLocation = skillOriginLocation,
                skillMetadata = skillMetadata
            )

            // Track skill execution time
            lastSkillExecutionTime = System.currentTimeMillis()
        }
    }

    /**
     * Handle damage taken
     * Triggers ON_DAMAGED skills
     */
    fun onDamaged(damager: org.bukkit.entity.LivingEntity?, damage: Double) {
        val event = TriggerEvent(
            trigger = SkillTrigger.ON_DAMAGED,
            triggerEntity = damager,
            target = damager,
            damage = damage
        )
        onTrigger(event)

        // Apply actual damage to health
        this.damage(damage)
    }

    /**
     * Handle attack
     * Triggers ON_ATTACK skills
     */
    fun onAttack(victim: org.bukkit.entity.LivingEntity, attack: org.bukkit.entity.LivingEntity?) {
        val event = TriggerEvent(
            trigger = SkillTrigger.ON_ATTACK,
            triggerEntity = attack,
            target = victim
        )
        onTrigger(event)
    }

    /**
     * Calculate simple pathfinding path
     */
    private fun calculatePath(from: Location, to: Location): List<Location> {
        // Simple pathfinding: create waypoints
        val path = mutableListOf<Location>()
        val distance = from.distance(to)
        val steps = (distance / 2.0).toInt().coerceAtLeast(1)

        for (i in 1..steps) {
            val progress = i.toDouble() / steps
            val x = from.x + (to.x - from.x) * progress
            val y = from.y + (to.y - from.y) * progress
            val z = from.z + (to.z - from.z) * progress
            path.add(Location(from.world, x, y, z))
        }

        return path
    }

    /**
     * Move towards a specific location without rotation
     */
    private fun moveTowardsLocation(targetLocation: Location, speed: Double) {
        if (entity == null || targetLocation.world != location.world) return

        val distance = location.distance(targetLocation)
        if (distance < 0.5) return

        val direction = targetLocation.toVector().subtract(location.toVector()).normalize()
        val movement = direction.multiply(speed)
        val newLocation = location.clone().add(movement)

        location = newLocation
        entity?.teleport(location.toPacket())
    }

    /**
     * Move the mob towards a target location
     */
    private fun moveTowards(targetLocation: Location, speed: Double) {
        if (entity == null || targetLocation.world != location.world) return

        val distance = location.distance(targetLocation)
        if (distance < 1.0) return // Already close enough

        // Calculate direction vector
        val direction = targetLocation.toVector().subtract(location.toVector()).normalize()

        // Calculate new position
        val movement = direction.multiply(speed)
        val newLocation = location.clone().add(movement)

        // Update yaw to face target
        val dx = targetLocation.x - location.x
        val dz = targetLocation.z - location.z
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()

        newLocation.yaw = yaw

        // Update location
        location = newLocation

        // Update entity position
        entity?.teleport(location.toPacket())
    }


    /**
     * Take damage
     */
    fun damage(amount: Double) {
        health = (health - amount).coerceAtLeast(0.0)
        if (health <= 0) {
            onDeath()
        }
    }

    /**
     * Handle mob death
     */
    private fun onDeath() {
        var dropCount = 0

        // Handle drops (only if enabled)
        if (manager.isDropsEnabled()) {
            definition.drops.forEach { dropConfig ->
                try {
                    // Check drop condition
                    val shouldDrop = dropConfig.condition?.let { condition ->
                        val context = CelEvaluator.createContext(
                            player = currentTarget,
                            mobHealth = health,
                            mobMaxHealth = definition.health,
                            targetDistance = currentTarget?.location?.distance(location) ?: Double.MAX_VALUE
                        )
                        celEvaluator.evaluateBoolean(condition, context)
                    } ?: true

                    // Check chance
                    if (shouldDrop && Math.random() <= dropConfig.chance) {
                        val material = org.bukkit.Material.getMaterial(dropConfig.item)
                        if (material != null) {
                            val itemStack = org.bukkit.inventory.ItemStack(material, dropConfig.amount)
                            location.world?.dropItemNaturally(location, itemStack)
                            dropCount++
                        } else {
                            plugin.debugLogger.warning("Unknown material: ${dropConfig.item}")
                        }
                    }
                } catch (e: Exception) {
                    plugin.debugLogger.warning("Failed to drop item ${dropConfig.item}: ${e.message}")
                }
            }
        }

        // Death effects
        ParticleBuilder(Particle.EXPLOSION).location(location).count(3).offset(0.0, 0.0, 0.0).receivers(48).spawn()

        location.world?.playSound(
            location,
            org.bukkit.Sound.ENTITY_GENERIC_DEATH,
            1.0f,
            0.8f
        )

        // Log death with appropriate detail level
        val locationStr = "${location.world?.name} ${location.blockX}, ${location.blockY}, ${location.blockZ}"
        plugin.debugLogger.mobDeath(definition.id, definition.displayName, locationStr, dropCount)

        despawn()

        // Remove from manager
        manager.activeMobs.remove(uuid)
    }

    // ============= IEntity Implementation =============

    override fun getUniqueId(): UUID = uuid

    override fun getLoc(): Location = location

    override fun getEntityType(): EntityType = definition.entityType

    override fun getHealth(): Double = health

    override fun getMaxHealth(): Double = definition.health

    override fun getVelocity(): Vector = Vector(0, 0, 0) // Packet entities don't have velocity

    override fun getPose(): Pose = Pose.STANDING // Default pose for packet entities

    override fun hasAI(): Boolean = resolvedAI != null

    override fun hasGravity(): Boolean = false // Packet entities don't have gravity

    override fun isOnGround(): Boolean = true // Assume on ground for packet entities

    override fun isInWater(): Boolean {
        val block = location.block
        return block.isLiquid
    }

    override fun isGliding(): Boolean = false // Packet entities don't glide

    override fun isSwimming(): Boolean = false // Packet entities don't swim

    override fun isInsideVehicle(): Boolean = false // Packet entities not in vehicles

    override fun getFireTicks(): Int = 0 // Packet entities don't have fire ticks

    override fun getLastDamageCause(): String? = lastDamageType

    override fun getLastDamageTime(): Long = lastSkillExecutionTime

    override fun getNoDamageTicks(): Int = 0 // Packet entities don't have invulnerability

    override fun hasLineOfSight(other: IEntity): Boolean {
        // Simple line of sight check
        val otherLoc = other.getLoc()
        val distance = location.distance(otherLoc)
        if (distance > 100) return false // Too far

        // Check if there are blocks in between
        val direction = otherLoc.toVector().subtract(location.toVector()).normalize()
        var checkLoc = location.clone()
        val step = 0.5

        for (i in 0 until (distance / step).toInt()) {
            checkLoc = checkLoc.add(direction.clone().multiply(step))
            if (checkLoc.block.type.isSolid) {
                return false
            }
        }

        return true
    }

    override fun getBukkitEntity(): org.bukkit.entity.LivingEntity? = null // Packet entities have no Bukkit entity

    override fun isPlayer(): Boolean = false // UniqueMobs are not players

    override fun asPlayer(): Player? = null // UniqueMobs cannot be players

    override fun getPotionEffect(effect: PotionEffectType): PotionEffect? {
        // Clean up expired effects first
        cleanupExpiredEffects()

        // Return active effect
        return activePotionEffects[effect]?.first
    }

    /**
     * Add a potion effect to this packet entity
     */
    override fun addPotionEffect(effect: PotionEffect) {
        val duration = effect.duration
        val expirationTime = if (duration == Int.MAX_VALUE || duration < 0) {
            Long.MAX_VALUE // Infinite duration
        } else {
            System.currentTimeMillis() + (duration * 50L) // Convert ticks to milliseconds
        }

        activePotionEffects[effect.type] = Pair(effect, expirationTime)
        plugin.debugLogger.debug("Added potion effect ${effect.type.key} to mob ${definition.id} (duration: $duration ticks)")
    }

    override fun damage(damage: Double, attacker: IEntity?) {
        val bukkitAttacker = attacker?.getBukkitEntity()

        if (bukkitAttacker != null) {
            // 攻撃者がBukkitEntityの場合、onDamagedを呼ぶ（スキルトリガー付き）
            onDamaged(bukkitAttacker, damage)
        } else {
            // 攻撃者がPacketEntityまたはnullの場合、直接ダメージを適用
            health = (health - damage).coerceAtLeast(0.0)
            if (attacker != null) {
                lastDamageType = "ENTITY_ATTACK"
            }
            if (health <= 0) {
                onDeath()
            }
        }
    }

    /**
     * Remove a potion effect from this packet entity
     */
    fun removePotionEffect(effectType: PotionEffectType) {
        activePotionEffects.remove(effectType)
        plugin.debugLogger.debug("Removed potion effect ${effectType.key} from mob ${definition.id}")
    }

    /**
     * Check if this entity has a specific potion effect
     */
    fun hasPotionEffect(effectType: PotionEffectType): Boolean {
        cleanupExpiredEffects()
        return activePotionEffects.containsKey(effectType)
    }

    /**
     * Get all active potion effects
     */
    fun getActivePotionEffects(): Collection<PotionEffect> {
        cleanupExpiredEffects()
        return activePotionEffects.values.map { it.first }
    }

    /**
     * Clear all potion effects
     */
    fun clearPotionEffects() {
        activePotionEffects.clear()
        plugin.debugLogger.debug("Cleared all potion effects from mob ${definition.id}")
    }

    /**
     * Clean up expired potion effects
     */
    private fun cleanupExpiredEffects() {
        val currentTime = System.currentTimeMillis()
        val expiredEffects = activePotionEffects.entries
            .filter { (_, pair) -> pair.second < currentTime }
            .map { it.key }

        expiredEffects.forEach { effectType ->
            activePotionEffects.remove(effectType)
            plugin.debugLogger.debug("Potion effect ${effectType.key} expired on mob ${definition.id}")
        }
    }

    // Track last damage type for conditions
    private var lastDamageType: String? = null

    /**
     * Set last damage type (for condition evaluation)
     */
    fun setLastDamageType(damageType: String?) {
        this.lastDamageType = damageType
    }
}
