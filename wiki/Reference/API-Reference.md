# API Reference

Complete reference for all public APIs in Structus - Kotlin Architecture Toolkit.

## 📦 Package Structure

```
com.melsardes.libraries.structuskotlin
├── domain/
│   ├── Entity<ID>
│   ├── ValueObject
│   ├── AggregateRoot<ID>
│   ├── Repository
│   ├── MessageOutboxRepository
│   ├── OutboxMessage
│   └── events/
│       ├── DomainEvent
│       └── BaseDomainEvent
│
└── application/
    ├── commands/
    │   ├── Command
    │   ├── CommandHandler<C, R>
    │   └── CommandBus
    ├── queries/
    │   ├── Query
    │   └── QueryHandler<Q, R>
    └── events/
        ├── DomainEventPublisher
        └── DomainEventHandler<E>
```

---

## Domain Layer

### Entity<ID>

Abstract base class for identity-based domain objects.

```kotlin
abstract class Entity<ID> {
    abstract val id: ID
    
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}
```

**Type Parameters**:
- `ID` - Type of the entity's identifier

**Properties**:
- `id: ID` - Unique identifier for the entity

**Methods**:
- `equals(other: Any?): Boolean` - Equality based on ID
- `hashCode(): Int` - Hash code based on ID
- `toString(): String` - String representation

**Example**:
```kotlin
data class UserId(val value: String)

class User(
    override val id: UserId,
    var name: String
) : Entity<UserId>()

val user1 = User(UserId("123"), "John")
val user2 = User(UserId("123"), "Jane")
println(user1 == user2)  // true - same ID
```

---

### ValueObject

Marker interface for immutable, attribute-based objects.

```kotlin
interface ValueObject
```

**Usage**:
- Implement with Kotlin data classes
- Ensure immutability
- Add validation in `init` block

**Example**:
```kotlin
data class Email(val value: String) : ValueObject {
    init {
        require(value.contains("@")) { "Invalid email" }
    }
}

data class Money(
    val amount: Double,
    val currency: String
) : ValueObject {
    init {
        require(amount >= 0) { "Amount cannot be negative" }
    }
}
```

---

### AggregateRoot<ID>

Abstract base class for aggregate roots with event management.

```kotlin
abstract class AggregateRoot<ID> : Entity<ID>() {
    val domainEvents: List<DomainEvent>
    
    protected fun recordEvent(event: DomainEvent)
    fun clearEvents()
    fun eventCount(): Int
    fun hasEvents(): Boolean
    
    // Lifecycle methods
    internal fun markAsCreated(by: String, at: Instant = Clock.System.now())
    internal fun markAsUpdated(by: String, at: Instant = Clock.System.now())
    fun softDelete(by: String, at: Instant = Clock.System.now())
    fun restore(by: String, at: Instant = Clock.System.now())
    fun isDeleted(): Boolean
    fun isActive(): Boolean
    internal fun incrementVersion()
}
```

**Properties**:
- `domainEvents: List<DomainEvent>` - Read-only list of recorded events

**Methods**:
- `recordEvent(event: DomainEvent)` - Record a domain event (protected)
- `clearEvents()` - Clear all recorded events
- `eventCount(): Int` - Number of recorded events
- `hasEvents(): Boolean` - Check if any events are recorded

**Lifecycle Methods**:
- `markAsCreated(by: String, at: Instant)` - Mark as created (internal)
- `markAsUpdated(by: String, at: Instant)` - Mark as updated (internal)
- `softDelete(by: String, at: Instant)` - Soft delete the aggregate
- `restore(by: String, at: Instant)` - Restore a soft-deleted aggregate
- `isDeleted(): Boolean` - Check if soft-deleted
- `isActive(): Boolean` - Check if active (not deleted)
- `incrementVersion()` - Increment version for optimistic locking (internal)

**Example**:
```kotlin
class Order(
    override val id: OrderId,
    var status: OrderStatus
) : AggregateRoot<OrderId>() {
    
    fun place() {
        require(status == OrderStatus.DRAFT)
        status = OrderStatus.PLACED
        recordEvent(OrderPlacedEvent(id.value))
    }
}

val order = Order(OrderId("123"), OrderStatus.DRAFT)
order.place()
println(order.eventCount())  // 1
order.clearEvents()
println(order.hasEvents())  // false
```

