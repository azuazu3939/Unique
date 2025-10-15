package com.github.azuazu3939.unique.effect.types.combat

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import java.util.*

/**
 * ダメージエフェクト
 *
 * amountパラメータはCEL式をサポート
 * 例: "10", "target.health * 0.3", "20 * (1 - distance.horizontal(source.location, target.location) / 30)"
 *
 * @param ignoreInvulnerability 無敵時間を無視するか（連続ヒット可能）
 * @param ignoreKnockbackResistance ノックバック耐性を無視するか（ノックバックを強制的に発生）
 * @param preventKnockback ノックバックを完全に封じるか（後処理で適用）
 */
class DamageEffect(
    id: String = "damage",
    private val amount: String,
    private val ignoreInvulnerability: Boolean = false,
    private val ignoreKnockbackResistance: Boolean = false,
    private val preventKnockback: Boolean = false,
    sync: Boolean = true
) : Effect(id, sync) {

    companion object {
        private val KNOCKBACK_RESISTANCE_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d")
    }

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        val damageValue = evaluateDamage(source, target)

        // 無敵時間とノックバック耐性を一時的に操作
        applyDamageWithOptions(target, damageValue, source)

        DebugLogger.effect("Damage($damageValue, ignoreInv=$ignoreInvulnerability, ignoreKB=$ignoreKnockbackResistance, preventKB=$preventKnockback)", target.name)
    }

    override suspend fun apply(source: PacketEntity, target: Entity) {
        if (target !is LivingEntity) return

        val damageValue = evaluateDamageFromPacket(source, target)

        // 無敵時間とノックバック耐性を一時的に操作
        applyDamageWithOptions(target, damageValue, null)

        DebugLogger.effect("Damage($damageValue, ignoreInv=$ignoreInvulnerability, ignoreKB=$ignoreKnockbackResistance, preventKB=$preventKnockback) from PacketEntity", target.name)
    }

    /**
     * 無敵時間とノックバック耐性を考慮してダメージを適用
     */
    private fun applyDamageWithOptions(target: LivingEntity, damage: Double, source: Entity?) {
        var originalNoDamageTicks: Int? = null
        var kbAttribute: org.bukkit.attribute.AttributeInstance? = null
        var originalKbValue: Double? = null
        var originalVelocity: org.bukkit.util.Vector? = null

        try {
            // 無敵時間を無視する場合
            if (ignoreInvulnerability) {
                originalNoDamageTicks = target.noDamageTicks
                target.noDamageTicks = 0
            }

            // ノックバック耐性を無視する場合（ノックバックを強制的に発生）
            if (ignoreKnockbackResistance) {
                kbAttribute = target.getAttribute(Attribute.KNOCKBACK_RESISTANCE)
                if (kbAttribute != null) {
                    originalKbValue = kbAttribute.baseValue
                    // 一時的にノックバック耐性を0に設定
                    kbAttribute.baseValue = 0.0
                }
            }

            // ノックバックを封じる場合は、ダメージ前のvelocityを保存
            if (preventKnockback) {
                originalVelocity = target.velocity.clone()
            }

            // ダメージを適用
            if (source != null) {
                target.damage(damage, source)
            } else {
                target.damage(damage)
            }

            // ノックバックを封じる場合は、元のvelocityを復元（後処理）
            if (preventKnockback && originalVelocity != null) {
                target.velocity = originalVelocity
            }

        } finally {
            // 元の値に戻す
            if (ignoreInvulnerability && originalNoDamageTicks != null) {
                target.noDamageTicks = originalNoDamageTicks
            }

            if (ignoreKnockbackResistance && kbAttribute != null && originalKbValue != null) {
                kbAttribute.baseValue = originalKbValue
            }
        }
    }

    private fun evaluateDamage(source: Entity, target: Entity): Double {
        return try {
            // 固定値ならそのまま返す
            amount.toDoubleOrNull() ?: run {
                // CEL式として評価
                val context = CELVariableProvider.buildTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(amount, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage amount: $amount", e)
            0.0
        }
    }

    private fun evaluateDamageFromPacket(source: PacketEntity, target: Entity): Double {
        return try {
            amount.toDoubleOrNull() ?: run {
                val context = CELVariableProvider.buildPacketEntityTargetContext(source, target)
                val evaluator = Unique.instance.celEvaluator
                evaluator.evaluateNumber(amount, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate damage amount: $amount", e)
            0.0
        }
    }

    override fun getDescription(): String = "Damage: $amount"
}
