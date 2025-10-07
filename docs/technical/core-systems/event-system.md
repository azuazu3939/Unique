# Event System

## 概要

Event System は、Unique プラグインにおける PacketMob のライフサイクルイベントを管理するシステムです。Bukkit の Event API を拡張し、PacketMob 固有のカスタムイベントを提供します。

### 主な目的

- **ライフサイクル管理**: PacketMob のスポーン、ダメージ、死亡などの重要なイベントを通知
- **拡張性**: 他のプラグインやシステムが PacketMob の動作に介入できる仕組みを提供
- **キャンセル機能**: 特定のイベントをキャンセルして動作を変更
- **イベント駆動設計**: 疎結合なシステム設計を実現

### イベントの種類

1. **PacketMobSpawnEvent**: PacketMob がスポーンされた時に発火（キャンセル可能）
2. **PacketMobDamageEvent**: PacketMob がダメージを受けた時に発火（キャンセル可能）
3. **PacketMobDeathEvent**: PacketMob が死亡した時に発火（キャンセル不可）
4. **PacketMobAttackEvent**: PacketMob が攻撃した時に発火（キャンセル可能）
5. **PacketMobTargetEvent**: PacketMob のターゲットが変更された時に発火（キャンセル可能）
6. **PacketMobSkillEvent**: PacketMob がスキルを使用した時に発火（キャンセル可能）
7. **PacketMobRemoveEvent**: PacketMob が削除される時に発火（キャンセル不可）
8. **PacketMobKillPlayerEvent**: PacketMob がプレイヤーを殺した時に発火（キャンセル可能）

## アーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                    PacketMob アクション                      │
│  spawn(), damage(), attack(), setTarget(), useSkill(), etc. │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  PacketMobEvent 発火                         │
│         Bukkit.getPluginManager().callEvent(event)          │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
         ┌────────────────┴────────────────┐
         │                                  │
         ▼                                  ▼
┌──────────────────────┐         ┌───────────────────────┐
│  イベントリスナー     │         │  イベントリスナー      │
│  (Unique内部)        │         │  (他のプラグイン)      │
├──────────────────────┤         ├───────────────────────┤
│ • SkillExecutor      │         │ • カスタムロジック     │
│ • MobManager         │         │ • 統計記録             │
│ • CombatSystem       │         │ • アナウンス           │
└──────────┬───────────┘         └───────────┬───────────┘
           │                                  │
           ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                  イベント処理結果                            │
│  • イベントがキャンセルされた場合、アクションを中止          │
│  • イベント内でプロパティが変更された場合、変更を反映        │
│  • カスタム処理の実行                                        │
└─────────────────────────────────────────────────────────────┘
```

## 実装詳細

### PacketMobEvent 基底クラス

すべての PacketMob イベントの基底クラス。

```kotlin
/**
 * PacketMob関連イベントの基底クラス
 */
