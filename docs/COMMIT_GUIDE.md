# Git Commit Guide - Phase 7 追加実装

## 📋 コミット手順

### 1. 新しいターゲッタークラスの追加

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/targeter/ConditionalTargeter.kt
git add src/main/kotlin/com/github/azuazu3939/unique/targeter/ChainTargeter.kt
git add src/main/kotlin/com/github/azuazu3939/unique/targeter/AreaTargeter.kt

git commit -m "feat: add advanced targeter types (Conditional, Chain, Area)

- ConditionalTargeter: フィルタリング機能を持つ条件付きターゲッター
- ChainTargeter: 連鎖的にターゲットを選択するターゲッター  
- AreaTargeter: Circle, Box, Cone, Donut の形状に対応
- 各ターゲッターはPacketEntityとEntityの両方に対応"
```

### 2. 新しいスキルタイプの追加

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/SummonSkill.kt
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/TeleportSkill.kt
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/BuffSkill.kt
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/CommandSkill.kt

git commit -m "feat: add new skill types (Summon, Teleport, Buff, Command)

- SummonSkill: バニラ/カスタムMobの召喚
- TeleportSkill: 5種類のテレポートタイプ (OFFSET, ABSOLUTE, BEHIND, TO_SOURCE, SWAP)
- BuffSkill: ポーション効果と属性変更
- CommandSkill: プレースホルダー対応のコマンド実行
- 全スキルでFolia対応の非同期/同期制御"
```

### 3. マネージャークラスの更新

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/manager/TargeterManager.kt
git add src/main/kotlin/com/github/azuazu3939/unique/manager/SkillManager.kt

git commit -m "refactor: update managers to support new targeter and skill types

- TargeterManager: 新しいターゲッタータイプの登録と生成に対応
- SkillManager: 新しいスキルタイプの読み込みと生成に対応
- YAML設定からの自動生成機能を強化"
```

### 4. サンプルYAMLファイルの追加

```bash
git add src/main/resources/sample/skills/advanced_skills.yml
git add src/main/resources/sample/mobs/advanced_mobs.yml
git add src/main/resources/sample/spawns/advanced_spawns.yml

git commit -m "sample: add comprehensive examples for advanced features

- advanced_skills.yml: 全ての新スキルタイプの使用例
- advanced_mobs.yml: 8種類の高度なMob定義例
  * Summoner, LightningMage, FlameWarrior
  * AncientDragon (フェーズ制ボス)
  * SupportHealer, Assassin, Necromancer
- advanced_spawns.yml: 条件付き/イベント駆動スポーン例"
```

### 5. ドキュメントの更新

```bash
git add README_EXTENDED.md

git commit -m "docs: add extended features documentation

- 新しいターゲッタータイプの詳細説明
- 新しいスキルタイプの使用方法とパラメータ
- 実装例とベストプラクティス
- プレースホルダー一覧
- 実装状況の更新"
```

### 6. 全てをまとめてコミット（推奨）

もし上記を個別にコミットせず、一度にまとめる場合:

```bash
git add src/main/kotlin/com/github/azuazu3939/unique/targeter/*.kt
git add src/main/kotlin/com/github/azuazu3939/unique/skill/types/*.kt
git add src/main/kotlin/com/github/azuazu3939/unique/manager/*.kt
git add src/main/resources/sample/**/*.yml
git add README_EXTENDED.md

git commit -m "feat: Phase 7 - Advanced Targeters and Skills Implementation

【ターゲッター拡張】
- ConditionalTargeter: 条件付きフィルタリング
- ChainTargeter: 連鎖ターゲティング (稲妻/感染効果)
- AreaTargeter: Circle, Box, Cone, Donut形状対応

【スキルタイプ追加】
- SummonSkill: バニラ/カスタムMob召喚、持続時間制御
- TeleportSkill: 5種類のテレポートモード
- BuffSkill: ポーション効果 + 属性変更システム
- CommandSkill: プレースホルダー対応コマンド実行

【マネージャー更新】
- TargeterManager/SkillManager: 新タイプの自動読み込み
- YAML設定からの型安全な生成

【サンプル追加】
- 8種類の高度なMob定義
- ボス戦、連鎖攻撃、サポートMobの実装例
- 条件付きスポーン、イベント駆動スポーン

【ドキュメント】
- README_EXTENDED.md: 全機能の詳細ドキュメント

