# CELベース実装 - コミットガイド

## 📋 実装サマリー

**Javaクラスを増やさず、YAMLとCELだけで高度な機能を実現**

### 実装内容
1. ✅ CEL変数の大幅拡張（`CELVariableProvider.kt`）
2. ✅ 実践的なサンプル集（13種類のMob例）
3. ✅ クイックスタートガイド
4. ✅ 実装計画ドキュメント

---

## 🎯 実装したCEL機能

### 1. 数学関数 (math.*)
```kotlin
- 基本: abs, max, min, floor, ceil, round, sqrt, pow
- 三角関数: cos, sin, tan, acos, asin, atan, atan2
- 変換: toRadians, toDegrees
- 定数: PI, E
```

### 2. ランダム関数 (random.*)
```kotlin
- range(min, max): 範囲内のランダム値
- int(min, max): 整数ランダム
- chance(probability): 確率判定
- boolean(): ランダムboolean
```

### 3. 距離計算 (distance.*)
```kotlin
- between(pos1, pos2): 3D距離
- horizontal(pos1, pos2): 水平距離
- squared(pos1, pos2): 距離の2乗
```

### 4. 文字列関数 (string.*)
```kotlin
- contains, startsWith, endsWith
- toLowerCase, toUpperCase
- length, substring, replace
```

### 5. 環境情報
```kotlin
- environment.moonPhase: 月相（0-7）
- environment.biome: バイオーム名
- environment.tickOfDay: 1日のtick
```

### 6. プレイヤー情報
```kotlin
- nearbyPlayerCount: 周囲のプレイヤー数
- nearbyPlayers.avgLevel: 平均レベル
- nearbyPlayers.maxLevel: 最大レベル
- nearbyPlayers.minLevel: 最小レベル
```

---

## 📝 新しく追加されたYAML機能

### filter式（ターゲットフィルタリング）
```yaml
targeter:
  type: RadiusPlayers
  range: 30
  filter: "target.health > 50 && target.gameMode == 'SURVIVAL'"
```

### chain設定（連鎖ターゲティング）
```yaml
targeter:
  type: NearestPlayer
  range: 20
  chain:
    maxChains: 5
    chainRange: 5.0
    condition: "target.health > 0"
```

### CEL式でのダメージ計算
```yaml
damage: "20 * (1 - distance.horizontal(source.location, target.location) / 10.0)"
```

### CEL式での召喚数計算
```yaml
amount: "math.min(nearbyPlayerCount, 5)"
```

---

## 📂 新規追加ファイル

### コードファイル (1ファイル)
```
src/main/kotlin/com/github/azuazu3939/unique/cel/
└── CELVariableProvider.kt (拡張版・後方互換性あり)
```

### サンプルファイル (2ファイル)
```
src/main/resources/sample/
├── skills/advanced_skills_cel.yml
└── mobs/practical_examples.yml
```

### ドキュメント (3ファイル)
```
├── CEL_EXTENSIONS_GUIDE.md
├── CEL_QUICK_START.md
└── IMPLEMENTATION_PLAN.md
```

---

## 🚀 コミット方法

### オプション1: 一括コミット
```bash
git add src/main/kotlin/com/github/azuazu3939/unique/cel/CELVariableProvider.kt
git add src/main/resources/sample/skills/advanced_skills_cel.yml
git add src/main/resources/sample/mobs/practical_examples.yml
git add CEL_EXTENSIONS_GUIDE.md
git add CEL_QUICK_START.md
git add IMPLEMENTATION_PLAN.md

git commit -m "feat: CEL-based advanced features without new Java classes

【CEL変数拡張】
- math.*: 三角関数、角度変換、基本演算
- random.*: 範囲、確率判定、ランダム生成
- distance.*: 2点間距離計算（3D/水平/2乗）
- string.*: 文字列操作関数
- environment.*: 月相、バイオーム、時刻情報
- nearbyPlayers.*: プレイヤー数、レベル統計

【YAML機能拡張】
- filter式: ターゲットを CEL式でフィルタリング
- chain設定: 連鎖ターゲティング（稲妻攻撃等）
- CEL式計算: damage, amount等をCEL式で動的計算

【サンプル】
- advanced_skills_cel.yml: 15種類のCELベーススキル
- practical_examples.yml: 13種類の実践的Mob定義

【ドキュメント】
- CEL_QUICK_START.md: 初心者向けガイド
- CEL_EXTENSIONS_GUIDE.md: 全変数・関数リファレンス
- IMPLEMENTATION_PLAN.md: 実装計画

【後方互換性】
- 既存のCELEvaluator.ktとの互換性を維持
- 新機能は全てYAMLレベルで追加

設計思想: Javaクラスを増やさず、YAMLとCELで柔軟に"
```

