plugins {
    java
    id("io.github.goooler.shadow") version "8.1.7"
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    // Geyser API - needed for all extensions
    compileOnly("org.geysermc.geyser:api:2.3.1-SNAPSHOT")

    // Include other dependencies here - e.g. for configuration.
    implementation("org.spongepowered:configurate-hocon:4.1.2")
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
        archiveClassifier.set("unshaded")
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("org.spongepowered.configurate", "net.onebeastchris.relocate.configurate")
        relocate("io.leangen.geantyref", "net.onebeastchris.relocate.geantyref")
        relocate("com.typesafe.config", "net.onebeastchris.relocate.typesafe")
    }
}


