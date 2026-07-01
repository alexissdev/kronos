plugins {
    java
    id("com.gradleup.shadow") version "8.3.6" apply false
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0" apply false
}

allprojects {
    group = "dev.alexissdev.kronos"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://repo.md-5.net/content/repositories/snapshots/") }
        maven { url = uri("https://jitpack.io") }
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
