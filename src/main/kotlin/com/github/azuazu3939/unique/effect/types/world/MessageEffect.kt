package com.github.azuazu3939.unique.effect.types.world

import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * メッセージエフェクト
 */
class MessageEffect(
    id: String = "message",
    private val message: String,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target is Player) {
            target.sendMessage(message)
            DebugLogger.effect("Message", target.name)
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target is Player) {
            target.sendMessage(message)
            DebugLogger.effect("Message from PacketEntity", target.name)
        }
    }

    override fun getDescription(): String = "Message: $message"
}
