# SkillExecutor - スキル実行エンジン

## 概要

`SkillExecutor` は、スキルの実行を管理するエンジンクラスです。非同期実行、遅延処理、キャンセル処理などを実装しています。

## クラス構造

```kotlin
class SkillExecutor(private val plugin: Unique) {
    // アクティブなスキル実行ジョブ
    private val activeSkillJobs = ConcurrentHashMap<UUID, Job>()
}
```

## 主要機能

### 1. スキル実行

```kotlin
suspend fun executeSkill(
    skill: Skill,
    source: Entity,
    targeter: Targeter
) {
    val jobId = UUID.randomUUID()

    val job = plugin.launch {
        try {
            // 実行遅延
            val executeDelay = skill.meta.executeDelay
            if (executeDelay.inWholeMilliseconds > 0) {
                delay(executeDelay.inWholeMilliseconds)
            }

            // スキル実行
            skill.execute(plugin, source, targeter)

        } catch (e: CancellationException) {
            DebugLogger.debug("Skill execution cancelled: ${skill.id}")
        } catch (e: Exception) {
            DebugLogger.error("Failed to execute skill: ${skill.id}", e)
        } finally {
            activeSkillJobs.remove(jobId)
        }
    }

    activeSkillJobs[jobId] = job
}
```

### 2. PacketEntityからの実行

```kotlin
suspend fun executeSkill(
    skill: Skill,
    source: PacketEntity,
    targeter: Targeter
) {
    val jobId = UUID.randomUUID()

    val job = plugin.launch {
        try {
            val executeDelay = skill.meta.executeDelay
            if (executeDelay.inWholeMilliseconds > 0) {
                delay(executeDelay.inWholeMilliseconds)
            }

            skill.execute(plugin, source, targeter)

        } catch (e: CancellationException) {
            DebugLogger.debug("Skill execution cancelled (PacketEntity): ${skill.id}")
        } catch (e: Exception) {
            DebugLogger.error("Failed to execute skill (PacketEntity): ${skill.id}", e)
        } finally {
            activeSkillJobs.remove(jobId)
        }
    }

    activeSkillJobs[jobId] = job
}
```

### 3. スキルキャンセル

```kotlin
suspend fun cancelAllSkills() {
    DebugLogger.info("Cancelling ${activeSkillJobs.size} active skill jobs...")

    activeSkillJobs.values.forEach { job ->
        job.cancel()
    }

    activeSkillJobs.clear()
}
```

## スキル実行フロー

```
1. executeSkill() 呼び出し
   └─> Coroutine 起動

2. 実行遅延 (executeDelay)
   └─> delay() で待機

3. スキル実行
   └─> skill.execute()
       ├─> ターゲット取得
       │   └─> targeter.getTargets()
       │
       ├─> エフェクト遅延 (effectDelay)
       │   └─> delay() で待機
       │
       └─> エフェクト適用
           └─> effect.apply() for each target

4. クリーンアップ
   └─> activeSkillJobs から削除
```

## SkillMeta

```kotlin
data class SkillMeta(
    val sync: Boolean = false,           // 同期実行するか
    val executeDelay: Duration = 0.ms,   // 実行遅延
    val effectDelay: Duration = 0.ms,    // エフェクト遅延
    val cancelOnDeath: Boolean = true,   // 死亡時にキャンセル
    val interruptible: Boolean = true    // 中断可能
)
```

**YAML設定例**:
```yaml
Skills:
  - "projectile{...} @NP{r=30.0} ~onTimer:30t{executeDelay=500ms, effectDelay=100ms}"
```

## 遅延処理

### executeDelay

スキル発動までの遅延:

```yaml
executeDelay: 500ms  # 0.5秒後に発動
```

**使用例**:
- 詠唱時間の実装
- タイミングを合わせた連携攻撃
- ボスのフェーズ移行

### effectDelay

エフェクト適用までの遅延:

```yaml
effectDelay: 100ms  # 0.1秒後にエフェクト適用
```

**使用例**:
- 発射体の着弾タイミング調整
- ビームの充填エフェクト
- アニメーションとの同期

## 同期実行

```yaml
sync: true  # メインスレッドで実行
```

**使用場面**:
- ブロック操作
- エンティティ生成
- ワールド操作

**デフォルト**: `false`（非同期実行）

## キャンセル処理

### 死亡時キャンセル

```yaml
cancelOnDeath: true  # ソースが死亡したらキャンセル
```

**動作**:
- スキル実行中にソースが死亡した場合、スキルをキャンセル
- エフェクト遅延中も含む

### 中断可能性

```yaml
interruptible: true  # 中断可能
```

**動作**:
- 新しいスキルが発動した場合、実行中のスキルを中断可能
- `false` の場合は実行完了まで中断不可

## パフォーマンス最適化

### コルーチンプール

```kotlin
// 地域別実行（Folia対応）
plugin.launch(plugin.regionDispatcher(location)) {
    // 特定地域での処理
}

// グローバル実行
plugin.launch(plugin.globalRegionDispatcher) {
    // ワールド横断処理
}
```

### 実行ジョブ管理

```kotlin
// アクティブなジョブ数を制限
if (activeSkillJobs.size > 1000) {
    DebugLogger.warn("Too many active skill jobs: ${activeSkillJobs.size}")
}
```

## トラブルシューティング

### スキルが実行されない

**原因**:
- `executeDelay` が長すぎる
- コルーチンが例外で停止
- ターゲットが見つからない

**解決方法**:
デバッグログを有効化:
```yaml
debug:
  enabled: true
  verbose: true
```

### スキルが遅れて発動する

**原因**:
- サーバー負荷が高い
- `executeDelay` が設定されている

**解決方法**:
```yaml
executeDelay: 0ms  # 遅延なし
```

---

**関連ドキュメント**:
- [Skill Types](skill-types.md) - スキルタイプの詳細
- [Effect System](../effect-system/effect-overview.md) - エフェクトシステム
- [Targeter System](../targeter-system/targeter-overview.md) - ターゲッターシステム
