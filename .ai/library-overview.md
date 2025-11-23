# Structus Library Overview for AI Agents

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
│         Presentation Layer              │  ← Controllers, DTOs, API
│  (Framework-specific, not in library)   │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        Application Layer                │  ← Commands, Queries, Handlers
│   com.melsardes.libraries.structuskotlin.application    │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Domain Layer                   │  ← Entities, Value Objects, Events
│     com.melsardes.libraries.structuskotlin.domain       │     Repository Interfaces
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Infrastructure Layer               │  ← Repository Implementations
│  (Framework-specific, not in library)   │     Database, External APIs
└─────────────────────────────────────────┘
```

## 📦 Core Components

### Domain Layer (`com.melsardes.libraries.structuskotlin.domain`)

#### 1. **Entity**
Base class for domain entities with identity.

```kotlin
abstract class Entity<ID : Any>(open val id: ID)
```

**When to use**: Objects with unique identity that persist over time.

#### 2. **AggregateRoot**
Special entity that is the entry point to an aggregate.

```kotlin
abstract class AggregateRoot<ID : Any>(id: ID) : Entity<ID>(id)
```

**When to use**: Root entity that controls access to its aggregate.

#### 3. **ValueObject**
Marker interface for immutable value objects.

```kotlin
interface ValueObject
```

**When to use**: Objects defined by their attributes, not identity (e.g., Address, Money).

#### 4. **DomainEvent**
Base interface for domain events.

```kotlin
interface DomainEvent {
    val eventId: String
    val occurredOn: Instant
    val aggregateId: String
    val eventType: String
}
```

**When to use**: To represent something that happened in the domain.

#### 5. **Repository Interfaces**
Define contracts for persistence without implementation details.

```kotlin
interface Repository<T : AggregateRoot<ID>, ID : Any>
interface CommandRepository<T : AggregateRoot<ID>, ID : Any> : Repository<T, ID>
interface QueryRepository<T : AggregateRoot<ID>, ID : Any> : Repository<T, ID>
```

**When to use**: Always define repository interfaces in the domain layer.

### Application Layer (`com.melsardes.libraries.structuskotlin.application`)

#### 1. **Command**
Represents an intent to change state.

```kotlin
interface Command
```

**When to use**: For write operations (create, update, delete).

#### 2. **CommandHandler**
Processes commands and changes state.

```kotlin
interface CommandHandler<C : Command, R> {
    suspend fun handle(command: C): Result<R>
}
```

**When to use**: To implement business logic for commands.

#### 3. **Query**
Represents a request for data.

```kotlin
interface Query<R>
```

**When to use**: For read operations (get, list, search).

#### 4. **QueryHandler**
Processes queries and returns data.

```kotlin
interface QueryHandler<Q : Query<R>, R> {
    suspend fun handle(query: Q): Result<R>
}
```

**When to use**: To implement data retrieval logic.

#### 5. **DomainEventPublisher**
Publishes domain events to external systems.

```kotlin
interface DomainEventPublisher {
    suspend fun publish(event: DomainEvent): Result<Unit>
    suspend fun publishAll(events: List<DomainEvent>): Result<Unit>
}
```

**When to use**: To notify external systems of domain changes.

## 🔄 Key Patterns

### 1. **CQRS (Command Query Responsibility Segregation)**

**Principle**: Separate read and write operations.

- **Commands**: Change state, return success/failure
- **Queries**: Read data, never change state

### 2. **Result Pattern**

All operations return `Result<T>` instead of throwing exceptions:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: DomainError) : Result<Nothing>()
}
```

**Why**: Makes error handling explicit and type-safe.

### 3. **Transactional Outbox Pattern**

Domain events are stored in an outbox table and published asynchronously:

```kotlin
interface MessageOutboxRepository {
    suspend fun save(message: OutboxMessage): Result<Unit>
    suspend fun findPendingMessages(limit: Int): Result<List<OutboxMessage>>
    suspend fun markAsPublished(messageId: String): Result<Unit>
}
```

**Why**: Ensures reliable event delivery and eventual consistency.

### 4. **Aggregate Pattern**

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
2. **Return Result<T>** for operations that can fail
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
- Match command/query name: `CreateUserCommandHandler`, `GetUserByIdQueryHandler`
- Suffix with `Handler`

### Events
- Use past tense: `UserCreated`, `ProfileUpdated`, `AccountDeleted`
- Suffix with `Event`: `UserCreatedEvent`

### Repositories
- Use entity name: `UserRepository`
- Split into command/query: `UserCommandRepository`, `UserQueryRepository`

## 🔧 Integration Points

### With Spring Boot
```kotlin
@Service
class CreateUserCommandHandler(
    private val userRepository: UserCommandRepository
) : CommandHandler<CreateUserCommand, UserId> {
    override suspend fun handle(command: CreateUserCommand): Result<UserId> {
        // Implementation
    }
}
```

### With Ktor
```kotlin
fun Route.userRoutes(handler: CreateUserCommandHandler) {
    post("/users") {
        val request = call.receive<CreateUserRequest>()
        val command = request.toCommand()
        when (val result = handler.handle(command)) {
            is Result.Success -> call.respond(HttpStatusCode.Created, result.value)
            is Result.Failure -> call.respond(HttpStatusCode.BadRequest, result.error)
        }
    }
}
```

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
| Notify of changes | `DomainEvent` + `DomainEventPublisher` | `domain.events` + `application.events` |
| Store events reliably | `MessageOutboxRepository` | `domain` |

## 🔗 Related Files

- [Usage Patterns](./usage-patterns.md) - Common implementation patterns
- [Code Templates](./code-templates.md) - Ready-to-use code
- [Troubleshooting](./troubleshooting.md) - Common issues

---

**Remember**: Structus is about enforcing clean architecture. Always ask: "Which layer does this belong to?"
