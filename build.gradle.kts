plugins {
    id("dev.architectury.loom")
    id("maven-publish")
}

val loader = property("loader") as String
val mc = property("minecraft_version") as String
val javaVersion = (property("java_version") as String).toInt()
val modId = property("mod_id") as String

version = "${property("mod_version")}+$mc-$loader"
group = property("mod_group") as String
base.archivesName.set(modId)

repositories {
    mavenCentral()
    maven("https://maven.parchmentmc.org/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.minecraftforge.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-$mc:${property("parchment")}@zip")
    })

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")

    when (loader) {
        "fabric" -> {
            modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader")}")
            modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api")}")
        }
        "forge" -> "forge"("net.minecraftforge:forge:${property("forge_version")}")
        "neoforge" -> "neoForge"("net.neoforged:neoforge:${property("neoforge_version")}")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "5000"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

// Minecraft 1.21.11 renamed ResourceLocation to Identifier (Mojang mappings); the sources keep the old name.
stonecutter {
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("ResourceLocation", "Identifier")
    }
}

// Platform-specific code lives under platform/<loader>; only the active loader is compiled.
sourceSets.main {
    java {
        listOf("fabric", "forge", "neoforge").filter { it != loader }.forEach {
            exclude("**/platform/$it/**")
        }
    }
    resources.srcDir(rootProject.file("loaders/$loader/resources"))
}

// Minecraft 1.21.11 replaced most of the rendering API. Files that had to be rewritten for it live in
// src/overlay-1.21.11/java (same package and name as the file they replace) and take the place of the
// Stonecutter-processed original only when building a 1.21.11 target.
if (stonecutter.eval(mc, ">=1.21.11")) {
    val overlayDir = rootProject.file("src/overlay-1.21.11/java")
    val mergedDir = layout.buildDirectory.dir("generated/overlay/main/java")
    val mergeOverlay = tasks.register<Sync>("mergeOverlay") {
        dependsOn("stonecutterGenerate")
        from(overlayDir)
        from(layout.buildDirectory.dir("generated/stonecutter/main/java"))
        into(mergedDir)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE // the overlay is listed first, so its files win
        listOf("fabric", "forge", "neoforge").filter { it != loader }.forEach { exclude("**/platform/$it/**") }
    }
    afterEvaluate {
        tasks.named<JavaCompile>("compileJava") {
            dependsOn(mergeOverlay)
            setSource(mergedDir)
        }
        tasks.named<Jar>("sourcesJar") {
            dependsOn(mergeOverlay)
            exclude("**/*.java")
            from(mergedDir)
        }
    }
}

// Example mod ("testmod"): compiled against the main output and packaged as a separate jar.
val testmod: SourceSet = sourceSets.create("testmod") {
    java {
        setSrcDirs(listOf(rootProject.file("src/testmod/java")))
        listOf("fabric", "forge", "neoforge").filter { it != loader }.forEach {
            exclude("**/testmod/$it/**")
        }
    }
    resources.setSrcDirs(listOf(rootProject.file("src/testmod/resources"), rootProject.file("loaders/$loader/testmod-resources")))
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + output
}

// Compile the testmod from Stonecutter's output so its version comments (//? if ...) apply too.
afterEvaluate {
    tasks.named<JavaCompile>("compileTestmodJava") {
        dependsOn("stonecutterGenerateTestmod")
        setSource(layout.buildDirectory.dir("generated/stonecutter/testmod/java"))
        listOf("fabric", "forge", "neoforge").filter { it != loader }.forEach { exclude("**/testmod/$it/**") }
    }
}

val metaProps = mapOf(
    "mod_id" to modId,
    "mod_name" to property("mod_name"),
    "version" to project.version,
    "mod_author" to property("mod_author"),
    "mod_license" to property("mod_license"),
    "mod_description" to property("mod_description"),
    "mod_homepage" to property("mod_homepage"),
    "mod_sources" to property("mod_sources"),
    "mod_issues" to property("mod_issues"),
    "mc_range" to property("mc_range"),
    "minecraft_version" to mc,
    "java_version" to javaVersion,
    "pack_format" to property("pack_format"),
)

tasks.named<ProcessResources>("processTestmodResources") {
    inputs.properties(metaProps)
    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(metaProps)
    }
}

val testmodJar = tasks.register<Jar>("testmodJar") {
    archiveBaseName.set("$modId-testmod")
    archiveClassifier.set("dev")
    from(testmod.output)
}

val remapTestmodJar = tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapTestmodJar") {
    dependsOn(testmodJar)
    inputFile.set(testmodJar.flatMap { it.archiveFile })
    archiveBaseName.set("$modId-testmod")
    archiveClassifier.set("")
}
tasks.named("build") { dependsOn(remapTestmodJar) }

tasks.processResources {
    inputs.properties(metaProps)
    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(metaProps)
    }
}

loom {
    runConfigs.configureEach {
        ideConfigGenerated(false)
        property("bloomberto.debugEvents", "true")
        findProperty("autoEffect")?.let { property("bloomberto.autoEffect", it.toString()) }
        findProperty("selfTest")?.let { property("bloomberto.selfTest", it.toString()) }
    }
}

// Final jars of every target are collected in the root build/libs folder.
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.named("remapJar").map { (it as org.gradle.jvm.tasks.Jar).archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
    dependsOn("build")
}
