import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.spotless)

    `java-library`
    `maven-publish`
}

group = "com.noxcrew.posthog-kotlin"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.kotlinx.coroutines)
    api(libs.okhttp)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.api)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    jvmToolchain(21)
}

spotless {
    spotless {
        kotlin {
            ktlint()
        }

        kotlinGradle {
            ktlint()
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "noxcrew-public"
            url = uri("https://maven.noxcrew.com/public")

            credentials {
                username = System.getenv("NOXCREW_MAVEN_PUBLIC_USERNAME")
                password = System.getenv("NOXCREW_MAVEN_PUBLIC_PASSWORD")
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "smp"
                description = "A PostHog library for Kotlin."
                url = "https://github.com/Noxcrew/posthog-kotlin"

                scm {
                    url = "https://github.com/Noxcrew/posthog-kotlin"
                    connection = "scm:git:https://github.com/Noxcrew/posthog-kotlin.git"
                    developerConnection = "scm:git:https://github.com/Noxcrew/posthog-kotlin.git"
                }

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id = "noxcrew"
                        name = "Noxcrew"
                        email = "contact@noxcrew.com"
                    }
                }
            }
        }
    }
}
