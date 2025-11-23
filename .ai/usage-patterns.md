# Structus Usage Patterns for AI Agents

This document provides common implementation patterns and anti-patterns to help AI agents generate correct code.

## ✅ Correct Patterns

### Pattern 1: Creating an Aggregate Root

**Scenario**: User wants to create a new aggregate (e.g., Order, Product, Customer)

```kotlin
// Domain Layer: src/main/kotlin/com/example/domain/order/Order.kt
package com.example.domain.order

import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.ValueObject
import java.time.Instant
import java.util.UUID

// Value Objects
data class OrderId(val value: UUID) : ValueObject
data class Money(val amount: Double, val currency: String) : ValueObject
data class OrderLine(
    val productId: String,
    val quantity: Int,
    val unitPrice: Money
) : ValueObject

// Aggregate Root
class Order(
    id: OrderId,
    val customerId: String,
    val lines: List<OrderLine>,
    val status: OrderStatus,
    val createdAt: Instant
) : AggregateRoot<OrderId>(id) {
    
    enum class OrderStatus {
        DRAFT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
    
    // Business logic methods
    fun confirm(): Result<Order> {
        if (status != OrderStatus.DRAFT) {
            return Result.Failure(DomainError.InvalidOperation("Cannot confirm non-draft order"))
        }
        if (lines.isEmpty()) {
            return Result.Failure(DomainError.ValidationError("Cannot confirm empty order"))
        }
        return Result.Success(copy(status = OrderStatus.CONFIRMED))
    }
    
    fun totalAmount(): Money {
        val total = lines.sumOf { it.quantity * it.unitPrice.amount }
        return Money(total, lines.first().unitPrice.currency)
    }
    
    private fun copy(
        id: OrderId = this.id,
        customerId: String = this.customerId,
        lines: List<OrderLine> = this.lines,
        status: OrderStatus = this.status,
        createdAt: Instant = this.createdAt
    ) = Order(id, customerId, lines, status, createdAt)
}
```

**Why this is correct**:
- ✅ Extends `AggregateRoot<OrderId>`
- ✅ Uses value objects for complex types
- ✅ Business logic in domain methods
- ✅ Returns `Result<T>` for operations that can fail
- ✅ Immutable (uses copy for changes)

---

### Pattern 2: Implementing CQRS with Commands

**Scenario**: User wants to add a command to create an order

```kotlin
// Application Layer: src/main/kotlin/com/example/application/commands/CreateOrderCommand.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.Command

data class CreateOrderCommand(
    val customerId: String,
    val lines: List<OrderLineDto>
) : Command

data class OrderLineDto(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val currency: String
)

// Command Handler
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import com.melsardes.libraries.structuskotlin.domain.Result
import com.example.domain.order.*

class CreateOrderCommandHandler(
    private val orderRepository: OrderCommandRepository,
    private val eventPublisher: DomainEventPublisher
) : CommandHandler<CreateOrderCommand, OrderId> {
    
    override suspend fun handle(command: CreateOrderCommand): Result<OrderId> {
        // 1. Validate
        if (command.lines.isEmpty()) {
            return Result.Failure(DomainError.ValidationError("Order must have at least one line"))
        }
        
        // 2. Create domain object
        val orderId = OrderId(UUID.randomUUID())
        val lines = command.lines.map { 
            OrderLine(it.productId, it.quantity, Money(it.unitPrice, it.currency))
        }
        val order = Order(
            id = orderId,
            customerId = command.customerId,
            lines = lines,
            status = Order.OrderStatus.DRAFT,
            createdAt = Instant.now()
        )
        
        // 3. Persist
        return when (val result = orderRepository.save(order)) {
            is Result.Success -> {
                // 4. Publish event
                val event = OrderCreatedEvent(
                    eventId = UUID.randomUUID().toString(),
                    occurredOn = Instant.now(),
                    aggregateId = orderId.value.toString(),
                    orderId = orderId,
                    customerId = command.customerId
                )
                eventPublisher.publish(event)
                Result.Success(orderId)
            }
            is Result.Failure -> result
        }
    }
}
```

**Why this is correct**:
- ✅ Command is a data class implementing `Command`
- ✅ Handler implements `CommandHandler<Command, Result>`
- ✅ Uses `suspend` for async operations
- ✅ Returns `Result<T>`
- ✅ Validates before creating domain object
- ✅ Publishes domain event after persistence

---

### Pattern 3: Implementing CQRS with Queries

**Scenario**: User wants to query orders

