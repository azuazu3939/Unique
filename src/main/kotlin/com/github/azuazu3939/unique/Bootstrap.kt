package com.github.azuazu3939.unique

import com.github.azuazu3939.unique.event.UniqueReloadAfterEvent
import com.github.azuazu3939.unique.event.UniqueReloadBeforeEvent
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Unique Bootstrap
 *
 * Paper plugin用のBootstrapクラス
 * コマンド登録などのライフサイクルイベントを処理
 */
@Suppress("UnstableApiUsage")
class Bootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        // コマンドライフサイクルイベントに登録
        context.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, ::commands)
    }

    private fun commands(event: ReloadableRegistrarEvent<Commands>) {
        with(event.registrar()) {
            // メインコマンドとエイリアスを登録
            register(UniqueCommand.builder().build(), "Main command for Unique plugin", listOf("uq", "uni"))
        }
    }
}

object UniqueCommand : BrigadierCommand {

    override fun builder(): LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("unique")
        .requires { it.sender.hasPermission("unique.command") }
        .executes { ctx ->
            sendHelp(ctx.source.sender)
            1
        }
        // /unique reload
        .then(
            Commands.literal("reload")
                .requires { it.sender.hasPermission("unique.reload") }
                .executes { ctx ->
                    handleReload(ctx.source.sender)
                    1
                }
        )
        // /unique list <type>
        .then(
            Commands.literal("list")
                .then(
                    Commands.literal("mobs")
                        .executes { ctx ->
                            handleListMobs(ctx.source.sender)
                            1
                        }
                )
                .then(
                    Commands.literal("spawns")
                        .executes { ctx ->
                            handleListSpawns(ctx.source.sender)
                            1
                        }
                )
                .then(
                    Commands.literal("entities")
                        .executes { ctx ->
                            handleListEntities(ctx.source.sender)
                            1
                        }
                )
        )
        // /unique spawn <mob>
        .then(
            Commands.literal("spawn")
                // .requires { it.sender.hasPermission("unique.spawn") }  // 一時的に無効化
                .then(
                    Commands.argument("mob", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests { ctx, builder ->
                            // Mob名の候補を提供
                            try {
                                Unique.instance.mobManager.getAllMobDefinitions().keys.forEach { mobName ->
                                    builder.suggest(mobName)
                                }
                            } catch (e: Exception) {
                                // 初期化中の場合は空リスト
                            }
                            builder.buildFuture()
                        }
                        .executes { ctx ->
                            val mobName = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "mob")
                            handleSpawn(ctx.source.sender, mobName)
                            1
                        }
                )
        )
        // /unique debug <type>
        .then(
            Commands.literal("debug")
                .requires { it.sender.hasPermission("unique.debug") }
                .then(
                    Commands.literal("cel")
                        .executes { ctx ->
                            handleDebugCel(ctx.source.sender)
                            1
                        }
                )
                .then(
                    Commands.literal("config")
                        .executes { ctx ->
                            handleDebugConfig(ctx.source.sender)
                            1
                        }
                )
                .then(
                    Commands.literal("entities")
                        .executes { ctx ->
                            handleDebugEntities(ctx.source.sender)
                            1
                        }
                )
                .then(
                    Commands.literal("skills")
                        .executes { ctx ->
                            handleDebugSkills(ctx.source.sender)
                            1
                        }
                )
                .then(
                    Commands.literal("spawns")
                        .executes { ctx ->
                            handleDebugSpawns(ctx.source.sender)
                            1
                        }
                )
        )
        // /unique info
        .then(
            Commands.literal("info")
                .executes { ctx ->
                    handleInfo(ctx.source.sender)
                    1
                }
        )

    /**
     * reload - 設定とMob定義を再読み込み
     */
    private fun handleReload(sender: CommandSender) {
        val plugin = Unique.instance
        val startTime = System.currentTimeMillis()

        // Beforeイベント発火
        val beforeEvent = UniqueReloadBeforeEvent(sender)
        Bukkit.getPluginManager().callEvent(beforeEvent)

        if (beforeEvent.isCancelled) {
            sender.sendMessage("§6[Unique] §cReload cancelled: ${beforeEvent.cancelReason ?: "Unknown reason"}")
            return
        }

        sender.sendMessage("§6[Unique] §7Reloading...")

        var success = true
        var errorMessage: String? = null
        var mobCount = 0
        var spawnCount = 0

        try {
            plugin.configManager.reloadConfigs()
            plugin.mobManager.loadMobDefinitions()
            mobCount = plugin.mobManager.getAllMobDefinitions().size

            plugin.spawnManager.loadSpawnDefinitions()
            plugin.spawnManager.restartSpawnTasks()
            spawnCount = plugin.spawnManager.getAllSpawnDefinitions().size

            sender.sendMessage("§6[Unique] §aReload complete!")
            sender.sendMessage("§6[Unique] §7Loaded §e$mobCount§7 mobs and §e$spawnCount§7 spawns")
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
            sender.sendMessage("§6[Unique] §cReload failed: ${e.message}")
            DebugLogger.error("Failed to reload configuration", e)
        }

        // Afterイベント発火
        val duration = System.currentTimeMillis() - startTime
        val afterEvent = UniqueReloadAfterEvent(sender, success, errorMessage).apply {
            this.mobCount = mobCount
            this.spawnCount = spawnCount
            this.duration = duration
        }
        Bukkit.getPluginManager().callEvent(afterEvent)
    }

