package com.github.azuazu3939.unique.entity.packet

import com.github.azuazu3939.unique.entity.*
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
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
import java.util.*

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
                Optional.empty() // velocity (nullで停止状態)
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
        DebugLogger.verbose("Sending move packet to ${player.name} for entity $entityId (dx=$deltaX, dy=$deltaY, dz=$deltaZ)")

        try {
            // 相対移動パケット作成
            // WrapperPlayServerEntityRelativeMoveは元の値を受け取り、内部で変換する
            val movePacket = WrapperPlayServerEntityRelativeMove(
                entityId,
                deltaX,
                deltaY,
                deltaZ,
                true // onGround
            )

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, movePacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send move packet to ${player.name}", e)
        }
    }

    /**
     * エンティティ視線変更パケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param yaw ヨー角（水平方向）
     * @param pitch ピッチ角（垂直方向）
     */
    fun sendRotationPacket(player: Player, entityId: Int, yaw: Float, pitch: Float) {
        DebugLogger.verbose("Sending rotation packet to ${player.name} for entity $entityId (yaw=$yaw, pitch=$pitch)")

        try {
            // エンティティ回転パケット作成
            val rotationPacket = WrapperPlayServerEntityRotation(
                entityId,
                yaw,
                pitch,
                true // onGround
            )

            // パケット送信
            PacketEvents.getAPI().playerManager.sendPacket(player, rotationPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send rotation packet to ${player.name}", e)
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
                EntityAnimation.DAMAGE -> {
                    // ダメージ（赤くなるエフェクト）はEntityStatusパケットで処理
                    val statusPacket = WrapperPlayServerEntityStatus(
                        entityId,
                        2 // HURT (赤くなるエフェクト)
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
     * エンティティタイプに応じた攻撃アニメーションパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param entityType エンティティタイプ
     */
    fun sendAttackAnimationPacket(player: Player, entityId: Int, entityType: EntityType) {
        DebugLogger.verbose("Sending attack animation packet to ${player.name} for entity $entityId ($entityType)")

        try {
            when (entityType) {
                // 特殊な攻撃アニメーションを持つエンティティ（EntityStatusで送信）
                EntityType.IRON_GOLEM -> {
                    // アイアンゴーレムの腕を振り上げるアニメーション
                    val statusPacket = WrapperPlayServerEntityStatus(entityId, 4)
                    PacketEvents.getAPI().playerManager.sendPacket(player, statusPacket)
                }
                EntityType.RAVAGER -> {
                    // Ravagerの咆哮攻撃アニメーション
                    val statusPacket = WrapperPlayServerEntityStatus(entityId, 4)
                    PacketEvents.getAPI().playerManager.sendPacket(player, statusPacket)
                }
                EntityType.EVOKER -> {
                    // Evokerの魔法詠唱アニメーション
                    val statusPacket = WrapperPlayServerEntityStatus(entityId, 10)
                    PacketEvents.getAPI().playerManager.sendPacket(player, statusPacket)
                }
                EntityType.VINDICATOR -> {
                    // Vindicatorの攻撃準備アニメーション
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                EntityType.WARDEN -> {
                    // Wardenの攻撃アニメーション（咆哮）
                    val statusPacket = WrapperPlayServerEntityStatus(entityId, 4)
                    PacketEvents.getAPI().playerManager.sendPacket(player, statusPacket)
                }
                EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN -> {
                    // Guardianのレーザー攻撃（視覚エフェクトのみ）
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                EntityType.WITCH -> {
                    // Witchがポーションを飲むアニメーション
                    val statusPacket = WrapperPlayServerEntityStatus(entityId, 15)
                    PacketEvents.getAPI().playerManager.sendPacket(player, statusPacket)
                }
                EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED -> {
                    // Zombieの攻撃アニメーション（ドアを破壊するような動き）
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON -> {
                    // Skeletonの弓を引くアニメーション（通常の攻撃）
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                EntityType.SPIDER, EntityType.CAVE_SPIDER -> {
                    // Spiderの攻撃アニメーション
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                EntityType.ENDERMAN -> {
                    // Endermanの攻撃アニメーション
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                EntityType.BLAZE -> {
                    // Blazeの火の玉発射アニメーション
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                EntityType.GHAST -> {
                    // Ghastの火の玉発射アニメーション
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
                // デフォルト: 通常の手を振るアニメーション
                else -> {
                    val animationPacket = WrapperPlayServerEntityAnimation(
                        entityId,
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
                    PacketEvents.getAPI().playerManager.sendPacket(player, animationPacket)
                }
            }

        } catch (e: Exception) {
            DebugLogger.error("Failed to send attack animation packet to ${player.name}", e)
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
     * 体の回転パケットを送信（yaw のみ、pitch は 0）
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param bodyYaw 体のヨー角度
     */
    fun sendBodyRotationPacket(player: Player, entityId: Int, bodyYaw: Float) {
        DebugLogger.verbose("Sending body rotation packet to ${player.name} for entity $entityId")

        try {
            // Bodyの回転はEntityRotationパケットで送信（pitch = 0）
            val rotationPacket = WrapperPlayServerEntityRotation(
                entityId,
                bodyYaw,
                0.0f, // pitch は 0
                true // onGround
            )

            PacketEvents.getAPI().playerManager.sendPacket(player, rotationPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send body rotation packet to ${player.name}", e)
        }
    }

    /**
     * Display Entity スポーンパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param displayEntity Display Entity
     */
    fun sendSpawnDisplayPacket(player: Player, displayEntity: PacketDisplayEntity) {
        DebugLogger.verbose("Sending display spawn packet to ${player.name} for entity ${displayEntity.entityId}")

        try {
            val peEntityType = convertToPEEntityType(displayEntity.entityType)

            val spawnPacket = WrapperPlayServerSpawnEntity(
                displayEntity.entityId,
                Optional.of(displayEntity.uuid),
                peEntityType,
                Vector3d(displayEntity.location.x, displayEntity.location.y, displayEntity.location.z),
                displayEntity.location.pitch,
                displayEntity.location.yaw,
                displayEntity.location.yaw,
                0,
                Optional.empty()
            )

            PacketEvents.getAPI().playerManager.sendPacket(player, spawnPacket)

            // Display Entity はメタデータも即座に送信
            sendDisplayMetadataPacket(player, displayEntity)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send display spawn packet to ${player.name}", e)
        }
    }

    /**
     * Display Entity メタデータパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param displayEntity Display Entity
     */
    fun sendDisplayMetadataPacket(player: Player, displayEntity: PacketDisplayEntity) {
        DebugLogger.verbose("Sending display metadata packet to ${player.name} for entity ${displayEntity.entityId}")

        try {
            val metadata = buildDisplayMetadata(displayEntity)

            val metadataPacket = WrapperPlayServerEntityMetadata(
                displayEntity.entityId,
                metadata
            )

            PacketEvents.getAPI().playerManager.sendPacket(player, metadataPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send display metadata packet to ${player.name}", e)
        }
    }

    /**
     * Display Entity メタデータを構築
     */
    private fun buildDisplayMetadata(display: PacketDisplayEntity): List<EntityData<*>> {
        val metadata = mutableListOf<EntityData<*>>()

        // Scale (Index 11) - Vector3f (X, Y, Z)
        // PacketEvents の Vector3f を使用
        metadata.add(EntityData(
            11,
            EntityDataTypes.VECTOR3F,
            com.github.retrooper.packetevents.util.Vector3f(
                display.scale,
                display.scale,
                display.scale
            )
        ))

        // Billboard mode (Index 14)
        val billboardByte = when (display.billboardMode) {
            BillboardMode.FIXED -> 0.toByte()
            BillboardMode.VERTICAL -> 1.toByte()
            BillboardMode.HORIZONTAL -> 2.toByte()
            BillboardMode.CENTER -> 3.toByte()
        }
        metadata.add(EntityData(14, EntityDataTypes.BYTE, billboardByte))

        // View range (Index 16)
        metadata.add(EntityData(16, EntityDataTypes.FLOAT, display.viewRange))

        // Shadow radius (Index 17)
        metadata.add(EntityData(17, EntityDataTypes.FLOAT, display.shadowRadius))

        // Shadow strength (Index 18)
        metadata.add(EntityData(18, EntityDataTypes.FLOAT, display.shadowStrength))

        // Display type specific metadata
        when (display.displayType) {
            DisplayType.TEXT -> {
                // Text (Index 22)
                if (display.text.isNotEmpty()) {
                    metadata.add(EntityData(
                        22,
                        EntityDataTypes.ADV_COMPONENT,
                        Component.text(display.text)
                    ))
                }

                // Background color (Index 24)
                metadata.add(EntityData(24, EntityDataTypes.INT, display.backgroundColor))

                // Text opacity (Index 25)
                metadata.add(EntityData(25, EntityDataTypes.BYTE, display.textOpacity))
            }

            DisplayType.ITEM -> {
                // Item (Index 22)
                display.itemStack?.let { item ->
                    if (!item.type.isAir) {
                        metadata.add(EntityData(
                            22,
                            EntityDataTypes.ITEMSTACK,
                            SpigotConversionUtil.fromBukkitItemStack(item)
                        ))
                    }
                }

                // Display type (Index 23) - 5=HEAD, 6=GUI, 7=GROUND, 8=FIXED
                metadata.add(EntityData(23, EntityDataTypes.BYTE, 8.toByte())) // FIXED
            }

            DisplayType.BLOCK -> {
                // Block state (Index 22)
                display.blockData?.let { blockData ->
                    // BlockData を state ID に変換
                    // 注: これは簡易実装。正確には BlockData から state ID を取得する必要がある
                    val blockState = try {
                        // material の ID を使用（簡易版）
                        blockData.material.ordinal
                    } catch (e: Exception) {
                        0 // デフォルト（AIR）
                    }
                    metadata.add(EntityData(22, EntityDataTypes.INT, blockState))
                }
            }
        }

        return metadata
    }

    /**
     * 相対移動パケットを送信（エイリアス）
     *
     * @param player 送信先プレイヤー
     * @param entityId エンティティID
     * @param deltaX X軸移動量
     * @param deltaY Y軸移動量
     * @param deltaZ Z軸移動量
     */
    fun sendRelativeMovePacket(
        player: Player,
        entityId: Int,
        deltaX: Double,
        deltaY: Double,
        deltaZ: Double
    ) {
        // sendMovePacket のエイリアス
        sendMovePacket(player, entityId, deltaX, deltaY, deltaZ)
    }

    /**
     * 発射体スポーンパケットを送信
     *
     * @param player 送信先プレイヤー
     * @param projectileId 発射体エンティティID
     * @param projectileType 発射体タイプ
     * @param location スポーン位置
     * @param velocity 速度ベクトル
     * @param shooterId 発射者のエンティティID（PacketMobの場合）
     */
    fun sendProjectileSpawnPacket(
        player: Player,
        projectileId: Int,
        projectileType: EntityType,
        location: Location,
        velocity: org.bukkit.util.Vector,
        shooterId: Int? = null
    ) {
        DebugLogger.verbose("Sending projectile spawn packet to ${player.name} for projectile $projectileId")

        try {
            val peEntityType = convertToPEEntityType(projectileType)

            // 速度データ（Minecraftプロトコル: velocity * 8000）
            val data = shooterId ?: 0

            val spawnPacket = WrapperPlayServerSpawnEntity(
                projectileId,
                Optional.of(UUID.randomUUID()),
                peEntityType,
                Vector3d(location.x, location.y, location.z),
                location.pitch,
                location.yaw,
                location.yaw,
                data,
                Optional.of(Vector3d(velocity.x, velocity.y, velocity.z))
            )

            PacketEvents.getAPI().playerManager.sendPacket(player, spawnPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send projectile spawn packet to ${player.name}", e)
        }
    }

    /**
     * 発射体速度更新パケットを送信
     *
     * @param player 送信先プレイヤー
     * @param projectileId 発射体エンティティID
     * @param velocity 新しい速度ベクトル
     */
    fun sendProjectileVelocityPacket(
        player: Player,
        projectileId: Int,
        velocity: org.bukkit.util.Vector
    ) {
        DebugLogger.verbose("Sending velocity packet to ${player.name} for projectile $projectileId")

        try {
            val velocityPacket = WrapperPlayServerEntityVelocity(
                projectileId,
                Vector3d(velocity.x, velocity.y, velocity.z)
            )

            PacketEvents.getAPI().playerManager.sendPacket(player, velocityPacket)

        } catch (e: Exception) {
            DebugLogger.error("Failed to send velocity packet to ${player.name}", e)
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
            // 発射体
            EntityType.ARROW -> EntityTypes.ARROW
            EntityType.SPECTRAL_ARROW -> EntityTypes.SPECTRAL_ARROW
            EntityType.TRIDENT -> EntityTypes.TRIDENT
            EntityType.FIREBALL -> EntityTypes.FIREBALL
            EntityType.SMALL_FIREBALL -> EntityTypes.SMALL_FIREBALL
            EntityType.DRAGON_FIREBALL -> EntityTypes.DRAGON_FIREBALL
            EntityType.WITHER_SKULL -> EntityTypes.WITHER_SKULL
            EntityType.SNOWBALL -> EntityTypes.SNOWBALL
            EntityType.EGG -> EntityTypes.EGG
            EntityType.ENDER_PEARL -> EntityTypes.ENDER_PEARL
            EntityType.EXPERIENCE_BOTTLE -> EntityTypes.EXPERIENCE_BOTTLE
            EntityType.SPLASH_POTION -> EntityTypes.SPLASH_POTION
            EntityType.LINGERING_POTION -> EntityTypes.LINGERING_POTION
            EntityType.ITEM -> EntityTypes.ITEM
            EntityType.FISHING_BOBBER -> EntityTypes.FISHING_BOBBER
            // Display Entities (1.19.4+)
            EntityType.TEXT_DISPLAY -> EntityTypes.TEXT_DISPLAY
            EntityType.ITEM_DISPLAY -> EntityTypes.ITEM_DISPLAY
            EntityType.BLOCK_DISPLAY -> EntityTypes.BLOCK_DISPLAY
            // 必要に応じて追加
            else -> {
                DebugLogger.warn("Unsupported entity type: $bukkitType, defaulting to ZOMBIE")
                EntityTypes.ZOMBIE
            }
        }
    }
}