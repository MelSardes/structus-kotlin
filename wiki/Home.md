# Structus - Kotlin Architecture Toolkit - Wiki

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](../LICENSE)
[![Version](https://img.shields.io/badge/Version-0.1.0-orange.svg)](https://github.com/structus-io/structus-kotlin)

Welcome to the **Structus** wiki! This comprehensive documentation will guide you through using this pure Kotlin library for implementing Domain-Driven Design (DDD), Command/Query Separation (CQS), and Event-Driven Architecture (EDA).

## 📚 Table of Contents

### Getting Started
- **[Installation Guide](Getting%20Started/Installation-Guide.md)** - How to add the library to your project
- **[Quick Start Tutorial](Getting%20Started/Quick-Start-Tutorial.md)** - Build your first application in 15 minutes
- **[Core Concepts](Getting%20Started/Core-Concepts.md)** - Understanding the fundamental building blocks

### Architecture & Design
- **[Architecture Overview](Architecture/Architecture-Overview.md)** - High-level architecture principles
- **[Layer Responsibilities](Architecture/Layer-Responsibilities.md)** - Understanding the four architectural layers
- **[Dependency Rules](Architecture/Dependency-Rules.md)** - How layers interact and depend on each other
- **[Design Patterns](Architecture/Design-Patterns.md)** - Common patterns and their implementations

### Domain Layer
- **[Entities](Domain-Entities.md)** - Identity-based domain objects
- **[Value Objects](Domain-Value-Objects.md)** - Immutable attribute-based objects
- **[Aggregate Roots](Domain-Aggregate-Roots.md)** - Consistency boundaries with event management
- **[Domain Events](Domain-Events.md)** - Capturing what happened in the domain
- **[Repositories](Domain-Repositories.md)** - Persistence contracts

### Application Layer
- **[Commands](Application-Commands.md)** - Write operations and state changes
- **[Command Handlers](Application-Command-Handlers.md)** - Executing business logic
- **[Command Bus](Application-Command-Bus.md)** - Dispatching commands to handlers
- **[Queries](Application-Queries.md)** - Read operations and data retrieval
- **[Query Handlers](Application-Query-Handlers.md)** - Optimized data access
- **[Event Publishing](Application-Event-Publishing.md)** - Publishing events to external systems

### Advanced Topics
- **[Transactional Outbox Pattern](Advanced%20Topics/Transactional-Outbox-Pattern.md)** - Solving the dual-write problem
- **[CQRS Implementation](Advanced%20Topics/CQRS-Implementation.md)** - Separating reads from writes
- **[Event Sourcing](Advanced%20Topics/Event-Sourcing.md)** - Building event-sourced systems
- **[Aggregate Lifecycle](Advanced%20Topics/Aggregate-Lifecycle.md)** - Creation, updates, soft deletes, and restoration
- **[Error Handling](Advanced%20Topics/Error-Handling.md)** - Using Result types and exceptions

### Framework Integration
- **[Spring Boot Integration](Spring-Boot-Integration.md)** - Using with Spring Boot
- **[Ktor Integration](Ktor-Integration.md)** - Using with Ktor
- **[Micronaut Integration](Micronaut-Integration.md)** - Using with Micronaut
- **[Pure Kotlin Setup](Pure-Kotlin-Setup.md)** - Framework-agnostic implementation

### Best Practices
- **[Best Practices](Best%20Practices/Best-Practices.md)** - Essential best practices and naming conventions
- **[Testing Strategies](Best%20Practices/Testing-Strategies.md)** - Unit, integration, and property-based testing
- **[Common Mistakes](Best%20Practices/Common-Mistakes.md)** - Pitfalls to avoid
- **[Performance Tips](Best%20Practices/Performance-Tips.md)** - Optimizing your architecture
- **[Design Patterns](Best%20Practices/Design-Patterns.md)** - Proven solutions to common problems

### Development
- **[Building from Source](Development/Building-From-Source.md)** - Compiling the library
- **[Contributing Guide](Development/Contributing-Guide.md)** - How to contribute to the project
- **[Release Process](Development/Release-Process.md)** - Semantic versioning and releases
- **[Roadmap](Reference/Roadmap.md)** - Future enhancements and features

### Reference
- **[API Reference](Reference/API-Reference.md)** - Complete API documentation
- **[Migration Guides](Reference/Migration-Guides.md)** - Upgrading between versions
- **[FAQ](Reference/FAQ.md)** - Frequently asked questions
- **[Glossary](Reference/Glossary.md)** - Terms and definitions
- **[Resources](Reference/Resources.md)** - External links and learning materials links

## 🎯 What is Explicit Architecture?

Explicit Architecture is a synthesis of several proven architectural patterns:

- **Domain-Driven Design (DDD)** - Focus on the core domain and domain logic
- **Clean Architecture** - Dependency inversion and layer separation
- **Hexagonal Architecture** - Ports and adapters (without the terminology)
- **Command/Query Separation (CQS)** - Separate reads from writes
- **Event-Driven Architecture (EDA)** - React to domain events

This library provides the **kernel** - the foundational building blocks that enforce these principles while remaining completely framework-agnostic.

## ✨ Key Features

- 🚀 **Pure Kotlin** - No framework dependencies (Spring, Ktor, Micronaut, etc.)
- 🔄 **Coroutine-Ready** - All I/O operations use suspend functions
- 📦 **Minimal Dependencies** - Only Kotlin stdlib + kotlinx-coroutines-core
- 📚 **Comprehensive Documentation** - Every component includes KDoc and examples
- 🏗️ **Framework-Agnostic** - Works with any framework or pure Kotlin
- 🎨 **Clean Architecture** - Enforces proper layer separation and dependencies

## 🚀 Quick Example

```kotlin
// Domain: Aggregate Root with events
class User(
    override val id: UserId,
    var email: Email,
    var status: UserStatus
) : AggregateRoot<UserId>() {
    
    fun register() {
        this.status = UserStatus.ACTIVE
        recordEvent(UserRegisteredEvent(
            aggregateId = id.value,
            email = email.value
        ))
    }
}

// Application: Command Handler
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

## 📖 Where to Start?

1. **New to the library?** Start with the [Installation Guide](Getting%20Started/Installation-Guide.md) and [Quick Start Tutorial](Getting%20Started/Quick-Start-Tutorial.md)
2. **Want to understand the concepts?** Read [Core Concepts](Getting%20Started/Core-Concepts.md) and [Architecture Overview](Architecture/Architecture-Overview.md)
3. **Ready to build?** Check out the domain and application layer guides
4. **Need specific patterns?** Jump to [Advanced Topics](#advanced-topics)
5. **Integrating with a framework?** See [Framework Integration](#framework-integration)

## 🤝 Community & Support

- **GitHub Repository**: [melsardes/structus-kotlin](https://github.com/structus-io/structus-kotlin)
- **Issues**: [Report bugs or request features](https://github.com/structus-io/structus-kotlin/issues)
- **Discussions**: [Ask questions and share ideas](https://github.com/structus-io/structus-kotlin/discussions)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

---

**Made with ❤️ for the Kotlin community**
