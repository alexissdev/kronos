dependencies {
    implementation(project(":core"))
    implementation("com.google.inject:guice:5.1.0")
    implementation("com.google.guava:guava:32.1.3-jre")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}
