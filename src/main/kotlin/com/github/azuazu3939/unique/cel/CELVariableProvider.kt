package com.github.azuazu3939.unique.cel

import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.util.maxHealth
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import kotlin.math.*

/**
 * CEL変数提供クラス
 *
 * YAML設定で使用できる変数をMapとして提供
 */
object CELVariableProvider {

    /**
     * エンティティ情報を変数Mapに変換
     */
    fun fromEntity(entity: Entity): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        // 基本情報
        variables["type"] = entity.type.name
        variables["uuid"] = entity.uniqueId.toString()
        variables["isDead"] = entity.isDead
        variables["age"] = entity.ticksLived

        // 生存エンティティの場合
        if (entity is LivingEntity) {
            variables["health"] = entity.health
            variables["maxHealth"] = entity.maxHealth()
            variables["isGlowing"] = entity.isGlowing
            variables["isInvisible"] = entity.isInvisible
            variables["hasAI"] = entity.hasAI()
        }

        // 座標情報
        variables["location"] = fromLocation(entity.location)

        return variables
    }

    /**
     * PacketEntity情報を変数Mapに変換
     */
    fun fromPacketEntity(packetEntity: PacketEntity): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        // 基本情報
        variables["type"] = packetEntity.entityType.name
        variables["uuid"] = packetEntity.uuid.toString()
        variables["entityId"] = packetEntity.entityId
        variables["isDead"] = packetEntity.isDead
        variables["age"] = packetEntity.ticksLived

        // 体力情報
        variables["health"] = packetEntity.health
        variables["maxHealth"] = packetEntity.maxHealth

        // 座標情報
        variables["location"] = fromLocation(packetEntity.location)

        return variables
    }

    /**
     * PacketMob情報を変数Mapに変換
     */
    fun fromPacketMob(packetMob: PacketMob): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        // PacketEntity情報を継承
        variables.putAll(fromPacketEntity(packetMob))

        // PacketMob固有情報
        variables["mobName"] = packetMob.mobName
        variables["customName"] = packetMob.customName
        variables["customNameVisible"] = packetMob.customNameVisible
        variables["hasAI"] = packetMob.hasAI
        variables["hasGravity"] = packetMob.hasGravity
        variables["isGlowing"] = packetMob.isGlowing
        variables["isInvisible"] = packetMob.isInvisible

        return variables
    }

    /**
     * プレイヤー情報を変数Mapに変換
     */
    fun fromPlayer(player: Player): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        // エンティティ情報を継承
        variables.putAll(fromEntity(player))

        // プレイヤー固有情報
        variables["name"] = player.name
        variables["displayName"] = player.displayName()
        variables["level"] = player.level
        variables["exp"] = player.exp
        variables["totalExperience"] = player.totalExperience
        variables["foodLevel"] = player.foodLevel
        variables["saturation"] = player.saturation
        variables["gameMode"] = player.gameMode.name
        variables["isFlying"] = player.isFlying
        variables["isSneaking"] = player.isSneaking
        variables["isSprinting"] = player.isSprinting
        variables["isBlocking"] = player.isBlocking
        variables["isSwimming"] = player.isSwimming
        variables["isGliding"] = player.isGliding
        variables["isOnGround"] = player.velocity.y == 0.0 // player.isOnGround は非推奨メソッドです。 これでサーバーの状態依存で判別可能。

        // インベントリ
        variables["inventorySize"] = player.inventory.size
        variables["heldItemSlot"] = player.inventory.heldItemSlot

        return variables
    }

    /**
     * 座標情報を変数Mapに変換
     */
    fun fromLocation(location: Location): Map<String, Any> {
        return mapOf(
            "x" to location.x,
            "y" to location.y,
            "z" to location.z,
            "blockX" to location.blockX,
            "blockY" to location.blockY,
            "blockZ" to location.blockZ,
            "world" to (location.world?.name ?: "unknown"),
            "yaw" to location.yaw,
            "pitch" to location.pitch
        )
    }

    /**
     * ワールド情報を変数Mapに変換
     */
    fun fromWorld(world: World): Map<String, Any> {
        return mapOf(
            "name" to world.name,
            "time" to world.time,
            "fullTime" to world.fullTime,
            "isDay" to (world.time < 13000),
            "isNight" to (world.time >= 13000),
            "hasStorm" to world.hasStorm(),
            "isThundering" to world.isThundering,
            "difficulty" to world.difficulty.name,
            "playerCount" to world.playerCount,
            "entityCount" to world.entityCount,
            "seaLevel" to world.seaLevel,
            "maxHeight" to world.maxHeight,
            "minHeight" to world.minHeight
        )
    }

    /**
     * ダメージイベント情報を変数Mapに変換
     */
    fun fromDamageEvent(event: EntityDamageEvent): Map<String, Any> {
        return mapOf(
            "amount" to event.damage,
            "finalAmount" to event.finalDamage,
            "cause" to event.cause.name,
            "isCancelled" to event.isCancelled
        )
    }

    /**
     * アイテム情報を変数Mapに変換
     */
    fun fromItemStack(item: ItemStack): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        variables["type"] = item.type.name
        variables["amount"] = item.amount
        variables["maxStackSize"] = item.maxStackSize
        variables["hasEnchantments"] = item.enchantments.isNotEmpty()

        // 耐久値
        val meta = item.itemMeta
        if (meta != null) {
            if (item.type.maxDurability > 0 && meta is Damageable) {
                variables["durability"] =  item.type.maxDurability -  meta.damage
                variables["maxDurability"] = item.type.maxDurability
            }

            // 表示名とLore
            variables["hasDisplayName"] = meta.hasDisplayName()
            if (meta.hasDisplayName()) {
                variables["displayName"] = item.displayName()
            }
            if (meta.hasLore()) {
                variables["lore"] = item.lore() ?: emptyList<Component>()
            }
        }

        return variables
    }

    /**
     * Math関数を提供
     */
    fun getMathFunctions(): Map<String, Function<Double>> {
        return mapOf(
            "random" to fun() = Math.random(),
            "abs" to fun(x: Double) = abs(x) ,
            "min" to fun(a: Double, b: Double) = min(a, b),
            "max" to fun(a: Double, b: Double) = max(a, b),
            "floor" to fun(x: Double) = floor(x),
            "ceil" to fun(x: Double) = ceil(x),
            "round" to fun(x: Double) = round(x),
            "sqrt" to fun(x: Double) = sqrt(x),
            "pow" to fun(x: Double, y: Double) = x.pow(y),
            "sin" to fun(x: Double) = sin(x),
            "cos" to fun(x: Double) = cos(x),
            "tan" to fun(x: Double) = tan(x)
        )
    }

    /**
     * String関数を提供
     */
    fun getStringFunctions(): Map<String, Any> {
        return mapOf(
            "contains" to { s: String, sub: String -> s.contains(sub) },
            "startsWith" to { s: String, prefix: String -> s.startsWith(prefix) },
            "endsWith" to { s: String, suffix: String -> s.endsWith(suffix) },
            "toLowerCase" to { s: String -> s.lowercase() },
            "toUpperCase" to { s: String -> s.uppercase() },
            "length" to { s: String -> s.length },
            "isEmpty" to { s: String -> s.isEmpty() },
            "isBlank" to { s: String -> s.isBlank() }
        )
    }

    /**
     * 2つのエンティティ間の情報を提供（ターゲット用）
     */
    fun fromEntityPair(source: Entity, target: Entity): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        // ソースエンティティ
        variables["entity"] = fromEntity(source)

        // ターゲット情報
        variables["target"] = if (target is Player) {
            fromPlayer(target)
        } else {
            fromEntity(target)
        }

        // 距離計算
        variables["distance"] = source.location.distance(target.location)
        variables["distance2D"] = calculateDistance2D(source.location, target.location)
        variables["distanceSquared"] = source.location.distanceSquared(target.location)

        // 相対位置
        val dx = target.location.x - source.location.x
        val dy = target.location.y - source.location.y
        val dz = target.location.z - source.location.z
        variables["dx"] = dx
        variables["dy"] = dy
        variables["dz"] = dz

        // 角度
        variables["angle"] = calculateAngle(source.location, target.location)

        return variables
    }

    /**
     * PacketEntityとEntityのペア情報を提供（ターゲット用）
     */
    fun fromPacketEntityPair(source: PacketEntity, target: Entity): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        // ソースPacketEntity
        variables["entity"] = if (source is PacketMob) {
            fromPacketMob(source)
        } else {
            fromPacketEntity(source)
        }

        // ターゲット情報
        variables["target"] = if (target is Player) {
            fromPlayer(target)
        } else {
            fromEntity(target)
        }

        // 距離計算
        variables["distance"] = source.location.distance(target.location)
        variables["distance2D"] = calculateDistance2D(source.location, target.location)
        variables["distanceSquared"] = source.location.distanceSquared(target.location)

        // 相対位置
        val dx = target.location.x - source.location.x
        val dy = target.location.y - source.location.y
        val dz = target.location.z - source.location.z
        variables["dx"] = dx
        variables["dy"] = dy
        variables["dz"] = dz

        // 角度
        variables["angle"] = calculateAngle(source.location, target.location)

        return variables
    }

    /**
     * PacketEntity同士のペア情報を提供
     */
    fun fromPacketEntityPair(source: PacketEntity, target: PacketEntity): Map<String, Any> {
        val variables = mutableMapOf<String, Any>()

        // ソースPacketEntity
        variables["entity"] = if (source is PacketMob) {
            fromPacketMob(source)
        } else {
            fromPacketEntity(source)
        }

        // ターゲットPacketEntity
        variables["target"] = if (target is PacketMob) {
            fromPacketMob(target)
        } else {
            fromPacketEntity(target)
        }

        // 距離計算
        variables["distance"] = source.location.distance(target.location)
        variables["distance2D"] = calculateDistance2D(source.location, target.location)
        variables["distanceSquared"] = source.location.distanceSquared(target.location)

        // 相対位置
        val dx = target.location.x - source.location.x
        val dy = target.location.y - source.location.y
        val dz = target.location.z - source.location.z
        variables["dx"] = dx
        variables["dy"] = dy
        variables["dz"] = dz

        // 角度
        variables["angle"] = calculateAngle(source.location, target.location)

        return variables
    }

    /**
     * 完全なコンテキストを構築
     *
     * @param baseContext 基本コンテキスト
     * @return CEL用の完全な変数Map
     */
    fun buildFullContext(baseContext: Map<String, Any>): Map<String, Any> {
        val fullContext = mutableMapOf<String, Any>()

        // ベースコンテキストをコピー
        fullContext.putAll(baseContext)

        // Math関数を追加
        fullContext["math"] = getMathFunctions()

        // String関数を追加
        fullContext["string"] = getStringFunctions()

        return fullContext
    }

    /**
     * 2D距離計算（Y軸無視）
     */
    private fun calculateDistance2D(loc1: Location, loc2: Location): Double {
        val dx = loc1.x - loc2.x
        val dz = loc1.z - loc2.z
        return sqrt(dx * dx + dz * dz)
    }

    /**
     * 2点間の角度計算
     */
    private fun calculateAngle(from: Location, to: Location): Double {
        val dx = to.x - from.x
        val dz = to.z - from.z
        return Math.toDegrees(atan2(dz, dx))
    }

    /**
     * デバッグ用: コンテキスト内容を表示
     */
    fun printContext(context: Map<String, Any>, indent: Int = 0) {
        val prefix = "  ".repeat(indent)
        context.forEach { (key, value) ->
            when (value) {
                is Map<*, *> -> {
                    println("$prefix$key:")
                    @Suppress("UNCHECKED_CAST")
                    printContext(value as Map<String, Any>, indent + 1)
                }
                else -> println("$prefix$key: $value")
            }
        }
    }
}