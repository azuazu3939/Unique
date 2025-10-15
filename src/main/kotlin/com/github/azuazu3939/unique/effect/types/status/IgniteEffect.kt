package com.github.azuazu3939.unique.effect.types.status

import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import kotlin.time.Duration

/**
 * 発火エフェクト
 */
class IgniteEffect(
    id: String = "ignite",
    private val duration: Duration,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        target.fireTicks = (duration.inWholeMilliseconds / 50).toInt()
        DebugLogger.effect("Ignite(${duration.inWholeSeconds}s)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        target.fireTicks = (duration.inWholeMilliseconds / 50).toInt()
        DebugLogger.effect("Ignite(${duration.inWholeSeconds}s) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Ignite for ${duration.inWholeSeconds}s"
}
