package com.github.azuazu3939.unique.skill.types

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.skill.Skill
import com.github.azuazu3939.unique.skill.SkillMeta
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.plugin.Plugin
import kotlin.math.cos
import kotlin.math.sin

/**
 * サモンスキル
 *
 * エンティティやCustomMobを召喚するスキル
 *
 * @param id スキルID
 * @param meta スキルメタ設定
 * @param condition 実行条件
 * @param summonType 召喚タイプ（vanilla / custom）
 * @param entityType バニラエンティティタイプ
 * @param customMobId カスタムMob ID
 * @param amount 召喚数
 * @param spreadRange 散らばり範囲
 * @param duration 召喚されたエンティティの持続時間（null = 永続）
 * @param inheritTarget 召喚者のターゲットを引き継ぐか
 */
class SummonSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val summonType: SummonType = SummonType.VANILLA,
    private val entityType: EntityType? = null,
    private val customMobId: String? = null,
    private val amount: Int = 1,
    private val spreadRange: Double = 2.0,
    private val duration: Long? = null,
    private val inheritTarget: Boolean = false
) : Skill(id, meta, condition) {

    enum class SummonType {
        VANILLA,  // バニラのエンティティ
        CUSTOM    // カスタムMob
    }

    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.skill("SummonSkill condition not met")
            return
        }

        // 実行遅延
        delay(meta.executeDelay)

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.skill("SummonSkill: No targets found")
            return
        }

        // 各ターゲット位置で召喚
        for (target in targets) {
            summonEntities(plugin, target.location, source)
        }
    }

    override suspend fun execute(plugin: Plugin, source: PacketEntity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.skill("SummonSkill condition not met (PacketEntity)")
            return
        }

        // 実行遅延
        delay(meta.executeDelay)

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.skill("SummonSkill: No targets found (PacketEntity)")
            return
        }

        // 各ターゲット位置で召喚
        for (target in targets) {
            summonEntities(plugin, target.location, null)
        }
    }

    /**
     * エンティティを召喚
     */
    private suspend fun summonEntities(
        plugin: Plugin,
        location: Location,
        source: Entity?
    ) {
        val world = location.world ?: return

        repeat(amount) { index ->
            // 散らばり位置を計算
            val offset = if (spreadRange > 0 && amount > 1) {
                val angle = (2 * Math.PI * index) / amount
                val distance = Math.random() * spreadRange
                Location(
                    world,
                    location.x + cos(angle) * distance,
                    location.y,
                    location.z + sin(angle) * distance
                )
            } else {
                location.clone()
            }

            when (summonType) {
                SummonType.VANILLA -> {
                    if (entityType == null) {
                        DebugLogger.error("SummonSkill: entityType is null for VANILLA summon")
                        return
                    }
                    summonVanillaEntity(plugin, offset, entityType, source)
                }
                SummonType.CUSTOM -> {
                    if (customMobId == null) {
                        DebugLogger.error("SummonSkill: customMobId is null for CUSTOM summon")
                        return
                    }
                    summonCustomMob(plugin, offset, customMobId)
                }
            }
        }

        DebugLogger.skill("SummonSkill: Summoned $amount entities at ${location.blockX}, ${location.blockY}, ${location.blockZ}")
    }

    /**
     * バニラエンティティを召喚
     */
    private suspend fun summonVanillaEntity(
        plugin: Plugin,
        location: Location,
        type: EntityType,
        source: Entity?
    ) {
        val world = location.world ?: return

        if (meta.sync) {
            // 同期処理
            withContext(plugin.globalRegionDispatcher) {
                val entity = world.spawnEntity(location, type)
                configureEntity(plugin, entity, source)
            }
        } else {
            // 非同期処理（地域スケジューラ使用）
            plugin.launch(plugin.globalRegionDispatcher) {
                val entity = world.spawnEntity(location, type)
                configureEntity(plugin, entity, source)
            }
        }
    }

    /**
     * カスタムMobを召喚
     */
    private suspend fun summonCustomMob(
        plugin: Plugin,
        location: Location,
        mobId: String
    ) {
        val unique = plugin as? Unique ?: return

        // MobManagerを使用してカスタムMobをスポーン
        unique.mobManager.spawnMob(mobId, location)

        DebugLogger.skill("SummonSkill: Spawned custom mob '$mobId'")
    }

    /**
     * 召喚されたエンティティを設定
     */
    private suspend fun configureEntity(
        plugin: Plugin,
        entity: Entity,
        source: Entity?
    ) {
        // ターゲット継承
        if (inheritTarget && source is LivingEntity && entity is Mob) {
            entity.target = source
        }

        // 持続時間の設定
        if (duration != null) {
            plugin.launch(plugin.globalRegionDispatcher) {
                delay(duration)
                if (entity.isValid && !entity.isDead) {
                    entity.remove()
                    DebugLogger.skill("SummonSkill: Removed entity after ${duration}ms")
                }
            }
        }
    }

    override fun getDescription(): String {
        return when (summonType) {
            SummonType.VANILLA -> "Summon $amount x $entityType"
            SummonType.CUSTOM -> "Summon $amount x Custom Mob '$customMobId'"
        }
    }

    override fun debugInfo(): String {
        return "SummonSkill[type=$summonType, entity=$entityType, custom=$customMobId, amount=$amount, spread=$spreadRange, duration=$duration]"
    }
}