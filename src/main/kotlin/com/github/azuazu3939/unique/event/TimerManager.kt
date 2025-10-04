package com.github.azuazu3939.unique.event

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.mob.UniqueMob
import com.github.azuazu3939.unique.nms.SchedulerTask
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages timer-based skill triggers
 * Runs at 1 tick (50ms) intervals and schedules skills based on their timer configuration
 */
class TimerManager(private val plugin: Unique) {

    private val platformScheduler = plugin.platformScheduler
    private var timerTask: SchedulerTask? = null

    // Map of mob UUID -> Map of skill ID -> (interval in ticks, last execution time)
    private val timerSkills = ConcurrentHashMap<String, MutableMap<String, TimerData>>()

    // Current tick count
    private var currentTick: Long = 0

    data class TimerData(
        val intervalTicks: Int,
        var lastExecutionTick: Long,
        var nextExecutionTick: Long
    )

    /**
     * Start the timer system (runs every 1 tick = 50ms)
     */
    fun start() {
        timerTask = platformScheduler.runTaskTimer(plugin, {
            tick()
        }, 1L, 1L) // Run every tick delay を1以上にしないといけない

        plugin.debugLogger.detailed("TimerManager started (1 tick interval)")
    }

    /**
     * Stop the timer system
     */
    fun stop() {
        timerTask?.cancel()
        timerTask = null
        timerSkills.clear()
        plugin.debugLogger.detailed("TimerManager stopped")
    }

    /**
     * Tick all timer-based skills
     */
    private fun tick() {
        currentTick++

        // Iterate through all registered timer skills
        timerSkills.forEach { (mobUuid, skills) ->
            val mob = plugin.mobManager.getMob(java.util.UUID.fromString(mobUuid))
            if (mob == null) {
                // Mob no longer exists, remove from tracking
                timerSkills.remove(mobUuid)
                return@forEach
            }

            // Check each skill's timer
            skills.forEach { (skillId, timerData) ->
                if (currentTick >= timerData.nextExecutionTick) {
                    // Time to execute this skill
                    executeTimerSkill(mob, skillId)

                    // Schedule next execution
                    timerData.lastExecutionTick = currentTick
                    timerData.nextExecutionTick = currentTick + timerData.intervalTicks
                }
            }
        }
    }

    /**
     * Execute a timer-based skill
     */
    private fun executeTimerSkill(mob: UniqueMob, skillId: String) {
        // Create timer trigger event
        val event = TriggerEvent(
            trigger = SkillTrigger.ON_TIMER,
            triggerEntity = null,
            target = null // Timer skills don't have a specific target
        )

        // Trigger the specific skill
        mob.onTriggerSpecificSkill(event, skillId)
    }

    /**
     * Register a timer skill for a mob
     */
    fun registerTimerSkill(mobUuid: String, skillId: String, intervalTicks: Int) {
        val mobSkills = timerSkills.getOrPut(mobUuid) { ConcurrentHashMap() }

        mobSkills[skillId] = TimerData(
            intervalTicks = intervalTicks,
            lastExecutionTick = currentTick,
            nextExecutionTick = currentTick + intervalTicks // First execution after interval
        )

        plugin.debugLogger.debug("Registered timer skill: mob=$mobUuid, skill=$skillId, interval=$intervalTicks ticks")
    }

    /**
     * Unregister a timer skill
     */
    fun unregisterTimerSkill(mobUuid: String, skillId: String) {
        timerSkills[mobUuid]?.remove(skillId)
        plugin.debugLogger.debug("Unregistered timer skill: mob=$mobUuid, skill=$skillId")
    }

    /**
     * Unregister all timer skills for a mob
     */
    fun unregisterMob(mobUuid: String) {
        timerSkills.remove(mobUuid)
        plugin.debugLogger.debug("Unregistered all timer skills for mob: $mobUuid")
    }

    /**
     * Get current tick count
     */
    fun getCurrentTick(): Long = currentTick
}