---

### Repository

Marker interface for all repository contracts.

```kotlin
interface Repository
```

**Usage**:
- Define repository interfaces in the domain layer
- Implement in the infrastructure layer
- Use suspend functions for async operations

**Example**:
```kotlin
interface UserRepository : Repository {
    suspend fun findById(id: UserId): User?
    suspend fun save(user: User)
    suspend fun delete(id: UserId)
}
```

---

### MessageOutboxRepository

Repository for the Transactional Outbox Pattern.

```kotlin
interface MessageOutboxRepository : Repository {
    suspend fun save(event: DomainEvent)
    suspend fun findUnpublished(limit: Int): List<OutboxMessage>
    suspend fun markAsPublished(messageId: String)
    suspend fun incrementRetryCount(messageId: String)
    suspend fun deletePublishedOlderThan(olderThanDays: Int): Int
    suspend fun findFailedEvents(maxRetries: Int): List<OutboxMessage>
}
```

**Methods**:
- `save(event: DomainEvent)` - Save event to outbox
- `findUnpublished(limit: Int)` - Find unpublished events
- `markAsPublished(messageId: String)` - Mark event as published
- `incrementRetryCount(messageId: String)` - Increment retry count
- `deletePublishedOlderThan(olderThanDays: Int)` - Cleanup old events
- `findFailedEvents(maxRetries: Int)` - Find events that exceeded retries

---

### OutboxMessage

Data class representing an outbox message.

```kotlin
data class OutboxMessage(
    val id: String,
    val eventId: String,
    val eventType: String,
    val aggregateId: String,
    val aggregateType: String,
    val event: DomainEvent,
    val occurredAt: Instant,
    val publishedAt: Instant? = null,
    val retryCount: Int = 0
) {
    companion object {
        fun from(event: DomainEvent): OutboxMessage
    }
    
    fun isPublished(): Boolean
    fun hasExceededRetries(maxRetries: Int): Boolean
}
```

**Properties**:
- `id: String` - Unique message ID
- `eventId: String` - Event ID
- `eventType: String` - Event type name
- `aggregateId: String` - Aggregate ID
- `aggregateType: String` - Aggregate type
- `event: DomainEvent` - The domain event
- `occurredAt: Instant` - When event occurred
- `publishedAt: Instant?` - When published (null if not published)
- `retryCount: Int` - Number of retry attempts

**Methods**:
- `from(event: DomainEvent)` - Create from domain event (companion)
- `isPublished(): Boolean` - Check if published
- `hasExceededRetries(maxRetries: Int): Boolean` - Check if exceeded retries

---

### DomainEvent

Base interface for all domain events.

```kotlin
interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val aggregateId: String
}
```

**Properties**:
- `eventId: String` - Unique event identifier
- `occurredAt: Instant` - When the event occurred
- `aggregateId: String` - ID of the aggregate that produced the event

---

### BaseDomainEvent

Base class for domain events with enhanced metadata.

```kotlin
abstract class BaseDomainEvent(
    aggregateId: String,
    aggregateType: String,
    eventVersion: Int,
    causationId: String? = null,
    correlationId: String? = null
) : DomainEvent {
    override val eventId: String
    override val occurredAt: Instant
    override val aggregateId: String
    open val aggregateType: String
    open val eventVersion: Int
    open val causationId: String?
    open val correlationId: String?
}
```

**Properties**:
- `eventId: String` - Auto-generated UUID
- `occurredAt: Instant` - Auto-generated timestamp
- `aggregateId: String` - Aggregate ID
- `aggregateType: String` - Type of aggregate (e.g., "User", "Order")
- `eventVersion: Int` - Schema version for event evolution
- `causationId: String?` - ID of command/event that caused this
- `correlationId: String?` - Business transaction tracking ID

**Example**:
```kotlin
data class UserRegisteredEvent(
    override val aggregateId: String,
    val email: String,
    val name: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1
)
```

---

## Application Layer - Commands

### Command

Marker interface for write operations.

```kotlin
interface Command
```

**Usage**:
- Implement with data classes
- Add validation in `init` block
- Use imperative naming (RegisterUser, PlaceOrder)

**Example**:
```kotlin
data class RegisterUserCommand(
    val email: String,
    val name: String
) : Command {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
    }
}
```