Phase 6完了後の追加実装
次: イベントリスナー、コマンドハンドラー実装"
```

---

## 🔍 コミット前チェックリスト

### ✅ コード品質
- [ ] 全ての新クラスにKDocコメントが記述されている
- [ ] パッケージ構造が正しい
- [ ] import文が整理されている
- [ ] 未使用のimportがない

### ✅ 機能
- [ ] 全てのターゲッタークラスがEntityとPacketEntityに対応
- [ ] 全てのスキルが非同期/同期を制御可能
- [ ] 条件システムが正しく統合されている
- [ ] エラーハンドリングが適切

### ✅ サンプル
- [ ] サンプルYAMLの構文が正しい
- [ ] 実際に動作するコード例
- [ ] コメントが十分

### ✅ ドキュメント
- [ ] README_EXTENDED.mdに全機能が記載
- [ ] 使用例が明確
- [ ] パラメータの説明が完全

---

## 📊 変更ファイルサマリー

### 新規追加ファイル (7ファイル)
```
src/main/kotlin/com/github/azuazu3939/unique/targeter/
├── ConditionalTargeter.kt
├── ChainTargeter.kt
└── AreaTargeter.kt

src/main/kotlin/com/github/azuazu3939/unique/skill/types/
├── SummonSkill.kt
├── TeleportSkill.kt
├── BuffSkill.kt
└── CommandSkill.kt
```

### 更新ファイル (2ファイル)
```
src/main/kotlin/com/github/azuazu3939/unique/manager/
├── TargeterManager.kt
└── SkillManager.kt
```

### サンプルファイル (3ファイル)
```
src/main/resources/sample/
├── skills/advanced_skills.yml
├── mobs/advanced_mobs.yml
└── spawns/advanced_spawns.yml
```

### ドキュメント (1ファイル)
```
README_EXTENDED.md
```

**合計**: 13ファイル

---

## 🚀 プッシュコマンド

```bash
# ブランチを作成して作業する場合
git checkout -b feature/phase7-advanced-features
git push -u origin feature/phase7-advanced-features

# mainブランチに直接プッシュする場合
git push origin main
```

---

## 📝 Pull Request テンプレート

GitHubでPRを作成する場合の推奨テンプレート:

```markdown
## 概要
Phase 7: 高度なターゲッターとスキルシステムの実装

## 変更内容

### 新機能
- ✨ ConditionalTargeter: 条件付きターゲット選択
- ✨ ChainTargeter: 連鎖ターゲティング
- ✨ AreaTargeter: 4種類の形状対応 (Circle, Box, Cone, Donut)
- ✨ SummonSkill: Mob召喚システム
- ✨ TeleportSkill: 5種類のテレポートモード
- ✨ BuffSkill: ポーション効果 + 属性変更
- ✨ CommandSkill: コマンド実行システム

### 改善
- 🔧 TargeterManager/SkillManager: 新タイプ対応
- 📝 包括的なサンプルYAML追加
- 📖 詳細ドキュメント作成

### サンプル
- 8種類の高度なMob定義
- ボス戦の実装例
- 連鎖攻撃、サポートMobの例

## テスト
- [ ] ConditionalTargeterの条件フィルタリング
- [ ] ChainTargeterの連鎖動作
- [ ] AreaTargeterの各形状
- [ ] SummonSkillのバニラ/カスタムMob召喚
- [ ] TeleportSkillの各モード
- [ ] BuffSkillのポーション効果
- [ ] CommandSkillのプレースホルダー

## スクリーンショット
（必要に応じて追加）

## 関連Issue
- Closes #X (該当するissue番号)

## チェックリスト
- [x] コードレビュー済み
- [x] ドキュメント更新済み
- [x] サンプルファイル追加済み
- [ ] 実機テスト完了
```

---

## 🎯 次のステップ

このコミット後の作業:

1. **Phase 7-B: イベントリスナー**
   ```bash
   git checkout -b feature/phase7b-event-listeners
   ```
    - EntityDamageListener
    - EntityDeathListener
    - PlayerInteractListener

2. **Phase 7-C: コマンドハンドラー**
   ```bash
   git checkout -b feature/phase7c-command-handler
   ```
    - `/unique reload`
    - `/unique spawn <mob>`
    - `/unique list`
    - `/unique debug`

3. **Phase 7-D: 統合テスト**
   ```bash
   git checkout -b feature/phase7d-integration-tests
   ```
    - ボス戦シナリオテスト
    - パフォーマンステスト
    - エッジケーステスト

---

## 🔖 タグ付け（リリース時）

メジャーマイルストーン達成時:

```bash
git tag -a v1.0.0-phase7 -m "Phase 7: Advanced Targeters and Skills"
git push origin v1.0.0-phase7
```

---

## 💡 ベストプラクティス

### コミットメッセージ規約
- `feat:` 新機能
- `fix:` バグ修正
- `refactor:` リファクタリング
- `docs:` ドキュメント
- `sample:` サンプルファイル
- `test:` テスト
- `chore:` その他

### コミットサイズ
- 1機能 = 1コミット
- 大きすぎる場合は分割
- 関連する変更はまとめる

### ブランチ戦略
- `main`: 安定版
- `develop`: 開発版
- `feature/*`: 機能開発
- `hotfix/*`: 緊急修正

---

以上でPhase 7の追加実装のコミットガイドは完了です！