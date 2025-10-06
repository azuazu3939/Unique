package com.github.azuazu3939.unique.util

import com.github.azuazu3939.unique.Unique
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.block.Biome
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType

/**
 * リソースキー解決ユーティリティ
 *
 * カスタムリソースキー（例: "custom:boss_roar"）を解決する
 * Paperのレジストリシステムを使用
 *
 * 解決順序:
 * 1. custom_namespace:key で検索
 * 2. minecraft:key で検索
 * 3. KEY (大文字、名前空間なし) で検索
 * 4. デフォルト値を返す
 */
object ResourceKeyResolver {

    /**
     * キーの変換バリエーションを生成
     * アンダースコアとドットの両方をサポート
     *
     * @param key 元のキー（例: "entity_ender_dragon_growl" or "entity.ender.dragon.growl"）
     * @return 変換バリエーションのリスト（元のキー + 変換後のキー）
     */
    private fun getKeyVariants(key: String): List<String> {
        val variants = mutableListOf(key.lowercase())

        // _ を . に変換したバリアントを追加
        if (key.contains("_")) {
            variants.add(key.lowercase().replace("_", "."))
        }

        // . を _ に変換したバリアントを追加
        if (key.contains(".")) {
            variants.add(key.lowercase().replace(".", "_"))
        }

        return variants.distinct()
    }

    /**
     * Sound を解決
     *
     * @param key リソースキー（例: "custom:boss_roar", "entity_ender_dragon_growl", "entity.ender.dragon.growl"）
     * @param default デフォルト値
     * @return 解決されたSound
     */
    fun resolveSound(key: String, default: Sound = Sound.BLOCK_NOTE_BLOCK_PLING): Sound {
        if (!Unique.instance.configManager.mainConfig.resources.enableCustomKeys) {
            return resolveSoundDirect(key, default)
        }

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT)

        // カスタムキーの場合（namespace:key形式）
        if (key.contains(":")) {
            val parts = key.split(":", limit = 2)
            val namespace = parts[0]
            val resourceKey = parts[1]

            // カスタムネームスペースで検索
            val customNamespace = Unique.instance.configManager.mainConfig.resources.customNamespace
            val keyVariants = getKeyVariants(resourceKey)

            if (namespace == customNamespace) {
                // 1. custom_namespace:key で試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
                // 2. minecraft:key で試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key("minecraft", variant))?.let { return it }
                }
            } else {
                // 指定されたネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
            }

