package com.github.azuazu3939.unique.config

import com.github.azuazu3939.unique.mob.data.EffectConfig
import com.github.azuazu3939.unique.mob.data.ParticleConfig
import com.github.azuazu3939.unique.mob.data.SoundConfig
import org.bukkit.configuration.ConfigurationSection

/**
 * Utility object for parsing YAML configurations with case-insensitive support
 */
object YamlParser {

    /**
     * Get configuration section ignoring case
     * Supports: mob/Mob/MOB, skill/Skill/SKILL, etc.
     */
    fun getSection(config: ConfigurationSection, key: String): ConfigurationSection? {
        // Try exact match first
        config.getConfigurationSection(key)?.let { return it }

        // Try case-insensitive match
        val keys = config.getKeys(false)
        val matchingKey = keys.firstOrNull { it.equals(key, ignoreCase = true) }
        return matchingKey?.let { config.getConfigurationSection(it) }
    }

    /**
     * Get string list ignoring case
     * Supports: Skills/skills, SkillRefs/skillRefs, etc.
     * Also supports MythicMobs-style single-line format with semicolons:
     * Skills: skill1; skill2; skill3
     */
    fun getStringList(config: ConfigurationSection, key: String): List<String>? {
        // Try exact match first
        if (config.isList(key)) {
            return config.getStringList(key).takeIf { it.isNotEmpty() }
        }

        // Try case-insensitive match
        val keys = config.getKeys(false)
        val matchingKey = keys.firstOrNull { it.equals(key, ignoreCase = true) }
        return matchingKey?.let {
            if (config.isList(it)) {
                config.getStringList(it).takeIf { list -> list.isNotEmpty() }
            } else if (config.isString(it)) {
                // Support MythicMobs-style single-line format
                // Skills: skill1; skill2; skill3
                val singleLine = config.getString(it) ?: return@let null
                singleLine.split(';').map { skill -> skill.trim() }.filter { skill -> skill.isNotEmpty() }
            } else {
                null
            }
        }
    }

    /**
     * Get list ignoring case
     * Supports: SkillInstances/skillInstances, AIBehaviors/aiBehaviors, etc.
     */
    fun getList(config: ConfigurationSection, key: String): List<*>? {
        // Try exact match first
        if (config.isList(key)) {
            return config.getList(key)?.takeIf { it.isNotEmpty() }
        }

        // Try case-insensitive match
        val keys = config.getKeys(false)
        val matchingKey = keys.firstOrNull { it.equals(key, ignoreCase = true) }
        return matchingKey?.let {
            if (config.isList(it)) {
                config.getList(it)?.takeIf { list -> list.isNotEmpty() }
            } else {
                null
            }
        }
    }

    /**
     * Parse effect configuration from map
     */
    fun parseEffect(map: Map<*, *>): EffectConfig? {
        val type = map["type"] as? String ?: return null
        return EffectConfig(
            type = type,
            duration = (map["duration"] as? Number)?.toInt() ?: 20,
            amplifier = (map["amplifier"] as? Number)?.toInt() ?: 0,
            condition = map["condition"] as? String
        )
    }

    /**
     * Parse effects list from any input
     */
    fun parseEffects(effects: Any?): List<EffectConfig>? {
        if (effects !is List<*>) return null
        return effects.mapNotNull { effectMap ->
            if (effectMap is Map<*, *>) parseEffect(effectMap) else null
        }
    }

    /**
     * Parse particle configuration from map
     */
    fun parseParticle(map: Map<*, *>): ParticleConfig? {
        val type = map["type"] as? String ?: return null
        return ParticleConfig(
            type = type,
            count = (map["count"] as? Number)?.toInt() ?: 1,
            offsetX = (map["offset_x"] as? Number)?.toDouble() ?: 0.0,
            offsetY = (map["offset_y"] as? Number)?.toDouble() ?: 0.0,
            offsetZ = (map["offset_z"] as? Number)?.toDouble() ?: 0.0,
            speed = (map["speed"] as? Number)?.toDouble() ?: 0.0
        )
    }

    /**
     * Parse sound configuration from map
     */
    fun parseSound(map: Map<*, *>): SoundConfig? {
        val sound = map["sound"] as? String ?: return null
        return SoundConfig(
            sound = sound,
            volume = (map["volume"] as? Number)?.toFloat() ?: 1.0f,
            pitch = (map["pitch"] as? Number)?.toFloat() ?: 1.0f
        )
    }

    /**
     * Parse particle from ConfigurationSection
     */
    fun parseParticleSection(section: ConfigurationSection): ParticleConfig {
        return ParticleConfig(
            type = section.getString("type") ?: "FLAME",
            count = section.getInt("count", 1),
            offsetX = section.getDouble("offset_x", 0.0),
            offsetY = section.getDouble("offset_y", 0.0),
            offsetZ = section.getDouble("offset_z", 0.0),
            speed = section.getDouble("speed", 0.0)
        )
    }

    /**
     * Parse sound from ConfigurationSection
     */
    fun parseSoundSection(section: ConfigurationSection): SoundConfig {
        return SoundConfig(
            sound = section.getString("sound") ?: "ENTITY_GENERIC_HURT",
            volume = section.getDouble("volume", 1.0).toFloat(),
            pitch = section.getDouble("pitch", 1.0).toFloat()
        )
    }
}
