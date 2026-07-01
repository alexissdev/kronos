plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    implementation(project(":core"))
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation("com.google.inject:guice:5.1.0")
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:32.1.3-jre")
}
