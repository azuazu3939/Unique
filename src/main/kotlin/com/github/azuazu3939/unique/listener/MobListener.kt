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
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaZ * deltaZ)

        // 距離が0の場合はノックバックなし
        if (distance < 0.01) {
            return
        }

        // 正規化して方向ベクトルを取得
        val dirX = deltaX / distance
        val dirZ = deltaZ / distance

        // ノックバック強度（Minecraftのデフォルト値）
        val baseKnockbackStrength = 0.4

        // knockbackResistanceを考慮（0.0 = ノックバックなし, 1.0 = 完全にノックバック）
        val knockbackResistance = packetMob.knockbackResistance
        val actualKnockback = baseKnockbackStrength * (1.0 - knockbackResistance)

        // ノックバックが無効化されている場合はスキップ
        if (actualKnockback <= 0.0) {
            return
        }

        // ノックバック移動量を計算（水平 + 垂直）
        val knockbackX = dirX * actualKnockback
        val knockbackY = 0.2  // 垂直方向のノックバック
        val knockbackZ = dirZ * actualKnockback

        // PacketMobを移動
        packetMob.move(knockbackX, knockbackY, knockbackZ)

        DebugLogger.verbose("Applied knockback to ${packetMob.mobName}: ($knockbackX, $knockbackY, $knockbackZ)")
    }

    /**
     * プレイヤー参加時
     * 周辺のパケットエンティティを自動スポーン
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        DebugLogger.debug("Player joined: ${player.name}")

        // TODO: マナ/MPシステムが有効な場合のみプレイヤーデータを作成
        // plugin.playerDataManager.getPlayerData(player)

        // 周辺のパケットエンティティを表示
        val config = plugin.configManager.mainConfig.performance
        if (config.autoSpawnOnJoin) {
            plugin.launch {
                plugin.packetEntityManager.autoSpawnForPlayer(player, config.viewDistance)
                DebugLogger.verbose("Auto-spawned entities for ${player.name} (range: ${config.viewDistance})")
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

        DebugLogger.debug("Player quit: ${player.name}")

        // TODO: マナ/MPシステムが有効な場合のみプレイヤーデータを削除
        // plugin.playerDataManager.removePlayerData(player)

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
        if (event.isCancelled) {
            DebugLogger.debug("PacketMob spawn cancelled: ${event.mobName}")
            return
        }

        DebugLogger.verbose("PacketMob spawned: ${event.mobName} at ${event.location}")
    }

    /**
     * PacketMobダメージイベント
     *
     * ダメージ値を変更したり、ダメージをキャンセルできます。
     * 例: 防具システム、ダメージ軽減バフ、無敵モード等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobDamage(event: PacketMobDamageEvent) {
        if (event.isCancelled) {
            DebugLogger.verbose("PacketMob damage cancelled")
            return
        }

        val damagerName = (event.damager as? Player)?.name ?: event.damager?.type?.name ?: "Unknown"
        DebugLogger.verbose("PacketMob damaged: ${event.mob.mobName} took ${event.damage} damage from $damagerName")
    }

    /**
     * PacketMob死亡イベント
     *
     * ドロップリストを変更したり、カスタム報酬を追加できます。
     * 例: 経験値、クエスト進行、統計記録等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobDeath(event: PacketMobDeathEvent) {
        val killerName = event.killer?.name ?: "Environment"
        DebugLogger.debug("PacketMob died: ${event.mob.mobName} killed by $killerName")

        // ダメージランキングを表示（設定で有効な場合のみ）
        if (event.mob.options.showDamageRanking) {
            event.mob.broadcastDamageRanking(limit = 10)
        }

        // 例: カスタムドロップを追加
        // event.drops.add(ItemStack(Material.DIAMOND, 1))
    }

    /**
     * PacketMob攻撃イベント
     *
     * ダメージ値を変更したり、攻撃をキャンセルできます。
     * 例: ダメージブースト、攻撃無効化、カウンター等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobAttack(event: PacketMobAttackEvent) {
        if (event.isCancelled) {
            DebugLogger.verbose("PacketMob attack cancelled")
            return
        }

        val targetName = (event.target as? Player)?.name ?: event.target.type.name
        DebugLogger.verbose("PacketMob attacks: ${event.mob.mobName} -> $targetName (${event.damage} damage)")
    }

    /**
     * PacketMobターゲット変更イベント
     *
     * ターゲットを変更したり、ターゲット変更をキャンセルできます。
     * 例: タンク/アグロシステム、特定プレイヤー保護等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobTarget(event: PacketMobTargetEvent) {
        if (event.isCancelled) {
            DebugLogger.verbose("PacketMob target change cancelled")
            return
        }

        val oldName = (event.oldTarget as? Player)?.name ?: "None"
        val newName = (event.newTarget as? Player)?.name ?: "None"
        DebugLogger.verbose("PacketMob target changed: ${event.mob.mobName} ($oldName -> $newName)")
    }

    /**
     * PacketMobスキル使用イベント
     *
     * スキル使用をキャンセルしたり、カスタム処理を追加できます。
     * 例: スキルクールダウン、マナコスト、カウンター等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobSkill(event: PacketMobSkillEvent) {
        if (event.isCancelled) {
            DebugLogger.verbose("PacketMob skill cancelled: ${event.skillName}")
            return
        }

        DebugLogger.verbose("PacketMob skill used: ${event.mob.mobName} -> ${event.skillName} (${event.trigger})")
    }

    /**
     * PacketMob削除イベント
     *
     * 削除理由を記録したり、クリーンアップ処理を追加できます。
     * 例: 統計記録、リソース解放等
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPacketMobRemove(event: PacketMobRemoveEvent) {
        DebugLogger.verbose("PacketMob removed: ${event.mob.mobName} (${event.reason})")
    }
}