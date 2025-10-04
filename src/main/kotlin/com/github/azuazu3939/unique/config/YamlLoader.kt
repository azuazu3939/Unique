package com.github.azuazu3939.unique.config

import com.github.azuazu3939.unique.util.DebugLogger
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * YAML読み込みヘルパー
 *
 * Hopliteを使わない軽量なYAML読み込み
 * 単純な設定やリスト取得に使用
 */
object YamlLoader {

    /**
     * YAMLファイルを読み込み
     *
     * @param file YAMLファイル
     * @return YamlConfiguration
     */
    fun load(file: File): YamlConfiguration? {
        if (!file.exists()) {
            DebugLogger.debug("File does not exist: ${file.name}")
            return null
        }

        return try {
            YamlConfiguration.loadConfiguration(file)
        } catch (e: Exception) {
            DebugLogger.error("Failed to load YAML: ${file.name}", e)
            null
        }
    }

    /**
     * 文字列リストを取得（デフォルト値付き）
     */
    fun getStringList(yaml: YamlConfiguration, path: String, default: List<String> = emptyList()): List<String> {
        return if (yaml.contains(path)) {
            yaml.getStringList(path)
        } else {
            default
        }
    }

    /**
     * 文字列を取得（デフォルト値付き）
     */
    fun getString(yaml: YamlConfiguration, path: String, default: String = ""): String {
        return yaml.getString(path, default) ?: default
    }

    /**
     * 整数を取得（デフォルト値付き）
     */
    fun getInt(yaml: YamlConfiguration, path: String, default: Int = 0): Int {
        return yaml.getInt(path, default)
    }

    /**
     * 実数を取得（デフォルト値付き）
     */
    fun getDouble(yaml: YamlConfiguration, path: String, default: Double = 0.0): Double {
        return yaml.getDouble(path, default)
    }

    /**
     * Boolean値を取得（デフォルト値付き）
     */
    fun getBoolean(yaml: YamlConfiguration, path: String, default: Boolean = false): Boolean {
        return yaml.getBoolean(path, default)
    }

    /**
     * セクションのキーリストを取得
     */
    fun getKeys(yaml: YamlConfiguration, path: String): Set<String> {
        val section = yaml.getConfigurationSection(path)
        return section?.getKeys(false) ?: emptySet()
    }

    /**
     * ディレクトリ内の全YAMLファイルを読み込み
     */
    fun loadDirectory(directory: File): Map<String, YamlConfiguration> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyMap()
        }

        val configs = mutableMapOf<String, YamlConfiguration>()
        val files = directory.listFiles { file ->
            file.extension == "yml" || file.extension == "yaml"
        } ?: return emptyMap()

        for (file in files) {
            val config = load(file)
            if (config != null) {
                configs[file.nameWithoutExtension] = config
            }
        }

        return configs
    }

    /**
     * YAMLファイルを保存
     */
    fun save(yaml: YamlConfiguration, file: File): Boolean {
        return try {
            yaml.save(file)
            true
        } catch (e: Exception) {
            DebugLogger.error("Failed to save YAML: ${file.name}", e)
            false
        }
    }

    /**
     * YAMLの内容をデバッグ出力
     */
    fun printDebug(yaml: YamlConfiguration, indent: Int = 0) {
        val prefix = "  ".repeat(indent)

        yaml.getKeys(false).forEach { key ->
            val value = yaml.get(key)
            when {
                yaml.isConfigurationSection(key) -> {
                    println("$prefix$key:")
                    yaml.getConfigurationSection(key)?.let { section ->
                        val subYaml = YamlConfiguration()
                        section.getKeys(true).forEach { subKey ->
                            subYaml.set(subKey, section.get(subKey))
                        }
                        printDebug(subYaml, indent + 1)
                    }
                }
                value is List<*> -> {
                    println("$prefix$key: [${value.joinToString(", ")}]")
                }
                else -> {
                    println("$prefix$key: $value")
                }
            }
        }
    }

    /**
     * 2つのYAML設定をマージ
     *
     * @param base ベース設定
     * @param override 上書き設定
     * @return マージされた設定
     */
    fun merge(base: YamlConfiguration, override: YamlConfiguration): YamlConfiguration {
        val merged = YamlConfiguration()

        // ベース設定をコピー
        base.getKeys(true).forEach { key ->
            merged.set(key, base.get(key))
        }

        // 上書き設定を適用
        override.getKeys(true).forEach { key ->
            merged.set(key, override.get(key))
        }

        return merged
    }

    /**
     * YAMLファイルのバックアップを作成
     */
    fun backup(file: File): File? {
        if (!file.exists()) {
            return null
        }

        val timestamp = System.currentTimeMillis()
        val backupFile = File(file.parentFile, "${file.nameWithoutExtension}_backup_$timestamp.yml")

        return try {
            file.copyTo(backupFile, overwrite = false)
            DebugLogger.debug("Created backup: ${backupFile.name}")
            backupFile
        } catch (e: Exception) {
            DebugLogger.error("Failed to create backup: ${file.name}", e)
            null
        }
    }

    /**
     * 古いバックアップファイルを削除
     *
     * @param directory ディレクトリ
     * @param keepCount 保持する世代数
     */
    fun cleanupBackups(directory: File, keepCount: Int = 5) {
        if (!directory.exists() || !directory.isDirectory) {
            return
        }

        val backupFiles = directory.listFiles { file ->
            file.name.contains("_backup_") && file.extension == "yml"
        }?.sortedByDescending { it.lastModified() } ?: return

        val toDelete = backupFiles.drop(keepCount)
        toDelete.forEach { file ->
            if (file.delete()) {
                DebugLogger.debug("Deleted old backup: ${file.name}")
            }
        }
    }
}