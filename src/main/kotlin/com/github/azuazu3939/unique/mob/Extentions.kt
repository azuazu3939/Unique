package com.github.azuazu3939.unique.mob

import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity

fun Location.toPacket() = com.github.retrooper.packetevents.protocol.world.Location(this.x, this.y, this.z, this.yaw, this.pitch)

fun LivingEntity.toHealth(): Double = this.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0