abstract class PacketMobEvent(
    val mob: PacketMob,
    isAsync: Boolean = false
) : Event(isAsync) {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

**設計のポイント:**

1. **mob プロパティ**: すべてのイベントで対象の PacketMob にアクセス可能
2. **非同期対応**: isAsync パラメータで非同期イベントをサポート
3. **HandlerList**: Bukkit の標準的なイベントハンドラー管理
4. **抽象クラス**: 共通機能を提供しつつ、具体的なイベントは継承して実装

### PacketMobSpawnEvent

PacketMob がスポーンされた時に発火するイベント。

```kotlin
/**
 * PacketMobスポーンイベント
 *
 * PacketMobがスポーンされた時に発火
 * キャンセル可能（スポーン阻止）
 */
class PacketMobSpawnEvent(
    mob: PacketMob,
    val location: Location,
    val mobName: String
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

**使用例:**

```kotlin
// スポーン処理内でイベントを発火
val event = PacketMobSpawnEvent(mob, location, mobName)
Bukkit.getPluginManager().callEvent(event)

if (event.isCancelled) {
    // スポーンがキャンセルされた
    mob.remove()
    return null
}

// 通常のスポーン処理を続行
```

### PacketMobDamageEvent

PacketMob がダメージを受けた時に発火するイベント。

```kotlin
/**
 * PacketMobダメージイベント
 *
 * PacketMobがダメージを受けた時に発火
 * キャンセル可能（ダメージ無効化）
 */
class PacketMobDamageEvent(
    mob: PacketMob,
    val damager: Entity?,
    var damage: Double,
    val cause: DamageCause = DamageCause.ENTITY_ATTACK
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * ダメージ原因
     */
    enum class DamageCause {
        ENTITY_ATTACK,      // エンティティ攻撃
        PLAYER_ATTACK,      // プレイヤー攻撃
        SKILL,              // スキル
        ENVIRONMENT,        // 環境（溶岩、落下等）
        CUSTOM              // カスタム
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

**使用例:**

```kotlin
// ダメージ処理内でイベントを発火
val event = PacketMobDamageEvent(
    mob = this,
    damager = attacker,
    damage = finalDamage,
    cause = PacketMobDamageEvent.DamageCause.PLAYER_ATTACK
)
Bukkit.getPluginManager().callEvent(event)

if (event.isCancelled) {
    // ダメージがキャンセルされた
    return
}

// イベントで変更されたダメージ値を使用
val actualDamage = event.damage
health -= actualDamage
```

**重要な機能:**

- **damage プロパティの変更**: リスナーでダメージ量を変更可能（var 修飾子）
- **DamageCause enum**: ダメージの原因を詳細に分類

### PacketMobDeathEvent

PacketMob が死亡した時に発火するイベント。

```kotlin
/**
 * PacketMob死亡イベント
 *
 * PacketMobが死亡した時に発火
 * キャンセル不可（死亡を防げない）
 */
class PacketMobDeathEvent(
    mob: PacketMob,
    val killer: Player?,
    val drops: MutableList<ItemStack> = mutableListOf()
) : PacketMobEvent(mob) {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

**使用例:**

```kotlin
// 死亡処理内でイベントを発火
val drops = mutableListOf<ItemStack>()
// ドロップアイテムを計算...

val event = PacketMobDeathEvent(this, killer, drops)
Bukkit.getPluginManager().callEvent(event)

// イベント内で変更されたドロップリストを使用
event.drops.forEach { item ->
    world.dropItemNaturally(location, item)
}
```

**重要な機能:**

- **drops リスト**: リスナーでドロップアイテムを追加・削除可能
- **キャンセル不可**: 死亡は確定しており、キャンセルできない

### PacketMobAttackEvent

PacketMob が攻撃した時に発火するイベント。

```kotlin
/**
 * PacketMob攻撃イベント
 *
 * PacketMobが攻撃した時に発火
 * キャンセル可能（攻撃阻止）
 */
class PacketMobAttackEvent(
    mob: PacketMob,
    val target: Entity,
    var damage: Double
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

**使用例:**

```kotlin
// 攻撃処理内でイベントを発火
val event = PacketMobAttackEvent(
    mob = this,
    target = target,
    damage = attackDamage
)
Bukkit.getPluginManager().callEvent(event)

if (event.isCancelled) {
    // 攻撃がキャンセルされた
    return
}

// イベントで変更されたダメージ値を使用
val actualDamage = event.damage
(target as? LivingEntity)?.damage(actualDamage, this.entity)
```

### PacketMobTargetEvent

PacketMob のターゲットが変更された時に発火するイベント。

```kotlin
/**
 * PacketMobターゲット変更イベント
 *
 * PacketMobのターゲットが変更された時に発火
 * キャンセル可能（ターゲット変更阻止）
 */
class PacketMobTargetEvent(
    mob: PacketMob,
    val oldTarget: Entity?,
    var newTarget: Entity?,
    val reason: TargetReason = TargetReason.CLOSEST_PLAYER
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * ターゲット変更理由
     */
    enum class TargetReason {
        CLOSEST_PLAYER,     // 最も近いプレイヤー
        ATTACKED,           // 攻撃された
        FORGOT,             // ターゲットを忘れた
        CUSTOM              // カスタム
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

**使用例:**

```kotlin
// ターゲット変更処理内でイベントを発火
val event = PacketMobTargetEvent(
    mob = this,
    oldTarget = currentTarget,
    newTarget = newTarget,
    reason = PacketMobTargetEvent.TargetReason.ATTACKED
)
Bukkit.getPluginManager().callEvent(event)

if (event.isCancelled) {
    // ターゲット変更がキャンセルされた
    return
}

// イベントで変更されたターゲットを使用
currentTarget = event.newTarget
```

**重要な機能:**

- **newTarget の変更**: リスナーで別のエンティティをターゲットに設定可能
- **TargetReason enum**: ターゲット変更の理由を詳細に分類

### PacketMobSkillEvent

PacketMob がスキルを使用した時に発火するイベント。

```kotlin
/**
 * PacketMobスキル使用イベント
 *
 * PacketMobがスキルを使用した時に発火
 * キャンセル可能（スキル使用阻止）
 */
class PacketMobSkillEvent(
    mob: PacketMob,
    val skillName: String,
    val trigger: SkillTriggerType
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /**
     * スキルトリガータイプ
     */
    enum class SkillTriggerType {
        ON_SPAWN,
        ON_ATTACK,
        ON_DAMAGED,
        ON_DEATH,
        ON_TIMER
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

**使用例:**

```kotlin
// スキル実行前にイベントを発火
val event = PacketMobSkillEvent(
    mob = mob,
    skillName = skillName,
    trigger = PacketMobSkillEvent.SkillTriggerType.ON_ATTACK
)
Bukkit.getPluginManager().callEvent(event)

if (event.isCancelled) {
    // スキル使用がキャンセルされた
    return
}

// スキルを実行
executeSkill(skillName)
```

### PacketMobRemoveEvent

PacketMob が削除される時に発火するイベント。

```kotlin
/**
 * PacketMob削除イベント
 *
 * PacketMobが削除される時に発火
 * キャンセル不可
 */
class PacketMobRemoveEvent(
    mob: PacketMob,
    val reason: RemoveReason = RemoveReason.DEATH
) : PacketMobEvent(mob) {

    /**
     * 削除理由
     */
    enum class RemoveReason {
        DEATH,          // 死亡
        DESPAWN,        // デスポーン
        UNLOAD,         // チャンクアンロード
        PLUGIN          // プラグイン指定
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

### PacketMobKillPlayerEvent

PacketMob がプレイヤーを殺した時に発火するイベント。

```kotlin
/**
 * PacketMobプレイヤーキルイベント
 *
 * PacketMobがプレイヤーを殺した時に発火
 * キャンセル可能（キラー設定を阻止）
 */
class PacketMobKillPlayerEvent(
    mob: PacketMob,
    val player: Player,
    var setKiller: Boolean = true
) : PacketMobEvent(mob), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
```

## 使用例

### イベントリスナーの登録

```kotlin
class CustomMobListener : Listener {

    @EventHandler
    fun onMobSpawn(event: PacketMobSpawnEvent) {
        // 特定のバイオームでのスポーンを阻止
        if (event.location.block.biome == Biome.DESERT) {
            event.isCancelled = true
            return
        }

        // スポーン時のアナウンス
        event.location.world.players.forEach { player ->
            player.sendMessage("${event.mobName} がスポーンしました！")
        }
    }

    @EventHandler
    fun onMobDamage(event: PacketMobDamageEvent) {
        // 特定のプレイヤーからのダメージを無効化
        val damager = event.damager
        if (damager is Player && damager.hasPermission("unique.admin")) {
            event.isCancelled = true
            damager.sendMessage("管理者モードのためダメージは無効化されました")
            return
        }

        // ダメージを1.5倍に増加
        if (event.cause == PacketMobDamageEvent.DamageCause.SKILL) {
            event.damage *= 1.5
        }
    }

    @EventHandler
    fun onMobDeath(event: PacketMobDeathEvent) {
        val mob = event.mob
        val killer = event.killer

        // 統計を記録
        if (killer != null) {
            // プレイヤーのキル数を増やす
            PlayerDataManager.addKillCount(killer, mob.definition.name)
        }

        // 追加ドロップを追加
        if (killer?.hasPermission("unique.bonus_drops") == true) {
            event.drops.add(ItemStack(Material.DIAMOND, 1))
        }

        // 死亡メッセージ
        mob.location.world.players.forEach { player ->
            player.sendMessage("${mob.definition.displayName} が倒されました！")
        }
    }

    @EventHandler
    fun onMobAttack(event: PacketMobAttackEvent) {
        // 特定のエンティティへの攻撃を阻止
        if (event.target.scoreboardTags.contains("protected")) {
            event.isCancelled = true
            return
        }

        // クリティカルヒット判定
        if (Random.nextDouble() < 0.1) { // 10%の確率
            event.damage *= 2.0
            event.target.world.playSound(
                event.target.location,
                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                1.0f,
                1.0f
            )
        }
    }

    @EventHandler
    fun onMobTarget(event: PacketMobTargetEvent) {
        // 管理者をターゲットにしない
        val newTarget = event.newTarget
        if (newTarget is Player && newTarget.hasPermission("unique.admin")) {
            event.isCancelled = true
            return
        }

        // ターゲット変更時のログ
        DebugLogger.debug("${event.mob.definition.name} のターゲットが変更されました: ${event.oldTarget?.name} -> ${event.newTarget?.name}")
    }

    @EventHandler
    fun onMobSkill(event: PacketMobSkillEvent) {
        // 特定のスキルを阻止
        if (event.skillName == "ultimate" && event.mob.health > event.mob.maxHealth * 0.5) {
            event.isCancelled = true
            return
        }

        // スキル使用時のエフェクト
        event.mob.location.world.spawnParticle(
            Particle.SPELL_WITCH,
            event.mob.location,
            50,
            0.5, 0.5, 0.5,
            0.1
        )
    }

    @EventHandler
    fun onMobRemove(event: PacketMobRemoveEvent) {
        // 削除理由に応じた処理
        when (event.reason) {
            PacketMobRemoveEvent.RemoveReason.DEATH -> {
                // 死亡エフェクト
                event.mob.location.world.spawnParticle(
                    Particle.EXPLOSION_LARGE,
                    event.mob.location,
                    1
                )
            }
            PacketMobRemoveEvent.RemoveReason.DESPAWN -> {
                // デスポーンログ
                DebugLogger.debug("${event.mob.definition.name} がデスポーンしました")
            }
            else -> {}
        }
    }

    @EventHandler
    fun onMobKillPlayer(event: PacketMobKillPlayerEvent) {
        val mob = event.mob
        val player = event.player

        // カスタム死亡メッセージ
        Bukkit.broadcastMessage("${player.name} は ${mob.definition.displayName} に倒されました")

        // 実績の付与
        if (mob.definition.name == "boss_dragon") {
            // "ドラゴンに敗北した" 実績
        }
    }
}

// リスナーの登録
Bukkit.getPluginManager().registerEvents(CustomMobListener(), plugin)
```

### プログラムからのイベント発火

```kotlin
// カスタムダメージ処理
fun customDamage(mob: PacketMob, damage: Double) {
    val event = PacketMobDamageEvent(
        mob = mob,
        damager = null,
        damage = damage,
        cause = PacketMobDamageEvent.DamageCause.CUSTOM
    )

    Bukkit.getPluginManager().callEvent(event)

    if (!event.isCancelled) {
        mob.health -= event.damage
    }
}

// カスタムスキル実行
fun executeCustomSkill(mob: PacketMob, skillName: String) {
    val event = PacketMobSkillEvent(
        mob = mob,
        skillName = skillName,
        trigger = PacketMobSkillEvent.SkillTriggerType.CUSTOM
    )

    Bukkit.getPluginManager().callEvent(event)

    if (!event.isCancelled) {
        // スキル実行処理
        SkillExecutor.execute(skillName, mob)
    }
}
```

### 優先度の設定

```kotlin
@EventHandler(priority = EventPriority.HIGHEST)
fun onMobDamage(event: PacketMobDamageEvent) {
    // 他のリスナーの後に実行される
}

@EventHandler(priority = EventPriority.LOWEST)
fun onMobSpawn(event: PacketMobSpawnEvent) {
    // 他のリスナーより先に実行される
}

@EventHandler(ignoreCancelled = true)
fun onMobAttack(event: PacketMobAttackEvent) {
    // キャンセル済みのイベントは無視される
}
```

## パフォーマンス最適化

### 非同期イベント

重い処理を伴うイベントは非同期で発火します。

```kotlin
// 非同期イベントの発火
val event = PacketMobDeathEvent(mob, killer, drops)
Bukkit.getScheduler().runTaskAsynchronously(plugin) { _ ->
    Bukkit.getPluginManager().callEvent(event)
}
```

### イベントのフィルタリング

不要なイベント処理を早期にスキップします。

```kotlin
@EventHandler
fun onMobDamage(event: PacketMobDamageEvent) {
    // 特定のモブタイプのみ処理
    if (event.mob.definition.name != "boss_mob") {
        return
    }

    // 処理...
}
```

### バッチ処理

複数のイベントをまとめて処理します。

```kotlin
class BatchEventProcessor : Listener {
    private val damageEvents = mutableListOf<PacketMobDamageEvent>()

    @EventHandler
    fun onMobDamage(event: PacketMobDamageEvent) {
        // イベントをキューに追加
        damageEvents.add(event)
    }

    fun processBatch() {
        // 一括処理
        damageEvents.forEach { event ->
            // 統計更新など
        }
        damageEvents.clear()
    }
}

// 定期的にバッチ処理を実行
Bukkit.getScheduler().runTaskTimer(plugin, { _ ->
    processor.processBatch()
}, 0L, 20L) // 1秒ごと
```

## ベストプラクティス

### 1. イベントのキャンセルは慎重に

他のプラグインの動作に影響を与える可能性があります。

```kotlin
@EventHandler
fun onMobSpawn(event: PacketMobSpawnEvent) {
    // 条件を明確にする
    if (shouldCancelSpawn(event.location, event.mobName)) {
        event.isCancelled = true
        // キャンセルした理由をログに記録
        DebugLogger.debug("Spawn cancelled: ${event.mobName} at ${event.location}")
    }
}
```

### 2. 変更可能なプロパティの適切な使用

damage や newTarget などの var プロパティは慎重に変更します。

```kotlin
@EventHandler
fun onMobDamage(event: PacketMobDamageEvent) {
    // 元の値を保存
    val originalDamage = event.damage

    // 変更
    event.damage *= 1.5

    // ログ出力
    DebugLogger.debug("Damage modified: $originalDamage -> ${event.damage}")
}
```

### 3. イベントの優先度を適切に設定

```kotlin
// データ収集は最初に実行
@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
fun onMobDeath(event: PacketMobDeathEvent) {
    // 統計記録（キャンセルの影響を受けない）
}

// ゲームロジックは通常の優先度
@EventHandler
fun onMobDamage(event: PacketMobDamageEvent) {
    // ダメージ計算の変更
}

// 最終的な処理は最後に実行
@EventHandler(priority = EventPriority.HIGHEST)
fun onMobAttack(event: PacketMobAttackEvent) {
    // 最終的なダメージ調整
}
```

### 4. エラーハンドリング

リスナー内でエラーが発生してもイベントシステム全体に影響しないようにします。

```kotlin
@EventHandler
fun onMobSkill(event: PacketMobSkillEvent) {
    try {
        // 処理...
    } catch (e: Exception) {
        DebugLogger.error("Error in skill event listener", e)
        // イベントはキャンセルしない（他のリスナーに影響させない）
    }
}
```

## トラブルシューティング

### イベントが発火しない

**問題**: イベントリスナーが呼ばれない

**解決策**:
1. リスナーが正しく登録されているか確認
2. EventHandler アノテーションが付いているか確認
3. メソッドのシグネチャが正しいか確認（パラメータは1つのみ）

```kotlin
// 正しい例
@EventHandler
fun onMobSpawn(event: PacketMobSpawnEvent) { ... }

// 間違った例（アノテーションなし）
fun onMobSpawn(event: PacketMobSpawnEvent) { ... }

// 間違った例（パラメータが多い）
@EventHandler
fun onMobSpawn(event: PacketMobSpawnEvent, mob: PacketMob) { ... }
```

### イベントの順序が期待通りでない

**問題**: イベントリスナーの実行順序が予期しない

**解決策**: EventPriority を明示的に設定

```kotlin
@EventHandler(priority = EventPriority.LOWEST)  // 最初に実行
fun earlyListener(event: PacketMobDamageEvent) { ... }

@EventHandler(priority = EventPriority.HIGHEST) // 最後に実行
fun lateListener(event: PacketMobDamageEvent) { ... }
```

### キャンセルしたイベントが実行される

**問題**: イベントをキャンセルしたのに処理が実行される

**解決策**: イベント発火側でキャンセルフラグをチェック

```kotlin
// イベント発火側
val event = PacketMobAttackEvent(this, target, damage)
Bukkit.getPluginManager().callEvent(event)

// キャンセルされたかチェック
if (event.isCancelled) {
    return // 処理を中止
}

// 通常の処理を続行
```

## 関連ドキュメント

- [Packet Mob](../entity-system/packet-mob.md) - PacketMob の実装詳細
- [Mob Manager](../mob-system/mob-manager.md) - MobManager でのイベント使用
- [Skill Executor](../skill-system/skill-executor.md) - スキル実行時のイベント発火
- [Combat System](../entity-system/combat.md) - 戦闘システムでのイベント使用

## まとめ

Event System は Unique プラグインの拡張性を支える重要なコンポーネントです。以下の特徴を持ちます:

1. **Bukkit 標準**: Bukkit の Event API に準拠した設計
2. **キャンセル機能**: 重要なイベントはキャンセル可能
3. **詳細な情報**: 各イベントは関連する詳細情報を提供
4. **変更可能なプロパティ**: damage や newTarget などを動的に変更可能
5. **拡張性**: 他のプラグインからも簡単にリスナーを登録可能

Event System を適切に活用することで、PacketMob の動作を柔軟にカスタマイズし、他のシステムとの統合を実現できます。
