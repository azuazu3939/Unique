package com.github.azuazu3939.unique.effect.types.world

import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.soundName
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity

/**
 * サウンドエフェクト
 */
class SoundEffect(
    id: String = "sound",
    private val sound: Sound,
    private val volume: Float = 1.0f,
    private val pitch: Float = 1.0f,
    sync: Boolean = false,
    category: SoundCategory
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val location = target.location
        location.world?.playSound(location, sound, volume, pitch)

        DebugLogger.effect("Sound(${sound.soundName()})", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val location = target.location
        location.world?.playSound(location, sound, volume, pitch)

        DebugLogger.effect("Sound(${sound.soundName()}) from PacketEntity", target.name)
    }

    override fun getDescription(): String = "Sound: ${sound.soundName()}"
}
