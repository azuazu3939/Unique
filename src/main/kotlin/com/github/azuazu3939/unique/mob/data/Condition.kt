package com.github.azuazu3939.unique.mob.data

/**
 * Represents a MythicMobs-style condition
 *
 * Format: ?conditionType{options} or ?~conditionType{options}
 *
 * Prefixes:
 * - ? = Check condition on mob (self)
 * - ?~ = Check condition on trigger entity (e.g., player who attacked)
 *
 * Suffixes:
 * - ! = Negate (check if false)
 *
 * Examples:
 * - ?raining - Check if it's raining
 * - ?night - Check if it's night
 * - ?~holding{m=WOODEN_SWORD} - Check if trigger is holding wooden sword
 * - ?!raining - Check if it's NOT raining
 * - ?~!holding{m=WOODEN_SWORD} - Check if trigger is NOT holding wooden sword
 */
data class Condition(
    val type: String,                           // Condition type (e.g., "raining", "night", "holding")
    val target: ConditionTarget,                 // Who to check (SELF or TRIGGER)
    val negate: Boolean = false,                 // If true, check for false condition
    val options: Map<String, String> = emptyMap() // Condition-specific options
)

/**
 * Who the condition is checked on
 */
enum class ConditionTarget {
    SELF,       // Check on the mob itself (?)
    TRIGGER     // Check on the trigger entity (?~)
}

/**
 * Target selector with conditions
 *
 * Format: @PIR{r=10;conditions=[...]}
 *
 * Types:
 * - @target - Current target
 * - @self - The mob itself
 * - @trigger - Entity that triggered the skill
 * - @PIR - Players In Range
 * - @EIR - Entities In Range
 */
data class TargetSelector(
    val type: String,                            // Selector type (target, self, trigger, PIR, EIR)
    val range: Double? = null,                   // Range for PIR/EIR
    val conditions: List<TargetCondition> = emptyList(), // Conditions for target filtering
    val options: Map<String, String> = emptyMap() // Additional options
)

/**
 * Condition for target filtering
 *
 * Format: - conditionType{options} result
 *
 * Examples:
 * - hasaura{aura=Plagued} true
 * - haspotioneffect{type=WITHER;d=1to999999;l=0to254} true
 */
data class TargetCondition(
    val type: String,                            // Condition type
    val options: Map<String, String>,            // Condition options
    val expectedResult: Boolean                  // Expected result (true or false)
)
