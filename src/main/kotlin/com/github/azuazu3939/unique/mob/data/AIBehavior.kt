package com.github.azuazu3939.unique.mob.data

/**
 * Represents a conditional AI behavior
 * Allows mobs to switch between different AI configurations based on conditions
 */
data class AIBehavior(
    val condition: String? = null,       // CEL expression (null = always active)
    val priority: Int = 0,                // Higher priority = selected first when condition is true
    val aiRef: String? = null,            // Reference to AI preset
    val ai: AIConfig? = null              // Inline AI config
)

/**
 * AI switch mode - when to re-evaluate AI selection
 */
enum class AISwitchMode {
    PERIODIC,        // Re-evaluate every X seconds
    ON_SKILL_END,    // Re-evaluate after skill execution
    NEVER            // Never switch (use first matching)
}
