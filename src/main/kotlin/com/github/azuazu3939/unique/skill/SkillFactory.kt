package com.github.azuazu3939.unique.skill

import com.github.azuazu3939.unique.effect.EffectFactory
import com.github.azuazu3939.unique.mob.SkillPhaseDefinition
import com.github.azuazu3939.unique.mob.SkillReference
import com.github.azuazu3939.unique.skill.types.AuraSkill
import com.github.azuazu3939.unique.skill.types.BeamSkill
import com.github.azuazu3939.unique.skill.types.ProjectileSkill
import com.github.azuazu3939.unique.targeter.TargeterFactory
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.ResourceKeyResolver
import com.github.azuazu3939.unique.util.getSound
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import kotlin.time.Duration.Companion.milliseconds

/**
 * SkillFactory - Skill定義からSkillインスタンスを生成
 *
 * YAMLから読み込まれたSkillReferenceを実際のSkillオブジェクトに変換します。
 */
object SkillFactory {

    /**
     * Skill定義からSkillインスタンスを生成
     *
     * @param reference Skill参照
     * @return Skillインスタンス、失敗時はnull
     */
    fun createSkill(reference: SkillReference): Skill? {
        return try {
            val skillType = reference.type?.lowercase() ?: "basic"

            when (skillType) {
                "basic" -> createBasicSkill(reference)
                "projectile" -> createProjectileSkill(reference)
                "meta", "metaskill" -> createMetaSkill(reference)
                "branch", "branchskill" -> createBranchSkill(reference)
                "beam" -> createBeamSkill(reference)
                "aura" -> createAuraSkill(reference)

                else -> {
                    DebugLogger.warn("Unknown skill type: ${reference.type}, defaulting to Basic")
                    createBasicSkill(reference)
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to create skill: ${reference.skill}", e)
            null
        }
    }

    /**
     * 複数のSkill定義からSkillインスタンスリストを生成
     */
    fun createSkills(references: List<SkillReference>): List<Skill> {
        return references.mapNotNull { createSkill(it) }
    }

    // ========================================
    // Skill生成メソッド
    // ========================================

    private fun createBasicSkill(ref: SkillReference): BasicSkill {
        val effects = EffectFactory.createEffects(ref.effects)

        return BasicSkill(
            id = ref.skill,
            meta = parseSkillMeta(ref.meta),
            // 条件は後で実装
            effects = effects
        )
    }

    private fun createProjectileSkill(ref: SkillReference): ProjectileSkill? {
        val projectileTypeName = ref.projectileType ?: run {
            DebugLogger.error("ProjectileSkill requires projectileType field")
            return null
        }

        val projectileType = try {
            ProjectileSkill.ProjectileType.valueOf(projectileTypeName.uppercase())
        } catch (e: IllegalArgumentException) {
            DebugLogger.error("Invalid projectile type: $projectileTypeName", e)
            return null
        }

        val hitEffects = EffectFactory.createEffects(ref.hitEffects)
        val tickEffects = EffectFactory.createEffects(ref.tickEffects)

        val hitSound = ref.hitSound?.let { parseSoundOrNull(it) }
        val launchSound = ref.launchSound?.let { parseSoundOrNull(it) }

        return ProjectileSkill(
            id = ref.skill,
            meta = parseSkillMeta(ref.meta),
            projectileType = projectileType,
            speed = ref.speed ?: "2.0",
            gravity = ref.gravity ?: true,
            hitEffects = hitEffects,
            tickEffects = tickEffects,
            maxDistance = ref.maxDistance ?: "50.0",
            hitRadius = ref.hitRadius ?: "1.0",
            pierce = ref.pierce ?: false,
            homing = ref.homing ?: "0.0",
            hitSound = hitSound,
            launchSound = launchSound
        )
    }

    private fun createMetaSkill(ref: SkillReference): MetaSkill? {
        if (ref.phases.isEmpty()) {
            DebugLogger.error("MetaSkill requires phases field")
            return null
        }

        val phases = ref.phases.mapNotNull { parsePhase(it) }

        if (phases.isEmpty()) {
            DebugLogger.error("MetaSkill has no valid phases")
            return null
        }

        return MetaSkill(
            id = ref.skill,
            meta = parseSkillMeta(ref.meta),
            phases = phases
        )
    }

    private fun createBranchSkill(ref: SkillReference): BranchSkill? {
        if (ref.branches.isEmpty()) {
            DebugLogger.error("BranchSkill requires branches field")
            return null
        }

        val branches = ref.branches.mapNotNull { branchDef ->
            val targeter = TargeterFactory.createTargeter(branchDef.targeter) ?: return@mapNotNull null
            val skills = createSkills(branchDef.skills)

            if (skills.isEmpty()) {
                DebugLogger.warn("Branch has no valid skills")
                return@mapNotNull null
            }

            SkillBranch(
                condition = branchDef.condition,
                skills = skills,
                targeter = targeter,
                isDefault = branchDef.isDefault
            )
        }

        if (branches.isEmpty()) {
            DebugLogger.error("BranchSkill has no valid branches")
            return null
        }

        return BranchSkill(
            id = ref.skill,
            meta = parseSkillMeta(ref.meta),
            branches = branches
        )
    }

    private fun createBeamSkill(ref: SkillReference): BeamSkill {
        val effects = EffectFactory.createEffects(ref.effects)

        val particle = ref.beamParticle?.let { parseParticleOrNull(it) } ?: Particle.FLAME
        val hitSound = ref.hitSound?.let { parseSoundOrNull(it) }
        val fireSound = ref.beamFireSound?.let { parseSoundOrNull(it) }

        return BeamSkill(
            id = ref.skill,
            meta = parseSkillMeta(ref.meta),
            range = ref.beamRange ?: "20.0",
            width = ref.beamWidth ?: "0.5",
            particle = particle,
            particleDensity = ref.beamParticleDensity ?: 0.3,
            duration = ref.beamDuration?.toLongOrNull() ?: 1000L,
            tickInterval = ref.beamTickInterval?.toLongOrNull() ?: 50L,
            piercing = ref.beamPiercing ?: true,
            effects = effects,
            hitSound = hitSound,
            fireSound = fireSound
        )
    }

    private fun createAuraSkill(ref: SkillReference): AuraSkill {
        val effects = EffectFactory.createEffects(ref.effects)

        val particle = ref.auraParticle?.let { parseParticleOrNull(it) } ?: Particle.ENCHANT
        val startSound = ref.auraStartSound?.let { parseSoundOrNull(it) }
        val tickSound = ref.auraTickSound?.let { parseSoundOrNull(it) }
        val endSound = ref.auraEndSound?.let { parseSoundOrNull(it) }

        return AuraSkill(
            id = ref.skill,
            meta = parseSkillMeta(ref.meta),
            radius = ref.auraRadius ?: "5.0",
            duration = ref.auraDuration?.toLongOrNull() ?: 10000L,
            tickInterval = ref.auraTickInterval?.toLongOrNull() ?: 1000L,
            particle = particle,
            particleCount = ref.auraParticleCount ?: 10,
            particleSpeed = ref.auraParticleSpeed ?: 0.1,
            effects = effects,
            selfAffect = ref.auraSelfAffect ?: false,
            maxTargets = ref.auraMaxTargets ?: 0,
            startSound = startSound,
            tickSound = tickSound,
            endSound = endSound
        )
    }

    // ========================================
    // ヘルパーメソッド
    // ========================================

    private fun parsePhase(phaseDef: SkillPhaseDefinition): SkillPhase? {
        val targeter = TargeterFactory.createTargeter(phaseDef.targeter) ?: return null
        val skills = createSkills(phaseDef.skills)

        if (skills.isEmpty()) {
            DebugLogger.warn("Phase ${phaseDef.name} has no valid skills")
            return null
        }

        return SkillPhase(
            name = phaseDef.name,
            targeter = targeter,
            skills = skills,
            meta = PhaseMeta(
                parallel = phaseDef.parallel,
                executeDelay = parseDuration(phaseDef.executeDelay),
                sync = phaseDef.sync
            )
        )
    }

    private fun parseSkillMeta(metaDef: com.github.azuazu3939.unique.mob.SkillMetaDefinition): SkillMeta {
        return SkillMeta(
            sync = metaDef.sync,
            executeDelay = parseDuration(metaDef.executeDelay),
            effectDelay = parseDuration(metaDef.effectDelay),
            cancelOnDeath = metaDef.cancelOnDeath,
            interruptible = metaDef.interruptible
            // 未実装
        )
    }

    private fun parseDuration(durationStr: String): kotlin.time.Duration {
        return try {
            when {
                durationStr.endsWith("ms") -> {
                    val ms = durationStr.removeSuffix("ms").toLong()
                    ms.milliseconds
                }
                durationStr.endsWith("s") -> {
                    val s = durationStr.removeSuffix("s").toLong()
                    (s * 1000).milliseconds
                }
                durationStr.endsWith("t") -> {
                    val ticks = durationStr.removeSuffix("t").toLong()
                    (ticks * 50).milliseconds
                }
                else -> {
                    // デフォルトはms
                    durationStr.toLongOrNull()?.milliseconds ?: 0.milliseconds
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to parse duration: $durationStr", e)
            0.milliseconds
        }
    }

    private fun parseSoundOrNull(soundName: String): Sound? {
        return try {
            getSound(NamespacedKey.minecraft(soundName.lowercase()))
        } catch (e: IllegalArgumentException) {
            DebugLogger.warn("Invalid sound: $soundName")
            null
        }
    }

    private fun parseParticleOrNull(particleName: String): Particle {
        return ResourceKeyResolver.resolveParticle(particleName, Particle.FLAME)
    }

    /**
     * Skillタイプの検証
     *
     * @param type Skillタイプ
     * @return 有効な場合true
     */
    fun isValidSkillType(type: String): Boolean {
        return type.lowercase() in listOf(
            "basic", "projectile", "meta", "metaskill", "branch", "branchskill", "beam", "aura"
        )
    }

    /**
     * 利用可能なSkillタイプ一覧を取得
     */
    fun getAvailableSkillTypes(): List<String> {
        return listOf(
            "basic", "projectile", "meta", "branch", "beam", "aura"
        )
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.info("=== SkillFactory Debug Info ===")
        DebugLogger.info("Available skill types: ${getAvailableSkillTypes().joinToString(", ")}")
        DebugLogger.info("Total skill types: ${getAvailableSkillTypes().size}")
    }
}
