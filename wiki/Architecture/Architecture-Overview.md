# Architecture Overview

This page provides a high-level overview of Explicit Architecture and how the library enforces its principles.

## 🎯 What is Explicit Architecture?

**Explicit Architecture** is a synthesis of proven architectural patterns:

- **Domain-Driven Design (DDD)** - Focus on core domain and business logic
- **Clean Architecture** - Dependency inversion and layer separation
- **Hexagonal Architecture** - Isolate business logic from external concerns
- **CQRS** - Separate reads from writes
- **Event-Driven Architecture** - React to domain events

The library provides the **kernel** - foundational building blocks that enforce these principles while remaining framework-agnostic.

## 🏛️ The Four Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│    (Controllers, DTOs, API)             │
│         ↓ depends on ↓                  │
├─────────────────────────────────────────┤
│        Application Layer                │
│  (Commands, Queries, Handlers)          │
│         ↓ depends on ↓                  │
├─────────────────────────────────────────┤
│          Domain Layer                   │
│  (Entities, Aggregates, Events)         │
│         ↑ implemented by ↑              │
├─────────────────────────────────────────┤
│       Infrastructure Layer              │
│  (Repositories, External APIs)          │
└─────────────────────────────────────────┘
```

### 1. Domain Layer (Core)

**Purpose**: Pure business logic and domain model

**Contains**:
- Entities (identity-based objects)
- Value Objects (attribute-based immutable objects)
- Aggregate Roots (consistency boundaries)
- Domain Events (facts about what happened)
- Repository Interfaces (persistence contracts)

**Rules**:
- ❌ NO dependencies on other layers
- ❌ NO framework dependencies
- ❌ NO infrastructure concerns (databases, HTTP, etc.)
- ✅ Pure Kotlin only
- ✅ Business rules and invariants

**Example**:
```kotlin
// Domain Layer - Pure business logic
class Order(
    override val id: OrderId,
    var status: OrderStatus,
    var totalAmount: Money
) : AggregateRoot<OrderId>() {
    
    fun place(items: List<OrderItem>) {
        require(status == OrderStatus.DRAFT) { "Order must be draft" }
        require(items.isNotEmpty()) { "Order must have items" }
        
        this.totalAmount = items.sumOf { it.price }
        this.status = OrderStatus.PLACED
        
        recordEvent(OrderPlacedEvent(
            aggregateId = id.value,
            totalAmount = totalAmount.amount
        ))
    }
}
```

### 2. Application Layer (Use Cases)

**Purpose**: Orchestrate domain logic and coordinate operations

**Contains**:
- Commands (write operations)
- Command Handlers (execute business logic)
- Queries (read operations)
- Query Handlers (retrieve data)
- Event Publishers (publish to external systems)

**Rules**:
- ✅ Depends on Domain Layer
- ✅ Orchestrates domain objects
- ✅ Manages transactions
- ❌ NO business logic (delegate to domain)
- ❌ NO direct database access (use repositories)

**Example**:
```kotlin
// Application Layer - Orchestration
class PlaceOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<PlaceOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: PlaceOrderCommand): Result<OrderId> {
        return runCatching {
            // 1. Load aggregate
            val order = orderRepository.findById(OrderId(command.orderId))
                ?: throw OrderNotFoundException(command.orderId)
            
            // 2. Call domain method
            order.place(command.items)
            
            // 3. Save aggregate
            orderRepository.save(order)
            
            // 4. Save events (Transactional Outbox)
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

### 3. Infrastructure Layer (Implementation Details)

**Purpose**: Implement technical concerns and external integrations

**Contains**:
- Repository Implementations (database access)
- Event Publisher Implementations (Kafka, RabbitMQ)
- External API clients
- Persistence models
- Mappers (domain ↔ persistence)

**Rules**:
- ✅ Implements Domain interfaces
- ✅ Framework-specific code lives here
- ✅ Database queries and ORM
- ❌ NO business logic

**Example**:
```kotlin
// Infrastructure Layer - Implementation
class PostgresOrderRepository(
    private val database: Database
) : OrderRepository {
    
    override suspend fun findById(id: OrderId): Order? {
        val row = database.query(
            "SELECT * FROM orders WHERE id = ?",
            id.value
        ) ?: return null
        
        return mapToOrder(row)
    }
    
    override suspend fun save(order: Order) {
        database.execute(
            """
            INSERT INTO orders (id, status, total_amount, created_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                status = EXCLUDED.status,
                total_amount = EXCLUDED.total_amount
            """,
            order.id.value,
            order.status.name,
            order.totalAmount.amount,
            order.createdAt
        )
    }
}
```

### 4. Presentation Layer (Boundary)

**Purpose**: Handle external communication (HTTP, CLI, messaging)

**Contains**:
- Controllers/Endpoints
- Request/Response DTOs
- Input validation
- HTTP status mapping

**Rules**:
- ✅ Depends on Application Layer
- ✅ Dispatches commands/queries
- ✅ Maps DTOs to commands/queries
- ❌ NO business logic
- ❌ NO direct repository access

**Example**:
```kotlin
// Presentation Layer - HTTP API
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val placeOrderHandler: PlaceOrderCommandHandler,
    private val getOrderHandler: GetOrderByIdQueryHandler
) {
    
    @PostMapping("/{orderId}/place")
    suspend fun placeOrder(
        @PathVariable orderId: String,
        @RequestBody request: PlaceOrderRequest
    ): ResponseEntity<OrderResponse> {
        val command = PlaceOrderCommand(
            orderId = orderId,
            items = request.items.map { it.toOrderItem() }
        )
        
        return placeOrderHandler(command).fold(
            onSuccess = { orderId ->
                ResponseEntity.ok(OrderResponse(orderId.value))
            },
            onFailure = { error ->
                ResponseEntity.badRequest().body(ErrorResponse(error.message))
            }
        )
    }
    
    @GetMapping("/{orderId}")
    suspend fun getOrder(@PathVariable orderId: String): OrderDto? {
        val query = GetOrderByIdQuery(orderId)
        return getOrderHandler(query)
    }
}
```

## 🔄 The Dependency Rule

**The Golden Rule**: Dependencies point **inward** only.

```
Presentation → Application → Domain ← Infrastructure
```

- **Presentation** depends on **Application**
- **Application** depends on **Domain**
- **Infrastructure** implements **Domain** interfaces
- **Domain** depends on **NOTHING**

This ensures:
- Domain logic is isolated and testable
- Business rules are independent of frameworks
- Easy to swap implementations (database, message broker, etc.)

## 🎭 CQRS Pattern

**Command/Query Responsibility Segregation** separates reads from writes.

### Write Side (Commands)

```
Controller → Command → CommandHandler → Aggregate → Repository
                                      ↓
                                   Events → Outbox
```

**Flow**:
1. Controller receives request
2. Creates command
3. Dispatches to handler
4. Handler loads aggregate
5. Aggregate executes business logic
6. Handler saves aggregate
7. Events saved to outbox

### Read Side (Queries)

```
Controller → Query → QueryHandler → Read Model
```

**Flow**:
1. Controller receives request
2. Creates query
3. Dispatches to handler
4. Handler queries optimized read model
5. Returns DTO

**Benefits**:
- Different optimization strategies
- Separate scaling (reads vs writes)
- Simplified models (no ORM complexity on reads)

## 📢 Event-Driven Architecture

Domain events enable loose coupling and eventual consistency.

### Event Flow

```
Aggregate → Records Event → Outbox → Publisher → Message Broker → Handlers
```

**Steps**:
1. **Aggregate** records event when state changes
2. **Command Handler** saves event to **Outbox** (same transaction)
3. **Background Process** publishes events from outbox
4. **Message Broker** distributes events
5. **Event Handlers** react to events

### Transactional Outbox Pattern

Solves the **dual-write problem** (writing to database AND message broker atomically).

```kotlin
// In Command Handler
suspend fun handle(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        withTransaction {
            // 1. Domain logic
            val order = Order.create(command.items)
            
            // 2. Save aggregate (database write #1)
            orderRepository.save(order)
            
            // 3. Save events to outbox (database write #2)
            // Both writes in SAME transaction!
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}

// Separate background process
class OutboxPublisher(
    private val outboxRepository: MessageOutboxRepository,
    private val eventPublisher: DomainEventPublisher
) {
    suspend fun publishPendingEvents() {
        val messages = outboxRepository.findUnpublished(limit = 100)
        
        messages.forEach { message ->
            try {
                eventPublisher.publish(message.event)
                outboxRepository.markAsPublished(message.id)
            } catch (e: Exception) {
                outboxRepository.incrementRetryCount(message.id)
            }
        }
    }
}
```

## 🏗️ Project Structure

Organize by **feature** (bounded context), not by technical layer:

```
src/main/kotlin/com/company/project/
│
├── order/                          # Feature/Bounded Context
│   ├── domain/                     # Pure business logic
│   │   ├── Order.kt                # Aggregate Root
│   │   ├── OrderItem.kt            # Entity
│   │   ├── Money.kt                # Value Object
│   │   ├── OrderRepository.kt      # Interface
│   │   └── events/
│   │       └── OrderPlacedEvent.kt
│   │
│   ├── application/                # Use cases
│   │   ├── commands/
│   │   │   ├── PlaceOrderCommand.kt
│   │   │   └── PlaceOrderCommandHandler.kt
│   │   └── queries/
│   │       ├── GetOrderQuery.kt
│   │       └── GetOrderQueryHandler.kt
│   │
│   ├── infrastructure/             # Technical details
│   │   ├── persistence/
│   │   │   ├── OrderTable.kt
│   │   │   └── OrderRepositoryImpl.kt
│   │   └── events/
│   │       └── KafkaOrderEventPublisher.kt
│   │
│   └── presentation/               # API boundary
│       ├── OrderController.kt
│       ├── PlaceOrderRequest.kt
│       └── OrderResponse.kt
│
└── shared/                         # Cross-cutting concerns
    ├── config/
    └── infrastructure/
```

## ✅ Architecture Validation

Use ArchUnit to enforce rules:

```kotlin
@Test
fun `domain layer should not depend on application layer`() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..application..")
        .check(importedClasses)
}

@Test
fun `domain layer should not depend on infrastructure`() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "..presentation..")
        .check(importedClasses)
}
```

## 🎯 Key Principles

1. **Dependency Inversion**: High-level modules don't depend on low-level modules
2. **Separation of Concerns**: Each layer has a single responsibility
3. **Explicit Over Implicit**: Clear contracts, no magic
4. **Framework Independence**: Business logic isolated from frameworks
5. **Testability**: Easy to test each layer independently

## 🚀 Next Steps

- **[Layer Responsibilities](Layer-Responsibilities.md)** - Deep dive into each layer
- **[Dependency Rules](Dependency-Rules.md)** - Understanding dependencies
- **[Design Patterns](Design-Patterns.md)** - Common patterns
- **[CQRS Implementation](CQRS-Implementation.md)** - Detailed CQRS guide
