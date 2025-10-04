package com.github.azuazu3939.unique.condition

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * 条件基底クラス
 *
 * CEL式またはカスタムロジックで条件を評価
 */
abstract class Condition(
    val id: String,
    val expressions: List<String> = emptyList()
) {

    /**
     * 条件を評価（エンティティコンテキスト）
     *
     * @param entity 評価対象のエンティティ
     * @return 条件を満たす場合true
     */
    abstract fun evaluate(entity: Entity): Boolean

    /**
     * 条件を評価（PacketEntityコンテキスト）
     *
     * @param packetEntity 評価対象のPacketEntity
     * @return 条件を満たす場合true
     */
    abstract fun evaluate(packetEntity: PacketEntity): Boolean

    /**
     * 条件を評価（カスタムコンテキスト）
     *
     * @param context カスタムコンテキスト
     * @return 条件を満たす場合true
     */
    abstract fun evaluate(context: Map<String, Any>): Boolean

    /**
     * 条件の説明を取得
     */
    open fun getDescription(): String {
        return "Condition: $id"
    }

    /**
     * 条件のデバッグ情報
     */
    open fun debugInfo(): String {
        return "Condition[id=$id, expressions=${expressions.size}]"
    }
}

/**
 * CEL式条件
 *
 * CELエンジンを使用して条件を評価
 */
class CelCondition(
    id: String,
    expressions: List<String>
) : Condition(id, expressions) {

    private val celEvaluator = Unique.instance.celEvaluator

    /**
     * エンティティコンテキストで評価
     */
    override fun evaluate(entity: Entity): Boolean {
        if (expressions.isEmpty()) return true

        val result = celEvaluator.evaluateAllConditions(expressions, entity)
        DebugLogger.condition(expressions.joinToString(" && "), result)
        return result
    }

    /**
     * PacketEntityコンテキストで評価
     */
    override fun evaluate(packetEntity: PacketEntity): Boolean {
        if (expressions.isEmpty()) return true

        val result = celEvaluator.evaluateAllPacketEntityConditions(expressions, packetEntity)
        DebugLogger.condition(expressions.joinToString(" && "), result)
        return result
    }

    /**
     * カスタムコンテキストで評価
     */
    override fun evaluate(context: Map<String, Any>): Boolean {
        if (expressions.isEmpty()) return true

        val result = Unique.instance.celEngine.evaluateAll(expressions, context)
        DebugLogger.condition(expressions.joinToString(" && "), result)
        return result
    }

    override fun getDescription(): String {
        return if (expressions.isEmpty()) {
            "Always true"
        } else {
            expressions.joinToString(" AND ")
        }
    }

    override fun debugInfo(): String {
        return "CelCondition[id=$id, expressions=${expressions.joinToString(", ")}]"
    }
}

/**
 * 常にtrueを返す条件
 */
class AlwaysTrueCondition(id: String = "always_true") : Condition(id) {

    override fun evaluate(entity: Entity): Boolean = true

    override fun evaluate(packetEntity: PacketEntity): Boolean = true

    override fun evaluate(context: Map<String, Any>): Boolean = true

    override fun getDescription(): String = "Always true"

    override fun debugInfo(): String = "AlwaysTrueCondition[id=$id]"
}

/**
 * 常にfalseを返す条件
 */
class AlwaysFalseCondition(id: String = "always_false") : Condition(id) {

    override fun evaluate(entity: Entity): Boolean = false

    override fun evaluate(packetEntity: PacketEntity): Boolean = false

    override fun evaluate(context: Map<String, Any>): Boolean = false

    override fun getDescription(): String = "Always false"

    override fun debugInfo(): String = "AlwaysFalseCondition[id=$id]"
}

/**
 * 複数条件のAND
 */
class AndCondition(
    id: String,
    private val conditions: List<Condition>
) : Condition(id) {

    override fun evaluate(entity: Entity): Boolean {
        return conditions.all { it.evaluate(entity) }
    }

    override fun evaluate(packetEntity: PacketEntity): Boolean {
        return conditions.all { it.evaluate(packetEntity) }
    }

    override fun evaluate(context: Map<String, Any>): Boolean {
        return conditions.all { it.evaluate(context) }
    }

    override fun getDescription(): String {
        return conditions.joinToString(" AND ") { it.getDescription() }
    }

    override fun debugInfo(): String {
        return "AndCondition[id=$id, conditions=${conditions.size}]"
    }
}

/**
 * 複数条件のOR
 */
class OrCondition(
    id: String,
    private val conditions: List<Condition>
) : Condition(id) {

    override fun evaluate(entity: Entity): Boolean {
        return conditions.any { it.evaluate(entity) }
    }

    override fun evaluate(packetEntity: PacketEntity): Boolean {
        return conditions.any { it.evaluate(packetEntity) }
    }

    override fun evaluate(context: Map<String, Any>): Boolean {
        return conditions.any { it.evaluate(context) }
    }

    override fun getDescription(): String {
        return conditions.joinToString(" OR ") { it.getDescription() }
    }

    override fun debugInfo(): String {
        return "OrCondition[id=$id, conditions=${conditions.size}]"
    }
}

/**
 * 条件のNOT
 */
class NotCondition(
    id: String,
    private val condition: Condition
) : Condition(id) {

    override fun evaluate(entity: Entity): Boolean {
        return !condition.evaluate(entity)
    }

    override fun evaluate(packetEntity: PacketEntity): Boolean {
        return !condition.evaluate(packetEntity)
    }

    override fun evaluate(context: Map<String, Any>): Boolean {
        return !condition.evaluate(context)
    }

    override fun getDescription(): String {
        return "NOT (${condition.getDescription()})"
    }

    override fun debugInfo(): String {
        return "NotCondition[id=$id, condition=${condition.debugInfo()}]"
    }
}