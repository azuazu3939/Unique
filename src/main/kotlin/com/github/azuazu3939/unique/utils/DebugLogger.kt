package com.github.azuazu3939.unique.utils

import java.util.logging.Logger

/**
 * Debug logger utility for level-based logging
 *
 * Debug Levels:
 * - 0: Critical only (errors, plugin lifecycle)
 * - 1: Important (mob spawn/death, commands)
 * - 2: Detailed (skills, AI, targeting)
 * - 3: Debug (CEL evaluation, exact coordinates, all operations)
 */
class DebugLogger(
    private val logger: Logger,
    private var debugLevel: Int = 1
) {

    /**
     * Set the debug level
     */
    fun setDebugLevel(level: Int) {
        debugLevel = level.coerceIn(0, 3)
    }

    /**
     * Get the current debug level
     */
    fun getDebugLevel(): Int = debugLevel

    /**
     * Log a critical message (level 0+)
     * For errors, plugin lifecycle, and other critical events
     */
    fun critical(message: String) {
        if (debugLevel >= 0) {
            logger.info(message)
        }
    }

    /**
     * Log a severe error (level 0+)
     */
    fun error(message: String) {
        if (debugLevel >= 0) {
            logger.severe(message)
        }
    }

    /**
     * Log a warning (level 0+)
     */
    fun warning(message: String) {
        if (debugLevel >= 0) {
            logger.warning(message)
        }
    }

    /**
     * Log an important message (level 1+)
     * For mob spawn/death, command execution, config reload
     */
    fun important(message: String) {
        if (debugLevel >= 1) {
            logger.info(message)
        }
    }

    /**
     * Log a detailed message (level 2+)
     * For skill execution, AI targeting, movement
     */
    fun detailed(message: String) {
        if (debugLevel >= 2) {
            logger.info(message)
        }
    }

    /**
     * Log a debug message (level 3+)
     * For CEL evaluation, exact coordinates, all parameters
     */
    fun debug(message: String) {
        if (debugLevel >= 3) {
            logger.info("[DEBUG] $message")
        }
    }

    /**
     * Log a reload message with appropriate detail level
     * - Level 0: Basic success/failure
     * - Level 1: Count of loaded items
     * - Level 2+: List of loaded items
     */
    fun reload(success: Boolean, summary: String, details: List<String> = emptyList()) {
        if (success) {
            when (debugLevel) {
                0 -> critical("Configuration reloaded")
                1 -> important("Configuration reloaded: $summary")
                else -> {
                    important("Configuration reloaded: $summary")
                    details.forEach { detail -> detailed("  - $detail") }
                }
            }
        } else {
            error("Configuration reload failed: $summary")
        }
    }

    /**
     * Log a mob spawn with appropriate detail level
     */
    fun mobSpawn(mobId: String, displayName: String, location: String, viewerCount: Int) {
        when (debugLevel) {
            0 -> { /* No logging */ }
            1 -> important("Spawned $displayName")
            2 -> detailed("Spawned $displayName ($mobId) for $viewerCount viewer(s)")
            else -> debug("Spawned $displayName ($mobId) at $location for $viewerCount viewer(s)")
        }
    }

    /**
     * Log a mob death with appropriate detail level
     */
    fun mobDeath(mobId: String, displayName: String, location: String, dropCount: Int) {
        when (debugLevel) {
            0 -> { /* No logging */ }
            1 -> important("$displayName died")
            2 -> detailed("$displayName ($mobId) died with $dropCount drop(s)")
            else -> debug("$displayName ($mobId) died at $location with $dropCount drop(s)")
        }
    }

    /**
     * Log a skill execution with appropriate detail level
     */
    fun skillExecution(mobId: String, skillId: String, targetName: String, damage: Double?) {
        when (debugLevel) {
            0, 1 -> { /* No logging */ }
            2 -> detailed("$mobId used $skillId on $targetName")
            else -> debug("$mobId used $skillId on $targetName (damage: $damage)")
        }
    }

    /**
     * Log CEL evaluation with appropriate detail level
     */
    fun celEvaluation(expression: String, result: Any?, context: Map<String, Any>) {
        if (debugLevel >= 3) {
            debug("CEL: '$expression' = $result")
            debug("  Context: $context")
        }
    }

    /**
     * Log a command execution
     */
    fun command(command: String, sender: String, result: String) {
        when (debugLevel) {
            0 -> { /* No logging */ }
            1 -> important("$sender executed: /$command")
            else -> detailed("$sender executed: /$command - $result")
        }
    }
}
