package com.github.azuazu3939.unique.mob.data

import com.github.azuazu3939.unique.config.YamlParser

/**
 * Represents a skill instance with potential overrides
 * Allows mobs to customize skill parameters without duplicating definitions
 */
data class SkillInstance(
    // Required: reference to skill definition
    val skillId: String,

    // Override parameters (null = use base skill value)
    val damageOverride: String? = null,           // CEL expression
    val triggerOverride: String? = null,          // Trigger notation (~onAttack, ~onTimer:20) OR CEL expression
    val cooldownOverride: Double? = null,         // Seconds
    val effectsOverride: List<EffectConfig>? = null,
    val particleOverride: ParticleConfig? = null,
    val soundOverride: SoundConfig? = null,

    // Particle/Sound offsets (additive to base values)
    val particleOffsetX: Double? = null,
    val particleOffsetY: Double? = null,
    val particleOffsetZ: Double? = null,
    val particleCountMultiplier: Double? = null,  // Multiply base count

    // Cooldown system
    val cooldownGroup: String? = null,            // Group for shared cooldown
    val useGlobalCooldown: Boolean = false,       // Use global cooldown
    val globalCooldownDuration: Double? = null,   // Override GCD duration

    // Targeting overrides
    val targetCondition: String? = null,          // CEL: additional target filter
    val maxTargets: Int? = null,                  // Maximum number of targets
    val targetRadius: Double? = null,             // AoE radius for multi-target

    // Execution modifiers
    val castTime: Double? = null,                 // Cast time before execution (seconds)
    val repeat: Int? = null,                      // Number of times to repeat
    val repeatDelay: Double? = null,              // Delay between repeats (seconds)

    // Compact notation fields
    val target: String? = null,                   // Target type: @target, @self, @nearby, etc.
    val targetSelector: TargetSelector? = null,   // Parsed target selector with conditions
    val chance: Double? = null,                   // Activation chance (0.0-1.0)
    val healthCondition: String? = null,          // Health condition: <50%, >75%, etc.
    val options: Map<String, String>? = null,     // Skill-specific options from {key=value;key2=value2}
    val conditions: List<Condition>? = null       // Conditions to check before execution
) {
    /**
     * Merge this instance with a base skill config
     */
    fun mergeWithBase(base: SkillConfig): SkillConfig {
        // Create particle config with overrides
        val mergedParticle = particleOverride
            ?: base.particles?.copy(
                offsetX = base.particles.offsetX + (particleOffsetX ?: 0.0),
                offsetY = base.particles.offsetY + (particleOffsetY ?: 0.0),
                offsetZ = base.particles.offsetZ + (particleOffsetZ ?: 0.0),
                count = (base.particles.count * (particleCountMultiplier ?: 1.0)).toInt()
            )

        return base.copy(
            trigger = triggerOverride ?: base.trigger,
            damage = damageOverride ?: base.damage,
            cooldown = cooldownOverride ?: base.cooldown,
            effects = effectsOverride ?: base.effects,
            particles = mergedParticle,
            sound = soundOverride ?: base.sound
        )
    }
}

/**
 * Helper to create skill instance from string or map
 */
