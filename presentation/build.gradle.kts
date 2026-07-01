dependencies {
    implementation(project(":core"))
    implementation(project(":application"))
    implementation(project(":api"))
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    implementation("com.google.inject:guice:5.1.0")
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("com.google.code.gson:gson:2.10.1")
}
