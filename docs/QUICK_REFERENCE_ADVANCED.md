# 高度な機能 - クイックリファレンス

## CEL式カスタムソート

```yaml
# 基本構文
@ターゲッター{sort=CUSTOM;sortExpression="式";limit=数}

# 昇順（小→大）
sortExpression="target.armor"

# 降順（大→小）※マイナスを付ける
sortExpression="-target.armor"

# よく使う例
sortExpression="target.armor"                          # 防御力低い順
sortExpression="-target.armor"                         # 防御力高い順
sortExpression="target.health / target.maxHealth"      # HP割合低い順
sortExpression="-target.attackDamage"                  # 攻撃力高い順
sortExpression="-(target.armor + target.health)"       # タンク性能高い順
sortExpression="target.movementSpeed"                  # 移動速度遅い順
```

## オフセット機能

```yaml
# 基本構文
@ターゲッター{sort=モード;offset=N;limit=M}

# offset = スキップする数
# limit = 取得する数

# よく使うパターン
offset=1;limit=1    # 2位のみ
offset=1;limit=2    # 2～3位
offset=1;limit=3    # 2～4位
offset=2;limit=3    # 3～5位
offset=3;limit=5    # 4～8位

# 実例
@AP{r=20;sort=NEAREST;offset=1;limit=1}              # 2番目に近いプレイヤー
@AL{r=25;sort=LOWEST_HEALTH;offset=1;limit=3}        # HP低い順で2～4位
@AL{r=30;sort=CUSTOM;sortExpression="-target.armor";offset=0;limit=3}  # 防御力1～3位
```

## PacketMobダメージ計算式

```yaml
# 基本構文
MobName:
  DamageFormula: "式"

# 利用可能な変数
damage          # 受けるダメージ
armor           # 防御力
armorToughness  # 防具強度
health          # 現在HP
maxHealth       # 最大HP

# 利用可能な関数
min(a, b)       # 最小値
max(a, b)       # 最大値
math.floor(x)   # 切り捨て

# よく使うパターン

## パターン1: パーセント軽減
DamageFormula: "damage * (1 - armor / 100)"
# armor=20 → 20%軽減

## パターン2: Minecraft標準
DamageFormula: "damage * (1 - min(20, armor) / 25)"
# armor=20 → 80%軽減（上限）

## パターン3: 固定値軽減
DamageFormula: "max(1, damage - armor * 0.5)"
# armor=20 → 10ダメージ軽減

## パターン4: HP依存
DamageFormula: "damage * (1 - (armor / 25) * (health / maxHealth))"
# HP高いほど硬い

## パターン5: 最低ダメージ保証
DamageFormula: "max(damage * 0.3, damage * (1 - armor / 25))"
# 最低30%は貫通
```

## 全機能組み合わせ例

```yaml
UltimateBoss:
  Type: WITHER
  Armor: "25"
  ArmorToughness: "12"

  # HP依存のダメージ軽減
  DamageFormula: "damage * (1 - ((min(20, armor) + armorToughness / 10) / 25) * (health / maxHealth * 0.8 + 0.2))"

  Skills:
    # 防御力が低い順に5体攻撃
    - damage{amount=50} @AL{r=25;sort=CUSTOM;sortExpression="target.armor";limit=5} ~onTimer:60

    # 2～4番目に近い敵に遠距離攻撃
    - projectile{type=FIREBALL} @AP{r=40;sort=NEAREST;offset=1;limit=3} ~onTimer:80

    # HP2～5位の味方を回復
    - heal{amount=30} @AL{r=30;sort=LOWEST_HEALTH;offset=1;limit=4} ~onTimer:100

    # 攻撃力上位3名を弱体化
    - potion{type=WEAKNESS} @AL{r=20;sort=CUSTOM;sortExpression="-target.attackDamage";limit=3} ~onTimer:120
```

## 戦術パターン集

### 弱点を突く戦術

