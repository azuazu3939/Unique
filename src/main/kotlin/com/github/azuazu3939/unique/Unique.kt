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

class Unique : SuspendingJavaPlugin() {

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

    @Suppress("UnstableApiUsage")
    override fun onLoad() {
        debugLogger = DebugLogger(logger, 1)

        platformScheduler = PlatformDetector.getScheduler()
        debugLogger.critical("Detected platform: ${PlatformDetector.getPlatformName()}")

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.checkForUpdates(false)
        PacketEvents.getAPI().load()

        debugLogger.critical("PacketEvents loaded in onLoad()")
    }

    override fun onEnable() {
        saveDefaultConfig()

        PacketEvents.getAPI().init()
        debugLogger.critical("PacketEvents initialized in onEnable()")

        configLoader = ConfigLoader(this)

        aiManager = AIManager()
        skillManager = SkillManager(this)
        mobManager = MobManager(this)
        timerManager = TimerManager(this)
        debugLogger.detailed("Managers initialized")

        server.pluginManager.registerEvents(MobEventListener(this), this)
        debugLogger.detailed("Bukkit event listeners registered")

        val entityEventHandler = EntityLibEventHandler(this)
        PacketEvents.getAPI().eventManager.registerListener(
            entityEventHandler,
            PacketListenerPriority.HIGHEST
        )
        debugLogger.detailed("Virtual entity packet listener registered")

        timerManager.start()
        debugLogger.detailed("TimerManager started")

        reloadConfigs()
        debugLogger.critical("Unique plugin has been enabled!")
    }

    override fun onDisable() {
        if (::timerManager.isInitialized) {
            timerManager.stop()
            debugLogger.detailed("TimerManager stopped")
        }

        if (::mobManager.isInitialized) {
            mobManager.clear()
            debugLogger.detailed("MobManager cleared")
        }

        try {
            PacketEvents.getAPI().terminate()
            debugLogger.critical("PacketEvents terminated")
        } catch (e: Exception) {
            debugLogger.critical("Error terminating PacketEvents: ${e.message}")
        }

        debugLogger.critical("Unique plugin has been disabled!")
    }

    fun reloadConfigs() {
        configLoader.reloadAll()
        debugLogger.important("Configuration reloaded successfully")
    }
}