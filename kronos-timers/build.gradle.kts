dependencies {
    implementation(project(":kronos-common"))
    implementation(project(":kronos-players"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    implementation("com.google.inject:guice:5.1.0")
    implementation("io.lettuce:lettuce-core:6.3.2.RELEASE")
    implementation("com.google.guava:guava:32.1.3-jre")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.6.0")
}

tasks.test { useJUnitPlatform() }
