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
 * PullEffect - ターゲットを引き寄せる
 *
 * ターゲットをソースの方向に引き寄せます。
 * 速度とノックバック抵抗を考慮します。
 *
 * パラメータ:
 * - strength: 引き寄せ強度（CEL式対応）
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Pull
 *     strength: "2.0"  # 強度2.0で引き寄せ
 * ```
 */
class PullEffect(
    id: String = "pull",
    private val strength: String = "1.0",
    private val radius: Double = 0.0,
    sync: Boolean = true
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        // 範囲モードの場合
        if (radius > 0.0) {
            applyAreaPull(source, source.location)
        } else {
            // 個別ターゲットモード
            applySinglePull(source, target)
        }
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        // 範囲モードの場合
        if (radius > 0.0) {
            applyAreaPullFromPacket(source, source.location)
        } else {
            // 個別ターゲットモード
            applySinglePullFromPacket(source, target)
        }
    }

    /**
     * 個別ターゲットを引き寄せる
     */
    private suspend fun applySinglePull(source: Entity, target: Entity) {
        val strengthValue = evaluateStrength(source, target)

        // ソースへの方向ベクトルを計算
        val direction = source.location.toVector().subtract(target.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        // 視覚効果
        target.world.spawnParticle(
            Particle.SWEEP_ATTACK,
            target.location.add(0.0, 1.0, 0.0),
            3,
            0.3, 0.3, 0.3,
            0.0
        )

        DebugLogger.effect("Pull(strength=$strengthValue)", target.name)
    }

    /**
     * 個別ターゲットを引き寄せる（PacketEntity）
     */
    private suspend fun applySinglePullFromPacket(source: PacketEntity, target: Entity) {
        val strengthValue = evaluateStrengthFromPacket(source, target)

        val direction = source.location.toVector().subtract(target.location.toVector()).normalize()
        val velocity = direction.multiply(strengthValue)

        target.velocity = velocity

        target.world.spawnParticle(
            Particle.SWEEP_ATTACK,
            target.location.add(0.0, 1.0, 0.0),
            3,
            0.3, 0.3, 0.3,
            0.0
        )

        DebugLogger.effect("Pull(strength=$strengthValue) from PacketEntity", target.name)
    }

    /**
     * 範囲内のエンティティを引き寄せる
     */
    private suspend fun applyAreaPull(source: Entity, center: Location) {
        val world = center.world ?: return
        val nearbyEntities = world.getNearbyEntitiesAsync(center, radius, radius, radius)
            .filter { it != source && it is LivingEntity }

        for (entity in nearbyEntities) {
            applySinglePull(source, entity)
        }

        DebugLogger.effect("Pull(area, radius=$radius, affected=${nearbyEntities.size})", "area")
    }

    /**
     * 範囲内のエンティティを引き寄せる（PacketEntity）
     */
    private suspend fun applyAreaPullFromPacket(source: PacketEntity, center: Location) {
        val world = center.world ?: return
        val nearbyEntities = world.getNearbyEntitiesAsync(center, radius, radius, radius)
            .filter { it is LivingEntity }

        for (entity in nearbyEntities) {
            applySinglePullFromPacket(source, entity)
        }

        DebugLogger.effect("Pull(area, radius=$radius, affected=${nearbyEntities.size}) from PacketEntity", "area")
    }

    private fun evaluateStrength(source: Entity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate pull strength: $strength", e)
            1.0
        }
    }

    private fun evaluateStrengthFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            strength.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(strength, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate pull strength: $strength", e)
            1.0
        }
    }

    override fun getDescription(): String = "Pull with strength $strength"
}