```yaml
# 防御力が低い敵を優先
- damage @AL{r=20;sort=CUSTOM;sortExpression="target.armor";limit=5}

# HP割合が低い敵を狙撃
- damage @AP{r=30;sort=CUSTOM;sortExpression="target.health / target.maxHealth";limit=3}
```

### 強敵対策戦術

```yaml
# タンク性能が高い敵に集中砲火
- damage @AL{r=25;sort=CUSTOM;sortExpression="-(target.armor + target.health)";limit=3}

# 攻撃力が高い敵を弱体化
- potion{type=WEAKNESS} @AL{r=20;sort=CUSTOM;sortExpression="-target.attackDamage";limit=3}
```

### 範囲制限戦術

```yaml
# 最も近い敵は避けて2～4番目を狙う
- damage @AP{r=30;sort=NEAREST;offset=1;limit=3}

# 遠すぎる敵は無視して中距離を狙う
- damage @AL{r=40;sort=NEAREST;limit=5}
```

### 支援戦術

```yaml
# HP1位は無視して2～5位にバフ
- potion{type=STRENGTH} @AL{r=25;sort=HIGHEST_HEALTH;offset=1;limit=4}

# 防御力下位3名を除いた中堅層をサポート
- heal @AL{r=30;sort=CUSTOM;sortExpression="target.armor";offset=3;limit=5}
```

## チートシート

| やりたいこと | 構文 |
|------------|------|
| 防御力が最も低い5体に攻撃 | `@AL{r=20;sort=CUSTOM;sortExpression="target.armor";limit=5}` |
| 防御力が最も高い3体に攻撃 | `@AL{r=20;sort=CUSTOM;sortExpression="-target.armor";limit=3}` |
| HP割合が低い3体を回復 | `@AP{r=15;sort=CUSTOM;sortExpression="target.health / target.maxHealth";limit=3}` |
| 攻撃力が高い3体を弱体化 | `@AL{r=20;sort=CUSTOM;sortExpression="-target.attackDamage";limit=3}` |
| 2番目に近い敵を攻撃 | `@AP{r=30;sort=NEAREST;offset=1;limit=1}` |
| 2～4番目に近い敵を攻撃 | `@AP{r=30;sort=NEAREST;offset=1;limit=3}` |
| HP1位を除いた2～5位にバフ | `@AL{r=25;sort=HIGHEST_HEALTH;offset=1;limit=4}` |
| 防御力20%軽減 | `DamageFormula: "damage * (1 - armor / 100)"` |
| Minecraft標準軽減 | `DamageFormula: "damage * (1 - min(20, armor) / 25)"` |
| 固定10ダメージ軽減 | `DamageFormula: "max(1, damage - 10)"` |
| HP依存軽減 | `DamageFormula: "damage * (1 - (armor / 25) * (health / maxHealth))"` |

## デバッグのヒント

### sortExpressionが動かない場合

1. CEL式の構文を確認（文字列として正しいか）
2. target.プレフィックスを付けているか確認
3. 降順の場合はマイナスを付けているか確認

```yaml
# ❌ 間違い
sortExpression=armor          # target. がない
sortExpression="armor"        # target. がない

# ✅ 正しい
sortExpression="target.armor"
sortExpression="-target.armor"  # 降順
```

### offsetが効かない場合

1. sortを指定しているか確認（sortなしではoffsetは無意味）
2. offsetの値が対象数より小さいか確認

```yaml
# ❌ 間違い
@AP{r=20;offset=1;limit=2}    # sortがない

# ✅ 正しい
@AP{r=20;sort=NEAREST;offset=1;limit=2}
```

### ダメージ計算式が動かない場合

1. PacketMobかどうか確認（通常のEntityでは動作しない）
2. CEL式の構文エラーを確認
3. max/minで値を保証しているか確認

```yaml
# ❌ 問題あり
DamageFormula: "damage - armor * 10"  # マイナスになる可能性

# ✅ 安全
DamageFormula: "max(1, damage - armor * 10)"  # 最低1ダメージ保証
```
