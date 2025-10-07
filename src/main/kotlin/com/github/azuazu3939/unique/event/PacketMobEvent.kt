package com.github.azuazu3939.unique.event

import com.github.azuazu3939.unique.entity.PacketMob
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * PacketMob関連イベントの基底クラス
 */
abstract class PacketMobEvent(
    val mob: PacketMob,
    isAsync: Boolean = false
) : Event(isAsync) {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMobスポーンイベント
 *
 * PacketMobがスポーンされた時に発火
 * キャンセル可能（スポーン阻止）
 */
class PacketMobSpawnEvent(
    mob: PacketMob,
    val location: Location,
    val mobName: String
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMobダメージイベント
 *
 * PacketMobがダメージを受けた時に発火
 * キャンセル可能（ダメージ無効化）
 */
class PacketMobDamageEvent(
    mob: PacketMob,
    val damager: Entity?,
    var damage: Double,
    val cause: DamageCause = DamageCause.ENTITY_ATTACK
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * ダメージ原因
     */
    enum class DamageCause {
        ENTITY_ATTACK,      // エンティティ攻撃
        PLAYER_ATTACK,      // プレイヤー攻撃
        SKILL,              // スキル
        ENVIRONMENT,        // 環境（溶岩、落下等）
        CUSTOM              // カスタム
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMob死亡イベント
 *
 * PacketMobが死亡した時に発火
 * キャンセル不可（死亡を防げない）
 */
class PacketMobDeathEvent(
    mob: PacketMob,
    val killer: Player?,
    val drops: MutableList<org.bukkit.inventory.ItemStack> = mutableListOf()
) : PacketMobEvent(mob) {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMob攻撃イベント
 *
 * PacketMobが攻撃した時に発火
 * キャンセル可能（攻撃阻止）
 */
class PacketMobAttackEvent(
    mob: PacketMob,
    val target: Entity,
    var damage: Double
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMobターゲット変更イベント
 *
 * PacketMobのターゲットが変更された時に発火
 * キャンセル可能（ターゲット変更阻止）
 */
class PacketMobTargetEvent(
    mob: PacketMob,
    val reason: TargetReason = TargetReason.CLOSEST_PLAYER
) : PacketMobEvent(mob, true), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * ターゲット変更理由
     */
    enum class TargetReason {
        CLOSEST_PLAYER,     // 最も近いプレイヤー
        ATTACKED,           // 攻撃された
        FORGOT,             // ターゲットを忘れた
        CUSTOM              // カスタム
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMobスキル使用イベント
 *
 * PacketMobがスキルを使用した時に発火
 * キャンセル可能（スキル使用阻止）
 */
class PacketMobSkillEvent(
    mob: PacketMob,
    val skillName: String,
    val trigger: SkillTriggerType
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * スキルトリガータイプ
     */
    enum class SkillTriggerType {
        ON_SPAWN,
        ON_ATTACK,
        ON_DAMAGED,
        ON_DEATH,
        ON_TIMER
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMob削除イベント
 *
 * PacketMobが削除される時に発火
 * キャンセル不可
 */
class PacketMobRemoveEvent(
    mob: PacketMob,
    val reason: RemoveReason = RemoveReason.DEATH
) : PacketMobEvent(mob) {

    /**
     * 削除理由
     */
    enum class RemoveReason {
        DEATH,          // 死亡
        DESPAWN,        // デスポーン
        UNLOAD,         // チャンクアンロード
        PLUGIN          // プラグイン指定
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * PacketMobプレイヤーキルイベント
 *
 * PacketMobがプレイヤーを殺した時に発火
 * キャンセル可能（キラー設定を阻止）
 */
class PacketMobKillPlayerEvent(
    mob: PacketMob,
    val player: Player,
    var setKiller: Boolean = true
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
