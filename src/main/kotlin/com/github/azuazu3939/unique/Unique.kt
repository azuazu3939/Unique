package com.github.azuazu3939.unique

import com.github.azuazu3939.unique.command.UniqueCommand
import com.github.azuazu3939.unique.config.ConfigLoader
import org.bukkit.plugin.java.JavaPlugin

class Unique : JavaPlugin() {

    private lateinit var configLoader: ConfigLoader

    override fun onEnable() {
        // Initialize config loader
        configLoader = ConfigLoader(this)

        // Load config and mobs on startup
        configLoader.loadAll()

        // Register commands
        getCommand("unique")?.let {
            val commandExecutor = UniqueCommand(this)
            it.setExecutor(commandExecutor)
            it.tabCompleter = commandExecutor
        }

        logger.info("Unique plugin has been enabled!")
    }

    override fun onDisable() {
        logger.info("Unique plugin has been disabled!")
    }

    fun reloadConfigs() {
        configLoader.reloadAll()
        logger.info("Configuration reloaded!")
    }
}
