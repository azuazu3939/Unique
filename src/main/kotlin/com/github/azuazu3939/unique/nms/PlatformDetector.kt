package com.github.azuazu3939.unique.nms

import org.bukkit.Bukkit

/**
 * Detects the current platform (Paper or Folia)
 */
object PlatformDetector {

    private var cachedScheduler: PlatformScheduler? = null

    /**
     * Get the appropriate scheduler for the current platform
     */
    fun getScheduler(): PlatformScheduler {
        if (cachedScheduler != null) return cachedScheduler!!

        cachedScheduler = if (isFolia()) {
            FoliaScheduler()
        } else {
            PaperScheduler()
        }

        return cachedScheduler!!
    }

    /**
     * Check if running on Folia
     */
    fun isFolia(): Boolean {
        return try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Get platform name
     */
    fun getPlatformName(): String {
        return if (isFolia()) "Folia" else "Paper"
    }

    /**
     * Get platform version
     */
    fun getPlatformVersion(): String {
        return Bukkit.getVersion()
    }
}
