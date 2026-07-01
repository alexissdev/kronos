plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" apply false
}

allprojects {
    group = "dev.alexissdev.kronos"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
