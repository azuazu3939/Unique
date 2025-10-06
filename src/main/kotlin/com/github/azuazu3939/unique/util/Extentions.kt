package com.github.azuazu3939.unique.util

import com.github.azuazu3939.unique.entity.PacketEntity
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.block.Biome
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ConcurrentHashMap

fun LivingEntity.maxHealth(): Double = this.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0

fun PotionEffectType.name(): String = this.key.toString()

fun getPotionEffectTypeName(type: Key): List<PotionEffect?> = RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION).getOrThrow(type).potionEffects

fun Sound.soundName(): String = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).getKeyOrThrow(this).toString()

fun getSound(name: Key): Sound = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT).getOrThrow(name)

fun Biome.biomeName(): String = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).getKeyOrThrow(this).toString()

// ========================================
// 変数ストレージ拡張関数
// ========================================

/**
 * Playerの変数ストレージ（ログアウト時にクリア）
 */
private val playerVariables = ConcurrentHashMap<java.util.UUID, MutableMap<String, Any>>()

/**
 * Entityが変数を保持できるか判定
 */
fun Entity.canHoldVariables(): Boolean {
    return this is Player
}

/**
 * PacketMobが変数を保持できるか判定（常にtrue）
 */
@Suppress("UnusedReceiverParameter")
fun PacketEntity.canHoldVariables(): Boolean = true

/**
 * Entityの変数を設定
 */
fun Entity.setVariable(name: String, value: Any) {
    when (this) {
        is Player -> {
            playerVariables.getOrPut(this.uniqueId) { mutableMapOf() }[name] = value
        }
        else -> {
            // PacketMobでない通常のEntityは変数を保持できない
            DebugLogger.warn("Entity ${this.type} cannot hold variables")
        }
    }
}

/**
 * PacketMobの変数を設定（既存メソッドのエイリアス）
 */
// PacketMob.setVariable() は既にクラス内で定義済み

/**
 * Entityの変数を取得
 */
fun Entity.getVariable(name: String): Any? {
    return when (this) {
        is Player -> playerVariables[this.uniqueId]?.get(name)
        else -> null
    }
}

/**
 * Entityのすべての変数を取得
 */
fun Entity.getVariables(): Map<String, Any> {
    return when (this) {
        is Player -> playerVariables[this.uniqueId]?.toMap() ?: emptyMap()
        else -> emptyMap()
    }
}

/**
 * Entityの変数をクリア
 */
fun Entity.clearVariables() {
    when (this) {
        is Player -> playerVariables[this.uniqueId]?.clear()
        else -> { }
    }
}

/**
 * Playerログアウト時に変数をクリア
 */
fun Player.clearVariablesOnLogout() {
    playerVariables.remove(this.uniqueId)
}