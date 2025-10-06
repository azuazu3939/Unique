package com.github.azuazu3939.unique.cel

import com.github.azuazu3939.unique.util.DebugLogger
import dev.cel.common.CelAbstractSyntaxTree
import dev.cel.common.CelValidationException
import dev.cel.common.types.SimpleType
import dev.cel.compiler.CelCompiler
import dev.cel.compiler.CelCompilerFactory
import dev.cel.runtime.CelEvaluationException
import dev.cel.runtime.CelRuntime
import dev.cel.runtime.CelRuntimeFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * CELエンジン管理クラス
 *
 * CEL式のコンパイル、キャッシュ、評価を管理
 */
class CELEngineManager(
    private val cacheSize: Int = 1000,
    private val evaluationTimeout: Long = 100L
) {

    /**
     * CELコンパイラ
     * 変数宣言を含む環境を設定
     * 遅延初期化でClassLoaderを適切に設定
     */
    private val compiler: CelCompiler by lazy {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = this::class.java.classLoader

        try {
            CelCompilerFactory.standardCelCompilerBuilder()
                .addVar("entity", SimpleType.DYN)
                .addVar("player", SimpleType.DYN)
                .addVar("world", SimpleType.DYN)
                .addVar("damage", SimpleType.DYN)
                .addVar("attacker", SimpleType.DYN)
                .addVar("victim", SimpleType.DYN)
                .addVar("target", SimpleType.DYN)
                .addVar("item", SimpleType.DYN)
                .addVar("location", SimpleType.DYN)
                .addVar("math", SimpleType.DYN)
                .addVar("string", SimpleType.DYN)
                .addVar("biome", SimpleType.DYN)
                .addVar("cooldown", SimpleType.DYN)
                .addVar("event", SimpleType.DYN)
                .addVar("skill", SimpleType.DYN)
                .addVar("spawner", SimpleType.DYN)
                .build()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    /**
     * CELランタイム
     * 遅延初期化でClassLoaderを適切に設定
     */
    private val runtime: CelRuntime by lazy {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = this::class.java.classLoader

        try {
            CelRuntimeFactory.standardCelRuntimeBuilder()
                .build()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    /**
     * コンパイル済みASTキャッシュ
     * 同じ式を何度もコンパイルしないようにキャッシュ
     */
    private val astCache = object : LinkedHashMap<String, CelAbstractSyntaxTree>(
        cacheSize,
        0.75f,
        true // アクセス順序でソート
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CelAbstractSyntaxTree>?): Boolean {
            return size > cacheSize
        }
    }

    /**
     * キャッシュアクセスの同期用
     */
    private val cacheLock = Any()

    /**
     * 統計情報
     */
    private val stats = ConcurrentHashMap<String, Long>()

    init {
        stats["compilations"] = 0L
        stats["cache_hits"] = 0L
        stats["cache_misses"] = 0L
        stats["evaluations"] = 0L
        stats["errors"] = 0L

        DebugLogger.info("CELEngineManager initialized")
        DebugLogger.debug("Cache size: $cacheSize")
        DebugLogger.debug("Evaluation timeout: ${evaluationTimeout}ms")
    }

    /**
     * CEL式をコンパイル（キャッシュ使用）
     *
     * @param expression CEL式
     * @return コンパイル済みAST
     * @throws CelValidationException コンパイルエラー
     */
    fun compile(expression: String): CelAbstractSyntaxTree {
        // キャッシュチェック
        synchronized(cacheLock) {
            astCache[expression]?.let {
                incrementStat("cache_hits")
                DebugLogger.verbose("Cache hit for expression: '$expression'")
                return it
            }
        }

        // キャッシュミス - 新規コンパイル
        incrementStat("cache_misses")
        incrementStat("compilations")

        val startTime = System.nanoTime()

        // ClassLoaderを明示的に設定（Hopliteと同様の対処）
        val originalClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = this::class.java.classLoader

        try {
            val ast = compiler.compile(expression).ast
            val duration = (System.nanoTime() - startTime) / 1_000_000L

            // キャッシュに追加
            synchronized(cacheLock) {
                astCache[expression] = ast
            }

            DebugLogger.verbose("Compiled expression: '$expression' (${duration}ms)")
            return ast

        } catch (e: CelValidationException) {
            incrementStat("errors")
            DebugLogger.error("CEL compilation error: ${e.message}")
            DebugLogger.debug("Expression: '$expression'")
            throw e
        } catch (e: ExceptionInInitializerError) {
            incrementStat("errors")
            DebugLogger.error("CEL initialization error: ${e.message}")
            DebugLogger.error("Caused by: ${e.cause?.message}")
            e.cause?.printStackTrace()
            throw e
        } catch (e: Exception) {
            incrementStat("errors")
            DebugLogger.error("Unexpected CEL error: ${e.javaClass.name} - ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            // ClassLoaderを元に戻す
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    /**
     * CEL式を評価（汎用）
     *
     * @param expression CEL式
     * @param context 変数コンテキスト
     * @return 評価結果
     */
    fun evaluate(expression: String, context: Map<String, Any>): Any? {
        incrementStat("evaluations")

        val startTime = System.nanoTime()

        try {
            val ast = compile(expression)
            val program = runtime.createProgram(ast)
            val result = program.eval(context)

            val duration = (System.nanoTime() - startTime) / 1_000_000L
            DebugLogger.celEvaluation(expression, result, duration)

            // タイムアウトチェック（警告のみ）
            if (duration > evaluationTimeout) {
                DebugLogger.warn("CEL evaluation exceeded timeout: ${duration}ms > ${evaluationTimeout}ms")
                DebugLogger.debug("Expression: '$expression'")
            }

            return result

        } catch (e: CelEvaluationException) {
            incrementStat("errors")
            DebugLogger.error("CEL evaluation error: ${e.message}")
            DebugLogger.debug("Expression: '$expression'")
            DebugLogger.debug("Context: $context")
            throw e
        } catch (e: Exception) {
            incrementStat("errors")
            DebugLogger.error("Unexpected error during CEL evaluation", e)
            throw e
        }
    }

    /**
     * Boolean型として評価（条件判定用）
     *
     * @param expression CEL式
     * @param context 変数コンテキスト
     * @return 評価結果（Boolean）
     */
    fun evaluateBoolean(expression: String, context: Map<String, Any>): Boolean {
        return when (val result = evaluate(expression, context)) {
            is Boolean -> result
            null -> false
            else -> {
                DebugLogger.warn("CEL expression did not return boolean: '$expression' -> $result")
                false
            }
        }
    }

    /**
     * Number型として評価
     *
     * @param expression CEL式
     * @param context 変数コンテキスト
     * @return 評価結果（Double）
     */
    fun evaluateNumber(expression: String, context: Map<String, Any>): Double {
        return when (val result = evaluate(expression, context)) {
            is Number -> result.toDouble()
            is String -> result.toDoubleOrNull() ?: 0.0
            null -> 0.0
            else -> {
                DebugLogger.warn("CEL expression did not return number: '$expression' -> $result")
                0.0
            }
        }
    }

    /**
     * String型として評価
     *
     * @param expression CEL式
     * @param context 変数コンテキスト
     * @return 評価結果（String）
     */
    fun evaluateString(expression: String, context: Map<String, Any>): String {
        val result = evaluate(expression, context)
        return result?.toString() ?: ""
    }

    /**
     * 複数の条件式を評価（AND条件）
     *
     * @param expressions 条件式のリスト
     * @param context 変数コンテキスト
     * @return 全ての条件がtrueの場合true
     */
    fun evaluateAll(expressions: List<String>, context: Map<String, Any>): Boolean {
        if (expressions.isEmpty()) return true

        for (expression in expressions) {
            if (!evaluateBoolean(expression, context)) {
                DebugLogger.condition(expression, false)
                return false
            }
            DebugLogger.condition(expression, true)
        }

        return true
    }

    /**
     * 複数の条件式を評価（OR条件）
     *
     * @param expressions 条件式のリスト
     * @param context 変数コンテキスト
     * @return いずれかの条件がtrueの場合true
     */
    fun evaluateAny(expressions: List<String>, context: Map<String, Any>): Boolean {
        if (expressions.isEmpty()) return false

        for (expression in expressions) {
            if (evaluateBoolean(expression, context)) {
                DebugLogger.condition(expression, true)
                return true
            }
            DebugLogger.condition(expression, false)
        }

        return false
    }

    /**
     * 式の妥当性チェック（コンパイルのみ）
     *
     * @param expression CEL式
     * @return エラーメッセージ（エラーがない場合null）
     */
    fun validate(expression: String): String? {
        return try {
            compile(expression)
            null
        } catch (e: CelValidationException) {
            e.message
        }
    }

    /**
     * キャッシュをクリア
     */
    fun clearCache() {
        synchronized(cacheLock) {
            val size = astCache.size
            astCache.clear()
            DebugLogger.info("CEL cache cleared ($size entries)")
        }
    }

    /**
     * 統計情報を取得
     */
    fun getStats(): Map<String, Long> {
        return stats.toMap()
    }

    /**
     * 統計情報をリセット
     */
    fun resetStats() {
        stats.keys.forEach { stats[it] = 0L }
        DebugLogger.debug("CEL statistics reset")
    }

    /**
     * 統計カウンターをインクリメント
     */
    private fun incrementStat(key: String) {
        stats.compute(key) { _, v -> (v ?: 0L) + 1L }
    }

    /**
     * キャッシュサイズを取得
     */
    fun getCacheSize(): Int {
        return synchronized(cacheLock) {
            astCache.size
        }
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("CEL Engine Debug Info")
        DebugLogger.info("Cache size: ${getCacheSize()} / $cacheSize")
        DebugLogger.info("Statistics:")
        stats.forEach { (key, value) ->
            DebugLogger.info("  $key: $value")
        }

        val hitRate = if (stats["cache_hits"]!! + stats["cache_misses"]!! > 0) {
            (stats["cache_hits"]!! * 100.0) / (stats["cache_hits"]!! + stats["cache_misses"]!!)
        } else {
            0.0
        }
        DebugLogger.info("  cache_hit_rate: ${"%.2f".format(hitRate)}%")
        DebugLogger.separator()
    }
}