package com.github.azuazu3939.unique.effect.types.movement

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Entity

/**
 * テレポートエフェクト
 */
class TeleportToEffect(
    id: String = "teleport_to",
    private val location: Location,
    sync: Boolean = false
) : Effect(id, sync) {

    override suspend fun apply(source: Entity, target: Entity) {
        target.teleportAsync(location)
        DebugLogger.effect("TeleportTo", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        target.teleportAsync(location)
        DebugLogger.effect("TeleportTo PacketEntity", target.name)
    }

    override fun getDescription(): String {
        return "Teleport to (${location.blockX}, ${location.blockY}, ${location.blockZ})"
    }
}

/**
 * TeleportEffect - ターゲットをテレポート
 *
 * ターゲットを指定位置またはソースの位置にテレポートします。
 * 相対座標やランダム範囲もサポート。
 *
 * パラメータ:
 * - mode: テレポートモード（TO_SOURCE, TO_TARGET, TO_LOCATION, RANDOM）
 * - range: ランダム範囲（RANDOM mode用、CEL式対応）
 *
 * 使用例:
 * ```yaml
 * effects:
 *   - type: Teleport
 *     mode: TO_SOURCE  # ソースの位置にテレポート
 * ```
 */
class TeleportEffect(
    id: String = "teleport",
    private val mode: TeleportMode = TeleportMode.TO_SOURCE,
    private val range: String = "5.0",  // RANDOM用
    sync: Boolean = true
) : Effect(id, sync) {

    enum class TeleportMode {
        TO_SOURCE,    // ソースの位置へ
        TO_TARGET,    // ターゲットの位置へ（既にいる場所）
        RANDOM        // ランダムな位置へ
    }

    override suspend fun apply(source: Entity, target: Entity) {
        val destination = when (mode) {
            TeleportMode.TO_SOURCE -> source.location
            TeleportMode.TO_TARGET -> target.location
            TeleportMode.RANDOM -> {
                val rangeValue = evaluateRange(source, target)
                val randomOffset = org.bukkit.util.Vector(
                    (Math.random() - 0.5) * rangeValue * 2,
                    Math.random() * rangeValue,
                    (Math.random() - 0.5) * rangeValue * 2
                )
                target.location.clone().add(randomOffset)
            }
        }

        target.teleport(destination)

        // テレポートエフェクト
        target.world.spawnParticle(
            Particle.PORTAL,
            target.location,
            50,
            0.5, 1.0, 0.5,
            0.5
        )
        target.world.playSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)

        DebugLogger.effect("Teleport(mode=$mode)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val destination = when (mode) {
            TeleportMode.TO_SOURCE -> source.location
            TeleportMode.TO_TARGET -> target.location
            TeleportMode.RANDOM -> {
                val rangeValue = evaluateRangeFromPacket(source, target)
                val randomOffset = org.bukkit.util.Vector(
                    (Math.random() - 0.5) * rangeValue * 2,
                    Math.random() * rangeValue,
                    (Math.random() - 0.5) * rangeValue * 2
                )
                target.location.clone().add(randomOffset)
            }
        }

        target.teleport(destination)

        target.world.spawnParticle(
            Particle.PORTAL,
            target.location,
            50,
            0.5, 1.0, 0.5,
            0.5
        )
        target.world.playSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)

        DebugLogger.effect("Teleport(mode=$mode) from PacketEntity", target.name)
    }

    private fun evaluateRange(source: Entity, target: Entity): Double {
        return try {
            range.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(range, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate teleport range: $range", e)
            5.0
        }
    }

    private fun evaluateRangeFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            range.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(range, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate teleport range: $range", e)
            5.0
        }
    }

    override fun getDescription(): String = "Teleport (mode: $mode)"
}
