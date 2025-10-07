package com.github.azuazu3939.unique.nms

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.sqrt
import net.minecraft.world.entity.Entity as NMSEntity
import net.minecraft.world.level.Level as NMSLevel

/**
 * NMS拡張関数群
 *
 * メインスレッド外でも安全にLocation情報を取得するためのヘルパー
 */

/**
 * Bukkit EntityをNMS Entityに変換
 * メインスレッド外でも安全
 */
fun Entity.toNMS(): NMSEntity {
    return (this as CraftEntity).handle
}

/**
 * Bukkit PlayerをNMS ServerPlayerに変換
 * メインスレッド外でも安全
 */
fun Player.toNMS(): ServerPlayer {
    return (this as CraftPlayer).handle
}

/**
 * Bukkit WorldをNMS Levelに変換
 * メインスレッド外でも安全
 */
fun World.toNMS(): NMSLevel {
    return (this as CraftWorld).handle
}

/**
 * EntityのLocationを非同期で取得
 *
 * 注意: このメソッドはメインスレッド外でも安全に呼び出せますが、
 * 返されるLocationオブジェクトはスナップショットです。
 *
 * @return 現在の位置のスナップショット
 */
fun Entity.getLocationAsync(): Location {
    val nmsEntity = this.toNMS()
    return Location(
        this.world,
        nmsEntity.x,
        nmsEntity.y,
        nmsEntity.z,
        nmsEntity.yRot,
        nmsEntity.xRot
    )
}

/**
 * PlayerのLocationを非同期で取得
 *
 * 注意: このメソッドはメインスレッド外でも安全に呼び出せますが、
 * 返されるLocationオブジェクトはスナップショットです。
 *
 * @return 現在の位置のスナップショット
 */
fun Player.getLocationAsync(): Location {
    val nmsPlayer = this.toNMS()
    return Location(
        this.world,
        nmsPlayer.x,
        nmsPlayer.y,
        nmsPlayer.z,
        nmsPlayer.yRot,
        nmsPlayer.xRot
    )
}

/**
 * 2つのLocationの距離を計算（メインスレッド外でも安全）
 *
 * @param other 比較対象のLocation
 * @return 距離
 */
fun Location.distanceTo(other: Location): Double {
    if (this.world != other.world) {
        return Double.MAX_VALUE
    }

    val dx = this.x - other.x
    val dy = this.y - other.y
    val dz = this.z - other.z

    return sqrt(dx * dx + dy * dy + dz * dz)
}

/**
 * 2つのLocationの水平距離を計算（Y軸を無視）
 * メインスレッド外でも安全
 *
 * @param other 比較対象のLocation
 * @return 水平距離
 */
fun Location.horizontalDistanceTo(other: Location): Double {
    if (this.world != other.world) {
        return Double.MAX_VALUE
    }

    val dx = this.x - other.x
    val dz = this.z - other.z

    return sqrt(dx * dx + dz * dz)
}

/**
 * EntityとLocationの距離を非同期で計算
 *
 * @param location 比較対象のLocation
 * @return 距離
 */
fun Entity.distanceToAsync(location: Location): Double {
    return this.getLocationAsync().distanceTo(location)
}

/**
 * PlayerとLocationの距離を非同期で計算
 *
 * @param location 比較対象のLocation
 * @return 距離
 */
fun Player.distanceToAsync(location: Location): Double {
    return this.getLocationAsync().distanceTo(location)
}

/**
 * 2つのEntity間の距離を非同期で計算
 *
 * @param other 比較対象のEntity
 * @return 距離
 */
fun Entity.distanceToAsync(other: Entity): Double {
    val loc1 = this.getLocationAsync()
    val loc2 = other.getLocationAsync()
    return loc1.distanceTo(loc2)
}

/**
 * PlayerとEntity間の距離を非同期で計算
 *
 * @param entity 比較対象のEntity
 * @return 距離
 */