```kotlin
// Application Layer: src/main/kotlin/com/example/application/queries/GetOrderByIdQuery.kt
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.Query
import com.example.domain.order.OrderId

data class GetOrderByIdQuery(val orderId: OrderId) : Query<OrderDto>

// Query Result DTO
data class OrderDto(
    val id: String,
    val customerId: String,
    val lines: List<OrderLineDto>,
    val status: String,
    val totalAmount: Double,
    val currency: String,
    val createdAt: String
)

// Query Handler
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler
import com.melsardes.libraries.structuskotlin.domain.Result
import com.example.domain.order.OrderQueryRepository

class GetOrderByIdQueryHandler(
    private val orderRepository: OrderQueryRepository
) : QueryHandler<GetOrderByIdQuery, OrderDto> {
    
    override suspend fun handle(query: GetOrderByIdQuery): Result<OrderDto> {
        return when (val result = orderRepository.findById(query.orderId)) {
            is Result.Success -> {
                val order = result.value
                val dto = OrderDto(
                    id = order.id.value.toString(),
                    customerId = order.customerId,
                    lines = order.lines.map { 
                        OrderLineDto(it.productId, it.quantity, it.unitPrice.amount, it.unitPrice.currency)
                    },
                    status = order.status.name,
                    totalAmount = order.totalAmount().amount,
                    currency = order.totalAmount().currency,
                    createdAt = order.createdAt.toString()
                )
                Result.Success(dto)
            }
            is Result.Failure -> result
        }
    }
}
```

**Why this is correct**:
- ✅ Query implements `Query<ResultType>`
- ✅ Handler implements `QueryHandler<Query, Result>`
- ✅ Returns DTO, not domain object
- ✅ Read-only operation
- ✅ No state changes

---

### Pattern 4: Repository Interface in Domain

**Scenario**: User needs to define repository contracts

```kotlin
// Domain Layer: src/main/kotlin/com/example/domain/order/OrderRepository.kt
package com.example.domain.order

import com.melsardes.libraries.structuskotlin.domain.CommandRepository
import com.melsardes.libraries.structuskotlin.domain.QueryRepository
import com.melsardes.libraries.structuskotlin.domain.Result

// Command Repository (Write operations)
interface OrderCommandRepository : CommandRepository<Order, OrderId> {
    suspend fun save(order: Order): Result<Unit>
    suspend fun update(order: Order): Result<Unit>
    suspend fun delete(orderId: OrderId): Result<Unit>
}

// Query Repository (Read operations)
interface OrderQueryRepository : QueryRepository<Order, OrderId> {
    suspend fun findById(orderId: OrderId): Result<Order>
    suspend fun findByCustomerId(customerId: String): Result<List<Order>>
    suspend fun findByStatus(status: Order.OrderStatus): Result<List<Order>>
}
```

**Why this is correct**:
- ✅ Interfaces defined in domain layer
- ✅ Separate command and query repositories
- ✅ All methods return `Result<T>`
- ✅ All methods are `suspend` functions
- ✅ No implementation details

---

### Pattern 5: Domain Events

**Scenario**: User wants to publish domain events

```kotlin
// Domain Layer: src/main/kotlin/com/example/domain/order/OrderEvents.kt
package com.example.domain.order

import com.melsardes.libraries.structuskotlin.domain.events.DomainEvent
import java.time.Instant

data class OrderCreatedEvent(
    override val eventId: String,
    override val occurredOn: Instant,
    override val aggregateId: String,
    val orderId: OrderId,
    val customerId: String
) : DomainEvent {
    override val eventType: String = "OrderCreated"
}

data class OrderConfirmedEvent(
    override val eventId: String,
    override val occurredOn: Instant,
    override val aggregateId: String,
    val orderId: OrderId
) : DomainEvent {
    override val eventType: String = "OrderConfirmed"
}
```

**Why this is correct**:
- ✅ Implements `DomainEvent` interface
- ✅ Immutable data classes
- ✅ Contains all required fields
- ✅ Past tense naming
- ✅ Defined in domain layer

---

## ❌ Anti-Patterns (What NOT to do)

### Anti-Pattern 1: Framework Dependencies in Domain

```kotlin
// ❌ WRONG - Spring annotations in domain layer
package com.example.domain.order

import org.springframework.stereotype.Component // ❌ NO!
import javax.persistence.Entity // ❌ NO!
import javax.persistence.Id // ❌ NO!

@Entity // ❌ WRONG!
class Order(
    @Id // ❌ WRONG!
    val id: String,
    val customerId: String
) : AggregateRoot<String>(id)
```

**Why this is wrong**: Domain layer must be framework-agnostic.

**Correct approach**: Keep domain pure, add framework annotations in infrastructure layer.

---

### Anti-Pattern 2: Throwing Exceptions for Business Logic

```kotlin
// ❌ WRONG - Throwing exceptions
class Order {
    fun confirm() {
        if (status != OrderStatus.DRAFT) {
            throw IllegalStateException("Cannot confirm non-draft order") // ❌ NO!
        }
        status = OrderStatus.CONFIRMED
    }
}
```

**Why this is wrong**: Exceptions are for exceptional cases, not business rules.

**Correct approach**: Return `Result<T>`

