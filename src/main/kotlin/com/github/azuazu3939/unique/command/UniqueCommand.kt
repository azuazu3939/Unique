package com.github.azuazu3939.unique.command

import com.github.azuazu3939.unique.Unique
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class UniqueCommand(private val plugin: Unique) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /unique reload")
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("unique.reload")) {
                    sender.sendMessage("§cYou don't have permission to use this command.")
                    return true
                }

                plugin.reloadConfigs()
                sender.sendMessage("§aConfig and Mobs files reloaded successfully!")
            }
            else -> {
                sender.sendMessage("§cUsage: /unique reload")
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("reload").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
