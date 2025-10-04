plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.2.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "com.github.azuazu3939"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.evokegames.gg/snapshots")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("dev.folia:folia-api:1.21.8-R0.1-SNAPSHOT")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-api:2.22.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-core:2.22.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    compileOnly("com.github.retrooper:packetevents-spigot:2.9.5")
    implementation("dev.cel:cel:0.11.0")

    paperweight.foliaDevBundle("1.21.8-R0.1-SNAPSHOT")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    processResources {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")

            val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name,
            )

            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(projectDir) { include("LICENSE") }
    }
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "com.github.azuazu3939.lib.com.zaxxer.hikari")
        relocate("org.mariadb.jdbc", "com.github.azuazu3939.lib.org.mariadb.jdbc")
        relocate("com.github.shynixn", "com.github.azuazu3939.lib.com.github.shynixn")
        relocate("org.jetbrains", "com.github.azuazu3939.lib.org.jetbrains")
        relocate("me.tofaa.entitylib", "com.github.azuazu3939.lib.me.tofaa.entitylib")
        relocate("dev.cel", "com.github.azuazu3939.lib.dev.cel")
    }
}

