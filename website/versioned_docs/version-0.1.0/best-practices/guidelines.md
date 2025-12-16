---
sidebar_position: 1
---

# Best Practices

Follow these guidelines to get the most out of Structus and maintain clean architecture.

## Domain Layer

### Value Objects

✅ **Do:**
- Make value objects immutable
- Validate in constructor
- Use data classes
- Implement meaningful operations

```kotlin
data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email" }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}
```

❌ **Don't:**
- Make value objects mutable
- Skip validation
- Put business logic outside value objects

### Aggregate Roots

✅ **Do:**
- Keep aggregates small
- Enforce invariants
- Record domain events
- Use factory methods

```kotlin
class Order(override val id: OrderId) : AggregateRoot<OrderId>() {
    
    fun addItem(item: OrderItem) {
        require(status == OrderStatus.DRAFT) { "Cannot modify confirmed order" }
        items.add(item)
        recordEvent(ItemAddedEvent(id.value, item.productId))
    }
    
    companion object {
        fun create(customerId: String): Order {
            val order = Order(OrderId(UUID.randomUUID().toString()))
            order.recordEvent(OrderCreatedEvent(order.id.value, customerId))
            return order
        }
    }
}
```

❌ **Don't:**
- Create god aggregates
- Skip event recording
- Allow invalid state transitions
- Expose mutable collections

### Repositories

✅ **Do:**
- Define interfaces in domain layer
- Use collection-like API
- Return domain objects
- Use suspend functions

```kotlin
interface OrderRepository : Repository {
    suspend fun findById(id: OrderId): Order?
    suspend fun save(order: Order)
    suspend fun delete(id: OrderId)
}
```

❌ **Don't:**
- Put implementation in domain layer
- Return DTOs from repositories
- Use blocking operations
- Expose database details

## Application Layer

### Commands

✅ **Do:**
- Validate in constructor
- Use descriptive names
- Keep them simple
- Make them immutable

```kotlin
data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemDto>
) : Command {
    init {
        require(customerId.isNotBlank()) { "Customer ID required" }
        require(items.isNotEmpty()) { "Order must have items" }
    }
}
```

❌ **Don't:**
- Put business logic in commands
- Use generic command names
- Make commands mutable
- Skip validation

### Command Handlers

✅ **Do:**
- Follow single responsibility
- Use Result type
- Clear events after publishing
- Handle errors gracefully

```kotlin
class CreateOrderHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        return runCatching {
            val order = Order.create(command.customerId)
            command.items.forEach { order.addItem(it.toDomain()) }
            
            orderRepository.save(order)
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

❌ **Don't:**
- Put domain logic in handlers
- Forget to clear events
- Swallow exceptions
- Mix multiple responsibilities

### Queries

✅ **Do:**
- Keep queries simple
- Use specific DTOs
- Optimize for reading
- Bypass domain model when appropriate

```kotlin
data class GetOrderQuery(val orderId: String) : Query

class GetOrderHandler(
    private val database: Database
) : QueryHandler<GetOrderQuery, OrderDto?> {
    
    override suspend operator fun invoke(query: GetOrderQuery): OrderDto? {
        return database.queryForObject(
            "SELECT * FROM orders WHERE id = ?",
            query.orderId
        )?.let { mapToDto(it) }
    }
}
```

❌ **Don't:**
- Modify state in queries
- Use domain model for queries
- Return entities from queries
- Over-complicate read models

## Infrastructure Layer

### Repository Implementation

✅ **Do:**
- Map between domain and persistence models
- Handle transactions properly
- Use appropriate database features
- Log errors

```kotlin
class OrderRepositoryImpl(
    private val database: Database
) : OrderRepository {
    
    override suspend fun save(order: Order) {
        database.transaction {
            val model = order.toPersistenceModel()
            execute("INSERT INTO orders (...) VALUES (...)", model)
        }
    }
    
    override suspend fun findById(id: OrderId): Order? {
        return database.queryForObject(
            "SELECT * FROM orders WHERE id = ?",
            id.value
        )?.toDomainModel()
    }
}
```

❌ **Don't:**
- Expose database details to domain
- Skip error handling
- Forget transactions
- Return persistence models

## Event Handling

### Domain Events

✅ **Do:**
- Use past tense naming
- Include all relevant data
- Make events immutable
- Version your events

```kotlin
data class OrderCreatedEvent(
    override val aggregateId: String,
    val customerId: String,
    val createdAt: Instant
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)
```

❌ **Don't:**
- Use present tense
- Include mutable data
- Forget event versioning
- Put business logic in events

### Transactional Outbox

✅ **Do:**
- Save events in same transaction
- Clear events after saving
- Handle publish failures
- Implement retry logic

```kotlin
override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        database.transaction {
            val order = Order.create(command.customerId)
            orderRepository.save(order)
            
            // Save to outbox in same transaction
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

❌ **Don't:**
- Publish events directly
- Forget to clear events
- Skip retry logic
- Ignore publish failures

## Testing

### Unit Tests

✅ **Do:**
- Test business logic in isolation
- Use mocks for dependencies
- Test edge cases
- Keep tests simple

```kotlin
@Test
fun `should create order with valid items`() = runTest {
    val command = CreateOrderCommand("CUST-1", listOf(item))
    val handler = CreateOrderHandler(mockRepo, mockOutbox)
    
    val result = handler(command)
    
    assertTrue(result.isSuccess)
    verify(mockRepo).save(any())
}
```

❌ **Don't:**
- Test framework code
- Use real databases in unit tests
- Test multiple things at once
- Skip error cases

## General Guidelines

### Naming Conventions

- **Commands**: Imperative (CreateOrder, CancelOrder)
- **Queries**: Question (GetOrderById, FindActiveOrders)
- **Events**: Past tense (OrderCreated, OrderCancelled)
- **Handlers**: CommandName + Handler (CreateOrderHandler)

### Error Handling

✅ **Do:**
- Use Result type for expected errors
- Use exceptions for unexpected errors
- Provide meaningful error messages
- Log errors appropriately

❌ **Don't:**
- Use exceptions for control flow
- Swallow exceptions
- Return null for errors
- Use generic error messages

### Dependencies

✅ **Do:**
- Inject dependencies via constructor
- Depend on interfaces, not implementations
- Keep dependencies minimal
- Use dependency injection

❌ **Don't:**
- Use service locators
- Create dependencies inside classes
- Depend on concrete implementations
- Have circular dependencies

## Performance

### Optimization Tips

1. **Use Projections**: Create optimized read models
2. **Cache Queries**: Cache frequently accessed data
3. **Batch Operations**: Process events in batches
4. **Index Wisely**: Add database indexes for queries
5. **Monitor**: Track performance metrics

### Common Pitfalls

❌ Avoid:
- N+1 query problems
- Loading entire aggregates for queries
- Synchronous event publishing
- Missing database indexes
- Over-fetching data

## Next Steps

- **[Common Mistakes](common-mistakes)** - Learn what to avoid
- **[Testing Strategies](testing-strategies)** - Comprehensive testing guide
- **[CQRS Implementation](../advanced/cqrs-implementation)** - Advanced patterns
