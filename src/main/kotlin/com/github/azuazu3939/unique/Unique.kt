package com.github.azuazu3939.unique

import com.github.azuazu3939.unique.cel.CELEngineManager
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.config.ConfigManager
import com.github.azuazu3939.unique.entity.PacketEntityManager
import com.github.azuazu3939.unique.listener.MobListener
import com.github.azuazu3939.unique.mob.MobManager
import com.github.azuazu3939.unique.player.PlayerDataManager
import com.github.azuazu3939.unique.skill.SkillExecutor
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.retrooper.packetevents.PacketEvents
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import kotlinx.coroutines.withContext
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
    lateinit var libsFolder: File
        private set
    lateinit var aiFolder: File
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

    // プレイヤーデータマネージャー
    lateinit var playerDataManager: PlayerDataManager
        private set

    /**
     * プラグイン読み込み（非同期）
     */
    override suspend fun onLoadAsync() {
        instance = this
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(instance))
        PacketEvents.getAPI().load()
    }

    /**
     * プラグイン有効化（非同期）
     */
    override fun onEnable() {
        launch {
            withContext(globalRegionDispatcher) {
                PacketEvents.getAPI().init()

                // フォルダ初期化
                initializeFolders()

                // 設定読み込み
                initializeConfig()

                // CELエンジン初期化
                initializeCEL()

                // スキルエグゼキューター初期化
                initializeSkillExecutor()

                // Mobマネージャー初期化
                initializeMobManager()

                // スポーンマネージャー初期化
                initializeSpawnManager()

                // パケットエンティティマネージャー初期化
                initializePacketEntityManager()

                // プレイヤーデータマネージャー初期化
                initializePlayerDataManager()

                // リスナー登録（初期化完了後）
                server.pluginManager.registerEvents(MobListener(instance), instance)

                DebugLogger.info("Unique plugin enabled successfully!")
                DebugLogger.info("Version: ${pluginMeta.version}")
                DebugLogger.info("API Version: ${pluginMeta.apiVersion}")
            }
        }
    }

    /**
     * プラグイン無効化
     */
    override suspend fun onDisableAsync() {
        // プレイヤーデータマネージャーをシャットダウン
        if (::playerDataManager.isInitialized) {
            playerDataManager.shutdown()
        }

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

        // サブフォルダ作成とサンプルファイル生成
        mobsFolder = createFolderWithSamples("mobs", listOf(
            "mobs/basic_mobs.yml",
            "mobs/boss_mobs.yml",
            "mobs/advanced_features_showcase.yml",
            "mobs/options_and_display_examples.yml",
            "mobs/resource_key_examples.yml"
        ))

        spawnsFolder = createFolderWithSamples("spawns", listOf(
            "spawns/world_spawns.yml"
        ))

        skillsFolder = createFolderWithSamples("skills", listOf(
            "skills/combat_skills.yml",
            "skills/aura_beam_skills.yml",
            "skills/projectile_skills.yml"
        ))

        conditionsFolder = createFolderWithSamples("conditions", listOf(
            "conditions/custom_conditions.yml"
        ))

        targetersFolder = createFolderWithSamples("targeters", listOf(
            "targeters/custom_targeters.yml"
        ))

        effectsFolder = createFolder("effects")

        // AIフォルダとそのサブフォルダを作成
        aiFolder = File(dataFolder, "ai")
        if (!aiFolder.exists()) {
            aiFolder.mkdirs()
            DebugLogger.debug("Created folder: ai")

            // ai/behaviors/
            createFolder("ai/behaviors")

            // ai/movements/
            createFolder("ai/movements")
        }

        // libsフォルダとそのサブフォルダを作成
        libsFolder = File(dataFolder, "libs")
        if (!libsFolder.exists()) {
            libsFolder.mkdirs()
            DebugLogger.debug("Created folder: libs")

            // libs/Effects/
            createFolderWithSamples("libs/Effects", listOf(
                "libs/Effects/area_effects.yml",
                "libs/Effects/damage_effects.yml",
                "libs/Effects/support_effects.yml",
                "libs/Effects/utility_effects.yml",
                "libs/Effects/visual_effects.yml"
            ))

            // libs/Targeters/
            createFolderWithSamples("libs/Targeters", listOf(
                "libs/Targeters/custom_targeters.yml"
            ))

            // libs/Triggers/
            createFolderWithSamples("libs/Triggers", listOf(
                "libs/Triggers/custom_triggers.yml"
            ))
        }

        sampleFolder = createFolderWithSamples("sample", listOf(
            "sample/README.md",
            "sample/ai_behavior_examples.yml",
            "sample/complete_example.yml",
            "sample/effect_examples.yml",
            "sample/inline_syntax_examples.yml",
            "sample/new_effects_examples.yml",
            "sample/new_skills_examples.yml",
            "sample/new_targeters_examples.yml",
            "sample/projectile_skill_examples.yml",
            "sample/targeter_examples.yml",
            "sample/ultimate_boss_showcase.yml",
            "sample/ultra_compact_syntax.yml"
        ))

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

        configManager = ConfigManager(instance)
        configManager.loadConfigs()

        // 設定の妥当性チェック
        if (!configManager.validateConfigs()) {
            DebugLogger.warn("Configuration validation failed, but continuing with current settings")
        }

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("Configuration loaded (${duration}ms)")
    }

    /**
     * スキルエグゼキューターを初期化
     */
    private fun initializeSkillExecutor() {
        val startTime = System.currentTimeMillis()

        skillExecutor = SkillExecutor(instance)

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("SkillExecutor initialized (${duration}ms)")
    }

    /**
     * Mobマネージャーを初期化
     */
    private fun initializeMobManager() {
        val startTime = System.currentTimeMillis()

        mobManager = MobManager(instance)
        mobManager.loadMobDefinitions()

        val mobCount = mobManager.getAllMobDefinitions().size
        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("MobManager initialized with $mobCount mobs (${duration}ms)")
    }

    /**
     * スポーンマネージャーを初期化
     */
    private fun initializeSpawnManager() {
        val startTime = System.currentTimeMillis()

        spawnManager = com.github.azuazu3939.unique.spawn.SpawnManager(instance)
        spawnManager.loadSpawnDefinitions()
        spawnManager.startSpawnTasks()

        val spawnCount = spawnManager.getAllSpawnDefinitions().size
        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("SpawnManager initialized with $spawnCount spawns (${duration}ms)")
    }

    /**
     * パケットエンティティマネージャーを初期化
     */
    private fun initializePacketEntityManager() {
        val startTime = System.currentTimeMillis()

        packetEntityManager = PacketEntityManager(instance)
        packetEntityManager.initialize()

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("PacketEntityManager initialized (${duration}ms)")
    }

    /**
     * プレイヤーデータマネージャーを初期化
     */
    private fun initializePlayerDataManager() {
        val startTime = System.currentTimeMillis()

        playerDataManager = PlayerDataManager(instance)

        // TODO: マナ/MPシステムは将来的な実装のため、デフォルトでは無効
        // 有効化する場合は以下のコメントを解除

        // マナ自然回復タスク（毎tick）
        // server.globalRegionScheduler.runAtFixedRate(instance, { _ ->
        //     playerDataManager.tickAllPlayers()
        // }, 1L, 1L)

        // マナバー更新タスク（5tick毎）
        // server.globalRegionScheduler.runAtFixedRate(instance, { _ ->
        //     playerDataManager.updateAllManaBars()
        // }, 5L, 5L)

        val duration = System.currentTimeMillis() - startTime
        DebugLogger.info("PlayerDataManager initialized (${duration}ms)")
    }


    /**
     * フォルダを作成（サンプルファイルなし）
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
     * フォルダを作成してサンプルファイルを生成
     */
    private fun createFolderWithSamples(folderName: String, resourcePaths: List<String>): File {
        val folder = File(dataFolder, folderName)
        val isNewFolder = !folder.exists()

        if (isNewFolder) {
            folder.mkdirs()
            DebugLogger.debug("Created folder: $folderName")

            // サンプルファイルをコピー
            resourcePaths.forEach { resourcePath ->
                try {
                    val fileName = resourcePath.substringAfterLast("/")
                    val targetFile = File(folder, fileName)

                    // リソースから読み込み
                    val resource = getResource(resourcePath)
                    if (resource != null) {
                        resource.use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        DebugLogger.debug("  Generated sample: $fileName")
                    } else {
                        DebugLogger.info("  Resource not found: $resourcePath")
                    }
                } catch (e: Exception) {
                    DebugLogger.error("  Failed to copy sample file: $resourcePath", e)
                }
            }
        }

        return folder
    }
}