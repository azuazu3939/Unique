package com.github.azuazu3939.unique.targeter.types.basic

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * 自分自身をターゲット
 */
class SelfTargeter(
    id: String = "self",
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val targets = listOf(source)
        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Self", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // PacketEntityの場合は空のリストを返す
        // BasicSkillがSelfTargeterを検出し、source自身に対してエフェクトを適用する特殊処理を行う
        DebugLogger.targeter("Self (PacketEntity)", 0)
        return emptyList()
    }

    override fun getDescription(): String = "Self"
}
