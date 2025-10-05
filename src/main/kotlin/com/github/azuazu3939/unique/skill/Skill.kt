package com.github.azuazu3939.unique.skill

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * スキルメタ設定
 */
data class SkillMeta(
    val sync: Boolean = false,
    val executeDelay: Duration = 0.milliseconds,
    val effectDelay: Duration = 0.milliseconds,
    val cancelOnDeath: Boolean = true,
    val interruptible: Boolean = false,
    val duration: Duration? = null
)

/**
 * スキル基底クラス
 */
abstract class Skill(
    val id: String,
    val meta: SkillMeta = SkillMeta(),
    val condition: Condition? = null
) {

    /**
     * スキルを実行
     *
     * @param plugin プラグインインスタンス
     * @param source ソースエンティティ
     * @param targeter ターゲッター
     */
    abstract suspend fun execute(
        plugin: Plugin,
        source: Entity,
        targeter: Targeter
    )

    /**
     * スキルを実行（PacketEntity）
     *
     * @param plugin プラグインインスタンス
     * @param source ソースPacketEntity
     * @param targeter ターゲッター
     */
    abstract suspend fun execute(
        plugin: Plugin,
        source: PacketEntity,
        targeter: Targeter
    )

    /**
     * 条件チェック
     */
    protected fun checkCondition(source: Entity): Boolean {
        if (condition == null) return true
        return condition.evaluate(source)
    }

    /**
     * 条件チェック（PacketEntity）
     */
    protected fun checkCondition(source: PacketEntity): Boolean {
        if (condition == null) return true
        return condition.evaluate(source)
    }

    /**
     * スキルの説明
     */
    open fun getDescription(): String {
        return "Skill: $id"
    }

    /**
     * デバッグ情報
     */
    open fun debugInfo(): String {
        return "Skill[id=$id, meta=$meta, condition=${condition?.debugInfo() ?: "none"}]"
    }
}

/**
 * 基本スキル
 * エフェクトを順次適用
 */
class BasicSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val effects: List<Effect>
) : Skill(id, meta, condition) {

    override suspend fun execute(
        plugin: Plugin,
        source: Entity,
        targeter: Targeter
    ) {
        val startTime = System.currentTimeMillis()

        // 条件チェック
        if (!checkCondition(source)) {
            DebugLogger.debug("Skill $id failed condition check")
            return
        }

        // executeDelay
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // 死亡チェック
        if (meta.cancelOnDeath && source.isDead) {
            DebugLogger.debug("Skill $id cancelled (source is dead)")
            return
        }

        // ターゲット取得
        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.debug("Skill $id has no targets")
            return
        }

        // エフェクト適用
        for (effect in effects) {
            // effectDelay
            if (meta.effectDelay.inWholeMilliseconds > 0) {
                delay(meta.effectDelay.inWholeMilliseconds)
            }

            // 死亡チェック
            if (meta.cancelOnDeath && source.isDead) {
                DebugLogger.debug("Skill $id cancelled (source died during execution)")
                return
            }

            // エフェクト適用（sync設定に応じて切り替え）
            for (target in targets) {
                if (effect.sync || meta.sync) {
                    // 同期実行
                    withContext(plugin.globalRegionDispatcher) {
                        effect.apply(source, target)
                    }
                } else {
                    // 非同期実行
                    effect.apply(source, target)
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.skillExecution(id, "completed", duration)
    }

    override suspend fun execute(
        plugin: Plugin,
        source: PacketEntity,
        targeter: Targeter
    ) {
        val startTime = System.currentTimeMillis()

        // 条件チェック
        if (!checkCondition(source)) {
            DebugLogger.debug("Skill $id (PacketEntity) failed condition check")
            return
        }

        // executeDelay
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // 死亡チェック
        if (meta.cancelOnDeath && source.isDead) {
            DebugLogger.debug("Skill $id (PacketEntity) cancelled (source is dead)")
            return
        }

        // ターゲット取得
        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.debug("Skill $id (PacketEntity) has no targets")
            return
        }

        // エフェクト適用
        for (effect in effects) {
            // effectDelay
            if (meta.effectDelay.inWholeMilliseconds > 0) {
                delay(meta.effectDelay.inWholeMilliseconds)
            }

            // 死亡チェック
            if (meta.cancelOnDeath && source.isDead) {
                DebugLogger.debug("Skill $id (PacketEntity) cancelled (source died during execution)")
                return
            }

            // エフェクト適用（sync設定に応じて切り替え）
            for (target in targets) {
                if (effect.sync || meta.sync) {
                    // 同期実行
                    withContext(plugin.globalRegionDispatcher) {
                        effect.apply(source, target)
                    }
                } else {
                    // 非同期実行
                    effect.apply(source, target)
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.skillExecution(id, "completed (PacketEntity)", duration)
    }

    override fun getDescription(): String {
        return "Basic Skill with ${effects.size} effects"
    }
}

/**
 * メタスキル
 * 複数のスキルを組み合わせて実行
 */
class MetaSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val phases: List<SkillPhase>
) : Skill(id, meta, condition) {

    override suspend fun execute(
        plugin: Plugin,
        source: Entity,
        targeter: Targeter
    ) {
        val startTime = System.currentTimeMillis()

        // 条件チェック
        if (!checkCondition(source)) {
            DebugLogger.debug("MetaSkill $id failed condition check")
            return
        }

        // executeDelay
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // 各フェーズを実行
        for (phase in phases) {
            // フェーズの遅延
            if (phase.meta.executeDelay.inWholeMilliseconds > 0) {
                delay(phase.meta.executeDelay.inWholeMilliseconds)
            }

            // 死亡チェック
            if (meta.cancelOnDeath && source.isDead) {
                DebugLogger.debug("MetaSkill $id cancelled (source died)")
                return
            }

            // フェーズ実行
            if (phase.meta.parallel) {
                // 並列実行
                executePhaseParallel(plugin, source, phase)
            } else {
                // 順次実行
                executePhaseSequential(plugin, source, phase)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.skillExecution(id, "completed", duration)
    }

    override suspend fun execute(
        plugin: Plugin,
        source: PacketEntity,
        targeter: Targeter
    ) {
        val startTime = System.currentTimeMillis()

        // 条件チェック
        if (!checkCondition(source)) {
            DebugLogger.debug("MetaSkill $id (PacketEntity) failed condition check")
            return
        }

        // executeDelay
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // 各フェーズを実行
        for (phase in phases) {
            // フェーズの遅延
            if (phase.meta.executeDelay.inWholeMilliseconds > 0) {
                delay(phase.meta.executeDelay.inWholeMilliseconds)
            }

            // 死亡チェック
            if (meta.cancelOnDeath && source.isDead) {
                DebugLogger.debug("MetaSkill $id (PacketEntity) cancelled (source died)")
                return
            }

            // フェーズ実行
            if (phase.meta.parallel) {
                // 並列実行
                executePhaseParallel(plugin, source, phase)
            } else {
                // 順次実行
                executePhaseSequential(plugin, source, phase)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.skillExecution(id, "completed (PacketEntity)", duration)
    }

    /**
     * フェーズを順次実行
     */
    private suspend fun executePhaseSequential(
        plugin: Plugin,
        source: Entity,
        phase: SkillPhase
    ) {
        for (skill in phase.skills) {
            skill.execute(plugin, source, phase.targeter)
        }
    }

    /**
     * フェーズを順次実行（PacketEntity）
     */
    private suspend fun executePhaseSequential(
        plugin: Plugin,
        source: PacketEntity,
        phase: SkillPhase
    ) {
        for (skill in phase.skills) {
            skill.execute(plugin, source, phase.targeter)
        }
    }

    /**
     * フェーズを並列実行
     */
    private suspend fun executePhaseParallel(
        plugin: Plugin,
        source: Entity,
        phase: SkillPhase
    ) {
        val jobs = phase.skills.map { skill ->
            plugin.launch {
                skill.execute(plugin, source, phase.targeter)
            }
        }

        // すべてのジョブが完了するまで待機
        jobs.joinAll()
    }

    /**
     * フェーズを並列実行（PacketEntity）
     */
    private suspend fun executePhaseParallel(
        plugin: Plugin,
        source: PacketEntity,
        phase: SkillPhase
    ) {
        val jobs = phase.skills.map { skill ->
            plugin.launch {
                skill.execute(plugin, source, phase.targeter)
            }
        }

        // すべてのジョブが完了するまで待機
        jobs.joinAll()
    }

    override fun getDescription(): String {
        return "Meta Skill with ${phases.size} phases"
    }
}

/**
 * スキルフェーズ
 * メタスキル内の1つのフェーズ
 */
data class SkillPhase(
    val name: String,
    val targeter: Targeter,
    val skills: List<Skill>,
    val meta: PhaseMeta = PhaseMeta()
)

/**
 * フェーズメタ設定
 */
data class PhaseMeta(
    val parallel: Boolean = false,
    val executeDelay: Duration = 0.milliseconds,
    val sync: Boolean = false
)

/**
 * スキル分岐
 *
 * CEL式による条件分岐でスキルを実行
 */
data class SkillBranch(
    val condition: String?,  // CEL式（nullの場合はdefault）
    val skills: List<Skill>,
    val targeter: Targeter,
    val isDefault: Boolean = false
)

/**
 * 分岐スキル
 *
 * 条件に応じて異なるスキルを実行
 * 最初にマッチした分岐のみ実行される
 */
class BranchSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val branches: List<SkillBranch>
) : Skill(id, meta, condition) {

    override suspend fun execute(
        plugin: Plugin,
        source: Entity,
        targeter: Targeter
    ) {
        val startTime = System.currentTimeMillis()

        // 条件チェック
        if (!checkCondition(source)) {
            DebugLogger.debug("BranchSkill $id failed condition check")
            return
        }

        // executeDelay
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // 死亡チェック
        if (meta.cancelOnDeath && source.isDead) {
            DebugLogger.debug("BranchSkill $id cancelled (source is dead)")
            return
        }

        // CEL評価器とコンテキストを準備
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildEntityContext(source)

        // 最初にマッチした分岐を実行
        for (branch in branches) {
            val shouldExecute = if (branch.isDefault) {
                true
            } else if (branch.condition != null) {
                try {
                    evaluator.evaluateBoolean(branch.condition, context)
                } catch (e: Exception) {
                    DebugLogger.error("Branch condition evaluation failed: ${branch.condition}", e)
                    false
                }
            } else {
                false
            }

            if (shouldExecute) {
                DebugLogger.debug("BranchSkill $id executing branch: ${branch.condition ?: "default"}")

                // 分岐のスキルを実行
                for (skill in branch.skills) {
                    skill.execute(plugin, source, branch.targeter)
                }

                // 最初にマッチした分岐のみ実行
                break
            }
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.skillExecution(id, "completed", duration)
    }

    override suspend fun execute(
        plugin: Plugin,
        source: PacketEntity,
        targeter: Targeter
    ) {
        val startTime = System.currentTimeMillis()

        // 条件チェック
        if (!checkCondition(source)) {
            DebugLogger.debug("BranchSkill $id (PacketEntity) failed condition check")
            return
        }

        // executeDelay
        if (meta.executeDelay.inWholeMilliseconds > 0) {
            delay(meta.executeDelay.inWholeMilliseconds)
        }

        // 死亡チェック
        if (meta.cancelOnDeath && source.isDead) {
            DebugLogger.debug("BranchSkill $id (PacketEntity) cancelled (source is dead)")
            return
        }

        // CEL評価器とコンテキストを準備
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildPacketEntityContext(source)

        // 最初にマッチした分岐を実行
        for (branch in branches) {
            val shouldExecute = if (branch.isDefault) {
                true
            } else if (branch.condition != null) {
                try {
                    evaluator.evaluateBoolean(branch.condition, context)
                } catch (e: Exception) {
                    DebugLogger.error("Branch condition evaluation failed: ${branch.condition}", e)
                    false
                }
            } else {
                false
            }

            if (shouldExecute) {
                DebugLogger.debug("BranchSkill $id (PacketEntity) executing branch: ${branch.condition ?: "default"}")

                // 分岐のスキルを実行
                for (skill in branch.skills) {
                    skill.execute(plugin, source, branch.targeter)
                }

                // 最初にマッチした分岐のみ実行
                break
            }
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.skillExecution(id, "completed (PacketEntity)", duration)
    }

    override fun getDescription(): String {
        return "Branch Skill with ${branches.size} branches"
    }

    override fun debugInfo(): String {
        val branchInfo = branches.joinToString(", ") {
            it.condition ?: "default"
        }
        return "BranchSkill[id=$id, branches=[$branchInfo]]"
    }
}