package com.github.azuazu3939.unique.effect.types.world

import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Particle
import org.bukkit.entity.Entity

/**
 * パーティクルエフェクト
 */
class ParticleEffect(
    id: String = "particle",
    private val particle: Particle,
    private val count: Int = 10,
    private val offsetX: Double = 0.5,
    private val offsetY: Double = 0.5,
    private val offsetZ: Double = 0.5,
    sync: Boolean = false
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val location = target.location
        location.world?.spawnParticle(
            particle,
            location,
            count,
            offsetX,
            offsetY,
            offsetZ
        )

        DebugLogger.effect("Particle(${particle.name})", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val location = target.location
        location.world?.spawnParticle(
            particle,
            location,
            count,
            offsetX,
            offsetY,
            offsetZ
        )

        DebugLogger.effect("Particle(${particle.name}) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Particle: ${particle.name} x$count"
}
