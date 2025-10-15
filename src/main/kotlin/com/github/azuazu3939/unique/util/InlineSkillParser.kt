package com.github.azuazu3939.unique.util

import com.github.azuazu3939.unique.mob.EffectDefinition
import com.github.azuazu3939.unique.mob.SkillReference
import com.github.azuazu3939.unique.mob.TargeterDefinition

/**
 * インライン構文パーサー
 *
 * MythicMobs風のインライン構文をパースして、SkillReferenceやEffectDefinitionに変換します。
 *
 * 例:
 * - projectile{type=FIREBALL;speed=2.5;maxRange=40}
 * - damage{amount=22}
 * - explosion{amount=12;radius=4.0;kb=1.5;fire=true}
 */
object InlineSkillParser {

    /**
     * インライン構文からSkillReferenceを生成
     *
     * @param inline インライン構文文字列
     * @param targeter デフォルトのTargeter（省略時）
     * @return SkillReference、パースに失敗した場合null
     */
    fun parseSkill(inline: String, targeter: TargeterDefinition? = null): SkillReference? {
        val trimmed = inline.trim()

        // スキル名と引数部分を分離
        val nameEnd = trimmed.indexOf('{')
        if (nameEnd == -1) {
            // 引数なしのスキル参照（例: "fireball"）
            return SkillReference(skill = trimmed)
        }

        val skillName = trimmed.take(nameEnd).trim()
        val argsSection = trimmed.substring(nameEnd + 1, trimmed.lastIndexOf('}'))

        // 引数をパース
        val args = parseArguments(argsSection)

        return when (skillName.lowercase()) {
            "projectile" -> parseProjectileSkill(args, skillName)
            "beam" -> parseBeamSkill(args, skillName)
            "aura" -> parseAuraSkill(args, skillName)
            "throw" -> parseThrowSkill(args, skillName)
            "damage", "explosion", "lightning", "particle", "sound", "potion",
            "heal", "freeze", "shield", "teleport", "pull", "push", "blind" -> {
                // これはEffectなので、Basicスキルとして扱う
                val effect = parseEffect(skillName, args)
                effect?.let {
                    SkillReference(
                        skill = "inline_${skillName}_${System.nanoTime()}",
                        type = "basic",
                        effects = listOf(it)
                    )
                }
            }
            else -> {
                DebugLogger.warn("Unknown inline skill type: $skillName")
                null
            }
        }
    }

