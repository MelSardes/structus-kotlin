---
sidebar_position: 2
---

# Library Overview for AI Agents

This guide helps AI coding assistants understand Structus architecture and generate correct code.

## 🎯 What is Structus?

**Structus** is a pure Kotlin library providing building blocks for **Explicit Architecture** - a synthesis of:
- **Domain-Driven Design (DDD)**
- **Command/Query Separation (CQS)**
- **Event-Driven Architecture (EDA)**

**Key Constraint**: The library is **framework-agnostic** with ZERO dependencies (except Kotlin stdlib and coroutines).

## 🏛️ Architecture Layers

Structus enforces a strict 4-layer architecture:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Controllers, DTOs, API endpoints)     │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        Application Layer                │
│   (Commands, Queries, Handlers)         │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Domain Layer                   │
│   (Entities, Value Objects, Events)     │
└─────────────────────────────────────────┘
                    ↑
┌─────────────────────────────────────────┐
│      Infrastructure Layer               │
│   (Repository Implementations, DB)      │
└─────────────────────────────────────────┘
```

## 📦 Core Components

### Domain Layer

Package: `com.melsardes.libraries.structuskotlin.domain`

#### Entity
Base class for domain entities with identity.

```kotlin
abstract class Entity<ID> {
    abstract val id: ID
    // Equality based on ID
}
```

**When to use**: Objects with unique identity that persist over time (User, Order, Product).

#### AggregateRoot
Special entity that is the entry point to an aggregate.

```kotlin
abstract class AggregateRoot<ID> : Entity<ID>() {
    val domainEvents: List<DomainEvent>
    protected fun recordEvent(event: DomainEvent)
    fun clearEvents()
}
```

**When to use**: Root entity that controls access to its aggregate and manages domain events.

#### ValueObject
Marker interface for immutable value objects.

```kotlin
interface ValueObject
```

**When to use**: Objects defined by their attributes, not identity (Email, Money, Address).

#### DomainEvent
Base interface for domain events.

```kotlin
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val aggregateId: String
}
```

**When to use**: To represent something that happened in the domain.

#### Repository Interfaces
Define contracts for persistence without implementation details.

```kotlin
interface Repository
```

**When to use**: Always define repository interfaces in the domain layer.

### Application Layer

Package: `com.melsardes.libraries.structuskotlin.application`

#### Command
Represents an intent to change state.

```kotlin
interface Command
```

**When to use**: For write operations (create, update, delete).

#### CommandHandler
Processes commands and changes state.

```kotlin
interface CommandHandler<in C : Command, out R> {
    suspend operator fun invoke(command: C): R
}
```

**When to use**: To implement business logic for commands.

#### Query
Represents a request for data.

```kotlin
interface Query
```

**When to use**: For read operations (get, list, search).

#### QueryHandler
Processes queries and returns data.

```kotlin
interface QueryHandler<in Q : Query, out R> {
    suspend operator fun invoke(query: Q): R
}
```

**When to use**: To implement data retrieval logic.

#### DomainEventPublisher
Publishes domain events to external systems.

```kotlin
interface DomainEventPublisher {
    suspend fun publish(event: DomainEvent)
    suspend fun publishBatch(events: List<DomainEvent>)
}
```

**When to use**: To notify external systems of domain changes.

## 🔄 Key Patterns

### 1. CQRS (Command Query Responsibility Segregation)

**Principle**: Separate read and write operations.

- **Commands**: Change state, return success/failure
- **Queries**: Read data, never change state

### 2. Result Pattern

All operations return `Result<T>` instead of throwing exceptions:

```kotlin
val result: Result<UserId> = handler(command)
result.fold(
    onSuccess = { userId -> /* handle success */ },
    onFailure = { error -> /* handle error */ }
)
```

**Why**: Makes error handling explicit and type-safe.

### 3. Transactional Outbox Pattern

Domain events are stored in an outbox table and published asynchronously:

```kotlin
interface MessageOutboxRepository : Repository {
    suspend fun save(event: DomainEvent)
    suspend fun findUnpublished(limit: Int): List<OutboxMessage>
    suspend fun markAsPublished(messageId: String)
}
```

**Why**: Ensures reliable event delivery and eventual consistency.

### 4. Aggregate Pattern

- One aggregate = one transaction boundary
- External references use IDs only
- Enforce invariants within aggregate

## 🚫 Critical Constraints

### ❌ What NOT to do:

1. **Don't add framework dependencies** to domain/application layers
2. **Don't use exceptions** for business logic errors (use Result)
3. **Don't bypass aggregate roots** to modify child entities
4. **Don't mix commands and queries** in the same handler
5. **Don't put business logic** in controllers or repositories

### ✅ What TO do:

1. **Use suspend functions** for all I/O operations
2. **Return Result&lt;T&gt;** for operations that can fail
3. **Define repository interfaces** in domain layer
4. **Implement repositories** in infrastructure layer
5. **Keep domain layer pure** - no framework code

## 📝 Naming Conventions

### Commands
- Use imperative verbs: `CreateUser`, `UpdateProfile`, `DeleteAccount`
- Suffix with `Command`: `CreateUserCommand`

### Queries
- Use descriptive nouns: `UserById`, `UserList`, `UserSearch`
- Suffix with `Query`: `GetUserByIdQuery`

### Handlers
- Match command/query name + `Handler`
- Examples: `CreateUserCommandHandler`, `GetUserByIdQueryHandler`

### Events
- Use past tense: `UserCreated`, `ProfileUpdated`, `AccountDeleted`
- Suffix with `Event`: `UserCreatedEvent`

### Repositories
- Use entity name: `UserRepository`
- Can split into command/query: `UserCommandRepository`, `UserQueryRepository`

## 🎓 Learning Path for AI Agents

When helping a developer with Structus:

1. **Identify the layer**: Domain, Application, or Infrastructure?
2. **Choose the pattern**: Command, Query, or Event?
3. **Use the right base class**: Entity, ValueObject, AggregateRoot?
4. **Follow conventions**: Naming, structure, error handling
5. **Maintain purity**: No framework code in domain/application

## 📚 Quick Reference

| Task | Use | Package |
|------|-----|---------|
| Create entity with identity | `Entity<ID>` or `AggregateRoot<ID>` | `domain` |
| Create immutable value | `data class X : ValueObject` | `domain` |
| Define persistence contract | `interface XRepository` | `domain` |
| Change state | `Command` + `CommandHandler` | `application.commands` |
| Read data | `Query` + `QueryHandler` | `application.queries` |
| Notify of changes | `DomainEvent` + Publisher | `domain.events` |
| Store events reliably | `MessageOutboxRepository` | `domain` |

## 🔗 Next Steps

- **[Usage Patterns](usage-patterns)** - Common implementation patterns
- **[Code Templates](code-templates)** - Ready-to-use code
- **[AI Prompts](prompts)** - Prompt templates for specific tasks

---

**Remember**: Structus is about enforcing clean architecture. Always ask: "Which layer does this belong to?"
