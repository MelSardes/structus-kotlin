# Core Concepts

This page explains the fundamental building blocks of the Structus.

## 📚 Table of Contents

- [Entity vs Value Object](#entity-vs-value-object)
- [Aggregate Roots](#aggregate-roots)
- [Domain Events](#domain-events)
- [Repositories](#repositories)
- [Commands vs Queries (CQS)](#commands-vs-queries-cqs)
- [Command Handlers](#command-handlers)
- [Query Handlers](#query-handlers)
- [Event Publishing](#event-publishing)

---

## Entity vs Value Object

### Entity

An **Entity** is a domain object with a unique identity that persists over time. Two entities are equal if they have the same ID, regardless of their attributes.

**Key Characteristics:**
- Has a unique identifier
- Mutable (can change state)
- Identity-based equality
- Lifecycle (created, updated, deleted)

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.domain.Entity

data class UserId(val value: String)

class User(
    override val id: UserId,
    var email: String,
    var name: String,
    var status: UserStatus
) : Entity<UserId>() {
    
    fun updateEmail(newEmail: String) {
        this.email = newEmail
    }
}

// Usage
val user1 = User(UserId("123"), "john@example.com", "John", UserStatus.ACTIVE)
val user2 = User(UserId("123"), "jane@example.com", "Jane", UserStatus.PENDING)

println(user1 == user2)  // true - same ID, same entity
```

### Value Object

A **Value Object** is an immutable object defined by its attributes. Two value objects are equal if all their attributes are equal.

**Key Characteristics:**
- No unique identifier
- Immutable (cannot change)
- Attribute-based equality
- Self-validating

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.domain.ValueObject

data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format: $value" }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}

data class Money(
    val amount: Double,
    val currency: String
) : ValueObject {
    init {
        require(amount >= 0) { "Amount cannot be negative" }
        require(currency.length == 3) { "Currency must be 3-letter code" }
    }
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies" }
        return Money(amount + other.amount, currency)
    }
}

// Usage
val email1 = Email("john@example.com")
val email2 = Email("john@example.com")
println(email1 == email2)  // true - same attributes

val price1 = Money(100.0, "USD")
val price2 = Money(50.0, "USD")
val total = price1 + price2  // Money(150.0, "USD")
```

### When to Use Which?

| Use Entity When | Use Value Object When |
|----------------|----------------------|
| Identity matters | Attributes define the object |
| Needs to be tracked over time | Immutable and replaceable |
| Has a lifecycle | No lifecycle |
| Example: User, Order, Product | Example: Email, Money, Address |

---

## Aggregate Roots

An **Aggregate Root** is a special type of Entity that serves as the consistency boundary for a group of related objects. It coordinates changes and records domain events.

**Key Characteristics:**
- Extends Entity
- Manages domain events
- Enforces business invariants
- Entry point for all operations on the aggregate

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

data class OrderId(val value: String)

enum class OrderStatus { PENDING, CONFIRMED, SHIPPED, CANCELLED }

class Order(
    override val id: OrderId,
    val customerId: String,
    var status: OrderStatus = OrderStatus.PENDING,
    var totalAmount: Double = 0.0
) : AggregateRoot<OrderId>() {
    
    fun confirm(amount: Double) {
        require(status == OrderStatus.PENDING) { "Order must be pending to confirm" }
        require(amount > 0) { "Amount must be positive" }
        
        this.totalAmount = amount
        this.status = OrderStatus.CONFIRMED
        
        // Record domain event
        recordEvent(OrderConfirmedEvent(
            aggregateId = id.value,
            customerId = customerId,
            amount = amount
        ))
    }
    
    fun ship() {
        require(status == OrderStatus.CONFIRMED) { "Order must be confirmed to ship" }
        
        this.status = OrderStatus.SHIPPED
        
        recordEvent(OrderShippedEvent(
            aggregateId = id.value,
            customerId = customerId
        ))
    }
}

// Domain Events
data class OrderConfirmedEvent(
    override val aggregateId: String,
    val customerId: String,
    val amount: Double
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)

data class OrderShippedEvent(
    override val aggregateId: String,
    val customerId: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)
```

**Event Management:**
```kotlin
val order = Order(OrderId("ORD-001"), "CUST-123")
order.confirm(250.0)
order.ship()

// Get recorded events
val events = order.domainEvents
println("Events recorded: ${events.size}")  // 2

// Process events (typically in a command handler)
events.forEach { event ->
    eventPublisher.publish(event)
}

// Clear events after processing
order.clearEvents()
```

---

## Domain Events

**Domain Events** represent facts about things that happened in your domain. They are immutable and named in past tense.

**Key Characteristics:**
- Immutable
- Past tense naming (UserRegistered, OrderPlaced)
- Contains all relevant information
- Includes metadata (eventId, occurredAt, aggregateId)

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

data class UserRegisteredEvent(
    override val aggregateId: String,
    val userId: String,
    val email: String,
    val registrationSource: String,
    override val causationId: String? = null,
    override val correlationId: String? = null
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1,
    causationId = causationId,
    correlationId = correlationId
)

// Usage
val event = UserRegisteredEvent(
    aggregateId = "USER-123",
    userId = "USER-123",
    email = "john@example.com",
    registrationSource = "web"
)

println("Event ID: ${event.eventId}")
println("Occurred at: ${event.occurredAt}")
println("Aggregate: ${event.aggregateType}#${event.aggregateId}")
```

---

## Repositories

**Repositories** provide an abstraction for data persistence. The interface lives in the domain layer, while the implementation lives in the infrastructure layer.

**Key Characteristics:**
- Interface in domain layer
- Implementation in infrastructure layer
- Collection-like API
- Uses suspend functions for async operations

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.domain.Repository

// Domain layer - Interface
interface UserRepository : Repository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun save(user: User)
    suspend fun delete(id: UserId)
    suspend fun existsByEmail(email: Email): Boolean
}

// Infrastructure layer - Implementation
class UserRepositoryImpl(
    private val database: Database
) : UserRepository {
    
    override suspend fun findById(id: UserId): User? {
        // Database-specific implementation
        return database.query("SELECT * FROM users WHERE id = ?", id.value)
            ?.let { mapToUser(it) }
    }
    
    override suspend fun save(user: User) {
        // Database-specific implementation
        database.execute("INSERT INTO users (...) VALUES (...)")
    }
    
    // ... other methods
}
```

---

## Commands vs Queries (CQS)

**Command/Query Separation (CQS)** separates operations that change state (commands) from operations that retrieve data (queries).

### Commands

**Commands** represent an intent to change state. They are named imperatively and may return a result indicating success or failure.

**Characteristics:**
- Imperative naming (RegisterUser, PlaceOrder)
- Changes state
- May return ID or success/failure
- Has side effects

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.Command

data class RegisterUserCommand(
    val email: String,
    val name: String,
    val password: String
) : Command {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(password.length >= 8) { "Password must be at least 8 characters" }
    }
}

data class PlaceOrderCommand(
    val customerId: String,
    val items: List<OrderItem>,
    val shippingAddress: Address
) : Command
```

### Queries

**Queries** retrieve data without changing state. They are named as questions and return data.

**Characteristics:**
- Question-based naming (GetUserById, FindActiveOrders)
- No state changes
- Returns data (DTOs)
- No side effects

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.Query

data class GetUserByIdQuery(
    val userId: String
) : Query

data class FindActiveOrdersQuery(
    val customerId: String,
    val page: Int = 0,
    val size: Int = 20
) : Query

data class SearchProductsQuery(
    val searchTerm: String,
    val category: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null
) : Query
```

### Comparison Table

| Aspect | Command | Query |
|--------|---------|-------|
| **Purpose** | Change state | Retrieve data |
| **Naming** | Imperative (RegisterUser) | Question (GetUserById) |
| **Return** | ID or Result | Data (DTO) |
| **Side Effects** | Yes | No |
| **Example** | CreateOrder, UpdateProfile | GetOrder, ListUsers |

---

## Command Handlers

**Command Handlers** execute business logic in response to commands. They orchestrate the flow: load aggregate → call domain method → save aggregate.

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler

class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
        return runCatching {
            // 1. Validate
            if (userRepository.existsByEmail(Email(command.email))) {
                throw IllegalStateException("Email already exists")
            }
            
            // 2. Create aggregate
            val user = User.create(
                email = Email(command.email),
                name = command.name,
                password = command.password
            )
            
            // 3. Save aggregate
            userRepository.save(user)
            
            // 4. Save events to outbox (Transactional Outbox Pattern)
            user.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // 5. Clear events
            user.clearEvents()
            
            user.id
        }
    }
}
```

---

## Query Handlers

**Query Handlers** retrieve data optimized for reading. They can bypass the domain model for performance.

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler

data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val status: String
)

class GetUserByIdQueryHandler(
    private val userRepository: UserRepository
) : QueryHandler<GetUserByIdQuery, UserDto?> {
    
    override suspend operator fun invoke(query: GetUserByIdQuery): UserDto? {
        val user = userRepository.findById(UserId(query.userId))
        return user?.let {
            UserDto(
                id = it.id.value,
                email = it.email.value,
                name = it.name,
                status = it.status.name
            )
        }
    }
}
```

---

## Event Publishing

**Event Publishers** send domain events to external systems (message brokers, event buses, etc.).

**Example:**
```kotlin
import com.melsardes.libraries.structuskotlin.application.events.DomainEventPublisher

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

## 🚀 Next Steps

- **[Architecture Overview](Architecture-Overview.md)** - Understand how these concepts fit together
- **[Quick Start Tutorial](Quick-Start-Tutorial.md)** - Build a complete application
- **[Domain Layer Guide](Domain-Entities.md)** - Deep dive into domain modeling
