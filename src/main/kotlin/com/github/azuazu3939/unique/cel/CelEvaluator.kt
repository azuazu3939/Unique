package com.github.azuazu3939.unique.cel

import dev.cel.common.CelAbstractSyntaxTree
import dev.cel.common.CelValidationException
import dev.cel.common.types.SimpleType
import dev.cel.compiler.CelCompiler
import dev.cel.compiler.CelCompilerFactory
import dev.cel.runtime.CelRuntime
import dev.cel.runtime.CelRuntimeFactory
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * CEL expression evaluator for dynamic mob behavior
 * Thread-safe and optimized for Folia's async execution
 */
class CelEvaluator {

    // Configure CEL compiler with common variables
    private val compiler: CelCompiler = CelCompilerFactory.standardCelCompilerBuilder()
        .addVar("player.distance", SimpleType.DOUBLE)
        .addVar("player.health", SimpleType.DOUBLE)
        .addVar("player.max_health", SimpleType.DOUBLE)
        .addVar("player.food_level", SimpleType.INT)
        .addVar("player_count", SimpleType.INT)
        .addVar("base_damage", SimpleType.DOUBLE)
        .addVar("missing_health_percent", SimpleType.DOUBLE)
        .addVar("target.distance", SimpleType.DOUBLE)
        .addVar("target.health", SimpleType.DOUBLE)
        .addVar("cooldown_ready", SimpleType.BOOL)
        .addVar("mob.health", SimpleType.DOUBLE)
        .addVar("mob.max_health", SimpleType.DOUBLE)
        .addVar("time", SimpleType.INT)
        .addVar("random", SimpleType.DOUBLE)
        .build()
    private val runtime: CelRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build()
    private val astCache = ConcurrentHashMap<String, CelAbstractSyntaxTree>()

    /**
     * Compile and cache a CEL expression
     */
    fun compile(expression: String): Result<CelAbstractSyntaxTree> {
        return try {
            val cached = astCache[expression]
            if (cached != null) {
                Result.success(cached)
            } else {
                val ast = compiler.compile(expression).ast
                astCache[expression] = ast
                Result.success(ast)
            }
        } catch (e: CelValidationException) {
            Result.failure(e)
        }
    }

    /**
     * Evaluate a CEL expression with provided context
     */
    fun evaluate(expression: String, context: Map<String, Any>): Result<Any> {
        val astResult = compile(expression)
        if (astResult.isFailure) {
            return Result.failure(astResult.exceptionOrNull()!!)
        }

        return try {
            val result = runtime.createProgram(astResult.getOrThrow())
                .eval(context)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evaluate as boolean (for conditions)
     */
    fun evaluateBoolean(expression: String, context: Map<String, Any>): Boolean {
        val result = evaluate(expression, context)
        return result.getOrNull() as? Boolean ?: false
    }

    /**
     * Evaluate as double (for damage calculations, etc.)
     */
    fun evaluateDouble(expression: String, context: Map<String, Any>): Double {
        val result = evaluate(expression, context)
        return when (val value = result.getOrNull()) {
            is Double -> value
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            else -> 0.0
        }
    }

    /**
     * Clear the AST cache (useful on reload)
     */
    fun clearCache() {
        astCache.clear()
    }

    companion object {
        /**
         * Create a context map for CEL evaluation
         */
        fun createContext(
            player: Player? = null,
            mobHealth: Double = 20.0,
            mobMaxHealth: Double = 20.0,
            baseDamage: Double = 0.0,
            targetDistance: Double = 0.0,
            cooldownReady: Boolean = true,
            additionalVars: Map<String, Any> = emptyMap()
        ): Map<String, Any> {
            val context = mutableMapOf<String, Any>()

            // Player-related variables
            if (player != null) {
                context["player.health"] = player.health
                context["player.max_health"] = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                context["player.food_level"] = player.foodLevel
                context["player.distance"] = targetDistance
            } else {
                context["player.health"] = 0.0
                context["player.max_health"] = 20.0
                context["player.food_level"] = 20
                context["player.distance"] = Double.MAX_VALUE
            }

            // Mob-related variables
            context["mob.health"] = mobHealth
            context["mob.max_health"] = mobMaxHealth
            context["missing_health_percent"] = if (mobMaxHealth > 0) {
                (mobMaxHealth - mobHealth) / mobMaxHealth
            } else {
                0.0
            }

            // Combat variables
            context["base_damage"] = baseDamage
            context["target.distance"] = targetDistance
            context["target.health"] = player?.health ?: 0.0
            context["cooldown_ready"] = cooldownReady

            // Utility variables
            context["time"] = System.currentTimeMillis().toInt()
            context["random"] = Math.random()
            context["player_count"] = player?.world?.players?.size ?: 0

            // Additional custom variables
            context.putAll(additionalVars)

            return context
        }
    }
}
