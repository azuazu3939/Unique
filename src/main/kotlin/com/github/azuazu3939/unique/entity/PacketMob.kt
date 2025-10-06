package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.entity.EntityAnimation.DAMAGE
import com.github.azuazu3939.unique.entity.EntityAnimation.DEATH
import com.github.azuazu3939.unique.entity.packet.PacketSender
import com.github.azuazu3939.unique.event.*
import com.github.azuazu3939.unique.mob.MobOptions
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.EventUtil
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.min

/**
 * AI状態
 */
enum class AIState {
    IDLE,       // 待機中
    TARGET,     // ターゲット追跡中
    ATTACK,     // 攻撃中
    WANDER      // 徘徊中
}

/**
 * Mob用パケットエンティティ
 *
 * カスタムMobの実装
 * PacketEventsを使用してパケットを送信
 */
class PacketMob(
    entityId: Int,
    uuid: UUID,
    entityType: EntityType,
    location: Location,
    val mobName: String
) : PacketEntity(entityId, uuid, entityType, location) {

    /**
     * カスタム名表示フラグ
     */
    var customNameVisible: Boolean = true

    /**
     * カスタム名
     */
    var customName: String = mobName
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { player ->
                sendMetadataPacket(Bukkit.getPlayer(player))
            }
        }

    /**
     * AI有効フラグ
     */
    var hasAI: Boolean = true

    /**
     * 重力フラグ
     */
    var hasGravity: Boolean = true

    /**
     * 発光フラグ
     */
    var isGlowing: Boolean = false
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { player ->
                sendMetadataPacket(Bukkit.getPlayer(player))
            }
        }

    /**
     * 不可視フラグ
     */
    var isInvisible: Boolean = false
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { player ->
                sendMetadataPacket(Bukkit.getPlayer(player))
            }
        }

    /**
     * 防具値（0-30）
     * Minecraftのダメージ軽減計算に使用される
     */
    var armor: Double = 0.0

    /**
     * 防具強度（0-20）
     * 高ダメージに対する追加軽減
     */
    var armorToughness: Double = 0.0

    /**
     * 攻撃力
     * 攻撃時に与えるダメージ量
     */
    var damage: Double = 1.0

    /**
     * ダメージ計算式（CEL式）
     * nullの場合はデフォルトの計算式を使用
     * 例: "damage * (1 - min(20, armor) / 25)"
     */
    var damageFormula: String? = null

    /**
     * Mobオプション設定
     */
    var options: MobOptions = MobOptions()

    /**
     * 変数ストレージ（Mob個体ごと）
     * スキル実行時に使用される一時変数を保存
     */
    private val variables = mutableMapOf<String, Any>()

    /**
     * 変数を設定
     */
    fun setVariable(name: String, value: Any) {
        variables[name] = value
    }

    /**
     * 変数を取得
     */
    fun getVariable(name: String): Any? {
        return variables[name]
    }

    /**
     * すべての変数を取得
     */
    fun getVariables(): Map<String, Any> {
        return variables.toMap()
    }

    /**
     * 変数をクリア
     */
    fun clearVariables() {
        variables.clear()
    }

    // ========================================
    // AI関連フィールド
    // ========================================

    /**
     * AI状態
     */
    var aiState: AIState = AIState.IDLE

    /**
     * 現在のターゲット
     */
    var currentTarget: Entity? = null

    /**
     * 最後にターゲットをチェックしたTick
     */
    var lastTargetCheckTick: Int = 0

    /**
     * 移動速度（ブロック/tick）
     * 0.1 = 2ブロック/秒（1秒=20tick）
     */
    var movementSpeed: Double = 0.1

    /**
     * 追跡範囲
     */
    var followRange: Double = 16.0

    /**
     * 攻撃範囲
     */
    var attackRange: Double = 2.0

    /**
     * 攻撃クールダウン（tick）
     */
    var attackCooldown: Int = 20

    /**
     * 最後に攻撃したTick
     */
    var lastAttackTick: Int = 0

    /**
     * ターゲット検索間隔（tick）
     */
    var targetSearchInterval: Int = 20

    /**
     * ノックバック耐性
     */
    var knockbackResistance: Double = 0.0

    /**
     * 移動方向に視点を向けるか
     */
    var lookAtMovementDirection: Boolean = true

    /**
     * 乗り越えられる壁の高さ（ブロック数）
     */
    var wallClimbHeight: Double = 1.0

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
     * 徘徊位置
     */
    private var wanderTarget: Location? = null

    /**
     * 最後に徘徊位置を更新したTick
     */
    private var lastWanderUpdateTick: Int = 0

    /**
     * 徘徊更新間隔（tick）
     */
    private val wanderUpdateInterval: Int = 100

    /**
     * 最後にbodyの向きを同期したTick
     */
    private var lastBodySyncTick: Int = 0

    /**
     * body同期間隔（tick）
     */
    private val bodySyncInterval: Int = 10

    /**
     * エンティティをスポーン
     */
    override suspend fun spawn(player: Player) {
        if (isDead) return
        PacketSender.sendSpawnPacket(player, this)
        addViewer(player)
    }

    /**
     * エンティティをデスポーン
     */
    override suspend fun despawn(player: Player) {
        PacketSender.sendDespawnPacket(player, entityId)
        removeViewer(player)
    }

    /**
     * テレポート
     */
    override suspend fun teleport(newLocation: Location) {
        if (isDead) return

        val oldLocation = location.clone()
        location = newLocation.clone()

        // 全ビューワーに送信
        viewers.forEach { player ->
            Bukkit.getPlayer(player)?.let {
                PacketSender.sendTeleportPacket(it, entityId, newLocation)
            }
        }

        DebugLogger.verbose("Teleported $mobName from $oldLocation to $newLocation")
    }

    /**
     * 相対移動
     */
    override suspend fun move(deltaX: Double, deltaY: Double, deltaZ: Double) {
        if (isDead) return

        location.add(deltaX, deltaY, deltaZ)

        // 全ビューワーに送信
        viewers.forEach { player ->
           Bukkit.getPlayer(player)?.let {
               PacketSender.sendMovePacket(it, entityId, deltaX, deltaY, deltaZ)
           }
        }

        DebugLogger.verbose("Moved $mobName by ($deltaX, $deltaY, $deltaZ)")
    }

    /**
     * メタデータ更新
     */
    override suspend fun updateMetadata() {
        if (isDead) return

        viewers.forEach { player ->
            sendMetadataPacket(Bukkit.getPlayer(player))
        }

        DebugLogger.verbose("Updated metadata for $mobName")
    }

    /**
     * メタデータパケット送信
     */
    private fun sendMetadataPacket(player: Player?) {
        if (player == null) return
        PacketSender.sendMetadataPacket(player, this)
    }

    /**
     * アニメーション再生
     */
    override suspend fun playAnimation(animation: EntityAnimation) {
        // 死亡時はDEATHアニメーションのみ再生可能
        if (isDead && animation != DEATH) {
            DebugLogger.verbose("Skipping animation $animation for $mobName (isDead=true)")
            return
        }

        viewers.forEach { player ->
            Bukkit.getPlayer(player)?.let {
                PacketSender.sendAnimationPacket(it, entityId, animation)
            }
        }
        DebugLogger.verbose("Playing animation $animation for $mobName")
    }

    /**
     * ダメージを受けられるかどうか
     * optionsで設定可能
     */
    override fun canTakeDamage(): Boolean {
        return options.canTakeDamage && !options.invincible
    }

    /**
     * ダメージを受ける（オーバーライド）
     */
    /**
     * ダメージを与える（攻撃者なし）
     */
    override suspend fun damage(amount: Double) {
        damage(amount, null)
    }

    /**
     * ダメージを与える（攻撃者あり）
     *
     * @param amount ダメージ量
     * @param damager ダメージを与えたエンティティ（Entity または PacketMob）
     */
    suspend fun damage(amount: Double, damager: Entity?) {
        // ダメージを受けられないなら無視
        if (!canTakeDamage()) {
            DebugLogger.verbose("$mobName cannot take damage (canTakeDamage=${options.canTakeDamage}, invincible=${options.invincible})")
            return
        }

        // 最後のダメージャーを記録
        lastDamager = damager
        lastDamageTick = ticksLived

        // ダメージランキングに記録（プレイヤーのみ）
        if (damager is Player) {
            damageTracker[damager.uniqueId] = damageTracker.getOrDefault(damager.uniqueId, 0.0) + amount
        }

        // ダメージ計算式を決定
        // 1. Mob個別の式
        // 2. 設定ファイルのデフォルト式
        // 3. 組み込みのデフォルト計算
        val formula = damageFormula
            ?: Unique.instance.configManager.mainConfig.damage.defaultFormula

        val reducedDamage = if (formula != null) {
            calculateDamageWithFormula(amount, formula)
        } else {
            calculateArmorReduction(amount)
        }

        val damagerName = when (damager) {
            is Player -> damager.name
            else -> damager?.type?.name ?: "Unknown"
        }
        DebugLogger.debug("$mobName took $reducedDamage damage from $damagerName (original: $amount, armor: $armor, toughness: $armorToughness) (${health}/${maxHealth} HP)")

        // ダメージイベント発火
        val damageEvent = EventUtil.callEventOrNull(
            PacketMobDamageEvent(this, damager, reducedDamage)
        )

        if (damageEvent == null) {
            DebugLogger.verbose("$mobName damage cancelled by event")
            return
        }

        super.damage(damageEvent.damage)
        val isDying = health <= 0.0

        // ダメージアニメーション再生（死亡時は除く、設定で有効な場合のみ）
        if (!isDying && options.showDamageAnimation) {
            playAnimation(DAMAGE)
        }

        // ダメージサウンド再生（死亡時は除く、設定で有効な場合のみ）
        if (!isDying && !options.silent && options.playHurtSound) {
            playHurtSound()
        }

        // OnDamagedスキルトリガー実行
        val instance = Unique.instance.mobManager.getMobInstance(this)
        if (instance != null) {
            Unique.instance.mobManager.executeSkillTriggers(
                this,
                instance.definition.skills.onDamaged,
                PacketMobSkillEvent.SkillTriggerType.ON_DAMAGED
            )
        }
    }

    /**
     * CEL式でダメージを計算
     *
     * @param damageAmount ダメージ量
     * @param formula CEL式（Mob個別 or 共通設定）
     */
    private fun calculateDamageWithFormula(damageAmount: Double, formula: String): Double {
        return try {
            val evaluator = Unique.instance.celEvaluator

            // ダメージ計算用のコンテキストを構築
            val context = buildDamageContext(damageAmount)

            // CEL式を評価
            // 数値に変換
            when (val result = evaluator.evaluate(formula, context)) {
                is Number -> result.toDouble().coerceAtLeast(0.0)
                else -> {
                    DebugLogger.error("Damage formula returned non-numeric value: $result (formula: $damageFormula)")
                    damageAmount
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage formula: $damageFormula", e)
            damageAmount
        }
    }

    /**
     * ダメージ計算用のCELコンテキストを構築
     */
    private fun buildDamageContext(damageAmount: Double): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // PacketMob情報
        context["entity"] = CELVariableProvider.buildPacketEntityInfo(this)

        // ダメージ情報
        context["damage"] = damageAmount
        context["armor"] = armor
        context["armorToughness"] = armorToughness
        context["health"] = health
        context["maxHealth"] = maxHealth

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
     * Minecraftのダメージ軽減計算（Armor + ArmorToughness）
     *
     * 計算式：
     * 1. defensePoints = armor値（0-30）
     * 2. toughness = armorToughness値（0-20）
     * 3. damageReduction = defensePoints - (damage / (2 + toughness / 4))
     * 4. finalDamage = damage * (1 - min(20, max(damageReduction / 5, damageReduction - damage / (2 + toughness / 4))) / 25)
     *
     * 簡易版: damage * (1 - min(20, armor) / 25)
     */
    private fun calculateArmorReduction(damage: Double): Double {
        if (armor <= 0.0) return damage

        // Minecraftの防具軽減計算（簡易版）
        // armor値による軽減率: 1ポイントあたり4%（最大20ポイント = 80%軽減）
        val armorReduction = min(20.0, armor) / 25.0  // 最大80%軽減

        // ArmorToughnessによる追加軽減（高ダメージに対してより効果的）
        val toughnessBonus = if (damage > 10.0 && armorToughness > 0.0) {
            val excessDamage = damage - 10.0
            val toughnessEffect = armorToughness / 20.0  // 0.0-1.0
            min(0.2, excessDamage / 100.0 * toughnessEffect)  // 最大20%追加軽減
        } else {
            0.0
        }

        val totalReduction = (armorReduction + toughnessBonus).coerceIn(0.0, 0.8)  // 最大80%軽減
        return damage * (1.0 - totalReduction)
    }

    /**
     * 殺す（オーバーライド）
     */
    override suspend fun kill() {
        val killer = lastDamager as? Player
        DebugLogger.info("$mobName was killed by ${killer?.name ?: "Unknown"}")

        // 死亡イベント発火
        val deathEvent = PacketMobDeathEvent(this, killer)

        // ドロップアイテムを計算してイベントに追加（region dispatcherのコンテキスト内で実行）
        val instance = Unique.instance.mobManager.getMobInstance(this)
        if (instance != null && killer != null) {
            withContext(Unique.instance.regionDispatcher(location)) {
                val drops = Unique.instance.mobManager.calculateDropItems(instance.definition, killer, location)
                deathEvent.drops.addAll(drops)
            }
        }

        EventUtil.callEvent(deathEvent)

        // イベントで追加/変更されたドロップをワールドに生成（region dispatcherのコンテキスト内で実行）
        withContext(Unique.instance.regionDispatcher(location)) {
            Unique.instance.mobManager.dropItemsInWorld(location, deathEvent.drops)
        }

        // OnDeathスキルトリガー実行
        if (instance != null) {
            Unique.instance.mobManager.executeSkillTriggers(
                this,
                instance.definition.skills.onDeath,
                PacketMobSkillEvent.SkillTriggerType.ON_DEATH
            )
        }

        // デスアニメーション再生
        playAnimation(DEATH)

        // 死亡サウンド再生（設定で有効な場合のみ）
        if (!options.silent) {
            playDeathSound()
        }

        super.kill()
    }

    /**
     * 更新処理
     */
    override suspend fun tick() {
        super.tick()

        // 最後のダメージャー情報をクリア（10秒経過後）
        if (lastDamager != null && ticksLived - lastDamageTick > 200) {
            lastDamager = null
        }

        // AI処理
        if (hasAI && !isDead) {
            tickAI()
        }
    }

    /**
     * AI処理
     */
    private suspend fun tickAI() {
        // ターゲット検索
        if (ticksLived - lastTargetCheckTick >= targetSearchInterval) {
            searchTarget()
            lastTargetCheckTick = ticksLived
        }

        // ターゲットの有効性チェック
        if (currentTarget != null) {
            if (currentTarget!!.isDead || !isInRange(currentTarget!!, followRange)) {
                currentTarget = null
                aiState = AIState.IDLE

                // ターゲットロスト時：ピッチを0に戻す
                resetPitch()
            }
        }

        // 状態に応じた処理
        when (aiState) {
            AIState.IDLE -> tickIdle()
            AIState.TARGET -> tickTarget()
            AIState.ATTACK -> tickAttack()
            AIState.WANDER -> tickWander()
        }

        // 定期的にbodyの向きをheadに合わせる
        if (ticksLived - lastBodySyncTick >= bodySyncInterval) {
            syncBodyRotation()
            lastBodySyncTick = ticksLived
        }
    }

    /**
     * ターゲット検索
     */
    private fun searchTarget() {
        val world = location.world ?: return

        // 範囲内のプレイヤーを検索
        val nearbyPlayers = world.getNearbyEntities(
            location,
            followRange,
            followRange,
            followRange
        ).filterIsInstance<Player>()
            .filter { !it.isDead && it.gameMode != org.bukkit.GameMode.SPECTATOR && it.gameMode != org.bukkit.GameMode.CREATIVE }

        if (nearbyPlayers.isNotEmpty()) {
            val newTarget = nearbyPlayers.minByOrNull { it.location.distance(location) }

            // ターゲット変更イベント発火＆キャンセルチェック
            val targetEvent = EventUtil.callEventOrNull(
                PacketMobTargetEvent(this, currentTarget, newTarget, PacketMobTargetEvent.TargetReason.CLOSEST_PLAYER)
            ) ?: return

            currentTarget = targetEvent.newTarget
            if (currentTarget != null) {
                aiState = AIState.TARGET
                DebugLogger.verbose("$mobName found target: ${(currentTarget as? Player)?.name}")
            }
        }
    }

    /**
     * 待機状態の処理
     */
    private suspend fun tickIdle() {
        // 一定確率で徘徊モードに移行
        if (Math.random() < 0.01) {
            aiState = AIState.WANDER
            wanderTarget = getRandomWanderLocation()
        }
    }

    /**
     * ターゲット追跡状態の処理
     */
    private suspend fun tickTarget() {
        val target = currentTarget ?: return

        val distance = location.distance(target.location)

        // 攻撃範囲内なら攻撃モードに移行
        if (distance <= attackRange) {
            aiState = AIState.ATTACK
            return
        }

        // ターゲットに向かって移動
        moveTowards(target.location)
    }

    /**
     * 攻撃状態の処理
     */
    private suspend fun tickAttack() {
        val target = currentTarget ?: run {
            aiState = AIState.IDLE
            return
        }

        val distance = location.distance(target.location)

        // 攻撃範囲外なら追跡モードに戻る
        if (distance > attackRange) {
            aiState = AIState.TARGET
            return
        }

        // 攻撃クールダウンチェック
        if (ticksLived - lastAttackTick >= attackCooldown) {
            performAttack(target)
            lastAttackTick = ticksLived
        }

        // ターゲットの方を向く
        lookAt(target.location)
    }

    /**
     * 徘徊状態の処理
     */
    private suspend fun tickWander() {
        val wander = wanderTarget

        // 徘徊先がない、または到達した場合
        if (wander == null || location.distance(wander) < 1.0) {
            aiState = AIState.IDLE
            wanderTarget = null
            return
        }

        // 徘徊先に向かって移動
        moveTowards(wander)
    }

    /**
     * 指定位置に向かって移動
     */
    private suspend fun moveTowards(target: Location) {
        val direction = target.toVector().subtract(location.toVector()).normalize()
        val distance = min(movementSpeed, location.distance(target))

        var deltaX = direction.x * distance
        var deltaZ = direction.z * distance

        // 移動方向に視点を向ける
        if (lookAtMovementDirection) {
            val targetLookLocation = location.clone().add(direction.multiply(1.0))
            lookAt(targetLookLocation)
        }

        val world = location.world ?: return
        val currentY = location.y

        // 移動先の座標を計算
        val nextX = (location.x + deltaX).toInt()
        val nextZ = (location.z + deltaZ).toInt()

        // 前方の壁をチェック（現在位置と異なる場合のみ）
        var canMoveHorizontally = true
        if (nextX != location.blockX || nextZ != location.blockZ) {
            val wallHeight = checkWallHeight(world, nextX, nextZ, currentY)

            if (wallHeight > wallClimbHeight) {
                // 壁が高すぎる場合は水平移動をキャンセル（Y方向の移動は継続）
                deltaX = 0.0
                deltaZ = 0.0
                canMoveHorizontally = false
                DebugLogger.verbose("$mobName blocked by wall (height: $wallHeight > max: $wallClimbHeight)")
            }
        }

        // 移動先の地面の高さをチェック（水平移動が可能な場合のみ）
        val currentFootY = currentY.toInt()
        var targetGroundY: Double? = null

        if (canMoveHorizontally) {
            // 移動先の地面を探す（下方向に3ブロック、上方向に2ブロック）
            for (i in -3..2) {
                val checkY = currentFootY + i
                val block = world.getBlockAt(nextX, checkY, nextZ)
                val blockAbove = world.getBlockAt(nextX, checkY + 1, nextZ)

                // 固体ブロックがあり、その上が空気なら地面として認識
                if (block.type.isSolid && !blockAbove.type.isSolid) {
                    targetGroundY = checkY + 1.0  // ブロックの上面
                    break
                }
            }
        } else {
            // 水平移動できない場合は現在位置の地面を探す
            for (i in -3..2) {
                val checkY = currentFootY + i
                val block = world.getBlockAt(location.blockX, checkY, location.blockZ)
                val blockAbove = world.getBlockAt(location.blockX, checkY + 1, location.blockZ)

                if (block.type.isSolid && !blockAbove.type.isSolid) {
                    targetGroundY = checkY + 1.0
                    break
                }
            }
        }

        val deltaY = if (targetGroundY != null) {
            val diff = targetGroundY - currentY
            when {
                // 段差が1ブロック以下の場合は一気に登る（角を通過）
                diff > 0.05 && diff <= 1.0 -> diff
                // 大きな段差は少しずつ登る
                diff > 1.0 -> 0.3
                // 下り坂（0.2ブロック以上の差がある場合のみ）
                diff < -0.2 -> -0.3
                // ほぼ平坦（-0.2 ~ 0.05の範囲）
                else -> 0.0
            }
        } else {
            // 地面が見つからない場合：落下
            -0.5
        }

        move(deltaX, deltaY, deltaZ)
    }

    /**
     * 前方の壁の高さをチェック（モブのヒットボックスを考慮）
     *
     * @param world ワールド
     * @param x 移動先のX座標
     * @param z 移動先のZ座標
     * @param currentY 現在のY座標
     * @return 壁の高さ（ブロック数）、壁がない場合は0.0
     */
    private fun checkWallHeight(world: org.bukkit.World, x: Int, z: Int, currentY: Double): Double {
        val startY = currentY.toInt()

        // エンティティタイプに応じたヒットボックスの幅を取得
        val hitboxWidth = getEntityHitboxWidth()
        val hitboxHeight = getEntityHitboxHeight()

        // ヒットボックスの範囲を計算（半径）
        val radius = (hitboxWidth / 2.0).toInt()

        var maxWallHeight = 0.0

        // ヒットボックスの範囲内の全ブロックをチェック
        for (offsetX in -radius..radius) {
            for (offsetZ in -radius..radius) {
                val checkX = x + offsetX
                val checkZ = z + offsetZ

                // この座標での壁の高さをチェック
                var wallHeightAtPos = 0.0

                // 足元から頭の高さまでチェック
                for (i in 0..hitboxHeight.toInt() + 2) {
                    val checkY = startY + i
                    val block = world.getBlockAt(checkX, checkY, checkZ)

                    if (block.type.isSolid) {
                        wallHeightAtPos = i + 1.0
                    } else {
                        // 空気ブロックに到達したら確定
                        break
                    }
                }

                // 最も高い壁を記録
                if (wallHeightAtPos > maxWallHeight) {
                    maxWallHeight = wallHeightAtPos
                }
            }
        }

        return maxWallHeight
    }

    /**
     * エンティティタイプに応じたヒットボックスの幅を取得
     */
    private fun getEntityHitboxWidth(): Double {
        return when (entityType) {
            // 人型 (0.6)
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.PLAYER,
            EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
            EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.CREEPER,
            EntityType.ENDERMAN, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.WITCH -> 0.6

            // 小型 (0.3-0.5)
            EntityType.SILVERFISH, EntityType.ENDERMITE -> 0.4
            EntityType.CAVE_SPIDER -> 0.7
            EntityType.BEE -> 0.7
            EntityType.VEX -> 0.4
            EntityType.CHICKEN -> 0.4
            EntityType.BAT -> 0.5

            // 中型 (0.9-1.0)
            EntityType.PIG, EntityType.SHEEP -> 0.9
            EntityType.COW -> 0.9
            EntityType.WOLF -> 0.6
            EntityType.CAT, EntityType.OCELOT -> 0.6
            EntityType.FOX -> 0.6
            EntityType.PANDA -> 1.3
            EntityType.POLAR_BEAR -> 1.4
            EntityType.GOAT -> 0.9

            // 大型 (1.0-2.0)
            EntityType.SPIDER -> 1.4
            EntityType.IRON_GOLEM -> 1.4
            EntityType.VILLAGER, EntityType.WANDERING_TRADER -> 0.6
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE -> 1.39
            EntityType.LLAMA, EntityType.TRADER_LLAMA -> 0.9
            EntityType.HOGLIN, EntityType.ZOGLIN -> 1.39
            EntityType.RAVAGER -> 1.95
            EntityType.STRIDER -> 0.9
            EntityType.CAMEL -> 1.7

            // ボス級
            EntityType.GIANT -> 3.6
            EntityType.WITHER -> 0.9
            EntityType.ENDER_DRAGON -> 16.0
            EntityType.WARDEN -> 0.9

            // 飛行系
            EntityType.GHAST -> 4.0
            EntityType.PHANTOM -> 0.9
            EntityType.BLAZE -> 0.6
            EntityType.PARROT -> 0.5

            // 水棲
            EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN -> 0.85
            EntityType.SQUID, EntityType.GLOW_SQUID -> 0.8
            EntityType.DOLPHIN -> 0.9
            EntityType.TURTLE -> 1.2
            EntityType.COD, EntityType.SALMON -> 0.5
            EntityType.PUFFERFISH -> 0.7
            EntityType.TROPICAL_FISH -> 0.5
            EntityType.AXOLOTL -> 0.75

            // スライム系（サイズ1の場合）
            EntityType.SLIME, EntityType.MAGMA_CUBE -> 2.04

            // その他
            EntityType.SHULKER -> 1.0
            EntityType.SNOW_GOLEM -> 0.7
            EntityType.EVOKER, EntityType.ILLUSIONER, EntityType.VINDICATOR, EntityType.PILLAGER -> 0.6
            EntityType.ALLAY -> 0.35
            EntityType.FROG -> 0.5
            EntityType.TADPOLE -> 0.4
            EntityType.SNIFFER -> 1.9

            else -> 0.6  // デフォルト
        }
    }

    /**
     * エンティティタイプに応じたヒットボックスの高さを取得
     */
    private fun getEntityHitboxHeight(): Double {
        return when (entityType) {
            // 人型 (1.8-2.0)
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.PLAYER,
            EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED,
            EntityType.STRAY, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.ZOMBIFIED_PIGLIN -> 1.95
            EntityType.WITHER_SKELETON -> 2.4
            EntityType.CREEPER -> 1.7
            EntityType.ENDERMAN -> 2.9
            EntityType.WITCH -> 1.95

            // 小型 (0.3-0.7)
            EntityType.SILVERFISH -> 0.3
            EntityType.ENDERMITE -> 0.3
            EntityType.CAVE_SPIDER -> 0.5
            EntityType.BEE -> 0.6
            EntityType.VEX -> 0.8
            EntityType.CHICKEN -> 0.7
            EntityType.BAT -> 0.9

            // 中型 (0.9-1.4)
            EntityType.PIG -> 0.9
            EntityType.SHEEP -> 1.3
            EntityType.COW -> 1.4
            EntityType.WOLF -> 0.85
            EntityType.CAT, EntityType.OCELOT -> 0.7
            EntityType.FOX -> 0.7
            EntityType.PANDA -> 1.25
            EntityType.POLAR_BEAR -> 1.4
            EntityType.GOAT -> 1.3

            // 大型 (0.9-2.2)
            EntityType.SPIDER -> 0.9
            EntityType.IRON_GOLEM -> 2.7
            EntityType.VILLAGER, EntityType.WANDERING_TRADER -> 1.95
            EntityType.HORSE, EntityType.DONKEY, EntityType.MULE -> 1.6
            EntityType.LLAMA, EntityType.TRADER_LLAMA -> 1.87
            EntityType.HOGLIN, EntityType.ZOGLIN -> 1.4
            EntityType.RAVAGER -> 2.2
            EntityType.STRIDER -> 1.7
            EntityType.CAMEL -> 2.375

            // ボス級
            EntityType.GIANT -> 12.0
            EntityType.WITHER -> 3.5
            EntityType.ENDER_DRAGON -> 8.0
            EntityType.WARDEN -> 2.9

            // 飛行系
            EntityType.GHAST -> 4.0
            EntityType.PHANTOM -> 0.5
            EntityType.BLAZE -> 1.8
            EntityType.PARROT -> 0.9

            // 水棲
            EntityType.GUARDIAN -> 0.85
            EntityType.ELDER_GUARDIAN -> 1.99
            EntityType.SQUID, EntityType.GLOW_SQUID -> 0.8
            EntityType.DOLPHIN -> 0.6
            EntityType.TURTLE -> 0.4
            EntityType.COD -> 0.4
            EntityType.SALMON -> 0.4
            EntityType.PUFFERFISH -> 0.7
            EntityType.TROPICAL_FISH -> 0.4
            EntityType.AXOLOTL -> 0.42

            // スライム系（サイズ1の場合）
            EntityType.SLIME, EntityType.MAGMA_CUBE -> 2.04

            // その他
            EntityType.SHULKER -> 1.0
            EntityType.SNOW_GOLEM -> 1.9
            EntityType.EVOKER, EntityType.ILLUSIONER, EntityType.VINDICATOR, EntityType.PILLAGER -> 1.95
            EntityType.ALLAY -> 0.6
            EntityType.FROG -> 0.5
            EntityType.TADPOLE -> 0.3
            EntityType.SNIFFER -> 1.75

            else -> 1.8  // デフォルト
        }
    }

    /**
     * 指定位置を向く
     */
    private suspend fun lookAt(target: Location) {
        val direction = target.toVector().subtract(location.toVector()).normalize()
        location.direction = direction

        // 視線変更パケットを全ビューワーに送信
        viewers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                PacketSender.sendHeadRotationPacket(player, entityId, location.yaw)
            }
        }
    }

    /**
     * ピッチを0に戻す（ターゲットロスト時）
     */
    private suspend fun resetPitch() {
        location.pitch = 0.0f

        // ヘッドローテーションパケットを送信
        viewers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                PacketSender.sendHeadRotationPacket(player, entityId, location.yaw)
            }
        }
    }

    /**
     * bodyの向きをheadの向きに合わせる
     */
    private suspend fun syncBodyRotation() {
        // bodyローテーションパケットを送信
        viewers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                PacketSender.sendBodyRotationPacket(player, entityId, location.yaw)
            }
        }
    }

    /**
     * ダメージサウンドを再生
     */
    private fun playHurtSound() {
        val sound = getHurtSound() ?: return
        val world = location.world ?: return

        // サウンドを再生（音量1.0、ピッチ1.0）
        world.playSound(location, sound, 1.0f, 1.0f)
    }

    /**
     * 死亡サウンドを再生
     */
    private fun playDeathSound() {
        val sound = getDeathSound() ?: return
        val world = location.world ?: return

        // サウンドを再生（音量1.0、ピッチ1.0）
        world.playSound(location, sound, 1.0f, 1.0f)
    }

    /**
     * エンティティタイプに応じたダメージサウンドを取得
     */
    private fun getHurtSound(): org.bukkit.Sound? {
        return when (entityType) {
            EntityType.ZOMBIE -> org.bukkit.Sound.ENTITY_ZOMBIE_HURT
            EntityType.SKELETON -> org.bukkit.Sound.ENTITY_SKELETON_HURT
            EntityType.SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_HURT
            EntityType.CREEPER -> org.bukkit.Sound.ENTITY_CREEPER_HURT
            EntityType.ENDERMAN -> org.bukkit.Sound.ENTITY_ENDERMAN_HURT
            EntityType.BLAZE -> org.bukkit.Sound.ENTITY_BLAZE_HURT
            EntityType.WITCH -> org.bukkit.Sound.ENTITY_WITCH_HURT
            EntityType.PIGLIN -> org.bukkit.Sound.ENTITY_PIGLIN_HURT
            EntityType.WITHER_SKELETON -> org.bukkit.Sound.ENTITY_WITHER_SKELETON_HURT
            EntityType.PHANTOM -> org.bukkit.Sound.ENTITY_PHANTOM_HURT
            EntityType.HOGLIN -> org.bukkit.Sound.ENTITY_HOGLIN_HURT
            EntityType.PIGLIN_BRUTE -> org.bukkit.Sound.ENTITY_PIGLIN_BRUTE_HURT
            EntityType.IRON_GOLEM -> org.bukkit.Sound.ENTITY_IRON_GOLEM_HURT
            EntityType.VILLAGER -> org.bukkit.Sound.ENTITY_VILLAGER_HURT
            EntityType.ENDER_DRAGON -> org.bukkit.Sound.ENTITY_ENDER_DRAGON_HURT
            EntityType.WITHER -> org.bukkit.Sound.ENTITY_WITHER_HURT
            EntityType.GHAST -> org.bukkit.Sound.ENTITY_GHAST_HURT
            EntityType.SLIME -> org.bukkit.Sound.ENTITY_SLIME_HURT
            EntityType.MAGMA_CUBE -> org.bukkit.Sound.ENTITY_MAGMA_CUBE_HURT
            EntityType.SILVERFISH -> org.bukkit.Sound.ENTITY_SILVERFISH_HURT
            EntityType.CAVE_SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_HURT
            EntityType.GUARDIAN -> org.bukkit.Sound.ENTITY_GUARDIAN_HURT
            EntityType.ELDER_GUARDIAN -> org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_HURT
            EntityType.SHULKER -> org.bukkit.Sound.ENTITY_SHULKER_HURT
            EntityType.VEX -> org.bukkit.Sound.ENTITY_VEX_HURT
            EntityType.VINDICATOR -> org.bukkit.Sound.ENTITY_VINDICATOR_HURT
            EntityType.EVOKER -> org.bukkit.Sound.ENTITY_EVOKER_HURT
            EntityType.RAVAGER -> org.bukkit.Sound.ENTITY_RAVAGER_HURT
            EntityType.PILLAGER -> org.bukkit.Sound.ENTITY_PILLAGER_HURT
            EntityType.DROWNED -> org.bukkit.Sound.ENTITY_DROWNED_HURT
            EntityType.HUSK -> org.bukkit.Sound.ENTITY_HUSK_HURT
            EntityType.STRAY -> org.bukkit.Sound.ENTITY_STRAY_HURT
            EntityType.ZOMBIE_VILLAGER -> org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_HURT
            EntityType.ENDERMITE -> org.bukkit.Sound.ENTITY_ENDERMITE_HURT
            EntityType.WARDEN -> org.bukkit.Sound.ENTITY_WARDEN_HURT
            EntityType.COW -> org.bukkit.Sound.ENTITY_COW_HURT
            EntityType.PIG -> org.bukkit.Sound.ENTITY_PIG_HURT
            EntityType.SHEEP -> org.bukkit.Sound.ENTITY_SHEEP_HURT
            EntityType.CHICKEN -> org.bukkit.Sound.ENTITY_CHICKEN_HURT
            EntityType.WOLF -> org.bukkit.Sound.ENTITY_WOLF_HURT
            EntityType.CAT -> org.bukkit.Sound.ENTITY_CAT_HURT
            EntityType.HORSE -> org.bukkit.Sound.ENTITY_HORSE_HURT
            else -> null  // サウンドが定義されていないエンティティタイプ
        }
    }

    /**
     * エンティティタイプに応じた死亡サウンドを取得
     */
    private fun getDeathSound(): org.bukkit.Sound? {
        return when (entityType) {
            EntityType.ZOMBIE -> org.bukkit.Sound.ENTITY_ZOMBIE_DEATH
            EntityType.SKELETON -> org.bukkit.Sound.ENTITY_SKELETON_DEATH
            EntityType.SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_DEATH
            EntityType.CREEPER -> org.bukkit.Sound.ENTITY_CREEPER_DEATH
            EntityType.ENDERMAN -> org.bukkit.Sound.ENTITY_ENDERMAN_DEATH
            EntityType.BLAZE -> org.bukkit.Sound.ENTITY_BLAZE_DEATH
            EntityType.WITCH -> org.bukkit.Sound.ENTITY_WITCH_DEATH
            EntityType.PIGLIN -> org.bukkit.Sound.ENTITY_PIGLIN_DEATH
            EntityType.WITHER_SKELETON -> org.bukkit.Sound.ENTITY_WITHER_SKELETON_DEATH
            EntityType.PHANTOM -> org.bukkit.Sound.ENTITY_PHANTOM_DEATH
            EntityType.HOGLIN -> org.bukkit.Sound.ENTITY_HOGLIN_DEATH
            EntityType.PIGLIN_BRUTE -> org.bukkit.Sound.ENTITY_PIGLIN_BRUTE_DEATH
            EntityType.IRON_GOLEM -> org.bukkit.Sound.ENTITY_IRON_GOLEM_DEATH
            EntityType.VILLAGER -> org.bukkit.Sound.ENTITY_VILLAGER_DEATH
            EntityType.ENDER_DRAGON -> org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH
            EntityType.WITHER -> org.bukkit.Sound.ENTITY_WITHER_DEATH
            EntityType.GHAST -> org.bukkit.Sound.ENTITY_GHAST_DEATH
            EntityType.SLIME -> org.bukkit.Sound.ENTITY_SLIME_DEATH
            EntityType.MAGMA_CUBE -> org.bukkit.Sound.ENTITY_MAGMA_CUBE_DEATH
            EntityType.SILVERFISH -> org.bukkit.Sound.ENTITY_SILVERFISH_DEATH
            EntityType.CAVE_SPIDER -> org.bukkit.Sound.ENTITY_SPIDER_DEATH
            EntityType.GUARDIAN -> org.bukkit.Sound.ENTITY_GUARDIAN_DEATH
            EntityType.ELDER_GUARDIAN -> org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_DEATH
            EntityType.SHULKER -> org.bukkit.Sound.ENTITY_SHULKER_DEATH
            EntityType.VEX -> org.bukkit.Sound.ENTITY_VEX_DEATH
            EntityType.VINDICATOR -> org.bukkit.Sound.ENTITY_VINDICATOR_DEATH
            EntityType.EVOKER -> org.bukkit.Sound.ENTITY_EVOKER_DEATH
            EntityType.RAVAGER -> org.bukkit.Sound.ENTITY_RAVAGER_DEATH
            EntityType.PILLAGER -> org.bukkit.Sound.ENTITY_PILLAGER_DEATH
            EntityType.DROWNED -> org.bukkit.Sound.ENTITY_DROWNED_DEATH
            EntityType.HUSK -> org.bukkit.Sound.ENTITY_HUSK_DEATH
            EntityType.STRAY -> org.bukkit.Sound.ENTITY_STRAY_DEATH
            EntityType.ZOMBIE_VILLAGER -> org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_DEATH
            EntityType.ENDERMITE -> org.bukkit.Sound.ENTITY_ENDERMITE_DEATH
            EntityType.WARDEN -> org.bukkit.Sound.ENTITY_WARDEN_DEATH
            EntityType.COW -> org.bukkit.Sound.ENTITY_COW_DEATH
            EntityType.PIG -> org.bukkit.Sound.ENTITY_PIG_DEATH
            EntityType.SHEEP -> org.bukkit.Sound.ENTITY_SHEEP_DEATH
            EntityType.CHICKEN -> org.bukkit.Sound.ENTITY_CHICKEN_DEATH
            EntityType.WOLF -> org.bukkit.Sound.ENTITY_WOLF_DEATH
            EntityType.CAT -> org.bukkit.Sound.ENTITY_CAT_DEATH
            EntityType.HORSE -> org.bukkit.Sound.ENTITY_HORSE_DEATH
            else -> null  // サウンドが定義されていないエンティティタイプ
        }
    }

    /**
     * ダメージランキングを取得
     *
     * @param limit 取得する順位数（デフォルト: 10位まで）
     * @return ランキング（UUID と累計ダメージのペアリスト）
     */
    fun getDamageRanking(limit: Int = 10): List<Pair<UUID, Double>> {
        return damageTracker.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    /**
     * プレイヤーの累計ダメージを取得
     *
     * @param player プレイヤー
     * @return 累計ダメージ
     */
    fun getPlayerDamage(player: Player): Double {
        return damageTracker.getOrDefault(player.uniqueId, 0.0)
    }

    /**
     * プレイヤーのランキング順位を取得
     *
     * @param player プレイヤー
     * @return 順位（1位から）、ランキング外の場合-1
     */
    fun getPlayerRank(player: Player): Int {
        val sorted = damageTracker.entries.sortedByDescending { it.value }
        val rank = sorted.indexOfFirst { it.key == player.uniqueId }
        return if (rank >= 0) rank + 1 else -1
    }

    /**
     * ダメージランキングをクリア
     */
    fun clearDamageRanking() {
        damageTracker.clear()
    }

    /**
     * ダメージランキングを表示（チャット）
     *
     * @param limit 表示する順位数
     */
    fun broadcastDamageRanking(limit: Int = 10) {
        val ranking = getDamageRanking(limit)
        if (ranking.isEmpty()) return

        val messages = mutableListOf<String>()
        messages.add("§6§l════════════════════════════")
        messages.add("§e§l  $mobName §7討伐ランキング")
        messages.add("§6§l════════════════════════════")

        ranking.forEachIndexed { index, (uuid, damage) ->
            val player = Bukkit.getPlayer(uuid)
            val name = player?.name ?: "Unknown"
            val medal = when (index) {
                0 -> "§6§l🥇"
                1 -> "§7§l🥈"
                2 -> "§c§l🥉"
                else -> "§f${index + 1}位"
            }
            messages.add("  $medal §f$name §7- §e${damage.toInt()} §7ダメージ")
        }

        messages.add("§6§l════════════════════════════")

        // 近くのプレイヤーにブロードキャスト
        location.world?.players?.forEach { player ->
            if (player.location.distance(location) <= 64.0) {
                messages.forEach { player.sendMessage(it) }
            }
        }
    }

    /**
     * 攻撃実行
     */
    private suspend fun performAttack(target: Entity) {
        DebugLogger.debug("$mobName attacks ${(target as? Player)?.name ?: target.type.name}")

        // MobInstance取得
        val instance = Unique.instance.mobManager.getMobInstance(this)
        val damageStr = instance?.definition?.damage
        val damage = if (damageStr == null || damageStr.equals("null", ignoreCase = true)) {
            5.0
        } else {
            damageStr.toDoubleOrNull() ?: 5.0
        }

        // 攻撃イベント発火＆キャンセルチェック
        val attackEvent = EventUtil.callEventOrNull(PacketMobAttackEvent(this, target, damage)) ?: run {
            DebugLogger.verbose("$mobName attack cancelled by event")
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

        // 攻撃アニメーション再生（エンティティタイプに応じた特殊アニメーション）
        playAttackAnimation()

        // OnAttackスキルトリガー実行
        instance?.let {
            Unique.instance.mobManager.executeSkillTriggers(
                this,
                it.definition.skills.onAttack,
                PacketMobSkillEvent.SkillTriggerType.ON_ATTACK
            )
        }
    }

    /**
     * 攻撃アニメーションを再生（エンティティタイプに応じた特殊アニメーション）
     */
    private suspend fun playAttackAnimation() {
        viewers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                PacketSender.sendAttackAnimationPacket(player, entityId, entityType)
            }
        }
    }

    /**
     * プレイヤーキル処理
     *
     * @param player 殺されたプレイヤー
     * @param instance MobInstance
     */
    private fun handlePlayerKill(player: Player, instance: com.github.azuazu3939.unique.mob.MobInstance?) {
        // YML設定でキラー設定が有効かチェック
        val setKillerEnabled = instance?.definition?.options?.setAsKiller ?: true

        // PacketMobKillPlayerイベント発火
        val killEvent = EventUtil.callEventOrNull(
            PacketMobKillPlayerEvent(this, player, setKillerEnabled)
        )

        if (killEvent != null && killEvent.setKiller) {
            // プレイヤーのキラーとして設定（死亡メッセージ用）
            // Note: PacketMobは実際のEntityではないため、死亡メッセージは手動で設定
            val deathMessage = "§c${player.name} §7was slain by §e$mobName"

            // ブロードキャスト
            player.world.players.forEach { p ->
                if (p.location.distance(player.location) <= 64.0) {
                    p.sendMessage(deathMessage)
                }
            }

            DebugLogger.info("$mobName killed ${player.name}")
        }
    }

    /**
     * ランダムな徘徊位置を取得
     */
    private fun getRandomWanderLocation(): Location {
        val randomX = (Math.random() - 0.5) * 10
        val randomZ = (Math.random() - 0.5) * 10
        return location.clone().add(randomX, 0.0, randomZ)
    }

    /**
     * 範囲内かチェック
     */
    private fun isInRange(entity: Entity, range: Double): Boolean {
        return location.distance(entity.location) <= range
    }

    /**
     * ビルダー
     */
    class Builder(
        private val entityId: Int,
        private val uuid: UUID,
        private val entityType: EntityType,
        private val location: Location,
        private val mobName: String
    ) {
        private var health: Double = 20.0
        private var maxHealth: Double = 20.0
        private var armor: Double = 0.0
        private var armorToughness: Double = 0.0
        private var damage: Double = 1.0
        private var damageFormula: String? = null
        private var customNameVisible: Boolean = true
        private var hasAI: Boolean = true
        private var hasGravity: Boolean = true
        private var isGlowing: Boolean = false
        private var isInvisible: Boolean = false

        // オプション設定
        private var options: MobOptions = MobOptions()

        // AI関連
        private var movementSpeed: Double = 0.1  // 2ブロック/秒（0.1 * 20tick/秒）
        private var followRange: Double = 16.0
        private var attackRange: Double = 2.0
        private var attackCooldown: Int = 20
        private var targetSearchInterval: Int = 20
        private var knockbackResistance: Double = 0.0
        private var lookAtMovementDirection: Boolean = true
        private var wallClimbHeight: Double = 1.0

        fun health(health: Double) = apply { this.health = health }
        fun maxHealth(maxHealth: Double) = apply { this.maxHealth = maxHealth }
        fun armor(armor: Double) = apply { this.armor = armor.coerceIn(0.0, 30.0) }
        fun armorToughness(toughness: Double) = apply { this.armorToughness = toughness.coerceIn(0.0, 20.0) }
        fun damage(damage: Double) = apply { this.damage = damage.coerceAtLeast(0.0) }
        fun damageFormula(formula: String?) = apply { this.damageFormula = formula }
        fun customNameVisible(visible: Boolean) = apply { this.customNameVisible = visible }
        fun hasAI(ai: Boolean) = apply { this.hasAI = ai }
        fun hasGravity(gravity: Boolean) = apply { this.hasGravity = gravity }
        fun glowing(glowing: Boolean) = apply { this.isGlowing = glowing }
        fun invisible(invisible: Boolean) = apply { this.isInvisible = invisible }

        // オプション設定
        fun options(options: MobOptions) = apply { this.options = options }

        // AI設定
        fun movementSpeed(speed: Double) = apply { this.movementSpeed = speed }
        fun followRange(range: Double) = apply { this.followRange = range }
        fun attackRange(range: Double) = apply { this.attackRange = range }
        fun attackCooldown(cooldown: Int) = apply { this.attackCooldown = cooldown }
        fun targetSearchInterval(interval: Int) = apply { this.targetSearchInterval = interval }
        fun knockbackResistance(resistance: Double) = apply { this.knockbackResistance = resistance }
        fun lookAtMovementDirection(enabled: Boolean) = apply { this.lookAtMovementDirection = enabled }
        fun wallClimbHeight(height: Double) = apply { this.wallClimbHeight = height.coerceAtLeast(0.0) }

        fun build(): PacketMob {
            val mob = PacketMob(entityId, uuid, entityType, location, mobName)
            mob.health = health
            mob.maxHealth = maxHealth
            mob.armor = armor
            mob.armorToughness = armorToughness
            mob.damage = damage
            mob.damageFormula = damageFormula
            mob.customNameVisible = customNameVisible
            mob.hasAI = hasAI
            mob.hasGravity = hasGravity
            mob.isGlowing = isGlowing
            mob.isInvisible = isInvisible

            // オプション設定
            mob.options = options

            // AI設定
            mob.movementSpeed = movementSpeed
            mob.followRange = followRange
            mob.attackRange = attackRange
            mob.attackCooldown = attackCooldown
            mob.targetSearchInterval = targetSearchInterval
            mob.knockbackResistance = knockbackResistance
            mob.lookAtMovementDirection = lookAtMovementDirection
            mob.wallClimbHeight = wallClimbHeight

            return mob
        }
    }


    companion object {
        /**
         * ビルダーを作成
         */
        fun builder(
            entityId: Int,
            uuid: UUID,
            entityType: EntityType,
            location: Location,
            mobName: String
        ): Builder {
            return Builder(entityId, uuid, entityType, location, mobName)
        }
    }
}
