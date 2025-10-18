plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("java")
    `maven-publish`
}

group = "com.github.ionelchis"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.core)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "com.github.ionelchis"
            artifactId = "miko"
            version = "0.1.0"
            pom {
                 name.set("Miko")
                description.set("A lightweight, reflection-based dependency injection library for Kotlin.")
                url.set("https://github.com/ionelchis/miko")
                licenses {
                    license {
                        name.set("Apache-2.0 license")
                        url.set("https://opensource.org/license/apache-2-0")
                    }
                }
                developers {
                    developer {
                        id.set("ionelchis")
                        name.set("Ionel Chis")
                    }
                }
            }
        }
    }
}

