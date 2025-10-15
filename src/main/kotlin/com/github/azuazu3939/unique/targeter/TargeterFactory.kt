package com.github.azuazu3939.unique.targeter

// Basic targeters

// Player targeters

// Entity targeters

// Sorting targeters

// Advanced targeters
import com.github.azuazu3939.unique.mob.TargeterDefinition
import com.github.azuazu3939.unique.targeter.types.advanced.*
import com.github.azuazu3939.unique.targeter.types.basic.SelfTargeter
import com.github.azuazu3939.unique.targeter.types.entity.LineOfSightTargeter
import com.github.azuazu3939.unique.targeter.types.entity.RadiusEntitiesTargeter
import com.github.azuazu3939.unique.targeter.types.player.NearestPlayerTargeter
import com.github.azuazu3939.unique.targeter.types.player.RadiusPlayersTargeter
import com.github.azuazu3939.unique.targeter.types.sorting.*
import com.github.azuazu3939.unique.util.DebugLogger

/**
 * TargeterFactory - Targeter定義からTargeterインスタンスを生成
 *
 * YAMLから読み込まれたTargeterDefinitionを実際のTargeterオブジェクトに変換します。
 */
object TargeterFactory {

    /**
     * Targeter定義からTargeterインスタンスを生成
     *
     * @param definition Targeter定義
     * @return Targeterインスタンス、失敗時はnull
     */
    fun createTargeter(definition: TargeterDefinition): Targeter? {
        return try {
            when (definition.type.lowercase()) {
                // ========== 基本Targeter ==========
                "self" -> SelfTargeter(filter = definition.filter)

                "nearestplayer" -> NearestPlayerTargeter(
                    range = definition.range,
                    filter = definition.filter
                )

                "radiusplayers" -> RadiusPlayersTargeter(
                    range = definition.range,
                    filter = definition.filter
                )

                "radiusentities" -> RadiusEntitiesTargeter(
                    range = definition.range,
                    filter = definition.filter
                )

                "lineofsight" -> LineOfSightTargeter(
                    maxDistance = definition.maxDistance,
                    filter = definition.filter
                )

                // ========== 新Targeter ==========
                "lowesthealth" -> createLowestHealthTargeter(definition)
                "highesthealth" -> createHighestHealthTargeter(definition)
                "nearest" -> createNearestTargeter(definition)
                "farthest" -> createFarthestTargeter(definition)
                "threat" -> createThreatTargeter(definition)

                // ========== 複合Targeter ==========
                "random" -> createRandomTargeter(definition)
                "area" -> createAreaTargeter(definition)
                "chain" -> createChainTargeter(definition)

                else -> {
                    DebugLogger.warn("Unknown targeter type: ${definition.type}")
                    null
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to create targeter: ${definition.type}", e)
            null
        }
    }

    // ========================================
    // 新Targeter生成メソッド
    // ========================================

    private fun createLowestHealthTargeter(def: TargeterDefinition): LowestHealthTargeter? {
        val baseTargeter = def.baseTargeter?.let { createTargeter(it) } ?: run {
            DebugLogger.error("LowestHealthTargeter requires baseTargeter")
            return null
        }

        return LowestHealthTargeter(
            baseTargeter = baseTargeter,
            filter = def.filter
        )
    }

    private fun createHighestHealthTargeter(def: TargeterDefinition): HighestHealthTargeter? {
        val baseTargeter = def.baseTargeter?.let { createTargeter(it) } ?: run {
            DebugLogger.error("HighestHealthTargeter requires baseTargeter")
            return null
        }

        return HighestHealthTargeter(
            baseTargeter = baseTargeter,
            filter = def.filter
        )
    }

    private fun createNearestTargeter(def: TargeterDefinition): NearestTargeter? {
        val baseTargeter = def.baseTargeter?.let { createTargeter(it) } ?: run {
            DebugLogger.error("NearestTargeter requires baseTargeter")
            return null
        }

        return NearestTargeter(
            baseTargeter = baseTargeter,
            filter = def.filter
        )
    }

    private fun createFarthestTargeter(def: TargeterDefinition): FarthestTargeter? {
        val baseTargeter = def.baseTargeter?.let { createTargeter(it) } ?: run {
            DebugLogger.error("FarthestTargeter requires baseTargeter")
            return null
        }

        return FarthestTargeter(
            baseTargeter = baseTargeter,
            filter = def.filter
        )
    }

    private fun createThreatTargeter(def: TargeterDefinition): ThreatTargeter? {
        val baseTargeter = def.baseTargeter?.let { createTargeter(it) } ?: run {
            DebugLogger.error("ThreatTargeter requires baseTargeter")
            return null
        }

        return ThreatTargeter(
            baseTargeter = baseTargeter,
            count = def.count,
            filter = def.filter
        )
    }

    // ========================================
    // 複合Targeter生成メソッド
    // ========================================

    private fun createRandomTargeter(def: TargeterDefinition): RandomTargeter? {
        val baseTargeter = def.baseTargeter?.let { createTargeter(it) } ?: run {
            DebugLogger.error("RandomTargeter requires baseTargeter")
            return null
        }

        return RandomTargeter(
            baseTargeter = baseTargeter,
            count = def.count,
            filter = def.filter
        )
    }

    private fun createAreaTargeter(def: TargeterDefinition): AreaTargeter? {
        val shapeName = def.shape ?: run {
            DebugLogger.error("AreaTargeter requires shape field")
            return null
        }

        val shape = try {
            AreaShape.valueOf(shapeName.uppercase())
        } catch (e: IllegalArgumentException) {
            DebugLogger.error("Invalid area shape: $shapeName", e)
            return null
        }

        val direction = def.direction?.let {
            try {
                Direction.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                DebugLogger.warn("Invalid direction: $it, using FORWARD")
                Direction.FORWARD
            }
        } ?: Direction.FORWARD

        return AreaTargeter(
            shape = shape,
            radius = def.radius ?: def.range.toString(),
            innerRadius = def.innerRadius ?: "0.0",
            angle = def.angle ?: "45.0",
            width = def.width ?: "10.0",
            height = def.height ?: "10.0",
            depth = def.depth ?: "10.0",
            direction = direction,
            targetPlayers = def.targetPlayers,
            targetMobs = def.targetMobs,
            filter = def.filter
        )
    }

    private fun createChainTargeter(def: TargeterDefinition): ChainTargeter? {
        val initialTargeter = def.initialTargeter?.let { createTargeter(it) } ?: run {
            DebugLogger.error("ChainTargeter requires initialTargeter")
            return null
        }

        return ChainTargeter(
            initialTargeter = initialTargeter,
            maxChains = def.maxChains ?: "5",
            chainRange = def.chainRange ?: "5.0",
            chainCondition = def.chainCondition,
            filter = def.filter
        )
    }

    /**
     * Targeterタイプの検証
     *
     * @param type Targeterタイプ
     * @return 有効な場合true
     */
    fun isValidTargeterType(type: String): Boolean {
        return type.lowercase() in listOf(
            // 基本
            "self", "nearestplayer", "radiusplayers", "radiusentities", "lineofsight",
            // 新Targeter
            "lowesthealth", "highesthealth", "nearest", "farthest", "threat",
            // 複合
            "random", "area", "chain"
        )
    }

    /**
     * 利用可能なTargeterタイプ一覧を取得
     */
    fun getAvailableTargeterTypes(): List<String> {
        return listOf(
            "self", "nearestplayer", "radiusplayers", "radiusentities", "lineofsight",
            "lowesthealth", "highesthealth", "nearest", "farthest", "threat",
            "random", "area", "chain"
        )
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.info("=== TargeterFactory Debug Info ===")
        DebugLogger.info("Available targeter types: ${getAvailableTargeterTypes().joinToString(", ")}")
        DebugLogger.info("Total targeter types: ${getAvailableTargeterTypes().size}")
    }
}