object SkillInstanceParser {
    /**
     * Parse from string (simple reference, compact notation) or map (with overrides)
     *
     * Examples:
     * - Simple: "skill_id"
     * - Compact: "ignite{ticks=100;damage=50} @target ~onAttack <50% 0.5"
     * - Map: {id: "skill_id", damage: "50", ...}
     */
    fun parse(input: Any): SkillInstance? {
        return when (input) {
            is String -> {
                // Check if compact notation (contains space, @, ~, {, <, >, or %)
                if (input.contains(' ') || input.contains('@') || input.contains('~') ||
                    input.contains('{') || input.contains('<') || input.contains('>') || input.contains('%')) {
                    parseCompactNotation(input)
                } else {
                    // Simple reference: "skill_id"
                    SkillInstance(skillId = input)
                }
            }
            is Map<*, *> -> {
                // Complex reference with overrides
                val skillId = input["id"] as? String ?: return null

                SkillInstance(
                    skillId = skillId,
                    damageOverride = input["damage"] as? String,
                    triggerOverride = input["trigger"] as? String,
                    cooldownOverride = (input["cooldown"] as? Number)?.toDouble(),
                    effectsOverride = YamlParser.parseEffects(input["effects"]),
                    particleOverride = (input["particle"] as? Map<*, *>)?.let { YamlParser.parseParticle(it) },
                    soundOverride = (input["sound"] as? Map<*, *>)?.let { YamlParser.parseSound(it) },
                    particleOffsetX = (input["particle_offset_x"] as? Number)?.toDouble(),
                    particleOffsetY = (input["particle_offset_y"] as? Number)?.toDouble(),
                    particleOffsetZ = (input["particle_offset_z"] as? Number)?.toDouble(),
                    particleCountMultiplier = (input["particle_count_multiplier"] as? Number)?.toDouble(),
                    cooldownGroup = input["cd_group"] as? String,
                    useGlobalCooldown = input["use_global_cd"] as? Boolean ?: false,
                    globalCooldownDuration = (input["gcd_duration"] as? Number)?.toDouble(),
                    targetCondition = input["target_condition"] as? String,
                    maxTargets = (input["max_targets"] as? Number)?.toInt(),
                    targetRadius = (input["target_radius"] as? Number)?.toDouble(),
                    castTime = (input["cast_time"] as? Number)?.toDouble(),
                    repeat = (input["repeat"] as? Number)?.toInt(),
                    repeatDelay = (input["repeat_delay"] as? Number)?.toDouble()
                )
            }
            else -> null
        }
    }

    /**
     * Parse compact MythicMobs-style notation
     * Format: skillId{option1=value1;option2=value2} @target ~trigger ?condition ?~condition <healthCondition chance
     *
     * Example: "ignite{ticks=100} @target ~onAttack ?raining ?~holding{m=WOODEN_SWORD} <50% 0.5"
     */
    private fun parseCompactNotation(notation: String): SkillInstance? {
        // Extract skill ID and options
        val skillIdMatch = Regex("^([a-zA-Z0-9_]+)").find(notation) ?: return null
        val skillId = skillIdMatch.value

        // Extract options from {key=value;key2=value2}
        val optionsMatch = Regex("\\{([^}]+)}").find(notation)
        val options = optionsMatch?.let { match ->
            val optionsString = match.groupValues[1]
            optionsString.split(';').mapNotNull { pair ->
                val parts = pair.split('=', limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null
                }
            }.toMap()
        }

        // Extract target (@target, @self, @PIR{r=10;conditions=[...]}, etc.)
        val targetSelector = parseTargetSelector(notation)
        val target = targetSelector?.type ?: run {
            val targetMatch = Regex("@([a-zA-Z0-9_]+)").find(notation)
            targetMatch?.value
        }

        // Extract trigger (~onAttack, ~onDamaged, ~onTimer:20, etc.)
        val triggerMatch = Regex("~([a-zA-Z0-9_:]+)").find(notation)
        val trigger = triggerMatch?.let {
            // Keep the full trigger notation (with ~)
            notation.substring(it.range)
        }

        // Extract conditions (?condition, ?~condition, ?!condition, ?~!condition)
        val conditions = parseConditions(notation)

        // Extract health condition (<50%, >75%, =100%, etc.)
        val healthConditionMatch = Regex("([<>=])([0-9.]+)%").find(notation)
        val healthCondition = healthConditionMatch?.value

        // Extract chance (last number that's not part of health condition or options)
        val chanceMatch = Regex("\\s([0-9.]+)\\s*$").find(notation)
        val chance = chanceMatch?.groupValues?.get(1)?.toDoubleOrNull()

        return SkillInstance(
            skillId = skillId,
            options = options,
            target = target,
            targetSelector = targetSelector,
            triggerOverride = trigger,
            healthCondition = healthCondition,
            chance = chance,
            conditions = conditions
        )
    }

