package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.entity.EntityAnimation.DAMAGE
import com.github.azuazu3939.unique.entity.EntityAnimation.DEATH
import com.github.azuazu3939.unique.entity.packet.PacketSender
import com.github.azuazu3939.unique.event.*
import com.github.azuazu3939.unique.mob.MobInstance
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.EventUtil
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.min

/**
 * PacketMobの戦闘機能を担当するクラス
 *
 * - ダメージ計算
 * - 防御力計算
 * - 死亡処理
 * - 攻撃処理
 * - ダメージランキング
 */
class PacketMobCombat(private val mob: PacketMob) {

    /**
     * 最後にダメージを与えたエンティティ
     */
    var lastDamager: Entity? = null
        private set

    /**
     * 最後にダメージを受けた時刻（tick）
     */
    var lastDamageTick: Int = 0
        private set

    /**
     * ダメージランキング（UUID -> 累計ダメージ）
     */
    private val damageTracker = mutableMapOf<UUID, Double>()

    /**
     * ダメージを与える（攻撃者あり）
     */
    suspend fun damage(amount: Double, damager: Entity?) {
        // ダメージを受けられないなら無視
        if (!mob.canTakeDamage() || mob.isDead) {
            return
        }

        // 最後のダメージャーを記録
        lastDamager = damager
        lastDamageTick = mob.ticksLived

        // ダメージランキングに記録（プレイヤーのみ）
        if (damager is Player) {
            damageTracker[damager.uniqueId] = damageTracker.getOrDefault(damager.uniqueId, 0.0) + amount
        }

        // ダメージ計算式を決定
        val formula = mob.damageFormula
            ?: Unique.instance.configManager.mainConfig.damage.defaultFormula

        val reducedDamage = if (formula != null) {
            calculateDamageWithFormula(amount, formula)
        } else {
            calculateArmorReduction(amount)
        }

        DebugLogger.debug("${mob.mobName} took $reducedDamage damage from ${getDamagerName(damager)} (original: $amount, armor: ${mob.armor}, toughness: ${mob.armorToughness}) (${mob.health}/${mob.maxHealth} HP)")

        // ダメージイベント発火
        val damageEvent = EventUtil.callEventOrNull(
            PacketMobDamageEvent(mob, damager, reducedDamage)
        )

        if (damageEvent == null) {
            return
        }

        // PacketEntityのdamageメソッドを呼び出し
        mob.applyDamage(damageEvent.damage)
        val isDying = mob.health <= 0.0

        // ノックバック効果を適用（死亡時は除く）
        if (!isDying && damager != null) {
            applyKnockbackFromDamage(damager, damageEvent.damage)
        }

        // ダメージアニメーション再生（死亡時は除く、設定で有効な場合のみ）
        if (!isDying && mob.options.showDamageAnimation) {
            mob.playAnimation(DAMAGE)
        }

        // ダメージサウンド再生（死亡時は除く、設定で有効な場合のみ）
        if (!isDying && !mob.options.silent && mob.options.playHurtSound) {
            mob.playHurtSound()
        }

        // OnDamagedスキルトリガー実行
        val instance = Unique.instance.mobManager.getMobInstance(mob)
        if (instance != null) {
            Unique.instance.mobManager.executeSkillTriggers(
                mob,
                instance.definition.skills.onDamaged,
                PacketMobSkillEvent.SkillTriggerType.ON_DAMAGED
            )
        }
    }

    /**
     * 殺す
     */
    suspend fun kill() {
        if (mob.isDead) return

        val killer = lastDamager as? Player
        DebugLogger.info("${mob.mobName} was killed by ${killer?.name ?: "Unknown"}")

        // 死亡フラグを先に立てる（OnDeathスキル実行前）
        mob.isDead = true
        mob.health = 0.0
        mob.deathTick = mob.ticksLived

        // 死亡イベント発火
        val deathEvent = PacketMobDeathEvent(mob, killer)

        // ドロップアイテムを計算してイベントに追加
        val instance = Unique.instance.mobManager.getMobInstance(mob)
        if (instance != null && killer != null) {
            withContext(Unique.instance.regionDispatcher(mob.location)) {
                val drops = Unique.instance.mobManager.calculateDropItems(instance.definition, killer, mob.location)
                deathEvent.drops.addAll(drops)
            }
        }

        EventUtil.callEvent(deathEvent)

        // イベントで追加/変更されたドロップをワールドに生成
        withContext(Unique.instance.regionDispatcher(mob.location)) {
            Unique.instance.mobManager.dropItemsInWorld(mob.location, deathEvent.drops)
        }

        // OnDeathスキルトリガー実行（ジョブキャンセルの前に実行）
        if (instance != null) {
            Unique.instance.mobManager.executeSkillTriggers(
                mob,
                instance.definition.skills.onDeath,
                PacketMobSkillEvent.SkillTriggerType.ON_DEATH
            )
        }

        // デスアニメーション再生
        mob.playAnimation(DEATH)

        // 死亡サウンド再生（設定で有効な場合のみ）
        if (!mob.options.silent) {
            mob.playDeathSound()
        }

        // MobInstanceを削除＆削除イベント発火
        Unique.instance.mobManager.getMobInstance(mob)?.let {
            Unique.instance.mobManager.removeMobInstance(mob.uuid)
            EventUtil.callEvent(PacketMobRemoveEvent(mob, PacketMobRemoveEvent.RemoveReason.DEATH))
        }

        DebugLogger.debug("PacketMob killed: ${mob.entityId} (${mob.mobName})")
    }

