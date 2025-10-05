# トラブルシューティングガイド

Uniqueプラグインで発生する可能性のある問題と解決方法をまとめています。

---

## 🔍 目次

1. [Mobがスポーンしない](#mobがスポーンしない)
2. [スキルが発動しない](#スキルが発動しない)
3. [CEL式でエラーが出る](#cel式でエラーが出る)
4. [YAMLパースエラー](#yamlパースエラー)
5. [パフォーマンス問題](#パフォーマンス問題)
6. [コマンドが動作しない](#コマンドが動作しない)
7. [ドロップが正しくない](#ドロップが正しくない)

---

## 🧟 Mobがスポーンしない

### 症状
- `/unique spawn MobName` でMobがスポーンしない
- 自動スポーンが機能しない

### 解決方法

#### 1. Mob定義の確認

**チェックポイント**:
```bash
/unique reload
```

ログで以下を確認：
```
[Unique] Loaded X mob definitions
```

**問題**: `Loaded 0 mob definitions`
- **原因**: YAMLファイルが正しく読み込まれていない
- **解決策**:
  - `plugins/Unique/mobs/` フォルダーにファイルが存在するか確認
  - ファイル拡張子が `.yml` または `.yaml` か確認
  - YAMLの構文エラーがないか確認（インデント、コロンなど）

#### 2. YAML構文の確認

**よくある間違い**:

❌ **NG**: インデントが不正
```yaml
CustomZombie:
Type: ZOMBIE  # インデントが足りない
  Health: 100
```

✅ **OK**: 正しいインデント
```yaml
CustomZombie:
  Type: ZOMBIE  # 2スペースインデント
  Health: 100
```

❌ **NG**: コロンの後にスペースがない
```yaml
CustomZombie:
  Type:ZOMBIE  # コロンの後にスペースが必要
```

✅ **OK**: コロンの後にスペース
```yaml
CustomZombie:
  Type: ZOMBIE
```

#### 3. Mob名の確認

**問題**: `Mob not found: MobName`
- **原因**: Mob名が間違っている、または定義されていない
- **解決策**:
  - YAMLファイルのトップレベルキー名を確認
  - 大文字小文字が一致しているか確認

**例**:
```yaml
# mobs/my_mob.yml
CustomZombie:  # ← この名前を使用
  Type: ZOMBIE
```

```bash
/unique spawn CustomZombie  # ← 正しい
/unique spawn customzombie  # ← 間違い（大文字小文字が一致しない）
```

#### 4. EntityTypeの確認

**問題**: `Invalid entity type: XXXX`
- **原因**: 存在しないEntityTypeを指定している
- **解決策**:
  - 正しいEntityType名を使用（全て大文字）
  - [Bukkitドキュメント](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html)で確認

**有効な例**:
```yaml
Type: ZOMBIE          # ✅
Type: WITHER_SKELETON # ✅
Type: ENDER_DRAGON    # ✅
```

**無効な例**:
```yaml
Type: zombie          # ❌ 小文字
Type: Zombie          # ❌ キャメルケース
Type: CUSTOM_ZOMBIE   # ❌ 存在しないタイプ
```

---

## ⚔️ スキルが発動しない

### 症状
- OnTimerスキルが実行されない
- OnDamagedスキルが発動しない

### 解決方法

#### 1. intervalの確認（OnTimer）

**問題**: スキルがまったく発動しない
- **原因**: `interval` が大きすぎる
- **解決策**: intervalの単位は **tick**（20tick = 1秒）

**例**:
```yaml
OnTimer:
  - name: FireballAttack
    interval: 100  # 5秒ごと（20tick × 5 = 100）
```

**デバッグ**:
```yaml
OnTimer:
  - name: TestSkill
    interval: 20  # 1秒ごとに変更してテスト
```

#### 2. ターゲッターの範囲確認

**問題**: ターゲットが見つからない
- **原因**: `range` が小さすぎる、またはプレイヤーが範囲外
- **解決策**: rangeを広げてテスト

**例**:
```yaml
targeter:
  type: NearestPlayer
  range: 5  # 5ブロックは狭い
```

**デバッグ**:
```yaml
targeter:
  type: NearestPlayer
  range: 50  # 範囲を広げてテスト
```

#### 3. 条件式の確認

**問題**: `condition` が常にfalse
- **原因**: CEL式が間違っている
- **解決策**: conditionをシンプルにしてテスト

**例**:
```yaml
# 問題のある条件
condition: "entity.health < entity.maxHealth * 0.5"
```

**デバッグ**:
```yaml
# 条件を削除してテスト
# condition: "true"  # または条件を削除
```

#### 4. スキル定義の確認

**問題**: `Skill not found: SkillName`
- **原因**: スキルが定義されていない、またはファイルが読み込まれていない
- **解決策**:
  - `plugins/Unique/skills/` フォルダーにファイルが存在するか確認
  - `/unique reload` を実行
  - ログで `Loaded X skill definitions` を確認

---

## 🧮 CEL式でエラーが出る

### 症状
- `Failed to evaluate CEL expression: XXX`
- Mobのステータスが期待と異なる

### 解決方法

#### 1. 文字列の引用符

❌ **NG**: 引用符を忘れる
```yaml
condition: "target.gameMode == SURVIVAL"
```

✅ **OK**: 文字列は引用符で囲む
```yaml
condition: "target.gameMode == 'SURVIVAL'"
```

#### 2. 整数除算

❌ **NG**: 整数除算になる可能性
```yaml
amount: "nearbyPlayers.count / 2"
```

✅ **OK**: 浮動小数点で計算
```yaml
amount: "nearbyPlayers.count / 2.0"
# または
amount: "math.floor(nearbyPlayers.count / 2.0)"
```

#### 3. 存在しない変数

❌ **NG**: コンテキストにない変数を参照
```yaml
# Mob定義のHealthで target を参照
Health: "target.maxHealth * 2"  # targetは存在しない
```

✅ **OK**: 利用可能な変数を使用
```yaml
# Mob定義では nearbyPlayers などを使用
Health: "100 + (nearbyPlayers.count * 50)"
```

**利用可能な変数**:
- **Mob定義（Health, Damage）**: `nearbyPlayers.*`, `world.*`, `environment.*`
- **Effect（amount, duration, amplifier）**: `entity.*`, `target.*`, `source.*`, `world.*`, `nearbyPlayers.*`
- **Drop（amount, chance）**: `entity.*`, `killer.*`, `world.*`, `nearbyPlayers.*`, `environment.*`

#### 4. 構文エラー

❌ **NG**: 括弧が不一致
```yaml
amount: "math.max(1, nearbyPlayers.count * 2"  # 閉じ括弧がない
```

✅ **OK**: 括弧を正しく閉じる
```yaml
amount: "math.max(1, nearbyPlayers.count * 2)"
```

#### 5. デバッグ方法

**手順**:
1. CEL式を単純化してテスト
2. 固定値に置き換えてテスト
3. ログでエラーメッセージを確認

**例**:
```yaml
# 元の式
Health: "100 + (nearbyPlayers.count * 50) + (nearbyPlayers.avgLevel * 10)"

# ステップ1: 単純化
Health: "100 + (nearbyPlayers.count * 50)"

# ステップ2: さらに単純化
Health: "100"

# ステップ3: 動作したら徐々に元に戻す
```

---

## 📄 YAMLパースエラー

### 症状
- `Failed to load mob file: XXX`
- `YAML parse error`

### 解決方法

#### 1. インデントの確認

**YAMLはインデントが命**:
- **スペース2個** でインデント（タブは使用不可）
- 同じレベルは同じインデント

❌ **NG**: タブを使用
```yaml
CustomZombie:
	Type: ZOMBIE  # タブは使用不可
```

✅ **OK**: スペース2個
```yaml
CustomZombie:
  Type: ZOMBIE
```

#### 2. コロンとスペース

❌ **NG**: コロンの後にスペースがない
```yaml
CustomZombie:
  Type:ZOMBIE
```

✅ **OK**: コロンの後にスペース
```yaml
CustomZombie:
  Type: ZOMBIE
```

#### 3. 引用符の使用

**特殊文字を含む場合は引用符**:

❌ **NG**: 引用符なし
```yaml
Display: &cカスタムゾンビ  # &が問題
```

✅ **OK**: 引用符で囲む
```yaml
Display: '&cカスタムゾンビ'
```

#### 4. リストの記法

❌ **NG**: ハイフンの後にスペースがない
```yaml
Skills:
  OnTimer:
    -name: FireballAttack
```

✅ **OK**: ハイフンの後にスペース
```yaml
Skills:
  OnTimer:
    - name: FireballAttack
```

#### 5. YAMLバリデーター

オンラインツールでYAMLを検証：
- [YAML Lint](http://www.yamllint.com/)
- [YAML Validator](https://jsonformatter.org/yaml-validator)

---

## ⚡ パフォーマンス問題

### 症状
- サーバーがラグい
- TPSが低下

### 解決方法

#### 1. スポーン数を制限

**問題**: Mobが多すぎる
- **解決策**: `maxNearby` を減らす

```yaml
ZombieSpawn:
  mob: "CustomZombie"
  spawnRate: 200
  maxNearby: 5  # 最大5体に制限
```

#### 2. スキルの間隔を調整

**問題**: スキルが頻繁に実行されすぎ
- **解決策**: `interval` を長くする

```yaml
OnTimer:
  - name: HeavySkill
    interval: 200  # 100 → 200に変更（5秒 → 10秒）
```

#### 3. パーティクルを減らす

**問題**: パーティクルが多すぎる
- **解決策**: `count` を減らす

```yaml
effects:
  - type: Particle
    particle: FLAME
    count: 10  # 100 → 10に変更
```

#### 4. CEL式のキャッシュ

**問題**: 同じCEL式が何度も評価される
- **解決策**: プラグインは自動的にキャッシュしますが、複雑すぎるCEL式は避ける

---

## 📦 コマンドが動作しない

### 症状
- `/unique` コマンドが認識されない
- `Unknown command`

### 解決方法

#### 1. プラグインが有効か確認

```bash
/plugins
```

`Unique` が **緑色** で表示されるか確認。

**赤色の場合**:
- サーバーログでエラーを確認
- プラグインの依存関係を確認（Paper 1.21.8以上、Java 21以上）

#### 2. 権限の確認

**必要な権限**:
```yaml
unique.admin  # すべてのコマンドを使用可能
```

**OP権限を付与**:
```bash
/op <プレイヤー名>
```

#### 3. コマンド一覧

```bash
/unique reload           # プラグインリロード
/unique spawn <MobName>  # Mobスポーン
/unique list             # Mob一覧表示（実装予定）
```

---

## 💎 ドロップが正しくない

### 症状
- Mobを倒してもアイテムがドロップしない
- ドロップ数が期待と異なる

### 解決方法

#### 1. チャンスの確認

**問題**: `chance` が低すぎる
- **解決策**: chanceを1.0にしてテスト

```yaml
drops:
  - item: DIAMOND
    amount: "1"
    chance: "1.0"  # 100%でドロップ
```

#### 2. 条件の確認

**問題**: `condition` が満たされていない
- **解決策**: conditionを削除してテスト

```yaml
drops:
  - item: DIAMOND
    amount: "1"
    chance: "1.0"
    # condition: "killer.gameMode == 'SURVIVAL'"  # 一時的に削除
```

#### 3. amountのCEL式

**問題**: amountのCEL式が0を返す
- **解決策**: 固定値でテスト

```yaml
# 問題のある式
amount: "math.max(1, nearbyPlayers.maxLevel / 10)"  # レベルが低いと0

# デバッグ
amount: "5"  # 固定値でテスト
```

#### 4. アイテム名の確認

**問題**: 存在しないアイテム名
- **解決策**: 正しいMaterial名を使用

```yaml
item: DIAMOND  # ✅ 正しい
# item: DIAMONDS  # ❌ 間違い
```

**Material一覧**: [Bukkit Material](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)

---

## 🆘 それでも解決しない場合

### ログの確認

1. **サーバーログを確認**:
   ```bash
   tail -f logs/latest.log
   ```

2. **エラーメッセージを探す**:
   - `[Unique] ERROR: ...`
   - `Failed to ...`
   - `Exception: ...`

### デバッグモードの有効化

`plugins/Unique/config.yml`:
```yaml
debug:
  enabled: true  # デバッグログを有効化
  logCEL: true   # CEL評価をログ出力
```

### サポート

**GitHubでIssueを作成**:
- リポジトリ: https://github.com/azuazu3939/Unique
- 以下の情報を含めてください:
  - サーバーバージョン（Paper/Folia, バージョン）
  - Uniqueバージョン
  - エラーメッセージ（ログ）
  - 問題を再現するYAML設定

---

## 📚 関連ドキュメント

- **[初めてのMob作成](TUTORIAL_GETTING_STARTED.md)** - 基本的な使い方
- **[CELクイックスタート](CEL_QUICK_START.md)** - CEL式の基本
- **[Effect一覧](REFERENCE_EFFECTS.md)** - Effectリファレンス
- **[Mob定義リファレンス](REFERENCE_MOB_DEFINITION.md)** - Mob定義の詳細

---

問題が解決しましたか？Uniqueを使って、素晴らしいカスタムMobを作成しましょう！
