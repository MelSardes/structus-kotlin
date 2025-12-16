---
sidebar_position: 1
---

# API Reference Overview

This section provides detailed API documentation for all Structus components.

## Package Structure

```
com.melsardes.libraries.structuskotlin
├── domain/
│   ├── Entity
│   ├── ValueObject
│   ├── AggregateRoot
│   ├── Repository
│   ├── MessageOutboxRepository
│   ├── OutboxMessage
│   └── events/
│       ├── DomainEvent
│       └── BaseDomainEvent
└── application/
    ├── commands/
    │   ├── Command
    │   ├── CommandHandler
    │   └── CommandBus
    ├── queries/
    │   ├── Query
    │   └── QueryHandler
    └── events/
        ├── DomainEventPublisher
        └── DomainEventHandler
```

## Quick Links

- **[Domain API](domain-api)** - Entities, Aggregates, Value Objects
- **[Application API](application-api)** - Commands, Queries, Handlers
- **[Events API](events-api)** - Domain Events, Publishers

## KDoc Documentation

For complete API documentation with detailed descriptions, parameters, and examples, see the generated KDoc:

:::info Coming Soon
KDoc documentation will be published at a dedicated URL once the library is released.
:::

## Usage Examples

### Creating an Entity

```kotlin
import com.melsardes.libraries.structuskotlin.domain.Entity

data class ProductId(val value: String)

class Product(
    override val id: ProductId,
    var name: String,
    var price: Double
) : Entity<ProductId>()
```

### Creating a Value Object

```kotlin
import com.melsardes.libraries.structuskotlin.domain.ValueObject

data class Price(
    val amount: Double,
    val currency: String
) : ValueObject {
    init {
        require(amount >= 0) { "Price cannot be negative" }
        require(currency.length == 3) { "Invalid currency code" }
    }
}
```

### Creating an Aggregate Root

```kotlin
import com.melsardes.libraries.structuskotlin.domain.AggregateRoot

class Cart(
    override val id: CartId,
    val userId: String
) : AggregateRoot<CartId>() {
    
    private val items = mutableListOf<CartItem>()
    
    fun addItem(productId: String, quantity: Int) {
        require(quantity > 0) { "Quantity must be positive" }
        
        items.add(CartItem(productId, quantity))
        
        recordEvent(ItemAddedToCartEvent(
            aggregateId = id.value,
            productId = productId,
            quantity = quantity
        ))
    }
}
```

### Implementing a Command Handler

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler

class AddItemToCartHandler(
    private val cartRepository: CartRepository
) : CommandHandler<AddItemToCartCommand, Result<Unit>> {
    
    override suspend operator fun invoke(
        command: AddItemToCartCommand
    ): Result<Unit> {
        return runCatching {
            val cart = cartRepository.findById(CartId(command.cartId))
                ?: throw IllegalArgumentException("Cart not found")
            
            cart.addItem(command.productId, command.quantity)
            cartRepository.save(cart)
        }
    }
}
```

### Implementing a Query Handler

```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler

class GetCartHandler(
    private val cartRepository: CartRepository
) : QueryHandler<GetCartQuery, CartDto?> {
    
    override suspend operator fun invoke(query: GetCartQuery): CartDto? {
        val cart = cartRepository.findById(CartId(query.cartId))
        return cart?.let { CartDto.from(it) }
    }
}
```

## Type Safety

All Structus APIs are designed with type safety in mind:

- **Generic Types**: Entities and Aggregates are generic over their ID type
- **Sealed Classes**: Used for discriminated unions
- **Data Classes**: Immutable value objects and events
- **Suspend Functions**: All I/O operations are suspending

## Coroutines Support

All repository and handler operations use Kotlin coroutines:

```kotlin
interface UserRepository : Repository {
    suspend fun findById(id: UserId): User?
    suspend fun save(user: User)
}

interface CommandHandler<in C : Command, out R> {
    suspend operator fun invoke(command: C): R
}
```

## Error Handling

Structus uses Kotlin's `Result` type for explicit error handling:

```kotlin
class RegisterUserHandler : CommandHandler<RegisterUserCommand, Result<UserId>> {
    override suspend operator fun invoke(
        command: RegisterUserCommand
    ): Result<UserId> {
        return runCatching {
            // Business logic here
            userId
        }
    }
}
```

## Next Steps

- **[Domain API](domain-api)** - Detailed domain layer API
- **[Application API](application-api)** - Detailed application layer API
- **[Events API](events-api)** - Detailed events API
- **[Best Practices](../best-practices/guidelines)** - API usage best practices
