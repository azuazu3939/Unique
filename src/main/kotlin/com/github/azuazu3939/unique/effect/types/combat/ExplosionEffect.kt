package com.github.azuazu3939.unique.effect.types.combat

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * 爆発エフェクト
 *
 * 指定位置で爆発を発生させます。
 * 範囲ダメージとノックバックを与えます。
 *
 * CEL対応パラメータ:
 * - damage: ダメージ量
 * - radius: 爆発半径
 * - knockback: ノックバック強度
 */
class ExplosionEffect(
    id: String = "explosion",
    private val damage: String = "20",  // CEL式対応
    private val radius: String = "3.0",  // CEL式対応
    private val knockback: String = "1.0",  // CEL式対応
    private val setFire: Boolean = false,
    private val breakBlocks: Boolean = false,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        val location = target.location
        val world = location.world ?: return

        // CEL式を評価
        val damageValue = evaluateDamage(source, target)
        val radiusValue = evaluateRadius(source, target)
        val knockbackValue = evaluateKnockback(source, target)

        // 爆発エフェクト
        world.createExplosion(location, 0f, setFire, breakBlocks)

        // 範囲内のエンティティにダメージ
        world.getNearbyEntitiesAsync(location, radiusValue, radiusValue, radiusValue)
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                // 距離減衰ダメージ
                val distance = entity.location.distance(location)
                val damageMultiplier = kotlin.math.max(0.0, 1.0 - distance / radiusValue)
                val finalDamage = damageValue * damageMultiplier

                entity.damage(finalDamage)

                // ノックバック
                val direction = entity.location.toVector().subtract(location.toVector()).normalize()
                entity.velocity = direction.multiply(knockbackValue * damageMultiplier)
            }

        DebugLogger.effect("Explosion(damage=$damageValue, radius=$radiusValue)", location.toString())
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val location = target.location
        val world = location.world ?: return

        val damageValue = evaluateDamageFromPacket(source, target)
        val radiusValue = evaluateRadiusFromPacket(source, target)
        val knockbackValue = evaluateKnockbackFromPacket(source, target)

        world.createExplosion(location, 0f, setFire, breakBlocks)

        world.getNearbyEntitiesAsync(location, radiusValue, radiusValue, radiusValue)
            .filterIsInstance<LivingEntity>()
            .forEach { entity ->
                val distance = entity.location.distance(location)
                val damageMultiplier = kotlin.math.max(0.0, 1.0 - distance / radiusValue)
                val finalDamage = damageValue * damageMultiplier

                entity.damage(finalDamage)

                val direction = entity.location.toVector().subtract(location.toVector()).normalize()
                entity.velocity = direction.multiply(knockbackValue * damageMultiplier)
            }

        DebugLogger.effect("Explosion(damage=$damageValue, radius=$radiusValue) from PacketEntity", location.toString())
    }

    private fun evaluateDamage(source: Entity, target: Entity): Double {
        return try {
            damage.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(damage, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion damage: $damage", e)
            20.0
        }
    }

    private fun evaluateRadius(source: Entity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion radius: $radius", e)
            3.0
        }
    }

    private fun evaluateKnockback(source: Entity, target: Entity): Double {
        return try {
            knockback.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(knockback, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion knockback: $knockback", e)
            1.0
        }
    }

    private fun evaluateDamageFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            damage.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(damage, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion damage: $damage", e)
            20.0
        }
    }

    private fun evaluateRadiusFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion radius: $radius", e)
            3.0
        }
    }

    private fun evaluateKnockbackFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            knockback.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(knockback, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate explosion knockback: $knockback", e)
            1.0
        }
    }

    override fun getDescription(): String = "Explode with $damage damage (radius: $radius)"
}
