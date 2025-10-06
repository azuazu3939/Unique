# カスタムリソースキーシステム

## 概要

Uniqueプラグインは、Minecraft標準のリソースキーに加えて、カスタムリソースキーをサポートしています。これにより、将来的なリソースパック対応や拡張性の向上が可能になります。

## リソースキー解決ルール

カスタムリソースキーは、以下の順序で解決されます：

```
1. "custom_namespace:key" で検索
2. 見つからなければ "minecraft:key" で検索
3. それでもなければ "KEY" (大文字変換) で検索
4. 最終的にデフォルト値を返す
```

## 対応リソースタイプ

- **Sound**: 効果音
- **Particle**: パーティクル
- **Material**: アイテム・ブロック
- **Biome**: バイオーム
- **Enchantment**: エンチャント
- **EntityType**: エンティティタイプ

## 設定

### config.yml

```yaml
resources:
  # カスタムリソースキーの名前空間
  customNamespace: "custom"

  # リソースが見つからない場合にエラーログを出力
  logMissingResources: false

  # カスタムリソースキー機能を有効化
  enableCustomKeys: true
```

## 使用例

### 1. 音（Sound）

```yaml
# 従来の形式（そのまま動作）
- sound{type=ENTITY_ENDER_DRAGON_GROWL} @Self ~onSpawn

# カスタムキー形式
- sound{type=custom:boss_roar} @Self ~onSpawn
# → "custom:boss_roar" で検索
# → なければ "minecraft:boss_roar"
# → なければ "BOSS_ROAR"
# → なければデフォルト音

# minecraft:プレフィックス明示
- sound{type=minecraft:entity_skeleton_ambient} @Self ~onTimer:100
```

### 2. パーティクル（Particle）

```yaml
# 従来の形式
- particle{type=FLAME;count=20} @Self ~onTimer:10

# カスタムキー
- particle{type=custom:magic_sparkle;count=50} @Self ~onTimer:20
# → "custom:magic_sparkle" で検索
# → なければ "minecraft:magic_sparkle"
# → なければ "MAGIC_SPARKLE"
# → なければデフォルトパーティクル
```

### 3. アイテム・ブロック（Material）

```yaml
# アイテムディスプレイ
ItemDisplay:
  Type: ITEM
  Item: custom:legendary_sword
  # → "custom:legendary_sword" で検索
  # → なければ "minecraft:legendary_sword"
  # → なければ "LEGENDARY_SWORD"
  # → なければ STONE

# ブロックディスプレイ
BlockDisplay:
  Type: BLOCK
  Block: custom:magic_crystal
```

### 4. ドロップアイテム

```yaml
Drops:
  # カスタムアイテム
  - item: custom:legendary_artifact
    amount: "1"
    chance: "1.0"

  # 通常のアイテム
  - item: DIAMOND
    amount: "5"
    chance: "1.0"
```

## コード内での使用

### ResourceKeyResolverの使用

```kotlin
import com.github.azuazu3939.unique.util.ResourceKeyResolver

// Sound を解決
val sound = ResourceKeyResolver.resolveSound("custom:boss_roar", Sound.ENTITY_ENDER_DRAGON_GROWL)

// Material を解決
val material = ResourceKeyResolver.resolveMaterial("custom:legendary_sword", Material.DIAMOND_SWORD)

// Particle を解決
val particle = ResourceKeyResolver.resolveParticle("custom:magic_sparkle", Particle.FLAME)

// ItemStack を作成
val itemStack = ResourceKeyResolver.createItemStack("custom:legendary_artifact", 1)

// BlockData を解決
val blockData = ResourceKeyResolver.resolveBlockData("custom:magic_crystal")
```

### エフェクトでの使用例

```kotlin
// SoundEffect での使用
val soundType = ResourceKeyResolver.resolveSound(
    definition.sound ?: "BLOCK_NOTE_BLOCK_PLING",
    Sound.BLOCK_NOTE_BLOCK_PLING
)

// ParticleEffect での使用
val particleType = ResourceKeyResolver.resolveParticle(
    definition.particle ?: "FLAME",
    Particle.FLAME
)

// ItemDisplay での使用
val itemStack = ResourceKeyResolver.createItemStack(
    definition.item ?: "DIAMOND",
    1
)
```

