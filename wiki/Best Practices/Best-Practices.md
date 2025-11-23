# Best Practices

Essential best practices for building applications with Structus.

## 🎯 General Principles

### 1. **Keep Aggregates Small**

```kotlin
// ✅ Good - Focused aggregate
class Order(
    override val id: OrderId,
    var status: OrderStatus,
    val customerId: CustomerId, // Reference, not embedded
    val items: MutableList<OrderItem>
) : AggregateRoot<OrderId>()

// ❌ Bad - Too much responsibility
class Order(
    override val id: OrderId,
    var status: OrderStatus,
    val customer: Customer, // Embedded aggregate
    val items: MutableList<OrderItem>,
    val payments: MutableList<Payment>,
    val shipments: MutableList<Shipment>
) : AggregateRoot<OrderId>()
```

### 2. **Use Value Objects for Validation**

```kotlin
// ✅ Good - Validation in value object
data class Email(val value: String) : ValueObject {
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
    }
}

// ❌ Bad - Validation scattered
data class User(
    val email: String // No validation
)
fun validateEmail(email: String): Boolean { /* ... */ }
```

### 3. **Make Illegal States Unrepresentable**

```kotlin
// ✅ Good - Type-safe states
sealed class OrderStatus {
    object Pending : OrderStatus()
    data class Confirmed(val confirmedAt: Instant) : OrderStatus()
    data class Shipped(val trackingNumber: String) : OrderStatus()
    object Cancelled : OrderStatus()
}

// ❌ Bad - Stringly-typed
data class Order(
    val status: String, // "pending", "confirmed", etc.
    val confirmedAt: Instant?, // Nullable, can be inconsistent
    val trackingNumber: String? // Nullable, can be inconsistent
)
```

## 🏗️ Domain Layer

### 1. **Rich Domain Models**

```kotlin
// ✅ Good - Behavior in domain
class Order : AggregateRoot<OrderId>() {
    fun confirm() {
        require(status == OrderStatus.Pending) { "Only pending orders can be confirmed" }
        status = OrderStatus.Confirmed(Instant.now())
        recordEvent(OrderConfirmedEvent(id.value))
    }
}

// ❌ Bad - Anemic domain model
class Order {
    var status: String = "pending"
}
class OrderService {
    fun confirmOrder(order: Order) {
        order.status = "confirmed"
    }
}
```

### 2. **Protect Invariants**

```kotlin
// ✅ Good - Invariants enforced
class Order(
    override val id: OrderId,
    var status: OrderStatus,
    private val _items: MutableList<OrderItem> = mutableListOf()
) : AggregateRoot<OrderId>() {
    
    val items: List<OrderItem> get() = _items.toList() // Read-only view
    
    fun addItem(item: OrderItem) {
        require(status == OrderStatus.Pending) { "Cannot modify confirmed order" }
        require(_items.size < 100) { "Maximum 100 items per order" }
        _items.add(item)
        recordEvent(OrderItemAddedEvent(id.value, item.productId))
    }
}

// ❌ Bad - Exposed mutable state
class Order(
    val items: MutableList<OrderItem> = mutableListOf() // Can be modified directly
)
```

### 3. **Use Factory Methods**

```kotlin
// ✅ Good - Factory method with validation
class User private constructor(
    override val id: UserId,
    var email: Email,
    var name: UserName
) : AggregateRoot<UserId>() {
    
    companion object {
        fun create(email: Email, name: UserName): User {
            val user = User(
                id = UserId.generate(),
                email = email,
                name = name
            )
            user.recordEvent(UserCreatedEvent(
                aggregateId = user.id.value,
                email = email.value,
                name = name.value
            ))
            return user
        }
    }
}

// ❌ Bad - Public constructor
class User(
    override val id: UserId,
    var email: Email
) : AggregateRoot<UserId>() // No event recorded
```

## 📝 Application Layer

### 1. **One Handler Per Command/Query**

```kotlin
// ✅ Good - Single responsibility
class CreateUserCommandHandler : CommandHandler<CreateUserCommand, Result<UserId>>
class UpdateUserEmailCommandHandler : CommandHandler<UpdateUserEmailCommand, Result<Unit>>

// ❌ Bad - Multiple responsibilities
class UserCommandHandler {
    fun handle(command: CreateUserCommand): Result<UserId>
    fun handle(command: UpdateUserEmailCommand): Result<Unit>
    fun handle(command: DeleteUserCommand): Result<Unit>
}
```

### 2. **Keep Handlers Thin**

```kotlin
// ✅ Good - Orchestration only
class CreateUserCommandHandler(
    private val repository: UserRepository,
    private val outbox: MessageOutboxRepository
) : CommandHandler<CreateUserCommand, Result<UserId>> {
    
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            val user = User.create(Email(command.email), UserName(command.name))
            repository.save(user)
            user.domainEvents.forEach { outbox.save(OutboxMessage.from(it)) }
            user.clearEvents()
            user.id
        }
    }
}

// ❌ Bad - Business logic in handler
class CreateUserCommandHandler {
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        // Validation logic
        if (command.email.isBlank()) return Result.failure(...)
        if (!command.email.contains("@")) return Result.failure(...)
        
        // Business logic
        val passwordHash = hashPassword(command.password)
        val verificationToken = generateToken()
        
        // ... too much logic here
    }
}
```

### 3. **Use Transactional Outbox**

