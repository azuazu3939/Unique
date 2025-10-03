package com.github.azuazu3939.unique.mob.targeting

import com.github.azuazu3939.unique.mob.condition.ConditionEvaluator
import com.github.azuazu3939.unique.mob.data.TargetSelector
import org.bukkit.Location
import org.bukkit.entity.LivingEntity

/**
 * Resolves MythicMobs-style target selectors
 *
 * Supported targeters:
 * - @Self - The mob itself
 * - @Target - Current target
 * - @Trigger - Entity that triggered the skill
 * - @Origin - Origin location/entity (skill execution point)
 * - @PIR{r=10} - Players In Radius
 * - @EIR{r=10} - Entities In Radius
 * - @PNO{r=10} - Players Near Origin
 * - @ENO{r=10} - Entities Near Origin
 */
class TargetResolver(private val conditionEvaluator: ConditionEvaluator) {

    /**
     * Resolve a target selector to a list of entities
     *
     * @param selector The target selector to resolve
     * @param origin Origin location (skill execution point)
     * @param self The mob itself
     * @param target Current target entity
     * @param trigger Trigger entity (e.g., player who attacked)
     * @return List of resolved target entities
     */
    fun resolve(
        selector: TargetSelector?,
        origin: Location,
        self: LivingEntity?,
        target: LivingEntity?,
        trigger: LivingEntity?
    ): List<LivingEntity> {
        if (selector == null) {
            // Default: target current target
            return target?.let { listOf(it) } ?: emptyList()
        }

        val targets = when (selector.type.uppercase()) {
            "SELF", "CASTER", "ME" -> {
                self?.let { listOf(it) } ?: emptyList()
            }

            "TARGET", "T" -> {
                target?.let { listOf(it) } ?: emptyList()
            }

            "TRIGGER", "TRIG" -> {
                trigger?.let { listOf(it) } ?: emptyList()
            }

            "ORIGIN", "O" -> {
                // Origin can be an entity or just a location
                // For now, return entities at origin location
                origin.world?.getNearbyEntities(origin, 1.0, 1.0, 1.0)
                    ?.filterIsInstance<LivingEntity>()
                    ?: emptyList()
            }

            "PIR", "PLAYERSINRADIUS", "PLAYERSRADIUS" -> {
                val range = selector.range ?: 10.0
                origin.world?.getNearbyPlayers(origin, range)?.toList() ?: emptyList()
            }

            "EIR", "ENTITIESINRADIUS", "ENTITIESRADIUS" -> {
                val range = selector.range ?: 10.0
                origin.world?.getNearbyEntities(origin, range, range, range)
                    ?.filterIsInstance<LivingEntity>()
                    ?.filter { it != self } // Exclude self
                    ?: emptyList()
            }

            "PNO", "PLAYERSNEARORIGIN", "PLAYERSORIGIN" -> {
                val range = selector.range ?: 10.0
                origin.world?.getNearbyPlayers(origin, range)?.toList() ?: emptyList()
            }

            "ENO", "ENTITIESNEARORIGIN", "ENTITIESORIGIN" -> {
                val range = selector.range ?: 10.0
                origin.world?.getNearbyEntities(origin, range, range, range)
                    ?.filterIsInstance<LivingEntity>()
                    ?.filter { it != self } // Exclude self
                    ?: emptyList()
            }

            else -> emptyList()
        }

        // Apply conditions to filter targets
        return if (selector.conditions.isNotEmpty()) {
            targets.filter { entity ->
                selector.conditions.all { condition ->
                    conditionEvaluator.evaluateTargetCondition(condition, entity)
                }
            }
        } else {
            targets
        }
    }

    /**
     * Resolve simple target string (@target, @self, etc.)
     */
    fun resolveSimple(
        targetString: String?,
        origin: Location,
        self: LivingEntity?,
        target: LivingEntity?,
        trigger: LivingEntity?
    ): List<LivingEntity> {
        if (targetString == null) {
            return target?.let { listOf(it) } ?: emptyList()
        }

        // Remove @ prefix if present
        val type = targetString.removePrefix("@")

        return when (type.uppercase()) {
            "SELF", "CASTER", "ME" -> self?.let { listOf(it) } ?: emptyList()
            "TARGET", "T" -> target?.let { listOf(it) } ?: emptyList()
            "TRIGGER", "TRIG" -> trigger?.let { listOf(it) } ?: emptyList()
            else -> target?.let { listOf(it) } ?: emptyList()
        }
    }
}
