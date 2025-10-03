package com.github.azuazu3939.unique.skill

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.mob.data.SkillConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages reusable skill definitions
 * Skills can be defined once and referenced by multiple mobs
 */
class SkillManager(private val plugin: Unique) {

    private val skills = ConcurrentHashMap<String, SkillConfig>()

    /**
     * Register a skill definition
     */
    fun registerSkill(skill: SkillConfig) {
        skills[skill.id] = skill
        plugin.debugLogger.detailed("Registered skill: ${skill.id}")
    }

    /**
     * Unregister a skill
     */
    fun unregisterSkill(skillId: String) {
        skills.remove(skillId)
    }

    /**
     * Get a skill by ID or alias
     */
    fun getSkill(skillId: String): SkillConfig? {
        // First try direct ID match
        skills[skillId]?.let { return it }

        // Then search aliases
        return skills.values.firstOrNull { skill ->
            skill.aliases.contains(skillId)
        }
    }

    /**
     * Check if a skill exists by ID or alias
     */
    fun hasSkill(skillId: String): Boolean {
        // Check direct ID
        if (skills.containsKey(skillId)) return true

        // Check aliases
        return skills.values.any { skill ->
            skill.aliases.contains(skillId)
        }
    }

    /**
     * Get all registered skills
     */
    fun getAllSkills(): Collection<SkillConfig> {
        return skills.values
    }

    /**
     * Get all skill IDs
     */
    fun getAllSkillIds(): Set<String> {
        return skills.keys.toSet()
    }

    /**
     * Clear all skills
     */
    fun clear() {
        skills.clear()
    }

    /**
     * Get the number of registered skills
     */
    fun getSkillCount(): Int {
        return skills.size
    }
}
