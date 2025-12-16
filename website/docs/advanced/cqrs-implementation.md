---
sidebar_position: 1
---

# CQRS Implementation

Learn how to implement Command Query Responsibility Segregation (CQRS) with Structus.

## What is CQRS?

**CQRS** separates read and write operations into different models:

- **Commands**: Change state (write operations)
- **Queries**: Retrieve data (read operations)

## Why CQRS?

### Benefits

1. **Optimized Performance**: Separate read and write models can be optimized independently
2. **Scalability**: Scale reads and writes separately
3. **Clarity**: Clear separation between operations that change state and those that don't
4. **Flexibility**: Use different data stores for reads and writes

### When to Use

✅ **Use CQRS when:**
- Complex business logic requires different read/write models
- Need to scale reads and writes independently
- Want to optimize queries separately from commands
- Building event-sourced systems

❌ **Don't use CQRS when:**
- Simple CRUD operations are sufficient
- Read and write models are identical
- Team is unfamiliar with the pattern
- Added complexity isn't justified

## Command Side (Write)

### 1. Define Commands

Commands represent intent to change state.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.Command

data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemDto>,
    val shippingAddress: AddressDto
) : Command {
    init {
        require(customerId.isNotBlank()) { "Customer ID required" }
        require(items.isNotEmpty()) { "Order must have items" }
    }
}

data class CancelOrderCommand(
    val orderId: String,
    val reason: String
) : Command {
    init {
        require(orderId.isNotBlank()) { "Order ID required" }
        require(reason.isNotBlank()) { "Cancellation reason required" }
    }
}
```

### 2. Implement Command Handlers

Command handlers execute business logic through the domain model.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler

class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(
        command: CreateOrderCommand
    ): Result<OrderId> {
        return runCatching {
            // 1. Validate customer exists
            val customer = customerRepository.findById(CustomerId(command.customerId))
                ?: throw IllegalArgumentException("Customer not found")
            
            // 2. Create order through domain model
            val order = Order.create(
                customerId = customer.id,
                items = command.items.map { it.toDomainModel() },
                shippingAddress = command.shippingAddress.toDomainModel()
            )
            
            // 3. Apply business rules
            order.validateMinimumAmount()
            order.calculateTotal()
            
            // 4. Save aggregate
            orderRepository.save(order)
            
            // 5. Save events to outbox
            order.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // 6. Clear events
            order.clearEvents()
            
            order.id
        }
    }
}
```

### 3. Command Bus (Optional)

A command bus can route commands to their handlers.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandBus
import kotlin.reflect.KClass

class InMemoryCommandBus : CommandBus {
    private val handlers = mutableMapOf<KClass<*>, CommandHandler<*, *>>()
    
    override fun <C : Command, R> register(
        commandClass: KClass<C>,
        handler: CommandHandler<C, R>
    ) {
        handlers[commandClass] = handler
    }
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun <C : Command, R> dispatch(command: C): R {
        val handler = handlers[command::class] as? CommandHandler<C, R>
            ?: throw IllegalArgumentException("No handler for ${command::class}")
        
        return handler(command)
    }
}

// Usage
val commandBus = InMemoryCommandBus()
commandBus.register(CreateOrderCommand::class, createOrderHandler)

val result = commandBus.dispatch(CreateOrderCommand(...))
```

## Query Side (Read)

### 1. Define Queries

Queries represent requests for data.

```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.Query

data class GetOrderByIdQuery(
    val orderId: String
) : Query

data class GetCustomerOrdersQuery(
    val customerId: String,
    val status: OrderStatus? = null,
    val page: Int = 0,
    val size: Int = 20
) : Query

data class SearchOrdersQuery(
    val searchTerm: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) : Query
```

### 2. Define Read Models (DTOs)

Read models are optimized for display.

```kotlin
data class OrderDto(
    val id: String,
    val customerId: String,
    val customerName: String,
    val status: String,
    val totalAmount: Double,
    val itemCount: Int,
    val createdAt: String
)

data class OrderDetailDto(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val status: String,
    val items: List<OrderItemDto>,
    val shippingAddress: AddressDto,
    val totalAmount: Double,
    val createdAt: String,
    val updatedAt: String
)

data class OrderSummaryDto(
    val totalOrders: Int,
    val totalRevenue: Double,
    val averageOrderValue: Double,
    val ordersByStatus: Map<String, Int>
)
```

### 3. Implement Query Handlers

Query handlers retrieve data optimized for reading.

```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler

