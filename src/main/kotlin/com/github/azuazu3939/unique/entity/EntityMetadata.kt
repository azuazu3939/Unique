package com.github.azuazu3939.unique.entity

import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.util.Vector3i
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import java.util.*

/**
 * Builder class for entity metadata
 * Simplifies creation of EntityData for PacketEvents
 */
class EntityMetadata {

    private val metadata = mutableListOf<EntityData<*>>()

    /**
     * Set entity flags (byte)
     * Index 0
     */
    fun setFlags(
        onFire: Boolean = false,
        crouching: Boolean = false,
        sprinting: Boolean = false,
        swimming: Boolean = false,
        invisible: Boolean = false,
        glowing: Boolean = false,
        elytraFlying: Boolean = false
    ): EntityMetadata {
        var flags: Byte = 0

        if (onFire) flags = (flags.toInt() or 0x01).toByte()
        if (crouching) flags = (flags.toInt() or 0x02).toByte()
        // 0x04 unused
        if (sprinting) flags = (flags.toInt() or 0x08).toByte()
        if (swimming) flags = (flags.toInt() or 0x10).toByte()
        if (invisible) flags = (flags.toInt() or 0x20).toByte()
        if (glowing) flags = (flags.toInt() or 0x40).toByte()
        if (elytraFlying) flags = (flags.toInt() or 0x80).toByte()

        metadata.add(EntityData(0, EntityDataTypes.BYTE, flags))
        return this
    }

    /**
     * Set air ticks
     * Index 1
     */
    fun setAirTicks(ticks: Int): EntityMetadata {
        metadata.add(EntityData(1, EntityDataTypes.INT, ticks))
        return this
    }

    /**
     * Set custom name
     * Index 2
     */
    fun setCustomName(name: Component?): EntityMetadata {
        if (name != null) {
            metadata.add(EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(name)))
        } else {
            metadata.add(EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.empty()))
        }
        return this
    }

    /**
     * Set custom name visible
     * Index 3
     */
    fun setCustomNameVisible(visible: Boolean): EntityMetadata {
        metadata.add(EntityData(3, EntityDataTypes.BOOLEAN, visible))
        return this
    }

    /**
     * Set silent (no sounds)
     * Index 4
     */
    fun setSilent(silent: Boolean): EntityMetadata {
        metadata.add(EntityData(4, EntityDataTypes.BOOLEAN, silent))
        return this
    }

    /**
     * Set no gravity
     * Index 5
     */
    fun setNoGravity(noGravity: Boolean): EntityMetadata {
        metadata.add(EntityData(5, EntityDataTypes.BOOLEAN, noGravity))
        return this
    }

    /**
     * Set entity pose
     * Index 6
     */
    fun setPose(pose: EntityPose): EntityMetadata {
        metadata.add(EntityData(6, EntityDataTypes.ENTITY_POSE, pose))
        return this
    }

    /**
     * Set frozen ticks (powder snow)
     * Index 7
     */
    fun setFrozenTicks(ticks: Int): EntityMetadata {
        metadata.add(EntityData(7, EntityDataTypes.INT, ticks))
        return this
    }

    // ============= Living Entity Metadata (Index 8+) =============

    /**
     * Set living entity flags
     * Index 8
     */
    fun setLivingFlags(
        handActive: Boolean = false,
        offhand: Boolean = false,
        riptide: Boolean = false
    ): EntityMetadata {
        var flags: Byte = 0

        if (handActive) flags = (flags.toInt() or 0x01).toByte()
        if (offhand) flags = (flags.toInt() or 0x02).toByte()
        if (riptide) flags = (flags.toInt() or 0x04).toByte()

        metadata.add(EntityData(8, EntityDataTypes.BYTE, flags))
        return this
    }

    /**
     * Set health
     * Index 9
     */
    fun setHealth(health: Float): EntityMetadata {
        metadata.add(EntityData(9, EntityDataTypes.FLOAT, health))
        return this
    }

    /**
     * Set potion effect color
     * Index 10
     */
    fun setPotionEffectColor(color: Int): EntityMetadata {
        metadata.add(EntityData(10, EntityDataTypes.INT, color))
        return this
    }

    /**
     * Set potion effect ambient
     * Index 11
     */
    fun setPotionEffectAmbient(ambient: Boolean): EntityMetadata {
        metadata.add(EntityData(11, EntityDataTypes.BOOLEAN, ambient))
        return this
    }

    /**
     * Set arrow count in entity
     * Index 12
     */
    fun setArrowCount(count: Int): EntityMetadata {
        metadata.add(EntityData(12, EntityDataTypes.INT, count))
        return this
    }

    /**
     * Set bee stinger count
     * Index 13
     */
    fun setBeeStingerCount(count: Int): EntityMetadata {
        metadata.add(EntityData(13, EntityDataTypes.INT, count))
        return this
    }

    /**
     * Set bed location (sleeping)
     * Index 14
     */
    fun setBedLocation(location: org.bukkit.Location?): EntityMetadata {
        if (location != null) {
            val blockPos = SpigotConversionUtil.fromBukkitLocation(location)
            metadata.add(EntityData(14, EntityDataTypes.OPTIONAL_BLOCK_POSITION,
                Optional.of(
                Vector3i(blockPos.x.toInt(), blockPos.y.toInt(), blockPos.z.toInt()
                ))))
        } else {
            metadata.add(EntityData(14, EntityDataTypes.OPTIONAL_BLOCK_POSITION, Optional.empty()))
        }
        return this
    }

    /**
     * Add raw entity data
     */
    fun addRaw(index: Int, type: com.github.retrooper.packetevents.protocol.entity.data.EntityDataType<Any>, value: Any): EntityMetadata {
        metadata.add(EntityData(index, type, value))
        return this
    }

    /**
     * Build the metadata list
     */
    fun build(): List<EntityData<*>> {
        return metadata.toList()
    }

    companion object {
        /**
         * Create a new metadata builder
         */
        fun create(): EntityMetadata {
            return EntityMetadata()
        }

        /**
         * Create metadata for a basic living entity
         */
        fun createBasic(
            customName: Component?,
            customNameVisible: Boolean = true,
            health: Float = 20f,
            invisible: Boolean = false,
            glowing: Boolean = false
        ): List<EntityData<*>> {
            return create()
                .setFlags(invisible = invisible, glowing = glowing)
                .setCustomName(customName)
                .setCustomNameVisible(customNameVisible)
                .setHealth(health)
                .build()
        }
    }
}
