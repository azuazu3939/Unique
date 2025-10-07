package com.github.azuazu3939.unique.skill.types

import com.github.azuazu3939.unique.Unique
import com.github.azuazu3939.unique.cel.CELEvaluator
import com.github.azuazu3939.unique.cel.CELVariableProvider
import com.github.azuazu3939.unique.condition.Condition
import com.github.azuazu3939.unique.effect.Effect
import com.github.azuazu3939.unique.entity.PacketEntity
import com.github.azuazu3939.unique.skill.Skill
import com.github.azuazu3939.unique.skill.SkillMeta
import com.github.azuazu3939.unique.targeter.Targeter
import com.github.azuazu3939.unique.util.DebugLogger
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.*
import org.bukkit.plugin.Plugin
import org.bukkit.projectiles.ProjectileSource
import org.bukkit.util.Vector

/**
 * ProjectileSkill - 発射体スキル
 *
 * ターゲットに向けて発射体を発射し、命中時にエフェクトを適用します。
 * 矢、ファイアボール、カスタムパーティクルなど様々な発射体タイプをサポート。
 *
 * @param id スキルID
 * @param meta スキルメタ設定
 * @param condition 実行条件
 * @param projectileType 発射体タイプ
 * @param speed 速度（CEL式対応、ブロック/tick）
 * @param gravity 重力の影響を受けるか
 * @param hitEffects 命中時のエフェクト
 * @param tickEffects 飛行中のエフェクト（毎tick）
 * @param maxDistance 最大飛行距離（CEL式対応）
 * @param hitRadius 命中判定半径（CEL式対応）
 * @param pierce 貫通するか（falseの場合は最初の命中で消滅）
 * @param homing ホーミング強度（0.0=なし、1.0=完全追尾、CEL式対応）
 * @param hitSound 命中時のサウンド
 * @param launchSound 発射時のサウンド
 */
