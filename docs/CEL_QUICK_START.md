# CELクイックスタートガイド

## 🎯 CELとは？

**Common Expression Language (CEL)** は、YAMLで条件や計算を柔軟に記述するための式言語です。

### なぜCELを使うのか？

❌ **JavaクラスでハードコーディングNG**:
```kotlin
// これは避けたい！
if (entity.health < 20 && target.gameMode == GameMode.CREATIVE) {
    // ...
}
```

✅ **YAMLで柔軟に記述OK**:
```yaml
condition: "entity.health < 20 && target.gameMode == 'CREATIVE'"
```

---

## 📚 利用可能な変数

### entity（Mob/プレイヤー）
```yaml
entity.type          # "ZOMBIE"
entity.health        # 15.0
entity.maxHealth     # 20.0
entity.isDead        # false
entity.location.x    # 100.5
entity.location.y    # 64.0
entity.location.z    # -50.3
```

### target（ターゲット）
```yaml
target.type          # "PLAYER"
target.health        # 18.0
target.gameMode      # "SURVIVAL"
target.isFlying      # false
target.isSneaking    # true
```

### world（ワールド）
```yaml
world.name           # "world"
world.time           # 6000
world.isDay          # true
world.isNight        # false
world.hasStorm       # false
world.playerCount    # 5
```

### nearbyPlayers（周囲のプレイヤー）
```yaml
nearbyPlayerCount    # 3
nearbyPlayers.avgLevel   # 25
nearbyPlayers.maxLevel   # 40
nearbyPlayers.minLevel   # 10
```

### environment（環境）
```yaml
environment.moonPhase    # 0 (満月)
environment.biome        # "PLAINS"
environment.tickOfDay    # 6000
```

---

## 🧮 利用可能な関数

### math（数学）
```yaml
math.abs(-5)              # 5
math.max(10, 20)          # 20
math.min(5, 3)            # 3
math.floor(3.7)           # 3
math.ceil(3.2)            # 4
math.sqrt(16)             # 4.0
math.pow(2, 3)            # 8.0

# 三角関数
math.cos(math.PI)         # -1.0
math.sin(math.PI / 2)     # 1.0
math.toRadians(180)       # 3.14159...
```

### random（ランダム）
```yaml
random.range(1.0, 10.0)   # 1.0〜10.0のランダム値
random.int(1, 6)          # 1〜6のランダム整数（サイコロ）
random.chance(0.5)        # 50%の確率でtrue
random.boolean()          # ランダムなtrue/false
```

### distance（距離計算）
```yaml
# 3D距離
distance.between(source.location, target.location)

# 水平距離（Y軸無視）
distance.horizontal(source.location, target.location)

# 距離の2乗（高速）
distance.squared(source.location, target.location)
```

### string（文字列）
```yaml
string.contains("Hello", "ell")     # true
string.startsWith("Minecraft", "Mine")  # true
string.toLowerCase("HELLO")         # "hello"
string.length("Test")               # 4
```

---

## 💡 実用例

### 例1: 距離に応じたダメージ
```yaml
# 近いほど強力、10ブロックで0ダメージ
damage: "20 * (1 - distance.horizontal(source.location, target.location) / 10.0)"
```

### 例2: HPが低いほど強力なバフ
```yaml
# HP30%以下で最大レベル3のバフ
amplifier: "math.floor((1 - entity.health / entity.maxHealth) * 3)"
```

### 例3: プレイヤー数に応じた召喚数
```yaml
# プレイヤー2人につき1体召喚（最大5体）
amount: "math.min(math.floor(nearbyPlayerCount / 2), 5)"
```

### 例4: 時間帯とバイオームで分岐
```yaml
condition: "world.isNight && string.contains(environment.biome, 'DARK')"
```

### 例5: 確率的な発動
```yaml
# 30%の確率でスキル発動
condition: "random.chance(0.3)"
```

### 例6: 複雑な範囲判定（円形）
```yaml
filter: >
  math.sqrt(
    math.pow(target.location.x - source.location.x, 2) + 
    math.pow(target.location.z - source.location.z, 2)
  ) <= 10.0
```

### 例7: 円錐範囲の判定
```yaml
filter: >
  distance.horizontal(source.location, target.location) <= 15.0 &&
  math.acos(
    (
      (target.location.x - source.location.x) * math.cos(math.toRadians(source.location.yaw)) +
      (target.location.z - source.location.z) * math.sin(math.toRadians(source.location.yaw))
    ) / distance.horizontal(source.location, target.location)
  ) <= math.toRadians(45)
```

---

## 📝 実践: スキル定義

### シンプルな条件
```yaml
FireballAttack:
  type: Projectile
  # HP50%以下で発動
  condition: "entity.health < entity.maxHealth * 0.5"
  targeter:
    type: NearestPlayer
    range: 30
```

### フィルター付きターゲティング
```yaml
ConditionalFireball:
  type: Projectile
  targeter:
    type: RadiusPlayers
    range: 30
    # HP50以上 かつ サバイバルモードのプレイヤーのみ
    filter: "target.health > 50 && target.gameMode == 'SURVIVAL'"
```

### 動的なダメージ計算
```yaml
DistanceBasedAttack:
  type: Damage
  # 距離に応じてダメージ減衰
  damage: "20 * math.max(0, 1 - distance.horizontal(source.location, target.location) / 15.0)"
```

### プレイヤー数に応じた召喚
```yaml
AdaptiveSummon:
  type: Summon
  summon:
    entityType: ZOMBIE
    # プレイヤー数に応じて召喚数を調整
    amount: "math.min(nearbyPlayerCount, 5)"
```

---

## ⚠️ よくある間違い

### ❌ 文字列の比較で引用符を忘れる
```yaml
# NG
condition: "target.gameMode == SURVIVAL"

# OK
condition: "target.gameMode == 'SURVIVAL'"
```

### ❌ 整数除算
```yaml
# NG（整数除算になる可能性）
amount: "nearbyPlayerCount / 2"

# OK
amount: "math.floor(nearbyPlayerCount / 2.0)"
```

### ❌ 存在しない変数を参照
```yaml
# NG（targetはターゲット指定時のみ）
condition: "target.health > 50"  # スキル実行時にtargetがない場合エラー

# OK
condition: "nearbyPlayerCount > 0"
```

---

## 🔧 デバッグ

### CEL式をテスト
```
/unique debug cel "entity.health < 20"
```

### コンテキストを確認
```
/unique debug context
```

---

## 📖 さらに学ぶ

- [CEL公式ドキュメント](https://github.com/google/cel-spec)
- `CEL_EXTENSIONS_GUIDE.md` - 全変数・関数リファレンス
- `advanced_skills_cel.yml` - 実践的なサンプル集

---

このガイドを活用して、YAMLだけで高度なMob動作を実現しましょう！