package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.entity.packet.PacketSender
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*

/**
 * Display Entity種別
 *
 * 1.19.4+で追加されたDisplay Entity
 * https://minecraft.fandom.com/wiki/Display
 */
enum class DisplayType {
    TEXT,       // テキスト表示
    ITEM,       // アイテム表示
    BLOCK       // ブロック表示
}

/**
 * Display Entity用パケットエンティティ
 *
 * ダメージを受けず、AIも持たない装飾専用エンティティ
 * 主にホログラム、看板、装飾などに使用
 */
class PacketDisplayEntity(
    entityId: Int,
    uuid: UUID,
    location: Location,
    val displayType: DisplayType,
    val displayName: String
) : PacketEntity(
    entityId,
    uuid,
    when (displayType) {
        DisplayType.TEXT -> EntityType.TEXT_DISPLAY
        DisplayType.ITEM -> EntityType.ITEM_DISPLAY
        DisplayType.BLOCK -> EntityType.BLOCK_DISPLAY
    },
    location
) {

    /**
     * 表示テキスト（TEXT_DISPLAY用）
     */
    var text: String = ""
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { playerId ->
                org.bukkit.Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * アイテム（ITEM_DISPLAY用）
     */
    var itemStack: org.bukkit.inventory.ItemStack? = null
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { playerId ->
                org.bukkit.Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * ブロックデータ（BLOCK_DISPLAY用）
     */
    var blockData: org.bukkit.block.data.BlockData? = null
        set(value) {
            field = value
            // メタデータ更新パケット送信
            viewers.forEach { playerId ->
                org.bukkit.Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * 拡大率
     */
    var scale: Float = 1.0f
        set(value) {
            field = value
            viewers.forEach { playerId ->
                org.bukkit.Bukkit.getPlayer(playerId)?.let { player ->
                    sendMetadataPacket(player)
                }
            }
        }

    /**
     * 表示範囲（ブロック単位）
     */
    var viewRange: Float = 1.0f

    /**
     * 影の半径
     */
    var shadowRadius: Float = 0.0f

    /**
     * 影の強度
     */
    var shadowStrength: Float = 1.0f

    /**
     * 背景色（TEXT_DISPLAY用）ARGB形式
     */
    var backgroundColor: Int = 0x40000000

    /**
     * テキストの不透明度（TEXT_DISPLAY用）
     */
    var textOpacity: Byte = -1

    /**
     * ビルボードモード
     * FIXED: 固定
     * VERTICAL: 垂直回転のみ
     * HORIZONTAL: 水平回転のみ
     * CENTER: 完全ビルボード（常にプレイヤーを向く）
     */
    var billboardMode: BillboardMode = BillboardMode.FIXED

    /**
     * Display Entityはダメージを受けない
     */
    override fun canTakeDamage(): Boolean = false

    /**
     * スポーン処理
     */
    override suspend fun spawn(player: Player) {
        try {
            PacketSender.sendSpawnDisplayPacket(player, this)
            addViewer(player)
            DebugLogger.verbose("Spawned $displayType display for ${player.name}")
        } catch (e: Exception) {
            DebugLogger.error("Failed to spawn display entity for ${player.name}", e)
        }
    }

    /**
     * デスポーン処理
     */
    override suspend fun despawn(player: Player) {
        try {
            PacketSender.sendDespawnPacket(player, entityId)
            removeViewer(player)
            DebugLogger.verbose("Despawned $displayType display for ${player.name}")

        } catch (e: Exception) {
            DebugLogger.error("Failed to despawn display entity for ${player.name}", e)
        }
    }

    /**
     * テレポート
     */
    override suspend fun teleport(newLocation: Location) {
        location = newLocation.clone()
        viewers.forEach { playerId ->
            org.bukkit.Bukkit.getPlayer(playerId)?.let { player ->
                PacketSender.sendTeleportPacket(player, entityId, newLocation)
            }
        }
        DebugLogger.verbose("Teleported $displayType display to $newLocation")
    }

    /**
     * 相対移動
     */
    override suspend fun move(deltaX: Double, deltaY: Double, deltaZ: Double) {
        location.add(deltaX, deltaY, deltaZ)
        viewers.forEach { playerId ->
            org.bukkit.Bukkit.getPlayer(playerId)?.let { player ->
                PacketSender.sendRelativeMovePacket(player, entityId, deltaX, deltaY, deltaZ)
            }
        }
    }

    /**
     * メタデータ更新
     */
    override suspend fun updateMetadata() {
        viewers.forEach { playerId ->
            org.bukkit.Bukkit.getPlayer(playerId)?.let { player ->
                sendMetadataPacket(player)
            }
        }
    }

    /**
     * アニメーション再生（Display Entityは未サポート）
     */
    override suspend fun playAnimation(animation: EntityAnimation) {
        // Display Entityはアニメーションをサポートしない
        DebugLogger.verbose("Animation not supported for display entities")
    }

    /**
     * ダメージ処理（オーバーライド - 何もしない）
     */
    override suspend fun damage(amount: Double) {
        // Display Entityはダメージを受けない
        DebugLogger.verbose("$displayName cannot take damage (Display Entity)")
    }

    /**
     * メタデータパケット送信
     */
    private fun sendMetadataPacket(player: Player?) {
        if (player == null) return
        try {
            PacketSender.sendDisplayMetadataPacket(player, this)
        } catch (e: Exception) {
            DebugLogger.error("Failed to send metadata packet to ${player.name}", e)
        }
    }

    /**
     * Builder
     */
    class Builder(
        private val entityId: Int,
        private val uuid: UUID,
        private val location: Location,
        private val displayType: DisplayType,
        private val displayName: String
    ) {
        private var text: String = ""
        private var itemStack: org.bukkit.inventory.ItemStack? = null
        private var blockData: org.bukkit.block.data.BlockData? = null
        private var scale: Float = 1.0f
        private var viewRange: Float = 1.0f
        private var shadowRadius: Float = 0.0f
        private var shadowStrength: Float = 1.0f
        private var backgroundColor: Int = 0x40000000
        private var textOpacity: Byte = -1
        private var billboardMode: BillboardMode = BillboardMode.FIXED

        fun text(text: String) = apply { this.text = text }
        fun itemStack(itemStack: org.bukkit.inventory.ItemStack) = apply { this.itemStack = itemStack }
        fun blockData(blockData: org.bukkit.block.data.BlockData) = apply { this.blockData = blockData }
        fun scale(scale: Float) = apply { this.scale = scale }
        fun viewRange(range: Float) = apply { this.viewRange = range }
        fun shadowRadius(radius: Float) = apply { this.shadowRadius = radius }
        fun shadowStrength(strength: Float) = apply { this.shadowStrength = strength }
        fun backgroundColor(color: Int) = apply { this.backgroundColor = color }
        fun textOpacity(opacity: Byte) = apply { this.textOpacity = opacity }
        fun billboardMode(mode: BillboardMode) = apply { this.billboardMode = mode }

        fun build(): PacketDisplayEntity {
            val display = PacketDisplayEntity(entityId, uuid, location, displayType, displayName)
            display.text = text
            display.itemStack = itemStack
            display.blockData = blockData
            display.scale = scale
            display.viewRange = viewRange
            display.shadowRadius = shadowRadius
            display.shadowStrength = shadowStrength
            display.backgroundColor = backgroundColor
            display.textOpacity = textOpacity
            display.billboardMode = billboardMode
            return display
        }
    }

    companion object {
        /**
         * ビルダーを作成
         */
        fun builder(
            entityId: Int,
            uuid: UUID,
            location: Location,
            displayType: DisplayType,
            displayName: String
        ): Builder {
            return Builder(entityId, uuid, location, displayType, displayName)
        }
    }
}

/**
 * ビルボードモード
 */
enum class BillboardMode {
    FIXED,          // 固定（回転しない）
    VERTICAL,       // 垂直軸のみ回転
    HORIZONTAL,     // 水平軸のみ回転
    CENTER          // 完全ビルボード（常にプレイヤーを向く）
}
