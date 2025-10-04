package com.github.azuazu3939.unique

import com.github.azuazu3939.unique.cel.CELEngineManager
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.command.UniqueCommand
import com.github.azuazu3939.unique.config.ConfigManager
import com.github.azuazu3939.unique.entity.PacketEntityManager
import com.github.azuazu3939.unique.listener.MobListener
import com.github.azuazu3939.unique.mob.MobManager
import com.github.azuazu3939.unique.skill.SkillExecutor
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.retrooper.packetevents.PacketEvents
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import java.io.File

/**
 * Unique - AI-friendly Mob/Item creation plugin
 *
 * メインプラグインクラス
 * MCコルーチン対応のSuspendingJavaPluginを継承
 */
class Unique : SuspendingJavaPlugin() {

    companion object {
        /**
         * プラグインインスタンス
         * 他のクラスから簡単にアクセス可能
         */
        lateinit var instance: Unique
            private set
    }

    // データフォルダ内のサブディレクトリ
    lateinit var mobsFolder: File
        private set
    lateinit var spawnsFolder: File
        private set
    lateinit var skillsFolder: File
        private set
    lateinit var conditionsFolder: File
        private set
    lateinit var targetersFolder: File
        private set
    lateinit var effectsFolder: File
        private set
    lateinit var sampleFolder: File
        private set

    // CELエンジン
    lateinit var celEngine: CELEngineManager
        private set
    lateinit var celEvaluator: CELEvaluator
        private set

    // 設定マネージャー
    lateinit var configManager: ConfigManager
        private set

    // パケットエンティティマネージャー
    lateinit var packetEntityManager: PacketEntityManager
        private set

    // スキルエグゼキューター
    lateinit var skillExecutor: SkillExecutor
        private set

    // Mobマネージャー
    lateinit var mobManager: MobManager
        private set

    // スポーンマネージャー
    lateinit var spawnManager: com.github.azuazu3939.unique.spawn.SpawnManager
        private set

    /**
     * プラグイン読み込み（非同期）
     */

    @Suppress("UnstableApiUsage")
    override suspend fun onLoadAsync() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.checkForUpdates(false)
        PacketEvents.getAPI().load()

        DebugLogger.info("PacketEvents loaded in onLoadAsync()")
    }

    /**
     * プラグイン有効化（非同期）
     */

    override suspend fun onEnableAsync() {
        instance = this
        saveDefaultConfig()

        // PacketEvents初期化
        PacketEvents.getAPI().init()

        // ロゴ表示
        displayLogo()

        // フォルダ初期化
        initializeFolders()

        // 設定読み込み
        initializeConfig()

        // CELエンジン初期化
        initializeCEL()

        // パケットエンティティマネージャー初期化
        initializePacketEntityManager()

        // リスナー登録
        server.pluginManager.registerEvents(MobListener(this), this)

        // コマンド登録
        getCommand("unique")?.setExecutor(UniqueCommand(this))

        DebugLogger.info("Unique plugin enabled successfully!")
        DebugLogger.info("Version: ${pluginMeta.version}")
        DebugLogger.info("API Version: ${pluginMeta.apiVersion}")
    }

    /**
     * プラグイン無効化（非同期）
     */
    override suspend fun onDisableAsync() {
        // パケットエンティティマネージャーをシャットダウン
        if (::packetEntityManager.isInitialized) {
            packetEntityManager.shutdown()
        }

        // CEL統計情報を表示
        if (::celEngine.isInitialized) {
            celEngine.printDebugInfo()
        }

        DebugLogger.info("Unique plugin disabled.")
    }

    /**
     * フォルダ構造を初期化
     */
    private fun initializeFolders() {
        // メインデータフォルダ
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
            DebugLogger.info("Created data folder: ${dataFolder.path}")
        }

        // サブフォルダ作成
        mobsFolder = createFolder("mobs")
        spawnsFolder = createFolder("spawns")
        skillsFolder = createFolder("skills")
        conditionsFolder = createFolder("conditions")
        targetersFolder = createFolder("targeters")
        effectsFolder = createFolder("effects")
        sampleFolder = createFolder("sample")

        DebugLogger.info("All folders initialized successfully")
    }

    /**
     * CELエンジンを初期化
     */
    private fun initializeCEL() {
        val startTime = System.currentTimeMillis()

        // 設定から値を取得
        val config = configManager.mainConfig.cel

        // CELEngineManagerを初期化
        celEngine = CELEngineManager(
            cacheSize = config.cacheSize,
            evaluationTimeout = config.evaluationTimeout
        )

        // CELEvaluatorを初期化
        celEvaluator = CELEvaluator(celEngine)

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("CEL engine initialized (${duration}ms)")
    }

    /**
     * 設定を初期化
     */
    private fun initializeConfig() {
        val startTime = System.currentTimeMillis()

        configManager = ConfigManager(this)
        configManager.loadConfigs()

        // 設定の妥当性チェック
        if (!configManager.validateConfigs()) {
            DebugLogger.warn("Configuration validation failed, but continuing with current settings")
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("Configuration loaded (${duration}ms)")
    }

    /**
     * パケットエンティティマネージャーを初期化
     */
    private fun initializePacketEntityManager() {
        val startTime = System.currentTimeMillis()

        packetEntityManager = PacketEntityManager(this)
        packetEntityManager.initialize()

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("PacketEntityManager initialized (${duration}ms)")
    }


    /**
     * フォルダを作成（存在しない場合のみ）
     */
    private fun createFolder(name: String): File {
        val folder = File(dataFolder, name)
        if (!folder.exists()) {
            folder.mkdirs()
            DebugLogger.debug("Created folder: $name")
        }
        return folder
    }

    /**
     * プラグインロゴを表示
     */
    private fun displayLogo() {
        val logo = """
            
            §6╔══════════════════════════════════════╗
            §6║  §e█░█ █▄░█ █ █▀█ █░█ █▀▀           §6║
            §6║  §e█▄█ █░▀█ █ ▀▀█ █▄█ ██▄           §6║
            §6║                                      §6║
            §6║  §7AI-Friendly Mob Creation Plugin   §6║
            §6║  §7Version: 1.0.0-SNAPSHOT           §6║
            §6╚══════════════════════════════════════╝
            
        """.trimIndent()

        logger.info(logo)
    }
}