class ProjectileSkill(
    id: String,
    meta: SkillMeta = SkillMeta(),
    condition: Condition? = null,
    private val projectileType: ProjectileType = ProjectileType.ARROW,
    private val speed: String = "2.0",
    private val gravity: Boolean = true,
    private val hitEffects: List<Effect> = emptyList(),
    private val tickEffects: List<Effect> = emptyList(),
    private val maxDistance: String = "50.0",
    private val hitRadius: String = "1.0",
    private val pierce: Boolean = false,
    private val homing: String = "0.0",
    private val hitSound: Sound? = Sound.ENTITY_ARROW_HIT,
    private val launchSound: Sound? = Sound.ENTITY_ARROW_SHOOT
) : Skill(id, meta, condition) {

    enum class ProjectileType {
        ARROW,          // 矢
        SPECTRAL_ARROW, // 光の矢
        FIREBALL,       // ファイアボール
        SMALL_FIREBALL, // 小ファイアボール
        SNOWBALL,       // 雪玉
        EGG,            // 卵
        ENDER_PEARL,    // エンダーパール
        WITHER_SKULL,   // ウィザースカル
        DRAGON_FIREBALL,// ドラゴンファイアボール
        PARTICLE_FLAME, // 炎パーティクル軌道
        PARTICLE_MAGIC, // 魔法パーティクル軌道
        PARTICLE_SOUL   // ソウルパーティクル軌道
    }

    override suspend fun execute(plugin: Plugin, source: Entity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.skill("ProjectileSkill condition not met")
            return
        }

        // 実行遅延
        delay(meta.executeDelay)

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.skill("ProjectileSkill: No targets found")
            return
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildEntityContext(source)
        val speedValue = evaluateCelDouble(speed, context, evaluator, 2.0)
        val maxDistanceValue = evaluateCelDouble(maxDistance, context, evaluator, 50.0)
        val hitRadiusValue = evaluateCelDouble(hitRadius, context, evaluator, 1.0)
        val homingValue = evaluateCelDouble(homing, context, evaluator, 0.0)

        // 各ターゲットに向けて発射
        for (target in targets) {
            launchProjectile(
                plugin,
                source,
                target,
                speedValue,
                maxDistanceValue,
                hitRadiusValue,
                homingValue
            )
        }
    }

    override suspend fun execute(plugin: Plugin, source: PacketEntity, targeter: Targeter) {
        if (!checkCondition(source)) {
            DebugLogger.skill("ProjectileSkill condition not met (PacketEntity)")
            return
        }

        // 実行遅延
        delay(meta.executeDelay)

        val targets = targeter.getTargets(source)
        if (targets.isEmpty()) {
            DebugLogger.skill("ProjectileSkill: No targets found (PacketEntity)")
            return
        }

        // CEL式を評価
        val evaluator = Unique.instance.celEvaluator
        val context = CELVariableProvider.buildPacketEntityContext(source)
        val speedValue = evaluateCelDouble(speed, context, evaluator, 2.0)
        val maxDistanceValue = evaluateCelDouble(maxDistance, context, evaluator, 50.0)
        val hitRadiusValue = evaluateCelDouble(hitRadius, context, evaluator, 1.0)
        val homingValue = evaluateCelDouble(homing, context, evaluator, 0.0)

        // 各ターゲットに向けて発射
        for (target in targets) {
            launchProjectileFromPacketEntity(
                plugin,
                source,
                target,
                speedValue,
                maxDistanceValue,
                hitRadiusValue,
                homingValue
            )
        }
    }

    /**
     * 発射体を発射（Entity source）
     */
    private suspend fun launchProjectile(
        plugin: Plugin,
        source: Entity,
        initialTarget: Entity,
        speedValue: Double,
        maxDistanceValue: Double,
        hitRadiusValue: Double,
        homingValue: Double
    ) {
        val startLocation = source.location.clone().add(0.0, 1.5, 0.0)
        val direction = initialTarget.location.toVector().subtract(startLocation.toVector()).normalize()

        // 発射サウンド
        launchSound?.let {
            startLocation.world?.playSound(startLocation, it, 1.0f, 1.0f)
        }

        // パーティクルタイプの場合は独自実装
        if (isParticleProjectile()) {
            launchParticleProjectile(
                plugin,
                source,
                startLocation,
                direction,
                initialTarget,
                speedValue,
                maxDistanceValue,
                hitRadiusValue,
                homingValue
            )
            return
        }

        // 実エンティティ発射体を発射（asyncスレッド）
        withContext(plugin.globalRegionDispatcher) {
            val projectile = spawnProjectileEntity(source, startLocation, direction, speedValue)
            if (projectile != null) {
                // 追尾処理
                if (homingValue > 0.0) {
                    trackProjectile(plugin, source, projectile, initialTarget, homingValue, hitRadiusValue)
                }
            }
        }
    }

    /**
     * 発射体を発射（PacketEntity source）
     */
    private suspend fun launchProjectileFromPacketEntity(
        plugin: Plugin,
        source: PacketEntity,
        initialTarget: Entity,
        speedValue: Double,
        maxDistanceValue: Double,
        hitRadiusValue: Double,
        homingValue: Double
    ) {
        val startLocation = source.location.clone().add(0.0, 1.5, 0.0)
        val direction = initialTarget.location.toVector().subtract(startLocation.toVector()).normalize()

        // 発射サウンド
        launchSound?.let {
            startLocation.world?.playSound(startLocation, it, 1.0f, 1.0f)
        }

        // パーティクルタイプの場合
        if (isParticleProjectile()) {
            launchParticleProjectileFromPacket(
                plugin,
                source,
                startLocation,
                direction,
                initialTarget,
                speedValue,
                maxDistanceValue,
                hitRadiusValue,
                homingValue
            )
            return
        }

        // 実エンティティ発射体（PacketEntityからはSourceなしで発射、asyncスレッド）
        withContext(plugin.globalRegionDispatcher) {
            val world = startLocation.world ?: return@withContext
            val projectile = world.spawn(startLocation, getProjectileClass())
            projectile.velocity = direction.multiply(speedValue)

            if (homingValue > 0.0) {
                trackProjectileFromPacket(plugin, source, projectile, initialTarget, homingValue, hitRadiusValue)
            }
        }
    }

    /**
     * パーティクル発射体を発射（Entity source）
     */
    private suspend fun launchParticleProjectile(
        plugin: Plugin,
        source: Entity,
        startLocation: Location,
        initialDirection: Vector,
        initialTarget: Entity,
        speedValue: Double,
        maxDistanceValue: Double,
        hitRadiusValue: Double,
        homingValue: Double
    ) {
        launchParticleProjectileCommon(
            plugin,
            startLocation,
            initialDirection,
            initialTarget,
            speedValue,
            maxDistanceValue,
            hitRadiusValue,
            homingValue,
            sourceEntity = source,
            sourcePacket = null
        )
    }

    /**
     * パーティクル発射体を発射（PacketEntity source）
     */
    private suspend fun launchParticleProjectileFromPacket(
        plugin: Plugin,
        source: PacketEntity,
        startLocation: Location,
        initialDirection: Vector,
        initialTarget: Entity,
        speedValue: Double,
        maxDistanceValue: Double,
        hitRadiusValue: Double,
        homingValue: Double
    ) {
        launchParticleProjectileCommon(
            plugin,
            startLocation,
            initialDirection,
            initialTarget,
            speedValue,
            maxDistanceValue,
            hitRadiusValue,
            homingValue,
            sourceEntity = null,
            sourcePacket = source
        )
    }

    /**
     * パーティクル発射体を発射（共通処理）
     */
    private suspend fun launchParticleProjectileCommon(
        plugin: Plugin,
        startLocation: Location,
        initialDirection: Vector,
        initialTarget: Entity,
        speedValue: Double,
        maxDistanceValue: Double,
        hitRadiusValue: Double,
        homingValue: Double,
        sourceEntity: Entity?,
        sourcePacket: PacketEntity?
    ) {
        plugin.launch {
            val currentLocation = startLocation.clone()
            var currentDirection = initialDirection.clone()
            var traveledDistance = 0.0
            val hitEntities = mutableSetOf<Entity>()

            while (isActive && traveledDistance < maxDistanceValue) {
                // ホーミング処理
                if (homingValue > 0.0 && initialTarget.isValid && !initialTarget.isDead) {
                    val toTarget = initialTarget.location.toVector().subtract(currentLocation.toVector()).normalize()
                    currentDirection = currentDirection.multiply(1.0 - homingValue).add(toTarget.multiply(homingValue)).normalize()
                }

                // 移動
                val velocity = currentDirection.clone().multiply(speedValue / 20.0)
                currentLocation.add(velocity)
                traveledDistance += velocity.length()

                // パーティクル表示
                val particle = getParticleType()
                currentLocation.world?.spawnParticle(particle, currentLocation, 2, 0.05, 0.05, 0.05, 0.0)

                // Tick エフェクト
                applyTickEffects(plugin, sourceEntity, sourcePacket)

                // 命中判定
                val nearbyEntities = currentLocation.world?.getNearbyEntities(
                    currentLocation,
                    hitRadiusValue,
                    hitRadiusValue,
                    hitRadiusValue
                )?.filter {
                    it is LivingEntity && it != sourceEntity && it.isValid && !it.isDead && it !in hitEntities
                } ?: emptyList()

                if (nearbyEntities.isNotEmpty()) {
                    val hitEntity = nearbyEntities.first()
                    hitEntities.add(hitEntity)

                    // 命中エフェクト適用
                    applyHitEffectsCommon(plugin, sourceEntity, sourcePacket, hitEntity)

                    // 貫通しない場合は終了
                    if (!pierce) {
                        break
                    }
                }

                delay(50)
            }
        }
    }

    /**
     * 実エンティティ発射体をスポーン
     */
    private fun spawnProjectileEntity(
        source: Entity,
        location: Location,
        direction: Vector,
        speedValue: Double
    ): Projectile? {
        location.world ?: return null

        val projectile = when (projectileType) {
            ProjectileType.ARROW -> (source as? ProjectileSource)?.launchProjectile(Arrow::class.java)
            ProjectileType.SPECTRAL_ARROW -> (source as? ProjectileSource)?.launchProjectile(SpectralArrow::class.java)
            ProjectileType.FIREBALL -> (source as? ProjectileSource)?.launchProjectile(Fireball::class.java)
            ProjectileType.SMALL_FIREBALL -> (source as? ProjectileSource)?.launchProjectile(SmallFireball::class.java)
            ProjectileType.SNOWBALL -> (source as? ProjectileSource)?.launchProjectile(Snowball::class.java)
            ProjectileType.EGG -> (source as? ProjectileSource)?.launchProjectile(Egg::class.java)
            ProjectileType.ENDER_PEARL -> (source as? ProjectileSource)?.launchProjectile(EnderPearl::class.java)
            ProjectileType.WITHER_SKULL -> (source as? ProjectileSource)?.launchProjectile(WitherSkull::class.java)
            ProjectileType.DRAGON_FIREBALL -> (source as? ProjectileSource)?.launchProjectile(DragonFireball::class.java)
            else -> null
        }

        projectile?.velocity = direction.multiply(speedValue)
        if (!gravity && projectile is AbstractArrow) {
            projectile.setGravity(false)
        }

        return projectile
    }

    /**
     * 発射体を追跡（Entity source）
     */
    private suspend fun trackProjectile(
        plugin: Plugin,
        source: Entity,
        projectile: Projectile,
        target: Entity,
        homingValue: Double,
        hitRadiusValue: Double
    ) {
        trackProjectileCommon(plugin, projectile, target, homingValue, hitRadiusValue, source, null)
    }

    /**
     * 発射体を追跡（PacketEntity source）
     */
    private suspend fun trackProjectileFromPacket(
        plugin: Plugin,
        source: PacketEntity,
        projectile: Projectile,
        target: Entity,
        homingValue: Double,
        hitRadiusValue: Double
    ) {
        trackProjectileCommon(plugin, projectile, target, homingValue, hitRadiusValue, null, source)
    }

    /**
     * 発射体を追跡（共通処理）
     */
    private suspend fun trackProjectileCommon(
        plugin: Plugin,
        projectile: Projectile,
        target: Entity,
        homingValue: Double,
        hitRadiusValue: Double,
        sourceEntity: Entity?,
        sourcePacket: PacketEntity?
    ) {
        plugin.launch {
            while (isActive && projectile.isValid && !projectile.isDead) {
                // ホーミング
                if (homingValue > 0.0 && target.isValid && !target.isDead) {
                    val toTarget = target.location.toVector().subtract(projectile.location.toVector()).normalize()
                    val currentVel = projectile.velocity
                    val newVel = currentVel.multiply(1.0 - homingValue).add(toTarget.multiply(homingValue * currentVel.length()))
                    projectile.velocity = newVel
                }

                // 命中判定
                val nearbyEntities = projectile.location.world?.getNearbyEntities(
                    projectile.location,
                    hitRadiusValue,
                    hitRadiusValue,
                    hitRadiusValue
                )?.filter {
                    it is LivingEntity && it != sourceEntity && it.isValid && !it.isDead
                } ?: emptyList()

                if (nearbyEntities.isNotEmpty()) {
                    val hitEntity = nearbyEntities.first()
                    applyHitEffectsCommon(plugin, sourceEntity, sourcePacket, hitEntity)

                    if (!pierce) {
                        projectile.remove()
                        break
                    }
                }

                delay(50)
            }
        }
    }

    /**
     * 命中エフェクトを適用（Entity source）
     */
    private suspend fun applyHitEffects(plugin: Plugin, source: Entity, target: Entity) {
        applyHitEffectsCommon(plugin, source, null, target)
    }

    /**
     * 命中エフェクトを適用（PacketEntity source）
     */
    private suspend fun applyHitEffectsFromPacket(plugin: Plugin, source: PacketEntity, target: Entity) {
        applyHitEffectsCommon(plugin, null, source, target)
    }

    /**
     * 命中エフェクトを適用（共通処理）
     */
    private suspend fun applyHitEffectsCommon(
        plugin: Plugin,
        sourceEntity: Entity?,
        sourcePacket: PacketEntity?,
        target: Entity
    ) {
        // 命中サウンド
        hitSound?.let {
            target.location.world?.playSound(target.location, it, 1.0f, 1.0f)
        }

        // エフェクト適用（メインスレッド）
        for (effect in hitEffects) {
            if (sourceEntity != null) {
                effect.apply(sourceEntity, target)
            } else if (sourcePacket != null) {
                effect.apply(sourcePacket, target)
            }
        }
    }

    /**
     * Tickエフェクトを適用（共通処理）
     */
    private suspend fun applyTickEffects(
        plugin: Plugin,
        sourceEntity: Entity?,
        sourcePacket: PacketEntity?
    ) {
        // メインスレッドで実行
        for (effect in tickEffects) {
            if (sourceEntity != null) {
                effect.apply(sourceEntity, sourceEntity)
            } else if (sourcePacket != null) {
                effect.apply(sourcePacket, sourcePacket)
            }
        }
    }

    /**
     * パーティクルタイプかどうか
     */
    private fun isParticleProjectile(): Boolean {
        return projectileType in listOf(
            ProjectileType.PARTICLE_FLAME,
            ProjectileType.PARTICLE_MAGIC,
            ProjectileType.PARTICLE_SOUL
        )
    }

    /**
     * パーティクルタイプを取得
     */
    private fun getParticleType(): Particle {
        return when (projectileType) {
            ProjectileType.PARTICLE_FLAME -> Particle.FLAME
            ProjectileType.PARTICLE_MAGIC -> Particle.ENCHANT
            ProjectileType.PARTICLE_SOUL -> Particle.SOUL
            else -> Particle.FLAME
        }
    }

    /**
     * 発射体クラスを取得
     */
    private fun getProjectileClass(): Class<out Projectile> {
        return when (projectileType) {
            ProjectileType.ARROW -> Arrow::class.java
            ProjectileType.SPECTRAL_ARROW -> SpectralArrow::class.java
            ProjectileType.FIREBALL -> Fireball::class.java
            ProjectileType.SMALL_FIREBALL -> SmallFireball::class.java
            ProjectileType.SNOWBALL -> Snowball::class.java
            ProjectileType.EGG -> Egg::class.java
            ProjectileType.ENDER_PEARL -> EnderPearl::class.java
            ProjectileType.WITHER_SKULL -> WitherSkull::class.java
            ProjectileType.DRAGON_FIREBALL -> DragonFireball::class.java
            else -> Arrow::class.java
        }
    }

    /**
     * CEL式を評価してDouble値を取得
     */
    private fun evaluateCelDouble(
        expression: String,
        context: Map<String, Any>,
        evaluator: CELEvaluator,
        defaultValue: Double
    ): Double {
        return try {
            expression.toDoubleOrNull() ?: run {
                when (val result = evaluator.evaluate(expression, context)) {
                    is Number -> result.toDouble()
                    else -> defaultValue
                }
            }
        } catch (e: Exception) {
            DebugLogger.error("Failed to evaluate CEL expression: $expression", e)
            defaultValue
        }
    }

    override fun getDescription(): String {
        return "Projectile Skill ($projectileType, speed: $speed, homing: $homing)"
    }

    override fun debugInfo(): String {
        return "ProjectileSkill[type=$projectileType, speed=$speed, maxDistance=$maxDistance, homing=$homing, pierce=$pierce, hitEffects=${hitEffects.size}]"
    }
}