    /**
     * 攻撃実行
     */
    fun performAttack(target: Entity) {
        DebugLogger.debug("${mob.mobName} attacks ${(target as? Player)?.name ?: target.type.name}")

        // MobInstance取得
        val instance = Unique.instance.mobManager.getMobInstance(mob)
        val damageStr = instance?.definition?.damage
        val damage = if (damageStr == null || damageStr.equals("null", ignoreCase = true)) {
            5.0
        } else {
            damageStr.toDoubleOrNull() ?: 5.0
        }

        // 攻撃イベント発火＆キャンセルチェック
        val attackEvent = EventUtil.callEventOrNull(PacketMobAttackEvent(mob, target, damage)) ?: run {
            return
        }

        // プレイヤーの場合、キル判定を行う
        if (target is Player) {
            val playerHealthBefore = target.health

            // ダメージを与える
            target.damage(attackEvent.damage)

            // プレイヤーが死んだかチェック
            if (playerHealthBefore > 0.0 && target.health <= 0.0 || target.isDead) {
                handlePlayerKill(target, instance)
            }
        } else {
            // 通常のエンティティの場合はBukkitのダメージ
            when (target) {
                is LivingEntity -> {
                    target.damage(attackEvent.damage)
                }
            }
        }

        // 攻撃アニメーション再生
        playAttackAnimation()

        // OnAttackスキルトリガー実行
        instance?.let {
            Unique.instance.mobManager.executeSkillTriggers(
                mob,
                it.definition.skills.onAttack,
                PacketMobSkillEvent.SkillTriggerType.ON_ATTACK
            )
        }
    }

