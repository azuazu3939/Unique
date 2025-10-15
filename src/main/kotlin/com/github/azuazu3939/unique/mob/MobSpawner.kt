package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.event.*
import com.github.azuazu3939.unique.util.*
import com.github.shynixn.mccoroutine.folia.asyncDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*

/**
 * Mobのスポーンとダメージ処理
 */
class MobSpawner(private val plugin: Unique) {

    /**
     * Mobをスポーン
     */
    suspend fun spawnMob(
        mobName: String,
        location: Location,
        mobDefinitions: Map<String, MobDefinition>,
        activeMobs: MutableMap<String, MobInstance>,
        skillExecutor: MobSkillExecutor,
        skillLibrary: Map<String, List<String>>
    ): PacketMob? {
        var mob: PacketMob? = null
        var uuid: UUID? = null
        var definition: MobDefinition? = null
        withContext(plugin.asyncDispatcher) {

            definition = mobDefinitions[mobName]
            if (definition == null) {
                DebugLogger.error("Mob definition not found: $mobName")
                return@withContext
            }

            // CEL評価用のコンテキストを構築
            val context = MobCELEvaluator.buildMobSpawnContext(plugin, location)

            // Health, Damage, Armor, ArmorToughnessを動的に評価
            val evaluatedHealth = MobCELEvaluator.evaluateHealth(plugin, definition.health, context)
            val evaluatedDamage = MobCELEvaluator.evaluateDamage(plugin, definition.damage, context)
            val evaluatedArmor = MobCELEvaluator.evaluateArmor(plugin, definition.armor, context)
            val evaluatedArmorToughness = MobCELEvaluator.evaluateArmorToughness(plugin, definition.armorToughness, context)

            // PacketMobを生成
            val entityId = plugin.packetEntityManager.generateEntityId()
            uuid = plugin.packetEntityManager.generateUUID()

            mob = PacketMob.builder(
                entityId = entityId,
                uuid = uuid,
                entityType = definition.getEntityType(),
                location = location,
                mobName = definition.getDisplayName()
            )
                .health(evaluatedHealth)
                .maxHealth(evaluatedHealth)
                .damage(evaluatedDamage)
                .armor(evaluatedArmor)
                .armorToughness(evaluatedArmorToughness)
                .damageFormula(definition.damageFormula)
                .customNameVisible(definition.appearance.customNameVisible)
                .hasAI(definition.ai.hasAI)
                .hasGravity(definition.ai.hasGravity)
                .glowing(definition.appearance.glowing)
                .invisible(definition.appearance.invisible)
                .options(definition.options)
                .movementSpeed(definition.ai.movementSpeed)
                .followRange(definition.ai.followRange)
                .knockbackResistance(definition.ai.knockbackResistance)
                .lookAtMovementDirection(definition.ai.lookAtMovementDirection)
                .wallClimbHeight(
                    definition.ai.wallClimbHeight ?: PacketMob.getDefaultStepHeight(definition.getEntityType())
                )
                .jumpStrength(definition.ai.jumpStrength ?: 0.5)
                .build()

            // スポーンイベント発火
            if (EventUtil.callEvent(PacketMobSpawnEvent(mob, location, mobName))) {
                DebugLogger.debug("Mob spawn cancelled by event: $mobName")
                mob = null
                return@withContext
            }

            plugin.packetEntityManager.registerEntity(mob)

            // MobInstanceを作成
            val instance = MobInstance(mobName, mob, definition)
            activeMobs[uuid.toString()] = instance


            // OnSpawnスキルを実行
            skillExecutor.executeSkillTriggers(mob, definition.skills.onSpawn, PacketMobSkillEvent.SkillTriggerType.ON_SPAWN, skillLibrary)
        }

        if (mob == null || uuid == null || definition == null) {
            return null
        }

        DebugLogger.spawn(mobName, location.toString())
        return mob
    }

    /**
     * Mob被ダメージ処理
     */
    suspend fun handleMobDamaged(
        mob: PacketMob,
        damager: Entity,
        damage: Double,
        activeMobs: Map<String, MobInstance>,
        skillExecutor: MobSkillExecutor,
        skillLibrary: Map<String, List<String>>
    ) {
        val instance = activeMobs[mob.uuid.toString()] ?: return
        val definition = instance.definition

        DebugLogger.debug("Mob ${instance.definitionName} damaged by ${damager.type} ($damage damage)")

        // ダメージイベント発火＆キャンセルチェック
        val cause = if (damager is Player) {
            PacketMobDamageEvent.DamageCause.PLAYER_ATTACK
        } else {
            PacketMobDamageEvent.DamageCause.ENTITY_ATTACK
        }

        val damageEvent = EventUtil.callEventOrNull(PacketMobDamageEvent(mob, damager, damage, cause)) ?: run {
            DebugLogger.verbose("Damage to ${instance.definitionName} cancelled by event")
            return
        }

        val finalDamage = damageEvent.damage

        // ダメージを適用
        mob.damage(finalDamage)

        // OnDamagedスキルトリガー実行
        skillExecutor.executeSkillTriggers(mob, definition.skills.onDamaged, PacketMobSkillEvent.SkillTriggerType.ON_DAMAGED, skillLibrary)

        // 死亡チェック - PacketMob.kill()で既に処理されているので、ここでは何もしない
        // PacketEntityManagerが dead_entity_cleanup_ticks 後に cleanup() を呼び出す
    }
}
