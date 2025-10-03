package com.github.azuazu3939.unique.event

/**
 * Parses trigger notation from skill strings
 * Examples:
 * - ~onAttack -> (ON_ATTACK, null)
 * - ~onDamaged -> (ON_DAMAGED, null)
 * - ~onTimer:20 -> (ON_TIMER, 20)
 * - ~onTimer:100 -> (ON_TIMER, 100)
 */
object TriggerParser {

    /**
     * Parse trigger from notation string
     * Returns pair of (SkillTrigger, interval in ticks if ON_TIMER)
     */
    fun parse(notation: String): Pair<SkillTrigger, Int?> {
        // Extract trigger pattern: ~onTriggerType or ~onTriggerType:param
        val triggerMatch = Regex("~([a-zA-Z0-9_:]+)").find(notation)
            ?: return Pair(SkillTrigger.ALWAYS, null) // No trigger specified, default to ALWAYS

        val triggerString = triggerMatch.groupValues[1]
        return SkillTrigger.parse(triggerString)
    }

    /**
     * Check if notation contains a trigger
     */
    fun hasTrigger(notation: String): Boolean {
        return notation.contains('~')
    }

    /**
     * Extract trigger substring from notation
     */
    fun extractTrigger(notation: String): String? {
        val triggerMatch = Regex("~([a-zA-Z0-9_:]+)").find(notation)
        return triggerMatch?.groupValues?.get(1)
    }
}
