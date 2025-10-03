package com.github.azuazu3939.unique.mob.ai

/**
 * States for AI behavior state machine
 */
enum class AIState {
    /**
     * Idle - no target, waiting
     */
    IDLE,

    /**
     * Targeting - evaluating potential targets
     */
    TARGETING,

    /**
     * Chasing - actively pursuing target
     */
    CHASING,

    /**
     * Attacking - in attack range, executing skills
     */
    ATTACKING,

    /**
     * Charging - preparing for dash attack
     */
    CHARGING,

    /**
     * Dashing - executing high-speed dash
     */
    DASHING,

    /**
     * Returning - returning to spawn/territorial point
     */
    RETURNING,

    /**
     * Cooldown - waiting after dash before next action
     */
    COOLDOWN
}
