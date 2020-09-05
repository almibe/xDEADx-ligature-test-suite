plugins {
    id("org.jetbrains.kotlin.jvm").version("1.4.0")
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
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("dev.ligature:ligature:0.1.0-SNAPSHOT")
    implementation("io.kotest:kotest-runner-junit5:4.2.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
