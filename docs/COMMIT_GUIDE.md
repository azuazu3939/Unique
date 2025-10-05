# Git Commit Guide - CELベース実装対応

## 📋 コミット手順（CEL拡張ベース）

このガイドは、**CELベースの実装方針**に従った開発のコミット手順を示します。
Javaクラスを増やさず、YAMLとCELで機能を実現する設計思想に基づいています。

---

## 🎯 CEL拡張実装のコミット

### 1. CEL変数・関数の拡張

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/cel/CELVariableProvider.kt

git commit -m "feat: extend CEL variables with advanced functions

- math.*: 三角関数、角度変換、基本演算 (cos, sin, tan, sqrt, pow, etc.)
- random.*: 範囲、確率判定、ランダム生成 (range, int, chance, boolean)
- distance.*: 2点間距離計算 (between, horizontal, squared)
- string.*: 文字列操作関数 (contains, startsWith, length, etc.)
- environment.*: 環境情報 (moonPhase, biome, tickOfDay)
- nearbyPlayers.*: プレイヤー統計 (avgLevel, maxLevel, minLevel, count)

後方互換性を維持しつつ、YAMLからアクセス可能な変数を大幅拡張"
```

### 2. YAML機能拡張（CELベース）

```bash
git add src/main/resources/sample/skills/advanced_skills_cel.yml
git add src/main/resources/sample/mobs/practical_examples.yml

git commit -m "feat: add CEL-powered YAML features

【YAML機能追加】
- filter式: ターゲットをCEL式でフィルタリング
- chain設定: 連鎖ターゲティング（稲妻攻撃等）
- CEL式計算: damage, amount等をCEL式で動的計算

【サンプル追加】
- advanced_skills_cel.yml: 15種類のCELベーススキル例
- practical_examples.yml: 13種類の実践的Mob定義
  * HP段階別ボス (BasicBoss)
  * 距離適応型Mob (RangeAdaptiveMob)
  * プレイヤー数スケーリング (PlayerScalingMob)
  * 時間帯反応型 (TimeBasedMob)
  * 環境反応型 (EnvironmentMob)

Javaクラスを追加せず、YAMLとCELだけで高度な機能を実現"
```

### 3. ドキュメント追加

```bash
git add docs/CEL_QUICK_START.md
git add docs/CEL_EXTENSIONS_GUIDE.md
git add docs/CEL_IMPLEMENTATION_COMMIT.md

git commit -m "docs: add comprehensive CEL documentation

- CEL_QUICK_START.md: 初心者向けクイックスタートガイド
- CEL_EXTENSIONS_GUIDE.md: 全CEL変数・関数の完全リファレンス
- CEL_IMPLEMENTATION_COMMIT.md: 実装コミットガイド

YAMLでの実装方法、CEL式の書き方、利用可能な全変数を網羅的に解説"
```

---

## 📝 通常機能追加のコミット

### 1. ターゲッター追加（CEL統合）

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/targeter/ConditionalTargeter.kt
git add src/main/kotlin/com/github/azuazu3939/unique/targeter/ChainTargeter.kt
git add src/main/kotlin/com/github/azuazu3939/unique/targeter/AreaTargeter.kt

git commit -m "feat: add advanced targeters with CEL integration

- ConditionalTargeter: CEL式によるフィルタリング機能
- ChainTargeter: 連鎖ターゲティング（CEL条件で制御）
- AreaTargeter: Circle, Box, Cone, Donut形状（CEL式で範囲計算）

PacketEntity/Entity両対応、条件式はすべてCELで記述"
```

### 2. スキルタイプ追加（CEL統合）

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/SummonSkill.kt
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/TeleportSkill.kt
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/BuffSkill.kt
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/CommandSkill.kt

git commit -m "feat: add skill types with dynamic CEL calculations

- SummonSkill: CEL式で召喚数・持続時間を動的計算
- TeleportSkill: CEL式でテレポート座標を計算
- BuffSkill: CEL式で効果時間・強度を計算
- CommandSkill: プレースホルダーでCEL変数を展開

全スキルがCEL式による動的な値計算に対応"
```

### 3. マネージャー更新

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/manager/TargeterManager.kt
git add src/main/kotlin/com/github/azuazu3939/unique/manager/SkillManager.kt

git commit -m "refactor: update managers for CEL-based configuration

- TargeterManager: CEL式によるフィルタ・条件サポート
- SkillManager: CEL式による動的パラメータ計算サポート

YAML設定からCEL式を評価して動的に値を生成"
```

### 4. サンプルYAML追加

```bash
git add src/main/resources/sample/skills/*.yml
git add src/main/resources/sample/mobs/*.yml
git add src/main/resources/sample/spawns/*.yml

git commit -m "sample: add practical YAML examples with CEL

- 各機能の実用的なCEL式使用例
- コメント付きで学習しやすい構成
- コピー&ペーストで即座に利用可能

実装した機能を示す具体例を提供"
```

---

## 🔍 コミット前チェックリスト

### ✅ CEL統合の確認
- [ ] 新機能の条件判定はCEL式で記述されている
- [ ] 動的な値計算にCEL式を活用している
- [ ] Javaクラスはデータ取得・適用のみを担当
- [ ] ビジネスロジックはYAML/CELに記述

### ✅ コード品質
- [ ] KDocコメントが記述されている
- [ ] パッケージ構造が正しい
- [ ] import文が整理されている
- [ ] 後方互換性が維持されている

### ✅ サンプル・ドキュメント
- [ ] サンプルYAMLにCEL式の使用例がある
- [ ] コメントで説明が十分
- [ ] ドキュメントに新CEL変数が記載されている

---

## 📊 推奨コミット単位

### オプション1: 機能ごとに分割
```bash
# 1. CEL変数拡張
git commit -m "feat: extend CEL variables..."

# 2. YAML機能追加
git commit -m "feat: add YAML features with CEL..."

# 3. サンプル追加
git commit -m "sample: add CEL usage examples..."

# 4. ドキュメント
git commit -m "docs: update CEL reference..."
```

### オプション2: 一括コミット
```bash
git add src/main/kotlin/**/*.kt
git add src/main/resources/sample/**/*.yml
git add docs/*.md

git commit -m "feat: CEL-based feature implementation

【CEL変数拡張】
- math.*, random.*, distance.*, string.*
- environment.*, nearbyPlayers.*

【YAML機能】
- filter式、chain設定、CEL式計算

【サンプル】
- 15スキル、13Mob定義の実例

【ドキュメント】
- クイックスタート、リファレンス、実装ガイド

設計思想: Javaクラスを増やさず、YAMLとCELで実現"
```

---

## 🎯 CELベース設計の原則

1. **条件判定はCELで** - Java側に条件ロジックを書かない
2. **動的計算はCELで** - ダメージ、範囲、個数などをCEL式で計算
3. **Javaは実行環境のみ** - データ取得と適用のみを担当
4. **YAML中心の設定** - ロジックはすべてYAMLに記述

---

## 🚀 プッシュ

```bash
# ブランチを作成
git checkout -b feature/cel-extensions

# コミット後プッシュ
git push -u origin feature/cel-extensions

# プルリクエスト作成
# タイトル: "CELベース機能拡張実装"
# 説明: YAML+CELで実現した機能、追加したCEL変数、サンプル例を記載
```

---

以上がCELベース実装方針に対応したコミットガイドです！