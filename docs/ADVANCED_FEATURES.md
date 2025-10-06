# 高度な機能ガイド

このドキュメントでは、Uniqueプラグインの高度な機能について説明します。

## 目次

1. [CEL式カスタムソート](#cel式カスタムソート)
2. [オフセット機能](#オフセット機能)
3. [PacketMobダメージ計算式](#packetmobダメージ計算式)
4. [実践例](#実践例)

---

## CEL式カスタムソート

### 概要

`CUSTOM`ソートモードを使用すると、CEL式で任意のソート基準を指定できます。これにより、防御力、攻撃力、HP割合、複合計算など、あらゆる属性や計算式でターゲットをソートできます。

### 構文

```yaml
@ターゲッター{sort=CUSTOM;sortExpression="式";limit=N}
```

### パラメータ

- **`sort`**: `CUSTOM` を指定
- **`sortExpression`**: ソート基準のCEL式（文字列）
- **`limit`**: 取得する最大数（オプション）

### ソート順序

- **昇順（小さい順）**: `"target.armor"` （そのまま）
- **降順（大きい順）**: `"-target.armor"` （マイナスを付ける）

### 使用可能な属性

ターゲットの全ての属性にアクセスできます：

```yaml
target.health               # 現在のHP
target.maxHealth            # 最大HP
target.armor                # 防御力
target.armorToughness       # 防具強度
target.attackDamage         # 攻撃力
target.attackSpeed          # 攻撃速度
target.knockbackResistance  # ノックバック耐性
target.movementSpeed        # 移動速度

# 計算式も使用可能
target.health / target.maxHealth                    # HP割合
target.armor + target.health                        # 防御力+HP
target.attackDamage * target.attackSpeed            # DPS近似値
distance.between(entity.location, target.location)  # 距離
```

### 基本例

#### 防御力でソート

```yaml
# 防御力が低い順に5体（昇順）
- damage{amount=30} @AL{r=20;sort=CUSTOM;sortExpression="target.armor";limit=5}

# 防御力が高い順に3体（降順）
- damage{amount=50} @AL{r=20;sort=CUSTOM;sortExpression="-target.armor";limit=3}
```

#### HP割合でソート

```yaml
# HP割合が低い順に3体を回復
- heal{amount=40} @AP{r=15;sort=CUSTOM;sortExpression="target.health / target.maxHealth";limit=3}

# HP割合が高い順に攻撃（元気な敵を優先）
- damage{amount=35} @AL{r=25;sort=CUSTOM;sortExpression="-(target.health / target.maxHealth)";limit=5}
```

#### 攻撃力でソート

```yaml
# 攻撃力が高い敵を弱体化（降順）
- potion{type=WEAKNESS;duration=200;amplifier=2} @AL{r=20;sort=CUSTOM;sortExpression="-target.attackDamage";limit=3}

# 攻撃力が低い敵をバフ（昇順）
- potion{type=STRENGTH;duration=150} @AL{r=15;sort=CUSTOM;sortExpression="target.attackDamage";limit=4}
```

### 複合式の例

#### タンク性能（防御力 + HP）

```yaml
# 最もタンク性能が高い敵を優先攻撃（降順）
- damage{amount=70} @AL{r=25;sort=CUSTOM;sortExpression="-(target.armor + target.health)";limit=3}

# タンク性能が低い敵から攻撃（昇順）
- damage{amount=40} @AL{r=20;sort=CUSTOM;sortExpression="target.armor + target.health";limit=5}
```

#### 総合戦闘力

```yaml
# 攻撃力 + HP + 防御力の合計が高い敵
- damage{amount=80} @AL{r=30;sort=CUSTOM;sortExpression="-(target.attackDamage + target.health + target.armor)";limit=2}
```

#### 条件付きソート

```yaml
# 移動速度が遅い敵を優先（重装備想定）
- damage{amount=45} @AL{r=30;sort=CUSTOM;sortExpression="target.movementSpeed";limit=4}

# ノックバック耐性が低い敵を吹き飛ばし
- push{strength=3.0} @AL{r=15;sort=CUSTOM;sortExpression="target.knockbackResistance";limit=5}
```

---

## オフセット機能

### 概要

オフセット機能を使用すると、ソート後の結果から先頭N個をスキップできます。これにより、「2番目に近い敵」「HP3位～5位の味方」など、ランク指定のターゲティングが可能になります。

### 構文

```yaml
@ターゲッター{sort=モード;offset=N;limit=M}
```

### パラメータ

- **`offset`**: スキップする数（先頭からN個を除外）
- **`limit`**: 取得する最大数
- **`sort`**: ソートモード（NEAREST、LOWEST_HEALTH、CUSTOM等）

### 基本例

#### 距離ランク指定

```yaml
# 2番目に近いプレイヤー（1番目をスキップ）
@AP{r=20;sort=NEAREST;offset=1;limit=1}

# 2～4番目に近いプレイヤー
@AP{r=30;sort=NEAREST;offset=1;limit=3}

# 5～7番目に近い敵
@AL{r=40;sort=NEAREST;offset=4;limit=3}
```

#### HP ランク指定

```yaml
# HP2位～4位の味方にバフ（1位は除外）
@AL{r=25;sort=HIGHEST_HEALTH;offset=1;limit=3}

# HP最下位を除いた下位2～4位を回復
@AP{r=30;sort=LOWEST_HEALTH;offset=1;limit=3}
```

#### 防御力ランク指定

```yaml
# 防御力1位を除いた2～5位に防御バフ（トップは不要）
@AL{r=20;sort=CUSTOM;sortExpression="-target.armor";offset=1;limit=4}

# 防御力最下位3名を除いた中堅層に攻撃
@AL{r=25;sort=CUSTOM;sortExpression="target.armor";offset=3;limit=5}
```

### 実践例

#### スナイパー（最も近い敵を避ける）

```yaml
SniperMob:
  Type: SKELETON
  Skills:
    # 2～4番目に近いプレイヤーを狙撃
    - damage{amount=55} @AP{r=40;sort=NEAREST;offset=1;limit=3} ~onTimer:80
```

#### 支援型（強い味方は無視）

```yaml
SupportMob:
  Type: EVOKER
  Skills:
    # HP1位は無視して2～5位にバフ
    - potion{type=STRENGTH;duration=200} @AL{r=25;sort=HIGHEST_HEALTH;offset=1;limit=4} ~onTimer:160
```

#### アサシン（中堅層を狙う）

```yaml
AssassinMob:
  Type: WITHER_SKELETON
  Skills:
    # HP上位3名を除いた4～8位を狙う
    - damage{amount=65} @AP{r=25;sort=HIGHEST_HEALTH;offset=3;limit=5} ~onAttack
```

### offsetとlimitの組み合わせ表

| offset | limit | 結果 |
|--------|-------|------|
| 0      | 1     | 1位 |
| 1      | 1     | 2位 |
| 1      | 2     | 2～3位 |
| 1      | 3     | 2～4位 |
| 2      | 3     | 3～5位 |
| 3      | 5     | 4～8位 |
| 0      | 3     | 1～3位（offsetなし） |

---

## PacketMobダメージ計算式

### 概要

PacketMob専用の機能として、受けるダメージの計算式をYAMLでカスタマイズできます。これにより、各Mobごとに独自のダメージ軽減ロジックを実装できます。

### 構文

```yaml
MobName:
  Type: ZOMBIE
  Armor: "20"
  ArmorToughness: "8"
  DamageFormula: "CEL式"
```

### パラメータ

- **`DamageFormula`**: ダメージ計算のCEL式（String）
- 未指定の場合はデフォルト計算式が使用されます

### 利用可能な変数

計算式内で以下の変数を使用できます：

```yaml
damage              # 受けるダメージ（軽減前）
armor               # 防御力
armorToughness      # 防具強度
health              # 現在のHP
maxHealth           # 最大HP
entity.health       # 同上（entity経由でもアクセス可能）
entity.maxHealth    # 同上
```

### 利用可能な関数

```yaml
min(a, b)           # 最小値
max(a, b)           # 最大値
math.floor(x)       # 切り捨て
math.ceil(x)        # 切り上げ
math.abs(x)         # 絶対値
```

### 基本例

#### Minecraft標準式

```yaml
StandardTank:
  Type: IRON_GOLEM
  Armor: "20"
  DamageFormula: "damage * (1 - min(20, armor) / 25)"
```

防御力20で最大80%軽減（Minecraftの標準計算式）

#### パーセント軽減式

```yaml
SimpleDefender:
  Type: ZOMBIE
  Armor: "15"
  DamageFormula: "damage * (1 - armor / 100)"
```

防御力15で15%軽減（シンプルな計算）

#### 固定値軽減式

```yaml
BarrierKnight:
  Type: ZOMBIE
  Armor: "18"
  DamageFormula: "max(1, damage - (armor * 0.5))"
```

防御力18で9ダメージ軽減（最低1ダメージは保証）

### 応用例

#### 防具強度を含む複雑な計算

```yaml
FortifiedBoss:
  Type: RAVAGER
  Armor: "25"
  ArmorToughness: "12"
  DamageFormula: "damage * (1 - (min(20, armor) + armorToughness / 10) / 25)"
```

防御力と防具強度の両方を考慮

#### HP依存の軽減率

```yaml
AdaptiveArmorBoss:
  Type: IRON_GOLEM
  Armor: "20"
  DamageFormula: "damage * (1 - (armor / 25) * (health / maxHealth))"
```

HPが高いほど軽減率が高い（HPが減ると脆くなる）

#### 最低ダメージ保証型

```yaml
BalancedGuardian:
  Type: WARDEN
  Armor: "22"
  DamageFormula: "max(damage * 0.3, damage * (1 - armor / 25))"
```

どんなに防御力が高くても最低30%のダメージは通す

#### 段階的軽減

```yaml
PhaseDefense:
  Type: ZOMBIE
  Armor: "20"
  DamageFormula: "health > maxHealth * 0.5 ? damage * (1 - armor / 25) : damage * (1 - armor / 30)"
```

HP50%以上では通常軽減、50%以下ではさらに硬くなる

### 計算式の設計ガイド

#### 1. パーセント軽減型

**特徴**: 直感的でわかりやすい

```yaml
# 防御力 = 軽減率(%)
DamageFormula: "damage * (1 - armor / 100)"

# 例: armor=25 → 25%軽減
# damage=100 → 受けるダメージ75
```

**メリット**: シンプル、調整しやすい
**デメリット**: 防御力100で無敵になる

---

#### 2. Minecraft標準型

**特徴**: バニラと同じ計算式

```yaml
DamageFormula: "damage * (1 - min(20, armor) / 25)"

# 防御力20で80%軽減（上限）
```

**メリット**: プレイヤーに馴染みがある
**デメリット**: 上限がある（防御力20以上は無意味）

---

#### 3. 固定値軽減型

**特徴**: 一定値を差し引く

```yaml
DamageFormula: "max(1, damage - armor * 係数)"

# 例: armor=20, 係数=0.5 → 10ダメージ軽減
```

**メリット**: 小ダメージに強い、大ダメージには弱い
**デメリット**: ダメージ1未満になる可能性（maxで保証）

---

#### 4. HP依存型

**特徴**: HPによって軽減率が変化

```yaml
DamageFormula: "damage * (1 - (armor / 25) * (health / maxHealth))"

# HP満タン時に最大軽減、瀕死時は軽減なし
```

**メリット**: 戦況に応じた動的防御
**デメリット**: 計算が複雑

---

#### 5. 最低ダメージ保証型

**特徴**: 必ず一定割合のダメージは通す

```yaml
DamageFormula: "max(damage * 最低割合, 通常計算式)"

# 例: 最低30%は必ず貫通
DamageFormula: "max(damage * 0.3, damage * (1 - armor / 25))"
```

**メリット**: 無敵化を防げる
**デメリット**: 防御力を上げても限界がある

---

## 実践例

### 例1: 防御特化ボス（全機能使用）

```yaml
UltimateDefender:
  Type: IRON_GOLEM
  Display: "&6&l究極の守護者"
  Health: "600"
  Damage: "35"
  Armor: "28"
  ArmorToughness: "15"

  # HP依存の複雑なダメージ軽減式
  DamageFormula: "damage * (1 - ((min(20, armor) + armorToughness / 8) / 25) * (health / maxHealth * 0.8 + 0.2))"

  Skills:
    # 防御力が低い敵を優先攻撃（弱点を突く）
    - damage{amount=50} @AL{r=20;sort=CUSTOM;sortExpression="target.armor";limit=5} ~onTimer:60

    # HP2～4位の味方を回復（最も瀕死は他に任せる）
    - heal{amount=40} @AL{r=25;sort=LOWEST_HEALTH;offset=1;limit=3} ~onTimer:100

    # 2～5番目に近い敵に遠距離攻撃（近すぎず遠すぎない距離）
    - projectile{type=ARROW;speed=2.5;damage=30} @AP{r=40;sort=NEAREST;offset=1;limit=4} ~onTimer:80
```

### 例2: 戦術的アサシン

```yaml
TacticalAssassin:
  Type: WITHER_SKELETON
  Display: "&4&l戦術暗殺者"
  Health: "250"
  Damage: "40"
  Armor: "12"

  # シンプルな%軽減
  DamageFormula: "damage * (1 - armor / 100)"

  Skills:
    # フェーズ1: 防御力が低い順に3体を狙う
    - damage{amount=60} @AL{r=25;sort=CUSTOM;sortExpression="target.armor";limit=3} ~onTimer:60
      {cond=entity.health > entity.maxHealth * 0.5}

    # フェーズ2: HP上位3名を除いた中堅層（4～8位）を狙う
    - damage{amount=80} @AP{r=30;sort=HIGHEST_HEALTH;offset=3;limit=5} ~onTimer:80
      {cond=entity.health <= entity.maxHealth * 0.5}

    # 常時: 攻撃力が高い敵を弱体化
    - potion{type=WEAKNESS;duration=150;amplifier=1} @AL{r=20;sort=CUSTOM;sortExpression="-target.attackDamage";limit=3} ~onTimer:120
```

### 例3: 適応型スナイパー

```yaml
AdaptiveSniper:
  Type: SKELETON
  Display: "&e&l適応型狙撃手"
  Health: "180"
  Damage: "32"
  Armor: "10"

  # 固定値軽減
  DamageFormula: "max(1, damage - (armor * 0.8))"

  Skills:
    # 2～3番目に近いプレイヤーに狙撃（最も近い敵は避ける）
    - damage{amount=70} @AP{r=50;sort=NEAREST;offset=1;limit=2} ~onTimer:100

    # HP割合が低い順に2～4位を狙撃（最も瀕死は放置）
    - damage{amount=65} @AP{r=40;sort=CUSTOM;sortExpression="target.health / target.maxHealth";offset=1;limit=3} ~onTimer:120

    # 移動速度が遅い敵を優先（重装備を想定）
    - projectile{type=ARROW;speed=3.5;damage=50} @AL{r=45;sort=CUSTOM;sortExpression="target.movementSpeed";limit=3} ~onTimer:80
```

---

## まとめ

### CEL式カスタムソート

✅ **メリット**:
- あらゆる属性でソート可能
- 複合計算式に対応
- 柔軟な戦術を実現

📝 **使用例**:
```yaml
sort=CUSTOM;sortExpression="target.armor"           # 防御力昇順
sort=CUSTOM;sortExpression="-target.attackDamage"   # 攻撃力降順
sort=CUSTOM;sortExpression="target.health / target.maxHealth"  # HP割合
```

---

### オフセット機能

✅ **メリット**:
- ランク指定のターゲティング
- 「2番目に近い敵」など精密な選択
- sortと組み合わせて強力

📝 **使用例**:
```yaml
offset=1;limit=1    # 2位
offset=1;limit=3    # 2～4位
offset=3;limit=5    # 4～8位
```

---

### PacketMobダメージ計算式

✅ **メリット**:
- Mob毎に異なる防御ロジック
- HP依存、段階的軽減など高度な計算
- YAMLで簡単に調整可能

📝 **使用例**:
```yaml
DamageFormula: "damage * (1 - armor / 100)"                     # %軽減
DamageFormula: "max(1, damage - armor * 0.5)"                   # 固定値軽減
DamageFormula: "damage * (1 - (armor / 25) * (health / maxHealth))"  # HP依存
```

---

## 参考リンク

- [サンプルファイル](../src/main/resources/mobs/advanced_features_showcase.yml)
- [CEL変数リファレンス](../src/main/resources/README.md#cel変数リファレンス完全版)
- [エイリアス定義](../src/main/resources/aliases.yml)
