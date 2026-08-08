plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Compiled against the oldest supported API (1.21.11) rather than the newest (26.2) so the same
    // jar keeps working across the whole supported range (1.21.11, 26.1.x, 26.2) - verified by
    // manually booting the built jar against real 1.21.11, 26.1.2, and 26.2 servers.
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    // Java 21 bytecode runs fine on newer JVMs (Paper 26.x requires the server itself to run on
    // Java 25, but that doesn't require the plugin to be compiled with a newer --release).
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        // Note: Paper 26.x requires the server JVM itself to be Java 25 - if this task fails with
        // "requires running the server with Java 25 or above", point JAVA_HOME (or Gradle's daemon
        // JVM) at a Java 25 installation, or test 26.x builds by running the jar manually instead.
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
