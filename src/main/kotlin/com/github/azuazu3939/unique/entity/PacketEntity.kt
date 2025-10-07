package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.util.DebugLogger
import kotlinx.coroutines.Job
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * パケットエンティティ基底クラス
 *
 * 実体を持たないエンティティをパケットで表現
 * 非同期操作が可能で、サーバー負荷が軽い
 */
abstract class PacketEntity(
    val entityId: Int,
    val uuid: UUID,
    val entityType: EntityType,
    initialLocation: Location
) {

    /**
     * 現在の座標
     */
    var location: Location = initialLocation.clone()
        protected set

    /**
     * 体力
     */
    var health: Double = 20.0

    /**
     * 最大体力
     */
    var maxHealth: Double = 20.0

    /**
     * 死亡フラグ
     */
    var isDead: Boolean = false

    /**
     * 生存時間（tick）
     */
    var ticksLived: Int = 0
        protected set

    /**
     * 死亡した時刻（tick）
     */
    var deathTick: Int = -1

    /**
     * このエンティティを見ているプレイヤー
     */
    val viewers = ConcurrentHashMap.newKeySet<UUID>()!!

    /**
     * アクティブなコルーチンジョブ
     */
    protected val activeJobs = ConcurrentHashMap<String, Job>()

    /**
     * カスタムメタデータ
     */
    protected val metadata = ConcurrentHashMap<String, Any>()

    /**
     * エンティティをスポーン（プレイヤーに表示）
     */
    abstract suspend fun spawn(player: Player)

    /**
     * エンティティを削除（プレイヤーから非表示）
     */
    abstract suspend fun despawn(player: Player)

    /**
     * すべてのビューワーに対してスポーン
     */
    suspend fun spawnForAll(players: Collection<Player>) {
        for (player in players) {
            spawn(player)
        }
    }

    /**
     * すべてのビューワーに対してデスポーン
     */
    suspend fun despawnForAll(players: Collection<Player>) {
        for (player in players) {
            despawn(player)
        }
    }

    suspend fun despawnForAll(players: Set<UUID>) {
        for (player in players) {
            val p = Bukkit.getPlayer(player) ?: continue
            despawn(p)
        }
    }

    /**
     * エンティティを移動
     */
    abstract suspend fun teleport(newLocation: Location)

    /**
     * 相対移動
     */
    abstract suspend fun move(deltaX: Double, deltaY: Double, deltaZ: Double)

    /**
     * メタデータを更新
     */
    abstract suspend fun updateMetadata()

    /**
     * アニメーションを再生
     */
    abstract suspend fun playAnimation(animation: EntityAnimation)

    /**
     * ダメージを受ける
     */
    open suspend fun damage(amount: Double) {
        if (isDead) return

        health = (health - amount).coerceAtLeast(0.0)

        if (health <= 0.0) {
            kill()
        }

        // NOTE: ダメージアニメーションはサブクラスで制御
        // サブクラス（PacketMob等）で条件付きでplayAnimation(DAMAGE)を呼ぶ
        updateMetadata()
    }

    /**
     * 回復
     */
    open suspend fun heal(amount: Double) {
        if (isDead) return

        health = (health + amount).coerceAtMost(maxHealth)
        updateMetadata()
    }

    /**
     * エンティティを殺す
     */
    open suspend fun kill() {
        if (isDead) return

        isDead = true
        health = 0.0
        deathTick = ticksLived  // 死亡時刻を記録

        // 死亡アニメーション
        playAnimation(EntityAnimation.DEATH)

        // 注：activeJobsのキャンセルはcleanup()で行う
        // これにより、OnDeathスキルなどが起動したジョブがキャンセルされるのを防ぐ

        DebugLogger.debug("PacketEntity killed: $entityId ($entityType), deathTick=$deathTick")
    }

    /**
     * ビューワーを追加
     */
    fun addViewer(player: Player) {
        viewers.add(player.uniqueId)
    }

    /**
     * ビューワーを削除
     */
    fun removeViewer(player: Player) {
        viewers.remove(player.uniqueId)
    }

    /**
     * ビューワーのリストを取得
     */
    fun getViewers(): Set<UUID> {
        return viewers.toSet()
    }

    /**
     * プレイヤーがビューワーかチェック
     */
    fun isViewer(player: Player): Boolean {
        return viewers.contains(player.uniqueId)
    }

    /**
     * カスタムメタデータを設定
     */
    fun setMetadata(key: String, value: Any) {
        metadata[key] = value
    }

    /**
     * カスタムメタデータを取得
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getMetadata(key: String): T? {
        return metadata[key] as? T
    }

    /**
     * カスタムメタデータを削除
     */
    fun removeMetadata(key: String) {
        metadata.remove(key)
    }

    /**
     * カスタムメタデータの存在確認
     */
    fun hasMetadata(key: String): Boolean {
        return metadata.containsKey(key)
    }

    /**
     * ダメージを受けられるかどうか
     * PacketMob: true
     * PacketDisplay: false
     */
    open fun canTakeDamage(): Boolean = true

    /**
     * 更新処理（1tick毎に呼ばれる）
     */
    open fun tick() {
        ticksLived++  // 死亡後もカウントを続ける（クリーンアップ判定に必要）
        if (isDead) return  // 死亡後はAI等の処理をスキップ
    }

    /**
     * クリーンアップ
     */
    open suspend fun cleanup() {
        DebugLogger.info("Cleaning up PacketEntity: $entityId (type=$entityType, viewers=${viewers.size})")

        despawnForAll(viewers.toSet())

        // ジョブをキャンセル
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()

        // ビューワーをクリア
        viewers.clear()

        DebugLogger.info("PacketEntity cleaned up: $entityId")
    }
}

/**
 * エンティティアニメーション
 */
enum class EntityAnimation {
    DAMAGE,
    DEATH,
    SWING_MAIN_HAND,
    LEAVE_BED,
    SWING_OFF_HAND,
    CRITICAL_HIT,
    MAGIC_CRITICAL_HIT,
    ATTACK  // エンティティタイプ固有の攻撃アニメーション
}