package com.github.azuazu3939.unique.util

import kotlin.collections.fold
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 時間単位パーサー
 *
 * YAML設定から時間を読み取り、Kotlin Durationに変換
 * MCコルーチンのdelay()に直接使用可能
 */
object TimeParser {

    /**
     * 文字列を Duration に変換
     *
     * サポートする形式:
     * - "500ms" / "500millis" → 500ミリ秒
     * - "1s" / "1sec" / "1second" → 1秒
     * - "1.5s" → 1.5秒
     * - "20tick" / "20ticks" → 約1秒（20tick ≈ 1000ms）
     * - "500" → 500ミリ秒（単位なしはデフォルトでms）
     *
     * @param input 時間文字列
     * @return Duration オブジェクト
     * @throws IllegalArgumentException 無効な形式の場合
     */
    fun parse(input: String): Duration {
        val trimmed = input.trim().lowercase()

        // 空文字列チェック
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Time string cannot be empty")
        }

        return when {
            // ミリ秒
            trimmed.endsWith("ms") || trimmed.endsWith("millis") -> {
                val value = extractNumber(trimmed, listOf("ms", "millis"))
                value.milliseconds
            }

            // 秒
            trimmed.endsWith("s") || trimmed.endsWith("sec") || trimmed.endsWith("second") -> {
                val value = extractNumber(trimmed, listOf("s", "sec", "second"))
                value.seconds
            }

            // Tick（非推奨だが互換性のため対応）
            trimmed.endsWith("tick") || trimmed.endsWith("ticks") -> {
                val ticks = extractNumber(trimmed, listOf("tick", "ticks"))
                ticksToMillis(ticks.toLong()).milliseconds
            }

            // 単位なし → ミリ秒として扱う
            else -> {
                val value = trimmed.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid time format: $input")
                value.milliseconds
            }
        }
    }

    /**
     * 数値部分を抽出
     */
    private fun extractNumber(input: String, suffixes: List<String>): Double {
        var numberPart = input
        for (suffix in suffixes) {
            if (numberPart.endsWith(suffix)) {
                numberPart = numberPart.removeSuffix(suffix).trim()
                break
            }
        }

        return numberPart.toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid number format: $input")
    }

    /**
     * Tickをミリ秒に変換
     *
     * 注意: Tickは可変であり、サーバーのTPS（20が基準）に依存
     * 1tick ≈ 50ms（TPS=20の場合）
     *
     * @param ticks Tick数
     * @return ミリ秒
     */
    fun ticksToMillis(ticks: Long): Long {
        return ticks * 50L
    }

    /**
     * ミリ秒をTickに変換
     *
     * @param millis ミリ秒
     * @return Tick数
     */
    fun millisToTicks(millis: Long): Long {
        return millis / 50L
    }

    /**
     * Duration を人間が読みやすい形式に変換
     *
     * @param duration Duration
     * @return 人間が読みやすい文字列（例: "1.5s", "500ms"）
     */
    fun format(duration: Duration): String {
        val millis = duration.inWholeMilliseconds

        return when {
            millis == 0L -> "0ms"
            millis % 1000 == 0L -> "${millis / 1000}s"
            millis >= 1000 -> "${millis / 1000.0}s"
            else -> "${millis}ms"
        }
    }

    /**
     * Duration を詳細形式に変換
     *
     * @param duration Duration
     * @return 詳細文字列（例: "1s 500ms", "2m 30s"）
     */
    fun formatDetailed(duration: Duration): String {
        val totalMillis = duration.inWholeMilliseconds

        if (totalMillis == 0L) return "0ms"

        val minutes = totalMillis / 60000
        val seconds = (totalMillis % 60000) / 1000
        val millis = totalMillis % 1000

        val parts = mutableListOf<String>()

        if (minutes > 0) parts.add("${minutes}m")
        if (seconds > 0) parts.add("${seconds}s")
        if (millis > 0 || parts.isEmpty()) parts.add("${millis}ms")

        return parts.joinToString(" ")
    }

    /**
     * 2つのDurationを比較
     *
     * @return 差分（duration1 - duration2）
     */
    fun diff(duration1: Duration, duration2: Duration): Duration {
        return duration1 - duration2
    }

    /**
     * 複数のDurationを合計
     */
    fun sum(durations: Array<Duration>): Duration {
        return durations.fold(Duration.ZERO) { acc, duration -> acc + duration }
    }

    /**
     * バリデーション: 負の値チェック
     */
    fun requireNonNegative(duration: Duration, fieldName: String = "duration") {
        require(!duration.isNegative()) {
            "$fieldName cannot be negative: ${format(duration)}"
        }
    }

    /**
     * バリデーション: 範囲チェック
     */
    fun requireInRange(
        duration: Duration,
        min: Duration,
        max: Duration,
        fieldName: String = "duration"
    ) {
        require(duration in min..max) {
            "$fieldName must be between ${format(min)} and ${format(max)}, got ${format(duration)}"
        }
    }
}