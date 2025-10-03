package com.github.azuazu3939.unique.mob.ai

/**
 * Types of AI behavior for custom mobs
 */
enum class AIType {
    /**
     * Simple AI - moves directly towards target
     */
    SIMPLE,

    /**
     * Aggressive AI - pursues target persistently with increased range and duration
     */
    AGGRESSIVE,

    /**
     * Pathfinding AI - uses Minecraft's pathfinding to navigate around obstacles
     */
    PATHFINDING,

    /**
     * Charge AI - charges up, then dashes at high speed, turns and repeats
     */
    CHARGE,

    /**
     * Ranged AI - maintains distance from target while attacking
     */
    RANGED,

    /**
     * Territorial AI - stays within a certain radius of spawn point
     */
    TERRITORIAL,

    /**
     * Stationary AI - doesn't move, only rotates to face target
     */
    STATIONARY;

    companion object {
        fun fromString(str: String): AIType {
            return entries.find { it.name.equals(str, ignoreCase = true) } ?: SIMPLE
        }
    }
}
