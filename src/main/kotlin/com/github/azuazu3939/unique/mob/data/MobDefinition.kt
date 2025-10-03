package com.github.azuazu3939.unique.mob.data

import com.github.azuazu3939.unique.mob.ai.AIType
import org.bukkit.entity.EntityType

/**
 * Represents a complete mob definition loaded from YAML
 */
data class MobDefinition(
    val id: String,
    val displayName: String,
    val entityType: EntityType = EntityType.ZOMBIE,
    val health: Double = 20.0,
    val model: ModelConfig? = null,
    val ai: AIConfig? = null,                             // Inline AI configuration (legacy)
    val aiRef: String? = null,                            // AI preset reference (legacy)
    val aiBehaviors: List<AIBehavior> = emptyList(),      // Conditional AI behaviors (recommended)
    val aiSwitchMode: AISwitchMode = AISwitchMode.PERIODIC,
    val aiSwitchInterval: Double = 3.0,                   // Seconds (for PERIODIC mode)
    val skills: List<SkillConfig> = emptyList(),          // Inline skill definitions (YAML: skills)
    val skillRefs: List<String> = emptyList(),            // Simple skill ID references (YAML: Skills)
    val skillInstances: List<SkillInstance> = emptyList(), // Skill instances with overrides (YAML: SkillInstances)
    val drops: List<DropConfig> = emptyList(),
    val attributes: Map<String, Any> = emptyMap()
)

/**
 * Model configuration for EntityLib
 */
data class ModelConfig(
    val id: String,
    val scale: Double = 1.0,
    val viewDistance: Double = 64.0
)

/**
 * AI behavior configuration with CEL expressions
 */
data class AIConfig(
    // Basic AI settings
    val type: AIType = AIType.SIMPLE,
    val targetCondition: String? = null, // CEL expression
    val damageFormula: String? = null,   // CEL expression
    val moveSpeed: Double = 0.3,
    val followRange: Double = 16.0,

    // Aggressive AI settings
    val pursuitTime: Double = 30.0,      // How long to pursue target (seconds)
    val memoryDuration: Double = 10.0,   // How long to remember target after losing sight (seconds)
    val aggroRange: Double = 32.0,       // Initial aggro detection range

    // Pathfinding settings
    val usePathfinding: Boolean = false,
    val pathfindingUpdateInterval: Int = 20, // Update path every N ticks

    // Charge AI settings
    val chargeDuration: Double = 2.0,    // Charge-up time (seconds)
    val dashSpeed: Double = 2.0,         // Speed during dash
    val dashDistance: Double = 20.0,     // Distance to dash
    val turnCooldown: Double = 3.0,      // Cooldown after dash (seconds)
    val chargeParticle: String? = null,  // Particle during charge

    // Ranged AI settings
    val preferredDistance: Double = 10.0, // Preferred distance from target
    val minDistance: Double = 5.0,       // Minimum distance to maintain
    val maxDistance: Double = 20.0,      // Maximum distance before closing in

    // Territorial AI settings
    val territoryRadius: Double = 32.0,  // Radius from spawn point
    val returnSpeed: Double = 0.5,       // Speed when returning to territory

    // Stationary AI settings
    val rotateToTarget: Boolean = true   // Whether to rotate to face target
)

/**
 * Skill configuration
 */
data class SkillConfig(
    val id: String,
    val aliases: List<String> = emptyList(), // Alternative names for this skill
    val trigger: String,     // Trigger notation (~onAttack, ~onTimer:20) OR CEL expression for backward compatibility
    val damage: String? = null, // CEL expression for damage calculation
    val cooldown: Double = 0.0,
    val effects: List<EffectConfig> = emptyList(),
    val particles: ParticleConfig? = null,
    val sound: SoundConfig? = null
)

/**
 * Effect configuration for skills
 */
data class EffectConfig(
    val type: String,
    val duration: Int = 20,
    val amplifier: Int = 0,
    val condition: String? = null // CEL expression
)

/**
 * Particle configuration
 */
data class ParticleConfig(
    val type: String,
    val count: Int = 1,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val offsetZ: Double = 0.0,
    val speed: Double = 0.0
)

/**
 * Sound configuration
 */
data class SoundConfig(
    val sound: String,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
)

/**
 * Drop configuration with CEL-based conditions
 */
data class DropConfig(
    val item: String,
    val amount: Int = 1,
    val chance: Double = 1.0,
    val condition: String? = null // CEL expression
)
