package com.github.azuazu3939.unique.config

import com.github.azuazu3939.unique.mob.ai.AIType
import com.github.azuazu3939.unique.mob.data.AIConfig

/**
 * MythicMobs-style compact YAML parser
 * Converts short notation to full configuration
 *
 * Examples:
 * - ms: 0.5 → move_speed: 0.5
 * - fr: 16 → follow_range: 16
 * - tc: "dist < 10" → target_condition: "dist < 10"
 */
object CompactParser {

    // AI field mappings (camelCase, no underscores)
    private val aiFieldMap = mapOf(
        // Basic
        "type" to "type",
        "t" to "type",

        // Movement
        "movespeed" to "moveSpeed",
        "moveSpeed" to "moveSpeed",
        "ms" to "moveSpeed",
        "speed" to "moveSpeed",

        "followrange" to "followRange",
        "followRange" to "followRange",
        "fr" to "followRange",
        "range" to "followRange",

        // Conditions
        "targetcondition" to "targetCondition",
        "targetCondition" to "targetCondition",
        "tc" to "targetCondition",
        "condition" to "targetCondition",

        "damageformula" to "damageFormula",
        "damageFormula" to "damageFormula",
        "df" to "damageFormula",
        "dmg" to "damageFormula",

        // Aggressive AI
        "pursuittime" to "pursuitTime",
        "pursuitTime" to "pursuitTime",
        "pt" to "pursuitTime",

        "memoryduration" to "memoryDuration",
        "memoryDuration" to "memoryDuration",
        "md" to "memoryDuration",
        "memory" to "memoryDuration",

        "aggrorange" to "aggroRange",
        "aggroRange" to "aggroRange",
        "ar" to "aggroRange",
        "aggro" to "aggroRange",

        // Pathfinding
        "usepathfinding" to "usePathfinding",
        "usePathfinding" to "usePathfinding",
        "pf" to "usePathfinding",
        "pathfinding" to "pathfinding",

        "pathfindingupdateinterval" to "pathfindingUpdateInterval",
        "pathfindingUpdateInterval" to "pathfindingUpdateInterval",
        "pfi" to "pathfindingUpdateInterval",

        // Charge
        "chargeduration" to "chargeDuration",
        "chargeDuration" to "chargeDuration",
        "cd" to "chargeDuration",
        "charge" to "chargeDuration",

        "dashspeed" to "dashSpeed",
        "dashSpeed" to "dashSpeed",
        "ds" to "dashSpeed",

        "dashdistance" to "dashDistance",
        "dashDistance" to "dashDistance",
        "dd" to "dashDistance",

        "turncooldown" to "turnCooldown",
        "turnCooldown" to "turnCooldown",
        "tcool" to "turnCooldown",

        "chargeparticle" to "chargeParticle",
        "chargeParticle" to "chargeParticle",
        "cp" to "chargeParticle",

        // Ranged
        "preferreddistance" to "preferredDistance",
        "preferredDistance" to "preferredDistance",
        "pd" to "preferredDistance",
        "pref" to "preferredDistance",

        "mindistance" to "minDistance",
        "minDistance" to "minDistance",
        "mind" to "minDistance",

        "maxdistance" to "maxDistance",
        "maxDistance" to "maxDistance",
        "maxd" to "maxDistance",

        // Territorial
        "territoryradius" to "territoryRadius",
        "territoryRadius" to "territoryRadius",
        "tr" to "territoryRadius",
        "territory" to "territoryRadius",

        "returnspeed" to "returnSpeed",
        "returnSpeed" to "returnSpeed",
        "rs" to "returnSpeed",

        // Stationary
        "rotatetotarget" to "rotateToTarget",
        "rotateToTarget" to "rotateToTarget",
        "rtt" to "rotateToTarget",
        "rotate" to "rotateToTarget"
    )

    /**
     * Expand compact AI configuration to full map
     */
    fun expandAIMap(compact: Map<*, *>): Map<String, Any> {
        val expanded = mutableMapOf<String, Any>()

        compact.forEach { (key, value) ->
            if (key is String && value != null) {
                val fullKey = aiFieldMap[key.lowercase()] ?: key
                expanded[fullKey] = value
            }
        }

        return expanded
    }

    /**
     * Parse compact AI inline notation
     * Format: {type: AGGRESSIVE, ms: 0.5, fr: 16}
     */
    fun parseCompactAI(data: Any?): Map<String, Any>? {
        return when (data) {
            is Map<*, *> -> expandAIMap(data)
            is String -> parseInlineString(data)
            else -> null
        }
    }

    /**
     * Parse inline string format
     * Example: "type=AGGRESSIVE ms=0.5 fr=16"
     */
    private fun parseInlineString(str: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        // Split by spaces or commas
        val parts = str.split(Regex("[,\\s]+"))

        for (part in parts) {
            if (part.contains('=')) {
                val (key, value) = part.split('=', limit = 2)
                val fullKey = aiFieldMap[key.trim().lowercase()] ?: key.trim()

                // Try to parse as number
                val parsedValue: Any = when {
                    value == "true" -> true
                    value == "false" -> false
                    value.toDoubleOrNull() != null -> value.toDouble()
                    value.toIntOrNull() != null -> value.toInt()
                    else -> value.trim().removeSurrounding("\"")
                }

                result[fullKey] = parsedValue
            }
        }

        return result
    }

    /**
     * Create AIConfig from compact map (camelCase fields)
     */
    fun createAIConfig(map: Map<String, Any>): AIConfig {
        return AIConfig(
            type = AIType.fromString(map["type"] as? String ?: "SIMPLE"),
            targetCondition = map["targetCondition"] as? String,
            damageFormula = map["damageFormula"] as? String,
            moveSpeed = (map["moveSpeed"] as? Number)?.toDouble() ?: 0.3,
            followRange = (map["followRange"] as? Number)?.toDouble() ?: 16.0,
            // Aggressive
            pursuitTime = (map["pursuitTime"] as? Number)?.toDouble() ?: 30.0,
            memoryDuration = (map["memoryDuration"] as? Number)?.toDouble() ?: 10.0,
            aggroRange = (map["aggroRange"] as? Number)?.toDouble() ?: 32.0,
            // Pathfinding
            usePathfinding = map["usePathfinding"] as? Boolean ?: false,
            pathfindingUpdateInterval = (map["pathfindingUpdateInterval"] as? Number)?.toInt() ?: 20,
            // Charge
            chargeDuration = (map["chargeDuration"] as? Number)?.toDouble() ?: 2.0,
            dashSpeed = (map["dashSpeed"] as? Number)?.toDouble() ?: 2.0,
            dashDistance = (map["dashDistance"] as? Number)?.toDouble() ?: 20.0,
            turnCooldown = (map["turnCooldown"] as? Number)?.toDouble() ?: 3.0,
            chargeParticle = map["chargeParticle"] as? String,
            // Ranged
            preferredDistance = (map["preferredDistance"] as? Number)?.toDouble() ?: 10.0,
            minDistance = (map["minDistance"] as? Number)?.toDouble() ?: 5.0,
            maxDistance = (map["maxDistance"] as? Number)?.toDouble() ?: 20.0,
            // Territorial
            territoryRadius = (map["territoryRadius"] as? Number)?.toDouble() ?: 32.0,
            returnSpeed = (map["returnSpeed"] as? Number)?.toDouble() ?: 0.5,
            // Stationary
            rotateToTarget = map["rotateToTarget"] as? Boolean ?: true
        )
    }
}
