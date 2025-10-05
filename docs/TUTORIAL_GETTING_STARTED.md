# 初めてのMob作成チュートリアル

このチュートリアルでは、Uniqueプラグインを使って初めてのカスタムMobを作成する手順を説明します。

---

## 📦 インストール

### 前提条件
- **サーバー**: Paper 1.21.8以上（Foliaも対応）
- **Java**: Java 21以上

### インストール手順

1. **プラグインJARをダウンロード**
   - 最新版の`Unique-X.X.X.jar`を取得

2. **pluginsフォルダーに配置**
   ```
   server/
   └── plugins/
       └── Unique-X.X.X.jar
   ```

3. **サーバーを起動**
   ```bash
   java -jar paper.jar
   ```

4. **プラグインフォルダーの確認**
   起動後、以下のフォルダーが自動生成されます：
   ```
   plugins/Unique/
   ├── config.yml
   ├── mobs/          # ここにMob定義を配置
   ├── spawns/        # ここにスポーン定義を配置
   ├── skills/        # スキル定義（オプション）
   ├── conditions/    # 条件定義（オプション）
   ├── targeters/     # ターゲッター定義（オプション）
   ├── effects/       # エフェクト定義（オプション）
   └── sample/        # サンプルファイル（読み込まれない）
   ```

---

## 🧟 ステップ1: 初めてのMob作成

### 1.1 基本的なゾンビを作成

`plugins/Unique/mobs/custom_zombie.yml` を作成：

```yaml
# 基本的なカスタムゾンビ
BasicZombie:
  Type: ZOMBIE
  Display: '&cカスタムゾンビ'
  Health: 100
  Damage: 10
```

### 1.2 動作確認

1. **プラグインをリロード**
   ```
   /unique reload
   ```

2. **Mobをスポーン**
   ```
   /unique spawn BasicZombie
   ```

3. **結果**
   - 赤色の名前「カスタムゾンビ」
   - HP: 100
   - 攻撃力: 10

---

## ⚔️ ステップ2: スキルを追加

### 2.1 タイマースキルを追加

`custom_zombie.yml` にスキルを追加：

```yaml
BasicZombie:
  Type: ZOMBIE
  Display: '&cカスタムゾンビ'
  Health: 100
  Damage: 10

  # ここから追加
  Skills:
    OnTimer:
      - name: FireballAttack
        interval: 100  # 5秒ごと（20tick = 1秒）
        targeter:
          type: NearestPlayer
          range: 20
        skills:
          - skill: Fireball
```

### 2.2 スキル定義を作成

`plugins/Unique/skills/basic_skills.yml` を作成：

```yaml
# ファイアボールスキル
Fireball:
  type: Damage
  effects:
    - type: Damage
      amount: "15"
    - type: Particle
      particle: FLAME
      count: 20
    - type: Sound
      sound: ENTITY_BLAZE_SHOOT
```

### 2.3 動作確認

1. **リロード**
   ```
   /unique reload
   ```

2. **スポーン**
   ```
   /unique spawn BasicZombie
   ```

3. **結果**
   - 5秒ごとに最寄りのプレイヤーにファイアボール攻撃
   - ダメージ: 15
   - 炎のパーティクル + 音

---

## 🎯 ステップ3: ターゲッターを活用

### 3.1 複数プレイヤーを対象にする

`custom_zombie.yml` を更新：

```yaml
BasicZombie:
  Type: ZOMBIE
  Display: '&cカスタムゾンビ'
  Health: 100
  Damage: 10

  Skills:
    OnTimer:
      # 範囲攻撃に変更
      - name: AreaAttack
        interval: 100
        targeter:
          type: RadiusPlayers  # 変更
          range: 10.0
        skills:
          - skill: AreaDamage
```

### 3.2 範囲ダメージスキルを作成

`basic_skills.yml` に追加：

```yaml
# 範囲ダメージスキル
AreaDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "10"
    - type: Particle
      particle: EXPLOSION_NORMAL
      count: 50
    - type: Sound
      sound: ENTITY_GENERIC_EXPLODE
```

---

## 🌍 ステップ4: 自動スポーンを設定

### 4.1 スポーン定義を作成

`plugins/Unique/spawns/zombie_spawn.yml` を作成：