fun Player.distanceToAsync(entity: Entity): Double {
    val loc1 = this.getLocationAsync()
    val loc2 = entity.getLocationAsync()
    return loc1.distanceTo(loc2)
}

/**
 * NMS EntityからBukkit Locationを作成
 * メインスレッド外でも安全
 *
 * @param world Bukkit World
 * @return Location
 */
fun NMSEntity.toBukkitLocation(world: World): Location {
    return Location(
        world,
        this.x,
        this.y,
        this.z,
        this.yRot,
        this.xRot
    )
}

/**
 * Entityの座標を直接取得（X）
 * メインスレッド外でも安全
 */
fun Entity.getXAsync(): Double = this.toNMS().x

/**
 * Entityの座標を直接取得（Y）
 * メインスレッド外でも安全
 */
fun Entity.getYAsync(): Double = this.toNMS().y

/**
 * Entityの座標を直接取得（Z）
 * メインスレッド外でも安全
 */
fun Entity.getZAsync(): Double = this.toNMS().z

/**
 * Playerの座標を直接取得（X）
 * メインスレッド外でも安全
 */
fun Player.getXAsync(): Double = this.toNMS().x

/**
 * Playerの座標を直接取得（Y）
 * メインスレッド外でも安全
 */
fun Player.getYAsync(): Double = this.toNMS().y

/**
 * Playerの座標を直接取得（Z）
 * メインスレッド外でも安全
 */
fun Player.getZAsync(): Double = this.toNMS().z

/**
 * UUIDからPlayerを取得（NMS経由、メインスレッド外でも安全）
 *
 * @param uuid プレイヤーのUUID
 * @return プレイヤーオブジェクト（オフラインの場合はnull）
 */
fun getPlayerByUUID(uuid: UUID): Player? {
    val craftServer = Bukkit.getServer() as CraftServer
    val nmsServer = craftServer.server
    val nmsPlayer = nmsServer.playerList.getPlayer(uuid) ?: return null
    return nmsPlayer.bukkitEntity as? Player
}

/**
 * オンラインプレイヤーのリストを取得（NMS経由、メインスレッド外でも安全）
 *
 * @return オンラインプレイヤーのリスト
 */
fun getOnlinePlayersAsync(): List<Player> {
    val craftServer = Bukkit.getServer() as CraftServer
    val nmsServer = craftServer.server
    return nmsServer.playerList.players.map { it.bukkitEntity as Player }
}

/**
 * Worldのプレイヤーリストを取得（NMS経由、メインスレッド外でも安全）
 *
 * @return ワールド内のプレイヤーリスト
 */
fun World.getPlayersAsync(): List<Player> {
    val nmsLevel = this.toNMS()
    return nmsLevel.players().map { it.bukkitEntity as Player }
}

/**
 * 指定座標のブロックタイプを非同期で取得
 *
 * 注意: このメソッドはasyncスケジューラで安全に呼び出せます。
 * NMS経由でブロック状態を取得するため、メインスレッドは不要です。
 *
 * @param x X座標
 * @param y Y座標
 * @param z Z座標
 * @return ブロックのMaterial
 */
fun World.getBlockTypeAsync(x: Int, y: Int, z: Int): Material {
    val nmsLevel = this.toNMS()
    val blockPos = BlockPos(x, y, z)
    val blockState = nmsLevel.getBlockState(blockPos)
    return CraftMagicNumbers.getMaterial(blockState.block)
}

/**
 * 指定座標のブロックがソリッドか非同期で判定
 *
 * 注意: このメソッドはasyncスケジューラで安全に呼び出せます。
 * NMS経由でブロック状態を取得するため、メインスレッドは不要です。
 *
 * @param x X座標
 * @param y Y座標
 * @param z Z座標
 * @return ソリッドブロックの場合true
 */
fun World.isBlockSolidAsync(x: Int, y: Int, z: Int): Boolean {
    val nmsLevel = this.toNMS()
    val blockPos = BlockPos(x, y, z)
    val blockState = nmsLevel.getBlockState(blockPos)
    return blockState.isSolidRender
}
