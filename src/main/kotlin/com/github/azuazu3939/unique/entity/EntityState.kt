package com.github.azuazu3939.unique.entity

/**
 * Represents the current state of an entity
 * Used for state machine logic and visual effects
 */
enum class EntityState {
    /**
     * Entity has not been spawned yet
     */
    UNSPAWNED,

    /**
     * Entity is idle, not performing any action
     */
    IDLE,

    /**
     * Entity has been spawned and is visible to viewers
     */
    SPAWNED,

    /**
     * Entity has been despawned and is no longer visible
     */
    DESPAWNED,

    /**
     * Entity is currently moving
     */
    MOVING,

    /**
     * Entity is being teleported
     */
    TELEPORTING,

    /**
     * Entity is attacking
     */
    ATTACKING,

    /**
     * Entity is taking damage
     */
    DAMAGED,

    /**
     * Entity is dead
     */
    DEAD,

    /**
     * Entity is on fire
     */
    BURNING,

    /**
     * Entity is frozen (from powder snow or similar)
     */
    FROZEN,

    /**
     * Entity is invisible
     */
    INVISIBLE,

    /**
     * Entity is glowing
     */
    GLOWING,

    /**
     * Entity is sneaking
     */
    SNEAKING,

    /**
     * Entity is sprinting
     */
    SPRINTING,

    /**
     * Entity is swimming
     */
    SWIMMING,

    /**
     * Entity is gliding (elytra)
     */
    GLIDING,

    /**
     * Entity is blocking with shield
     */
    BLOCKING,

    /**
     * Entity is in vehicle
     */
    IN_VEHICLE
}
