---
sidebar_position: 1
slug: /intro
---

# Introduction to Structus

Welcome to **Structus** - a pure Kotlin JVM library providing the foundational building blocks for implementing **Explicit Architecture**.

## What is Structus?

Structus is a **shared kernel** for large-scale projects that synthesizes:

- 🏛️ **Domain-Driven Design (DDD)**
- 📝 **Command/Query Separation (CQS)**
- 📡 **Event-Driven Architecture (EDA)**

It defines interfaces and base classes for all core business concepts and architectural patterns while remaining completely **framework-agnostic**.

## Key Features

- 🚀 **Pure Kotlin**: No framework dependencies (Spring, Ktor, Micronaut, etc.)
- 🔄 **Coroutine-Ready**: All I/O operations use suspend functions
- 📦 **Minimal Dependencies**: Only Kotlin stdlib + kotlinx-coroutines-core
- 📚 **Comprehensive Documentation**: Every component includes KDoc and examples
- 🏗️ **Framework-Agnostic**: Works with any framework or pure Kotlin
- 🎨 **Clean Architecture**: Enforces proper layer separation and dependencies

## Why Explicit Architecture?

Explicit Architecture helps you:

1. **Separate Concerns**: Clear boundaries between domain, application, and infrastructure
2. **Testability**: Easy to test business logic in isolation
3. **Flexibility**: Switch frameworks or databases without rewriting business logic
4. **Maintainability**: Code is organized and predictable
5. **Scalability**: Architecture scales with your team and codebase

## Architecture Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│    (Controllers, DTOs, REST APIs)       │
└─────────────────────────────────────────┘
              ↓ depends on
┌─────────────────────────────────────────┐
│        Application Layer                │
│  (Commands, Queries, Handlers, Events)  │
└─────────────────────────────────────────┘
              ↓ depends on
┌─────────────────────────────────────────┐
│          Domain Layer                   │
│  (Entities, Aggregates, Value Objects)  │
└─────────────────────────────────────────┘
              ↑ implemented by
┌─────────────────────────────────────────┐
│      Infrastructure Layer               │
│  (Database, External APIs, Messaging)   │
└─────────────────────────────────────────┘
```

## Quick Example

Here's a taste of what Structus code looks like:

```kotlin
// Domain Layer - Pure business logic
class User(
    override val id: UserId,
    var email: Email,
    var name: String
) : AggregateRoot<UserId>() {
    
    fun activate() {
        status = UserStatus.ACTIVE
        recordEvent(UserActivatedEvent(id.value))
    }
}

// Application Layer - Use case
class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
        return runCatching {
            val user = User.create(Email(command.email), command.name)
            userRepository.save(user)
            
            // Transactional Outbox Pattern
            user.domainEvents.forEach { outboxRepository.save(it) }
            user.clearEvents()
            
            user.id
        }
    }
}
```

## Next Steps

Ready to get started? Here's what to do next:

1. **[About Structus](about)** - Learn about Structus and its creator
2. **[Installation Guide](getting-started/installation)** - Set up Structus in your project
3. **[Quick Start Tutorial](getting-started/quick-start)** - Build your first app in 15 minutes
4. **[Core Concepts](getting-started/core-concepts)** - Understand the fundamental concepts
5. **[Architecture Overview](architecture/overview)** - Deep dive into the architecture
6. **[Roadmap](roadmap)** - Explore upcoming Structus ecosystem projects

## Community & Support

- 💬 [GitHub Discussions](https://github.com/melsardes/structus-kotlin/discussions)
- 🐛 [Issue Tracker](https://github.com/melsardes/structus-kotlin/issues)
- ⭐ [Star on GitHub](https://github.com/melsardes/structus-kotlin)

---

**Made with ❤️ for the Kotlin community**