```kotlin
// ✅ CORRECT
fun confirm(): Result<Order> {
    if (status != OrderStatus.DRAFT) {
        return Result.Failure(DomainError.InvalidOperation("Cannot confirm non-draft order"))
    }
    return Result.Success(copy(status = OrderStatus.CONFIRMED))
}
```

---

### Anti-Pattern 3: Mutable Entities

```kotlin
// ❌ WRONG - Mutable properties
class Order(
    id: OrderId,
    var customerId: String, // ❌ var instead of val
    var status: OrderStatus  // ❌ var instead of val
) : AggregateRoot<OrderId>(id) {
    
    fun confirm() {
        status = OrderStatus.CONFIRMED // ❌ Direct mutation
    }
}
```

**Why this is wrong**: Makes tracking changes difficult, breaks immutability.

**Correct approach**: Use immutable properties and return new instances

```kotlin
// ✅ CORRECT
class Order(
    id: OrderId,
    val customerId: String,
    val status: OrderStatus
) : AggregateRoot<OrderId>(id) {
    
    fun confirm(): Result<Order> {
        return Result.Success(copy(status = OrderStatus.CONFIRMED))
    }
    
    private fun copy(status: OrderStatus = this.status) = 
        Order(id, customerId, status)
}
```

---

### Anti-Pattern 4: Business Logic in Handlers

```kotlin
// ❌ WRONG - Business logic in handler
class CreateOrderCommandHandler : CommandHandler<CreateOrderCommand, OrderId> {
    override suspend fun handle(command: CreateOrderCommand): Result<OrderId> {
        // ❌ Business logic here instead of in domain
        if (command.lines.isEmpty()) {
            return Result.Failure(DomainError.ValidationError("Empty order"))
        }
        
        val total = command.lines.sumOf { it.quantity * it.unitPrice } // ❌ Calculation here
        
        if (total > 10000) { // ❌ Business rule here
            return Result.Failure(DomainError.ValidationError("Order too large"))
        }
        
        // ...
    }
}
```

**Why this is wrong**: Business logic belongs in domain, not application layer.

**Correct approach**: Put business logic in domain entities

```kotlin
// ✅ CORRECT - Business logic in domain
class Order {
    fun validate(): Result<Unit> {
        if (lines.isEmpty()) {
            return Result.Failure(DomainError.ValidationError("Empty order"))
        }
        if (totalAmount().amount > 10000) {
            return Result.Failure(DomainError.ValidationError("Order too large"))
        }
        return Result.Success(Unit)
    }
}

// Handler just orchestrates
class CreateOrderCommandHandler : CommandHandler<CreateOrderCommand, OrderId> {
    override suspend fun handle(command: CreateOrderCommand): Result<OrderId> {
        val order = createOrderFromCommand(command)
        return when (val validation = order.validate()) {
            is Result.Success -> orderRepository.save(order)
            is Result.Failure -> validation
        }
    }
}
```

---

### Anti-Pattern 5: Mixing Commands and Queries

```kotlin
// ❌ WRONG - Command that returns data
class CreateOrderCommand : Command

class CreateOrderCommandHandler : CommandHandler<CreateOrderCommand, OrderDto> { // ❌ Returns DTO
    override suspend fun handle(command: CreateOrderCommand): Result<OrderDto> {
        val order = createOrder(command)
        orderRepository.save(order)
        
        // ❌ Querying after command
        return orderRepository.findById(order.id).map { it.toDto() }
    }
}
```

**Why this is wrong**: Violates CQRS principle.

**Correct approach**: Commands return IDs, queries return data

```kotlin
// ✅ CORRECT - Command returns ID only
class CreateOrderCommandHandler : CommandHandler<CreateOrderCommand, OrderId> {
    override suspend fun handle(command: CreateOrderCommand): Result<OrderId> {
        val order = createOrder(command)
        return orderRepository.save(order).map { order.id }
    }
}

// Separate query for getting data
class GetOrderByIdQueryHandler : QueryHandler<GetOrderByIdQuery, OrderDto> {
    override suspend fun handle(query: GetOrderByIdQuery): Result<OrderDto> {
        return orderRepository.findById(query.orderId).map { it.toDto() }
    }
}
```

---

## 🎯 Pattern Selection Guide

| Scenario | Pattern to Use |
|----------|---------------|
| Creating a new entity | Aggregate Root Pattern |
| Changing state | Command + CommandHandler |
| Reading data | Query + QueryHandler |
| Notifying of changes | Domain Event + EventPublisher |
| Defining persistence | Repository Interface (in domain) |
| Implementing persistence | Repository Implementation (in infrastructure) |
| Validating business rules | Domain methods returning Result<T> |
| Handling errors | Result<T> pattern, not exceptions |

## 📚 Related Files

- [Library Overview](./library-overview.md) - Core concepts
- [Code Templates](./code-templates.md) - Ready-to-use code
- [Troubleshooting](./troubleshooting.md) - Common issues

---

**Remember**: When in doubt, ask "Where does this logic belong?" and "Am I following CQRS?"
