package com.github.azuazu3939.unique.skill

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.Job
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * スキル実行エンジン
 *
 * スキルの実行とライフサイクル管理
 */
class SkillExecutor(private val plugin: Plugin) {

    /**
     * アクティブなスキルジョブ
     */
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    /**
     * ジョブIDカウンター
     */
    private val jobIdCounter = AtomicLong(0)

    /**
     * 実行中のスキル数
     */
    private val executionCount = AtomicLong(0)

    /**
     * スキルを実行
     *
     * @param skill スキル
     * @param source ソースエンティティ
     * @param targeter ターゲッター
     * @return ジョブID
     */
    fun executeSkill(
        skill: Skill,
        source: Entity,
        targeter: Targeter
    ): Long {
        val jobId = jobIdCounter.incrementAndGet()

        // 最大同時実行数チェック
        val maxConcurrent = Unique.instance.configManager.mainConfig.performance.maxConcurrentSkills
        if (executionCount.get() >= maxConcurrent) {
            DebugLogger.warn("Max concurrent skills reached ($maxConcurrent), skill $skill.id queued")
        }

        val job = plugin.launch {
            try {
                executionCount.incrementAndGet()
                DebugLogger.debug("Executing skill: ${skill.id} (job $jobId)")

                skill.execute(plugin, source, targeter)

            } catch (e: Exception) {
                DebugLogger.error("Error executing skill ${skill.id}", e)
            } finally {
                executionCount.decrementAndGet()
                activeJobs.remove(jobId)
            }
        }

        activeJobs[jobId] = job
        return jobId
    }

    /**
     * スキルを実行（PacketEntity）
     *
     * @param skill スキル
     * @param source ソースPacketEntity
     * @param targeter ターゲッター
     * @return ジョブID
     */
    fun executeSkill(
        skill: Skill,
        source: PacketEntity,
        targeter: Targeter
    ): Long {
        val jobId = jobIdCounter.incrementAndGet()

        // 最大同時実行数チェック
        val maxConcurrent = Unique.instance.configManager.mainConfig.performance.maxConcurrentSkills
        if (executionCount.get() >= maxConcurrent) {
            DebugLogger.warn("Max concurrent skills reached ($maxConcurrent), skill $skill.id queued")
        }

        val job = plugin.launch {
            try {
                executionCount.incrementAndGet()
                DebugLogger.debug("Executing skill: ${skill.id} from PacketEntity (job $jobId)")

                skill.execute(plugin, source, targeter)

            } catch (e: Exception) {
                DebugLogger.error("Error executing skill ${skill.id} from PacketEntity", e)
            } finally {
                executionCount.decrementAndGet()
                activeJobs.remove(jobId)
            }
        }

        activeJobs[jobId] = job
        return jobId
    }

    /**
     * スキルをキャンセル
     *
     * @param jobId ジョブID
     */
    fun cancelSkill(jobId: Long) {
        val job = activeJobs.remove(jobId)
        if (job != null) {
            job.cancel()
            DebugLogger.debug("Cancelled skill job $jobId")
        }
    }

    /**
     * エンティティに関連するすべてのスキルをキャンセル
     *
     * @param entity エンティティ
     */
    fun cancelSkillsForEntity(entity: Entity) {
        val jobs = activeJobs.values.toList()
        jobs.forEach { it.cancel() }
        activeJobs.clear()

        DebugLogger.debug("Cancelled all skills for entity ${entity.uniqueId}")
    }

    /**
     * PacketEntityに関連するすべてのスキルをキャンセル
     *
     * @param packetEntity PacketEntity
     */
    fun cancelSkillsForPacketEntity(packetEntity: PacketEntity) {
        val jobs = activeJobs.values.toList()
        jobs.forEach { it.cancel() }
        activeJobs.clear()

        DebugLogger.debug("Cancelled all skills for PacketEntity ${packetEntity.uuid}")
    }

    /**
     * すべてのスキルをキャンセル
     */
    fun cancelAllSkills() {
        val count = activeJobs.size
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()

        DebugLogger.info("Cancelled all skills ($count jobs)")
    }

    /**
     * アクティブなジョブ数を取得
     */
    fun getActiveJobCount(): Int {
        return activeJobs.size
    }

    /**
     * 実行中のスキル数を取得
     */
    fun getExecutionCount(): Long {
        return executionCount.get()
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("SkillExecutor Debug Info")
        DebugLogger.info("Active jobs: ${activeJobs.size}")
        DebugLogger.info("Executing skills: ${executionCount.get()}")
        DebugLogger.info("Total jobs created: ${jobIdCounter.get()}")
        DebugLogger.separator()
    }
}