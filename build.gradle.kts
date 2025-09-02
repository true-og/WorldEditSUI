/* This is free and unencumbered software released into the public domain */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.provideDelegate

plugins {
    id("java")
    id("java-library")
    id("com.diffplug.spotless") version "7.0.4"
    id("com.gradleup.shadow") version "8.3.6"
    id("checkstyle")
    eclipse
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

group = "eu.kennytv.worldeditsui"

version = "1.7.4"

val apiVersion = "1.19"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://repo.purpurmc.org/snapshots") }
    maven { url = uri("file://${System.getProperty("user.home")}/.m2/repository") }
    System.getProperty("SELF_MAVEN_LOCAL_REPO")?.let {
        val dir = file(it)
        if (dir.isDirectory) {
            println("Using SELF_MAVEN_LOCAL_REPO at: $it")
            maven { url = uri("file://${dir.absolutePath}") }
        } else {
            logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
            mavenLocal()
        }
    } ?: logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "eclipse")

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://maven.enginehub.org/repo/") }
        maven { url = uri("https://repo.purpurmc.org/snapshots") }
        maven { url = uri("file://${System.getProperty("user.home")}/.m2/repository") }
        System.getProperty("SELF_MAVEN_LOCAL_REPO")?.let {
            val dir = file(it)
            if (dir.isDirectory) {
                println("Using SELF_MAVEN_LOCAL_REPO at: $it")
                maven { url = uri("file://${dir.absolutePath}") }
            } else {
                logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
                mavenLocal()
            }
        } ?: logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
    }
}

project(":worldedit-sui") {
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        implementation(project(":we-compat:we-compat-common"))
        implementation(project(":we-compat:we6-compat"))
        implementation(project(":we-compat:we7-compat"))
        implementation("org.bstats:bstats-bukkit:3.0.2")
        compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT")
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")
        compileOnly("org.jetbrains:annotations:24.0.1")
    }

    tasks.named<ProcessResources>("processResources") {
        val props = mapOf("version" to rootProject.version, "apiVersion" to apiVersion)
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
        from(rootProject.file("LICENSE")) { into("/") }
    }

    tasks.named<ShadowJar>("shadowJar") {
        relocate("org.bstats", "eu.kennytv.worldeditsui.lib.bstats")
        archiveBaseName.set("WorldEditSUI")
        archiveClassifier.set("")
        minimize()
        manifest { attributes(mapOf("paperweight-mappings-namespace" to "mojang")) }
    }

    tasks.named<Jar>("jar") { manifest { attributes(mapOf("paperweight-mappings-namespace" to "mojang")) } }
}

project(":we-compat") {
    // aggregator
}

project(":we-compat:we-compat-common") {
    dependencies {
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:6.1.4-SNAPSHOT")
        compileOnly("org.jetbrains:annotations:24.0.1")
    }
}

project(":we-compat:we6-compat") {
    dependencies {
        compileOnly(project(":we-compat:we-compat-common"))
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:6.1.4-SNAPSHOT")
        compileOnly("com.sk89q:worldguard:6.1.1-SNAPSHOT")
        compileOnly("org.jetbrains:annotations:24.0.1")
    }
}

project(":we-compat:we7-compat") {
    dependencies {
        compileOnly(project(":we-compat:we-compat-common"))
        compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")
        compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9") { exclude(group = "com.sk89q.worldedit") }
        compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
        compileOnly("org.jetbrains:annotations:24.0.1")
    }
}

subprojects {
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

tasks.jar { archiveClassifier.set("part") }

tasks.build { dependsOn(tasks.spotlessApply, ":worldedit-sui:shadowJar") }

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:-removal")
    }
}

spotless {
    java {
        eclipse().configFile("config/formatter/eclipse-java-formatter.xml")
        leadingTabsToSpaces()
        removeUnusedImports()
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
        target("build.gradle.kts", "settings.gradle.kts")
    }
}

checkstyle {
    toolVersion = "10.18.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = true
    isShowViolations = true
}

tasks.named("compileJava") { dependsOn("spotlessApply") }

tasks.named("spotlessCheck") { dependsOn("spotlessApply") }
