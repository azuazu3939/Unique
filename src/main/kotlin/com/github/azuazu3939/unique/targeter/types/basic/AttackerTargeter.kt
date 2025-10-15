package com.github.azuazu3939.unique.targeter.types.basic

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * 攻撃者をターゲット（ダメージイベント時）
 */
class AttackerTargeter(
    id: String = "attacker",
    filter: String? = null
) : Targeter(id, filter) {

    // 攻撃者はコンテキストから取得する必要がある
    private var lastAttacker: Entity? = null

    fun setAttacker(attacker: Entity?) {
        lastAttacker = attacker
    }

    override fun getTargets(source: Entity): List<Entity> {
        val attacker = lastAttacker
        val targets = if (attacker != null && attacker.isValid && !attacker.isDead) {
            listOf(attacker)
        } else {
            emptyList()
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Attacker", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        val attacker = lastAttacker
        val targets = if (attacker != null && attacker.isValid && !attacker.isDead) {
            listOf(attacker)
        } else {
            emptyList()
        }

        val filtered = filterByFilter(source, targets)

        DebugLogger.targeter("Attacker (PacketEntity)", filtered.size)
        return filtered
    }

    override fun getDescription(): String = "Attacker"
}
