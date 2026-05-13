import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("net.fabricmc.fabric-loom-remap")
  `maven-publish`
  id("org.jetbrains.kotlin.jvm") version "2.3.20"
  id("com.github.jmongard.git-semver-plugin") version "0.18.0"
}

semver {
  groupVersionIncrements = false
}

version = semver.infoVersion
val archivesBaseName: String by project
val minecraftVersion: String by project
val loaderVersion: String by project
val fabricApiVersion: String by project
val fabricKotlinVersion: String by project
val wynntilsVersion: String by project

base {
  archivesName = archivesBaseName
}

repositories {
  maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
  maven("https://api.modrinth.com/maven")
}

dependencies {
  minecraft("com.mojang:minecraft:$minecraftVersion")
  mappings(loom.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

  modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
  modImplementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

  modLocalRuntime("me.djtheredstoner:DevAuth-fabric:1.2.2")
  modImplementation("maven.modrinth:wynntils:v$wynntilsVersion-fabric")

  implementation("org.lwjgl:lwjgl-nanovg:3.3.3")
  listOf("linux", "macos-arm64").forEach {
    implementation("org.lwjgl:lwjgl-nanovg:3.3.3:natives-$it")
  }
}

loom {
  accessWidenerPath = file("src/main/resources/foxaddons.accesswidener")

  runConfigs.named("client") {
    isIdeConfigGenerated = true
    vmArgs.addAll(
      arrayOf(
        "-Dmixin.debug.export=true", "-Ddevauth.enabled=true", "-Ddevauth.account=main", "-Dfoxaddons.debug=true"
      )
    )
  }
}

afterEvaluate {
  loom.runs.named("client") {
    vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
  }
}

tasks {
  processResources {
    filesMatching("fabric.mod.json") {
      expand(project.properties)
    }
  }

  withType<JavaCompile>().configureEach {
    options.release = 21
  }

  jar {
    inputs.property("archivesName", base.archivesName)

    from("LICENSE") {
      rename { "${it}_${base.archivesName.get()}" }
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_21
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}
