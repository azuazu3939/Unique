package com.github.azuazu3939.unique.event

/**
 * Represents when a skill should be triggered
 */
enum class SkillTrigger {
    /**
     * Trigger when mob attacks
     */
    ON_ATTACK,

    /**
     * Trigger when mob takes damage
     */
    ON_DAMAGED,

    /**
     * Trigger on a timer interval
     */
    ON_TIMER,

    /**
     * Trigger when mob spawns
     */
    ON_SPAWN,

    /**
     * Trigger when mob dies
     */
    ON_DEATH,

    /**
     * Trigger when mob targets an entity
     */
    ON_TARGET,

    /**
     * Trigger when player interacts with entity (right-click)
     */
    ON_INTERACT,

    /**
     * Always ready to trigger (controlled by cooldown only)
     */
    ALWAYS;

    companion object {
        /**
         * Parse trigger from string
         * Examples: "onAttack", "attack", "onTimer:20", "timer:40"
         */
        fun parse(trigger: String): Pair<SkillTrigger, Int?> {
            val parts = trigger.split(':')
            val triggerName = parts[0].trim()
            val interval = parts.getOrNull(1)?.toIntOrNull()

            val triggerType = when (triggerName.lowercase()) {
                "onattack", "attack" -> ON_ATTACK
                "ondamaged", "damaged", "onhit", "hit" -> ON_DAMAGED
                "ontimer", "timer", "interval" -> ON_TIMER
                "onspawn", "spawn" -> ON_SPAWN
                "ondeath", "death" -> ON_DEATH
                "ontarget", "target" -> ON_TARGET
                "oninteract", "interact", "onrightclick", "rightclick" -> ON_INTERACT
                "always" -> ALWAYS
                else -> ALWAYS
            }

            return Pair(triggerType, interval)
        }
    }
}

/**
 * Trigger event data
 */
data class TriggerEvent(
    val trigger: SkillTrigger,
    val triggerEntity: org.bukkit.entity.LivingEntity? = null,  // Entity that triggered (e.g., attacker)
    val target: org.bukkit.entity.LivingEntity? = null,          // Current target
    val damage: Double = 0.0                                      // Damage amount (for ON_DAMAGED)
)
