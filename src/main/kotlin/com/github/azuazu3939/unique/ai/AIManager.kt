package com.github.azuazu3939.unique.ai

import com.github.azuazu3939.unique.mob.data.AIConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages reusable AI configurations
 * Allows AI presets to be defined once and shared across multiple mobs
 */
class AIManager {

    private val aiConfigs = ConcurrentHashMap<String, AIConfig>()

    /**
     * Register an AI configuration
     */
    fun registerAI(id: String, config: AIConfig) {
        aiConfigs[id] = config
    }

    /**
     * Get an AI configuration by ID
     */
    fun getAI(id: String): AIConfig? {
        return aiConfigs[id]
    }

    /**
     * Check if an AI configuration exists
     */
    fun hasAI(id: String): Boolean {
        return aiConfigs.containsKey(id)
    }

    /**
     * Get all registered AI IDs
     */
    fun getAllAIIds(): Set<String> {
        return aiConfigs.keys.toSet()
    }

    /**
     * Clear all registered AI configurations
     */
    fun clear() {
        aiConfigs.clear()
    }

    /**
     * Get total number of registered AI configurations
     */
    fun size(): Int {
        return aiConfigs.size
    }
}
