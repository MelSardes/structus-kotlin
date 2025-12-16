/*
 * Structus - Kotlin Architecture Toolkit (structus-kotlin)
 * 
 * A pure Kotlin JVM library providing the foundational building blocks for
 * implementing Explicit Architecture - a synthesis of Domain-Driven Design (DDD),
 * Command/Query Separation (CQS), and Event-Driven Architecture (EDA).
 * 
 * CONSTRAINT: This library is intentionally dependency-free (except for Kotlin stdlib
 * and kotlinx-coroutines-core) to remain framework-agnostic and reusable across any
 * technology stack (Spring, Ktor, Micronaut, etc.).
 */

import java.util.Properties

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    
    // Maven publishing for distribution
    `maven-publish`
    
    // JaCoCo for code coverage
    jacoco
}

// Load local properties if file exists (for sensitive credentials)
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    val props = Properties().apply {
        localPropertiesFile.inputStream().use { load(it) }
    }
    props.stringPropertyNames().forEach { key ->
        project.ext.set(key, props.getProperty(key))
    }
}

// Project metadata - read from gradle.properties
group = project.property("group") as String
version = project.property("version") as String

java {
    withSourcesJar()
    withJavadocJar()
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // MANDATORY CONSTRAINT: Only Kotlin stdlib and coroutines are allowed
    // This ensures the library remains pure and framework-agnostic
    
    // Kotlin Coroutines for async/suspend function support
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    
    // NO OTHER DEPENDENCIES ALLOWED
    // No Spring, Ktor, R2DBC, JDBC, Hibernate, or any framework-specific libraries
    
    // Test dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.7")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("2.2.21")
            // Use JUnit Platform
            useJUnitJupiter()
        }
    }
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.12"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal() // 50% minimum coverage
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Configure Kotlin compiler options
kotlin {
    compilerOptions {
        // Enable explicit API mode to enforce documentation (main sources only)
        freeCompilerArgs.add("-Xexplicit-api=strict")
        optIn.add("kotlin.time.ExperimentalTime")
    }
    
    // Disable explicit API mode for test sources
    target.compilations.getByName("test").compilerOptions.configure {
        freeCompilerArgs.set(emptyList())
    }
}

// Configure JAR base name to match the library name
tasks.withType<Jar> {
    archiveBaseName.set("structus-kotlin")
}

tasks.jar {
    manifest {
        attributes(mapOf(
            "Implementation-Title" to "Structus - Kotlin Architecture Toolkit",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Structus Project"
        ))
    }
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.melsardes.libraries"
            artifactId = "structus-kotlin"
            version = project.version.toString()
            
            from(components["java"])
            
            pom {
                name.set("Structus - Kotlin Architecture Toolkit")
                description.set("A pure Kotlin library for implementing Explicit Architecture with DDD, CQS, and EDA patterns")
                url.set("https://github.com/MelSardes/structus-kotlin")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("MelSardes")
                        name.set("Mel Sardes")
                        email.set("dev.melsardes@gmail.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/MelSardes/structus-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/MelSardes/structus-kotlin.git")
                    url.set("https://github.com/MelSardes/structus-kotlin")
                }
                
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/MelSardes/structus-kotlin/issues")
                }
                
                ciManagement {
                    system.set("GitHub Actions")
                    url.set("https://github.com/MelSardes/structus-kotlin/actions")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MelSardes/structus-kotlin")
            credentials {
                username = System.getenv("GITHUB_ACTOR") 
                    ?: project.findProperty("gpr.user") as String?
                    ?: project.ext.properties["gpr.user"] as String?
                password = System.getenv("GITHUB_TOKEN") 
                    ?: project.findProperty("gpr.token") as String?
                    ?: project.ext.properties["gpr.token"] as String?
            }
        }
    }
}