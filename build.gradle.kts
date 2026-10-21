plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
    id("ploceus") version "1.17.4"
}

// OneClient runs 1.8.9 on Ornithe (calamus gen2 intermediary + Feather mappings), not Legacy Fabric.
ploceus {
    setIntermediaryGeneration(2)
}

version = property("mod_version") as String
group = property("maven_group") as String
base.archivesName = property("mod_id") as String

dependencies {
    val mc = property("minecraft_version") as String
    minecraft("com.mojang:minecraft:$mc")
    mappings(ploceus.layeredMappings {
        mappings("net.ornithemc:feather-gen2:$mc+build.${property("feather_build")}:v2") {
            containsUnpick()
        }
    })
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

// Java 8 bytecode so the jar also loads on launchers that run 1.8.9 on Java 8.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}

tasks.processResources {
    val props = mapOf(
        "id" to project.property("mod_id"),
        "name" to project.property("mod_name"),
        "version" to project.version,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}
