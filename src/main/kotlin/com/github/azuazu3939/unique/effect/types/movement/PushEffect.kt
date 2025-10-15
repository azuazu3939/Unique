package com.github.azuazu3939.unique.effect.types.movement

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

/**
 * PushEffect - ターゲットを押し出す
 *
 * ターゲットをソースから遠ざける方向に押し出します。
 * radiusが指定されている場合、ソース位置から範囲内のエンティティを全て押し出します。
 *
 * パラメータ:
 * - strength: 押し出し強度（CEL式対応）
 * - radius: 範囲半径（0の場合は個別ターゲットモード）
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Push
 *     strength: "3.0"
 *     radius: 8.0  # 半径8ブロック内のエンティティを押し出す
 * ```
 */
class PushEffect(
    id: String = "push",
    private val strength: String = "1.5",
    private val radius: Double = 0.0,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        // 範囲モードの場合
        if (radius > 0.0) {
            applyAreaPush(source, source.location)
        } else {
            // 個別ターゲットモード
            applySinglePush(source, target)
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        // 範囲モードの場合
        if (radius > 0.0) {
            applyAreaPushFromPacket(source, source.location)
        } else {
            // 個別ターゲットモード
            applySinglePushFromPacket(source, target)
        }
    }

    /**
     * 個別ターゲットを押し出す
     */
    private suspend fun applySinglePush(source: Entity, target: Entity) {
        val strengthValue = evaluateStrength(source, target)

        // ソースから離れる方向ベクトルを計算
        val direction = target.location.toVector().subtract(source.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        // 視覚効果
        target.world.spawnParticle(
            Particle.EXPLOSION,
            target.location.add(0.0, 0.5, 0.0),
            5,
            0.2, 0.2, 0.2,
            0.0
        )

        DebugLogger.effect("Push(strength=$strengthValue)", target.name)
    }

    /**
     * 個別ターゲットを押し出す（PacketEntity）
     */
    private suspend fun applySinglePushFromPacket(source: PacketEntity, target: Entity) {
        val strengthValue = evaluateStrengthFromPacket(source, target)

        val direction = target.location.toVector().subtract(source.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        target.world.spawnParticle(
            Particle.EXPLOSION,
            target.location.add(0.0, 0.5, 0.0),
            5,
            0.2, 0.2, 0.2,
            0.0
        )

        DebugLogger.effect("Push(strength=$strengthValue) from PacketEntity", target.name)
    }

    /**
     * 範囲内のエンティティを押し出す
     */
    private suspend fun applyAreaPush(source: Entity, center: Location) {
        val world = center.world ?: return
        val nearbyEntities = world.getNearbyEntitiesAsync(center, radius, radius, radius)
            .filter { it != source && it is LivingEntity }

        for (entity in nearbyEntities) {
            applySinglePush(source, entity)
        }

        DebugLogger.effect("Push(area, radius=$radius, affected=${nearbyEntities.size})", "area")
    }

    /**
     * 範囲内のエンティティを押し出す（PacketEntity）
     */
    private suspend fun applyAreaPushFromPacket(source: PacketEntity, center: Location) {
        val world = center.world ?: return
        val nearbyEntities = world.getNearbyEntitiesAsync(center, radius, radius, radius)
            .filter { it is LivingEntity }

        for (entity in nearbyEntities) {
            applySinglePushFromPacket(source, entity)
        }

        DebugLogger.effect("Push(area, radius=$radius, affected=${nearbyEntities.size}) from PacketEntity", "area")
    }

    private fun evaluateStrength(source: Entity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate push strength: $strength", e)
            1.5
        }
    }

    private fun evaluateStrengthFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate push strength: $strength", e)
            1.5
        }
    }

    override fun getDescription(): String = "Push with strength $strength"
}
