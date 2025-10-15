package com.github.azuazu3939.unique.targeter.types.entity

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * 視線先のターゲット
 */
class LineOfSightTargeter(
    id: String = "line_of_sight",
    private val maxDistance: Double = 50.0,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        if (source !is LivingEntity) {
            return emptyList()
        }

        val target = source.getTargetEntity(maxDistance.toInt())
        val targets = if (target != null && target.isValid && !target.isDead) {
            listOf(target)
        } else {
            emptyList()
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("LineOfSight(maxDistance=$maxDistance)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // PacketEntityには視線がないため、空リストを返す
        DebugLogger.targeter("LineOfSight (PacketEntity)", 0)
        return emptyList()
    }

    override fun getDescription(): String = "Entity in line of sight (max $maxDistance blocks)"
}
