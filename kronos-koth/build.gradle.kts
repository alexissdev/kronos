dependencies {
    implementation(project(":kronos-common"))
    implementation(project(":kronos-factions"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    implementation("com.google.inject:guice:5.1.0")
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
    implementation("com.google.guava:guava:32.1.3-jre")
}
