package com.github.azuazu3939.unique.nms

import kotlinx.coroutines.CoroutineScope
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

/**
 * Platform-agnostic scheduler interface
 * Abstracts differences between Paper and Folia
 */
interface PlatformScheduler {

    /**
     * Run a task on the main thread
     */
    fun runTask(plugin: Plugin, task: Runnable)

    /**
     * Run a task asynchronously
     */
    fun runTaskAsynchronously(plugin: Plugin, task: Runnable)

    /**
     * Run a task on the main thread after a delay
     */
    fun runTaskLater(plugin: Plugin, task: Runnable, delay: Long)

    /**
     * Run a repeating task
     */
    fun runTaskTimer(plugin: Plugin, task: Runnable, delay: Long, period: Long): SchedulerTask

    /**
     * Run a task at a specific location (Folia region-aware)
     * On Paper, this is equivalent to runTask
     */
    fun runTaskAtLocation(plugin: Plugin, location: Location, task: Runnable)

    /**
     * Run a task for a specific entity (Folia entity-aware)
     * On Paper, this is equivalent to runTask
     */
    fun runTaskForEntity(plugin: Plugin, entity: Entity, task: Runnable)

    /**
     * Get coroutine scope for this platform
     */
    fun getCoroutineScope(plugin: Plugin): CoroutineScope

    /**
     * Check if running on Folia
     */
    fun isFolia(): Boolean
}

/**
 * Represents a scheduled task
 */
interface SchedulerTask {
    fun cancel()
    fun isCancelled(): Boolean
}
