dependencies {
    implementation(project(":kronos-common"))
    implementation(project(":kronos-players"))
    implementation(project(":kronos-factions"))
    implementation(project(":kronos-koth"))
    implementation(project(":kronos-timers"))
    implementation(project(":kronos-claims"))
    implementation(project(":kronos-classes"))
    implementation(project(":kronos-economy"))
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    implementation("com.google.inject:guice:5.1.0")
}
