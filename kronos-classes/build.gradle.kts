dependencies {
    implementation(project(":kronos-common"))
    implementation(project(":kronos-timers"))
    implementation(project(":kronos-players"))
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    implementation("com.google.inject:guice:5.1.0")
    implementation("com.google.guava:guava:32.1.3-jre")
}
