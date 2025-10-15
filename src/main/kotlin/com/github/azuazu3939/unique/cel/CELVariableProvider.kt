package com.github.azuazu3939.unique.cel

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.nms.getNearbyEntitiesAsync
import com.github.azuazu3939.unique.util.biomeName
import com.github.azuazu3939.unique.util.canHoldVariables
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
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

        // 型変換関数（cast形式 - 互換性のため）
        context["cast"] = buildCastFunctions()

        // グローバル型変換関数（推奨）
        context.putAll(buildGlobalCastFunctions())

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
            val nearbyPlayers = world.getNearbyEntitiesAsync(
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
        context["cast"] = buildCastFunctions()

        // グローバル型変換関数（推奨）
        context.putAll(buildGlobalCastFunctions())

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
     * PacketEntityとEntityのターゲットコンテキストを構築
     */
    fun buildPacketEntityTargetContext(source: PacketEntity, target: Entity): Map<String, Any> {
        val context = buildPacketEntityContext(source).toMutableMap()

        // ターゲット情報を追加
        context["target"] = buildEntityInfo(target)

        // ソース情報を明示的に追加
        context["source"] = context["entity"]!!

        return context
    }

    /**
     * PacketEntity同士のペアコンテキストを構築
     */
    fun buildPacketEntityPairContext(source: PacketEntity, target: PacketEntity): Map<String, Any> {
        val context = buildPacketEntityContext(source).toMutableMap()

        // ターゲット情報を追加
        context["target"] = buildPacketEntityInfo(target)

        // ソース情報を明示的に追加
        context["source"] = context["entity"]!!

        return context
    }

    /**
     * エンティティ情報を構築
     */
    internal fun buildEntityInfo(entity: Entity): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        info["type"] = entity.type.name
        info["uuid"] = entity.uniqueId.toString()
        info["isDead"] = entity.isDead
        info["age"] = entity.ticksLived

        // 変数格納可能性（PlayerとPacketMobのみ可能）
        info["canHoldVariables"] = entity.canHoldVariables()

        // ダメージを受けられるか（LivingEntityのみ）
        info["canTakeDamage"] = entity is LivingEntity

        // 位置情報
        info["location"] = buildLocationInfo(entity.location)

        // LivingEntity固有の情報
        if (entity is LivingEntity) {
            info["health"] = entity.health
            info["maxHealth"] = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0

            // アトリビュート情報（直接アクセス用）
            info["armor"] = entity.getAttribute(Attribute.ARMOR)?.value ?: 0.0
            info["armorToughness"] = entity.getAttribute(Attribute.ARMOR_TOUGHNESS)?.value ?: 0.0
            info["attackDamage"] = entity.getAttribute(Attribute.ATTACK_DAMAGE)?.value ?: 1.0
            info["attackSpeed"] = entity.getAttribute(Attribute.ATTACK_SPEED)?.value ?: 4.0
            info["knockbackResistance"] = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.value ?: 0.0
            info["movementSpeed"] = entity.getAttribute(Attribute.MOVEMENT_SPEED)?.value ?: 0.7

            // 全アトリビュートへのアクセス
            info["attributes"] = buildAttributesMap(entity)
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
    internal fun buildPacketEntityInfo(packetEntity: PacketEntity): Map<String, Any> {
        val info = mutableMapOf(
            "entityId" to packetEntity.entityId,
            "location" to buildLocationInfo(packetEntity.location),
            "canHoldVariables" to packetEntity.canHoldVariables(),
            "canTakeDamage" to packetEntity.canTakeDamage()
        )

        // PacketMobの場合は追加情報を含める
        if (packetEntity is PacketMob) {
            info["health"] = packetEntity.health
            info["maxHealth"] = packetEntity.maxHealth
            info["armor"] = packetEntity.armor
            info["armorToughness"] = packetEntity.armorToughness
            info["attackDamage"] = packetEntity.damage
            info["movementSpeed"] = packetEntity.movementSpeed
            info["followRange"] = packetEntity.followRange
            info["attackRange"] = packetEntity.attackRange

            // PacketMob用のアトリビュートマップ
            info["attributes"] = buildPacketMobAttributesMap(packetEntity)
        }

        return info
    }

    /**
     * PacketMobのアトリビュートマップを構築
     */
    private fun buildPacketMobAttributesMap(mob: PacketMob): Map<String, Double> {
        return mapOf(
            "armor" to mob.armor,
            "armor_toughness" to mob.armorToughness,
            "attack_damage" to mob.damage,
            "movement_speed" to mob.movementSpeed,
            "follow_range" to mob.followRange,
            "attack_range" to mob.attackRange,
            "max_health" to mob.maxHealth
        )
    }

    /**
     * 位置情報を構築
     */
    internal fun buildLocationInfo(location: Location): Map<String, Any> {
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
    internal fun buildWorldInfo(world: org.bukkit.World): Map<String, Any> {
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
        return entity.world.getNearbyEntitiesAsync(
            entity.location, 20.0, 20.0, 20.0
        ).filterIsInstance<Player>()
    }

    /**
     * 周囲のMobを取得
     */
    private fun getNearbyMobs(entity: Entity): List<LivingEntity> {
        return entity.world.getNearbyEntitiesAsync(
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

    /**
     * 型変換関数を構築
     */
    private fun buildCastFunctions(): Map<String, Any> {
        return mapOf(
            // 数値変換
            "toInt" to { value: Any ->
                when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    is Boolean -> if (value) 1 else 0
                    else -> 0
                }
            },
            "toLong" to { value: Any ->
                when (value) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    is Boolean -> if (value) 1L else 0L
                    else -> 0L
                }
            },
            "toDouble" to { value: Any ->
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    is Boolean -> if (value) 1.0 else 0.0
                    else -> 0.0
                }
            },
            "toFloat" to { value: Any ->
                when (value) {
                    is Number -> value.toFloat()
                    is String -> value.toFloatOrNull() ?: 0.0f
                    is Boolean -> if (value) 1.0f else 0.0f
                    else -> 0.0f
                }
            },

            // 真偽値変換
            "toBoolean" to { value: Any ->
                when (value) {
                    is Boolean -> value
                    is Number -> value.toDouble() != 0.0
                    is String -> value.toBoolean()
                    else -> false
                }
            },

            // 文字列変換
            "toString" to { value: Any -> value.toString() },

            // 安全な変換（失敗時にデフォルト値を返す）
            "toIntOr" to { value: Any, default: Int ->
                when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: default
                    else -> default
                }
            },
            "toDoubleOr" to { value: Any, default: Double ->
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: default
                    else -> default
                }
            },

            // 範囲制限付き変換
            "toIntClamped" to { value: Any, min: Int, max: Int ->
                val intValue = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: min
                    else -> min
                }
                intValue.coerceIn(min, max)
            },
            "toDoubleClamped" to { value: Any, min: Double, max: Double ->
                val doubleValue = when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: min
                    else -> min
                }
                doubleValue.coerceIn(min, max)
            }
        )
    }

    /**
     * グローバル型変換関数を構築（推奨形式）
     * cast.toInt(value) ではなく toInt(value) で使用可能
     */
    private fun buildGlobalCastFunctions(): Map<String, Any> {
        return mapOf(
            // 数値変換
            "toInt" to { value: Any ->
                when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    is Boolean -> if (value) 1 else 0
                    else -> 0
                }
            },
            "toLong" to { value: Any ->
                when (value) {
                    is Number -> value.toLong()
                    is String -> value.toLongOrNull() ?: 0L
                    is Boolean -> if (value) 1L else 0L
                    else -> 0L
                }
            },
            "toDouble" to { value: Any ->
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    is Boolean -> if (value) 1.0 else 0.0
                    else -> 0.0
                }
            },
            "toFloat" to { value: Any ->
                when (value) {
                    is Number -> value.toFloat()
                    is String -> value.toFloatOrNull() ?: 0.0f
                    is Boolean -> if (value) 1.0f else 0.0f
                    else -> 0.0f
                }
            },

            // 真偽値変換
            "toBoolean" to { value: Any ->
                when (value) {
                    is Boolean -> value
                    is Number -> value.toDouble() != 0.0
                    is String -> value.toBoolean()
                    else -> false
                }
            },

            // 文字列変換
            "toString" to { value: Any ->
                value.toString()
            },

            // 安全な変換（デフォルト値付き）
            "toIntOr" to { value: Any, default: Int ->
                when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: default
                    else -> default
                }
            },
            "toDoubleOr" to { value: Any, default: Double ->
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: default
                    else -> default
                }
            },

            // 範囲制限付き変換
            "toIntClamped" to { value: Any, min: Int, max: Int ->
                val intValue = when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: min
                    else -> min
                }
                intValue.coerceIn(min, max)
            },
            "toDoubleClamped" to { value: Any, min: Double, max: Double ->
                val doubleValue = when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: min
                    else -> min
                }
                doubleValue.coerceIn(min, max)
            },

            // コレクション変換
            "toList" to { value: Any ->
                when (value) {
                    is List<*> -> value
                    is String -> {
                        // "[]" または "[1, 2, 3]" 形式をパース
                        if (value.startsWith("[") && value.endsWith("]")) {
                            val content = value.substring(1, value.length - 1).trim()
                            if (content.isEmpty()) {
                                emptyList<Any>()
                            } else {
                                content.split(",").map { it.trim() }
                            }
                        } else {
                            // カンマ区切り文字列
                            value.split(",").map { it.trim() }
                        }
                    }
                    is Array<*> -> value.toList()
                    else -> listOf(value)
                }
            },
            "toMap" to { value: Any ->
                when (value) {
                    is Map<*, *> -> value
                    is String -> {
                        // "{}" または "{key: value, ...}" 形式をパース
                        if (value.startsWith("{") && value.endsWith("}")) {
                            val content = value.substring(1, value.length - 1).trim()
                            if (content.isEmpty()) {
                                emptyMap<String, Any>()
                            } else {
                                // 簡易的なマップパーサー
                                content.split(",").associate {
                                    val parts = it.split(":")
                                    if (parts.size == 2) {
                                        parts[0].trim() to parts[1].trim()
                                    } else {
                                        it.trim() to ""
                                    }
                                }
                            }
                        } else {
                            mapOf("value" to value)
                        }
                    }
                    else -> mapOf("value" to value)
                }
            }
        )
    }

    /**
     * 全アトリビュートのマップを構築
     */
    private fun buildAttributesMap(entity: LivingEntity): Map<String, Double> {
        val attributes = mutableMapOf<String, Double>()

        // Paperレジストリから全てのAttributeを取得
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)

        registry.forEach { attribute ->
            entity.getAttribute(attribute)?.let { instance ->
                // レジストリキーから名前を取得し、"generic."または"generic_"を除去して小文字に変換
                val key = registry.getKey(attribute)
                val fullName = key?.value()?.lowercase() ?: attribute.key.value()

                // "generic." または "generic_" を除去
                val name = fullName.removePrefix("generic.").removePrefix("generic_")

                // . 形式で追加
                attributes[name] = instance.value

                // _ 形式でも追加（プラグイン間の互換性のため）
                if (name.contains(".")) {
                    attributes[name.replace(".", "_")] = instance.value
                } else if (name.contains("_")) {
                    attributes[name.replace("_", ".")] = instance.value
                }
            }
        }

        return attributes
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
        if (!context.containsKey("cast")) {
            context["cast"] = buildCastFunctions()
        }

        // グローバル型変換関数を追加
        context.putAll(buildGlobalCastFunctions())

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