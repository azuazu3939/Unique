package com.github.azuazu3939.unique.mob

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.entity.PacketMob
import com.github.azuazu3939.unique.event.PacketMobSkillEvent
import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Mob管理クラス
 *
 * Mob定義の読み込みとインスタンス生成を管理する。
 * 実際の処理は専用のヘルパークラスに委譲する。
 */
class MobManager(private val plugin: Unique) {

    /**
     * 読み込まれたMob定義
     */
    private val mobDefinitions = ConcurrentHashMap<String, MobDefinition>()

    /**
     * スキルライブラリ（スキル名 -> インライン構文リスト）
     */
    private val skillLibrary = ConcurrentHashMap<String, List<String>>()

    /**
     * アクティブなMobインスタンス
     */
    private val activeMobs = ConcurrentHashMap<String, MobInstance>()

    /**
     * ヘルパークラス
     */
    private val loader = MobLoader(plugin)
    private val spawner = MobSpawner(plugin)
    private val skillExecutor = MobSkillExecutor(plugin)
    private val dropHandler = MobDropHandler(plugin)

    /**
     * 初期化
     */
    fun initialize() {
        DebugLogger.info("Initializing MobManager...")

        skillLibrary.clear()
        skillLibrary.putAll(loader.loadSkillLibrary())

        mobDefinitions.clear()
        mobDefinitions.putAll(loader.loadMobDefinitions())

        DebugLogger.info("MobManager initialized (${mobDefinitions.size} mob types, ${skillLibrary.size} skill definitions loaded)")
    }

    /**
     * スキルライブラリを再読み込み
     */
    fun loadSkillLibrary() {
        skillLibrary.clear()
        skillLibrary.putAll(loader.loadSkillLibrary())
        DebugLogger.info("Reloaded ${skillLibrary.size} skill definitions")
    }

    /**
     * Mob定義を再読み込み
     */
    fun loadMobDefinitions() {
        mobDefinitions.clear()
        mobDefinitions.putAll(loader.loadMobDefinitions())
        DebugLogger.info("Reloaded ${mobDefinitions.size} mob definitions")
    }

    /**
     * 全てのMob関連データを再読み込み
     */
    fun reload() {
        loadSkillLibrary()
        loadMobDefinitions()
        DebugLogger.info("MobManager reloaded (${mobDefinitions.size} mobs, ${skillLibrary.size} skills)")
    }

    /**
     * Mob定義を取得
     */
    fun getMobDefinition(name: String): MobDefinition? {
        return mobDefinitions[name]
    }

    /**
     * すべてのMob定義を取得
     */
    fun getAllMobDefinitions(): Map<String, MobDefinition> {
        return mobDefinitions.toMap()
    }

    /**
     * Mobをスポーン
     */
    suspend fun spawnMob(mobName: String, location: Location): PacketMob? {
        return spawner.spawnMob(mobName, location, mobDefinitions, activeMobs, skillExecutor, skillLibrary)
    }

    /**
     * Mob被ダメージ処理
     */
    suspend fun handleMobDamaged(mob: PacketMob, damager: Entity, damage: Double) {
        spawner.handleMobDamaged(mob, damager, damage, activeMobs, skillExecutor, skillLibrary)
    }

    /**
     * スキルトリガーを実行
     */
    fun executeSkillTriggers(
        mob: PacketMob,
        triggers: List<SkillTrigger>,
        triggerType: PacketMobSkillEvent.SkillTriggerType
    ) {
        skillExecutor.executeSkillTriggers(mob, triggers, triggerType, skillLibrary)
    }

    /**
     * ドロップ処理（計算 + ワールドにドロップ）
     */
    suspend fun processDrops(
        definition: MobDefinition,
        location: Location,
        killer: Player,
        eventDrops: MutableList<ItemStack>
    ) {
        dropHandler.processDrops(definition, location, killer, eventDrops)
    }

    /**
     * ドロップアイテムを計算（リストを返すのみ）
     */
    fun calculateDropItems(
        definition: MobDefinition,
        killer: Player,
        location: Location
    ): List<ItemStack> {
        return dropHandler.calculateDropItems(definition, killer, location)
    }

    /**
     * UUIDからMobInstanceを取得
     */
    fun getMobInstance(uuid: UUID): MobInstance? {
        return activeMobs[uuid.toString()]
    }

    /**
     * EntityIdからMobInstanceを取得
     */
    fun getMobInstance(entityId: Int): MobInstance? {
        val entity = plugin.packetEntityManager.getEntityByEntityId(entityId) as? PacketMob
            ?: return null
        return activeMobs[entity.uuid.toString()]
    }

    /**
     * PacketMobからMobInstanceを取得
     */
    fun getMobInstance(mob: PacketMob): MobInstance? {
        return activeMobs[mob.uuid.toString()]
    }

    /**
     * MobInstanceを削除
     */
    fun removeMobInstance(uuid: UUID) {
        activeMobs.remove(uuid.toString())?.let {
            DebugLogger.debug("Removed MobInstance: ${it.definitionName}")
        }
    }

    /**
     * デバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("MobManager Debug Info")
        DebugLogger.info("Loaded mob definitions: ${mobDefinitions.size}")
        DebugLogger.info("Active mob instances: ${activeMobs.size}")

        mobDefinitions.forEach { (name, def) ->
            DebugLogger.info("  $name: ${def.type} (HP: ${def.health})")
        }

        DebugLogger.separator()
    }
}

/**
 * Mobインスタンス
 */
data class MobInstance(
    val definitionName: String,
    val entity: PacketMob,
    val definition: MobDefinition
)