## デフォルトダメージ計算式

### 設定

config.ymlでデフォルトのダメージ計算式を設定できます：

```yaml
damage:
  # すべてのPacketMobに適用されるデフォルト式
  defaultFormula: "damage * (1 - armor / 100)"
```

### 優先順位

1. **Mob個別の式**: MobDefinitionで`damageFormula`を指定
2. **共通設定**: config.ymlの`damage.defaultFormula`
3. **組み込みデフォルト**: Minecraft標準式相当

### 使用例

```yaml
# config.yml
damage:
  defaultFormula: "damage * (1 - armor / 100)"

# Mob定義
DefaultMob:
  Type: ZOMBIE
  Armor: "10"
  # damageFormula なし
  # → config.damage.defaultFormula を使用
  # → "damage * (1 - armor / 100)"

CustomMob:
  Type: IRON_GOLEM
  Armor: "20"
  DamageFormula: "max(damage * 0.5, damage * (1 - armor / 30))"
  # → この式が優先される

FallbackMob:
  Type: SKELETON
  Armor: "5"
  # damageFormula なし
  # config.damage.defaultFormula も null
  # → 組み込みデフォルトを使用
```

## 利用可能なCEL変数（ダメージ計算）

```yaml
damage          # 受けるダメージ
armor           # 防御力
armorToughness  # 防具強度
health          # 現在HP
maxHealth       # 最大HP
entity.*        # エンティティ情報（entity.health, entity.maxHealth等）
math.*          # 数学関数（math.floor, math.ceil等）
min(a, b)       # 最小値
max(a, b)       # 最大値
```

### 計算式の例

```yaml
# Minecraft標準式
defaultFormula: "damage * (1 - min(20, armor) / 25)"

# パーセント軽減
defaultFormula: "damage * (1 - armor / 100)"

# 固定値軽減
defaultFormula: "max(1, damage - armor * 0.5)"

# HP依存軽減
defaultFormula: "damage * (1 - (armor / 25) * (health / maxHealth))"

# 最低ダメージ保証
defaultFormula: "max(damage * 0.3, damage * (1 - armor / 25))"

# 複雑な式（防具強度込み）
defaultFormula: "damage * (1 - (min(20, armor) + armorToughness / 10) / 25)"
```

## 将来的な拡張

カスタムリソースキーシステムにより、以下の拡張が可能になります：

1. **カスタムサウンド**: リソースパックでカスタム音を追加
2. **カスタムパーティクル**: カスタムパーティクル効果
3. **カスタムアイテム**: プラグイン独自のアイテム
4. **カスタムブロック**: プラグイン独自のブロック

現時点では、カスタムキーはminecraft:にフォールバックしますが、将来的にこれらのカスタムリソースが実際に読み込まれるようになります。

## 互換性

- **既存のYML**: すべて正常に動作します
- **従来の形式**: `SOUND_NAME` 形式も引き続きサポート
- **新機能**: `custom:key` 形式はオプトイン
- **段階的移行**: 既存の設定を変更せずに新機能を利用可能

## トラブルシューティング

### リソースが見つからない

```yaml
# ログに警告を出力
resources:
  logMissingResources: true
```

ログに以下のような警告が出力されます：
```
[WARN] Failed to resolve Sound: custom:boss_roar
```

### カスタムキーを無効化

```yaml
# 従来の動作に戻す
resources:
  enableCustomKeys: false
```

### デフォルト式の構文エラー

デフォルト計算式の構文エラーは、すべてのMobに影響します。テスト用のMobで動作確認してから設定してください。

```yaml
# テスト用Mob
TestMob:
  Type: ZOMBIE
  Health: "100"
  Armor: "10"
  # テスト中はMob個別の式を使用
  DamageFormula: "damage * (1 - armor / 100)"
```

## まとめ

- **柔軟性**: カスタムキーと従来形式の両方をサポート
- **拡張性**: 将来のリソースパック対応に備えた設計
- **互換性**: 既存のYMLはそのまま動作
- **簡潔性**: デフォルト計算式でYMLを簡潔に保つ
