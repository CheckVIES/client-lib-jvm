import org.jreleaser.model.Active
import java.util.Base64

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.dokka)
    alias(libs.plugins.jreleaser)
    `maven-publish`
}

dependencies {
    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Tests
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

// OpenAPI Generator Configuration
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/openapi/spec.yaml".replace("\\", "/"))
    outputDir.set(layout.buildDirectory.dir("generated-sources").get().asFile.absolutePath)
    packageName.set("com.checkvies.client.generated")
    apiPackage.set("com.checkvies.client.generated.api")
    modelPackage.set("com.checkvies.client.generated.model")
    configOptions.set(
        mapOf(
            "library" to "jvm-ktor",
            "serializationLibrary" to "kotlinx_serialization",
            "enumPropertyNaming" to "PascalCase",
            "useCoroutines" to "true"
        )
    )
}

sourceSets {
    main {
        kotlin.srcDir(layout.buildDirectory.dir("generated-sources/src/main/kotlin"))
    }
}

val artefactDir = "_waiting-deploy"

// Publishing
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.checkvies"
            artifactId = "client"
            version = project.version.toString()

            pom {
                name.set("CheckVIES API Client")
                description.set("Kotlin/JVM client for CheckVIES automated API")
                url.set("https://github.com/checkvies/client-lib-jvm")
                licenses {
                    license {
                        name.set("Apache-2.0 license")
                        url.set("https://github.com/checkvies/client-lib-jvm")
                    }
                }
                developers {
                    developer {
                        id.set("checkvies")
                        name.set("DEIMDAL DOO")
                        email.set("software@checkvies.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/checkvies/client-lib-jvm.git")
                    developerConnection.set("scm:git:https://github.com/checkvies/client-lib-jvm.git")
                    url.set("https://github.com/checkvies/client-lib-jvm")
                    tag.set(version)
                }
            }
        }
    }
    repositories {
        maven {
            setUrl(layout.buildDirectory.dir(artefactDir))
        }
    }
}

jreleaser {
    release {
        github {
            enabled = false
        }
        generic {
            enabled = true
            skipRelease = true
        }
    }
    gitRootSearch = true
    signing {
        pgp {
            active = Active.ALWAYS
            armored = true
            publicKey = retrieveKey("ENCODED_GPG_PUBLIC_KEY")
            secretKey = retrieveKey("ENCODED_GPG_SECRET_KEY")
        }
    }
    deploy {
        maven {
            mavenCentral {
                register("maven-central") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(layout.buildDirectory.dir(artefactDir).get())
                    skipPublicationCheck = true
                }
            }
        }
    }
}

fun retrieveKey(name: String): String {
    val base64 = System.getenv(name) ?: ""
    val decodedBytes = Base64.getDecoder().decode(base64)
    val key = String(decodedBytes, Charsets.UTF_8)
    return key
}

java {
    withSourcesJar()
}

val dokkaJar by tasks.registering(Jar::class) {
    val dokkaTask = tasks.named<org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml")
    from(dokkaTask.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            artifact(dokkaJar)
        }
    }
}

tasks.jreleaserDeploy {
    dependsOn(tasks.publish)
}

tasks.openApiGenerate {
    dependsOn(tasks.clean)
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

afterEvaluate {
    tasks.named<Jar>("sourcesJar") {
        dependsOn(tasks.openApiGenerate)
    }
}

// Dokka configuration
dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka"))
    }
}

tasks.dokkaGeneratePublicationHtml {
    dependsOn(tasks.openApiGenerate)
}
