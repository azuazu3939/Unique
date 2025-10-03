package com.github.azuazu3939.unique.skill

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CelEvaluator
import com.github.azuazu3939.unique.entity.IEntity
import com.github.azuazu3939.unique.mob.condition.ConditionEvaluator
import com.github.azuazu3939.unique.mob.data.SkillConfig
import com.github.azuazu3939.unique.mob.data.SkillInstance
import com.github.azuazu3939.unique.mob.targeting.TargetResolver
import com.github.azuazu3939.unique.nms.PlatformDetector
import com.github.shynixn.mccoroutine.folia.asyncDispatcher
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Player
import kotlin.random.Random

/**
 * Optimized skill executor with async/sync support
 * Executes skills sequentially but leverages async execution where possible
 */
class SkillExecutor(
    private val plugin: Unique,
    private val celEvaluator: CelEvaluator,
    private val conditionEvaluator: ConditionEvaluator,
    private val targetResolver: TargetResolver,
    private val cooldownManager: CooldownManager
) {

    private val platformScheduler = PlatformDetector.getScheduler()
    private val scope = platformScheduler.getCoroutineScope(plugin)

    /**
     * Execute a list of skills sequentially
     * Each skill completes before the next one starts
     */
    suspend fun executeSkillChain(
        skills: List<Pair<SkillConfig, SkillInstance?>>,
        mobLocation: Location,
        mobHealth: Double,
        mobMaxHealth: Double,
        target: IEntity?,
        trigger: IEntity?,
        originLocation: Location,
        skillMetadata: Map<String, SkillInstance>
    ) {
        for ((skill, instance) in skills) {
            executeSkill(
                skill = skill,
                instance = instance,
                mobLocation = mobLocation,
                mobHealth = mobHealth,
                mobMaxHealth = mobMaxHealth,
                target = target,
                trigger = trigger,
                originLocation = originLocation
            )
        }
    }

    /**
     * Execute a single skill with async/sync optimization
     */
    private suspend fun executeSkill(
        skill: SkillConfig,
        instance: SkillInstance?,
        mobLocation: Location,
        mobHealth: Double,
        mobMaxHealth: Double,
        target: IEntity?,
        trigger: IEntity?,
        originLocation: Location
    ) {
        // Resolve targets
        val targets = resolveTargets(instance, originLocation, target, trigger)
        if (targets.isEmpty()) {
            plugin.debugLogger.debug("Skill ${skill.id}: no targets found")
            return
        }

        // Check conditions
        if (!checkConditions(instance, mobLocation, mobHealth, mobMaxHealth, target, trigger)) {
            return
        }

        // Check cooldown
        val cooldownReady = checkCooldown(skill, instance)
        if (!cooldownReady) return

        // Evaluate trigger
        if (!evaluateTrigger(skill, target, mobHealth, mobMaxHealth, mobLocation, true)) {
            return
        }

        // Cast time (wait before execution)
        instance?.castTime?.let { castTime ->
            if (castTime > 0) {
                delay((castTime * 1000).toLong())
            }
        }

        // Execute skill (with repeat support)
        val repeatCount = instance?.repeat ?: 1
        val repeatDelay = instance?.repeatDelay ?: 0.0

        repeat(repeatCount) { iteration ->
            // Determine execution mode
            val executionMode = determineExecutionMode(skill, instance)

            when (executionMode) {
                SkillExecutionMode.ASYNC -> {
                    // Execute asynchronously for better performance
                    withContext(plugin.asyncDispatcher) {
                        executeSkillEffects(skill, targets, mobLocation, mobHealth, mobMaxHealth, true)
                    }
                }

                SkillExecutionMode.SYNC, SkillExecutionMode.AUTO -> {
                    // Execute on main thread
                    withContext(plugin.globalRegionDispatcher) {
                        executeSkillEffects(skill, targets, mobLocation, mobHealth, mobMaxHealth, true)
                    }
                }

                SkillExecutionMode.ENTITY_THREAD -> {
                    // Folia: execute on entity thread
                    // Paper: fallback to sync
                    if (platformScheduler.isFolia() && target != null && trigger != null && trigger.getBukkitEntity() != null) {
                        withContext(plugin.entityDispatcher(trigger.getBukkitEntity()!!)) {
                            executeSkillEffects(skill, targets, mobLocation, mobHealth, mobMaxHealth, true)
                        }
                    } else {
                        withContext(plugin.globalRegionDispatcher) {
                            executeSkillEffects(skill, targets, mobLocation, mobHealth, mobMaxHealth, true)
                        }
                    }
                }

                SkillExecutionMode.REGION_THREAD -> {
                    // Folia: execute on region thread
                    // Paper: fallback to sync
                    if (platformScheduler.isFolia()) {
                        withContext(plugin.regionDispatcher(originLocation)) {
                            executeSkillEffects(skill, targets, mobLocation, mobHealth, mobMaxHealth, true)
                        }
                    } else {
                        withContext(plugin.globalRegionDispatcher) {
                            executeSkillEffects(skill, targets, mobLocation, mobHealth, mobMaxHealth, true)
                        }
                    }
                }
            }

            // Wait for repeat delay
            if (iteration < repeatCount - 1 && repeatDelay > 0) {
                delay((repeatDelay * 1000).toLong())
            }
        }

        // Trigger cooldowns
        triggerCooldowns(skill, instance)
    }

    /**
     * Execute actual skill effects on all targets
     */
    private suspend fun executeSkillEffects(
        skill: SkillConfig,
        targets: List<IEntity>,
        mobLocation: Location,
        mobHealth: Double,
        mobMaxHealth: Double,
        cooldownReady: Boolean
    ) {
        targets.forEach { targetEntity ->
            val targetContext = CelEvaluator.createContext(
                player = targetEntity as? Player,
                mobHealth = mobHealth,
                mobMaxHealth = mobMaxHealth,
                targetDistance = mobLocation.distance(targetEntity.getLoc()),
                cooldownReady = cooldownReady
            )

            // Calculate and apply damage
            val damage = skill.damage?.let { formula ->
                celEvaluator.evaluateDouble(formula, targetContext)
            }

            if (damage != null && damage > 0) {
                targetEntity.damage(damage, null)
            }

            // Apply potion effects
            applyPotionEffects(skill, targetEntity, targetContext)

            // Spawn particles
            spawnParticles(skill, targetEntity.getLoc())

            // Play sound
            playSound(skill, targetEntity.getLoc())

            // Log execution
            val targetName = if (targetEntity is Player) targetEntity.name else targetEntity.getEntityType().name
            plugin.debugLogger.skillExecution(skill.id, skill.id, targetName, damage)
        }
    }

    /**
     * Resolve targets based on selector
     */
    private fun resolveTargets(
        instance: SkillInstance?,
        originLocation: Location,
        target: IEntity?,
        trigger: IEntity?
    ): List<IEntity> {
        return if (instance?.targetSelector != null) {
            targetResolver.resolve(
                selector = instance.targetSelector,
                origin = originLocation,
                self = null,
                target = target,
                trigger = trigger
            )
        } else if (instance?.target != null) {
            targetResolver.resolveSimple(
                targetString = instance.target,
                origin = originLocation,
                self = null,
                target = target,
                trigger = trigger
            )
        } else {
            target?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * Check all conditions for skill execution
     */
    private fun checkConditions(
        instance: SkillInstance?,
        mobLocation: Location,
        mobHealth: Double,
        mobMaxHealth: Double,
        target: IEntity?,
        trigger: IEntity?
    ): Boolean {
        // Check chance
        if (instance?.chance != null && Random.nextDouble() > instance.chance) {
            return false
        }

        // Check health condition
        if (instance?.healthCondition != null) {
            val healthPercent = (mobHealth / mobMaxHealth) * 100.0
            val conditionMet = when {
                instance.healthCondition.startsWith("<") -> {
                    val threshold = instance.healthCondition.substring(1).removeSuffix("%").toDoubleOrNull() ?: 0.0
                    healthPercent < threshold
                }
                instance.healthCondition.startsWith(">") -> {
                    val threshold = instance.healthCondition.substring(1).removeSuffix("%").toDoubleOrNull() ?: 0.0
                    healthPercent > threshold
                }
                instance.healthCondition.startsWith("=") -> {
                    val threshold = instance.healthCondition.substring(1).removeSuffix("%").toDoubleOrNull() ?: 0.0
                    healthPercent == threshold
                }
                else -> true
            }
            if (!conditionMet) return false
        }

        // Check MythicMobs-style conditions
        if (instance?.conditions != null && instance.conditions.isNotEmpty()) {
            val allConditionsMet = instance.conditions.all { condition ->
                conditionEvaluator.evaluate(
                    condition = condition,
                    mobLocation = mobLocation,
                    mobEntity = null,
                    triggerEntity = trigger
                )
            }
            if (!allConditionsMet) return false
        }

        return true
    }

    /**
     * Check cooldown
     */
    private fun checkCooldown(skill: SkillConfig, instance: SkillInstance?): Boolean {
        return cooldownManager.isReady(
            skillId = skill.id,
            individualCd = skill.cooldown,
            groupId = instance?.cooldownGroup,
            groupCd = skill.cooldown,
            useGcd = instance?.useGlobalCooldown ?: false
        )
    }

    /**
     * Evaluate trigger condition
     */
    private fun evaluateTrigger(
        skill: SkillConfig,
        target: IEntity?,
        mobHealth: Double,
        mobMaxHealth: Double,
        mobLocation: Location,
        cooldownReady: Boolean
    ): Boolean {
        val context = CelEvaluator.createContext(
            player = target as? Player,
            mobHealth = mobHealth,
            mobMaxHealth = mobMaxHealth,
            targetDistance = target?.getLoc()?.distance(mobLocation) ?: Double.MAX_VALUE,
            cooldownReady = cooldownReady
        )
        return celEvaluator.evaluateBoolean(skill.trigger, context)
    }

    /**
     * Trigger cooldowns after skill execution
     */
    private fun triggerCooldowns(skill: SkillConfig, instance: SkillInstance?) {
        cooldownManager.trigger(
            skillId = skill.id,
            groupId = instance?.cooldownGroup,
            useGcd = instance?.useGlobalCooldown ?: false,
            gcdDuration = instance?.globalCooldownDuration
        )
    }

    /**
     * Determine execution mode based on skill type
     */
    private fun determineExecutionMode(skill: SkillConfig, instance: SkillInstance?): SkillExecutionMode {
        // Check if instance specifies execution mode
        val context = instance?.options?.get("executionMode")
        if (context != null) {
            return try {
                SkillExecutionMode.valueOf(context.uppercase())
            } catch (e: Exception) {
                SkillExecutionMode.AUTO
            }
        }

        // Auto-detect based on skill properties
        // Skills with particles and sounds only can be async
        // Skills that modify world or entities must be sync
        return if (skill.damage == null && skill.effects.isEmpty()) {
            // Only particles/sounds - can be async
            SkillExecutionMode.ASYNC
        } else {
            // Damage or effects - needs sync for safety
            SkillExecutionMode.SYNC
        }
    }

    /**
     * Apply potion effects to target
     */
    private fun applyPotionEffects(skill: SkillConfig, target: IEntity, context: Map<String, Any>) {
        skill.effects.forEach { effectConfig ->
            try {
                val shouldApply = effectConfig.condition?.let { condition ->
                    celEvaluator.evaluateBoolean(condition, context)
                } ?: true

                if (shouldApply) {
                    val effectType = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.POTION)
                        .getOrThrow(NamespacedKey.minecraft(effectConfig.type))
                        .potionEffects

                    effectType.forEach {
                        val effect = org.bukkit.potion.PotionEffect(
                            it.type,
                            effectConfig.duration,
                            effectConfig.amplifier
                        )
                        target.addPotionEffect(effect)
                    }
                }
            } catch (e: Exception) {
                plugin.debugLogger.warning("Failed to apply effect ${effectConfig.type}: ${e.message}")
            }
        }
    }

    /**
     * Spawn particles at location
     */
    private fun spawnParticles(skill: SkillConfig, location: Location) {
        skill.particles?.let { particleConfig ->
            try {
                val particleType = Particle.valueOf(particleConfig.type)
                location.world?.spawnParticle(
                    particleType,
                    location.x + particleConfig.offsetX,
                    location.y + particleConfig.offsetY,
                    location.z + particleConfig.offsetZ,
                    particleConfig.count,
                    particleConfig.offsetX,
                    particleConfig.offsetY,
                    particleConfig.offsetZ,
                    particleConfig.speed
                )
            } catch (e: Exception) {
                plugin.debugLogger.warning("Failed to spawn particle ${particleConfig.type}: ${e.message}")
            }
        }
    }

    /**
     * Play sound at location
     */
    private fun playSound(skill: SkillConfig, location: Location) {
        skill.sound?.let { soundConfig ->
            try {
                val sound = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.SOUND_EVENT)
                    .getOrThrow(NamespacedKey.minecraft(soundConfig.sound.lowercase()))

                location.world?.playSound(
                    location,
                    sound,
                    soundConfig.volume,
                    soundConfig.pitch
                )
            } catch (e: Exception) {
                plugin.debugLogger.warning("Failed to play sound ${soundConfig.sound}: ${e.message}")
            }
        }
    }
}
