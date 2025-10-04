package com.github.azuazu3939.unique.entity

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.*
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.*
import com.github.retrooper.packetevents.protocol.entity.type.EntityType as PacketEntityType
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation

/**
 * Utility class for sending entity-related packets
 * Wraps PacketEvents API for easier entity management
 */
object EntityPacketHelper {

    /**
     * Send spawn living entity packet
     */
    fun sendSpawnPacket(
        player: Player,
        entityId: Int,
        uuid: UUID,
        entityType: PacketEntityType,
        location: Location
    ) {
        val packet = WrapperPlayServerSpawnEntity(
            entityId,
            Optional.of(uuid),
            entityType,
            Vector3d(location.x, location.y, location.z),
            location.pitch,
            location.yaw,
            0f, // head pitch
            0,  // data
            Optional.empty() )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity metadata packet
     */
    fun sendMetadataPacket(
        player: Player,
        entityId: Int,
        metadata: List<EntityData<*>>
    ) {
        val packet = WrapperPlayServerEntityMetadata(entityId, metadata)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity teleport packet
     */
    fun sendTeleportPacket(
        player: Player,
        entityId: Int,
        location: Location
    ) {
        val packet = WrapperPlayServerEntityTeleport(
            entityId,
            location.toPacketLocation(),
            location.isOnGround()
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity relative move packet
     */
    fun sendRelativeMovePacket(
        player: Player,
        entityId: Int,
        delta: Vector,
        onGround: Boolean
    ) {
        val packet = WrapperPlayServerEntityRelativeMove(
            entityId,
            delta.x,
            delta.y,
            delta.z,
            onGround
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity relative move and rotation packet
     */
    fun sendRelativeMoveAndRotationPacket(
        player: Player,
        entityId: Int,
        delta: Vector,
        yaw: Float,
        pitch: Float,
        onGround: Boolean
    ) {
        val packet = WrapperPlayServerEntityRelativeMoveAndRotation(
            entityId,
            delta.x,
            delta.y,
            delta.z,
            yaw,
            pitch,
            onGround
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity rotation packet
     */
    fun sendRotationPacket(
        player: Player,
        entityId: Int,
        yaw: Float,
        pitch: Float,
        onGround: Boolean
    ) {
        val packet = WrapperPlayServerEntityRotation(
            entityId,
            yaw,
            pitch,
            onGround
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity head rotation packet
     */
    fun sendHeadRotationPacket(
        player: Player,
        entityId: Int,
        yaw: Float
    ) {
        val packet = WrapperPlayServerEntityHeadLook(entityId, yaw)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send destroy entities packet
     */
    fun sendDestroyPacket(
        player: Player,
        vararg entityIds: Int
    ) {
        val packet = WrapperPlayServerDestroyEntities(*entityIds)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity animation packet
     */
    fun sendAnimationPacket(
        player: Player,
        entityId: Int,
        animation: WrapperPlayServerEntityAnimation.EntityAnimationType
    ) {
        val packet = WrapperPlayServerEntityAnimation(entityId, animation)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity status packet
     */
    fun sendStatusPacket(
        player: Player,
        entityId: Int,
        status: Int
    ) {
        val packet = WrapperPlayServerEntityStatus(entityId, status)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Send entity velocity packet
     */
    fun sendVelocityPacket(
        player: Player,
        entityId: Int,
        velocity: Vector
    ) {
        val packet = WrapperPlayServerEntityVelocity(
            entityId,
            Vector3d(velocity.x, velocity.y, velocity.z),
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /**
     * Broadcast packet to multiple players
     */
    fun broadcast(
        players: Collection<Player>,
        packetBuilder: (Player) -> Any
    ) {
        players.forEach { player ->
            val packet = packetBuilder(player)
            PacketEvents.getAPI().playerManager.sendPacket(player, packet)
        }
    }

    /**
     * Convert Bukkit Location to PacketEvents Location
     */
    private fun Location.toPacketLocation(): PacketLocation {
        return PacketLocation(x, y, z, yaw, pitch)
    }

    /**
     * Check if location is on ground (simple check)
     */
    private fun Location.isOnGround(): Boolean {
        return this.clone().subtract(0.0, 0.1, 0.0).block.type.isSolid
    }
}
