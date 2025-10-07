# Config System

## 概要

Config System は、Unique プラグインの設定管理を担当する中核システムです。Hoplite ライブラリによる型安全な設定読み込みと、Bukkit の YamlConfiguration による軽量な読み込みの両方をサポートします。

### 主な目的

- **型安全な設定管理**: Kotlin データクラスによる型安全な設定の読み込みと検証
- **階層的な設定構造**: ネストされた設定セクションの適切な管理
- **デフォルト値のサポート**: 設定の欠落時に適切なデフォルト値を提供
- **Hot Reload**: プラグインを再起動せずに設定を再読み込み
- **バリデーション**: 設定値の妥当性チェックとエラーレポート

### システム構成要素

1. **ConfigManager**: メイン設定管理クラス。Hoplite を使用した型安全な読み込み
2. **MainConfig**: メイン設定のデータクラス定義（config.yml に対応）
3. **YamlLoader**: 軽量な YAML 読み込みヘルパー（単純な設定やリスト取得用）

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                    YAML 設定ファイル                         │
│  config.yml, mobs/*.yml, spawns/*.yml, effects/*.yml        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
         ┌────────────────┴────────────────┐
         │                                  │
         ▼                                  ▼
┌────────────────────┐          ┌──────────────────────┐
│  ConfigManager     │          │   YamlLoader         │
│  (Hoplite使用)     │          │   (軽量版)           │
├────────────────────┤          ├──────────────────────┤
│ • 型安全な読み込み  │          │ • 単純な値取得       │
│ • データクラスへ変換│          │ • リスト取得         │
│ • デフォルト値処理  │          │ • セクション走査     │
│ • バリデーション    │          │ • バックアップ機能   │
└────────┬───────────┘          └──────────┬───────────┘
         │                                  │
         ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                       設定オブジェクト                        │
│  MainConfig, DebugConfig, PerformanceConfig, etc.           │
│  または YamlConfiguration                                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│               プラグインの各種システム                        │
│  CEL Engine, Mob Manager, Spawn Manager, etc.              │
└─────────────────────────────────────────────────────────────┘
```

## 実装詳細

### ConfigManager

Hoplite を使用した型安全な設定管理クラス。

```kotlin
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
            mainConfig = loadSingleFile<MainConfig>(configFile)

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
     */
    @OptIn(ExperimentalHoplite::class)
    inline fun <reified T : Any> loadSingleFile(file: File): T {
        DebugLogger.verbose("Loading file: ${file.name}")

        val startTime = System.nanoTime()

        // ClassLoaderを明示的に設定
        val originalClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = this::class.java.classLoader

        val config = try {
            ConfigLoaderBuilder.default()
                .addSource(PropertySource.file(file))
                .withExplicitSealedTypes()
                .build()
                .loadConfigOrThrow<T>()
        } finally {
            // ClassLoaderを元に戻す
            Thread.currentThread().contextClassLoader = originalClassLoader
        }

        val duration = (System.nanoTime() - startTime) / 1_000_000L
        DebugLogger.timing("Load ${file.name}", duration)

        return config
    }

    /**
     * ディレクトリ内の全YAMLファイルを読み込み
     */
    inline fun <reified T : Any> loadDirectory(directory: File): Map<String, T> {
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
                val config: T = loadSingleFile<T>(file)
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
}
```

**主な機能:**

1. **型安全な読み込み**: Hoplite によるデータクラスへの自動マッピング
2. **ClassLoader 管理**: Bukkit プラグイン環境での適切な ClassLoader 設定
3. **エラーハンドリング**: 読み込み失敗時のフォールバック処理
4. **パフォーマンス計測**: 読み込み時間の測定とログ出力
5. **ディレクトリ一括読み込み**: 複数の設定ファイルを一度に読み込み

### MainConfig データクラス

メイン設定を表す型安全なデータクラス。

```kotlin
/**
 * メイン設定
 *
 * config.ymlに対応するデータクラス
 * Hopliteがデフォルト値を自動的に使用
 */
data class MainConfig(
    val debug: DebugConfig = DebugConfig(),
    val performance: PerformanceConfig = PerformanceConfig(),
    val spawn: SpawnConfig = SpawnConfig(),
    val cel: CelConfig = CelConfig(),
    val damage: DamageConfig = DamageConfig(),
    val resources: ResourceConfig = ResourceConfig(),
    val experimental: ExperimentalConfig = ExperimentalConfig()
)

