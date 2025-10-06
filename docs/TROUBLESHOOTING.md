# トラブルシューティング

Uniqueプラグインで発生する可能性のある問題と解決方法をまとめたガイドです。

## 目次

- [プラグインが起動しない](#プラグインが起動しない)
- [Mobがスポーンしない](#mobがスポーンしない)
- [スキルが発動しない](#スキルが発動しない)
- [エラーメッセージ別の対処法](#エラーメッセージ別の対処法)
- [パフォーマンスの問題](#パフォーマンスの問題)
- [CEL式のエラー](#cel式のエラー)
- [よくある質問](#よくある質問)

---

## プラグインが起動しない

### 問題: プラグインがロードされない

#### 原因1: Javaバージョンが古い

**症状:**
```
Unsupported class file major version
```

**解決方法:**
- Java 21以上をインストール
- サーバーの起動スクリプトで正しいJavaを指定

```bash
# 正しい例
/usr/lib/jvm/java-21-openjdk/bin/java -jar server.jar
```

#### 原因2: Paper/Foliaバージョンが古い

**症状:**
```
This plugin requires Paper 1.21.8 or higher
```

**解決方法:**
- Paper 1.21.8以上にアップデート
- [Paper公式サイト](https://papermc.io/)から最新版をダウンロード

#### 原因3: 依存プラグインが不足

**症状:**
```
Missing dependency: PacketEvents
```

**解決方法:**
- PacketEventsプラグインをインストール
- [PacketEvents公式](https://github.com/retrooper/packetevents)からダウンロード

### 問題: 設定ファイルが生成されない

**解決方法:**

1. プラグインフォルダを手動作成：
```bash
mkdir -p plugins/Unique/mobs
mkdir -p plugins/Unique/skills
mkdir -p plugins/Unique/effects
mkdir -p plugins/Unique/spawns
```

2. サーバーを再起動

---

## Mobがスポーンしない

### 問題: `/unique spawn` コマンドが動作しない

#### 原因1: Mob IDが間違っている

**症状:**
```
Mob 'XYZ' not found
```

**解決方法:**
- 正しいMob IDを確認：`/unique list`
- 大文字小文字が一致しているか確認
- YAMLファイルが正しくロードされているか確認

#### 原因2: YAMLの構文エラー

**症状:**
```
Failed to load mob definition: XYZ
YAML parse error
```

**解決方法:**

1. インデント（字下げ）を確認
   - **必ず半角スペース2つ**を使用
   - タブは使用不可

```yaml
# ✅ 正しい（スペース2つ）
MobName:
  type: ZOMBIE
  health: '50'

# ❌ 間違い（タブ）
MobName:
	type: ZOMBIE
```

2. 引用符を確認
   - CEL式や数値は `'...'` で囲む
   - 文字列も `'...'` を推奨

```yaml
# ✅ 正しい
health: '50'
damage: '10 + (nearbyPlayers.count * 2)'

# ❌ 間違い
health: 50
damage: 10 + (nearbyPlayers.count * 2)
```

3. オンラインYAMLバリデーターで確認
   - [YAML Lint](http://www.yamllint.com/)

#### 原因3: エンティティタイプが無効

**症状:**
```
Invalid entity type: XYZ
```

**解決方法:**
- 有効なエンティティタイプを使用

```yaml
# ✅ 有効なタイプ
type: ZOMBIE
type: SKELETON
type: BLAZE
type: WITHER

# ❌ 無効なタイプ
type: ZOMBIE_CUSTOM  # 存在しない
type: zombie         # 小文字は不可
```

---

## スキルが発動しない

### 問題: onTimerスキルが発動しない

#### 原因1: intervalの単位を間違えている

**症状:**
- スキルが全く発動しない
- 発動が異常に遅い/早い

**解決方法:**

```yaml
# ❌ 間違い: 秒数を指定
onTimer:
  - name: attack
    interval: 5  # 0.25秒ごと（早すぎる）

# ✅ 正しい: tick数を指定（20tick = 1秒）
onTimer:
  - name: attack
    interval: 100  # 5秒ごと（100 tick = 5秒）
```

#### 原因2: ターゲットが見つからない

**症状:**
- スキルが発動するはずなのに何も起こらない

**解決方法:**

1. **範囲を拡大**
```yaml
targeter:
  type: nearestplayer
  range: 30.0  # 範囲を広げる
```

2. **デバッグモードで確認**
```
/unique debug
```

3. **ターゲッターを変更**
```yaml
# シンプルなターゲッターで試す
targeter:
  type: radiusplayers
  range: 50.0
```

#### 原因3: 条件式が false

**症状:**
- 条件付きスキルが発動しない

**解決方法:**

1. **条件式を削除してテスト**
```yaml
# 条件を一時的に削除
# condition: 'entity.health < 50'
```

2. **条件式を確認**
```yaml
# ✅ 正しい
condition: 'entity.health < entity.maxHealth * 0.5'

# ❌ 間違い
condition: 'entity.hp < 50'  # 変数名が間違い
```

### 問題: ProjectileSkillが発射されない

#### 原因: 無効な発射体タイプ

**解決方法:**

```yaml
# ✅ 有効な発射体タイプ
projectileType: ARROW
projectileType: FIREBALL
projectileType: SNOWBALL
projectileType: WITHER_SKULL

# ❌ 無効
projectileType: CUSTOM_ARROW  # 存在しない
```

### 問題: BeamSkill/AuraSkillが動作しない

#### 原因: Phase 2機能が未実装

**解決方法:**
- プラグインバージョンを確認
- Phase 2以降であることを確認

```
/unique version
```

---

## エラーメッセージ別の対処法

### `NullPointerException`

**原因:**
- 必須フィールドが不足
- 無効な参照

**解決方法:**

1. **必須フィールドを確認**
```yaml
# 必須フィールド
MobName:
  type: ZOMBIE      # 必須
  display: 'Name'   # 必須
  health: '50'      # 必須
```

2. **targeterを確認**
```yaml
# ✅ 正しい
targeter:
  type: nearestplayer
  range: 15.0

# ❌ targeterがない（エラー）
skills:
  - skill: attack
    type: basic
    # targeter: がない！
```

### `ClassCastException`

**原因:**
- 型の不一致
- 誤った値

**解決方法:**

```yaml
# ✅ 正しい型
health: '50'           # String（CEL対応）
interval: 100          # Int
glowing: true          # Boolean

# ❌ 間違った型
health: true           # Boolean（NG）
interval: '100'        # String（NG）
glowing: 'true'        # String（NG）
```

### `Failed to evaluate CEL expression`

**原因:**
- CEL式の構文エラー
- 存在しない変数

**解決方法:**

1. **変数名を確認**
```yaml
# ✅ 正しい変数名
'entity.health'
'target.distance'
'world.time'
'nearbyPlayers.count'

# ❌ 存在しない変数
'entity.hp'
'target.dist'
'player.count'
```

2. **構文を確認**
```yaml
# ✅ 正しい構文
'10 + (entity.health * 0.1)'
'entity.health < entity.maxHealth * 0.5'
'world.time > 13000 ? 20 : 10'

# ❌ 間違った構文
'10 + entity.health * 0.1'  # 括弧がない
'entity.health < 50%'        # %は使えない
'if world.time > 13000 then 20 else 10'  # ifは使えない
```

3. **[完全リファレンス](REFERENCE.md#cel式リファレンス)を参照**

### `Async operation in sync context`

**原因:**
- 同期処理が必要な操作を非同期で実行

**解決方法:**

```yaml
# ブロック操作、テレポートは sync: true を推奨
skills:
  - skill: teleport_skill
    type: basic
    effects:
      - type: teleport
      - type: setblock
    meta:
      sync: true  # これを追加
```

---

## パフォーマンスの問題

### 問題: サーバーがラグい

#### 原因1: BeamSkill/AuraSkillが重い

**解決方法:**

1. **間隔を長くする**
```yaml
# ❌ 重い
onTimer:
  - name: laser
    interval: 20  # 1秒ごと（重い）
    skills:
      - type: beam
        beamDuration: '5000'  # 5秒（長すぎる）

# ✅ 軽い
onTimer:
  - name: laser
    interval: 200  # 10秒ごと
    skills:
      - type: beam
        beamDuration: '2000'  # 2秒
        beamTickInterval: '100'  # 更新間隔を長く
```

2. **ターゲット数を制限**
```yaml
targeter:
  type: radiusplayers
  range: 15.0  # 範囲を狭く

# または
skills:
  - type: aura
    auraMaxTargets: 10  # 最大ターゲット数を制限
```

#### 原因2: 発射体が多すぎる

**解決方法:**

1. **発射間隔を長くする**
```yaml
onTimer:
  - interval: 100  # 短すぎると重い
```

2. **パーティクル密度を下げる**
```yaml
skills:
  - type: projectile
    particle: FLAME
    particleDensity: 0.3  # 密度を下げる（デフォルト: 0.5）
```

#### 原因3: 範囲検索が重い

**解決方法:**

1. **範囲を必要最小限に**
```yaml
# ❌ 重い
targeter:
  type: radiusentities
  range: 100.0  # 広すぎる

# ✅ 軽い
targeter:
  type: radiusentities
  range: 20.0  # 必要最小限
```

2. **フィルターを活用**
```yaml
targeter:
  type: radiusplayers
  range: 30.0
  filter: 'target.distance < 15'  # さらに絞る
```

---

## CEL式のエラー

### 問題: CEL式が評価されない

#### よくある間違い

```yaml
# ❌ 間違い1: 引用符がない
health: 100 + (nearbyPlayers.count * 10)

# ✅ 正しい
health: '100 + (nearbyPlayers.count * 10)'

# ❌ 間違い2: 変数名が間違い
condition: 'entity.hp < 50'

# ✅ 正しい
condition: 'entity.health < 50'

# ❌ 間違い3: 演算子が間違い
condition: 'entity.health == 100'  # 数値の等価比較は避ける

# ✅ 正しい
condition: 'entity.health < 100'
```

### 問題: 期待した値にならない

**デバッグ方法:**

1. **デバッグモード有効化**
```
/unique debug
```

2. **単純な式でテスト**
```yaml
# まず固定値でテスト
health: '100'

# 動作したら変数を追加
health: '100 + nearbyPlayers.count'

# さらに複雑に
health: '100 + (nearbyPlayers.count * 10)'
```

3. **messageエフェクトで確認**
```yaml
effects:
  - type: message
    message: 'Health: {entity.health}'
```

---

## よくある質問

### Q: Mobが強すぎる/弱すぎる

**A:** 数値を調整してください。

```yaml
# 弱い場合
health: '200'  # 体力を増やす
damage: '20'   # 攻撃力を上げる
armor: '15'    # 防御力を上げる

# 強い場合
health: '30'   # 体力を減らす
damage: '5'    # 攻撃力を下げる
```

### Q: スキルの発動タイミングを変えたい

**A:** `interval` を調整してください。

```yaml
# 早くする
interval: 40  # 2秒（20tick = 1秒）

# 遅くする
interval: 300  # 15秒
```

### Q: エフェクトが見えない

**A:** パーティクル数を増やしてください。

```yaml
effects:
  - type: particle
    particle: FLAME
    particleCount: 50  # 数を増やす
    particleSpeed: 0.2
```

### Q: Mobがすぐ消える

**A:** デスポーン設定を確認してください。

```yaml
# config.yml で設定
despawn:
  enabled: false  # デスポーン無効化
```

### Q: 複数のMobを同時にスポーンしたい

**A:** コマンドで数を指定してください。

```
/unique spawn MobName 10
```

### Q: 特定の場所に自動スポーンさせたい

**A:** spawnsファイルを使用してください。

```yaml
# spawns/my_spawn.yml
spawn1:
  mobId: BossMob
  location:
    world: world
    x: 100
    y: 64
    z: 200
  interval: 6000  # 5分ごと（tick）
  maxMobs: 1
```

### Q: プレイヤー数に応じて強さを変えたい

**A:** CEL式を使用してください。

```yaml
StrongZombie:
  type: ZOMBIE
  health: '100 + (nearbyPlayers.count * 20)'
  damage: '10 + (nearbyPlayers.count * 2)'
```

### Q: 夜だけ強くしたい

**A:** CEL式で時間判定してください。

```yaml
NightHunter:
  type: ZOMBIE
  damage: 'world.time > 13000 ? 20 : 10'
```

### Q: 体力が減ると行動が変わるボスを作りたい

**A:** BranchSkillを使用してください。

```yaml
skills:
  onTimer:
    - name: adaptive
      targeter:
        type: nearestplayer
        range: 20.0
      skills:
        - skill: phase_skill
          type: branch
          branches:
            - condition: 'entity.health > entity.maxHealth * 0.5'
              skills: [...]  # フェーズ1
            - condition: 'true'
              skills: [...]  # フェーズ2
```

---

## サポート

### 問題が解決しない場合

1. **ログを確認**
   - `logs/latest.log` を確認
   - エラーメッセージをコピー

2. **デバッグモード有効化**
```
/unique debug
```

3. **最小構成でテスト**
   - 最もシンプルなMob定義で試す
   - 1つずつ機能を追加

4. **GitHub Issuesで報告**
   - [Issues](https://github.com/azuazu3939/unique/issues)
   - エラーログを添付
   - 再現手順を記載

### 報告テンプレート

```
## 環境
- Minecraft: 1.21.8
- Paper: Build #XXX
- Java: 21
- Unique: Phase 2

## 問題
[何が起こるか]

## 期待する動作
[どうなってほしいか]

## 再現手順
1. ...
2. ...
3. ...

## エラーログ
```
[ログをここに貼り付け]
```

## 設定ファイル
```yaml
[問題のあるYAMLをここに貼り付け]
```
```

---

## 関連ドキュメント

- [クイックスタート](QUICKSTART.md)
- [完全ガイド](GUIDE.md)
- [完全リファレンス](REFERENCE.md)
- [変更履歴](CHANGELOG.md)

---

**最終更新**: 2025-10-06
