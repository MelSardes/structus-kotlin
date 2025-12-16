---
sidebar_position: 3
pagination_next: architecture/overview
pagination_prev: getting-started/quick-start
---

# Core Concepts

This page explains the fundamental building blocks of Structus.

## Entity vs Value Object

### Entity

An **Entity** is a domain object with a unique identity that persists over time. Two entities are equal if they have the same ID, regardless of their attributes.

**Key Characteristics:**
- Has a unique identifier
- Mutable (can change state)
- Identity-based equality
- Lifecycle (created, updated, deleted)

```kotlin
import com.melsardes.libraries.structuskotlin.domain.Entity

data class UserId(val value: String)

class User(
    override val id: UserId,
    var email: String,
    var name: String
) : Entity<UserId>() {
    
    fun updateEmail(newEmail: String) {
        this.email = newEmail
    }
}

// Two users with same ID are equal
val user1 = User(UserId("123"), "john@example.com", "John")
val user2 = User(UserId("123"), "jane@example.com", "Jane")
println(user1 == user2)  // true - same ID
```

### Value Object

A **Value Object** is an immutable object defined by its attributes. Two value objects are equal if all their attributes are equal.

**Key Characteristics:**
- No unique identifier
- Immutable (cannot change)
- Attribute-based equality
- Self-validating

```kotlin
import com.melsardes.libraries.structuskotlin.domain.ValueObject

data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { 
            "Invalid email format: $value" 
        }
    }
    
    companion object {
        private val EMAIL_REGEX = 
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
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
        require(currency == other.currency) { 
            "Cannot add different currencies" 
        }
        return Money(amount + other.amount, currency)
    }
}
```

### When to Use Which?

| Use Entity When | Use Value Object When |
|----------------|----------------------|
| Identity matters | Attributes define the object |
| Needs to be tracked over time | Immutable and replaceable |
| Has a lifecycle | No lifecycle |
| Example: User, Order, Product | Example: Email, Money, Address |

## Aggregate Roots

An **Aggregate Root** is a special type of Entity that serves as the consistency boundary for a group of related objects.

**Key Characteristics:**
- Extends Entity
- Manages domain events
- Enforces business invariants
- Entry point for all operations

```kotlin
import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

data class OrderId(val value: String)

class Order(
    override val id: OrderId,
    val customerId: String,
    var status: OrderStatus = OrderStatus.PENDING
) : AggregateRoot<OrderId>() {
    
    fun confirm(amount: Double) {
        require(status == OrderStatus.PENDING) { 
            "Order must be pending to confirm" 
        }
        
        this.status = OrderStatus.CONFIRMED
        
        // Record domain event
        recordEvent(OrderConfirmedEvent(
            aggregateId = id.value,
            customerId = customerId,
            amount = amount
        ))
    }
}
```

## Domain Events

**Domain Events** represent facts about things that happened in your domain.

**Key Characteristics:**
- Immutable
- Past tense naming (UserRegistered, OrderPlaced)
- Contains all relevant information
- Includes metadata (eventId, occurredAt, aggregateId)

```kotlin
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

data class UserRegisteredEvent(
    override val aggregateId: String,
    val userId: String,
    val email: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1
)
```

## Repositories

**Repositories** provide an abstraction for data persistence.

**Key Characteristics:**
- Interface in domain layer
- Implementation in infrastructure layer
- Collection-like API
- Uses suspend functions

```kotlin
import com.melsardes.libraries.structuskotlin.domain.Repository

// Domain layer - Interface
interface UserRepository : Repository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun save(user: User)
    suspend fun existsByEmail(email: Email): Boolean
}
```

## Commands vs Queries (CQS)

**Command/Query Separation** separates operations that change state (commands) from operations that retrieve data (queries).

### Commands

**Commands** represent an intent to change state.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.Command

data class RegisterUserCommand(
    val email: String,
    val name: String
) : Command {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
    }
}
```

### Queries

**Queries** retrieve data without changing state.

```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.Query

data class GetUserByIdQuery(
    val userId: String
) : Query

data class SearchProductsQuery(
    val searchTerm: String,
    val category: String? = null
) : Query
```

### Comparison

| Aspect | Command | Query |
|--------|---------|-------|
| **Purpose** | Change state | Retrieve data |
| **Naming** | Imperative | Question |
| **Return** | ID or Result | Data (DTO) |
| **Side Effects** | Yes | No |

## Command Handlers

**Command Handlers** execute business logic in response to commands.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler

class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(
        command: RegisterUserCommand
    ): Result<UserId> {
        return runCatching {
            // 1. Validate
            if (userRepository.existsByEmail(Email(command.email))) {
                throw IllegalStateException("Email already exists")
            }
            
            // 2. Create aggregate
            val user = User.create(
                email = Email(command.email),
                name = command.name
            )
            
            // 3. Save aggregate
            userRepository.save(user)
            
            // 4. Save events to outbox
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

## Query Handlers

**Query Handlers** retrieve data optimized for reading.

```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler

data class UserDto(
    val id: String,
    val email: String,
    val name: String
)

class GetUserByIdQueryHandler(
    private val userRepository: UserRepository
) : QueryHandler<GetUserByIdQuery, UserDto?> {
    
    override suspend operator fun invoke(
        query: GetUserByIdQuery
    ): UserDto? {
        val user = userRepository.findById(UserId(query.userId))
        return user?.let {
            UserDto(
                id = it.id.value,
                email = it.email.value,
                name = it.name
            )
        }
    }
}
```

## Event Publishing

**Event Publishers** send domain events to external systems.

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

## Next Steps

- **[Architecture Overview](../architecture/overview)** - Understand how these concepts fit together
- **[Quick Start Tutorial](quick-start)** - Build a complete application
- **[CQRS Implementation](../advanced/cqrs-implementation)** - Deep dive into CQRS
