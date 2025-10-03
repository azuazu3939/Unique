package com.github.azuazu3939.unique.mob.condition

import com.github.azuazu3939.unique.mob.data.Condition
import com.github.azuazu3939.unique.mob.data.ConditionTarget
import com.github.azuazu3939.unique.mob.data.TargetCondition
import com.github.azuazu3939.unique.mob.toHealth
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.math.acos

/**
 * Evaluates MythicMobs-style conditions
 */
class ConditionEvaluator {

    /**
     * Evaluate a condition
     */
    fun evaluate(
        condition: Condition,
        mobLocation: Location,
        mobEntity: LivingEntity?,
        triggerEntity: LivingEntity?
    ): Boolean {
        val targetEntity = when (condition.target) {
            ConditionTarget.SELF -> mobEntity
            ConditionTarget.TRIGGER -> triggerEntity
        }

        val result = when (condition.type.lowercase()) {
            // World conditions
            "raining", "rain" -> evaluateRaining(mobLocation)
            "storming", "storm", "thundering" -> evaluateStorming(mobLocation)
            "night" -> evaluateNight(mobLocation)
            "day" -> evaluateDay(mobLocation)
            "sunny" -> evaluateSunny(mobLocation)
            "worldtime" -> evaluateWorldTime(mobLocation, condition.options)
            "lightlevel" -> evaluateLightLevel(mobLocation, condition.options)

            // Entity conditions
            "holding", "itemsinhand" -> evaluateHolding(targetEntity, condition.options)
            "wearing" -> evaluateWearing(targetEntity, condition.options)
            "sneaking", "crouching" -> evaluateSneaking(targetEntity)
            "sprinting" -> evaluateSprinting(targetEntity)
            "blocking" -> evaluateBlocking(targetEntity)
            "gliding" -> evaluateGliding(targetEntity)
            "swimming" -> evaluateSwimming(targetEntity)
            "onfire", "burning" -> evaluateOnFire(targetEntity)
            "stance", "pose" -> evaluateStance(targetEntity, condition.options)
            "hasai" -> evaluateHasAI(targetEntity)
            "hasgravity" -> evaluateHasGravity(targetEntity)
            "hasinventory" -> evaluateHasInventory(targetEntity)
            "mounted", "riding" -> evaluateMounted(targetEntity)
            "incombat" -> evaluateInCombat(targetEntity)
            "lastdamagecause" -> evaluateLastDamageCause(targetEntity, condition.options)

            // Potion effects
            "haspotioneffect", "haspotion" -> evaluateHasPotionEffect(targetEntity, condition.options)

            // Location conditions
            "inwater" -> evaluateInWater(targetEntity)
            "inlava" -> evaluateInLava(targetEntity)
            "onground" -> evaluateOnGround(targetEntity)
            "altitude", "height", "y" -> evaluateAltitude(targetEntity, mobLocation, condition.options)
            "biome" -> evaluateBiome(mobLocation, condition.options)
            "blocktype" -> evaluateBlockType(mobLocation, condition.options)
            "inblock" -> evaluateInBlock(targetEntity, condition.options)
            "outside" -> evaluateOutside(mobLocation)

            // Distance and position
            "distance" -> evaluateDistance(mobEntity, targetEntity, condition.options)
            "lineofsight", "los" -> evaluateLineOfSight(mobEntity, targetEntity)
            "fieldofview", "fov" -> evaluateFieldOfView(mobEntity, targetEntity, condition.options)
            "yaw" -> evaluateYaw(targetEntity, condition.options)
            "pitch" -> evaluatePitch(targetEntity, condition.options)

            // Entity type
            "entitytype", "mobtype" -> evaluateEntityType(targetEntity, condition.options)
            "mobsinradius" -> evaluateMobsInRadius(mobLocation, condition.options)

            // Health and damage
            "health" -> evaluateHealth(targetEntity, condition.options)
            "healthpercent" -> evaluateHealthPercent(targetEntity, condition.options)
            "maxhealth" -> evaluateMaxHealth(targetEntity, condition.options)
            "fallspeed" -> evaluateFallSpeed(targetEntity, condition.options)

            // Player-specific conditions
            "gamemode" -> evaluateGameMode(targetEntity, condition.options)
            "level", "playerlevel" -> evaluateLevel(targetEntity, condition.options)
            "haspermission", "permission" -> evaluatePermission(targetEntity, condition.options)
            "hunger", "foodlevel" -> evaluateHunger(targetEntity, condition.options)
            "oxygen", "air" -> evaluateOxygen(targetEntity, condition.options)
            "hasitem" -> evaluateHasItem(targetEntity, condition.options)
            "playertime" -> evaluatePlayerTime(targetEntity, condition.options)
            "playerkills" -> evaluatePlayerKills(targetEntity, condition.options)

            // Default: unknown condition = false
            else -> false
        }

        // Apply negation if needed
        return if (condition.negate) !result else result
    }

