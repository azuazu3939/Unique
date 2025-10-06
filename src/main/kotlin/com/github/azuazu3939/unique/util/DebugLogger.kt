package com.github.azuazu3939.unique.util

import com.github.azuazu3939.unique.Unique
import java.util.logging.Level

/**
 * デバッグログユーティリティ
 *
 * レベル別のログ出力とデバッグモードの切り替えに対応
 */
object DebugLogger {

    /**
     * デバッグモード（config.ymlで制御予定）
     */
    var debugMode: Boolean = true
        private set

    /**
     * 詳細ログモード
     */
    var verboseMode: Boolean = false
        private set

    /**
     * デバッグモードを設定
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        info("Debug mode: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    /**
     * 詳細モードを設定
     */
    fun setVerboseMode(enabled: Boolean) {
        verboseMode = enabled
        info("Verbose mode: ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    /**
     * 情報ログ（常に表示）
     */
    fun info(message: String) {
        log(Level.INFO, message)
    }

    /**
     * 警告ログ（常に表示）
     */
    fun warn(message: String) {
        log(Level.WARNING, message)
    }

    /**
     * エラーログ（常に表示）
     */
    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Unique.instance.logger.log(Level.SEVERE, message, throwable)
        } else {
            log(Level.SEVERE, message)
        }
    }

    /**
     * デバッグログ（デバッグモードでのみ表示）
     */
    fun debug(message: String) {
        if (debugMode) {
            log(Level.INFO, "§7[DEBUG] $message")
        }
    }

    /**
     * 詳細ログ（詳細モードでのみ表示）
     */
    fun verbose(message: String) {
        if (verboseMode) {
            log(Level.INFO, "§8[VERBOSE] $message")
        }
    }

    /**
     * スキル実行ログ（デバッグモード用）
     */
    fun skillExecution(skillName: String, phase: String, duration: Long) {
        if (debugMode) {
            debug("§e[SKILL] §f$skillName §7[$phase] §7(${duration}ms)")
        }
    }

    /**
     * CEL評価ログ（詳細モード用）
     */
    fun celEvaluation(expression: String, result: Any?, duration: Long) {
        if (verboseMode) {
            verbose("§b[CEL] §f'$expression' §7-> §f$result §7(${duration}ms)")
        }
    }

    /**
     * タイミングログ（詳細モード用）
     */
    fun timing(operation: String, duration: Long) {
        if (verboseMode) {
            verbose("§d[TIMING] §f$operation §7took ${duration}ms")
        }
    }

    /**
     * 条件評価ログ（デバッグモード用）
     */
    fun condition(condition: String, result: Boolean) {
        if (debugMode) {
            val resultColor = if (result) "§a" else "§c"
            debug("§6[CONDITION] §f'$condition' §7-> $resultColor$result")
        }
    }

    /**
     * ターゲッターログ（デバッグモード用）
     */
    fun targeter(targeterType: String, targetCount: Int) {
        if (debugMode) {
            debug("§3[TARGETER] §f$targeterType §7found §f$targetCount §7target(s)")
        }
    }

    fun targeter(targeterType: String, content: String) {
        if (debugMode) {
            debug("§3[TARGETER] §f$targeterType §7found §f$content §7target(s)")
        }
    }

    /**
     * エフェクトログ（詳細モード用）
     */
    fun effect(effectType: String, target: String) {
        if (verboseMode) {
            verbose("§5[EFFECT] §f$effectType §7applied to §f$target")
        }
    }

    fun skill(content: String) {
        if (debugMode) {
            debug("§a[SKILL] §f$content")
        }
    }

    /**
     * スポーンログ（デバッグモード用）
     */
    fun spawn(mobType: String, location: String) {
        if (debugMode) {
            debug("§2[SPAWN] §f$mobType §7at §f$location")
        }
    }

    /**
     * 内部ログメソッド
     */
    private fun log(level: Level, message: String) {
        Unique.instance.logger.log(level, message)
    }

    /**
     * セパレーター（視認性向上）
     */
    fun separator(title: String = "") {
        if (debugMode) {
            if (title.isEmpty()) {
                debug("§8" + "═".repeat(50))
            } else {
                val padding = (50 - title.length - 2) / 2
                debug("§8" + "═".repeat(padding) + " §f$title §8" + "═".repeat(padding))
            }
        }
    }

    /**
     * スタックトレースをデバッグ出力
     */
    fun printStackTrace(throwable: Throwable) {
        if (debugMode) {
            error("Exception occurred:", throwable)
        }
    }
}