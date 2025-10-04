package com.github.azuazu3939.unique.config

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.util.DebugLogger
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ConfigSource
import com.sksamuel.hoplite.PropertySource
import java.io.File
import kotlin.reflect.KClass

/**
 * 設定管理クラス
 *
 * Hopliteを使用した型安全なYAML設定の読み込み
 */
class ConfigManager(private val plugin: Unique) {

    /**
     * メイン設定
     */
    lateinit var mainConfig: MainConfig
        private set

    /**
     * 設定を読み込み
     */
    fun loadConfigs() {
        DebugLogger.separator("Loading Configurations")
        val startTime = System.currentTimeMillis()

        try {
            // メイン設定の読み込み
            loadMainConfig()

            val duration = System.currentTimeMillis() - startTime
            DebugLogger.info("All configurations loaded successfully (${duration}ms)")
            DebugLogger.separator()

        } catch (e: Exception) {
            DebugLogger.error("Failed to load configurations", e)
            DebugLogger.separator()
            throw e
        }
    }

    /**
     * メイン設定を読み込み
     */
    private fun loadMainConfig() {
        val configFile = File(plugin.dataFolder, "config.yml")

        // デフォルト設定ファイルをコピー（存在しない場合）
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
            DebugLogger.info("Created default config.yml")
        }

        try {
            mainConfig = loadSingleFile(configFile, MainConfig::class)

            // DebugLoggerに設定を適用
            DebugLogger.setDebugMode(mainConfig.debug.enabled)
            DebugLogger.setVerboseMode(mainConfig.debug.verbose)

            DebugLogger.info("✓ Loaded config.yml")
            DebugLogger.debug("  Debug mode: ${mainConfig.debug.enabled}")
            DebugLogger.debug("  Verbose mode: ${mainConfig.debug.verbose}")

        } catch (e: Exception) {
            DebugLogger.error("Failed to load config.yml", e)
            // デフォルト設定でフォールバック
            mainConfig = MainConfig()
            DebugLogger.warn("Using default configuration")
        }
    }

    /**
     * 単一ファイルを読み込み（Hoplite使用）
     *
     * @param file YAMLファイル
     * @param clazz 読み込む型
     * @return 設定オブジェクト
     */
    inline fun <reified T : Any> loadSingleFile(file: File, clazz: KClass<T>): T {
        DebugLogger.verbose("Loading file: ${file.name}")

        val startTime = System.nanoTime()

        val config = ConfigLoaderBuilder.default()
            .addSource(PropertySource.file(file))
            .build()
            .loadConfigOrThrow(clazz, listOf(ConfigSource.PathSource(file.toPath())), clazz.simpleName)
        val duration = (System.nanoTime() - startTime) / 1_000_000L
        DebugLogger.timing("Load ${file.name}", duration)

        return config
    }

    /**
     * ディレクトリ内の全YAMLファイルを読み込み
     *
     * @param directory ディレクトリ
     * @return ファイル名 -> 設定オブジェクトのマップ
     */
    inline fun <reified T> loadDirectory(directory: File): Map<String, T> {
        if (!directory.exists() || !directory.isDirectory) {
            DebugLogger.debug("Directory does not exist: ${directory.name}")
            return emptyMap()
        }

        val configs = mutableMapOf<String, T>()
        val files = directory.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: return emptyMap()

        DebugLogger.debug("Loading ${files.size} files from ${directory.name}/")

        for (file in files) {
            try {
                val config: T = loadSingleFile(file, Any::class) as T
                val name = file.nameWithoutExtension
                configs[name] = config
                DebugLogger.verbose("  ✓ Loaded ${file.name}")
            } catch (e: Exception) {
                DebugLogger.error("  ✗ Failed to load ${file.name}", e)
            }
        }

        DebugLogger.debug("Loaded ${configs.size}/${files.size} files from ${directory.name}/")
        return configs
    }

    /**
     * デフォルト値とカスタム値をマージして読み込み
     *
     * @param defaultFile デフォルト設定ファイル
     * @param customFile カスタム設定ファイル
     * @param clazz 読み込む型
     * @return マージされた設定
     */
    fun <T> loadWithDefaults(defaultFile: File, customFile: File, clazz: Class<T>): T {
        val sources = mutableListOf<PropertySource>()

        // カスタム設定を優先
        if (customFile.exists()) {
            sources.add(PropertySource.file(customFile))
        }

        // デフォルト設定
        if (defaultFile.exists()) {
            sources.add(PropertySource.file(defaultFile))
        }

        return ConfigLoaderBuilder.default()
            .addSource(sources.first())
            .build()
            .loadConfigOrThrow(defaultFile.path)
    }

    /**
     * 設定を再読み込み
     */
    fun reloadConfigs() {
        DebugLogger.info("Reloading configurations...")
        loadConfigs()
    }

    /**
     * 設定の妥当性チェック
     */
    fun validateConfigs(): Boolean {
        DebugLogger.separator("Validating Configurations")
        var valid = true

        // メイン設定の検証
        if (!validateMainConfig()) {
            valid = false
        }

        if (valid) {
            DebugLogger.info("✓ All configurations are valid")
        } else {
            DebugLogger.warn("✗ Some configurations have issues")
        }

        DebugLogger.separator()
        return valid
    }

    /**
     * メイン設定の検証
     */
    private fun validateMainConfig(): Boolean {
        var valid = true

        // CEL設定の検証
        if (mainConfig.cel.cacheSize < 0) {
            DebugLogger.error("CEL cache size must be >= 0: ${mainConfig.cel.cacheSize}")
            valid = false
        }

        if (mainConfig.cel.evaluationTimeout < 0) {
            DebugLogger.error("CEL evaluation timeout must be >= 0: ${mainConfig.cel.evaluationTimeout}")
            valid = false
        }

        // パフォーマンス設定の検証
        if (mainConfig.performance.maxConcurrentSkills < 1) {
            DebugLogger.error("Max concurrent skills must be >= 1: ${mainConfig.performance.maxConcurrentSkills}")
            valid = false
        }

        // スポーン設定の検証
        if (mainConfig.spawn.checkInterval < 1) {
            DebugLogger.error("Spawn check interval must be >= 1: ${mainConfig.spawn.checkInterval}")
            valid = false
        }

        if (mainConfig.spawn.maxTotalSpawns < 0) {
            DebugLogger.error("Max total spawns must be >= 0: ${mainConfig.spawn.maxTotalSpawns}")
            valid = false
        }

        return valid
    }

    /**
     * 設定のデバッグ情報を出力
     */
    fun printDebugInfo() {
        DebugLogger.separator("Configuration Debug Info")
        DebugLogger.info("Main Config:")
        DebugLogger.info("  Debug: ${mainConfig.debug}")
        DebugLogger.info("  Performance: ${mainConfig.performance}")
        DebugLogger.info("  Spawn: ${mainConfig.spawn}")
        DebugLogger.info("  CEL: ${mainConfig.cel}")
        DebugLogger.info("  Experimental: ${mainConfig.experimental}")
        DebugLogger.separator()
    }
}