package com.github.azuazu3939.unique.effect.types.world

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.azuazu3939.unique.util.ResourceKeyResolver
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity

/**
 * SetBlockEffect - ターゲット位置にブロックを設置または破壊
 *
 * 指定されたブロックタイプを設置、または空気ブロックで破壊します。
 * radiusを指定すると複数ブロックを設置可能（CEL式対応）
 *
 * 使用例:
 * ```yaml
 * # 単一ブロック設置
 * - type: SET_BLOCK
 *   blockType: STONE
 *   radius: "0"
 *
 * # 範囲ブロック設置
 * - type: SET_BLOCK
 *   blockType: BARRIER
 *   radius: "2.0"
 *   temporary: true
 *   temporaryDuration: "5000"  # 5秒後に元に戻る
 * ```
 */
class SetBlockEffect(
    id: String = "set_block",
    private val blockType: String = "AIR",
    private val radius: String = "0.0",  // CEL式対応
    private val temporary: Boolean = false,
    private val temporaryDuration: String = "5000",  // 一時設置の持続時間 (ms)
    sync: Boolean = true
) : Effect(id, sync) {

    private val material: Material by lazy {
        ResourceKeyResolver.resolveMaterial(blockType, Material.AIR)
    }

    override suspend fun apply(source: Entity, target: Entity) {
        val radiusValue = evaluateRadius(source, target)
        setBlocks(target.location, radiusValue)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        val radiusValue = evaluateRadius(source, target)
        setBlocks(target.location, radiusValue)
    }

    /**
     * ブロック設置処理
     */
    private fun setBlocks(location: Location, radiusValue: Double) {
        val world = location.world ?: return
        val centerBlock = location.block

        if (radiusValue <= 0.0) {
            // 単一ブロック設置
            setBlock(centerBlock)
        } else {
            // 範囲ブロック設置
            val radiusSquared = radiusValue * radiusValue
            val radiusInt = radiusValue.toInt()

            for (x in -radiusInt..radiusInt) {
                for (y in -radiusInt..radiusInt) {
                    for (z in -radiusInt..radiusInt) {
                        val block = world.getBlockAt(
                            centerBlock.x + x,
                            centerBlock.y + y,
                            centerBlock.z + z
                        )

                        val distanceSquared = (x * x + y * y + z * z).toDouble()
                        if (distanceSquared <= radiusSquared) {
                            setBlock(block)
                        }
                    }
                }
            }
        }

        DebugLogger.effect("SetBlock(${material.name}, radius=$radiusValue)", "at ${location.blockX},${location.blockY},${location.blockZ}")
    }

    /**
     * 単一ブロック設置
     */
    private fun setBlock(block: Block) {
        if (temporary) {
            // 元のブロック状態を保存
            val originalType = block.type
            val originalData = block.blockData.clone()

            // ブロックを設置
            block.type = material

            // 一定時間後に元に戻す
            val durationTicks = (evaluateTemporaryDuration() / 50).toInt().coerceAtLeast(1)
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("Unique")!!,
                Runnable {
                    block.type = originalType
                    block.blockData = originalData
                },
                durationTicks.toLong()
            )
        } else {
            // 永続的に設置
            block.type = material
        }
    }

    /**
     * Radius評価（Entity source）
     */
    private fun evaluateRadius(source: Entity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate radius: $radius", e)
            0.0
        }
    }

    /**
     * Radius評価（PacketEntity source）
     */
    private fun evaluateRadius(source: PacketEntity, target: Entity): Double {
        return try {
            radius.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(radius, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate radius: $radius", e)
            0.0
        }
    }

    /**
     * Temporary duration評価
     */
    private fun evaluateTemporaryDuration(): Long {
        return try {
            temporaryDuration.toLongOrNull() ?: 5000L
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate temporary duration: $temporaryDuration", e)
            5000L
        }
    }

    override fun getDescription(): String = "Set $blockType in radius $radius" + if (temporary) " (temporary ${temporaryDuration}ms)" else ""
}
