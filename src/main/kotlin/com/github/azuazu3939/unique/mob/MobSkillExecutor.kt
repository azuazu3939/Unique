package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.event.PacketMobSkillEvent
import com.github.azuazu3939.unique.skill.BasicSkill
import com.github.azuazu3939.unique.skill.SkillFactory
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.EventUtil
import com.github.azuazu3939.unique.util.InlineSkillParser
import org.bukkit.entity.Entity

/**
 * Mobスキルの実行管理
 */
class MobSkillExecutor(private val plugin: Unique) {

    /**
     * スキルトリガーを実行
     */
    fun executeSkillTriggers(
        mob: PacketMob,
        triggers: List<SkillTrigger>,
        triggerType: PacketMobSkillEvent.SkillTriggerType,
        skillLibrary: Map<String, List<String>>
    ) {
        if (triggers.isEmpty()) return

        triggers.forEach { trigger ->
            try {
                // スキルイベント発火＆キャンセルチェック
                if (EventUtil.callEvent(PacketMobSkillEvent(mob, trigger.name, triggerType))) {
                    DebugLogger.debug("Skill ${trigger.name} cancelled by event")
                    return@forEach
                }

                // 条件チェック（onDamagedの場合はdamager情報も含める）
                val conditionMet =
                    if (triggerType == PacketMobSkillEvent.SkillTriggerType.ON_DAMAGED && mob.combat.lastDamager != null) {
                        evaluateCondition(trigger.condition, mob, mob.combat.lastDamager)
                    } else {
                        evaluateCondition(trigger.condition, mob)
                    }

                if (!conditionMet) {
                    DebugLogger.debug("Skill trigger ${trigger.name} condition not met")
                    return@forEach
                }

                // スキル実行
                executeSkillsForTrigger(mob, trigger, skillLibrary)
                DebugLogger.debug("Executed skill trigger: ${trigger.name}")

            } catch (e: Exception) {
                DebugLogger.error("Failed to execute skill trigger: ${trigger.name}", e)
            }
        }
    }

    /**
     * トリガーに対してスキルを実行
     */
    private fun executeSkillsForTrigger(
        mob: PacketMob,
        trigger: SkillTrigger,
        skillLibrary: Map<String, List<String>>
    ) {
        val targeter = MobBuilder.buildTargeter(trigger.targeter)

        trigger.skills.forEach { skillRef ->
            // スキル名参照の場合、スキルライブラリから定義を読み込んで展開
            if (skillRef.effects.isEmpty() && skillLibrary.containsKey(skillRef.skill)) {
                // スキルライブラリから定義を取得
                val inlineList = skillLibrary[skillRef.skill] ?: emptyList()
                DebugLogger.debug("Resolving skill '${skillRef.skill}' from library (${inlineList.size} inline definitions)")

                // 各インライン定義をSkillとして実行
                for (inlineStr in inlineList) {
                    val parsedSkillRef = InlineSkillParser.parseSkill(inlineStr)
                    if (parsedSkillRef != null) {
                        // SkillFactoryでSkillインスタンスを生成
                        val skill = SkillFactory.createSkill(parsedSkillRef)
                        if (skill != null) {
                            plugin.skillExecutor.executeSkill(skill, mob, targeter)
                        } else {
                            DebugLogger.warn("Failed to create skill from: $inlineStr")
                        }
                    }
                }
            } else {
                // 直接定義されているスキル/エフェクトを使用
                val skill = BasicSkill(
                    id = skillRef.skill,
                    meta = MobBuilder.buildSkillMeta(skillRef.meta),
                    effects = MobBuilder.buildEffects(plugin, skillRef.effects)
                )
                plugin.skillExecutor.executeSkill(skill, mob, targeter)
            }
        }
    }

    /**
     * 条件を評価（攻撃者情報なし）
     */
    private fun evaluateCondition(condition: String, mob: PacketMob): Boolean {
        return condition == "true" || plugin.celEvaluator.evaluatePacketEntityCondition(condition, mob)
    }

    /**
     * 条件を評価（攻撃者情報あり）
     */
    private fun evaluateCondition(condition: String, mob: PacketMob, damager: Entity?): Boolean {
        if (condition == "true") return true
        if (damager == null) return plugin.celEvaluator.evaluatePacketEntityCondition(condition, mob)

        return plugin.celEvaluator.evaluatePacketEntityTargetCondition(condition, mob, damager)
    }
}
