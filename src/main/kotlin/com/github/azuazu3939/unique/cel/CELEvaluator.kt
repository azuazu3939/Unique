package com.github.azuazu3939.unique.cel

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.entity.Entity

/**
 * CEL評価ヘルパークラス
 *
 * 高レベルのCEL評価機能を提供
 * エンティティやプレイヤーから自動的にコンテキストを構築
 */
class CELEvaluator(private val engine: CELEngineManager) {

    /**
     * エンティティを使った条件評価
     *
     * @param expression CEL式
     * @param entity エンティティ
     * @return 評価結果
     */
    fun evaluateEntityCondition(expression: String, entity: Entity): Boolean {
        val context = buildEntityContext(entity)
        return engine.evaluateBoolean(expression, context)
    }

    /**
     * エンティティペアを使った条件評価（ターゲット用）
     *
     * @param expression CEL式
     * @param source ソースエンティティ
     * @param target ターゲットエンティティ
     * @return 評価結果
     */
    fun evaluateTargetCondition(
        expression: String,
        source: Entity,
        target: Entity
    ): Boolean {
        val context = buildTargetContext(source, target)
        return engine.evaluateBoolean(expression, context)
    }

    /**
     * 複数の条件を評価（AND）
     *
     * @param expressions 条件式リスト
     * @param entity エンティティ
     * @return 全ての条件がtrueの場合true
     */
    fun evaluateAllConditions(
        expressions: List<String>,
        entity: Entity
    ): Boolean {
        if (expressions.isEmpty()) return true

        val context = buildEntityContext(entity)
        return engine.evaluateAll(expressions, context)
    }

    /**
     * 複数の条件を評価（ターゲット付き、AND）
     */
    fun evaluateAllTargetConditions(
        expressions: List<String>,
        source: Entity,
        target: Entity
    ): Boolean {
        if (expressions.isEmpty()) return true

        val context = buildTargetContext(source, target)
        return engine.evaluateAll(expressions, context)
    }

    // ========== PacketEntity用の評価メソッド ==========

    /**
     * PacketEntityを使った条件評価
     *
     * @param expression CEL式
     * @param packetEntity PacketEntity
     * @return 評価結果
     */
    fun evaluatePacketEntityCondition(expression: String, packetEntity: PacketEntity): Boolean {
        val context = buildPacketEntityContext(packetEntity)
        return engine.evaluateBoolean(expression, context)
    }

    /**
     * PacketEntityとEntityのペアを使った条件評価（ターゲット用）
     *
     * @param expression CEL式
     * @param source PacketEntity
     * @param target ターゲットエンティティ
     * @return 評価結果
     */
    fun evaluatePacketEntityTargetCondition(
        expression: String,
        source: PacketEntity,
        target: Entity
    ): Boolean {
        val context = buildPacketEntityTargetContext(source, target)
        return engine.evaluateBoolean(expression, context)
    }

    /**
     * PacketEntity同士のペアを使った条件評価
     *
     * @param expression CEL式
     * @param source ソースPacketEntity
     * @param target ターゲットPacketEntity
     * @return 評価結果
     */
    fun evaluatePacketEntityPairCondition(
        expression: String,
        source: PacketEntity,
        target: PacketEntity
    ): Boolean {
        val context = buildPacketEntityPairContext(source, target)
        return engine.evaluateBoolean(expression, context)
    }

    /**
     * 複数の条件を評価（PacketEntity、AND）
     *
     * @param expressions 条件式リスト
     * @param packetEntity PacketEntity
     * @return 全ての条件がtrueの場合true
     */
    fun evaluateAllPacketEntityConditions(
        expressions: List<String>,
        packetEntity: PacketEntity
    ): Boolean {
        if (expressions.isEmpty()) return true

        val context = buildPacketEntityContext(packetEntity)
        return engine.evaluateAll(expressions, context)
    }

    /**
     * 複数の条件を評価（PacketEntityとターゲット、AND）
     */
    fun evaluateAllPacketEntityTargetConditions(
        expressions: List<String>,
        source: PacketEntity,
        target: Entity
    ): Boolean {
        if (expressions.isEmpty()) return true

        val context = buildPacketEntityTargetContext(source, target)
        return engine.evaluateAll(expressions, context)
    }

    /**
     * 数値計算式を評価（PacketEntity）
     *
     * @param expression CEL式
     * @param packetEntity PacketEntity
     * @return 計算結果
     */
    fun evaluateNumericExpressionForPacketEntity(expression: String, packetEntity: PacketEntity): Double {
        val context = buildPacketEntityContext(packetEntity)
        return engine.evaluateNumber(expression, context)
    }

    /**
     * 文字列式を評価（PacketEntity）
     *
     * @param expression CEL式
     * @param packetEntity PacketEntity
     * @return 文字列結果
     */
    fun evaluateStringExpressionForPacketEntity(expression: String, packetEntity: PacketEntity): String {
        val context = buildPacketEntityContext(packetEntity)
        return engine.evaluateString(expression, context)
    }

    // ========== 既存のEntity用メソッド ==========

    /**
     * 数値計算式を評価
     *
     * @param expression CEL式
     * @param entity エンティティ
     * @return 計算結果
     */
    fun evaluateNumericExpression(expression: String, entity: Entity): Double {
        val context = buildEntityContext(entity)
        return engine.evaluateNumber(expression, context)
    }

    /**
     * 文字列式を評価
     *
     * @param expression CEL式
     * @param entity エンティティ
     * @return 文字列結果
     */
    fun evaluateStringExpression(expression: String, entity: Entity): String {
        val context = buildEntityContext(entity)
        return engine.evaluateString(expression, context)
    }

