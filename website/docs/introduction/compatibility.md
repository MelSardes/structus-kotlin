---
sidebar_position: 2
---

# Compatibility

This page provides information about compatibility between Structus versions and various dependencies.

## Kotlin Compatibility

| Structus Version | Kotlin Version | Notes                                  |
|------------------|---------------|----------------------------------------|
| 0.1.0            | 1.8.0+        | Requires Kotlin 1.8.0 or higher        |
| 0.2.0 (upcoming) | 1.9.0+        | Will use Kotlin 1.9 features           |

## JDK Compatibility

| Structus Version | JDK Version | Notes                                   |
|------------------|-------------|------------------------------------------|
| 0.1.0            | JDK 11+     | Compatible with JDK 11 through JDK 21   |
| 0.2.0 (upcoming) | JDK 11+     | Compatible with JDK 11 through JDK 21   |

## Coroutines Compatibility

| Structus Version | Coroutines Version | Notes                               |
|------------------|-------------------|--------------------------------------|
| 0.1.0            | 1.6.4+            | Requires kotlinx.coroutines 1.6.4+   |
| 0.2.0 (upcoming) | 1.7.0+            | Will use newer coroutines features   |

## Spring Boot Compatibility

While Structus is framework-agnostic, here's the compatibility with Spring Boot for reference:

| Structus Version | Spring Boot Version | Notes                             |
|------------------|---------------------|-----------------------------------|
| 0.1.0            | 2.7.x, 3.x         | Compatible with both versions      |
| 0.2.0 (upcoming) | 3.x                | Will focus on Spring Boot 3        |

## Ktor Compatibility

| Structus Version | Ktor Version | Notes                                   |
|------------------|--------------|------------------------------------------|
| 0.1.0            | 2.x          | Compatible with Ktor 2.x                |
| 0.2.0 (upcoming) | 2.x          | Will continue supporting Ktor 2.x       |

## Gradle Compatibility

| Structus Version | Gradle Version | Notes                                |
|------------------|----------------|--------------------------------------|
| 0.1.0            | 7.0+           | Compatible with Gradle 7.0 and above |
| 0.2.0 (upcoming) | 7.3+           | Will require Gradle 7.3 and above    |

## Maven Compatibility

| Structus Version | Maven Version | Notes                                |
|------------------|--------------|---------------------------------------|
| 0.1.0            | 3.6+         | Compatible with Maven 3.6 and above   |
| 0.2.0 (upcoming) | 3.6+         | Will continue supporting Maven 3.6+   |

## Operating System Compatibility

Structus is compatible with any operating system that supports the JVM:

- Windows
- macOS
- Linux
- Any other OS with JVM support

## Multiplatform Support

Currently, Structus is designed for JVM only. Multiplatform support is on our roadmap:

| Structus Version | Platforms    | Notes                                   |
|------------------|--------------|------------------------------------------|
| 0.1.0            | JVM only     | No multiplatform support                |
| 0.2.0 (upcoming) | JVM only     | No multiplatform support                |
| Future versions  | JVM, JS, Native | Planned for future releases            |

## IDE Support

Structus is developed with IDE support in mind:

| IDE             | Support Level | Notes                                   |
|-----------------|--------------|------------------------------------------|
| IntelliJ IDEA   | Full         | Recommended IDE for development          |
| Android Studio  | Full         | Based on IntelliJ IDEA                   |
| Eclipse         | Partial      | Basic functionality supported            |
| VS Code         | Partial      | With Kotlin extension                    |

## Experimental Features

Features marked as experimental in Structus have no compatibility guarantees between versions and may change without notice.

## Migration

When upgrading between Structus versions, please refer to our [Migration Guide](../migration-guide) for detailed instructions.