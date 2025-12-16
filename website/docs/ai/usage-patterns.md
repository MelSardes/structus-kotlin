---
sidebar_position: 3
---

# Usage Patterns

Common implementation patterns and anti-patterns to help generate correct code.

## ✅ Correct Patterns

### Pattern 1: Creating an Aggregate Root

```kotlin
// Domain Layer
import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.ValueObject

data class OrderId(val value: String) : ValueObject

class Order(
    override val id: OrderId,
    val customerId: String,
    val status: OrderStatus
) : AggregateRoot<OrderId>() {
    
    enum class OrderStatus { DRAFT, CONFIRMED, SHIPPED }
    
    fun confirm(): Result<Unit> {
        if (status != OrderStatus.DRAFT) {
            return Result.failure(IllegalStateException("Cannot confirm"))
        }
        // Business logic here
        recordEvent(OrderConfirmedEvent(id.value))
        return Result.success(Unit)
    }
}
```

**Why this is correct**:
- ✅ Extends `AggregateRoot<OrderId>`
- ✅ Uses value objects for IDs
- ✅ Business logic in domain methods
- ✅ Returns `Result<T>` for operations
- ✅ Records domain events

### Pattern 2: Implementing Commands

```kotlin
// Application Layer
import com.melsardes.libraries.structuskotlin.application.commands.Command
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler

data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItem>
) : Command

class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(
        command: CreateOrderCommand
    ): Result<OrderId> {
        return runCatching {
            // 1. Create aggregate
            val order = Order.create(command.customerId, command.items)
            
            // 2. Save aggregate
            orderRepository.save(order)
            
            // 3. Save events to outbox
            order.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // 4. Clear events
            order.clearEvents()
            
            order.id
        }
    }
}
```

**Why this is correct**:
- ✅ Command is a data class implementing `Command`
- ✅ Handler implements `CommandHandler<Command, Result>`
- ✅ Uses `suspend` for async operations
- ✅ Returns `Result<T>`
- ✅ Follows Transactional Outbox Pattern

### Pattern 3: Implementing Queries

```kotlin
// Application Layer
import com.melsardes.libraries.structuskotlin.application.queries.Query
import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler

data class GetOrderByIdQuery(val orderId: String) : Query

data class OrderDto(
    val id: String,
    val customerId: String,
    val status: String
)

class GetOrderByIdQueryHandler(
    private val orderRepository: OrderRepository
) : QueryHandler<GetOrderByIdQuery, OrderDto?> {
    
    override suspend operator fun invoke(
        query: GetOrderByIdQuery
    ): OrderDto? {
        val order = orderRepository.findById(OrderId(query.orderId))
        return order?.let {
            OrderDto(
                id = it.id.value,
                customerId = it.customerId,
                status = it.status.name
            )
        }
    }
}
```

**Why this is correct**:
- ✅ Query implements `Query`
- ✅ Handler implements `QueryHandler<Query, Result>`
- ✅ Returns DTO, not domain object
- ✅ Read-only operation
- ✅ No state changes

### Pattern 4: Repository Interface

```kotlin
// Domain Layer
import com.melsardes.libraries.structuskotlin.domain.Repository

interface OrderRepository : Repository {
    suspend fun findById(id: OrderId): Order?
    suspend fun save(order: Order)
    suspend fun delete(id: OrderId)
}
```

**Why this is correct**:
- ✅ Interface defined in domain layer
- ✅ All methods are `suspend` functions
- ✅ No implementation details

### Pattern 5: Domain Events

```kotlin
// Domain Layer
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

data class OrderCreatedEvent(
    override val aggregateId: String,
    val orderId: String,
    val customerId: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)
```

**Why this is correct**:
- ✅ Extends `BaseDomainEvent`
- ✅ Immutable data class
- ✅ Past tense naming
- ✅ Defined in domain layer

## ❌ Anti-Patterns

### Anti-Pattern 1: Framework Dependencies in Domain

```kotlin
// ❌ WRONG
import org.springframework.stereotype.Component // ❌ NO!

@Component // ❌ WRONG!
class Order : AggregateRoot<OrderId>()
```

**Why wrong**: Domain layer must be framework-agnostic.

**Correct**: Keep domain pure, no framework annotations.

### Anti-Pattern 2: Mutable Entities

```kotlin
// ❌ WRONG
class Order(
    override val id: OrderId,
    var status: OrderStatus // ❌ var instead of val
) : AggregateRoot<OrderId>() {
    fun confirm() {
        status = OrderStatus.CONFIRMED // ❌ Direct mutation
    }
}
```

**Why wrong**: Makes tracking changes difficult.

**Correct**: Use immutable properties, return new instances.

### Anti-Pattern 3: Business Logic in Handlers

```kotlin
// ❌ WRONG
class CreateOrderHandler : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        // ❌ Business logic here
        if (command.items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty order"))
        }
        // ...
    }
}
```

**Why wrong**: Business logic belongs in domain.

**Correct**: Put business logic in domain entities.

### Anti-Pattern 4: Mixing Commands and Queries

```kotlin
// ❌ WRONG - Command that returns data
class CreateOrderHandler : CommandHandler<CreateOrderCommand, OrderDto> {
    override suspend operator fun invoke(command: CreateOrderCommand): OrderDto {
        val order = createOrder(command)
        orderRepository.save(order)
        return order.toDto() // ❌ Returning data from command
    }
}
```

**Why wrong**: Violates CQRS principle.

**Correct**: Commands return IDs, queries return data.

## 🎯 Pattern Selection Guide

| Scenario | Pattern to Use |
|----------|---------------|
| Creating entity | Aggregate Root Pattern |
| Changing state | Command + CommandHandler |
| Reading data | Query + QueryHandler |
| Notifying changes | Domain Event |
| Defining persistence | Repository Interface |
| Validating rules | Domain methods with Result&lt;T&gt; |

## 📚 Related

- **[Library Overview](library-overview)** - Core concepts
- **[Code Templates](code-templates)** - Ready-to-use code
- **[AI Prompts](prompts)** - Prompt templates

---

**Remember**: When in doubt, ask "Where does this logic belong?"
