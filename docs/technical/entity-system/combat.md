# PacketMobCombat - 戦闘システム

## 概要

`PacketMobCombat` は、PacketMobの戦闘処理を管理するコンポーネントクラスです。ダメージ計算、防御力処理、死亡処理、攻撃ランキングなどを実装しています。

## クラス構造

```kotlin
class PacketMobCombat(private val mob: PacketMob) {
    // ダメージ記憶
    private val damageMap = ConcurrentHashMap<UUID, Double>()

    // 最後の攻撃者
    var lastDamager: Entity? = null
        private set

    // 最後のダメージ時刻
    private var lastDamageTick: Int = 0
}
```

## 主要機能

### 1. ダメージ処理

```kotlin
suspend fun damage(amount: Double, damager: Entity?) {
    if (!mob.canTakeDamage() || mob.isDead) return

    // ダメージイベント発火
    val cause = if (damager is Player) {
        PacketMobDamageEvent.DamageCause.PLAYER_ATTACK
    } else {
        PacketMobDamageEvent.DamageCause.ENTITY_ATTACK
    }

    val event = PacketMobDamageEvent(mob, damager, amount, cause)
    val eventResult = EventUtil.callEventOrNull(event)
    if (eventResult == null) return  // キャンセルされた

    val finalDamage = calculateDamage(eventResult.damage, damager)

    // 体力を減らす
    mob.health = (mob.health - finalDamage).coerceAtLeast(0.0)

    // ダメージ記憶
    if (damager is Player) {
        recordDamage(damager, finalDamage)
    }

    // 最後の攻撃者を記録
    lastDamager = damager
    lastDamageTick = mob.ticksLived

    // ダメージアニメーション
    if (mob.options.showDamageAnimation) {
        mob.playAnimation(EntityAnimation.TAKE_DAMAGE)
    }

    // ダメージサウンド
    if (mob.options.playHurtSound && !mob.options.silent) {
        mob.playHurtSound()
    }

    // 死亡チェック
    if (mob.health <= 0.0) {
        mob.kill()
    }
}
```

### 2. ダメージ計算

```kotlin
private fun calculateDamage(baseDamage: Double, damager: Entity?): Double {
    // 防御力による軽減
    val armor = mob.armor.coerceIn(0.0, 30.0)
    val armorToughness = mob.armorToughness.coerceIn(0.0, 20.0)

    // Minecraft の防御力計算式
    val damageReduction = min(20.0,
        (armor - baseDamage / (2.0 + armorToughness / 4.0))
    ) / 25.0

    val reducedDamage = baseDamage * (1.0 - damageReduction)

    return reducedDamage.coerceAtLeast(0.0)
}
```

**防御力計算式**:
```
軽減率 = min(20, armor - damage / (2 + armorToughness / 4)) / 25
最終ダメージ = 基本ダメージ × (1 - 軽減率)
```

### 3. 死亡処理

```kotlin
suspend fun handleDeath(killer: Player?) {
    if (mob.isDead) return

    mob.isDead = true
    mob.health = 0.0
    mob.deathTick = mob.ticksLived

    // 死亡イベント発火
    val drops = mutableListOf<ItemStack>()
    val deathEvent = PacketMobDeathEvent(mob, killer, drops)
    EventUtil.callEvent(deathEvent)

    // ドロップ処理
    if (killer != null) {
        val mobInstance = Unique.instance.mobManager.getMobInstance(mob)
        mobInstance?.let { instance ->
            Unique.instance.mobManager.processDrops(
                instance.definition,
                mob.location,
                killer,
                deathEvent.drops
            )
        }
    }

    // OnDeathスキル実行
    executeDeathSkills()

    // 死亡サウンド
    if (!mob.options.silent) {
        mob.playDeathSound()
    }

    // MobInstance削除
    Unique.instance.mobManager.removeMobInstance(mob.uuid)
}
```

### 4. ダメージランキング

```kotlin
private fun recordDamage(player: Player, damage: Double) {
    damageMap.compute(player.uniqueId) { _, current ->
        (current ?: 0.0) + damage
    }
}

fun getDamageRanking(limit: Int = 10): List<Pair<UUID, Double>> {
    return damageMap.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { it.key to it.value }
}

fun getPlayerDamage(player: Player): Double {
    return damageMap[player.uniqueId] ?: 0.0
}

fun getPlayerRank(player: Player): Int {
    val ranking = getDamageRanking(Int.MAX_VALUE)
    val index = ranking.indexOfFirst { it.first == player.uniqueId }
    return if (index >= 0) index + 1 else -1
}

fun clearDamageRanking() {
    damageMap.clear()
}

fun broadcastDamageRanking(limit: Int = 10) {
    val ranking = getDamageRanking(limit)
    val message = buildRankingMessage(ranking)

    // 範囲64ブロック以内のプレイヤーに送信
    mob.location.world?.getNearbyEntities(
        mob.location, 64.0, 64.0, 64.0
    )?.filterIsInstance<Player>()
    ?.forEach { player ->
        player.sendMessage(message)
    }
}
```

### 5. ダメージ記憶のクリア

```kotlin
fun shouldClearDamageMemory(currentTick: Int, memoryTicks: Int): Boolean {
    return currentTick - lastDamageTick > memoryTicks
}

fun clearDamageMemory() {
    lastDamager = null
    lastDamageTick = 0
}
```

## 戦闘プロパティ

### 防御力

```yaml
Armor: 15.0              # 防御力（0-30）
ArmorToughness: 5.0      # 防御力強度（0-20）
```

**防御力の効果**:
- Armor 0: ダメージ軽減なし
- Armor 10: 約28%軽減
- Armor 20: 約80%軽減
- Armor 30: 最大軽減

**防御力強度**:
- 高ダメージ攻撃に対する追加耐性
- 0: 通常
- 10: 中程度の耐性
- 20: 高い耐性

### ダメージ設定

```yaml
Options:
  canTakeDamage: true      # ダメージを受けるか
  invincible: false        # 無敵フラグ
  showDamageAnimation: true # ダメージアニメーション表示
```

### サウンド設定

```yaml
Options:
  silent: false            # すべてのサウンドを無効化
  playHurtSound: true      # ダメージサウンド再生
```

## イベント

### PacketMobDamageEvent

```kotlin
@EventHandler
fun onMobDamage(event: PacketMobDamageEvent) {
    val mob = event.mob
    var damage = event.damage
    val damager = event.damager

    // ダメージ量を変更
    event.damage = damage * 1.5

    // ダメージをキャンセル
    if (someCondition) {
        event.isCancelled = true
    }
}
```

### PacketMobDeathEvent

```kotlin
@EventHandler
fun onMobDeath(event: PacketMobDeathEvent) {
    val mob = event.mob
    val killer = event.killer
    val drops = event.drops

    // ドロップアイテムを追加
    drops.add(ItemStack(Material.DIAMOND, 5))

    // ドロップをクリア
    drops.clear()
}
```

## トラブルシューティング

### Mobがダメージを受けない

**原因**:
- `canTakeDamage: false`
- `invincible: true`
- イベントでキャンセルされている

**解決方法**:
```yaml
Options:
  canTakeDamage: true
  invincible: false
```

### 防御力が効いていない

**原因**:
- Armorが0に設定されている
- ダメージ計算式が正しくない

**解決方法**:
```yaml
Armor: 15.0
ArmorToughness: 5.0
```

---

**関連ドキュメント**:
- [PacketMob](packet-mob.md) - Mob本体の実装
- [AI System](ai-system.md) - AI処理
- [Physics System](physics.md) - ノックバック処理
