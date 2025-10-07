# クイックスタート - 5分で始めるUnique

このガイドでは、Uniqueプラグインを使って最初のカスタムMobを作成します。

## 📦 インストール

1. **プラグインを配置**
   ```
   plugins/Unique.jar
   ```

2. **サーバーを起動**
   - 自動で設定ファイルが生成されます

3. **フォルダー構成を確認**
   ```
   plugins/Unique/
   ├── config.yml          # メイン設定
   ├── mobs/              # Mob定義フォルダー
   ├── skills/            # スキルライブラリ
   ├── spawns/            # スポーン設定
   └── logs/              # ログファイル
   ```

## 🎯 最初のMobを作成

### ステップ1: Mob定義ファイルを作成

`plugins/Unique/mobs/my_first_mob.yml` を作成：

```yaml
SimpleFireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "100"
  Skills:
    - projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=20}]} @NearestPlayer{r=30} ~onTimer:60
```

### ステップ2: リロード

サーバーで以下のコマンドを実行：
```
/unique reload
```

### ステップ3: スポーン

以下のコマンドでMobをスポーン：
```
/unique spawn SimpleFireMage
```

🎉 **完成！** 炎の魔法使いが3秒毎にファイヤーボールを発射します。

## 📖 構文の基本

### 超コンパクト構文

```
スキル定義 @ターゲッター ~トリガー
```

- **スキル定義**: `projectile{...}` や `damage{...}` など
- **ターゲッター**: `@NearestPlayer{r=30}` など（誰を狙うか）
- **トリガー**: `~onTimer:60` など（いつ発動するか）

### 例の解説

```yaml
- projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=20}]} @NearestPlayer{r=30} ~onTimer:60
```

1. **projectile{...}**: ファイヤーボールを発射
2. **@NearestPlayer{r=30}**: 30ブロック以内の最も近いプレイヤーを狙う
3. **~onTimer:60**: 60tick（3秒）毎に発動

## 🚀 次のステップ

### スキルを追加

```yaml
SimpleFireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "100"
  Skills:
    # メイン攻撃（3秒毎）
    - projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=20}]} @NearestPlayer{r=30} ~onTimer:60

    # ダメージを受けた時の反撃
    - explosion{amount=15;radius=5.0;fire=true} @Self ~onDamaged

    # 体力が少ない時の回復
    - heal{amount=20} @Self ~onTimer:200
```

### 複数のMobを作成

同じファイルに複数のMobを定義できます：

```yaml
SimpleFireMage:
  type: BLAZE
  display: "&c炎の魔法使い"
  health: "100"
  Skills:
    - projectile{type=FIREBALL;speed=2.5;onHit=[damage{amount=20}]} @NP{r=30} ~onTimer:60

IceMage:
  type: STRAY
  display: "&b氷の魔法使い"
  health: "120"
  Skills:
    - projectile{type=SNOWBALL;speed=3.0;onHit=[damage{amount=15},freeze{duration=3s}]} @NP{r=25} ~onTimer:50

LightningMage:
  type: WITCH
  display: "&e雷の魔法使い"
  health: "80"
  Skills:
    - lightning{damage=25;fire=true} @NP{r=35} ~onTimer:80
```

## 🎯 よく使うパターン

### パターン1: 近接攻撃Mob

```yaml
StrongZombie:
  type: ZOMBIE
  display: "&c強化ゾンビ"
  health: "60"
  damage: "12"
  Skills:
    - damage{amount=20} @TargetLocation ~onAttack
```

### パターン2: 遠距離攻撃Mob

```yaml
Archer:
  type: SKELETON
  display: "&7精鋭射手"
  health: "50"
  Skills:
    - projectile{type=ARROW;speed=4.0;onHit=[damage{amount=15}]} @NP{r=30} ~onTimer:40
```

### パターン3: 範囲攻撃ボス

```yaml
AreaBoss:
  type: WITHER_SKELETON
  display: "&5範囲攻撃ボス"
  health: "200"
  Skills:
    - explosion{amount=20;radius=8.0;kb=2.0} @Self ~onTimer:100
    - damage{amount=25} @RadiusPlayers{r=10;c=5} ~onTimer:120
```

## ⏱️ 時間単位

| 書き方 | 意味 |
|--------|------|
| `~onTimer:30` | 30tick（1.5秒）毎 |
| `~onTimer:60` | 60tick（3秒）毎 |
| `~onTimer:3s` | 3秒毎 |
| `~onTimer:5m` | 5分毎 |

**Tip**: 単位なしの数値はtickとして扱われます（MythicMobs互換）

## 🎨 ターゲッター

| 書き方 | 意味 |
|--------|------|
| `@NP{r=30}` | 30ブロック以内の最も近いプレイヤー |
| `@TL` | 攻撃対象の位置 |
| `@Self` | 自分自身 |
| `@RP{r=10;c=5}` | 10ブロック以内の最大5人のプレイヤー |

## 🔥 エフェクト

| エフェクト | 例 |
|-----------|-----|
| **ダメージ** | `damage{amount=20}` |
| **爆発** | `explosion{amount=15;radius=5.0;fire=true}` |
| **雷** | `lightning{damage=30;fire=true}` |
| **回復** | `heal{amount=10}` |
| **凍結** | `freeze{duration=3s;amplifier=2}` |

## 📝 トリガー

| トリガー | 意味 |
|---------|------|
| `~onTimer:60` | 60tick毎に発動 |
| `~onDamaged` | ダメージを受けた時 |
| `~onDeath` | 死亡時 |
| `~onSpawn` | スポーン時 |
| `~onAttack` | 攻撃時 |

## 💡 Tips

### 改行で見やすく

```yaml
Skills:
  - >
    projectile{
      type=FIREBALL;
      speed=2.5;
      onHit=[damage{amount=20}]
    }
    @NearestPlayer{r=30}
    ~onTimer:60
```

### CEL式で動的に

```yaml
BossMob:
  health: "100 + (nearbyPlayers.count * 50)"  # プレイヤー数に応じて体力増加
  damage: "10 + (nearbyPlayers.avgLevel * 0.5)"  # レベルに応じてダメージ増加
```

### ドロップ設定

```yaml
MyMob:
  type: ZOMBIE
  display: "&c宝箱ゾンビ"
  Drops:
    - item: DIAMOND
      amount: "1-3"
      chance: "0.5"
    - item: EMERALD
      amount: "5"
      chance: "1.0"
```

## 🔧 便利なコマンド

```bash
# Mobをリロード
/unique reload

# Mobをスポーン
/unique spawn <MobName>

# デバッグモード切り替え
/unique debug

# Mob一覧表示
/unique list
```

## 📚 次に読むべきドキュメント

- **[完全ガイド](GUIDE.md)** - 全機能の詳細な説明
- **[構文リファレンス](ULTRA_COMPACT_SYNTAX.md)** - 構文の完全ガイド
- **[機能リファレンス](REFERENCE.md)** - 全パラメータ一覧

## ❓ トラブルシューティング

### Mobがスポーンしない
1. `/unique list` でMobが読み込まれているか確認
2. ログファイル（`plugins/Unique/logs/`）を確認
3. YAML構文エラーがないか確認

### スキルが発動しない
1. `/unique debug` でデバッグモードを有効化
2. トリガーの条件（距離、時間）を確認
3. ターゲッターの範囲を確認

### エラーが出る
1. [トラブルシューティング](TROUBLESHOOTING.md)を確認
2. スペルミスや構文エラーを確認
3. GitHubでIssueを作成

---

**おめでとうございます！** これでUniqueプラグインの基本をマスターしました 🎉
