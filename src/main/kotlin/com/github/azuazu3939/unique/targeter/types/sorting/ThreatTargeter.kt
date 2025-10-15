package com.github.azuazu3939.unique.targeter.types.sorting

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.targeter.ThreatManager
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * ThreatTargeter - 脅威度（Threat/Aggro）に基づいてターゲットを選択
 *
 * 基本Targeterからターゲット候補を取得し、脅威度が最も高いものを選択します。
 * 脅威度はEntity MetadataまたはPacketMobの内部状態に保存されます。
 *
 * 使用例:
 * ```yaml
 * targeter:
 *   type: THREAT
 *   baseTargeter:
 *     type: RADIUSPLAYERS
 *     range: 20.0
 *   count: 3  # 上位3体を選択
 * ```
 *
 * 脅威度の追加方法:
 * ```kotlin
 * // Damageを与えた時に脅威度を追加
 * ThreatManager.addThreat(mob, player, 10.0)
 * ```
 */
class ThreatTargeter(
    id: String = "threat",
    private val baseTargeter: Targeter,
    private val count: Int = 1,
    filter: String? = null
) : Targeter(id, filter) {

    override fun getTargets(source: Entity): List<Entity> {
        val candidates = baseTargeter.getTargets(source)
        if (candidates.isEmpty()) {
            DebugLogger.targeter("Threat (no candidates)", 0)
            return emptyList()
        }

        // 脅威度でソート（降順）
        val sorted = candidates.sortedByDescending { ThreatManager.getThreat(source, it) }

        // 上位count個を返す
        val selected = sorted.take(count.coerceAtLeast(1))

        // フィルター適用
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Threat (selected top $count by threat)", filtered.size)
        return filtered
    }

    override fun getTargets(source: PacketEntity): List<Entity> {
        // PacketEntityの場合はEntityに変換してから取得
        // PacketMobの場合は内部にthreatMapを持つことを想定
        val candidates = baseTargeter.getTargets(source)
        if (candidates.isEmpty()) {
            DebugLogger.targeter("Threat from PacketEntity (no candidates)", 0)
            return emptyList()
        }

        // PacketEntity用の脅威度取得は、将来的にPacketMobに実装
        // 現時点ではメタデータベースのフォールバック
        val sorted = candidates.sortedByDescending { target ->
            // PacketEntityのUUIDをキーとして使用
            if (target.hasMetadata("${ThreatManager.THREAT_METADATA_KEY}_packetentity_${source.entityId}")) {
                target.getMetadata("${ThreatManager.THREAT_METADATA_KEY}_packetentity_${source.entityId}")
                    .firstOrNull()?.asDouble() ?: 0.0
            } else {
                0.0
            }
        }

        val selected = sorted.take(count.coerceAtLeast(1))
        val filtered = filterByFilter(source, selected)

        DebugLogger.targeter("Threat from PacketEntity (selected top $count by threat)", filtered.size)
        return filtered
    }

    override fun getDescription(): String {
        return "Threat-based (top $count) from (${baseTargeter.getDescription()})"
    }

    override fun debugInfo(): String {
        return "ThreatTargeter[base=${baseTargeter.debugInfo()}, count=$count, filter=${filter ?: "none"}]"
    }
}