class GetOrderByIdQueryHandler(
    private val database: Database
) : QueryHandler<GetOrderByIdQuery, OrderDetailDto?> {
    
    override suspend operator fun invoke(
        query: GetOrderByIdQuery
    ): OrderDetailDto? {
        // Direct database query, bypassing domain model
        return database.queryForObject("""
            SELECT 
                o.id,
                o.customer_id,
                c.name as customer_name,
                c.email as customer_email,
                o.status,
                o.total_amount,
                o.created_at,
                o.updated_at
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            WHERE o.id = ?
        """, query.orderId)?.let { row ->
            OrderDetailDto(
                id = row.getString("id"),
                customerId = row.getString("customer_id"),
                customerName = row.getString("customer_name"),
                customerEmail = row.getString("customer_email"),
                status = row.getString("status"),
                items = loadOrderItems(row.getString("id")),
                shippingAddress = loadShippingAddress(row.getString("id")),
                totalAmount = row.getDouble("total_amount"),
                createdAt = row.getString("created_at"),
                updatedAt = row.getString("updated_at")
            )
        }
    }
}
```

### 4. Optimized Queries

Use database-specific features for performance.

```kotlin
class SearchOrdersQueryHandler(
    private val database: Database
) : QueryHandler<SearchOrdersQuery, List<OrderDto>> {
    
    override suspend operator fun invoke(
        query: SearchOrdersQuery
    ): List<OrderDto> {
        // Use full-text search, indexes, etc.
        return database.query("""
            SELECT 
                o.id,
                o.customer_id,
                c.name as customer_name,
                o.status,
                o.total_amount,
                COUNT(oi.id) as item_count,
                o.created_at
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            LEFT JOIN order_items oi ON o.id = oi.order_id
            WHERE 
                to_tsvector('english', c.name || ' ' || o.id) 
                @@ plainto_tsquery('english', ?)
                ${if (query.startDate != null) "AND o.created_at >= ?" else ""}
                ${if (query.endDate != null) "AND o.created_at <= ?" else ""}
            GROUP BY o.id, c.name
            ORDER BY o.created_at DESC
        """, buildQueryParams(query))
            .map { mapToOrderDto(it) }
    }
}
```

## Separate Read/Write Databases

For advanced scenarios, use separate databases.

### Write Database (Normalized)

```kotlin
class OrderCommandRepository(
    private val writeDb: Database
) : OrderRepository {
    
    override suspend fun save(order: Order) {
        writeDb.transaction {
            // Save to normalized tables
            execute("INSERT INTO orders (...) VALUES (...)")
            order.items.forEach { item ->
                execute("INSERT INTO order_items (...) VALUES (...)")
            }
        }
    }
}
```

### Read Database (Denormalized)

```kotlin
class OrderQueryRepository(
    private val readDb: Database
) {
    
    suspend fun findById(orderId: String): OrderDetailDto? {
        // Query denormalized view
        return readDb.queryForObject(
            "SELECT * FROM order_details_view WHERE id = ?",
            orderId
        )?.let { mapToDto(it) }
    }
}
```

### Synchronization via Events

```kotlin
class OrderProjectionHandler : DomainEventHandler<OrderCreatedEvent> {
    
    override suspend fun handle(event: OrderCreatedEvent) {
        // Update read database
        readDb.execute("""
            INSERT INTO order_details_view (
                id, customer_id, customer_name, status, ...
            ) VALUES (?, ?, ?, ?, ...)
        """, event.orderId, event.customerId, ...)
    }
}
```

## Best Practices

### ✅ Do

- Use domain model for commands
- Optimize queries for reading
- Keep read models simple
- Use projections for complex queries
- Cache query results when appropriate

### ❌ Don't

- Use domain model for queries
- Put business logic in query handlers
- Make queries modify state
- Over-complicate read models
- Forget to handle eventual consistency

## Testing

### Command Handler Tests

```kotlin
@Test
fun `should create order successfully`() = runTest {
    // Arrange
    val command = CreateOrderCommand(...)
    val handler = CreateOrderCommandHandler(mockRepository, mockOutbox)
    
    // Act
    val result = handler(command)
    
    // Assert
    assertTrue(result.isSuccess)
    verify(mockRepository).save(any())
}
```

### Query Handler Tests

```kotlin
@Test
fun `should return order by id`() = runTest {
    // Arrange
    val query = GetOrderByIdQuery("ORDER-123")
    val handler = GetOrderByIdQueryHandler(mockDatabase)
    
    // Act
    val result = handler(query)
    
    // Assert
    assertNotNull(result)
    assertEquals("ORDER-123", result.id)
}
```

## Next Steps

- **[Transactional Outbox Pattern](transactional-outbox)** - Event publishing
- **[Event-Driven Architecture](event-driven)** - Building with events
- **[Testing Strategies](../best-practices/testing-strategies)** - Testing CQRS
