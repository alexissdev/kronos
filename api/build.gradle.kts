plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":application"))
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    implementation("com.google.inject:guice:5.1.0")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("api")
    dependencies {
        exclude(dependency("org.spigotmc:spigot-api:.*"))
        exclude(dependency("com.google.inject:guice:.*"))
    }
}
