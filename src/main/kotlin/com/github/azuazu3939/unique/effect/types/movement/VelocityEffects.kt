package com.github.azuazu3939.unique.effect.types.movement

import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * 速度変更エフェクト
 */
class VelocityEffect(
    id: String = "velocity",
    private val velocityX: Double,
    private val velocityY: Double,
    private val velocityZ: Double,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val velocity = org.bukkit.util.Vector(velocityX, velocityY, velocityZ)
        target.velocity = velocity

        DebugLogger.effect("Velocity($velocityX, $velocityY, $velocityZ)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val velocity = org.bukkit.util.Vector(velocityX, velocityY, velocityZ)
        target.velocity = velocity

        DebugLogger.effect("Velocity($velocityX, $velocityY, $velocityZ) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Set velocity to ($velocityX, $velocityY, $velocityZ)"
}

/**
 * ノックバックエフェクト
 */
class KnockbackEffect(
    id: String = "knockback",
    private val strength: Double = 1.0,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val direction = target.location.toVector()
            .subtract(source.location.toVector())
            .normalize()
            .multiply(strength)

        target.velocity = target.velocity.add(direction)

        DebugLogger.effect("Knockback($strength)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val direction = target.location.toVector()
            .subtract(source.location.toVector())
            .normalize()
            .multiply(strength)

        target.velocity = target.velocity.add(direction)

        DebugLogger.effect("Knockback($strength) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Knockback: $strength"
}
