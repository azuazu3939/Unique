package com.github.azuazu3939.unique.util

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffectType

fun LivingEntity.maxHealth(): Double = this.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0

fun PotionEffectType.name(): String = this.key.toString()

fun Sound.soundName(): String = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).getKeyOrThrow(this).toString()

fun getSound(name: Key): Sound = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).getOrThrow(name)