    /**
     * カスタムコンテキストで評価
     *
     * @param expression CEL式
     * @param customContext カスタム変数
     * @return 評価結果
     */
    fun evaluateWithContext(
        expression: String,
        customContext: Map<String, Any>
    ): Any? {
        val fullContext = CELVariableProvider.buildFullContext(customContext)
        return engine.evaluate(expression, fullContext)
    }

    /**
     * 条件評価（カスタムコンテキスト）
     */
    fun evaluateBooleanWithContext(
        expression: String,
        customContext: Map<String, Any>
    ): Boolean {
        val fullContext = CELVariableProvider.buildFullContext(customContext)
        return engine.evaluateBoolean(expression, fullContext)
    }

    /**
     * 汎用評価（カスタムコンテキスト）
     */
    fun evaluate(
        expression: String,
        context: Map<String, Any>
    ): Any? {
        val fullContext = CELVariableProvider.buildFullContext(context)
        return engine.evaluate(expression, fullContext)
    }

    /**
     * Boolean評価（カスタムコンテキスト）
     */
    fun evaluateBoolean(
        expression: String,
        context: Map<String, Any>
    ): Boolean {
        val fullContext = CELVariableProvider.buildFullContext(context)
        return engine.evaluateBoolean(expression, fullContext)
    }

    /**
     * 数値評価（カスタムコンテキスト）
     */
    fun evaluateNumber(
        expression: String,
        context: Map<String, Any>
    ): Double {
        val fullContext = CELVariableProvider.buildFullContext(context)
        return engine.evaluateNumber(expression, fullContext)
    }

    /**
     * 文字列評価（カスタムコンテキスト）
     */
    fun evaluateString(
        expression: String,
        context: Map<String, Any>
    ): String {
        val fullContext = CELVariableProvider.buildFullContext(context)
        return engine.evaluateString(expression, fullContext)
    }

    /**
     * エンティティコンテキストを構築
     */
    private fun buildEntityContext(entity: Entity): Map<String, Any> {
        return CELVariableProvider.buildEntityContext(entity)
    }

    /**
     * ターゲットコンテキストを構築
     */
    private fun buildTargetContext(source: Entity, target: Entity): Map<String, Any> {
        return CELVariableProvider.buildTargetContext(source, target)
    }

    /**
     * PacketEntityコンテキストを構築
     */
    private fun buildPacketEntityContext(packetEntity: PacketEntity): Map<String, Any> {
        return CELVariableProvider.buildPacketEntityContext(packetEntity)
    }

    /**
     * PacketEntityとEntityのターゲットコンテキストを構築
     */
    private fun buildPacketEntityTargetContext(source: PacketEntity, target: Entity): Map<String, Any> {
        return CELVariableProvider.buildPacketEntityTargetContext(source, target)
    }

    /**
     * PacketEntity同士のペアコンテキストを構築
     */
    private fun buildPacketEntityPairContext(source: PacketEntity, target: PacketEntity): Map<String, Any> {
        return CELVariableProvider.buildPacketEntityPairContext(source, target)
    }

    /**
     * 式の妥当性をバッチ検証
     *
     * @param expressions 検証する式のリスト
     * @return エラーマップ（式 -> エラーメッセージ）
     */
    fun validateExpressions(expressions: List<String>): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        for (expression in expressions) {
            val error = engine.validate(expression)
            if (error != null) {
                errors[expression] = error
            }
        }

        return errors
    }

    /**
     * 設定ファイルの式を検証
     *
     * @param configName 設定ファイル名
     * @param expressions 検証する式
     * @return 妥当性（全てvalidならtrue）
     */
    fun validateConfigExpressions(
        configName: String,
        expressions: List<String>
    ): Boolean {
        DebugLogger.separator("Validating CEL Expressions: $configName")

        val errors = validateExpressions(expressions)

        if (errors.isEmpty()) {
            DebugLogger.info("✓ All ${expressions.size} expressions are valid")
            return true
        } else {
            DebugLogger.error("✗ Found ${errors.size} invalid expressions:")
            errors.forEach { (expr, error) ->
                DebugLogger.error("  '$expr' -> $error")
            }
            return false
        }
    }

    /**
     * テスト用: 式を評価してログ出力
     */
    fun testExpression(expression: String, entity: Entity) {
        DebugLogger.separator("Testing CEL Expression")
        DebugLogger.info("Expression: '$expression'")

        try {
            val context = buildEntityContext(entity)
            val result = engine.evaluate(expression, context)

            DebugLogger.info("Result: $result (${result?.javaClass?.simpleName})")
            DebugLogger.info("Context preview:")
            context.forEach { (key, value) ->
                if (value !is Map<*, *> && value !is Function<*>) {
                    DebugLogger.info("  $key = $value")
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Evaluation failed: ${e.message}", e)
        }

        DebugLogger.separator()
    }

    /**
     * デバッグ用: コンテキスト全体を表示
     */
    fun debugContext(entity: Entity) {
        val context = buildEntityContext(entity)

        DebugLogger.separator("CEL Context Debug")
        DebugLogger.info("Entity: ${entity.type.name}")
        CELVariableProvider.printContext(context)
        DebugLogger.separator()
    }

    companion object {
        /**
         * プラグインのCELEvaluatorインスタンスを取得
         */
        fun getInstance(): CELEvaluator {
            // 後でUnique.ktに追加される予定
            return Unique.instance.celEvaluator
        }
    }
}