---

### CommandHandler<C, R>

Interface for executing business logic.

```kotlin
interface CommandHandler<in C : Command, out R> {
    suspend operator fun invoke(command: C): R
}
```

**Type Parameters**:
- `C` - Command type
- `R` - Return type

**Methods**:
- `invoke(command: C): R` - Execute the command (operator function)

**Example**:
```kotlin
class RegisterUserCommandHandler(
    private val userRepository: UserRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
        return runCatching {
            val user = User.create(Email(command.email), command.name)
            userRepository.save(user)
            user.id
        }
    }
}

// Usage
val result = handler(RegisterUserCommand("test@example.com", "Test"))
```

---

### CommandBus

Interface for dispatching commands to handlers.

```kotlin
interface CommandBus {
    fun <C : Command, R> register(
        commandClass: KClass<C>,
        handler: CommandHandler<C, R>
    )
    
    suspend fun <C : Command, R> dispatch(command: C): R
}
```

**Methods**:
- `register(commandClass, handler)` - Register a handler for a command type
- `dispatch(command)` - Dispatch command to registered handler

**Example**:
```kotlin
val commandBus = SimpleCommandBus()
commandBus.register(RegisterUserCommand::class, registerUserHandler)

val result = commandBus.dispatch(RegisterUserCommand("test@example.com", "Test"))
```

---

## Application Layer - Queries

### Query

Marker interface for read operations.

```kotlin
interface Query
```

**Usage**:
- Implement with data classes
- Use question-based naming (GetUserById, FindOrders)

**Example**:
```kotlin
data class GetUserByIdQuery(val userId: String) : Query

data class FindOrdersQuery(
    val customerId: String,
    val page: Int = 0,
    val size: Int = 20
) : Query
```

---

### QueryHandler<Q, R>

Interface for retrieving data.

```kotlin
interface QueryHandler<in Q : Query, out R> {
    suspend operator fun invoke(query: Q): R
}
```

**Type Parameters**:
- `Q` - Query type
- `R` - Return type (typically a DTO)

**Methods**:
- `invoke(query: Q): R` - Execute the query (operator function)

**Example**:
```kotlin
class GetUserByIdQueryHandler(
    private val userRepository: UserRepository
) : QueryHandler<GetUserByIdQuery, UserDto?> {
    
    override suspend operator fun invoke(query: GetUserByIdQuery): UserDto? {
        return userRepository.findById(UserId(query.userId))?.toDto()
    }
}

// Usage
val user = handler(GetUserByIdQuery("123"))
```

---

## Application Layer - Events

### DomainEventPublisher

Interface for publishing events to external systems.

```kotlin
interface DomainEventPublisher {
    suspend fun publish(event: DomainEvent)
    suspend fun publishBatch(events: List<DomainEvent>)
}
```

**Methods**:
- `publish(event: DomainEvent)` - Publish single event
- `publishBatch(events: List<DomainEvent>)` - Publish multiple events

**Example**:
```kotlin
class KafkaDomainEventPublisher(
    private val kafkaProducer: KafkaProducer
) : DomainEventPublisher {
    
    override suspend fun publish(event: DomainEvent) {
        val topic = "domain-events-${event.aggregateType.lowercase()}"
        kafkaProducer.send(topic, event.eventId, serializeEvent(event))
    }
    
    override suspend fun publishBatch(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
```

---

### DomainEventHandler<E>

Interface for handling domain events.

```kotlin
interface DomainEventHandler<in E : DomainEvent> {
    suspend fun handle(event: E)
}
```

**Type Parameters**:
- `E` - Event type

**Methods**:
- `handle(event: E)` - Process the event

**Example**:
```kotlin
class SendWelcomeEmailHandler(
    private val emailService: EmailService
) : DomainEventHandler<UserRegisteredEvent> {
    
    override suspend fun handle(event: UserRegisteredEvent) {
        emailService.send(
            to = event.email,
            subject = "Welcome!",
            body = "Welcome to our platform!"
        )
    }
}
```

---

## 📚 See Also

- **[Core Concepts](Core-Concepts.md)** - Understanding the building blocks
- **[Quick Start Tutorial](Quick-Start-Tutorial.md)** - Practical examples
- **[Architecture Overview](Architecture-Overview.md)** - How it all fits together
