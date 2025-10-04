package com.github.azuazu3939.unique.command

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.launch
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Uniqueメインコマンド
 *
 * /unique <subcommand> [<args...>]
 */
class UniqueCommand(private val plugin: Unique) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "list" -> handleList(sender, args)
            "spawn" -> handleSpawn(sender, args)
            "debug" -> handleDebug(sender, args)
            "info" -> handleInfo(sender)
            else -> sendHelp(sender)
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("reload", "list", "spawn", "debug", "info")
                .filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2) {
            when (args[0].lowercase()) {
                "list" -> return listOf("mobs", "spawns", "entities")
                    .filter { it.startsWith(args[1].lowercase()) }

                "spawn" -> return plugin.mobManager.getAllMobDefinitions().keys
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }

                "debug" -> return listOf("cel", "config", "entities", "skills", "spawns")
                    .filter { it.startsWith(args[1].lowercase()) }
            }
        }

        return emptyList()
    }

    /**
     * reload - 設定とMob定義を再読み込み
     */
    private fun handleReload(sender: CommandSender) {
        sender.sendMessage("§6[Unique] §7Reloading...")

        try {
            // 設定再読み込み
            plugin.configManager.reloadConfigs()

            // Mob定義再読み込み
            plugin.mobManager.loadMobDefinitions()

            // スポーン定義再読み込み
            plugin.spawnManager.loadSpawnDefinitions()
            plugin.spawnManager.restartSpawnTasks()

            sender.sendMessage("§6[Unique] §aReload complete!")
            DebugLogger.info("Configuration reloaded by ${sender.name}")

        } catch (e: Exception) {
            sender.sendMessage("§6[Unique] §cReload failed: ${e.message}")
            DebugLogger.error("Failed to reload configuration", e)
        }
    }

    /**
     * list - 一覧表示
     */
    private fun handleList(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§6[Unique] §7Usage: /unique list <mobs|spawns|entities>")
            return
        }

        when (args[1].lowercase()) {
            "mobs" -> {
                val mobs = plugin.mobManager.getAllMobDefinitions()
                sender.sendMessage("§6[Unique] §7Loaded Mobs (${mobs.size}):")
                mobs.forEach { (name, def) ->
                    sender.sendMessage("  §e$name §7- ${def.type} (HP: ${def.getHealth()})")
                }
            }

            "spawns" -> {
                val spawns = plugin.spawnManager.getAllSpawnDefinitions()
                sender.sendMessage("§6[Unique] §7Loaded Spawns (${spawns.size}):")
                spawns.forEach { (name, def) ->
                    sender.sendMessage("  §e$name §7- Mob: ${def.mob}, Rate: ${def.spawnRate}t")
                }
            }

            "entities" -> {
                val entities = plugin.packetEntityManager.getAllEntities()
                sender.sendMessage("§6[Unique] §7Active Entities (${entities.size}):")
                entities.forEach { entity ->
                    sender.sendMessage("  §e${entity.entityType} §7- ID: ${entity.entityId}, HP: ${entity.health}/${entity.maxHealth}")
                }
            }

            else -> sender.sendMessage("§6[Unique] §cInvalid list type")
        }
    }

    /**
     * spawn - Mobを手動スポーン
     */
    private fun handleSpawn(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§6[Unique] §cThis command can only be used by players")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§6[Unique] §7Usage: /unique spawn <mob_name>")
            return
        }

        val mobName = args[1]
        val definition = plugin.mobManager.getMobDefinition(mobName)

        if (definition == null) {
            sender.sendMessage("§6[Unique] §cMob not found: $mobName")
            return
        }

        plugin.launch {
            try {
                val location = sender.location
                val mob = plugin.mobManager.spawnMob(mobName, location)

                if (mob != null) {
                    sender.sendMessage("§6[Unique] §aSpawned $mobName at your location")
                    DebugLogger.info("${sender.name} spawned $mobName at ${location.blockX}, ${location.blockY}, ${location.blockZ}")
                } else {
                    sender.sendMessage("§6[Unique] §cFailed to spawn $mobName")
                }
            } catch (e: Exception) {
                sender.sendMessage("§6[Unique] §cError: ${e.message}")
                DebugLogger.error("Failed to spawn mob", e)
            }
        }
    }

    /**
     * debug - デバッグ情報表示
     */
    private fun handleDebug(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§6[Unique] §7Usage: /unique debug <cel|config|entities|skills|spawns>")
            return
        }

        when (args[1].lowercase()) {
            "cel" -> {
                plugin.celEngine.printDebugInfo()
                sender.sendMessage("§6[Unique] §7CEL debug info printed to console")
            }

            "config" -> {
                plugin.configManager.printDebugInfo()
                sender.sendMessage("§6[Unique] §7Config debug info printed to console")
            }

            "entities" -> {
                plugin.packetEntityManager.printDebugInfo()
                sender.sendMessage("§6[Unique] §7Entity debug info printed to console")
            }

            "skills" -> {
                plugin.skillExecutor.printDebugInfo()
                sender.sendMessage("§6[Unique] §7Skill debug info printed to console")
            }

            "spawns" -> {
                plugin.spawnManager.printDebugInfo()
                sender.sendMessage("§6[Unique] §7Spawn debug info printed to console")
            }

            else -> sender.sendMessage("§6[Unique] §cInvalid debug type")
        }
    }

    /**
     * info - プラグイン情報表示
     */
    private fun handleInfo(sender: CommandSender) {
        val mobs = plugin.mobManager.getAllMobDefinitions().size
        val spawns = plugin.spawnManager.getAllSpawnDefinitions().size
        val entities = plugin.packetEntityManager.getEntityCount()
        val activeSkills = plugin.skillExecutor.getActiveJobCount()

        sender.sendMessage("§6╔══════════════════════════════════════╗")
        sender.sendMessage("§6║  §eUnique Plugin Information         §6║")
        sender.sendMessage("§6╠══════════════════════════════════════╣")
        sender.sendMessage("§6║  §7Version: §f${plugin.pluginMeta.version.padEnd(24)} §6║")
        sender.sendMessage("§6║  §7Loaded Mobs: §f${mobs.toString().padEnd(20)} §6║")
        sender.sendMessage("§6║  §7Loaded Spawns: §f${spawns.toString().padEnd(18)} §6║")
        sender.sendMessage("§6║  §7Active Entities: §f${entities.toString().padEnd(16)} §6║")
        sender.sendMessage("§6║  §7Active Skills: §f${activeSkills.toString().padEnd(18)} §6║")
        sender.sendMessage("§6╚══════════════════════════════════════╝")
    }

    /**
     * ヘルプ表示
     */
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6[Unique] §7Available commands:")
        sender.sendMessage("  §e/unique reload §7- Reload configuration")
        sender.sendMessage("  §e/unique list <type> §7- List mobs/spawns/entities")
        sender.sendMessage("  §e/unique spawn <mob> §7- Spawn a mob")
        sender.sendMessage("  §e/unique debug <type> §7- Show debug info")
        sender.sendMessage("  §e/unique info §7- Show plugin info")
    }
}