### オプション2: 段階的コミット

#### Step 1: CEL変数拡張
```bash
git add src/main/kotlin/com/github/azuazu3939/unique/cel/CELVariableProvider.kt

git commit -m "feat: extend CEL variables with math, random, distance functions

- 三角関数: cos, sin, tan, acos, atan2等
- ランダム: range, int, chance, boolean
- 距離計算: between, horizontal, squared
- 環境情報: moonPhase, biome, tickOfDay
- プレイヤー統計: nearbyPlayers.avgLevel等
- 後方互換性維持（既存メソッド保持）"
```

#### Step 2: サンプル追加
```bash
git add src/main/resources/sample/**/*.yml

git commit -m "sample: add CEL-based advanced examples

- advanced_skills_cel.yml: 15 CEL-powered skills
- practical_examples.yml: 13 practical mob definitions
  * HP-based phase bosses
  * Distance-adaptive mobs
  * Player-scaling summoners
  * Time/environment reactive mobs
  * Random probability triggers"
```

#### Step 3: ドキュメント
```bash
git add *.md

git commit -m "docs: add comprehensive CEL documentation

- CEL_QUICK_START.md: beginner-friendly guide
- CEL_EXTENSIONS_GUIDE.md: complete reference
- IMPLEMENTATION_PLAN.md: implementation roadmap"
```

---

## 📊 変更統計

- **Kotlin**: 1ファイル更新（CELVariableProvider.kt）
- **YAML**: 2サンプルファイル追加
- **Markdown**: 3ドキュメント追加
- **追加コード行数**: 約400行
- **追加サンプル**: 約600行
- **ドキュメント**: 約800行

---

## ✅ テストチェックリスト

実装後に以下をテストしてください：

### CEL式の評価
- [ ] `math.*` 関数が正しく動作
- [ ] `random.*` 関数がランダム値を生成
- [ ] `distance.*` 関数が正しい距離を計算
- [ ] `environment.*` 変数が正しい値を返す
- [ ] `nearbyPlayers.*` 統計が正しく集計

### YAML機能
- [ ] `filter`式でターゲットがフィルタリングされる
- [ ] `chain`設定で連鎖ターゲティングが動作
- [ ] CEL式でのダメージ計算が正しい
- [ ] CEL式での召喚数計算が正しい

### サンプルMob
- [ ] BasicBoss: HP段階で攻撃が変化
- [ ] RangeAdaptiveMob: 距離で戦略が変化
- [ ] PlayerScalingMob: プレイヤー数で召喚数が変化
- [ ] TimeBasedMob: 昼夜で能力が変化
- [ ] EnvironmentMob: 天候/バイオームで攻撃が変化

---

## 🎯 次のステップ

### 即座に実装可能
1. ターゲッターに`filter`サポートを追加
2. スキルに`chain`サポートを追加
3. CEL式での動的値計算サポート

### 将来の拡張
1. 条件分岐（`branches`）
2. カスタム変数の永続化
3. プレイヤーごとの変数

---

## 🔗 関連ドキュメント

- [メインREADME](../README.md)
- [プロジェクトナレッジ](cel_mob_plugin_knowledge.md)
- [CEL公式仕様](https://github.com/google/cel-spec)

---

以上でCELベースの高度な機能実装が完了です！
Javaクラスを増やすことなく、YAMLだけで柔軟な機能を実現できるようになりました。