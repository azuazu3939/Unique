package com.github.azuazu3939.unique.entity

import com.github.azuazu3939.unique.mob.toHealth
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Wrapper for Bukkit LivingEntity
 */
class BukkitEntityWrapper(private val entity: LivingEntity) : IEntity {

    override fun getUniqueId(): UUID = entity.uniqueId

    override fun getLoc(): Location = entity.location

    override fun getEntityType(): EntityType = entity.type

    override fun getHealth(): Double = entity.health

    override fun getMaxHealth(): Double = entity.toHealth()

    override fun getVelocity(): Vector = entity.velocity

    override fun getPose(): Pose = entity.pose

    override fun hasAI(): Boolean = entity.hasAI()

    override fun hasGravity(): Boolean = entity.hasGravity()

    override fun isOnGround(): Boolean = entity.isOnGround

    override fun isInWater(): Boolean = entity.isInWater

    override fun isGliding(): Boolean = entity.isGliding

    override fun isSwimming(): Boolean = entity.isSwimming

    override fun isInsideVehicle(): Boolean = entity.isInsideVehicle

    override fun getFireTicks(): Int = entity.fireTicks

    override fun getLastDamageCause(): String? = entity.lastDamageCause?.cause?.name

    override fun getLastDamageTime(): Long = 0

    override fun getNoDamageTicks(): Int = entity.noDamageTicks

    override fun hasLineOfSight(other: IEntity): Boolean {
        val otherBukkit = other.getBukkitEntity() ?: return false
        return entity.hasLineOfSight(otherBukkit)
    }

    override fun getBukkitEntity(): LivingEntity = entity

    override fun isPlayer(): Boolean = entity is Player

    override fun asPlayer(): Player? = entity as? Player

    override fun getPotionEffect(effect: PotionEffectType): PotionEffect? = entity.getPotionEffect(effect)

    override fun addPotionEffect(effect: PotionEffect) {
        entity.addPotionEffect(effect)
    }

    override fun damage(damage: Double, attacker: IEntity?) {
        if (attacker != null && attacker.getBukkitEntity() != null) {
            entity.damage(damage, attacker.getBukkitEntity())
        }
    }
}
