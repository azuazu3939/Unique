package com.github.azuazu3939.unique.skill

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages skill cooldowns with support for:
 * - Individual cooldowns (per skill)
 * - Group cooldowns (shared across skill groups)
 * - Global cooldown (affects all skills)
 */
class CooldownManager {

    // Individual skill cooldowns: skillId -> lastUseTime
    private val individualCooldowns = ConcurrentHashMap<String, Long>()

    // Group cooldowns: groupId -> lastUseTime
    private val groupCooldowns = ConcurrentHashMap<String, Long>()

    // Global cooldown: single timestamp
    private var globalCooldownEnd: Long = 0L

    // Default global cooldown duration (milliseconds)
    private var defaultGcdDuration: Long = 1000L // 1 second

    /**
     * Set default global cooldown duration
     */
    fun setDefaultGcdDuration(seconds: Double) {
        defaultGcdDuration = (seconds * 1000).toLong()
    }

    /**
     * Check if a skill is ready (all cooldowns satisfied)
     */
    fun isReady(
        skillId: String,
        individualCd: Double,
        groupId: String? = null,
        groupCd: Double? = null,
        useGcd: Boolean = false
    ): Boolean {
        val currentTime = System.currentTimeMillis()

        // Check global cooldown
        if (useGcd && currentTime < globalCooldownEnd) {
            return false
        }

        // Check group cooldown
        if (groupId != null && groupCd != null) {
            val lastGroupUse = groupCooldowns[groupId] ?: 0L
            val groupCdMillis = (groupCd * 1000).toLong()
            if (currentTime - lastGroupUse < groupCdMillis) {
                return false
            }
        }

        // Check individual cooldown
        val lastUse = individualCooldowns[skillId] ?: 0L
        val individualCdMillis = (individualCd * 1000).toLong()
        return currentTime - lastUse >= individualCdMillis
    }

    /**
     * Get remaining cooldown time in seconds
     */
    fun getRemainingCooldown(
        skillId: String,
        individualCd: Double,
        groupId: String? = null,
        groupCd: Double? = null,
        useGcd: Boolean = false
    ): Double {
        val currentTime = System.currentTimeMillis()
        var maxRemaining = 0.0

        // Check global cooldown
        if (useGcd) {
            val gcdRemaining = (globalCooldownEnd - currentTime) / 1000.0
            if (gcdRemaining > maxRemaining) {
                maxRemaining = gcdRemaining
            }
        }

        // Check group cooldown
        if (groupId != null && groupCd != null) {
            val lastGroupUse = groupCooldowns[groupId] ?: 0L
            val groupCdMillis = (groupCd * 1000).toLong()
            val groupRemaining = (groupCdMillis - (currentTime - lastGroupUse)) / 1000.0
            if (groupRemaining > maxRemaining) {
                maxRemaining = groupRemaining
            }
        }

        // Check individual cooldown
        val lastUse = individualCooldowns[skillId] ?: 0L
        val individualCdMillis = (individualCd * 1000).toLong()
        val individualRemaining = (individualCdMillis - (currentTime - lastUse)) / 1000.0
        if (individualRemaining > maxRemaining) {
            maxRemaining = individualRemaining
        }

        return maxRemaining.coerceAtLeast(0.0)
    }

    /**
     * Trigger cooldowns after skill use
     */
    fun trigger(
        skillId: String,
        groupId: String? = null,
        useGcd: Boolean = false,
        gcdDuration: Double? = null
    ) {
        val currentTime = System.currentTimeMillis()

        // Set individual cooldown
        individualCooldowns[skillId] = currentTime

        // Set group cooldown
        if (groupId != null) {
            groupCooldowns[groupId] = currentTime
        }

        // Set global cooldown
        if (useGcd) {
            val gcdMillis = if (gcdDuration != null) {
                (gcdDuration * 1000).toLong()
            } else {
                defaultGcdDuration
            }
            globalCooldownEnd = currentTime + gcdMillis
        }
    }

    /**
     * Reset a specific skill cooldown
     */
    fun reset(skillId: String) {
        individualCooldowns.remove(skillId)
    }

    /**
     * Reset a group cooldown
     */
    fun resetGroup(groupId: String) {
        groupCooldowns.remove(groupId)
    }

    /**
     * Reset global cooldown
     */
    fun resetGlobalCooldown() {
        globalCooldownEnd = 0L
    }

    /**
     * Reset all cooldowns
     */
    fun resetAll() {
        individualCooldowns.clear()
        groupCooldowns.clear()
        globalCooldownEnd = 0L
    }

    /**
     * Get all active cooldowns (for debugging)
     */
    fun getActiveCooldowns(): Map<String, Double> {
        val currentTime = System.currentTimeMillis()
        val result = mutableMapOf<String, Double>()

        // Individual cooldowns
        individualCooldowns.forEach { (skillId, lastUse) ->
            val remaining = (currentTime - lastUse) / 1000.0
            if (remaining > 0) {
                result[skillId] = remaining
            }
        }

        // Group cooldowns
        groupCooldowns.forEach { (groupId, lastUse) ->
            val remaining = (currentTime - lastUse) / 1000.0
            if (remaining > 0) {
                result["group:$groupId"] = remaining
            }
        }

        // Global cooldown
        val gcdRemaining = (globalCooldownEnd - currentTime) / 1000.0
        if (gcdRemaining > 0) {
            result["global"] = gcdRemaining
        }

        return result
    }
}
