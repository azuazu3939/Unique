# 開発ガイド

Uniqueプラグインのカスタム拡張を作成するための開発者向けガイドです。

## 目次

- [開発環境のセットアップ](#開発環境のセットアップ)
- [プロジェクト構成](#プロジェクト構成)
- [カスタムスキルの作成](#カスタムスキルの作成)
- [カスタムエフェクトの作成](#カスタムエフェクトの作成)
- [カスタムターゲッターの作成](#カスタムターゲッターの作成)
- [CEL式の拡張](#cel式の拡張)
- [ビルドとデプロイ](#ビルドとデプロイ)
- [ベストプラクティス](#ベストプラクティス)

---

## 開発環境のセットアップ

### 必要なツール

- **JDK**: Java 21以上
- **ビルドツール**: Gradle 8.5以上
- **IDE**: IntelliJ IDEA（推奨）または Eclipse
- **Git**: バージョン管理

### プロジェクトのクローン

```bash
git clone https://github.com/azuazu3939/unique.git
cd unique
```

### ビルド

```bash
./gradlew build
```

生成されたJARファイル: `build/libs/unique-<version>.jar`

---

## プロジェクト構成

```
unique/
├── src/main/kotlin/com/github/azuazu3939/unique/
│   ├── Unique.kt                      # メインクラス
│   ├── cel/                           # CELシステム
│   │   ├── CELEngineManager.kt
│   │   ├── CELEvaluator.kt
│   │   └── CELVariableProvider.kt
│   ├── skill/                         # スキルシステム
│   │   ├── Skill.kt                   # 基底クラス
│   │   ├── SkillFactory.kt            # スキル生成
│   │   └── types/                     # 各スキル実装
│   │       ├── BasicSkill.kt
│   │       ├── ProjectileSkill.kt
│   │       ├── BeamSkill.kt
│   │       └── AuraSkill.kt
│   ├── effect/                        # エフェクトシステム
│   │   ├── Effect.kt                  # 基底クラス
│   │   └── EffectFactory.kt           # エフェクト生成
│   ├── targeter/                      # ターゲッターシステム
│   │   ├── Targeter.kt                # 基底クラス
│   │   └── TargeterFactory.kt         # ターゲッター生成
│   ├── mob/                           # Mob管理
│   │   ├── MobDefinition.kt           # Mob定義
│   │   └── MobManager.kt              # Mob管理
│   ├── entity/                        # エンティティシステム
│   │   ├── PacketEntity.kt
│   │   └── PacketEntityManager.kt
│   ├── config/                        # 設定管理
│   │   ├── ConfigManager.kt
│   │   └── YamlLoader.kt
│   └── util/                          # ユーティリティ
│       ├── DebugLogger.kt
│       └── Extensions.kt
├── src/main/resources/
│   ├── plugin.yml                     # プラグイン定義
│   ├── config.yml                     # デフォルト設定
│   └── sample/                        # サンプルファイル
└── build.gradle.kts                   # ビルド設定
```

---

## カスタムスキルの作成

### ステップ1: Skillクラスを継承

```kotlin
package com.github.azuazu3939.unique.skill.types

import com.github.azuazu3939.unique.skill.Skill
import com.github.azuazu3939.unique.effect.Effect
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

class CustomSkill(
    private val customParameter: String = "default",
    private val effects: List<Effect> = emptyList()
) : Skill() {

    override suspend fun execute(source: Entity, targets: List<Entity>) {
        // スキルのロジックを実装
        targets.forEach { target ->
            if (target is LivingEntity) {
                // カスタム処理
                performCustomAction(source, target)

                // エフェクトを適用
                effects.forEach { effect ->
                    effect.apply(source, target)
                }
            }
        }
    }

    private fun performCustomAction(source: Entity, target: LivingEntity) {
        // ここにカスタム処理を実装
        DebugLogger.info("CustomSkill executed: $customParameter")
    }
}
```

### ステップ2: MobDefinitionに定義を追加

```kotlin
// MobDefinition.kt に追加
data class SkillReference(
    // 既存フィールド...

    // カスタムスキル用フィールド
    val customParameter: String? = null
)
```

### ステップ3: SkillFactoryに登録

```kotlin
// SkillFactory.kt に追加
private fun createCustomSkill(reference: SkillReference): Skill {
    val customParameter = reference.customParameter ?: "default"
    val effects = reference.effects?.mapNotNull {
        effectFactory.createEffect(it)
    } ?: emptyList()

    return CustomSkill(
        customParameter = customParameter,
        effects = effects
    )
}

// createSkill() メソッドに追加
fun createSkill(reference: SkillReference): Skill? {
    val skillType = reference.type?.lowercase() ?: "basic"
    return try {
        when (skillType) {
            // 既存スキル...
            "custom" -> createCustomSkill(reference)
            else -> null
        }
    } catch (e: Exception) {
        DebugLogger.error("Failed to create skill: $skillType", e)
        null
    }
}
```

### ステップ4: YAMLで使用

```yaml
CustomMob:
  type: ZOMBIE
  health: '100'

  skills:
    onTimer:
      - name: my_custom_skill
        interval: 100

        targeter:
          type: nearestplayer
          range: 20.0

        skills:
          - skill: custom_attack
            type: custom
            customParameter: 'test_value'
            effects:
              - type: damage
                damageAmount: 15.0
```

---

## カスタムエフェクトの作成

### ステップ1: Effectクラスを継承

```kotlin
package com.github.azuazu3939.unique.effect

import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import com.github.azuazu3939.unique.util.DebugLogger

class CustomEffect(
    private val customValue: Double = 1.0
) : Effect() {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        // カスタムエフェクトの処理
        performCustomEffect(source, target)
    }

    private fun performCustomEffect(source: Entity, target: LivingEntity) {
        // ここにカスタム処理を実装
        DebugLogger.info("CustomEffect applied with value: $customValue")

        // 例: カスタム計算
        val result = customValue * 2.0
        target.sendMessage("Custom effect: $result")
    }
}
```

### ステップ2: CEL式対応（オプション）

```kotlin
class CustomEffect(
    private val customValue: String = "1.0"  // CEL式対応のため String型
) : Effect() {

    override suspend fun apply(source: Entity, target: Entity) {
        if (target !is LivingEntity) return

        // CEL式を評価
        val evaluatedValue = evaluateCustomValue(source, target)
        performCustomEffect(source, target, evaluatedValue)
    }

    private fun evaluateCustomValue(source: Entity, target: Entity): Double {
        return try {
            // まず数値として解析を試みる
            customValue.toDoubleOrNull() ?: run {
                // CEL式として評価
                val context = CELVariableProvider.buildTargetContext(source, target)
                Unique.instance.celEvaluator.evaluateNumber(customValue, context)
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate customValue: $customValue", e)
            1.0  // デフォルト値
        }
    }

    private fun performCustomEffect(
        source: Entity,
        target: LivingEntity,
        value: Double
    ) {
        DebugLogger.info("CustomEffect applied with value: $value")
        target.sendMessage("Custom effect: $value")
    }
}
```

### ステップ3: MobDefinitionとFactoryに登録

```kotlin
// MobDefinition.kt
data class EffectDefinition(
    // 既存フィールド...

    // カスタムエフェクト用
    val customValue: String? = null
)

// EffectFactory.kt
private fun createCustomEffect(definition: EffectDefinition): Effect {
    val customValue = definition.customValue ?: "1.0"
    return CustomEffect(customValue = customValue)
}

fun createEffect(definition: EffectDefinition): Effect? {
    return try {
        when (definition.type.lowercase()) {
            // 既存エフェクト...
            "custom" -> createCustomEffect(definition)
            else -> null
        }
    }
}
```

---

## カスタムターゲッターの作成

### ステップ1: Targeterクラスを継承

```kotlin
package com.github.azuazu3939.unique.targeter

import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity

class CustomTargeter(
    private val customRange: Double = 10.0,
    private val customFilter: String? = null
) : Targeter() {

    override fun getTargets(source: Entity): List<Entity> {
        // カスタムターゲット選択ロジック
        val candidates = findCandidates(source)

        // フィルター適用（基底クラスのメソッドを使用）
        return filterByFilter(source, candidates)
    }

    private fun findCandidates(source: Entity): List<Entity> {
        // ここにカスタムターゲット検索ロジックを実装
        return source.world.getNearbyEntities(
            source.location,
            customRange,
            customRange,
            customRange
        ).filterIsInstance<LivingEntity>()
    }
}
```

### ステップ2: ベースターゲッター対応（高度）

```kotlin
class CustomTargeter(
    private val baseTargeter: Targeter? = null,
    private val customParameter: Int = 1,
    private val customFilter: String? = null
) : Targeter() {

    override fun getTargets(source: Entity): List<Entity> {
        // ベースターゲッターから候補を取得
        val candidates = baseTargeter?.getTargets(source) ?: emptyList()

        // カスタムフィルタリング
        val filtered = applyCustomFilter(source, candidates)

        // 基底クラスのフィルターを適用
        return filterByFilter(source, filtered)
    }

    private fun applyCustomFilter(
        source: Entity,
        candidates: List<Entity>
    ): List<Entity> {
        // ここにカスタムフィルタリングロジックを実装
        return candidates.take(customParameter)
    }
}
```

### ステップ3: Factoryに登録

```kotlin
// MobDefinition.kt
data class TargeterDefinition(
    // 既存フィールド...

    // カスタムターゲッター用
    val customRange: Double? = null,
    val customParameter: Int? = null
)

// TargeterFactory.kt
private fun createCustomTargeter(definition: TargeterDefinition): Targeter {
    val customRange = definition.customRange ?: 10.0
    val customFilter = definition.filter

    return CustomTargeter(
        customRange = customRange,
        customFilter = customFilter
    )
}

fun createTargeter(definition: TargeterDefinition): Targeter? {
    return try {
        when (definition.type.lowercase()) {
            // 既存ターゲッター...
            "custom" -> createCustomTargeter(definition)
            else -> null
        }
    }
}
```

---

## CEL式の拡張

### カスタム変数の追加

```kotlin
// CELVariableProvider.kt に追加

fun buildCustomContext(entity: Entity): Map<String, Any> {
    val baseContext = buildEntityContext(entity)

    return baseContext + mapOf(
        "custom" to mapOf(
            "customValue1" to getCustomValue1(entity),
            "customValue2" to getCustomValue2(entity)
        )
    )
}

private fun getCustomValue1(entity: Entity): Double {
    // カスタム値の計算
    return 42.0
}

private fun getCustomValue2(entity: Entity): String {
    // カスタム文字列の取得
    return "custom_data"
}
```

### カスタム関数の追加

```kotlin
// CELEngineManager.kt に追加

private fun registerCustomFunctions(envBuilder: Env.Builder) {
    // カスタム関数の登録
    envBuilder.addFunction(
        "customFunction",
        Overload.unary(
            "customFunction_double",
            { arg -> customFunctionImpl(arg as Double) },
            SimpleType.DOUBLE,
            SimpleType.DOUBLE
        )
    )
}

private fun customFunctionImpl(value: Double): Double {
    // カスタム関数の実装
    return value * 2.0
}
```

使用例:
```yaml
health: 'customFunction(nearbyPlayers.count)'
condition: 'custom.customValue1 > 50'
```

---

## ビルドとデプロイ

### Gradleタスク

```bash
# ビルド
./gradlew build

# クリーンビルド
./gradlew clean build

# テスト実行
./gradlew test

# JAR作成
./gradlew shadowJar
```

### build.gradle.kts の設定

```kotlin
plugins {
    kotlin("jvm") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.github.azuazu3939"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // PacketEvents
    compileOnly("com.github.retrooper:packetevents-spigot:2.0.0")

    // Hoplite (設定管理)
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.5")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.5")

    // CEL-Java
    implementation("dev.cel:cel:0.4.4")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.12.1")
}
```

### デプロイ

1. **ローカルテストサーバー**
```bash
# ビルド
./gradlew shadowJar

# プラグインをコピー
cp build/libs/unique-*.jar /path/to/server/plugins/

# サーバー起動
cd /path/to/server
java -jar paper.jar
```

2. **本番環境**
```bash
# リリースビルド
./gradlew clean shadowJar

# GitHubリリース作成
git tag v1.0.0
git push origin v1.0.0
```

---

## ベストプラクティス

### 1. エラーハンドリング

```kotlin
override suspend fun execute(source: Entity, targets: List<Entity>) {
    try {
        targets.forEach { target ->
            if (target is LivingEntity) {
                performAction(source, target)
            }
        }
    } catch (e: Exception) {
        DebugLogger.error("Failed to execute CustomSkill", e)
    }
}
```

### 2. 非同期処理

```kotlin
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.azuazu3939.unique.Unique

class CustomSkill : Skill() {
    override suspend fun execute(source: Entity, targets: List<Entity>) {
        // 重い処理は非同期で
        Unique.instance.launch {
            performHeavyOperation()
        }

        // 同期が必要な処理
        withContext(Dispatchers.sync) {
            performSyncOperation()
        }
    }
}
```

### 3. CEL式のキャッシュ

```kotlin
class CustomEffect(
    private val valueExpression: String
) : Effect() {

    // 評価結果をキャッシュ
    private val cachedValues = mutableMapOf<String, Double>()

    private fun evaluateValue(source: Entity, target: Entity): Double {
        val cacheKey = "${source.uniqueId}-${target.uniqueId}"
        return cachedValues.getOrPut(cacheKey) {
            val context = CELVariableProvider.buildTargetContext(source, target)
            Unique.instance.celEvaluator.evaluateNumber(valueExpression, context)
        }
    }
}
```

### 4. デバッグログ

```kotlin
DebugLogger.debug("CustomSkill: Starting execution")
DebugLogger.info("CustomSkill: Processed ${targets.size} targets")
DebugLogger.warn("CustomSkill: Unexpected condition detected")
DebugLogger.error("CustomSkill: Fatal error occurred", exception)
```

### 5. 型安全な設定

```kotlin
data class CustomSkillConfig(
    val parameter1: String = "default",
    val parameter2: Int = 10,
    val parameter3: Double = 1.5
)

class CustomSkill(config: CustomSkillConfig) : Skill() {
    private val param1 = config.parameter1
    private val param2 = config.parameter2
    private val param3 = config.parameter3
}
```

### 6. テストの作成

```kotlin
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CustomSkillTest {

    @Test
    fun `test custom skill execution`() {
        val skill = CustomSkill(customParameter = "test")
        val source = mockEntity()
        val targets = listOf(mockEntity())

        runBlocking {
            skill.execute(source, targets)
        }

        // アサーション
        assertEquals(expected, actual)
    }
}
```

---

## コントリビューション

### プルリクエストの作成

1. **Forkする**
```bash
# GitHubでFork
git clone https://github.com/YOUR_USERNAME/unique.git
```

2. **ブランチを作成**
```bash
git checkout -b feature/custom-skill
```

3. **変更をコミット**
```bash
git add .
git commit -m "Add CustomSkill feature"
```

4. **プッシュ**
```bash
git push origin feature/custom-skill
```

5. **プルリクエストを作成**
   - GitHubでプルリクエストを作成
   - 変更内容を説明
   - テスト結果を記載

### コミットメッセージ規約

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:**
- `feat`: 新機能
- `fix`: バグ修正
- `docs`: ドキュメント変更
- `style`: コードスタイル変更
- `refactor`: リファクタリング
- `test`: テスト追加/修正
- `chore`: ビルド設定など

**例:**
```
feat(skill): Add CustomSkill implementation

- Implement CustomSkill class
- Add factory method
- Add YAML definition support

Closes #123
```

---

## リソース

### 公式ドキュメント

- [Paper API](https://docs.papermc.io/)
- [PacketEvents](https://github.com/retrooper/packetevents)
- [CEL-Java](https://github.com/projectnessie/cel-java)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### Unique ドキュメント

- [クイックスタート](QUICKSTART.md)
- [完全ガイド](GUIDE.md)
- [完全リファレンス](REFERENCE.md)
- [トラブルシューティング](TROUBLESHOOTING.md)

### コミュニティ

- **GitHub**: [リポジトリ](https://github.com/azuazu3939/unique)
- **Issues**: [バグ報告・要望](https://github.com/azuazu3939/unique/issues)
- **Wiki**: [コミュニティWiki](https://github.com/azuazu3939/unique/wiki)

---

**最終更新**: 2025-10-06
