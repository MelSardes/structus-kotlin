---
sidebar_position: 1
---

# Domain Modeling Recipes

This guide provides practical recipes for effective domain modeling with Structus.

## Implementing Value Objects

Value objects represent concepts that are identified by their attributes rather than by an identity. Here are some practical recipes for implementing value objects in Structus:

### Basic Value Object

```kotlin
data class EmailAddress(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format: $value" }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}
```

### Money Value Object

```kotlin
data class Money(val amount: BigDecimal, val currency: Currency) : ValueObject {
    init {
        require(amount.scale() <= 2) { "Money amount cannot have more than 2 decimal places" }
    }
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add money with different currencies" }
        return Money(amount + other.amount, currency)
    }
    
    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract money with different currencies" }
        return Money(amount - other.amount, currency)
    }
    
    companion object {
        fun zero(currency: Currency): Money = Money(BigDecimal.ZERO, currency)
    }
}
```

## Creating Aggregate Roots

Aggregate roots are the entry point for a cluster of domain objects that should be treated as a unit. Here's how to implement them effectively:

### Order Aggregate

```kotlin
class Order private constructor(
    override val id: OrderId,
    private val customerId: CustomerId,
    private val _items: MutableList<OrderItem> = mutableListOf(),
    private var status: OrderStatus = OrderStatus.DRAFT
) : AggregateRoot<OrderId>() {

    val items: List<OrderItem> get() = _items.toList()
    
    fun addItem(item: OrderItem) {
        requireDraft()
        _items.add(item)
        recordEvent(OrderItemAddedEvent(id.value, item.productId.value, item.quantity))
    }
    
    fun removeItem(productId: ProductId) {
        requireDraft()
        val removed = _items.removeIf { it.productId == productId }
        if (removed) {
            recordEvent(OrderItemRemovedEvent(id.value, productId.value))
        }
    }
    
    fun confirm() {
        requireDraft()
        require(_items.isNotEmpty()) { "Cannot confirm an empty order" }
        status = OrderStatus.CONFIRMED
        recordEvent(OrderConfirmedEvent(id.value))
    }
    
    fun cancel(reason: String) {
        require(status != OrderStatus.CANCELLED) { "Order is already cancelled" }
        require(status != OrderStatus.DELIVERED) { "Delivered orders cannot be cancelled" }
        
        status = OrderStatus.CANCELLED
        recordEvent(OrderCancelledEvent(id.value, reason))
    }
    
    fun totalAmount(): Money {
        if (_items.isEmpty()) return Money.zero(Currency.getInstance("USD"))
        
        val currency = _items.first().price.currency
        val total = _items.sumOf { it.quantity * it.price.amount }
        return Money(total, currency)
    }
    
    private fun requireDraft() {
        require(status == OrderStatus.DRAFT) { 
            "Operation not allowed. Order status is $status"
        }
    }
    
    enum class OrderStatus {
        DRAFT, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
    
    companion object {
        fun create(id: OrderId, customerId: CustomerId): Order {
            val order = Order(id, customerId)
            order.recordEvent(OrderCreatedEvent(id.value, customerId.value))
            return order
        }
    }
}
```

## Working with Domain Events

Domain events represent something that happened in the domain. Here's how to effectively use them:

### Event Definition

```kotlin
data class OrderCreatedEvent(
    override val aggregateId: String,
    val customerId: String,
    override val occurredAt: Instant = Instant.now()
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)
```

### Publishing Events from Repositories

```kotlin
class OrderRepositoryImpl(
    private val database: Database,
    private val outboxRepository: OutboxRepository
) : OrderRepository {
    
    override suspend fun save(order: Order) {
        database.transaction {
            // 1. Save the aggregate
            val orderTable = order.toOrderTable()
            database.execute("INSERT INTO orders (...) VALUES (...)", orderTable)
            
            // 2. Save each order item
            order.items.forEach { item ->
                database.execute("INSERT INTO order_items (...) VALUES (...)", item.toItemTable())
            }
            
            // 3. Save events to the outbox (same transaction)
            order.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
        }
    }
}
```

## Best Practices

### Use Factory Methods

Always use factory methods to create aggregates. This ensures that all invariants are satisfied and all necessary events are recorded:

```kotlin
// Avoid exposing constructors
private constructor(...)

// Use static factory methods
companion object {
    fun create(...): Entity {
        // Create entity
        // Validate invariants
        // Record creation event
        return entity
    }
}
```

### Protect Invariants

Always protect your domain invariants by:

1. Using validation in constructors and methods
2. Making state private and exposing it through getters
3. Using immutable collections or defensive copying

### Keep Aggregates Small

Focus on maintaining small, focused aggregates. If an aggregate becomes too complex, it might indicate a need for splitting it into multiple aggregates with their own boundaries.

## Next Steps

Once you've mastered these recipes, check out the [Event Sourcing](event-sourcing) guide to learn how to implement event sourcing with Structus.