    /**
     * Evaluate target condition (for target filtering in selectors)
     */
    fun evaluateTargetCondition(
        condition: TargetCondition,
        entity: LivingEntity?
    ): Boolean {
        if (entity == null) return false

        val result = when (condition.type.lowercase()) {
            "hasaura" -> {
                // TODO: Implement aura system
                false
            }
            "haspotioneffect", "haspotion" -> {
                val type = condition.options["type"] ?: return false
                val effectType = try {
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION).getOrThrow(NamespacedKey.minecraft(type.lowercase())).potionEffects
                } catch (e: Exception) {
                    null
                } ?: return false

                return effectType.all {
                    val effect = entity.getPotionEffect(it.type) ?: return false

                    // Check duration range
                    val durationRange = condition.options["d"]
                    if (durationRange != null) {
                        val (minDur, maxDur) = parseRange(durationRange)
                        if (effect.duration !in minDur..maxDur) return false
                    }

                    // Check level range
                    val levelRange = condition.options["l"]
                    return if (levelRange != null) {
                        val (minLvl, maxLvl) = parseRange(levelRange)
                        effect.amplifier in minLvl..maxLvl
                    } else {
                        false
                    }
                }
            }
            // Entity conditions
            "holding", "itemsinhand" -> evaluateHolding(entity, condition.options)
            "wearing" -> evaluateWearing(entity, condition.options)
            "sneaking", "crouching" -> evaluateSneaking(entity)
            "sprinting" -> evaluateSprinting(entity)
            "blocking" -> evaluateBlocking(entity)
            "gliding" -> evaluateGliding(entity)
            "swimming" -> evaluateSwimming(entity)
            "onfire", "burning" -> evaluateOnFire(entity)
            "stance", "pose" -> evaluateStance(entity, condition.options)
            "hasai" -> evaluateHasAI(entity)
            "hasgravity" -> evaluateHasGravity(entity)
            "mounted", "riding" -> evaluateMounted(entity)
            "incombat" -> evaluateInCombat(entity)

            // Health conditions
            "health" -> {
                val range = condition.options["range"] ?: condition.options["r"] ?: return false
                val (min, max) = parseDoubleRange(range)
                entity.health in min..max
            }
            "healthpercent" -> {
                val percent = (entity.health / entity.toHealth()) * 100
                val range = condition.options["range"] ?: condition.options["r"] ?: return false
                val (min, max) = parseDoubleRange(range)
                percent in min..max
            }
            "maxhealth" -> evaluateMaxHealth(entity, condition.options)

            // Entity type
            "entitytype", "mobtype" -> evaluateEntityType(entity, condition.options)

            // Location conditions
            "inwater" -> evaluateInWater(entity)
            "inlava" -> evaluateInLava(entity)
            "onground" -> evaluateOnGround(entity)
            "altitude", "height", "y" -> evaluateAltitude(entity, entity.location, condition.options)
            "biome" -> evaluateBiome(entity.location, condition.options)
            "inblock" -> evaluateInBlock(entity, condition.options)
            "outside" -> evaluateOutside(entity.location)

            // Player-specific
            "gamemode" -> evaluateGameMode(entity, condition.options)
            "level", "playerlevel" -> evaluateLevel(entity, condition.options)
            "haspermission", "permission" -> evaluatePermission(entity, condition.options)
            "hunger", "foodlevel" -> evaluateHunger(entity, condition.options)
            "oxygen", "air" -> evaluateOxygen(entity, condition.options)
            "hasitem" -> evaluateHasItem(entity, condition.options)

            else -> false
        }