    /**
     * ダメージランキングを取得
     */
    fun getDamageRanking(limit: Int = 10): List<Pair<UUID, Double>> {
        return damageTracker.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    /**
     * プレイヤーの累計ダメージを取得
     */
    fun getPlayerDamage(player: Player): Double {
        return damageTracker[player.uniqueId] ?: 0.0
    }

    /**
     * プレイヤーのランキング順位を取得
     */
    fun getPlayerRank(player: Player): Int {
        val sortedDamage = damageTracker.entries
            .sortedByDescending { it.value }
            .map { it.key }

        val rank = sortedDamage.indexOf(player.uniqueId)
        return if (rank >= 0) rank + 1 else -1
    }

    /**
     * ダメージランキングをクリア
     */
    fun clearDamageRanking() {
        damageTracker.clear()
    }

    /**
     * ダメージランキングをブロードキャスト
     */
    fun broadcastDamageRanking(limit: Int = 10) {
        if (damageTracker.isEmpty()) return

        val sortedDamage = damageTracker.entries
            .sortedByDescending { it.value }
            .take(limit)

        val message = buildString {
            appendLine("§6========== ${mob.mobName} Damage Ranking ==========")
            sortedDamage.forEachIndexed { index, entry ->
                val player = Bukkit.getPlayer(entry.key)
                val playerName = player?.name ?: "Unknown"
                appendLine("§e${index + 1}. §f$playerName: §c${String.format("%.1f", entry.value)} damage")
            }
            append("§6================================================")
        }

        mob.location.world?.players?.forEach { player ->
            if (player.location.distance(mob.location) <= 64.0) {
                player.sendMessage(message)
            }
        }
    }

    /**
     * ダメージ記憶をクリアする必要があるかチェック
     */
    fun shouldClearDamageMemory(currentTick: Int, memoryTicks: Int): Boolean {
        return lastDamager != null && currentTick - lastDamageTick > memoryTicks
    }

    /**
     * ダメージ記憶をクリア
     */
    fun clearDamageMemory() {
        lastDamager = null
    }

    // ========================================
    // Private Methods
    // ========================================

    /**
     * CEL式でダメージを計算
     */
    private fun calculateDamageWithFormula(damageAmount: Double, formula: String): Double {
        return try {
            val evaluator = Unique.instance.celEvaluator
            val context = buildDamageContext(damageAmount)

            when (val result = evaluator.evaluate(formula, context)) {
                is Number -> result.toDouble().coerceAtLeast(0.0)
                else -> {
                    DebugLogger.error("Damage formula returned non-numeric value: $result (formula: ${mob.damageFormula})")
                    damageAmount
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage formula: ${mob.damageFormula}", e)
            damageAmount
        }
    }

    /**
     * ダメージ計算用のCELコンテキストを構築
     */
    private fun buildDamageContext(damageAmount: Double): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // PacketMob情報
        context["entity"] = CELVariableProvider.buildPacketEntityInfo(mob)

        // ダメージ情報
        context["damage"] = damageAmount
        context["armor"] = mob.armor
        context["armorToughness"] = mob.armorToughness
        context["health"] = mob.health
        context["maxHealth"] = mob.maxHealth

        // 数学関数
        context["math"] = mapOf(
            "min" to { a: Double, b: Double -> min(a, b) },
            "max" to { a: Double, b: Double -> kotlin.math.max(a, b) },
            "abs" to { value: Double -> kotlin.math.abs(value) },
            "floor" to { value: Double -> kotlin.math.floor(value) },
            "ceil" to { value: Double -> kotlin.math.ceil(value) },
            "round" to { value: Double -> kotlin.math.round(value) }
        )

        // グローバル関数
        context["min"] = { a: Double, b: Double -> min(a, b) }
        context["max"] = { a: Double, b: Double -> kotlin.math.max(a, b) }

        return context
    }

    /**
     * Minecraftのダメージ軽減計算
     */
    private fun calculateArmorReduction(damage: Double): Double {
        if (mob.armor <= 0.0) return damage

        val armorReduction = min(20.0, mob.armor) / 25.0  // 最大80%軽減

        // ArmorToughnessによる追加軽減
        val toughnessBonus = if (damage > 10.0 && mob.armorToughness > 0.0) {
            val excessDamage = damage - 10.0
            val toughnessEffect = mob.armorToughness / 20.0
            min(0.2, excessDamage / 100.0 * toughnessEffect)
        } else {
            0.0
        }

        val totalReduction = (armorReduction + toughnessBonus).coerceIn(0.0, 0.8)
        return damage * (1.0 - totalReduction)
    }

    /**
     * 攻撃アニメーションを再生
     */
    private fun playAttackAnimation() {
        mob.viewers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                PacketSender.sendAttackAnimationPacket(player, mob.entityId, mob.entityType)
            }
        }
    }

    /**
     * プレイヤーキル処理
     */
    private fun handlePlayerKill(player: Player, instance: MobInstance?) {
        val setKillerEnabled = instance?.definition?.options?.setAsKiller ?: true

        // PacketMobKillPlayerイベント発火
        val killEvent = EventUtil.callEventOrNull(
            PacketMobKillPlayerEvent(mob, player, setKillerEnabled)
        )

        if (killEvent != null && killEvent.setKiller) {
            // 死亡メッセージ
            val deathMessage = "§c${player.name} §7was slain by §e${mob.mobName}"

            // ブロードキャスト
            player.world.players.forEach { p ->
                if (p.location.distance(player.location) <= 64.0) {
                    p.sendMessage(deathMessage)
                }
            }

            DebugLogger.info("${mob.mobName} killed ${player.name}")
        }
    }

    /**
     * ダメージャー名を取得
     */
    private fun getDamagerName(damager: Entity?): String {
        return when (damager) {
            is Player -> damager.name
            else -> damager?.type?.name ?: "Unknown"
        }
    }

    /**
     * ダメージからノックバックを適用
     *
     * @param damager ダメージ元エンティティ
     * @param damage ダメージ量
     */
    private fun applyKnockbackFromDamage(damager: Entity, damage: Double) {
        // ノックバック強度を計算（ダメージに応じて調整）
        val baseKnockback = 0.4
        val damageMultiplier = min(1.0, damage / 10.0)
        val knockbackStrength = baseKnockback * (1.0 + damageMultiplier * 0.5)

        // 垂直方向の強度
        val verticalStrength = 0.4

        // Physicsコンポーネントにノックバックを適用
        mob.getPhysicsComponent().applyKnockback(
            damager.location.x,
            damager.location.z,
            knockbackStrength,
            verticalStrength
        )
    }
}
