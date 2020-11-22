plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
    `java-library`
    `maven-publish`
}

project.group = "dev.ligature"
project.version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    implementation("dev.ligature:ligature:0.1.0-SNAPSHOT")
    testImplementation("io.kotest:kotest-runner-junit5:4.3.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