/**
 * デバッグ設定
 */
data class DebugConfig(
    val enabled: Boolean = true,
    val verbose: Boolean = false,
    val logSkillExecution: Boolean = true,
    val logCelEvaluation: Boolean = false,
    val logTimings: Boolean = false
)

/**
 * パフォーマンス設定
 */
data class PerformanceConfig(
    val asyncByDefault: Boolean = true,
    val packetEntityUpdateInterval: Int = 1,
    val maxConcurrentSkills: Int = 100,
    val viewDistance: Double = 64.0,
    val autoSpawnOnJoin: Boolean = true,
    val aiTickInterval: Int = 1,
    val deadEntityCleanupTicks: Long = 40L,
    val batchCleanupDelayMs: Long = 100L,
    val skipAiWhenNoViewers: Boolean = true,
    val damageMemoryTicks: Long = 200L,
    val contextSearchRange: Double = 50.0
)

/**
 * スポーン設定
 */
data class SpawnConfig(
    val enabled: Boolean = true,
    val checkInterval: Int = 20,
    val maxTotalSpawns: Int = 200
)

/**
 * CEL設定
 */
data class CelConfig(
    val cacheSize: Int = 1000,
    val evaluationTimeout: Long = 100L
)

/**
 * ダメージ計算設定
 */
data class DamageConfig(
    val defaultFormula: String? = null
)

/**
 * リソースキー設定
 */
data class ResourceConfig(
    val customNamespace: String = "custom",
    val logMissingResources: Boolean = false,
    val enableCustomKeys: Boolean = true
)

/**
 * 実験的機能設定
 */
data class ExperimentalConfig(
    val enableNewFeatures: Boolean = false
)
```

**設計の特徴:**

1. **デフォルト値**: すべてのパラメータにデフォルト値を設定
2. **階層的構造**: 関連する設定をサブクラスでグループ化
3. **Immutable**: データクラスによる不変性の保証
4. **ドキュメント**: KDoc コメントによる各設定項目の説明
5. **型安全性**: Boolean、Int、Long、Double などの適切な型定義

### YamlLoader

軽量な YAML 読み込みヘルパー。単純な値の取得やリスト操作に使用します。

```kotlin
object YamlLoader {