    /**
     * Parse target selector with conditions
     * Format: @PIR{r=10;conditions=[...]}
     */
    private fun parseTargetSelector(notation: String): TargetSelector? {
        // Match @TYPE{options} pattern
        val selectorMatch = Regex("@([A-Z]+)\\{([^}]+)}").find(notation) ?: return null
        val selectorType = selectorMatch.groupValues[1]
        val optionsString = selectorMatch.groupValues[2]

        // Parse options
        val options = mutableMapOf<String, String>()
        val conditions = mutableListOf<TargetCondition>()

        // Split by semicolons, but handle nested brackets
        var depth = 0
        var currentOption = StringBuilder()

        for (char in optionsString) {
            when (char) {
                '[' -> {
                    depth++
                    currentOption.append(char)
                }
                ']' -> {
                    depth--
                    currentOption.append(char)
                }
                ';' -> {
                    if (depth == 0) {
                        processOption(currentOption.toString().trim(), options, conditions)
                        currentOption = StringBuilder()
                    } else {
                        currentOption.append(char)
                    }
                }
                else -> currentOption.append(char)
            }
        }

        // Process last option
        if (currentOption.isNotEmpty()) {
            processOption(currentOption.toString().trim(), options, conditions)
        }

        // Extract range
        val range: Double? = options["r"]?.toDoubleOrNull()

        return TargetSelector(
            type = selectorType,
            range = range,
            conditions = conditions,
            options = options
        )
    }

    private fun processOption(
        option: String,
        options: MutableMap<String, String>,
        conditions: MutableList<TargetCondition>
    ) {
        if (option.startsWith("conditions=")) {
            // Parse conditions list
            val conditionsStr = option.substringAfter("conditions=")
                .removeSurrounding("[", "]")

            // Split by lines (- prefix)
            val conditionLines = conditionsStr.split("-").filter { it.isNotBlank() }

            for (line in conditionLines) {
                val trimmed = line.trim()

                // Parse: conditionType{options} result
                val condMatch = Regex("([a-zA-Z0-9_]+)\\{([^}]+)}\\s+(true|false)").find(trimmed)
                if (condMatch != null) {
                    val condType = condMatch.groupValues[1]
                    val condOptionsStr = condMatch.groupValues[2]
                    val expectedResult = condMatch.groupValues[3] == "true"

                    val condOptions = condOptionsStr.split(';').mapNotNull { pair ->
                        val parts = pair.split('=', limit = 2)
                        if (parts.size == 2) {
                            parts[0].trim() to parts[1].trim()
                        } else {
                            null
                        }
                    }.toMap()

                    conditions.add(TargetCondition(condType, condOptions, expectedResult))
                }
            }
        } else {
            // Regular option
            val parts = option.split('=', limit = 2)
            if (parts.size == 2) {
                options[parts[0].trim()] = parts[1].trim()
            }
        }
    }

    /**
     * Parse conditions from notation
     * Format: ?condition{options} or ?~condition{options}
     * With negation: ?!condition or ?~!condition
     */
    private fun parseConditions(notation: String): List<Condition> {
        val conditions = mutableListOf<Condition>()

        // Match all ?condition or ?~condition patterns
        val conditionRegex = Regex("\\?(~?)(!?)([a-zA-Z0-9_]+)(\\{[^}]+})?")
        val matches = conditionRegex.findAll(notation)

        for (match in matches) {
            val isTrigger = match.groupValues[1] == "~"
            val isNegate = match.groupValues[2] == "!"
            val condType = match.groupValues[3]
            val optionsStr = match.groupValues[4]

            val options = if (optionsStr.isNotEmpty()) {
                optionsStr.removeSurrounding("{", "}").split(';').mapNotNull { pair ->
                    val parts = pair.split('=', limit = 2)
                    if (parts.size == 2) {
                        parts[0].trim() to parts[1].trim()
                    } else {
                        null
                    }
                }.toMap()
            } else {
                emptyMap()
            }

            conditions.add(Condition(
                type = condType,
                target = if (isTrigger) ConditionTarget.TRIGGER else ConditionTarget.SELF,
                negate = isNegate,
                options = options
            ))
        }

        return conditions
    }
}
