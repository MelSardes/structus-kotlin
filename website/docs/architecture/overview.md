---
sidebar_position: 1
---

# Architecture Overview

Structus implements **Explicit Architecture**, a synthesis of Domain-Driven Design (DDD), Command/Query Separation (CQS), and Event-Driven Architecture (EDA).

## The Four Layers

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

## Domain Layer

The **Domain Layer** contains pure business logic with no framework dependencies.

### Components

- **Entities**: Objects with unique identity
- **Value Objects**: Immutable objects defined by attributes
- **Aggregate Roots**: Consistency boundaries
- **Domain Events**: Facts about what happened
- **Repository Interfaces**: Persistence contracts

### Example

```kotlin
// Pure domain logic
class Order(
    override val id: OrderId,
    val customerId: String
) : AggregateRoot<OrderId>() {
    
    fun confirm() {
        require(status == OrderStatus.PENDING)
        status = OrderStatus.CONFIRMED
        recordEvent(OrderConfirmedEvent(id.value))
    }
}
```

### Rules

- ✅ No framework dependencies
- ✅ No database code
- ✅ No HTTP/REST code
- ✅ Pure Kotlin only

## Application Layer

The **Application Layer** orchestrates use cases and coordinates domain objects.

### Components

- **Commands**: Intent to change state
- **Command Handlers**: Execute business logic
- **Queries**: Request for data
- **Query Handlers**: Retrieve data
- **Event Publishers**: Publish domain events

### Example

```kotlin
class CreateOrderHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(
        command: CreateOrderCommand
    ): Result<OrderId> {
        return runCatching {
            val order = Order.create(command.customerId, command.items)
            orderRepository.save(order)
            
            // Transactional Outbox Pattern
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

### Rules

- ✅ Depends on Domain Layer
- ✅ Orchestrates use cases
- ✅ No framework-specific code
- ✅ Uses suspend functions

## Infrastructure Layer

The **Infrastructure Layer** provides concrete implementations of domain interfaces.

### Components

- **Repository Implementations**: Database access
- **Event Publishers**: Message broker integration
- **External Services**: Third-party API clients
- **Persistence Models**: Database schemas

### Example

```kotlin
class OrderRepositoryImpl(
    private val database: Database
) : OrderRepository {
    
    override suspend fun save(order: Order) {
        database.execute(
            "INSERT INTO orders (...) VALUES (...)",
            mapToPersistenceModel(order)
        )
    }
    
    override suspend fun findById(id: OrderId): Order? {
        return database.query("SELECT * FROM orders WHERE id = ?", id.value)
            ?.let { mapToDomainModel(it) }
    }
}
```

### Rules

- ✅ Implements domain interfaces
- ✅ Framework-specific code allowed
- ✅ Database access
- ✅ External API calls

## Presentation Layer

The **Presentation Layer** handles communication with the outside world.

### Components

- **Controllers**: HTTP endpoints
- **DTOs**: Data transfer objects
- **Request/Response Models**: API contracts
- **Validation**: Input validation

### Example

```kotlin
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val createOrderHandler: CreateOrderHandler,
    private val getOrderHandler: GetOrderByIdHandler
) {
    
    @PostMapping
    suspend fun createOrder(
        @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val command = request.toCommand()
        val result = createOrderHandler(command)
        
        return result.fold(
            onSuccess = { orderId -> 
                ResponseEntity.ok(OrderResponse(orderId.value))
            },
            onFailure = { error -> 
                ResponseEntity.badRequest().build()
            }
        )
    }
}
```

### Rules

- ✅ Depends on Application Layer
- ✅ Framework-specific code allowed
- ✅ Handles HTTP/REST
- ✅ Input validation

## Dependency Rule

**The Dependency Rule** is the most important principle:

> Dependencies can only point inward. Inner layers know nothing about outer layers.

```
Presentation → Application → Domain
                ↑
         Infrastructure
```

### What This Means

1. **Domain** depends on nothing
2. **Application** depends on Domain
3. **Infrastructure** depends on Domain and Application
4. **Presentation** depends on Application

### Why It Matters

- **Testability**: Test business logic without frameworks
- **Flexibility**: Change frameworks without changing business logic
- **Maintainability**: Clear boundaries and responsibilities
- **Portability**: Reuse domain logic across projects

## CQRS Pattern

Structus uses **Command Query Responsibility Segregation (CQRS)**.

### Commands (Write Side)

- Change state
- Use domain model
- Validate business rules
- Record events

### Queries (Read Side)

- Retrieve data
- Bypass domain model
- Optimize for reading
- No side effects

```kotlin
// Command - goes through domain model
class CreateOrderHandler : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        val order = Order.create(command.customerId, command.items)
        orderRepository.save(order)
        return Result.success(order.id)
    }
}

// Query - optimized read
class GetOrderHandler : QueryHandler<GetOrderQuery, OrderDto?> {
    override suspend operator fun invoke(query: GetOrderQuery): OrderDto? {
        // Direct database query, bypassing domain model
        return database.queryForObject(
            "SELECT * FROM orders WHERE id = ?",
            query.orderId
        )
    }
}
```

## Event-Driven Architecture

Domain events enable loose coupling and eventual consistency.

### Transactional Outbox Pattern

```kotlin
// 1. Save aggregate and events in same transaction
orderRepository.save(order)
order.domainEvents.forEach { outboxRepository.save(it) }
order.clearEvents()

// 2. Separate process publishes events
class OutboxPublisher {
    suspend fun publishPendingEvents() {
        val messages = outboxRepository.findUnpublished(100)
        messages.forEach { message ->
            eventPublisher.publish(message.event)
            outboxRepository.markAsPublished(message.id)
        }
    }
}
```

## Best Practices

### ✅ Do

- Keep domain layer pure
- Use value objects for validation
- Record domain events for state changes
- Use Result type for error handling
- Test business logic in isolation

### ❌ Don't

- Put framework code in domain layer
- Skip validation in value objects
- Forget to clear events after publishing
- Use exceptions for business logic flow
- Mix commands and queries

## Next Steps

- **[Domain Layer](domain-layer)** - Deep dive into domain modeling
- **[Application Layer](application-layer)** - Learn about commands and queries
- **[Infrastructure Layer](infrastructure-layer)** - Implement persistence
- **[CQRS Implementation](../advanced/cqrs-implementation)** - Advanced CQRS patterns
