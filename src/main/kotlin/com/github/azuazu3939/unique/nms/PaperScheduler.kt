package com.github.azuazu3939.unique.nms

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Paper-specific scheduler implementation
 * Uses Bukkit scheduler for all operations
 */
class PaperScheduler : PlatformScheduler {

    override fun runTask(plugin: Plugin, task: Runnable) {
        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun runTaskAsynchronously(plugin: Plugin, task: Runnable) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, task)
    }

    override fun runTaskLater(plugin: Plugin, task: Runnable, delay: Long) {
        plugin.server.scheduler.runTaskLater(plugin, task, delay)
    }

    override fun runTaskTimer(plugin: Plugin, task: Runnable, delay: Long, period: Long): SchedulerTask {
        val bukkitTask = plugin.server.scheduler.runTaskTimer(plugin, task, delay, period)
        return BukkitSchedulerTask(bukkitTask)
    }

    override fun runTaskAtLocation(plugin: Plugin, location: Location, task: Runnable) {
        // Paper doesn't have region-specific scheduling, use main thread
        runTask(plugin, task)
    }

    override fun runTaskForEntity(plugin: Plugin, entity: Entity, task: Runnable) {
        // Paper doesn't have entity-specific scheduling, use main thread
        runTask(plugin, task)
    }

    override fun getCoroutineScope(plugin: Plugin): CoroutineScope {
        return CoroutineScope(plugin.globalRegionDispatcher + SupervisorJob())
    }

    override fun isFolia(): Boolean = false
}

/**
 * Wrapper for BukkitTask
 */
class BukkitSchedulerTask(private val task: BukkitTask) : SchedulerTask {
    override fun cancel() {
        task.cancel()
    }
    override fun isCancelled() = task.isCancelled
}
