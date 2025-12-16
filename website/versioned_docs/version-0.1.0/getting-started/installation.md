---
sidebar_position: 1
pagination_next: getting-started/quick-start
pagination_prev: about
---

# Installation Guide

This guide will help you add **Structus** to your Kotlin project.

## Prerequisites

- **Kotlin**: 2.2.0 or higher
- **JDK**: 21 or higher
- **Build Tool**: Gradle (recommended) or Maven

## Current Status

:::warning Development Status
The library is currently under active development and not yet published to Maven Central or GitHub Packages.
:::

To use the library, you need to build it from source and publish it to your local Maven repository.

## Build from Source

### 1. Clone the Repository

```bash
git clone https://github.com/melsardes/structus-kotlin.git
cd structus-kotlin
```

### 2. Build and Publish Locally

```bash
./gradlew publishToMavenLocal
```

This installs the library to your local Maven repository (`~/.m2/repository`).

### 3. Add to Your Project

#### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenLocal()  // Important: Add local Maven repository
    mavenCentral()
}

dependencies {
    implementation("com.melsardes.libraries:structus-kotlin:0.1.0")
}
```

#### Gradle (Groovy DSL)

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.melsardes.libraries:structus-kotlin:0.1.0'
}
```

#### Maven

```xml
<dependencies>
    <dependency>
        <groupId>com.melsardes.libraries</groupId>
        <artifactId>structus-kotlin</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

:::tip
Maven automatically checks your local repository (`~/.m2/repository`) by default.
:::

## Verify Installation

Create a test file to verify the installation:

```kotlin
import com.melsardes.libraries.structuskotlin.domain.Entity
import com.melsardes.libraries.structuskotlin.domain.AggregateRoot

data class TestId(val value: String)

class TestEntity(override val id: TestId) : Entity<TestId>()

fun main() {
    val entity = TestEntity(TestId("test-123"))
    println("Installation successful! Entity ID: ${entity.id.value}")
}
```

## What's Included

The library provides these core packages:

```
com.melsardes.libraries.structuskotlin
├── domain/
│   ├── Entity
│   ├── ValueObject
│   ├── AggregateRoot
│   ├── Repository
│   ├── MessageOutboxRepository
│   └── events/
│       ├── DomainEvent
│       └── BaseDomainEvent
└── application/
    ├── commands/
    │   ├── Command
    │   ├── CommandHandler
    │   └── CommandBus
    ├── queries/
    │   ├── Query
    │   └── QueryHandler
    └── events/
        ├── DomainEventPublisher
        └── DomainEventHandler
```

## Dependencies

Structus has minimal dependencies:

- **kotlinx-coroutines-core**: 1.9.0
- **Kotlin stdlib**: Included automatically

:::info Framework Agnostic
No framework dependencies (Spring, Ktor, etc.) are included, keeping the library pure and framework-agnostic.
:::

## Troubleshooting

### Build fails with "Could not find structus-kotlin"

**Solution**: Ensure you've run `./gradlew publishToMavenLocal` and added `mavenLocal()` to your repositories.

### Compilation errors with suspend functions

**Solution**: Verify you have:
- kotlinx-coroutines-core in dependencies
- Kotlin 2.2.0 or higher

### "Explicit API mode" errors

**Solution**: The library uses explicit API mode, but your code doesn't need to. To enable it in your project:

```kotlin
kotlin {
    explicitApi()
}
```

## Next Steps

- **[Quick Start Tutorial](quick-start)** - Build your first application
- **[Core Concepts](core-concepts)** - Understand the building blocks
- **[Architecture Overview](../architecture/overview)** - Learn the architectural principles

## Support

Need help?

- 📖 [Documentation](../intro)
- 🐛 [GitHub Issues](https://github.com/melsardes/structus-kotlin/issues)
- 💬 [Discussions](https://github.com/melsardes/structus-kotlin/discussions)