```yaml
# ワールドスポーンの近くに定期スポーン
ZombieSpawner:
  mob: "BasicZombie"
  spawnRate: 200  # 10秒ごと
  maxNearby: 5    # 最大5体まで

  # スポーン条件
  conditions:
    - "world.isNight"  # 夜間のみ

  # スポーン範囲（円形）
  region:
    type: "circle"
    center:
      x: 0
      y: 64
      z: 0
    radius: 50.0
```

### 4.2 動作確認

1. **リロード**
   ```
   /unique reload
   ```

2. **結果**
   - 夜間に自動的にスポーン
   - 座標(0, 64, 0)を中心に半径50ブロック内
   - 最大5体まで
   - 10秒ごとにスポーン判定

---

## 🎨 ステップ5: CEL式で動的にする

### 5.1 プレイヤー数でスケールするボス

`mobs/scaling_boss.yml` を作成：

```yaml
ScalingBoss:
  Type: WITHER_SKELETON
  Display: '&4&lスケーリングボス'

  # プレイヤー数でHP・ダメージがスケール
  Health: "100 + (nearbyPlayers.count * 50)"  # 1人につき+50HP
  Damage: "10 + (nearbyPlayers.avgLevel * 0.5)"  # レベルでダメージ増加

  Skills:
    OnTimer:
      - name: ScalingAttack
        interval: 100
        targeter:
          type: NearestPlayer
          range: 20
        skills:
          - skill: DynamicDamage
```

### 5.2 動的ダメージスキル

`skills/basic_skills.yml` に追加：

```yaml
# ターゲットのHPに応じたダメージ
DynamicDamage:
  type: Damage
  effects:
    - type: Damage
      amount: "target.maxHealth * 0.2"  # 最大HPの20%
    - type: Particle
      particle: CRIT_MAGIC
      count: 30
```

### 5.3 動作確認

```
/unique spawn ScalingBoss
```

**結果**:
- プレイヤー1人: HP 150, ダメージ 10
- プレイヤー3人（平均レベル20）: HP 250, ダメージ 20
- スキルダメージはターゲットのHPに比例

---

## 💡 次のステップ

### 学習リソース
- **[スキルシステム入門](TUTORIAL_SKILLS.md)** - OnDamaged, OnDeath, OnAttackの使い方
- **[ターゲッターシステム入門](TUTORIAL_TARGETERS.md)** - 11種類のTargeterを使いこなす
- **[スポーンシステム入門](TUTORIAL_SPAWN.md)** - 高度なスポーン設定
- **[CEL動的機能ガイド](FEATURE_CEL_DYNAMIC.md)** - CEL式を使った動的パラメータ

### リファレンス
- **[Effect一覧](REFERENCE_EFFECTS.md)** - 全11種類のEffectリファレンス
- **[Targeter一覧](REFERENCE_TARGETERS.md)** - 全11種類のTargeterリファレンス
- **[Mob定義リファレンス](REFERENCE_MOB_DEFINITION.md)** - 全プロパティの詳細
- **[CELクイックスタート](CEL_QUICK_START.md)** - CEL式の基本

### サンプルファイル
`plugins/Unique/sample/` フォルダーに15種類のサンプルが用意されています：
- `basic_zombie.yml` - 基本的なMob例
- `skill_system_examples.yml` - スキルシステムの実践例
- `cel_dynamic_features.yml` - CEL動的機能の例
- `spawn_examples.yml` - スポーン設定の例

---

## ⚠️ トラブルシューティング

### Mobがスポーンしない
1. `/unique reload` でプラグインをリロード
2. ログでエラーを確認
3. YAML構文が正しいか確認（インデントに注意）

### スキルが発動しない
1. `interval` が大きすぎないか確認（100 = 5秒）
2. `targeter` の範囲内にプレイヤーがいるか確認
3. `condition` でスキル発動が制限されていないか確認

### CEL式でエラーが出る
1. 文字列比較は引用符で囲む: `"target.gameMode == 'SURVIVAL'"`
2. 変数名が正しいか確認: `entity`, `target`, `world`, `nearbyPlayers`
3. 数値計算は `.0` を付けて浮動小数点にする: `1 / 2.0`

詳細は **[トラブルシューティングガイド](TROUBLESHOOTING.md)** を参照してください。

---

これで基本的なMob作成ができるようになりました！さらに高度な機能を学んで、独自のMobを作成しましょう。
