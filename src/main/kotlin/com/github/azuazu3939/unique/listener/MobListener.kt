package com.github.azuazu3939.unique.listener

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.event.*
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.launch
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

    /**
     * プレイヤー参加時
     * 周辺のパケットエンティティを自動スポーン
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        DebugLogger.debug("Player joined: ${player.name}")

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