        return result == condition.expectedResult
    }

    // World condition evaluators
    private fun evaluateRaining(location: Location): Boolean {
        return location.world?.hasStorm() ?: false
    }

    private fun evaluateStorming(location: Location): Boolean {
        return location.world?.isThundering ?: false
    }

    private fun evaluateNight(location: Location): Boolean {
        val time = location.world?.time ?: return false
        return time in 13000..23000
    }

    private fun evaluateDay(location: Location): Boolean {
        val time = location.world?.time ?: return false
        return time !in 13000..23000
    }

    private fun evaluateSunny(location: Location): Boolean {
        return !(location.world?.hasStorm() ?: true)
    }

    // Entity condition evaluators
    private fun evaluateHolding(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false

        val material = options["m"] ?: options["material"] ?: return false
        val mat = try {
            Material.valueOf(material.uppercase())
        } catch (e: Exception) {
            return false
        }

        return entity.inventory.itemInMainHand.type == mat ||
                entity.inventory.itemInOffHand.type == mat
    }

    private fun evaluateWearing(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false

        val material = options["m"] ?: options["material"] ?: return false
        val mat = try {
            Material.valueOf(material.uppercase())
        } catch (e: Exception) {
            return false
        }

        val armor = entity.inventory.armorContents
        return armor.any { it?.type == mat }
    }

    private fun evaluateSneaking(entity: LivingEntity?): Boolean {
        if (entity !is Player) return false
        return entity.isSneaking
    }

    private fun evaluateSprinting(entity: LivingEntity?): Boolean {
        if (entity !is Player) return false
        return entity.isSprinting
    }

    private fun evaluateBlocking(entity: LivingEntity?): Boolean {
        if (entity !is Player) return false
        return entity.isBlocking
    }

    private fun evaluateGliding(entity: LivingEntity?): Boolean {
        return entity?.isGliding ?: false
    }

    private fun evaluateSwimming(entity: LivingEntity?): Boolean {
        return entity?.isSwimming ?: false
    }

    private fun evaluateOnFire(entity: LivingEntity?): Boolean {
        return entity?.fireTicks?.let { it > 0 } ?: false
    }

    private fun evaluateHasPotionEffect(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false

        val type = options["type"] ?: options["t"] ?: return false
        val effectType = try {
            RegistryAccess.registryAccess().getRegistry(RegistryKey.POTION).getOrThrow(NamespacedKey.minecraft(type.lowercase())).potionEffects
        } catch (e: Exception) {
            return false
        }

        return effectType.all { entity.hasPotionEffect(it.type) }
    }

    private fun evaluateInWater(entity: LivingEntity?): Boolean {
        return entity?.isInWater ?: false
    }

    private fun evaluateInLava(entity: LivingEntity?): Boolean {
        val location = entity?.location ?: return false
        return location.block.type == Material.LAVA
    }

    private fun evaluateOnGround(entity: LivingEntity?): Boolean {
        return entity?.isOnGround ?: false
    }

    /**
     * Parse range string (e.g., "1to999999" or "0to254")
     */
    private fun parseRange(range: String): Pair<Int, Int> {
        val parts = range.split("to")
        if (parts.size != 2) return 0 to Int.MAX_VALUE

        val min = parts[0].toIntOrNull() ?: 0
        val max = parts[1].toIntOrNull() ?: Int.MAX_VALUE
        return min to max
    }

    /**
     * Parse double range string (e.g., "1.0to10.5")
     */
    private fun parseDoubleRange(range: String): Pair<Double, Double> {
        val parts = range.split("to")
        if (parts.size != 2) return 0.0 to Double.MAX_VALUE

        val min = parts[0].toDoubleOrNull() ?: 0.0
        val max = parts[1].toDoubleOrNull() ?: Double.MAX_VALUE
        return min to max
    }

    // ============= NEW CONDITION EVALUATORS =============

    // World conditions
    private fun evaluateWorldTime(location: Location, options: Map<String, String>): Boolean {
        val time = location.world?.time ?: return false
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseRange(range)
        return time in min..max
    }

    private fun evaluateLightLevel(location: Location, options: Map<String, String>): Boolean {
        val level = location.block.lightLevel.toInt()
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseRange(range)
        return level in min..max
    }

    // Entity state conditions
    private fun evaluateStance(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val stance = options["stance"] ?: options["s"] ?: return false
        return entity.pose.name.equals(stance, ignoreCase = true)
    }

    private fun evaluateHasAI(entity: LivingEntity?): Boolean {
        return entity?.hasAI() ?: false
    }

    private fun evaluateHasGravity(entity: LivingEntity?): Boolean {
        return entity?.hasGravity() ?: false
    }

    private fun evaluateHasInventory(entity: LivingEntity?): Boolean {
        return entity is Player
    }

    private fun evaluateMounted(entity: LivingEntity?): Boolean {
        return entity?.isInsideVehicle ?: false
    }

    private fun evaluateInCombat(entity: LivingEntity?): Boolean {
        if (entity == null) return false
        // Consider in combat if recently damaged (within last 5 seconds)
        return entity.noDamageTicks > 0 || (System.currentTimeMillis() - entity.lastDamage) < 5000
    }

    private fun evaluateLastDamageCause(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val cause = options["cause"] ?: options["c"] ?: return false
        val lastCause = entity.lastDamageCause?.cause?.name ?: return false
        return lastCause.equals(cause, ignoreCase = true)
    }

    // Location conditions
    private fun evaluateAltitude(entity: LivingEntity?, location: Location, options: Map<String, String>): Boolean {
        val y = entity?.location?.y ?: location.y
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return y in min..max
    }

    private fun evaluateBiome(location: Location, options: Map<String, String>): Boolean {
        val biome = options["biome"] ?: options["b"] ?: return false
        val actualBiome = location.block.biome.key.key
        return actualBiome.equals(biome, ignoreCase = true) || actualBiome.contains(biome, ignoreCase = true)
    }

    private fun evaluateBlockType(location: Location, options: Map<String, String>): Boolean {
        val blockType = options["type"] ?: options["t"] ?: return false
        val actualType = location.block.type.name
        return actualType.equals(blockType, ignoreCase = true)
    }

    private fun evaluateInBlock(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val blockType = options["type"] ?: options["t"] ?: return false
        val actualType = entity.location.block.type.name
        return actualType.equals(blockType, ignoreCase = true)
    }

    private fun evaluateOutside(location: Location): Boolean {
        return (location.world?.getHighestBlockYAt(location) ?: 0) <= location.blockY
    }

    // Distance and position conditions
    private fun evaluateDistance(mob: LivingEntity?, target: LivingEntity?, options: Map<String, String>): Boolean {
        if (mob == null || target == null) return false
        val distance = mob.location.distance(target.location)
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return distance in min..max
    }

    private fun evaluateLineOfSight(mob: LivingEntity?, target: LivingEntity?): Boolean {
        if (mob == null || target == null) return false
        return mob.hasLineOfSight(target)
    }

    private fun evaluateFieldOfView(mob: LivingEntity?, target: LivingEntity?, options: Map<String, String>): Boolean {
        if (mob == null || target == null) return false

        val angle = options["angle"] ?: options["a"] ?: "90"
        val maxAngle = angle.toDoubleOrNull() ?: 90.0

        val mobLoc = mob.location
        val targetLoc = target.location

        // Calculate direction vector from mob to target
        val toTarget = targetLoc.toVector().subtract(mobLoc.toVector()).normalize()

        // Get mob's looking direction
        val mobDirection = mobLoc.direction.normalize()

        // Calculate angle between vectors
        val dotProduct = mobDirection.dot(toTarget)
        val angleRad = acos(dotProduct.coerceIn(-1.0, 1.0))
        val angleDeg = Math.toDegrees(angleRad)

        return angleDeg <= maxAngle
    }

    private fun evaluateYaw(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val yaw = entity.location.yaw.toDouble()
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return yaw in min..max
    }

    private fun evaluatePitch(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val pitch = entity.location.pitch.toDouble()
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return pitch in min..max
    }

    // Entity type conditions
    private fun evaluateEntityType(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val type = options["type"] ?: options["t"] ?: return false
        return entity.type.name.equals(type, ignoreCase = true)
    }

    private fun evaluateMobsInRadius(location: Location, options: Map<String, String>): Boolean {
        val radius = options["radius"]?.toDoubleOrNull() ?: options["r"]?.toDoubleOrNull() ?: return false
        val count = options["count"]?.toIntOrNull() ?: options["c"]?.toIntOrNull() ?: return false

        val nearbyEntities = location.world?.getNearbyLivingEntities(location, radius) ?: return false
        return nearbyEntities.size >= count
    }

    // Health conditions
    private fun evaluateHealth(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val health = entity.health
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return health in min..max
    }

    private fun evaluateHealthPercent(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val percent = (entity.health / entity.toHealth()) * 100
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return percent in min..max
    }

    private fun evaluateMaxHealth(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val maxHealth = entity.toHealth()
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return maxHealth in min..max
    }

    private fun evaluateFallSpeed(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity == null) return false
        val fallSpeed = -entity.velocity.y // Negative Y velocity when falling
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseDoubleRange(range)
        return fallSpeed in min..max
    }

    // Player-specific conditions
    private fun evaluateGameMode(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val mode = options["mode"] ?: options["m"] ?: return false
        return entity.gameMode.name.equals(mode, ignoreCase = true)
    }

    private fun evaluateLevel(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val level = entity.level
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseRange(range)
        return level in min..max
    }

    private fun evaluatePermission(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val permission = options["permission"] ?: options["p"] ?: return false
        return entity.hasPermission(permission)
    }

    private fun evaluateHunger(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val hunger = entity.foodLevel
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseRange(range)
        return hunger in min..max
    }

    private fun evaluateOxygen(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val oxygen = entity.remainingAir
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseRange(range)
        return oxygen in min..max
    }

    private fun evaluateHasItem(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val material = options["item"] ?: options["i"] ?: return false
        val mat = try {
            Material.valueOf(material.uppercase())
        } catch (e: Exception) {
            return false
        }

        val amount = options["amount"]?.toIntOrNull() ?: options["a"]?.toIntOrNull() ?: 1

        return entity.inventory.all(mat).values.sumOf { it?.amount ?: 0 } >= amount
    }

    private fun evaluatePlayerTime(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val time = entity.playerTime
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseRange(range)
        return time.toInt() in min..max
    }

    private fun evaluatePlayerKills(entity: LivingEntity?, options: Map<String, String>): Boolean {
        if (entity !is Player) return false
        val kills = entity.getStatistic(org.bukkit.Statistic.PLAYER_KILLS)
        val range = options["range"] ?: options["r"] ?: return false
        val (min, max) = parseRange(range)
        return kills in min..max
    }
}
