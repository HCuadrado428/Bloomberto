pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
    }
    plugins {
        id("dev.architectury.loom") version "1.11-SNAPSHOT"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "bloomberto"

// One Stonecutter "version" per (Minecraft version, loader) pair -> one jar each.
// The first argument is the project name, the second the Minecraft version used by
// Stonecutter conditions such as `//? if >=1.21 {`.
stonecutter {
    create(rootProject) {
        version("1.20.1-fabric", "1.20.1")
        version("1.20.1-forge", "1.20.1")
        version("1.21.1-fabric", "1.21.1")
        version("1.21.1-forge", "1.21.1")
        version("1.21.1-neoforge", "1.21.1")
        vcsVersion = "1.21.1-fabric"
    }
}
