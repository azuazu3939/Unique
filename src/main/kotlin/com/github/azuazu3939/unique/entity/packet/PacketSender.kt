package com.github.azuazu3939.unique.entity.packet

import com.github.azuazu3939.unique.entity.EntityAnimation
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.*
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import java.util.Optional

/**
 * パケット送信ヘルパー
 *
 * PacketEventsを使用してパケットを送信
 */
object PacketSender {

    /**
     * エンティティスポーンパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entity エンティティ
     */
    fun sendSpawnPacket(player: Player, entity: PacketEntity) {
        DebugLogger.verbose("Sending spawn packet to ${player.name} for entity ${entity.entityId}")

        try {
            // EntityTypeを変換
            val peEntityType = convertToPEEntityType(entity.entityType)

            // スポーンパケット作成
            val spawnPacket = WrapperPlayServerSpawnEntity(
                entity.entityId,
                Optional.of(entity.uuid),
                peEntityType,
                Vector3d(entity.location.x, entity.location.y, entity.location.z),
                entity.location.pitch,
                entity.location.yaw,
                entity.location.yaw, // headYaw
                0, // data (0 for most entities)
                null // velocity (nullで停止状態)
            )

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, spawnPacket)

            // メタデータも送信
            sendMetadataPacket(player, entity)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send spawn packet to ${player.name}", e)
        }
    }

    /**
     * エンティティ削除パケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     */
    fun sendDespawnPacket(player: Player, entityId: Int) {
        DebugLogger.verbose("Sending despawn packet to ${player.name} for entity $entityId")

        try {
            // デスポーンパケット作成
            val despawnPacket = WrapperPlayServerDestroyEntities(entityId)

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, despawnPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send despawn packet to ${player.name}", e)
        }
    }

    /**
     * エンティティテレポートパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param location 新しい座標
     */
    fun sendTeleportPacket(player: Player, entityId: Int, location: Location) {
        DebugLogger.verbose("Sending teleport packet to ${player.name} for entity $entityId")

        try {
            // テレポートパケット作成
            val teleportPacket = WrapperPlayServerEntityTeleport(
                entityId,
                Vector3d(location.x, location.y, location.z),
                location.yaw,
                location.pitch,
                true // onGround
            )

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, teleportPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send teleport packet to ${player.name}", e)
        }
    }

    /**
     * エンティティ相対移動パケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param deltaX X軸移動量
     * @param deltaY Y軸移動量
     * @param deltaZ Z軸移動量
     */
    fun sendMovePacket(
        player: Player,
        entityId: Int,
        deltaX: Double,
        deltaY: Double,
        deltaZ: Double
    ) {
        DebugLogger.verbose("Sending move packet to ${player.name} for entity $entityId")

        try {
            // 相対移動パケット作成
            // Minecraftプロトコル: delta値 * 4096でshort型に変換
            val dx = (deltaX * 4096).toInt().toShort()
            val dy = (deltaY * 4096).toInt().toShort()
            val dz = (deltaZ * 4096).toInt().toShort()

            val movePacket = WrapperPlayServerEntityRelativeMove(
                entityId,
                dx.toDouble(),
                dy.toDouble(),
                dz.toDouble(),
                true // onGround
            )

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, movePacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send move packet to ${player.name}", e)
        }
    }

    /**
     * エンティティメタデータパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entity エンティティ
     */
    fun sendMetadataPacket(player: Player, entity: PacketEntity) {
        DebugLogger.verbose("Sending metadata packet to ${player.name} for entity ${entity.entityId}")

        try {
            val metadata = buildEntityMetadata(entity)

            val metadataPacket = WrapperPlayServerEntityMetadata(
                entity.entityId,
                metadata
            )

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, metadataPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send metadata packet to ${player.name}", e)
        }
    }

    /**
     * エンティティメタデータを構築
     */
    @Suppress("KotlinConstantConditions")
    private fun buildEntityMetadata(entity: PacketEntity): List<EntityData<*>> {
        val metadata = mutableListOf<EntityData<*>>()

        // 基本エンティティフラグ (Index 0)
        var flags: Byte = 0

        if (entity is PacketMob) {
            if (entity.isGlowing) flags = (flags.toInt() or 0x40).toByte()
            if (entity.isInvisible) flags = (flags.toInt() or 0x20).toByte()
            if (!entity.hasGravity) flags = (flags.toInt() or 0x80).toByte()
        }

        metadata.add(EntityData(0, EntityDataTypes.BYTE, flags))

        // PacketMobの場合、追加のメタデータ
        if (entity is PacketMob) {
            // カスタム名 (Index 2)
            if (entity.customName.isNotEmpty()) {
                metadata.add(EntityData(
                    2,
                    EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                    Optional.of(Component.text(entity.customName))
                ))
            }

            // カスタム名表示 (Index 3)
            metadata.add(EntityData(3, EntityDataTypes.BOOLEAN, entity.customNameVisible))

            // AI無効フラグ (Index 15) - Living Entity
            metadata.add(EntityData(
                15,
                EntityDataTypes.BYTE,
                if (entity.hasAI) 0.toByte() else 0x01.toByte()
            ))
        }

        return metadata
    }

    /**
     * エンティティアニメーションパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param animation アニメーション
     */
    fun sendAnimationPacket(player: Player, entityId: Int, animation: EntityAnimation) {
        DebugLogger.verbose("Sending animation packet to ${player.name} for entity $entityId: $animation")

        try {
            when (animation) {
                EntityAnimation.DEATH -> {
                    // 死亡はEntityStatusパケットで処理
                    val statusPacket = WrapperPlayServerEntityStatus(
                        entityId,
                        3 // PLAY_DEATH_ANIMATION
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, statusPacket)
                }
                else -> {
                    // 通常のアニメーションパケット
                    val animationType = when (animation) {
                        EntityAnimation.SWING_MAIN_HAND ->
                            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                        EntityAnimation.SWING_OFF_HAND ->
                            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND
                        EntityAnimation.DAMAGE ->
                            WrapperPlayServerEntityAnimation.EntityAnimationType.HURT
                        EntityAnimation.CRITICAL_HIT ->
                            WrapperPlayServerEntityAnimation.EntityAnimationType.CRITICAL_HIT
                        EntityAnimation.MAGIC_CRITICAL_HIT ->
                            WrapperPlayServerEntityAnimation.EntityAnimationType.MAGIC_CRITICAL_HIT
                        else -> return
                    }

                    val animationPacket = WrapperPlayServerEntityAnimation(entityId, animationType)
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
            }

        } catch (e: Exception) {
            DebugLogger.error("Failed to send animation packet to ${player.name}", e)
        }
    }

    /**
     * エンティティ装備パケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param equipment 装備アイテムのリスト (スロット, アイテム)
     */
    fun sendEquipmentPacket(
        player: Player,
        entityId: Int,
        equipment: List<Pair<EquipmentSlot, org.bukkit.inventory.ItemStack?>>
    ) {
        DebugLogger.verbose("Sending equipment packet to ${player.name} for entity $entityId")

        try {
            // 装備データを変換
            val equipmentList = equipment.map { (slot, item) ->
                if (item == null || item.type.isAir) {
                    Equipment(slot, ItemStack.EMPTY)
                } else {
                    Equipment(slot, SpigotConversionUtil.fromBukkitItemStack(item))
                }
            }

            if (equipmentList.isEmpty()) return

            // 装備パケット作成
            val equipmentPacket = WrapperPlayServerEntityEquipment(entityId, equipmentList)

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, equipmentPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send equipment packet to ${player.name}", e)
        }
    }

    /**
     * エンティティ装備パケットを送信（オーバーロード - 装備なし）
     */
    fun sendEquipmentPacket(player: Player, entityId: Int) {
        sendEquipmentPacket(player, entityId, emptyList())
    }

    /**
     * 回転パケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param yaw ヨー角度
     * @param pitch ピッチ角度
     */
    fun sendRotationPacket(player: Player, entityId: Int, yaw: Float, pitch: Float) {
        DebugLogger.verbose("Sending rotation packet to ${player.name} for entity $entityId")

        try {
            val rotationPacket = WrapperPlayServerEntityRotation(
                entityId,
                yaw,
                pitch,
                true // onGround
            )

            PacketEvents.getAPI().playerManager.sendPacket(player, rotationPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send rotation packet to ${player.name}", e)
        }
    }

    /**
     * 頭の回転パケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param headYaw 頭のヨー角度
     */
    fun sendHeadRotationPacket(player: Player, entityId: Int, headYaw: Float) {
        DebugLogger.verbose("Sending head rotation packet to ${player.name} for entity $entityId")

        try {
            val headRotationPacket = WrapperPlayServerEntityHeadLook(entityId, headYaw)

            PacketEvents.getAPI().playerManager.sendPacket(player, headRotationPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send head rotation packet to ${player.name}", e)
        }
    }

    /**
     * 複数プレイヤーにパケット送信
     */
    fun sendToMultiple(players: Collection<Player>, packetSender: (Player) -> Unit) {
        players.forEach { player ->
            try {
                packetSender(player)
            } catch (e: Exception) {
                DebugLogger.error("Failed to send packet to ${player.name}", e)
            }
        }
    }

    /**
     * Bukkit EntityTypeをPacketEvents EntityTypeに変換
     */
    fun convertToPEEntityType(
        bukkitType: EntityType
    ): com.github.retrooper.packetevents.protocol.entity.type.EntityType {
        return when (bukkitType) {
            EntityType.ZOMBIE -> EntityTypes.ZOMBIE
            EntityType.SKELETON -> EntityTypes.SKELETON
            EntityType.SPIDER -> EntityTypes.SPIDER
            EntityType.CREEPER -> EntityTypes.CREEPER
            EntityType.ENDERMAN -> EntityTypes.ENDERMAN
            EntityType.BLAZE -> EntityTypes.BLAZE
            EntityType.WITCH -> EntityTypes.WITCH
            EntityType.PIGLIN -> EntityTypes.PIGLIN
            EntityType.WITHER_SKELETON -> EntityTypes.WITHER_SKELETON
            EntityType.PHANTOM -> EntityTypes.PHANTOM
            EntityType.HOGLIN -> EntityTypes.HOGLIN
            EntityType.PIGLIN_BRUTE -> EntityTypes.PIGLIN_BRUTE
            EntityType.IRON_GOLEM -> EntityTypes.IRON_GOLEM
            EntityType.VILLAGER -> EntityTypes.VILLAGER
            EntityType.ENDER_DRAGON -> EntityTypes.ENDER_DRAGON
            EntityType.WITHER -> EntityTypes.WITHER
            EntityType.GHAST -> EntityTypes.GHAST
            EntityType.SLIME -> EntityTypes.SLIME
            EntityType.MAGMA_CUBE -> EntityTypes.MAGMA_CUBE
            EntityType.SILVERFISH -> EntityTypes.SILVERFISH
            EntityType.CAVE_SPIDER -> EntityTypes.CAVE_SPIDER
            EntityType.GUARDIAN -> EntityTypes.GUARDIAN
            EntityType.ELDER_GUARDIAN -> EntityTypes.ELDER_GUARDIAN
            EntityType.SHULKER -> EntityTypes.SHULKER
            EntityType.VEX -> EntityTypes.VEX
            EntityType.VINDICATOR -> EntityTypes.VINDICATOR
            EntityType.EVOKER -> EntityTypes.EVOKER
            EntityType.RAVAGER -> EntityTypes.RAVAGER
            EntityType.PILLAGER -> EntityTypes.PILLAGER
            EntityType.DROWNED -> EntityTypes.DROWNED
            EntityType.HUSK -> EntityTypes.HUSK
            EntityType.STRAY -> EntityTypes.STRAY
            EntityType.ZOMBIE_VILLAGER -> EntityTypes.ZOMBIE_VILLAGER
            EntityType.ENDERMITE -> EntityTypes.ENDERMITE
            EntityType.WARDEN -> EntityTypes.WARDEN
            EntityType.ARMOR_STAND -> EntityTypes.ARMOR_STAND
            // 友好的Mob
            EntityType.COW -> EntityTypes.COW
            EntityType.PIG -> EntityTypes.PIG
            EntityType.SHEEP -> EntityTypes.SHEEP
            EntityType.CHICKEN -> EntityTypes.CHICKEN
            EntityType.WOLF -> EntityTypes.WOLF
            EntityType.CAT -> EntityTypes.CAT
            EntityType.HORSE -> EntityTypes.HORSE
            // 必要に応じて追加
            else -> {
                DebugLogger.warn("Unsupported entity type: $bukkitType, defaulting to ZOMBIE")
                EntityTypes.ZOMBIE
            }
        }
    }
}