package com.github.azuazu3939.unique.effect.types.world

import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * コマンド実行エフェクト
 */
class CommandEffect(
    id: String = "command",
    private val command: String,
    private val asOp: Boolean = false,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target is Player) {
            val wasOp = target.isOp

            if (asOp && !wasOp) {
                target.isOp = true
            }

            target.performCommand(command)

            if (asOp && !wasOp) {
                target.isOp = false
            }

            DebugLogger.effect("Command($command)", target.name)
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target is Player) {
            val wasOp = target.isOp

            if (asOp && !wasOp) {
                target.isOp = true
            }

            target.performCommand(command)

            if (asOp && !wasOp) {
                target.isOp = false
            }

            DebugLogger.effect("Command($command) from PacketEntity", target.name)
        }
    }

    override fun getDescription(): String {
        return if (asOp) "Command (as OP): $command" else "Command: $command"
    }
}
