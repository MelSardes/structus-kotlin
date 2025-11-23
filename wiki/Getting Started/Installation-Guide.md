# Installation Guide

This guide will help you add **Structus** - the Kotlin Architecture Toolkit to your project.

## 📋 Prerequisites

- **Kotlin**: 2.2.21 or higher
- **JDK**: 21 or higher
- **Build Tool**: Gradle (recommended) or Maven

## 🚧 Current Status

**The library is currently under active development and not yet published to Maven Central or GitHub Packages.**

To use the library, you need to build it from source and publish it to your local Maven repository.

## 🔨 Build from Source

### Step 1: Clone the Repository

```bash
git clone https://github.com/melsardes/structus-kotlin.git
cd structus-kotlin
```

### Step 2: Build and Publish to Local Maven

```bash
./gradlew publishToMavenLocal
```

This will build the library and install it to your local Maven repository (`~/.m2/repository`).

### Step 3: Use in Your Project

Once published locally, add the dependency to your project:

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

**Note**: Maven automatically checks your local repository (`~/.m2/repository`) by default.

## ✅ Verify Installation

Create a simple test file to verify the installation:

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

Run this file to confirm the library is properly installed.

## 📚 What's Included

The library includes the following packages:

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
│
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

## 🔄 Dependency Management

The library has minimal dependencies:

- **kotlinx-coroutines-core**: 1.9.0 (for suspend functions)
- **Kotlin stdlib**: Automatically included

No framework dependencies (Spring, Ktor, etc.) are included, keeping the library pure and framework-agnostic.

## 🚀 Next Steps

Now that you have the library installed:

1. **[Quick Start Tutorial](Quick-Start-Tutorial.md)** - Build your first application
2. **[Core Concepts](Core-Concepts.md)** - Understand the building blocks
3. **[Architecture Overview](Architecture-Overview.md)** - Learn the architectural principles

## 🐛 Troubleshooting

### Build fails with "Could not find com.melsardes.libraries:structus-kotlin"

**Solution**: Make sure you've run `./gradlew publishToMavenLocal` first and that `mavenLocal()` is in your repositories configuration.

### Compilation errors with suspend functions

**Solution**: Ensure you have kotlinx-coroutines-core in your dependencies and Kotlin 2.2.0 or higher.

### "Explicit API mode" errors in your code

**Solution**: The library uses explicit API mode, but your code doesn't need to. If you want to enable it:

```kotlin
kotlin {
    explicitApi()
}
```

## 📞 Support

If you encounter issues:

- Check the [FAQ](FAQ.md)
- Search [GitHub Issues](https://github.com/melsardes/structus-kotlin/issues)
- Ask in [GitHub Discussions](https://github.com/melsardes/structus-kotlin/discussions)
