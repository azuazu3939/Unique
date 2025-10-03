package com.github.azuazu3939.unique

import com.github.azuazu3939.unique.ai.AIManager
import com.github.azuazu3939.unique.config.ConfigLoader
import com.github.azuazu3939.unique.event.EntityLibEventHandler
import com.github.azuazu3939.unique.event.MobEventListener
import com.github.azuazu3939.unique.event.TimerManager
import com.github.azuazu3939.unique.mob.MobManager
import com.github.azuazu3939.unique.nms.PlatformDetector
import com.github.azuazu3939.unique.nms.PlatformScheduler
import com.github.azuazu3939.unique.skill.SkillManager
import com.github.azuazu3939.unique.utils.DebugLogger
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.tofaa.entitylib.APIConfig
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform

class Unique : SuspendingJavaPlugin() {

    lateinit var plugin: Unique

    lateinit var debugLogger: DebugLogger
        private set

    lateinit var aiManager: AIManager
        private set

    lateinit var skillManager: SkillManager
        private set

    lateinit var mobManager: MobManager
        private set

    lateinit var platformScheduler: PlatformScheduler
        private set

    lateinit var timerManager: TimerManager
        private set

    private lateinit var configLoader: ConfigLoader

    override fun onLoad() {
        // Initialize debug logger early
        debugLogger = DebugLogger(logger, 1)

        // Initialize platform scheduler
        platformScheduler = PlatformDetector.getScheduler()
        debugLogger.critical("Detected platform: ${PlatformDetector.getPlatformName()}")

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()

        debugLogger.critical("PacketEvents initialized")
    }

    override fun onEnable() {
        plugin = this
        PacketEvents.getAPI().init()
        EntityLib.init(
            SpigotEntityLibPlatform(this),
            APIConfig(PacketEvents.getAPI()).tickTickables().usePlatformLogger())

        debugLogger.critical("EntityLib initialized")

        // Initialize AI manager
        aiManager = AIManager()

        // Initialize skill manager
        skillManager = SkillManager(this)

        // Initialize mob manager
        mobManager = MobManager(this)

        // Initialize timer manager
        timerManager = TimerManager(this)
        timerManager.start()
        debugLogger.detailed("TimerManager initialized")

        // Initialize config loader
        configLoader = ConfigLoader(this)

        // Register event listeners
        server.pluginManager.registerEvents(MobEventListener(this), this)
        debugLogger.detailed("Bukkit event listeners registered")

        // Register EntityLib packet listener for fake entity interactions
        val entityLibHandler = EntityLibEventHandler(this)
        PacketEvents.getAPI().eventManager.registerListener(entityLibHandler, PacketListenerPriority.HIGHEST)
        debugLogger.detailed("EntityLib packet listener registered")

        // Load config, skills, and mobs on startup
        configLoader.loadAll()

        debugLogger.critical("Unique plugin has been enabled!")
    }

    override fun onDisable() {
        // Stop timer manager
        if (::timerManager.isInitialized) {
            timerManager.stop()
        }

        // Cleanup all mobs
        if (::mobManager.isInitialized) {
            mobManager.clear()
        }

        // Terminate PacketEvents
        PacketEvents.getAPI().terminate()

        debugLogger.critical("Unique plugin has been disabled!")
    }

    fun reloadConfigs() {
        configLoader.reloadAll()
        debugLogger.important("Configuration reloaded successfully")
    }
}