    /**
     * list mobs
     */
    private fun handleListMobs(sender: CommandSender) {
        val mobs = Unique.instance.mobManager.getAllMobDefinitions()
        sender.sendMessage("§6[Unique] §7Loaded Mobs (${mobs.size}):")
        mobs.forEach { (name, def) ->
            sender.sendMessage("  §e$name §7- ${def.type} (HP: ${def.health})")
        }
    }

    /**
     * list spawns
     */
    private fun handleListSpawns(sender: CommandSender) {
        val spawns = Unique.instance.spawnManager.getAllSpawnDefinitions()
        sender.sendMessage("§6[Unique] §7Loaded Spawns (${spawns.size}):")
        spawns.forEach { (name, def) ->
            sender.sendMessage("  §e$name §7- Mob: ${def.mob}, Rate: ${def.spawnRate}t")
        }
    }

    /**
     * list entities
     */
    private fun handleListEntities(sender: CommandSender) {
        val entities = Unique.instance.packetEntityManager.getAllEntities()
        sender.sendMessage("§6[Unique] §7Active Entities (${entities.size}):")
        entities.forEach { entity ->
            sender.sendMessage("  §e${entity.entityType} §7- ID: ${entity.entityId}, HP: ${entity.health}/${entity.maxHealth}")
        }
    }

    /**
     * spawn - Mobを手動スポーン
     */
    private fun handleSpawn(sender: CommandSender, mobName: String) {
        if (sender !is Player) {
            sender.sendMessage("§6[Unique] §cThis command can only be used by players")
            return
        }

        val plugin = Unique.instance
        val definition = plugin.mobManager.getMobDefinition(mobName)

        if (definition == null) {
            sender.sendMessage("§6[Unique] §cMob not found: $mobName")
            return
        }

        val location = sender.location

        plugin.launch(plugin.regionDispatcher(location)) {
            try {
                val mob = plugin.mobManager.spawnMob(mobName, location)

                if (mob != null) {
                    sender.sendMessage("§6[Unique] §aSpawned $mobName at your location")
                } else {
                    sender.sendMessage("§6[Unique] §cFailed to spawn $mobName")
                }
            } catch (e: Exception) {
                sender.sendMessage("§6[Unique] §cError: ${e.message}")
                DebugLogger.error("Failed to spawn mob via command", e)
            }
        }
    }

    /**
     * debug cel
     */
    private fun handleDebugCel(sender: CommandSender) {
        Unique.instance.celEngine.printDebugInfo()
        sender.sendMessage("§6[Unique] §7CEL debug info printed to console")
    }

    /**
     * debug config
     */
    private fun handleDebugConfig(sender: CommandSender) {
        Unique.instance.configManager.printDebugInfo()
        sender.sendMessage("§6[Unique] §7Config debug info printed to console")
    }

    /**
     * debug entities
     */
    private fun handleDebugEntities(sender: CommandSender) {
        Unique.instance.packetEntityManager.printDebugInfo()
        sender.sendMessage("§6[Unique] §7Entity debug info printed to console")
    }

    /**
     * debug skills
     */
    private fun handleDebugSkills(sender: CommandSender) {
        Unique.instance.skillExecutor.printDebugInfo()
        sender.sendMessage("§6[Unique] §7Skill debug info printed to console")
    }

    /**
     * debug spawns
     */
    private fun handleDebugSpawns(sender: CommandSender) {
        Unique.instance.spawnManager.printDebugInfo()
        sender.sendMessage("§6[Unique] §7Spawn debug info printed to console")
    }

    /**
     * info - プラグイン情報表示
     */
    private fun handleInfo(sender: CommandSender) {
        val plugin = Unique.instance
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


@FunctionalInterface
interface BrigadierCommand {
    fun builder(): LiteralArgumentBuilder<CommandSourceStack>
}