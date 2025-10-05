package com.github.azuazu3939.unique.cel

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.util.biomeName
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.math.*

/**
 * CEL変数提供クラス（拡張版）
 *
 * YAMLとCELで全ての機能を実現するための変数群を提供
 */
object CELVariableProvider {

    /**
     * エンティティ用のコンテキストを構築
     */
    fun buildEntityContext(entity: Entity): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // 基本的なエンティティ情報
        context["entity"] = buildEntityInfo(entity)

        // ワールド情報
        context["world"] = buildWorldInfo(entity.world)

        // 環境情報
        context["environment"] = buildEnvironmentInfo(entity.location)

        // 周囲のエンティティ情報
        context["nearbyPlayerCount"] = getNearbyPlayers(entity).size
        context["nearbyMobCount"] = getNearbyMobs(entity).size
        context["nearbyPlayers"] = buildNearbyPlayersInfo(entity)

        // 数学関数
        context["math"] = buildMathFunctions()

        // ランダム関数
        context["random"] = buildRandomFunctions()

        // 距離計算関数
        context["distance"] = buildDistanceFunctions()

        // 文字列関数
        context["string"] = buildStringFunctions()

        return context
    }

    /**
     * PacketEntity用のコンテキストを構築
     */
    fun buildPacketEntityContext(packetEntity: PacketEntity): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // PacketEntity情報
        context["entity"] = buildPacketEntityInfo(packetEntity)

        // ワールド情報
        val world = packetEntity.location.world
        if (world != null) {
            context["world"] = buildWorldInfo(world)
            context["environment"] = buildEnvironmentInfo(packetEntity.location)

            // 周囲のエンティティ情報
            val nearbyPlayers = world.getNearbyEntities(
                packetEntity.location, 20.0, 20.0, 20.0
            ).filterIsInstance<Player>()

            context["nearbyPlayerCount"] = nearbyPlayers.size
            context["nearbyPlayers"] = buildPlayersInfo(nearbyPlayers)
        }

        // 関数群
        context["math"] = buildMathFunctions()
        context["random"] = buildRandomFunctions()
        context["distance"] = buildDistanceFunctions()
        context["string"] = buildStringFunctions()

        return context
    }

    /**
     * ターゲット付きコンテキストを構築
     */
    fun buildTargetContext(source: Entity, target: Entity): Map<String, Any> {
        val context = buildEntityContext(source).toMutableMap()

        // ターゲット情報を追加
        context["target"] = buildEntityInfo(target)

        // ソース情報を明示的に追加
        context["source"] = context["entity"]!!

        return context
    }

    /**
     * エンティティ情報を構築
     */
    private fun buildEntityInfo(entity: Entity): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        info["type"] = entity.type.name
        info["uuid"] = entity.uniqueId.toString()
        info["isDead"] = entity.isDead
        info["age"] = entity.ticksLived

        // 位置情報
        info["location"] = buildLocationInfo(entity.location)

        // LivingEntity固有の情報
        if (entity is LivingEntity) {
            info["health"] = entity.health
            info["maxHealth"] = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        }

        // Player固有の情報
        if (entity is Player) {
            info["name"] = entity.name
            info["level"] = entity.level
            info["exp"] = entity.exp
            info["foodLevel"] = entity.foodLevel
            info["gameMode"] = entity.gameMode.name
            info["isFlying"] = entity.isFlying
            info["isSneaking"] = entity.isSneaking
            info["isSprinting"] = entity.isSprinting
        }

        return info
    }

    /**
     * PacketEntity情報を構築
     */
    private fun buildPacketEntityInfo(packetEntity: PacketEntity): Map<String, Any> {
        return mapOf(
            "entityId" to packetEntity.entityId,
            "location" to buildLocationInfo(packetEntity.location)
        )
    }

    /**
     * 位置情報を構築
     */
    private fun buildLocationInfo(location: Location): Map<String, Any> {
        return mapOf(
            "x" to location.x,
            "y" to location.y,
            "z" to location.z,
            "yaw" to location.yaw.toDouble(),
            "pitch" to location.pitch.toDouble(),
            "world" to (location.world?.name ?: "unknown")
        )
    }

    /**
     * ワールド情報を構築
     */
    private fun buildWorldInfo(world: org.bukkit.World): Map<String, Any> {
        return mapOf(
            "name" to world.name,
            "time" to world.time,
            "fullTime" to world.fullTime,
            "isDay" to (world.time !in 12300..23850),
            "isNight" to (world.time in 12300..23850),
            "hasStorm" to world.hasStorm(),
            "isThundering" to world.isThundering,
            "difficulty" to world.difficulty.name,
            "playerCount" to world.players.size
        )
    }

    /**
     * 環境情報を構築
     */
    private fun buildEnvironmentInfo(location: Location): Map<String, Any> {
        val world = location.world ?: return emptyMap()

        return mapOf(
            "moonPhase" to ((world.fullTime / 24000) % 8).toInt(),
            "dayOfCycle" to (world.fullTime / 24000).toInt(),
            "tickOfDay" to (world.time % 24000).toInt(),
            "biome" to location.block.biome.biomeName()
        )
    }

    /**
     * 周囲のプレイヤー情報を構築
     */
    private fun buildNearbyPlayersInfo(entity: Entity): Map<String, Any> {
        val nearbyPlayers = getNearbyPlayers(entity)

        return if (nearbyPlayers.isEmpty()) {
            mapOf(
                "avgLevel" to 0,
                "maxLevel" to 0,
                "minLevel" to 0,
                "count" to 0
            )
        } else {
            mapOf(
                "avgLevel" to nearbyPlayers.map { it.level }.average(),
                "maxLevel" to nearbyPlayers.maxOf { it.level },
                "minLevel" to nearbyPlayers.minOf { it.level },
                "count" to nearbyPlayers.size
            )
        }
    }

    /**
     * プレイヤーリスト情報を構築
     */
    private fun buildPlayersInfo(players: List<Player>): Map<String, Any> {
        return if (players.isEmpty()) {
            mapOf(
                "avgLevel" to 0,
                "maxLevel" to 0,
                "minLevel" to 0,
                "count" to 0
            )
        } else {
            mapOf(
                "avgLevel" to players.map { it.level }.average(),
                "maxLevel" to players.maxOf { it.level },
                "minLevel" to players.minOf { it.level },
                "count" to players.size
            )
        }
    }

    /**
     * 周囲のプレイヤーを取得
     */
    private fun getNearbyPlayers(entity: Entity): List<Player> {
        return entity.world.getNearbyEntities(
            entity.location, 20.0, 20.0, 20.0
        ).filterIsInstance<Player>()
    }

    /**
     * 周囲のMobを取得
     */
    private fun getNearbyMobs(entity: Entity): List<LivingEntity> {
        return entity.world.getNearbyEntities(
            entity.location, 20.0, 20.0, 20.0
        ).filterIsInstance<LivingEntity>()
            .filter { it !is Player && it != entity }
    }

    /**
     * 数学関数を構築
     */
    private fun buildMathFunctions(): Map<String, Any> {
        return mapOf(
            // 基本関数
            "abs" to { value: Double -> abs(value) },
            "max" to { a: Double, b: Double -> max(a, b) },
            "min" to { a: Double, b: Double -> min(a, b) },
            "floor" to { value: Double -> floor(value) },
            "ceil" to { value: Double -> ceil(value) },
            "round" to { value: Double -> round(value) },
            "sqrt" to { value: Double -> sqrt(value) },
            "pow" to { base: Double, exp: Double -> base.pow(exp) },

            // 三角関数
            "toRadians" to { degrees: Double -> Math.toRadians(degrees) },
            "toDegrees" to { radians: Double -> Math.toDegrees(radians) },
            "cos" to { radians: Double -> cos(radians) },
            "sin" to { radians: Double -> sin(radians) },
            "tan" to { radians: Double -> tan(radians) },
            "acos" to { value: Double -> acos(value) },
            "asin" to { value: Double -> asin(value) },
            "atan" to { value: Double -> atan(value) },
            "atan2" to { y: Double, x: Double -> atan2(y, x) },

            // 定数
            "PI" to PI,
            "E" to E
        )
    }

    /**
     * ランダム関数を構築
     */
    private fun buildRandomFunctions(): Map<String, Any> {
        return mapOf(
            "range" to fun(min: Double, max: Double) = min + (max - min) * Math.random(),
            "int" to fun(min: Int, max: Int) = min + (Math.random() * (max - min + 1)).toInt(),
            "chance" to fun(probability: Double) = Math.random() < probability,
            "boolean" to fun() = Math.random() < 0.5

        )
    }

    /**
     * 距離計算関数を構築
     */
    private fun buildDistanceFunctions(): Map<String, Any> {
        return mapOf(
            "between" to { pos1: Map<*, *>, pos2: Map<*, *> ->
                val x1 = (pos1["x"] as? Number)?.toDouble() ?: 0.0
                val y1 = (pos1["y"] as? Number)?.toDouble() ?: 0.0
                val z1 = (pos1["z"] as? Number)?.toDouble() ?: 0.0
                val x2 = (pos2["x"] as? Number)?.toDouble() ?: 0.0
                val y2 = (pos2["y"] as? Number)?.toDouble() ?: 0.0
                val z2 = (pos2["z"] as? Number)?.toDouble() ?: 0.0

                val dx = x2 - x1
                val dy = y2 - y1
                val dz = z2 - z1
                sqrt(dx * dx + dy * dy + dz * dz)
            },
            "horizontal" to { pos1: Map<*, *>, pos2: Map<*, *> ->
                val x1 = (pos1["x"] as? Number)?.toDouble() ?: 0.0
                val z1 = (pos1["z"] as? Number)?.toDouble() ?: 0.0
                val x2 = (pos2["x"] as? Number)?.toDouble() ?: 0.0
                val z2 = (pos2["z"] as? Number)?.toDouble() ?: 0.0

                val dx = x2 - x1
                val dz = z2 - z1
                sqrt(dx * dx + dz * dz)
            },
            "squared" to { pos1: Map<*, *>, pos2: Map<*, *> ->
                val x1 = (pos1["x"] as? Number)?.toDouble() ?: 0.0
                val y1 = (pos1["y"] as? Number)?.toDouble() ?: 0.0
                val z1 = (pos1["z"] as? Number)?.toDouble() ?: 0.0
                val x2 = (pos2["x"] as? Number)?.toDouble() ?: 0.0
                val y2 = (pos2["y"] as? Number)?.toDouble() ?: 0.0
                val z2 = (pos2["z"] as? Number)?.toDouble() ?: 0.0

                val dx = x2 - x1
                val dy = y2 - y1
                val dz = z2 - z1
                dx * dx + dy * dy + dz * dz
            }
        )
    }

    /**
     * 文字列関数を構築
     */
    private fun buildStringFunctions(): Map<String, Any> {
        return mapOf(
            "contains" to { str: String, substr: String ->
                str.contains(substr)
            },
            "startsWith" to { str: String, prefix: String ->
                str.startsWith(prefix)
            },
            "endsWith" to { str: String, suffix: String ->
                str.endsWith(suffix)
            },
            "toLowerCase" to { str: String ->
                str.lowercase()
            },
            "toUpperCase" to { str: String ->
                str.uppercase()
            },
            "length" to { str: String ->
                str.length
            },
            "substring" to { str: String, start: Int, end: Int ->
                str.substring(start, end)
            },
            "replace" to { str: String, old: String, new: String ->
                str.replace(old, new)
            }
        )
    }

    // ========== 後方互換性メソッド（CELEvaluator用） ==========

    /**
     * エンティティ情報を取得（後方互換）
     */
    fun fromEntity(entity: Entity): Map<String, Any> {
        return buildEntityInfo(entity)
    }

    /**
     * プレイヤー情報を取得（後方互換）
     */
    fun fromPlayer(player: Player): Map<String, Any> {
        return buildEntityInfo(player)
    }

    /**
     * ワールド情報を取得（後方互換）
     */
    fun fromWorld(world: org.bukkit.World): Map<String, Any> {
        return buildWorldInfo(world)
    }

    /**
     * ロケーション情報を取得（後方互換）
     */
    fun fromLocation(location: Location): Map<String, Any> {
        return buildLocationInfo(location)
    }

    /**
     * エンティティペア情報を取得（後方互換）
     */
    fun fromEntityPair(source: Entity, target: Entity): Map<String, Any> {
        return mapOf(
            "source" to buildEntityInfo(source),
            "target" to buildEntityInfo(target),
            "entity" to buildEntityInfo(source)
        )
    }

    /**
     * PacketEntity情報を取得（後方互換）
     */
    fun fromPacketEntity(packetEntity: PacketEntity): Map<String, Any> {
        return buildPacketEntityInfo(packetEntity)
    }

    /**
     * PacketMob情報を取得（後方互換）
     */
    fun fromPacketMob(packetMob: PacketEntity): Map<String, Any> {
        return buildPacketEntityInfo(packetMob)
    }

    /**
     * PacketEntityとEntityのペア情報を取得（後方互換）
     */
    fun fromPacketEntityPair(source: PacketEntity, target: Entity): Map<String, Any> {
        return mapOf(
            "source" to buildPacketEntityInfo(source),
            "target" to buildEntityInfo(target),
            "entity" to buildPacketEntityInfo(source)
        )
    }

    /**
     * PacketEntity同士のペア情報を取得（後方互換）
     */
    fun fromPacketEntityPair(source: PacketEntity, target: PacketEntity): Map<String, Any> {
        return mapOf(
            "source" to buildPacketEntityInfo(source),
            "target" to buildPacketEntityInfo(target),
            "entity" to buildPacketEntityInfo(source)
        )
    }

    /**
     * フルコンテキストを構築（後方互換）
     */
    fun buildFullContext(baseContext: Map<String, Any>): Map<String, Any> {
        val context = baseContext.toMutableMap()

        // 関数群を追加（まだ含まれていない場合）
        if (!context.containsKey("math")) {
            context["math"] = buildMathFunctions()
        }
        if (!context.containsKey("random")) {
            context["random"] = buildRandomFunctions()
        }
        if (!context.containsKey("distance")) {
            context["distance"] = buildDistanceFunctions()
        }
        if (!context.containsKey("string")) {
            context["string"] = buildStringFunctions()
        }

        return context
    }

    /**
     * コンテキストをデバッグ出力（後方互換）
     */
    fun printContext(context: Map<String, Any>) {
        context.forEach { (key, value) ->
            when (value) {
                is Map<*, *> -> {
                    println("$key: <Map with ${value.size} entries>")
                }
                is Function<*> -> {
                    println("$key: <Function>")
                }
                else -> {
                    println("$key: $value")
                }
            }
        }
    }
}