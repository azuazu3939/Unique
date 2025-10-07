package com.github.azuazu3939.unique.listener

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.event.*
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.sqrt

/**
 * Mobイベントリスナー
 *
 * Mobとプレイヤーのインタラクションを処理
 */
class MobListener(private val plugin: Unique) : Listener {

    init {
        // PacketEventsリスナーを登録
        PacketEvents.getAPI().eventManager.registerListener(PacketAttackListener())
    }

    /**
     * PacketEvents用の攻撃パケットリスナー
     */
    inner class PacketAttackListener : PacketListenerAbstract() {
        override fun onPacketReceive(event: PacketReceiveEvent) {
            // InteractEntityパケット（エンティティへの攻撃・右クリック）
            if (event.packetType != PacketType.Play.Client.INTERACT_ENTITY) return

            val player = event.getPlayer<Player>() ?: return
            val wrapper = WrapperPlayClientInteractEntity(event)

            // 攻撃アクションのみ処理
            if (wrapper.action != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return

            val entityId = wrapper.entityId

            // PacketMobを検索
            val packetMob = plugin.packetEntityManager.getEntityByEntityId(entityId) as? PacketMob ?: return

            // イベントをキャンセル（Bukkitに処理させない）
            event.isCancelled = true

            // ダメージ処理
            plugin.launch {
                withContext(plugin.entityDispatcher(player)) {
                    val damage = calculatePlayerDamage(player)
                    packetMob.damage(damage, player)

                    // PacketMobにノックバックを適用
                    applyKnockbackToPacketMob(player, packetMob)

                    DebugLogger.verbose("${player.name} attacked ${packetMob.mobName} for $damage damage")
                }
            }
        }
    }

    /**
     * プレイヤーのダメージを計算
     */
    private fun calculatePlayerDamage(player: Player): Double {
        val item = player.inventory.itemInMainHand
        // 武器の種類に応じたダメージ
        val baseDamage = when {
            item.type.name.contains("SWORD") -> 7.0
            item.type.name.contains("AXE") -> 9.0
            item.type.name.contains("PICKAXE") -> 5.0
            item.type.name.contains("SHOVEL") -> 4.0
            else -> 1.0  // 素手
        }
        // TODO: エンチャント、属性、ポーション効果などを考慮
        return baseDamage
    }

    /**
     * PacketMobにノックバックを適用
     */
    private suspend fun applyKnockbackToPacketMob(player: Player, packetMob: PacketMob) {
        // プレイヤーからPacketMobへの方向ベクトルを計算
        val playerLocation = player.location
        val mobLocation = packetMob.location

        val deltaX = mobLocation.x - playerLocation.x
        val deltaZ = mobLocation.z - playerLocation.z

        // 水平距離を計算
        val distance = sqrt(deltaX * deltaX + deltaZ * deltaZ)

        // 距離が0の場合はノックバックなし
        if (distance < 0.01) {
            return
        }

        // 正規化して方向ベクトルを取得
        val dirX = deltaX / distance
        val dirZ = deltaZ / distance

        // ノックバック強度を計算
        var knockbackStrength = 0.4  // 基本ノックバック

        // スプリント攻撃判定
        val isSprinting = player.isSprinting
        if (isSprinting) {
            knockbackStrength += 1.0  // スプリント攻撃でバニラ相当の増加
        }

        // ノックバックエンチャント判定
        val item = player.inventory.itemInMainHand
        if (item.hasItemMeta()) {
            val knockbackLevel = item.itemMeta.enchants[org.bukkit.enchantments.Enchantment.KNOCKBACK] ?: 0
            if (knockbackLevel > 0) {
                knockbackStrength += knockbackLevel * 0.4  // レベルごとに+0.4
            }
        }

        // knockbackResistanceを考慮（0.0 = 完全にノックバック, 1.0 = ノックバックなし）
        val knockbackResistance = packetMob.knockbackResistance
        val actualKnockback = knockbackStrength * (1.0 - knockbackResistance)

        // ノックバックが無効化されている場合はスキップ
        if (actualKnockback <= 0.0) {
            return
        }

        // ノックバック移動量を計算（水平 + 垂直）
        val knockbackX = dirX * actualKnockback
        // スプリント攻撃の場合はY軸ノックバックなし（水平方向のみ）
        val knockbackY = if (isSprinting) 0.0 else 0.35
        val knockbackZ = dirZ * actualKnockback

        // PacketMobに速度を追加（move()ではなくvelocityとして追加）
        packetMob.addVelocity(knockbackX, knockbackY, knockbackZ)
    }

    /**
     * プレイヤー参加時
     * 周辺のパケットエンティティを自動スポーン
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // 周辺のパケットエンティティを表示
        val config = plugin.configManager.mainConfig.performance
        if (config.autoSpawnOnJoin) {
            plugin.launch {
                plugin.packetEntityManager.autoSpawnForPlayer(player, config.viewDistance)
                plugin.packetEntityManager.autoDespawnForPlayer(player, config.viewDistance * 2)
            }
        }
    }

    /**
     * プレイヤー退出時
     * プレイヤーのビューワー情報をクリーンアップ
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // パケットエンティティのビューワーから削除
        val entities = plugin.packetEntityManager.getEntitiesViewedBy(player)
        entities.forEach { entity ->
            entity.removeViewer(player)
        }
    }


    // ========================================
    // PacketMobカスタムイベント
    // ========================================

    /**
     * PacketMobスポーンイベント
     *
     * 他のプラグインがこのイベントをリッスンして、
     * スポーンをキャンセルしたり、カスタム処理を追加できます。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobSpawn(event: PacketMobSpawnEvent) {
        // イベント処理（必要に応じて他プラグインが使用）
    }

    /**
     * PacketMobダメージイベント
     *
     * ダメージ値を変更したり、ダメージをキャンセルできます。
     * 例: 防具システム、ダメージ軽減バフ、無敵モード等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobDamage(event: PacketMobDamageEvent) {
        // イベント処理（必要に応じて他プラグインが使用）
    }

    /**
     * PacketMob死亡イベント
     *
     * ドロップリストを変更したり、カスタム報酬を追加できます。
     * 例: 経験値、クエスト進行、統計記録等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobDeath(event: PacketMobDeathEvent) {
        // ダメージランキングを表示（設定で有効な場合のみ）
        if (event.mob.options.showDamageRanking) {
            event.mob.broadcastDamageRanking()
        }
    }

    /**
     * PacketMob攻撃イベント
     *
     * ダメージ値を変更したり、攻撃をキャンセルできます。
     * 例: ダメージブースト、攻撃無効化、カウンター等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobAttack(event: PacketMobAttackEvent) {
        // イベント処理（必要に応じて他プラグインが使用）
    }

    /**
     * PacketMobターゲット変更イベント
     *
     * ターゲットを変更したり、ターゲット変更をキャンセルできます。
     * 例: タンク/アグロシステム、特定プレイヤー保護等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobTarget(event: PacketMobTargetEvent) {
        // イベント処理（必要に応じて他プラグインが使用）
    }

    /**
     * PacketMobスキル使用イベント
     *
     * スキル使用をキャンセルしたり、カスタム処理を追加できます。
     * 例: スキルクールダウン、マナコスト、カウンター等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobSkill(event: PacketMobSkillEvent) {
        // イベント処理（必要に応じて他プラグインが使用）
    }

    /**
     * PacketMob削除イベント
     *
     * 削除理由を記録したり、クリーンアップ処理を追加できます。
     * 例: 統計記録、リソース解放等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobRemove(event: PacketMobRemoveEvent) {
        // イベント処理（必要に応じて他プラグインが使用）
    }
}