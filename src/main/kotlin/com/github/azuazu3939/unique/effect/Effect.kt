package com.github.azuazu3939.unique.effect

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.withContext
import org.bukkit.entity.Entity

/**
 * エフェクト基底クラス
 *
 * エンティティに対する効果を適用
 */
abstract class Effect(
    val id: String,
    val sync: Boolean = false
) {

    /**
     * エフェクトを適用
     *
     * @param source ソースエンティティ
     * @param target ターゲットエンティティ
     */
    abstract suspend fun apply(source: Entity, target: Entity)

    /**
     * エフェクトを適用（PacketEntityソース）
     *
     * @param source ソースPacketEntity
     * @param target ターゲットエンティティ
     */
    abstract suspend fun apply(source: PacketEntity, target: Entity)

    /**
     * エフェクトを適用（PacketEntity to PacketEntity）
     *
     * @param source ソースPacketEntity
     * @param target ターゲットPacketEntity
     */
    open suspend fun apply(source: PacketEntity, target: PacketEntity) {
        // デフォルト実装: PacketEntityに対するエフェクトは制限される
        DebugLogger.verbose("Effect $id: PacketEntity to PacketEntity not supported, skipping")
    }

    /**
     * syncフラグに基づいて適切なコンテキストでエフェクトを適用（Entity -> Entity）
     */
    suspend fun applyWithContext(source: Entity, target: Entity) {
        if (sync) {
            withContext(Unique.instance.regionDispatcher(target.location)) {
                apply(source, target)
            }
        } else {
            apply(source, target)
        }
    }

    /**
     * syncフラグに基づいて適切なコンテキストでエフェクトを適用（PacketEntity -> Entity）
     */
    suspend fun applyWithContext(source: PacketEntity, target: Entity) {
        if (sync) {
            withContext(Unique.instance.regionDispatcher(target.location)) {
                apply(source, target)
            }
        } else {
            apply(source, target)
        }
    }

    /**
     * syncフラグに基づいて適切なコンテキストでエフェクトを適用（PacketEntity -> PacketEntity）
     */
    suspend fun applyWithContext(source: PacketEntity, target: PacketEntity) {
        if (sync) {
            withContext(Unique.instance.regionDispatcher(target.location)) {
                apply(source, target)
            }
        } else {
            apply(source, target)
        }
    }

    /**
     * エフェクトの説明
     */
    open fun getDescription(): String {
        return "Effect: $id"
    }

    /**
     * デバッグ情報
     */
    open fun debugInfo(): String {
        return "Effect[id=$id, sync=$sync]"
    }
}
