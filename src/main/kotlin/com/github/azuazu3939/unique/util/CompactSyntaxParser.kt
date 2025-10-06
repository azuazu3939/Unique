package com.github.azuazu3939.unique.util

import com.github.azuazu3939.unique.mob.SkillMetaDefinition
import com.github.azuazu3939.unique.mob.SkillReference
import com.github.azuazu3939.unique.mob.SkillTrigger
import com.github.azuazu3939.unique.mob.TargeterDefinition

/**
 * 超コンパクト構文パーサー
 *
 * 例:
 * - projectile{...} @NearestPlayer{r=30.0} ~onTimer:30t
 * - FireballAttack @TL ~onTimer:30t
 * - damage{amount=22} @Self ~onDamaged
 */
object CompactSyntaxParser {

    /**
     * 超コンパクト構文からSkillTriggerを生成
     *
     * 構文: スキル定義 @ターゲッター ~トリガー
     *
     * @param line 1行の定義文字列
     * @return SkillTrigger、パースに失敗した場合null
     */
    fun parseSkillTrigger(line: String): SkillTrigger? {
        try {
            val trimmed = line.trim()

            // トリガー部分を抽出（~で始まる部分）
            val triggerMatch = Regex("~(\\w+)(?::(\\S+))?").find(trimmed)
            val triggerType = triggerMatch?.groupValues?.get(1)?.lowercase() ?: return null
            val triggerParam = triggerMatch.groupValues.getOrNull(2) // interval など

            // トリガー部分を除去
            val withoutTrigger = trimmed.take(triggerMatch.range.first).trim()

            // ターゲッター部分を抽出（@で始まる部分）
            val targeterMatch = Regex("@(\\w+)(?:\\{([^}]+)})?").find(withoutTrigger)
            val targeter = if (targeterMatch != null) {
                parseTargeter(targeterMatch.groupValues[1], targeterMatch.groupValues.getOrNull(2))
            } else {
                // デフォルトターゲッター
                TargeterDefinition(type = "Self")
            }

            // ターゲッター部分を除去してスキル定義を取得
            val skillDef = if (targeterMatch != null) {
                withoutTrigger.take(targeterMatch.range.first).trim()
            } else {
                withoutTrigger
            }

            // スキル定義をパース
            val skill = parseSkillDefinition(skillDef) ?: return null

            // トリガー名を生成
            val triggerName = "auto_${triggerType}_${System.nanoTime()}"

            // intervalをパース（onTimer用）
            val interval = if (triggerType == "ontimer" && triggerParam != null) {
                try {
                    val duration = TimeParser.parse(triggerParam)
                    TimeParser.millisToTicks(duration.inWholeMilliseconds).toInt()
                } catch (e: Exception) {
                    DebugLogger.warn("Failed to parse interval: $triggerParam")
                    null
                }
            } else null

            return SkillTrigger(
                name = triggerName,
                interval = interval,
                targeter = targeter,
                meta = SkillMetaDefinition(),
                skills = listOf(skill)
            )

        } catch (e: Exception) {
            DebugLogger.error("Failed to parse compact syntax: $line", e)
            return null
        }
    }

    /**
     * ターゲッターをパース
     *
     * @param type ターゲッタータイプ（省略形もサポート）
     * @param params パラメータ文字列（key=value;key=value...）
     * @return TargeterDefinition
     */
    fun parseTargeter(type: String, params: String?): TargeterDefinition {
        // 省略形を展開
        val fullType = expandTargeterType(type)

        // パラメータをパース
        val paramMap = if (params != null) {
            InlineSkillParser.parseArguments(params)
        } else {
            emptyMap()
        }

        return TargeterDefinition(
            type = fullType,
            range = paramMap["r"]?.toString()?.toDoubleOrNull()
                ?: paramMap["range"]?.toString()?.toDoubleOrNull()
                ?: 16.0,
            maxDistance = paramMap["maxDist"]?.toString()?.toDoubleOrNull()
                ?: paramMap["maxDistance"]?.toString()?.toDoubleOrNull()
                ?: 50.0,
            count = paramMap["c"]?.toString()?.toIntOrNull()
                ?: paramMap["count"]?.toString()?.toIntOrNull()
                ?: 1,
            condition = paramMap["cond"]?.toString()
                ?: paramMap["condition"]?.toString()
                ?: "true",
            filter = paramMap["filter"]?.toString(),

            // Area用
            shape = paramMap["shape"]?.toString(),
            radius = paramMap["radius"]?.toString(),
            innerRadius = paramMap["innerRadius"]?.toString(),
            angle = paramMap["angle"]?.toString(),
            width = paramMap["width"]?.toString(),
            height = paramMap["height"]?.toString(),
            depth = paramMap["depth"]?.toString(),
            direction = paramMap["dir"]?.toString() ?: paramMap["direction"]?.toString(),
            targetPlayers = paramMap["players"]?.toString()?.toBooleanStrictOrNull() ?: true,
            targetMobs = paramMap["mobs"]?.toString()?.toBooleanStrictOrNull() ?: true,

            // Chain用
            maxChains = paramMap["chains"]?.toString() ?: paramMap["maxChains"]?.toString(),
            chainRange = paramMap["chainRange"]?.toString(),
            chainCondition = paramMap["chainCond"]?.toString() ?: paramMap["chainCondition"]?.toString()
        )
    }

    /**
     * ターゲッタータイプの省略形を展開
     */
    private fun expandTargeterType(abbr: String): String {
        return when (abbr.uppercase()) {
            "TL" -> "TargetLocation"
            "NP" -> "NearestPlayer"
            "RP" -> "RadiusPlayers"
            "RND" -> "Random"
            "LH" -> "LowestHealth"
            "HH" -> "HighestHealth"
            "AREA" -> "Area"
            "CHAIN" -> "Chain"
            else -> abbr
        }
    }

    /**
     * スキル定義をパース
     *
     * インライン構文またはスキル名参照に対応
     */
    private fun parseSkillDefinition(def: String): SkillReference? {
        val trimmed = def.trim()

        return if (trimmed.contains("{")) {
            // インライン構文
            InlineSkillParser.parseSkill(trimmed)
        } else {
            // スキル名参照
            SkillReference(skill = trimmed)
        }
    }

    /**
     * トリガータイプを取得
     *
     * @param line コンパクト構文の行
     * @return トリガータイプ（ontimer, ondamaged, ondeath, onspawn, onattack）、見つからない場合null
     */
    fun extractTriggerType(line: String): String? {
        val triggerMatch = Regex("~(\\w+)").find(line)
        return triggerMatch?.groupValues?.get(1)?.lowercase()
    }
}
