# Getting Started with Structus

This guide will help you get started with the Structus, a pure Kotlin library providing building blocks for Domain-Driven Design (DDD) and Clean Architecture applications.

## Table of Contents

- [Installation](#installation)
- [Core Concepts](#core-concepts)
- [Quick Start Examples](#quick-start-examples)
  - [1. Creating Entities](#1-creating-entities)
  - [2. Creating Value Objects](#2-creating-value-objects)
  - [3. Building Aggregate Roots](#3-building-aggregate-roots)
  - [4. Working with Domain Events](#4-working-with-domain-events)
  - [5. Using kotlin.Result for Error Handling](#5-using-kotlinresult-for-error-handling)
  - [6. Implementing Commands and Queries](#6-implementing-commands-and-queries)
  - [7. Handling Domain Events](#7-handling-domain-events)
- [Best Practices](#best-practices)
- [Next Steps](#next-steps)

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.melsardes.libraries:structus-kotlin:0.1.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.melsardes.libraries:structus-kotlin:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.melsardes.libraries</groupId>
    <artifactId>structus-kotlin</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Core Concepts

The library provides the following core building blocks:

- **Entity**: Domain objects with unique identity
- **ValueObject**: Immutable objects defined by their attributes
- **AggregateRoot**: Entities that serve as consistency boundaries
- **DomainEvent**: Immutable facts about things that happened
- **Command/Query**: CQRS pattern support
- **Repository**: Abstraction for data persistence

## Quick Start Examples

### 1. Creating Entities

Entities have unique identities that persist over time. Two entities are equal if they have the same ID.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.Entity

// Define an entity ID
data class UserId(val value: String)

// Create an entity
class User(
    override val id: UserId,
    var email: String,
    var name: String
) : Entity<UserId>() {
    
    fun updateEmail(newEmail: String) {
        email = newEmail
    }
}

// Usage
fun main() {
    val user1 = User(UserId("123"), "john@example.com", "John Doe")
    val user2 = User(UserId("123"), "jane@example.com", "Jane Doe")
    
    // Same ID = same entity, even with different attributes
    println(user1 == user2)  // true
    println(user1.hashCode() == user2.hashCode())  // true
}
```

### 2. Creating Value Objects

Value Objects are immutable and defined by their attributes. Two value objects are equal if all their attributes are equal.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.ValueObject

// Define a value object using a data class
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
fun main() {
    val price1 = Money(100.0, "USD")
    val price2 = Money(50.0, "USD")
    
    val total = price1 + price2
    println(total)  // Money(amount=150.0, currency=USD)
}
```

### 3. Building Aggregate Roots

Aggregate Roots coordinate changes within an aggregate and record domain events.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

// Define domain events
data class OrderPlacedEvent(
    override val aggregateId: String,
    val customerId: String,
    val totalAmount: Double
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)

data class OrderCancelledEvent(
    override val aggregateId: String,
    val reason: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)

// Define aggregate root
data class OrderId(val value: String)

enum class OrderStatus { PENDING, CONFIRMED, CANCELLED }

class Order(
    override val id: OrderId,
    val customerId: String,
    var status: OrderStatus = OrderStatus.PENDING,
    var totalAmount: Double = 0.0
) : AggregateRoot<OrderId>() {
    
    fun place(amount: Double) {
        require(status == OrderStatus.PENDING) { "Order already placed" }
        
        totalAmount = amount
        status = OrderStatus.CONFIRMED
        
        // Record domain event
        recordEvent(OrderPlacedEvent(
            aggregateId = id.value,
            customerId = customerId,
            totalAmount = amount
        ))
    }
    
    fun cancel(reason: String) {
        require(status == OrderStatus.CONFIRMED) { "Can only cancel confirmed orders" }
        
        status = OrderStatus.CANCELLED
        
        // Record domain event
        recordEvent(OrderCancelledEvent(
            aggregateId = id.value,
            reason = reason
        ))
    }
}

// Usage
fun main() {
    val order = Order(OrderId("ORD-001"), "CUST-123")
    
    // Place the order
    order.place(250.0)
    
    // Get recorded events
    val events = order.domainEvents
    println("Events recorded: ${events.size}")  // 1
    println(events.first())  // OrderPlacedEvent(...)
    
    // Clear events after processing
    order.clearEvents()
}
```

### 4. Working with Domain Events

Domain events represent facts about things that happened in your domain.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent
import java.time.Instant

// Define a domain event with correlation tracking
data class UserRegisteredEvent(
    override val aggregateId: String,
    val email: String,
    val registrationSource: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1,
    causationId = null,  // ID of command that caused this
    correlationId = null  // Business transaction ID
)

// Usage
fun main() {
    val event = UserRegisteredEvent(
        aggregateId = "USER-123",
        email = "john@example.com",
        registrationSource = "web"
    )
    
    println("Event ID: ${event.eventId}")
    println("Occurred at: ${event.occurredAt}")
    println("Aggregate: ${event.aggregateType}#${event.aggregateId}")
    println("Version: ${event.eventVersion}")
}
```

### 5. Using kotlin.Result for Error Handling

Use the standard `kotlin.Result` for explicit error handling.

```kotlin
// Define custom exceptions for business errors
class UserNotFoundException(val userId: String) : Exception("User with ID '$userId' not found.")
class InvalidEmailException(val email: String) : Exception("Invalid email format: '$email'")

// Service that returns Result
class UserService {
    private val users = mutableMapOf<String, String>()

    fun registerUser(email: String): Result<String> {
        return runCatching {
            if (!email.contains("@")) {
                throw InvalidEmailException(email)
            }
            if (users.containsKey(email)) {
                throw IllegalStateException("User already exists")
            }
            val userId = "USER-${users.size + 1}"
            users[email] = userId
            userId
        }
    }

    fun findUser(userId: String): Result<String> {
        return users.entries.find { it.value == userId }
            ?.let { Result.success(it.key) }
            ?: Result.failure(UserNotFoundException(userId))
    }
}

// Usage
fun main() {
    val service = UserService()

    // Success case
    service.registerUser("john@example.com")
        .onSuccess { userId -> println("User registered: $userId") }
        .onFailure { error -> println("Error: ${error.message}") }

    // Failure case
    service.registerUser("john@example.com")
        .onSuccess { userId -> println("User registered: $userId") }
        .onFailure { error -> println("Error: ${error.message}") }
    
    // Chaining operations
    val emailResult = service.findUser("USER-1")
        .map { email -> "Email for USER-1 is $email" }
        .getOrElse { "Could not find user" }
    
    println(emailResult)
}
```

### 6. Implementing Commands and Queries

Implement CQRS pattern with commands (writes) and queries (reads).

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.*
import com.melsardes.libraries.structuskotlin.application.queries.*

// Define a command
data class CreateUserCommand(
    val email: String,
    val name: String
) : Command

// Define a command handler
class CreateUserCommandHandler : CommandHandler<CreateUserCommand, Result<String>> {
    
    override suspend operator fun invoke(command: CreateUserCommand): Result<String> {
        return runCatching {
            if (!command.email.contains("@")) {
                throw IllegalArgumentException("Invalid email")
            }
            // Create user (simplified)
            val userId = "USER-${System.currentTimeMillis()}"
            userId
        }
    }
}

// Define a query
data class GetUserQuery(val userId: String) : Query

data class UserDto(val id: String, val email: String, val name: String)

// Define a query handler
class GetUserQueryHandler : QueryHandler<GetUserQuery, UserDto?> {
    
    override suspend operator fun invoke(query: GetUserQuery): UserDto? {
        // Fetch from read model (simplified)
        return if (query.userId == "USER-123") {
            UserDto("USER-123", "john@example.com", "John Doe")
        } else {
            null
        }
    }
}

// Usage
suspend fun main() {
    val commandHandler = CreateUserCommandHandler()
    val queryHandler = GetUserQueryHandler()
    
    // Execute command
    val command = CreateUserCommand("jane@example.com", "Jane Doe")
    commandHandler(command)
        .onSuccess { userId -> println("User created: $userId") }
        .onFailure { error -> println("Error: ${error.message}") }
    
    // Execute query
    val query = GetUserQuery("USER-123")
    val user = queryHandler(query)
    println("User found: $user")
}
```

### 7. Handling Domain Events

Implement event handlers to react to domain events.

```kotlin
import com.melsardes.libraries.structuskotlin.application.events.*
import com.melsardes.libraries.structuskotlin.domain.events.DomainEvent
import java.time.Instant
import kotlin.reflect.KClass

// Define a domain event
data class OrderPlacedEvent(
    override val eventId: String,
    override val occurredAt: Instant,
    override val aggregateId: String,
    val customerId: String,
    val amount: Double
) : DomainEvent

// Implement an event handler
class SendOrderConfirmationEmailHandler : DomainEventHandler<OrderPlacedEvent> {
    
    override suspend fun handle(event: OrderPlacedEvent) {
        println("Sending confirmation email for order ${event.aggregateId}")
        println("Customer: ${event.customerId}, Amount: ${event.amount}")
        // emailService.send(...)
    }
}

class UpdateOrderStatisticsHandler : DomainEventHandler<OrderPlacedEvent> {
    
    override suspend fun handle(event: OrderPlacedEvent) {
        println("Updating statistics for order ${event.aggregateId}")
        // statisticsRepository.increment(...)
    }
}

// A simple event dispatcher
class EventDispatcher {
    private val handlers = mutableMapOf<KClass<out DomainEvent>, MutableList<DomainEventHandler<*>>>()

    fun <E : DomainEvent> register(eventClass: KClass<E>, handler: DomainEventHandler<E>) {
        handlers.getOrPut(eventClass) { mutableListOf() }.add(handler)
    }

    suspend fun dispatch(event: DomainEvent) {
        handlers[event::class]?.forEach { handler ->
            @Suppress("UNCHECKED_CAST")
            (handler as DomainEventHandler<DomainEvent>).handle(event)
        }
    }
}

// Usage
suspend fun main() {
    val emailHandler = SendOrderConfirmationEmailHandler()
    val statsHandler = UpdateOrderStatisticsHandler()
    
    val dispatcher = EventDispatcher()
    dispatcher.register(OrderPlacedEvent::class, emailHandler)
    dispatcher.register(OrderPlacedEvent::class, statsHandler)
    
    val event = OrderPlacedEvent(
        eventId = "EVT-001",
        occurredAt = Instant.now(),
        aggregateId = "ORD-123",
        customerId = "CUST-456",
        amount = 250.0
    )
    
    dispatcher.dispatch(event)
}
```

## Best Practices

### 1. Keep Aggregates Small
- Design aggregates around consistency boundaries
- Avoid large object graphs within a single aggregate
- Use eventual consistency between aggregates

### 2. Use Value Objects
- Prefer value objects over primitive types
- Make value objects immutable
- Implement validation in value object constructors

### 3. Record Domain Events
- Record events for all significant state changes
- Name events in past tense (e.g., `OrderPlaced`, not `PlaceOrder`)
- Include all relevant information in events

### 4. Handle Errors Explicitly
- Use `kotlin.Result` for expected, recoverable errors
- Reserve exceptions for truly exceptional cases (e.g., infrastructure failures)
- Define custom, meaningful exceptions for business errors

### 5. Separate Commands from Queries
- Commands change state and return minimal data
- Queries return data without side effects
- Use different models for reads and writes (CQRS)

### 6. Use Correlation IDs
- Track related events across aggregates
- Include `causationId` and `correlationId` in events
- Maintain correlation chain for debugging

## Next Steps

- **Read the API Documentation**: Explore the KDoc documentation for detailed API reference
- **Check the Examples**: See the `examples/` directory for more complex scenarios
- **Review RECOMMENDATIONS.md**: Learn about best practices and patterns
- **Join the Community**: Contribute to the project on GitHub

## Additional Resources

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://vaughnvernon.com/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)

## Support

If you encounter any issues or have questions:
- Open an issue on [GitHub](https://github.com/structus-io/structus-kotlin/issues)
- Check existing documentation in the repository
- Review the test suite for usage examples

---

**Happy coding with Explicit Architecture!** 🚀