    /**
     * YAMLファイルを読み込み
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
     * 2つのYAML設定をマージ
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
```

**主な機能:**

1. **軽量な読み込み**: Hoplite を使わずに Bukkit の YamlConfiguration を直接使用
2. **デフォルト値サポート**: 値が存在しない場合のデフォルト値提供
3. **マージ機能**: 複数の YAML ファイルをマージ
4. **バックアップ機能**: 設定ファイルのバックアップと世代管理
5. **セクション走査**: ConfigurationSection の走査とキー取得

### 設定のバリデーション

ConfigManager は、読み込んだ設定の妥当性をチェックします。

```kotlin
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
```

**バリデーションのポイント:**

1. **範囲チェック**: 数値が適切な範囲内にあるか確認
2. **必須項目チェック**: 必須項目が設定されているか確認
3. **整合性チェック**: 複数の設定項目間の整合性を確認
4. **エラーレポート**: 問題がある場合は詳細なエラーメッセージを出力

## 使用例

### メイン設定の読み込み

```yaml
# config.yml
debug:
  enabled: true
  verbose: false
  logSkillExecution: true
  logCelEvaluation: false
  logTimings: false

performance:
  asyncByDefault: true
  packetEntityUpdateInterval: 1
  maxConcurrentSkills: 100
  viewDistance: 64.0
  aiTickInterval: 1
  deadEntityCleanupTicks: 40
  skipAiWhenNoViewers: true

spawn:
  enabled: true
  checkInterval: 20
  maxTotalSpawns: 200

cel:
  cacheSize: 1000
  evaluationTimeout: 100

resources:
  customNamespace: "myserver"
  logMissingResources: false
  enableCustomKeys: true
```

```kotlin
// プラグイン起動時
val configManager = ConfigManager(plugin)
configManager.loadConfigs()

// 設定を使用
val debugMode = configManager.mainConfig.debug.enabled
val maxSkills = configManager.mainConfig.performance.maxConcurrentSkills
val spawnInterval = configManager.mainConfig.spawn.checkInterval
```

### カスタムデータクラスの読み込み

```yaml
# mobs/boss.yml
name: "BossMonster"
health: 1000.0
damage: 50.0
skills:
  - "fireball"
  - "lightning"
drops:
  - item: DIAMOND
    chance: 0.5
```

```kotlin
// データクラス定義
data class MobConfig(
    val name: String,
    val health: Double,
    val damage: Double,
    val skills: List<String>,
    val drops: List<DropConfig>
)

data class DropConfig(
    val item: String,
    val chance: Double
)

// 読み込み
val mobsDir = File(plugin.dataFolder, "mobs")
val mobConfigs = configManager.loadDirectory<MobConfig>(mobsDir)

// 使用
val bossMob = mobConfigs["boss"]
println("Boss name: ${bossMob?.name}")
println("Boss health: ${bossMob?.health}")
```

### 軽量な YAML 読み込み

```kotlin
// YamlLoaderを使った簡単な読み込み
val file = File(plugin.dataFolder, "simple.yml")
val yaml = YamlLoader.load(file) ?: return

// 値の取得
val mobName = YamlLoader.getString(yaml, "mob.name", "DefaultMob")
val mobHealth = YamlLoader.getDouble(yaml, "mob.health", 100.0)
val skills = YamlLoader.getStringList(yaml, "mob.skills", emptyList())

// セクションのキー取得
val mobKeys = YamlLoader.getKeys(yaml, "mobs")
mobKeys.forEach { key ->
    println("Mob: $key")
}
```

### 設定の再読み込み

```kotlin
// コマンドやイベントから
configManager.reloadConfigs()

// バリデーション付き
if (configManager.validateConfigs()) {
    println("設定は正常です")
} else {
    println("設定にエラーがあります")
}
```

### バックアップと復元

```kotlin
// 設定ファイルのバックアップ
val configFile = File(plugin.dataFolder, "config.yml")
val backupFile = YamlLoader.backup(configFile)

// 古いバックアップを削除（最新5世代を保持）
YamlLoader.cleanupBackups(plugin.dataFolder, keepCount = 5)

// 設定のマージ
val defaultYaml = YamlLoader.load(File(plugin.dataFolder, "default.yml"))
val customYaml = YamlLoader.load(File(plugin.dataFolder, "custom.yml"))

if (defaultYaml != null && customYaml != null) {
    val merged = YamlLoader.merge(defaultYaml, customYaml)
    YamlLoader.save(merged, File(plugin.dataFolder, "merged.yml"))
}
```

## パフォーマンス最適化

### Lazy Loading

設定ファイルは必要になった時点で読み込みます。

```kotlin
class FeatureManager(private val configManager: ConfigManager) {
    // 遅延初期化
    private val featureConfigs by lazy {
        val dir = File(plugin.dataFolder, "features")
        configManager.loadDirectory<FeatureConfig>(dir)
    }

    fun getFeature(name: String): FeatureConfig? {
        return featureConfigs[name]
    }
}
```

### キャッシング

頻繁にアクセスする設定値はキャッシュします。

```kotlin
class ConfigCache(private val configManager: ConfigManager) {
    // よく使う設定をキャッシュ
    val debugMode: Boolean by lazy { configManager.mainConfig.debug.enabled }
    val maxSkills: Int by lazy { configManager.mainConfig.performance.maxConcurrentSkills }
    val spawnInterval: Int by lazy { configManager.mainConfig.spawn.checkInterval }

    fun refresh() {
        // キャッシュをクリア（再読み込み時）
        // lazy delegateは再評価されない点に注意
    }
}
```

### ディレクトリ読み込みの最適化

並列読み込みで複数ファイルを高速に処理します。

```kotlin
fun loadDirectoryParallel(directory: File): Map<String, MobConfig> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyMap()
    }

    val files = directory.listFiles { file ->
        file.extension == "yml" || file.extension == "yaml"
    } ?: return emptyMap()

    return files.asSequence()
        .mapNotNull { file ->
            try {
                val config = configManager.loadSingleFile<MobConfig>(file)
                file.nameWithoutExtension to config
            } catch (e: Exception) {
                DebugLogger.error("Failed to load ${file.name}", e)
                null
            }
        }
        .toMap()
}
```

## ベストプラクティス

### 1. データクラスの設計

設定構造をデータクラスで明確に定義します。

```kotlin
// 良い例 - 明確な型定義
data class SkillConfig(
    val name: String,
    val cooldown: Int,
    val manaCost: Int,
    val effects: List<EffectConfig>,
    val conditions: List<String> = emptyList()  // デフォルト値
)

// 悪い例 - Any型の乱用
data class SkillConfig(
    val name: String,
    val properties: Map<String, Any>  // 型安全性が失われる
)
```

### 2. デフォルト値の設定

すべてのパラメータにデフォルト値を設定します。

```kotlin
// 良い例
data class PerformanceConfig(
    val asyncByDefault: Boolean = true,
    val maxConcurrentSkills: Int = 100,
    val viewDistance: Double = 64.0
)

// 悪い例 - デフォルト値なし
data class PerformanceConfig(
    val asyncByDefault: Boolean,
    val maxConcurrentSkills: Int,
    val viewDistance: Double
)
```

### 3. 設定の階層化

関連する設定をグループ化して階層構造にします。

```yaml
# 良い例
performance:
  async:
    enabled: true
    maxConcurrentSkills: 100
  entity:
    updateInterval: 1
    viewDistance: 64.0

# 悪い例 - フラットな構造
performanceAsyncEnabled: true
performanceMaxConcurrentSkills: 100
performanceEntityUpdateInterval: 1
performanceEntityViewDistance: 64.0
```

### 4. エラーハンドリング

設定読み込みエラーに適切に対応します。

```kotlin
fun loadConfigSafely(file: File): MobConfig {
    return try {
        configManager.loadSingleFile<MobConfig>(file)
    } catch (e: Exception) {
        DebugLogger.error("Failed to load config: ${file.name}", e)
        // フォールバック設定を返す
        MobConfig(
            name = "DefaultMob",
            health = 100.0,
            damage = 10.0
        )
    }
}
```

### 5. 設定の文書化

各設定項目にコメントを追加します。

```yaml
performance:
  # 非同期実行をデフォルトで有効にする
  # true: スキルは非同期で実行され、サーバーのメインスレッドをブロックしません
  # false: スキルは同期で実行されます
  asyncByDefault: true

  # 同時に実行できるスキルの最大数
  # この値を超えるとスキル実行がキューに入ります
  # 推奨値: 50-200
  maxConcurrentSkills: 100
```

## トラブルシューティング

### 読み込みエラー

**問題**: 設定ファイルが読み込めない

```
Failed to load config.yml: Cannot deserialize value of type `Int` from String value
```

**解決策**: YAML の型と データクラスの型が一致しているか確認

```yaml
# 修正前 - 文字列として解釈される
maxConcurrentSkills: "100"

# 修正後 - 数値として解釈される
maxConcurrentSkills: 100
```

### デフォルト値が使われない

**問題**: デフォルト値が期待通りに使われない

**解決策**: YAML に値が存在する場合はデフォルト値は使われません。YAML から項目を削除するか、コメントアウトします。

```yaml
# 修正前 - デフォルト値を使いたいのに明示的に設定されている
asyncByDefault: false

# 修正後 - コメントアウトしてデフォルト値を使用
# asyncByDefault: false  # デフォルト値（true）を使用
```

### ClassLoader 関連エラー

**問題**: Bukkit プラグイン環境で Hoplite が動作しない

```
ClassNotFoundException: com.sksamuel.hoplite.ConfigLoader
```

**解決策**: ConfigManager の loadSingleFile() メソッドは ClassLoader を適切に設定しています。プラグインの依存関係に Hoplite が正しく含まれているか確認してください。

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.5")
}
```

## 関連ドキュメント

- [CEL Engine](cel-engine.md) - CEL 式を使用した動的設定値の評価
- [Mob Manager](../mob-system/mob-manager.md) - モブ定義ファイルの読み込み
- [Spawn System](../mob-system/spawn-system.md) - スポーン定義ファイルの読み込み
- [Debug Logger](../util/debug-logger.md) - デバッグログ機能

## まとめ

Config System は Unique プラグインの設定管理を担当する重要なコンポーネントです。以下の特徴を持ちます:

1. **型安全性**: Kotlin データクラスによる型安全な設定管理
2. **柔軟性**: Hoplite と YamlLoader の2つのアプローチをサポート
3. **デフォルト値**: すべての設定にデフォルト値を提供
4. **バリデーション**: 設定値の妥当性チェックとエラーレポート
5. **Hot Reload**: プラグインを再起動せずに設定を再読み込み可能
6. **パフォーマンス**: 効率的な読み込みとキャッシング機能

適切な Config System の活用により、保守性が高く、拡張しやすいプラグイン設定を実現できます。