    /**
     * インライン構文からEffectDefinitionを生成
     */
    fun parseEffect(effectType: String, args: Map<String, Any>): EffectDefinition? {
        return try {
            when (effectType.lowercase()) {
                "damage" -> EffectDefinition(
                    type = "Damage",
                    damageAmount = args["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                )
                "explosion" -> EffectDefinition(
                    type = "Explosion",
                    explosionDamage = args["amount"]?.toString()?.toDoubleOrNull() ?: args["damage"]?.toString()?.toDoubleOrNull(),
                    explosionRadius = args["radius"]?.toString()?.toDoubleOrNull(),
                    explosionKnockback = args["kb"]?.toString()?.toDoubleOrNull() ?: args["knockback"]?.toString()?.toDoubleOrNull(),
                    explosionSetFire = args["fire"]?.toString()?.toBooleanStrictOrNull() ?: false,
                    explosionBreakBlocks = args["breakBlocks"]?.toString()?.toBooleanStrictOrNull() ?: false
                )
                "lightning" -> EffectDefinition(
                    type = "Lightning",
                    lightningDamage = args["damage"]?.toString()?.toDoubleOrNull() ?: args["amount"]?.toString()?.toDoubleOrNull(),
                    lightningSetFire = args["fire"]?.toString()?.toBooleanStrictOrNull() ?: false,
                    lightningVisualOnly = args["visualOnly"]?.toString()?.toBooleanStrictOrNull() ?: false
                )
                "particle" -> EffectDefinition(
                    type = "Particle",
                    particle = args["type"]?.toString() ?: "FLAME",
                    particleCount = args["amount"]?.toString()?.toIntOrNull() ?: args["count"]?.toString()?.toIntOrNull() ?: 10,
                    offsetX = args["offsetX"]?.toString()?.toDoubleOrNull() ?: 0.5,
                    offsetY = args["offsetY"]?.toString()?.toDoubleOrNull() ?: 0.5,
                    offsetZ = args["offsetZ"]?.toString()?.toDoubleOrNull() ?: 0.5
                )
                "sound" -> EffectDefinition(
                    type = "Sound",
                    sound = args["type"]?.toString() ?: "ENTITY_GENERIC_EXPLODE",
                    soundVolume = args["volume"]?.toString()?.toFloatOrNull() ?: 1.0f,
                    soundPitch = args["pitch"]?.toString()?.toFloatOrNull() ?: 1.0f
                )
                "potion" -> EffectDefinition(
                    type = "Potion",
                    potionType = args["type"]?.toString() ?: "SPEED",
                    potionDuration = args["duration"]?.toString() ?: "5s",
                    potionAmplifier = args["amplifier"]?.toString()?.toIntOrNull() ?: 0
                )
                "heal" -> EffectDefinition(
                    type = "Heal",
                    healAmount = args["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                )
                "freeze" -> EffectDefinition(
                    type = "Freeze",
                    freezeDuration = args["duration"]?.toString() ?: "3s",
                    freezeAmplifier = args["amplifier"]?.toString()?.toIntOrNull() ?: 0
                )
                "shield" -> EffectDefinition(
                    type = "Shield",
                    shieldAmount = args["amount"]?.toString()?.toDoubleOrNull() ?: 0.0,
                    shieldDuration = args["duration"]?.toString() ?: "10s"
                )
                "teleport" -> EffectDefinition(
                    type = "Teleport",
                    teleportMode = args["mode"]?.toString() ?: "TO_TARGET",
                    teleportRange = args["range"]?.toString()?.toDoubleOrNull()
                )
                "pull" -> EffectDefinition(
                    type = "Pull",
                    pullStrength = args["strength"]?.toString()?.toDoubleOrNull() ?: 1.0,
                    radius = args["radius"]?.toString()?.toDoubleOrNull()
                )
                "push" -> EffectDefinition(
                    type = "Push",
                    pushStrength = args["strength"]?.toString()?.toDoubleOrNull() ?: 1.0,
                    radius = args["radius"]?.toString()?.toDoubleOrNull()
                )
                "blind" -> EffectDefinition(
                    type = "Blind",
                    blindDuration = args["duration"]?.toString() ?: "3s",
                    blindAmplifier = args["amplifier"]?.toString()?.toIntOrNull() ?: 0
                )
                else -> null
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to parse effect: $effectType", e)
            null
        }
    }

    /**
     * Projectileスキルをパース
     */
    private fun parseProjectileSkill(args: Map<String, Any>, skillName: String): SkillReference {
        val hitEffects = parseEffectList(args["onHit"])
        val tickEffects = parseEffectList(args["onTick"])

        return SkillReference(
            skill = args["name"]?.toString() ?: "inline_projectile_${System.nanoTime()}",
            type = "projectile",
            projectileType = args["type"]?.toString() ?: "FIREBALL",
            speed = args["speed"]?.toString() ?: "2.0",
            gravity = args["gravity"]?.toString()?.toBooleanStrictOrNull() ?: false,
            maxDistance = args["maxRange"]?.toString() ?: args["maxDistance"]?.toString() ?: "50.0",
            hitRadius = args["hr"]?.toString() ?: args["hitRadius"]?.toString() ?: "1.0",
            homing = args["hom"]?.toString() ?: args["homing"]?.toString() ?: "0.0",
            pierce = args["pierce"]?.toString()?.toBooleanStrictOrNull() ?: false,
            hitEffects = hitEffects,
            tickEffects = tickEffects,
            hitSound = args["hitSound"]?.toString(),
            launchSound = args["launchSound"]?.toString()
        )
    }

    /**
     * Beamスキルをパース
     */
    private fun parseBeamSkill(args: Map<String, Any>, skillName: String): SkillReference {
        val effects = parseEffectList(args["effects"])

        return SkillReference(
            skill = args["name"]?.toString() ?: "inline_beam_${System.nanoTime()}",
            type = "beam",
            beamRange = args["range"]?.toString() ?: "20.0",
            beamWidth = args["width"]?.toString() ?: "0.5",
            beamParticle = args["particle"]?.toString() ?: "FLAME",
            beamParticleDensity = args["density"]?.toString()?.toDoubleOrNull() ?: 0.3,
            beamDuration = args["duration"]?.toString() ?: "1000",
            beamTickInterval = args["tickInterval"]?.toString() ?: "50",
            beamPiercing = args["piercing"]?.toString()?.toBooleanStrictOrNull() ?: true,
            effects = effects,
            beamFireSound = args["fireSound"]?.toString()
        )
    }

    /**
     * Auraスキルをパース
     */
    private fun parseAuraSkill(args: Map<String, Any>, skillName: String): SkillReference {
        val effects = parseEffectList(args["effects"])

        return SkillReference(
            skill = args["name"]?.toString() ?: "inline_aura_${System.nanoTime()}",
            type = "aura",
            auraRadius = args["radius"]?.toString() ?: "5.0",
            auraDuration = args["duration"]?.toString() ?: "10000",
            auraTickInterval = args["tickInterval"]?.toString() ?: "1000",
            auraParticle = args["particle"]?.toString() ?: "ENCHANT",
            auraParticleCount = args["particleCount"]?.toString()?.toIntOrNull() ?: 10,
            auraParticleSpeed = args["particleSpeed"]?.toString()?.toDoubleOrNull() ?: 0.1,
            auraSelfAffect = args["selfAffect"]?.toString()?.toBooleanStrictOrNull() ?: false,
            auraMaxTargets = args["maxTargets"]?.toString()?.toIntOrNull() ?: 0,
            effects = effects,
            auraStartSound = args["startSound"]?.toString(),
            auraTickSound = args["tickSound"]?.toString(),
            auraEndSound = args["endSound"]?.toString()
        )
    }

    /**
     * Throwスキルをパース
     */
    private fun parseThrowSkill(args: Map<String, Any>, skillName: String): SkillReference {
        return SkillReference(
            skill = args["name"]?.toString() ?: "inline_throw_${System.nanoTime()}",
            type = "throw",
            throwVelocity = args["velocity"]?.toString() ?: "1.0",
            throwVelocityY = args["velocityY"]?.toString() ?: "1.0",
            throwMode = args["mode"]?.toString() ?: "UP"
        )
    }

    /**
     * エフェクトリストをパース
     *
     * onHit=[damage{amount=22}, explosion{...}] のような配列形式をパース
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseEffectList(value: Any?): List<EffectDefinition> {
        if (value == null) return emptyList()

        when (value) {
            is List<*> -> {
                return value.mapNotNull { item ->
                    when (item) {
                        is String -> parseInlineEffect(item)
                        is Map<*, *> -> parseEffectFromMap(item as Map<String, Any>)
                        else -> null
                    }
                }
            }
            is String -> {
                // 配列形式の文字列をパース: "[damage{amount=22}, explosion{...}]"
                val trimmed = value.trim().removeSurrounding("[", "]")
                if (trimmed.isEmpty()) return emptyList()

                // カンマで分割（ただしネストした{}内のカンマは無視）
                val effects = mutableListOf<EffectDefinition>()
                var depth = 0
                val currentEffect = StringBuilder()

                for (char in trimmed) {
                    when (char) {
                        '{' -> {
                            depth++
                            currentEffect.append(char)
                        }
                        '}' -> {
                            depth--
                            currentEffect.append(char)
                        }
                        ',' -> {
                            if (depth == 0) {
                                val effectStr = currentEffect.toString().trim()
                                if (effectStr.isNotEmpty()) {
                                    parseInlineEffect(effectStr)?.let { effects.add(it) }
                                }
                                currentEffect.clear()
                            } else {
                                currentEffect.append(char)
                            }
                        }
                        else -> currentEffect.append(char)
                    }
                }

                // 最後のエフェクト
                val effectStr = currentEffect.toString().trim()
                if (effectStr.isNotEmpty()) {
                    parseInlineEffect(effectStr)?.let { effects.add(it) }
                }

                return effects
            }
            else -> return emptyList()
        }
    }

    /**
     * インライン形式のエフェクトをパース
     *
     * 例: "damage{amount=22}" -> EffectDefinition
     */
    private fun parseInlineEffect(inline: String): EffectDefinition? {
        val trimmed = inline.trim().removePrefix("-").trim()

        val nameEnd = trimmed.indexOf('{')
        if (nameEnd == -1) {
            // 引数なしのエフェクト
            return parseEffect(trimmed, emptyMap())
        }

        val effectName = trimmed.take(nameEnd).trim()
        val argsSection = trimmed.substring(nameEnd + 1, trimmed.lastIndexOf('}'))
        val args = parseArguments(argsSection)

        return parseEffect(effectName, args)
    }

    /**
     * MapからEffectDefinitionを生成
     */
    private fun parseEffectFromMap(map: Map<String, Any>): EffectDefinition? {
        val type = map["type"]?.toString() ?: return null
        return parseEffect(type, map)
    }

    /**
     * 引数文字列をパースしてMapに変換
     *
     * 例: "type=FIREBALL;speed=2.5;onHit=[damage{amount=22}]"
     *     -> {"type": "FIREBALL", "speed": "2.5", "onHit": [EffectDefinition]}
     */
    fun parseArguments(argsStr: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        if (argsStr.isBlank()) return result

        var depth = 0
        val currentKey = StringBuilder()
        val currentValue = StringBuilder()
        var parsingValue = false

        for (char in argsStr) {
            when (char) {
                '=' if depth == 0 -> {
                    parsingValue = true
                }
                ';' if depth == 0 -> {
                    if (currentKey.isNotEmpty() && currentValue.isNotEmpty()) {
                        val key = currentKey.toString().trim()
                        val value = parseValue(currentValue.toString().trim())
                        result[key] = value
                    }
                    currentKey.clear()
                    currentValue.clear()
                    parsingValue = false
                }
                '{', '[' -> {
                    depth++
                    if (parsingValue) currentValue.append(char)
                    else currentKey.append(char)
                }
                '}', ']' -> {
                    depth--
                    if (parsingValue) currentValue.append(char)
                    else currentKey.append(char)
                }
                else -> {
                    if (parsingValue) currentValue.append(char)
                    else currentKey.append(char)
                }
            }
        }

        // 最後のキーバリューペア
        if (currentKey.isNotEmpty() && currentValue.isNotEmpty()) {
            val key = currentKey.toString().trim()
            val value = parseValue(currentValue.toString().trim())
            result[key] = value
        }

        return result
    }

    /**
     * 値をパースして適切な型に変換
     */
    private fun parseValue(valueStr: String): Any {
        return when {
            valueStr.equals("true", ignoreCase = true) -> true
            valueStr.equals("false", ignoreCase = true) -> false
            valueStr.startsWith("[") && valueStr.endsWith("]") -> {
                // 配列の場合はそのまま文字列として返す（後でparseEffectListで処理）
                valueStr
            }
            valueStr.toDoubleOrNull() != null -> valueStr.toDouble()
            valueStr.toIntOrNull() != null -> valueStr.toInt()
            else -> valueStr
        }
    }
}
