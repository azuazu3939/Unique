package com.github.azuazu3939.unique.skill

/**
 * Defines how a skill should be executed
 */
enum class SkillExecutionMode {
    /**
     * Execute asynchronously (default for most skills)
     * Use for: damage calculations, particle spawning, sound playing
     */
    ASYNC,

    /**
     * Execute synchronously on the main thread
     * Use for: world modifications, entity spawning, teleportation
     */
    SYNC,

    /**
     * Execute on the entity's thread (Folia-specific)
     * On Paper, equivalent to SYNC
     */
    ENTITY_THREAD,

    /**
     * Execute on the location's region thread (Folia-specific)
     * On Paper, equivalent to SYNC
     */
    REGION_THREAD,

    /**
     * Automatically determine execution mode based on skill type
     */
    AUTO
}

/**
 * Skill execution context
 * Contains information about how and where to execute a skill
 */
data class SkillExecutionContext(
    val mode: SkillExecutionMode = SkillExecutionMode.AUTO,
    val requiresSync: Boolean = false,  // Override for skills that must be sync
    val location: org.bukkit.Location? = null,  // For region-specific execution
    val entity: org.bukkit.entity.Entity? = null  // For entity-specific execution
)
