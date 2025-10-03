package com.github.azuazu3939.unique.nms

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

/**
 * Folia-specific scheduler implementation
 * Uses Folia's region-threaded scheduler
 */
class FoliaScheduler : PlatformScheduler {

    override fun runTask(plugin: Plugin, task: Runnable) {
        // Use global region for non-location-specific tasks
        plugin.server.globalRegionScheduler.run(plugin) { task.run() }
    }

    override fun runTaskAsynchronously(plugin: Plugin, task: Runnable) {
        plugin.server.asyncScheduler.runNow(plugin) { task.run() }
    }

    override fun runTaskLater(plugin: Plugin, task: Runnable, delay: Long) {
        plugin.server.globalRegionScheduler.runDelayed(plugin, { task.run() }, delay)
    }

    override fun runTaskTimer(plugin: Plugin, task: Runnable, delay: Long, period: Long): SchedulerTask {
        val foliaTask = plugin.server.globalRegionScheduler.runAtFixedRate(
            plugin,
            { task.run() },
            delay,
            period
        )
        return FoliaSchedulerTask(foliaTask)
    }

    override fun runTaskAtLocation(plugin: Plugin, location: Location, task: Runnable) {
        // Use region scheduler for location-specific tasks
        plugin.server.regionScheduler.run(plugin, location) { task.run() }
    }

    override fun runTaskForEntity(plugin: Plugin, entity: Entity, task: Runnable) {
        // Use entity scheduler for entity-specific tasks
        entity.scheduler.run(plugin, { task.run() }, null)
    }

    override fun getCoroutineScope(plugin: Plugin): CoroutineScope {
        return CoroutineScope(plugin.globalRegionDispatcher + SupervisorJob())
    }

    override fun isFolia(): Boolean = true
}

/**
 * Wrapper for Folia ScheduledTask
 */
class FoliaSchedulerTask(private val task: io.papermc.paper.threadedregions.scheduler.ScheduledTask) : SchedulerTask {
    override fun cancel() {
        task.cancel()
    }
    override fun isCancelled() = task.isCancelled
}
