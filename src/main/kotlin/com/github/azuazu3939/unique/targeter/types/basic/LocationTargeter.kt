package com.github.azuazu3939.unique.targeter.types.basic

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * 座標ベースのターゲット
 */
class LocationTargeter(
    id: String = "location",
    private val location: Location,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        // 座標にはエンティティがいないので空リストを返す
        // 実際の使用ではスキルがこの座標を使用する
        DebugLogger.targeter("Location", 0)
        return emptyList()
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // 座標にはエンティティがいないので空リストを返す
        DebugLogger.targeter("Location (PacketEntity)", 0)
        return emptyList()
    }

    fun getLocation(): Location {
        return location.clone()
    }

    override fun getDescription(): String {
        return "Location(${location.blockX}, ${location.blockY}, ${location.blockZ})"
    }
}
