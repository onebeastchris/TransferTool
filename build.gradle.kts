plugins {
    java
    id("io.freefair.lombok") version "8.4"
    id("com.gradleup.shadow") version "8.3.3"
}

version = "1.2.0"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    // Geyser API - needed for all extensions
    compileOnly("org.geysermc.geyser:api:2.4.1-SNAPSHOT")

    // Include other dependencies here - e.g. for configuration.
    // TODO remove
    implementation("org.spongepowered:configurate-hocon:4.1.2")
    implementation("org.spongepowered:configurate-yaml:4.2.0-GeyserMC-SNAPSHOT")
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    jar {
        archiveBaseName.set("DEV-TransferTool")
        archiveClassifier.set("unshaded")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set(version.toString())
        relocate("org.spongepowered.configurate", "dev.onechris.extension.transfertool.relocate.configurate")
        relocate("io.leangen.geantyref", "dev.onechris.extension.transfertool.relocate.geantyref")
        relocate("com.typesafe.config", "dev.onechris.extension.transfertool.relocate.typesafe")
    }
}


