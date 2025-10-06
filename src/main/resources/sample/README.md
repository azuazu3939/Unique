# Sample フォルダー

このフォルダーには、Uniqueプラグインの完全な実装例が含まれています。

## ファイル一覧

### YAML サンプル
- **complete_example.yml** - 完全な実装例（スキル+Mob+スポーン）
  - スキル定義、Mob定義、スポーン定義をすべて含む
  - 実際の使用時は各定義を適切なフォルダーに分けて配置

### Kotlin サンプル
- **reload_event_example.kt** - リロードイベントのサンプルコード
  - プラグイン拡張のためのコード例

## フォルダー構造

プラグインの設定ファイルは以下のように配置してください：

```
plugins/Unique/
├── config.yml          # プラグイン設定
├── mobs/               # Mob定義専用
│   ├── basic_mobs.yml
│   └── boss_mobs.yml
├── skills/             # スキル定義専用
│   ├── projectile_skills.yml
│   ├── combat_skills.yml
│   └── aura_beam_skills.yml
├── spawns/             # スポーン定義専用
│   └── world_spawns.yml
├── targeters/          # ターゲッター定義（カスタム）
│   └── custom_targeters.yml
├── triggers/           # トリガー定義（カスタム）
│   └── custom_triggers.yml
├── conditions/         # 条件定義
│   └── custom_conditions.yml
└── effects/            # エフェクト定義
    └── custom_effects.yml
```

## 超コンパクト構文

Mob定義では超コンパクト構文を使用します：

```yaml
Skills:
  # スキル定義 @ターゲッター ~トリガー
  - ExampleFireball @NP{r=50} ~onTimer:40
  - damage{amount=35} @TL ~onAttack
```

詳細は [GUIDE.md](../../../docs/GUIDE.md) を参照してください。

## クイックスタート

1. `complete_example.yml` をコピー
2. スキル定義を `skills/` に移動
3. Mob定義を `mobs/` に移動
4. スポーン定義を `spawns/` に移動
5. `/unique reload` でリロード
6. `/unique spawn ExampleBoss` でテスト

## サポート

詳しくは以下のドキュメントを参照：
- [クイックスタート](../../../docs/QUICKSTART.md)
- [完全ガイド](../../../docs/GUIDE.md)
- [リファレンス](../../../docs/REFERENCE.md)