            logMissingResource("Sound", key)
            return default
        }

        // 通常のキー（namespace なし） → minecraft: として扱う
        return resolveSoundDirect(key, default)
    }

    /**
     * 直接Sound名から解決（namespace なし → minecraft: として扱う）
     */
    private fun resolveSoundDirect(key: String, default: Sound): Sound {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT)
        val keyVariants = getKeyVariants(key)

        // minecraft:key として解決（全バリアント試行）
        for (variant in keyVariants) {
            registry.get(Key.key("minecraft", variant))?.let { return it }
        }

        logMissingResource("Sound", key)
        return default
    }

    /**
     * Material を解決
     */
    fun resolveMaterial(key: String, default: Material = Material.STONE): Material {
        if (!Unique.instance.configManager.mainConfig.resources.enableCustomKeys) {
            return resolveMaterialDirect(key, default)
        }

        if (key.contains(":")) {
            val parts = key.split(":", limit = 2)
            val namespace = parts[0]
            val resourceKey = parts[1]

            val customNamespace = Unique.instance.configManager.mainConfig.resources.customNamespace
            val keyVariants = getKeyVariants(resourceKey)

            if (namespace == customNamespace) {
                // 1. カスタムネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    resolveMaterialByKey(Key.key(namespace, variant))?.let { return it }
                }
                // 2. minecraft:key で試行（全バリアント）
                for (variant in keyVariants) {
                    resolveMaterialByKey(Key.key("minecraft", variant))?.let { return it }
                }
            } else {
                // 指定されたネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    resolveMaterialByKey(Key.key(namespace, variant))?.let { return it }
                }
            }

            logMissingResource("Material", key)
            return default
        }

        // namespace なし → minecraft: として扱う
        return resolveMaterialDirect(key, default)
    }

    private fun resolveMaterialDirect(key: String, default: Material): Material {
        val keyVariants = getKeyVariants(key)

        // minecraft:key として解決（全バリアント試行）
        for (variant in keyVariants) {
            resolveMaterialByKey(Key.key("minecraft", variant))?.let { return it }
        }

        logMissingResource("Material", key)
        return default
    }

    /**
     * KeyからMaterialを解決
     */
    private fun resolveMaterialByKey(key: Key): Material? {
        // ItemとBlockの両方のレジストリを試行
        return try {
            Registry.MATERIAL.getOrThrow(key)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Particle を解決
     */
    fun resolveParticle(key: String, default: Particle = Particle.FLAME): Particle {
        if (!Unique.instance.configManager.mainConfig.resources.enableCustomKeys) {
            return resolveParticleDirect(key, default)
        }

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.PARTICLE_TYPE)

        if (key.contains(":")) {
            val parts = key.split(":", limit = 2)
            val namespace = parts[0]
            val resourceKey = parts[1]

            val customNamespace = Unique.instance.configManager.mainConfig.resources.customNamespace
            val keyVariants = getKeyVariants(resourceKey)

            if (namespace == customNamespace) {
                // 1. カスタムネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
                // 2. minecraft:key で試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key("minecraft", variant))?.let { return it }
                }
            } else {
                // 指定されたネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
            }

            logMissingResource("Particle", key)
            return default
        }

        // namespace なし → minecraft: として扱う
        return resolveParticleDirect(key, default)
    }

    private fun resolveParticleDirect(key: String, default: Particle): Particle {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.PARTICLE_TYPE)
        val keyVariants = getKeyVariants(key)

        // minecraft:key として解決（全バリアント試行）
        for (variant in keyVariants) {
            registry.get(Key.key("minecraft", variant))?.let { return it }
        }

        logMissingResource("Particle", key)
        return default
    }

    /**
     * Biome を解決
     */
    fun resolveBiome(key: String, default: Biome = Biome.PLAINS): Biome {
        if (!Unique.instance.configManager.mainConfig.resources.enableCustomKeys) {
            return resolveBiomeDirect(key, default)
        }

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)

        if (key.contains(":")) {
            val parts = key.split(":", limit = 2)
            val namespace = parts[0]
            val resourceKey = parts[1]

            val customNamespace = Unique.instance.configManager.mainConfig.resources.customNamespace
            val keyVariants = getKeyVariants(resourceKey)

            if (namespace == customNamespace) {
                // 1. カスタムネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
                // 2. minecraft:key で試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key("minecraft", variant))?.let { return it }
                }
            } else {
                // 指定されたネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
            }

            logMissingResource("Biome", key)
            return default
        }

        // namespace なし → minecraft: として扱う
        return resolveBiomeDirect(key, default)
    }

    private fun resolveBiomeDirect(key: String, default: Biome): Biome {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
        val keyVariants = getKeyVariants(key)

        // minecraft:key として解決（全バリアント試行）
        for (variant in keyVariants) {
            registry.get(Key.key("minecraft", variant))?.let { return it }
        }

        logMissingResource("Biome", key)
        return default
    }

    /**
     * Enchantment を解決
     */
    fun resolveEnchantment(key: String, default: Enchantment? = null): Enchantment? {
        if (!Unique.instance.configManager.mainConfig.resources.enableCustomKeys) {
            return resolveEnchantmentDirect(key, default)
        }

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)

        if (key.contains(":")) {
            val parts = key.split(":", limit = 2)
            val namespace = parts[0]
            val resourceKey = parts[1]

            val customNamespace = Unique.instance.configManager.mainConfig.resources.customNamespace
            val keyVariants = getKeyVariants(resourceKey)

            if (namespace == customNamespace) {
                // 1. カスタムネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
                // 2. minecraft:key で試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key("minecraft", variant))?.let { return it }
                }
            } else {
                // 指定されたネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
            }

            logMissingResource("Enchantment", key)
            return default
        }

        // namespace なし → minecraft: として扱う
        return resolveEnchantmentDirect(key, default)
    }

    private fun resolveEnchantmentDirect(key: String, default: Enchantment?): Enchantment? {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
        val keyVariants = getKeyVariants(key)

        // minecraft:key として解決（全バリアント試行）
        for (variant in keyVariants) {
            registry.get(Key.key("minecraft", variant))?.let { return it }
        }

        logMissingResource("Enchantment", key)
        return default
    }

    /**
     * EntityType を解決
     */
    fun resolveEntityType(key: String, default: EntityType = EntityType.PIG): EntityType {
        if (!Unique.instance.configManager.mainConfig.resources.enableCustomKeys) {
            return resolveEntityTypeDirect(key, default)
        }

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)

        if (key.contains(":")) {
            val parts = key.split(":", limit = 2)
            val namespace = parts[0]
            val resourceKey = parts[1]

            val customNamespace = Unique.instance.configManager.mainConfig.resources.customNamespace
            val keyVariants = getKeyVariants(resourceKey)

            if (namespace == customNamespace) {
                // 1. カスタムネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
                // 2. minecraft:key で試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key("minecraft", variant))?.let { return it }
                }
            } else {
                // 指定されたネームスペースで試行（全バリアント）
                for (variant in keyVariants) {
                    registry.get(Key.key(namespace, variant))?.let { return it }
                }
            }

            logMissingResource("EntityType", key)
            return default
        }

        // namespace なし → minecraft: として扱う
        return resolveEntityTypeDirect(key, default)
    }

    private fun resolveEntityTypeDirect(key: String, default: EntityType): EntityType {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)
        val keyVariants = getKeyVariants(key)

        // minecraft:key として解決（全バリアント試行）
        for (variant in keyVariants) {
            registry.get(Key.key("minecraft", variant))?.let { return it }
        }

        logMissingResource("EntityType", key)
        return default
    }

    /**
     * リソースが見つからなかったことをログに記録
     */
    private fun logMissingResource(type: String, key: String) {
        if (Unique.instance.configManager.mainConfig.resources.logMissingResources) {
            DebugLogger.warn("Failed to resolve $type: $key")
        }
    }

    /**
     * BlockData文字列を解決
     *
     * @param key ブロックキー（例: "custom:magic_block", "DIAMOND_BLOCK"）
     * @return BlockData、またはnull
     */
    fun resolveBlockData(key: String): org.bukkit.block.data.BlockData? {
        val material = resolveMaterial(key, Material.STONE)
        return if (material.isBlock) {
            org.bukkit.Bukkit.createBlockData(material)
        } else {
            DebugLogger.warn("Material $key is not a block")
            null
        }
    }

    /**
     * ItemStack を作成
     */
    fun createItemStack(key: String, amount: Int = 1): org.bukkit.inventory.ItemStack {
        val material = resolveMaterial(key, Material.STONE)
        return org.bukkit.inventory.ItemStack(material, amount)
    }
}
