package com.github.azuazu3939.unique.config

import com.github.azuazu3939.unique.Unique
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigLoader(private val plugin: Unique) {

    private val mobsFolder: File = File(plugin.dataFolder, "Mobs")
    private val configFile: File = File(plugin.dataFolder, "config.yml")

    fun loadAll() {
        loadConfig()
        loadMobs()
    }

    private fun loadConfig() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        if (!configFile.exists()) {
            plugin.saveDefaultConfig()
        } else {
            plugin.reloadConfig()
        }
    }

    private fun loadMobs() {
        if (!mobsFolder.exists()) {
            mobsFolder.mkdirs()
        }

        val ymlFiles = mobsFolder.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: emptyArray()

        for (file in ymlFiles) {
            try {
                val config = YamlConfiguration.loadConfiguration(file)
                // ここでMobのデータを処理できます（現在は空のまま）
                plugin.logger.info("Loaded mob configuration: ${file.name}")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load ${file.name}: ${e.message}")
            }
        }
    }

    fun reloadAll() {
        loadAll()
    }
}