```kotlin
// ✅ Good - Atomic save + event publishing
suspend fun handle(command: CreateUserCommand): Result<UserId> {
    return runCatching {
        val user = User.create(Email(command.email), UserName(command.name))
        
        // Both in same transaction
        repository.save(user)
        user.domainEvents.forEach { event ->
            outboxRepository.save(OutboxMessage.from(event))
        }
        user.clearEvents()
        
        user.id
    }
}

// ❌ Bad - Direct event publishing (dual-write problem)
suspend fun handle(command: CreateUserCommand): Result<UserId> {
    val user = User.create(Email(command.email), UserName(command.name))
    repository.save(user) // Transaction 1
    eventPublisher.publish(user.domainEvents) // Transaction 2 - can fail!
    user.id
}
```

## 🔍 Query Optimization

### 1. **Use Projections**

```kotlin
// ✅ Good - Specific projection
data class UserSummaryDto(
    val id: String,
    val email: String,
    val name: String,
    val status: String
)

interface UserQueryRepository {
    suspend fun findSummaries(): List<UserSummaryDto>
}

// ❌ Bad - Returning full entities
interface UserQueryRepository {
    suspend fun findAll(): List<User> // Loads everything
}
```

### 2. **Separate Read Models**

```kotlin
// ✅ Good - Optimized read model
data class UserSearchProjection(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val lastLoginAt: Instant?,
    val searchVector: String // For full-text search
)

// ❌ Bad - Using write model for queries
class User : AggregateRoot<UserId>() {
    // Complex domain logic
    // Not optimized for queries
}
```

## 🧪 Testing

### 1. **Test Behavior, Not Implementation**

```kotlin
// ✅ Good - Tests behavior
@Test
fun `should record UserCreatedEvent when user is created`() {
    val user = User.create(Email("test@example.com"), UserName("Test"))
    
    val events = user.domainEvents
    assertEquals(1, events.size)
    assertTrue(events.first() is UserCreatedEvent)
}

// ❌ Bad - Tests implementation details
@Test
fun `should call recordEvent method`() {
    val user = spy(User.create(...))
    verify(user).recordEvent(any())
}
```

### 2. **Use Test Builders**

```kotlin
// ✅ Good - Test builder
class UserBuilder {
    private var email = Email("test@example.com")
    private var name = UserName("Test User")
    
    fun withEmail(email: Email) = apply { this.email = email }
    fun withName(name: UserName) = apply { this.name = name }
    
    fun build() = User.create(email, name)
}

@Test
fun `test with builder`() {
    val user = UserBuilder()
        .withEmail(Email("custom@example.com"))
        .build()
}

// ❌ Bad - Repetitive setup
@Test
fun `test 1`() {
    val user = User.create(Email("test@example.com"), UserName("Test"))
}

@Test
fun `test 2`() {
    val user = User.create(Email("test@example.com"), UserName("Test"))
}
```

## 🚀 Performance

### 1. **Lazy Load Collections**

```kotlin
// ✅ Good - Lazy loading
class Order(
    override val id: OrderId,
    var status: OrderStatus
) : AggregateRoot<OrderId>() {
    
    private var _items: List<OrderItem>? = null
    
    suspend fun getItems(repository: OrderItemRepository): List<OrderItem> {
        if (_items == null) {
            _items = repository.findByOrderId(id)
        }
        return _items!!
    }
}

// ❌ Bad - Eager loading everything
class Order(
    override val id: OrderId,
    val items: List<OrderItem>, // Always loaded
    val payments: List<Payment>, // Always loaded
    val shipments: List<Shipment> // Always loaded
)
```

### 2. **Batch Operations**

```kotlin
// ✅ Good - Batch save
suspend fun saveAll(users: List<User>) {
    repository.saveAll(users)
    
    val events = users.flatMap { it.domainEvents }
    outboxRepository.saveAll(events.map { OutboxMessage.from(it) })
    
    users.forEach { it.clearEvents() }
}

// ❌ Bad - One by one
suspend fun saveAll(users: List<User>) {
    users.forEach { user ->
        repository.save(user)
        user.domainEvents.forEach { event ->
            outboxRepository.save(OutboxMessage.from(event))
        }
        user.clearEvents()
    }
}
```

## 📦 Naming Conventions

### 1. **Commands**
- Use imperative verbs: `CreateUser`, `UpdateUserEmail`, `DeleteUser`
- Suffix with `Command`: `CreateUserCommand`

### 2. **Queries**
- Use descriptive names: `GetUserById`, `SearchUsers`, `FindActiveUsers`
- Suffix with `Query`: `GetUserByIdQuery`

### 3. **Events**
- Use past tense: `UserCreated`, `EmailChanged`, `OrderShipped`
- Suffix with `Event`: `UserCreatedEvent`

### 4. **Handlers**
- Suffix with `Handler`: `CreateUserCommandHandler`, `GetUserByIdQueryHandler`

### 5. **Repositories**
- Suffix with `Repository`: `UserRepository`, `OrderRepository`
- Separate command/query: `UserCommandRepository`, `UserQueryRepository`

## 🔗 Related Topics

- **[Testing Strategies](Testing-Strategies.md)** - Comprehensive testing guide
- **[Error Handling](Error-Handling.md)** - Error handling patterns
- **[CQRS Implementation](CQRS-Implementation.md)** - CQRS best practices
- **[Architecture Overview](Architecture-Overview.md)** - Architectural principles

---

**Next Steps**: Review [Common Mistakes](Common-Mistakes.md) to avoid pitfalls.
