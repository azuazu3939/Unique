package com.github.azuazu3939.unique

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Suppress("UnstableApiUsage")
class Bootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        val lifecycleManager: LifecycleEventManager<BootstrapContext> = context.lifecycleManager

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(
                Commands.literal("unique")
                    .requires { it.sender.hasPermission("unique.use") }
                    .then(
                        Commands.literal("spawn")
                            .requires { it.sender.hasPermission("unique.spawn") }
                            .then(
                                Commands.argument("mobId", StringArgumentType.string())
                                    .suggests { _, builder ->
                                        // Get registered mob IDs dynamically
                                        val pluginInstance = JavaPlugin.getPlugin(Unique::class.java)
                                        if (pluginInstance.isEnabled) {
                                            try {
                                                pluginInstance.mobManager.getAllMobDefinitions()
                                                    .map { it.id }
                                                    .forEach { builder.suggest(it) }
                                            } catch (e: Exception) {
                                                // mobManager not initialized yet
                                            }
                                        }
                                        builder.buildFuture()
                                    }
                                    .executes { ctx ->
                                        val mobId = StringArgumentType.getString(ctx, "mobId")
                                        handleSpawn(ctx.source, mobId, 1, null)
                                        1
                                    }
                                    .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                            .executes { ctx ->
                                                val mobId = StringArgumentType.getString(ctx, "mobId")
                                                val amount = IntegerArgumentType.getInteger(ctx, "amount")
                                                handleSpawn(ctx.source, mobId, amount, null)
                                                1
                                            }
                                            .then(
                                                Commands.argument("players", StringArgumentType.greedyString())
                                                    .suggests { _, builder ->
                                                        // Suggest @selectors
                                                        builder.suggest("@a")  // All players
                                                        builder.suggest("@s")  // Self
                                                        builder.suggest("@p")  // Nearest player

                                                        // Suggest online player names
                                                        Bukkit.getOnlinePlayers().forEach { player ->
                                                            builder.suggest(player.name)
                                                        }
                                                        builder.buildFuture()
                                                    }
                                                    .executes { ctx ->
                                                        val mobId = StringArgumentType.getString(ctx, "mobId")
                                                        val amount = IntegerArgumentType.getInteger(ctx, "amount")
                                                        val playersArg = StringArgumentType.getString(ctx, "players")
                                                        handleSpawn(ctx.source, mobId, amount, playersArg)
                                                        1
                                                    }
                                            )
                                    )
                            )
                    )
                    .then(
                        Commands.literal("reload")
                            .requires { it.sender.hasPermission("unique.reload") }
                            .executes { ctx ->
                                handleReload(ctx.source)
                                1
                            }
                    )
                    .build(),
                "Main command for Unique plugin",
                listOf("uni")
            )
        }
    }

    private fun handleSpawn(
        source: io.papermc.paper.command.brigadier.CommandSourceStack,
        mobId: String,
        amount: Int,
        playersArg: String?
    ) {
        val plugin = Bukkit.getPluginManager().getPlugin("unique") as? Unique
        if (plugin == null || !plugin.isEnabled) {
            source.sender.sendMessage("§cPlugin not loaded or not enabled!")
            return
        }

        val executor = source.executor
        if (executor !is org.bukkit.entity.Player) {
            source.sender.sendMessage("§cThis command can only be used by players.")
            return
        }

        // Parse target players
        val viewers = if (playersArg.isNullOrBlank()) {
            // Default: all online players
            listOf(executor)
        } else {
            // Parse player names/selectors
            when (playersArg) {
                "@a" -> executor.world.players
                "@s", "@p" -> listOf(executor)
                else -> {
                    // Try to find player by name
                    val player = Bukkit.getPlayer(playersArg)
                    if (player != null) {
                        listOf(player)
                    } else {
                        source.sender.sendMessage("§cPlayer not found: $playersArg")
                        return
                    }
                }
            }
        }

        if (viewers.isEmpty()) {
            source.sender.sendMessage("§cNo viewers found!")
            return
        }

        // Spawn mobs
        val location = executor.location
        var spawnedCount = 0

        repeat(amount) {
            val mob = plugin.mobManager.spawnMob(mobId, location.clone().add(
                (Math.random() - 0.5) * 2,
                0.0,
                (Math.random() - 0.5) * 2
            ), viewers)
            if (mob != null) {
                spawnedCount++
            }
        }

        if (spawnedCount > 0) {
            source.sender.sendMessage("§aSpawned §e$spawnedCount§a x §e$mobId§a for §e${viewers.size}§a viewer(s)")
            plugin.debugLogger.command("unique spawn $mobId $amount $playersArg", executor.name, "Spawned $spawnedCount mob(s)")
        } else {
            source.sender.sendMessage("§cFailed to spawn mob: $mobId (not found or error)")
            plugin.debugLogger.command("unique spawn $mobId", executor.name, "Failed to spawn mob")
        }
    }

    private fun handleReload(source: io.papermc.paper.command.brigadier.CommandSourceStack) {
        val plugin = JavaPlugin.getPlugin(Unique::class.java)
        try {
            plugin.reloadConfigs()
            source.sender.sendMessage("§aConfig and Mobs files reloaded successfully!")
            plugin.debugLogger.command("unique reload", source.sender.name, "Reload successful")
        } catch (e: Exception) {
            source.sender.sendMessage("§cFailed to reload: ${e.message}")
            plugin.debugLogger.error("Reload failed: ${e.message}")
            plugin.debugLogger.command("unique reload", source.sender.name, "Reload failed")
            if (plugin.debugLogger.getDebugLevel() >= 3) {
                e.printStackTrace()
            }
        }
    }
}