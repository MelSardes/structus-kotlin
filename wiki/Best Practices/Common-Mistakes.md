# Common Mistakes

Learn from common mistakes when implementing Explicit Architecture with Structus.

## 🚫 Domain Layer Mistakes

### 1. **Anemic Domain Models**

```kotlin
// ❌ Bad - No behavior, just data
class Order(
    val id: OrderId,
    var status: String,
    val items: MutableList<OrderItem>
)

// Behavior in service layer
class OrderService {
    fun confirmOrder(order: Order) {
        order.status = "CONFIRMED"
    }
}

// ✅ Good - Rich domain model
class Order(
    override val id: OrderId,
    private var status: OrderStatus
) : AggregateRoot<OrderId>() {
    
    fun confirm() {
        require(status == OrderStatus.PENDING) { 
            "Only pending orders can be confirmed" 
        }
        status = OrderStatus.CONFIRMED
        recordEvent(OrderConfirmedEvent(id.value))
    }
}
```

**Why it's wrong**: Business logic scattered across services, hard to maintain and test.

### 2. **Exposing Mutable Collections**

```kotlin
// ❌ Bad - Mutable collection exposed
class Order(
    override val id: OrderId,
    val items: MutableList<OrderItem> = mutableListOf()
) : AggregateRoot<OrderId>()

// Can be modified directly, bypassing business rules
order.items.add(item) // No validation, no events!

// ✅ Good - Encapsulated collection
class Order(
    override val id: OrderId,
    private val _items: MutableList<OrderItem> = mutableListOf()
) : AggregateRoot<OrderId>() {
    
    val items: List<OrderItem> get() = _items.toList()
    
    fun addItem(item: OrderItem) {
        require(_items.size < 100) { "Max 100 items" }
        _items.add(item)
        recordEvent(OrderItemAddedEvent(id.value, item.productId))
    }
}
```

**Why it's wrong**: Invariants can be violated, events not recorded.

### 3. **Forgetting to Record Events**

```kotlin
// ❌ Bad - No event recorded
class User : AggregateRoot<UserId>() {
    fun activate() {
        status = UserStatus.ACTIVE
        // Forgot to record event!
    }
}

// ✅ Good - Event recorded
class User : AggregateRoot<UserId>() {
    fun activate() {
        status = UserStatus.ACTIVE
        recordEvent(UserActivatedEvent(id.value))
    }
}
```

**Why it's wrong**: Other parts of the system won't know about state changes.

### 4. **Not Clearing Events After Publishing**

```kotlin
// ❌ Bad - Events not cleared
suspend fun handle(command: CreateUserCommand): Result<UserId> {
    val user = User.create(Email(command.email), UserName(command.name))
    repository.save(user)
    
    user.domainEvents.forEach { event ->
        outboxRepository.save(OutboxMessage.from(event))
    }
    // Forgot to clear events!
    
    return Result.success(user.id)
}

// Next time you save, events will be published again!

// ✅ Good - Events cleared
suspend fun handle(command: CreateUserCommand): Result<UserId> {
    val user = User.create(Email(command.email), UserName(command.name))
    repository.save(user)
    
    user.domainEvents.forEach { event ->
        outboxRepository.save(OutboxMessage.from(event))
    }
    user.clearEvents() // Clear after publishing
    
    return Result.success(user.id)
}
```

**Why it's wrong**: Events will be published multiple times.

### 5. **Large Aggregates**

```kotlin
// ❌ Bad - Too much in one aggregate
class Order(
    override val id: OrderId,
    var status: OrderStatus,
    val customer: Customer, // Embedded aggregate
    val items: MutableList<OrderItem>,
    val payments: MutableList<Payment>,
    val shipments: MutableList<Shipment>,
    val invoices: MutableList<Invoice>
) : AggregateRoot<OrderId>()

// ✅ Good - Focused aggregate with references
class Order(
    override val id: OrderId,
    var status: OrderStatus,
    val customerId: CustomerId, // Reference only
    private val _items: MutableList<OrderItem>
) : AggregateRoot<OrderId>()

// Separate aggregates
class Payment(override val id: PaymentId, val orderId: OrderId) : AggregateRoot<PaymentId>()
class Shipment(override val id: ShipmentId, val orderId: OrderId) : AggregateRoot<ShipmentId>()
```

**Why it's wrong**: Performance issues, complex transactions, hard to maintain.

## 🚫 Application Layer Mistakes

### 6. **Business Logic in Handlers**

```kotlin
// ❌ Bad - Business logic in handler
class CreateUserCommandHandler : CommandHandler<CreateUserCommand, Result<UserId>> {
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            // Validation logic in handler
            if (command.email.isBlank()) {
                throw ValidationException("Email is required")
            }
            if (!command.email.contains("@")) {
                throw ValidationException("Invalid email")
            }
            
            // Business logic in handler
            val passwordHash = hashPassword(command.password)
            val verificationToken = UUID.randomUUID().toString()
            
            val user = User(
                id = UserId.generate(),
                email = Email(command.email),
                passwordHash = passwordHash,
                verificationToken = verificationToken
            )
            
            repository.save(user)
            user.id
        }
    }
}

// ✅ Good - Business logic in domain
class CreateUserCommandHandler : CommandHandler<CreateUserCommand, Result<UserId>> {
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            // Validation in value object
            val email = Email(command.email) // Throws if invalid
            val name = UserName(command.name)
            
            // Business logic in domain
            val user = User.create(email, name)
            
            repository.save(user)
            user.domainEvents.forEach { outbox.save(OutboxMessage.from(it)) }
            user.clearEvents()
            
            user.id
        }
    }
}
```

**Why it's wrong**: Business logic should be in the domain layer, not application layer.

### 7. **Dual-Write Problem**

```kotlin
// ❌ Bad - Dual write (can fail between operations)
suspend fun handle(command: CreateUserCommand): Result<UserId> {
    val user = User.create(Email(command.email), UserName(command.name))
    
    repository.save(user) // Transaction 1
    eventPublisher.publish(user.domainEvents) // Transaction 2 - can fail!
    
    // If publish fails, user is saved but events not published
    return Result.success(user.id)
}

// ✅ Good - Transactional outbox
suspend fun handle(command: CreateUserCommand): Result<UserId> {
    val user = User.create(Email(command.email), UserName(command.name))
    
    // Both in same transaction
    repository.save(user)
    user.domainEvents.forEach { event ->
        outboxRepository.save(OutboxMessage.from(event))
    }
    user.clearEvents()
    
    return Result.success(user.id)
}
```

**Why it's wrong**: Inconsistent state if event publishing fails.

### 8. **Not Using Result Type**

```kotlin
// ❌ Bad - Throwing exceptions for expected failures
class GetUserByIdQueryHandler : QueryHandler<GetUserByIdQuery, UserDto> {
    override suspend fun invoke(query: GetUserByIdQuery): UserDto {
        return repository.findById(UserId(query.userId))
            ?: throw EntityNotFoundException("User not found")
    }
}

// Caller has to catch exceptions
try {
    val user = handler(query)
} catch (e: EntityNotFoundException) {
    // Handle
}

// ✅ Good - Using Result type
class GetUserByIdQueryHandler : QueryHandler<GetUserByIdQuery, Result<UserDto>> {
    override suspend fun invoke(query: GetUserByIdQuery): Result<UserDto> {
        return runCatching {
            repository.findById(UserId(query.userId))
                ?: throw EntityNotFoundException("User not found")
        }
    }
}

// Caller handles Result
handler(query).fold(
    onSuccess = { user -> /* ... */ },
    onFailure = { error -> /* ... */ }
)
```

**Why it's wrong**: Exceptions for expected failures make error handling implicit.

## 🚫 Architecture Mistakes

### 9. **Wrong Dependency Direction**

```kotlin
// ❌ Bad - Domain depends on infrastructure
// In domain layer
class User : AggregateRoot<UserId>() {
    fun save(repository: UserJpaRepository) { // Infrastructure dependency!
        repository.save(this)
    }
}

// ✅ Good - Infrastructure depends on domain
// In domain layer
interface UserRepository {
    suspend fun save(user: User)
}

// In infrastructure layer
class UserJpaRepositoryImpl : UserRepository {
    override suspend fun save(user: User) {
        // JPA implementation
    }
}
```

**Why it's wrong**: Violates dependency inversion principle.

### 10. **Framework Dependencies in Domain**

```kotlin
// ❌ Bad - Spring annotations in domain
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue
    override val id: UserId,
    
    @Column(nullable = false)
    var email: Email
) : AggregateRoot<UserId>()

// ✅ Good - Pure domain model
class User(
    override val id: UserId,
    var email: Email
) : AggregateRoot<UserId>()

// Mapping in infrastructure layer
@Entity
@Table(name = "users")
class UserEntity(
    @Id val id: String,
    @Column val email: String
) {
    fun toDomain() = User(UserId(id), Email(email))
}
```

**Why it's wrong**: Domain should be framework-agnostic.

### 11. **Mixing Commands and Queries**

```kotlin
// ❌ Bad - Command returns data
data class CreateUserCommand(
    val email: String,
    val name: String
) : Command

class CreateUserCommandHandler : CommandHandler<CreateUserCommand, Result<UserDto>> {
    override suspend fun invoke(command: CreateUserCommand): Result<UserDto> {
        val user = User.create(Email(command.email), UserName(command.name))
        repository.save(user)
        
        // Returning full DTO from command
        return Result.success(UserDto(
            id = user.id.value,
            email = user.email.value,
            name = user.name.value,
            status = user.status.name,
            createdAt = user.createdAt
        ))
    }
}

// ✅ Good - Command returns minimal data
class CreateUserCommandHandler : CommandHandler<CreateUserCommand, Result<UserId>> {
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        val user = User.create(Email(command.email), UserName(command.name))
        repository.save(user)
        
        // Return only ID
        return Result.success(user.id)
    }
}

// Use query to get full data
class GetUserByIdQueryHandler : QueryHandler<GetUserByIdQuery, Result<UserDto>>
```

**Why it's wrong**: Violates CQRS principle, couples write and read models.

## 🚫 Testing Mistakes

### 12. **Testing Implementation Details**

```kotlin
// ❌ Bad - Testing private methods
@Test
fun `should call private validation method`() {
    val user = User.create(Email("test@example.com"), UserName("Test"))
    
    // Using reflection to test private method
    val method = User::class.java.getDeclaredMethod("validateEmail")
    method.isAccessible = true
    method.invoke(user)
}

// ✅ Good - Testing behavior
@Test
fun `should throw exception for invalid email`() {
    assertThrows<IllegalArgumentException> {
        Email("invalid-email")
    }
}
```

**Why it's wrong**: Tests become brittle, coupled to implementation.

### 13. **Not Testing Edge Cases**

```kotlin
// ❌ Bad - Only happy path
@Test
fun `should create user`() {
    val user = User.create(Email("test@example.com"), UserName("Test"))
    assertNotNull(user)
}

// ✅ Good - Testing edge cases
@Test
fun `should throw exception for blank email`() {
    assertThrows<IllegalArgumentException> {
        Email("")
    }
}

@Test
fun `should throw exception for email without @`() {
    assertThrows<IllegalArgumentException> {
        Email("invalid-email")
    }
}

@Test
fun `should throw exception for email too long`() {
    val longEmail = "a".repeat(256) + "@example.com"
    assertThrows<IllegalArgumentException> {
        Email(longEmail)
    }
}
```

**Why it's wrong**: Edge cases are where bugs hide.

## 🚫 Performance Mistakes

### 14. **N+1 Query Problem**

```kotlin
// ❌ Bad - N+1 queries
suspend fun getOrdersWithItems(): List<OrderDto> {
    val orders = orderRepository.findAll() // 1 query
    
    return orders.map { order ->
        val items = itemRepository.findByOrderId(order.id) // N queries
        OrderDto(order, items)
    }
}

// ✅ Good - Batch loading
suspend fun getOrdersWithItems(): List<OrderDto> {
    val orders = orderRepository.findAll()
    val orderIds = orders.map { it.id }
    
    // Single query for all items
    val itemsByOrderId = itemRepository.findByOrderIds(orderIds)
        .groupBy { it.orderId }
    
    return orders.map { order ->
        OrderDto(order, itemsByOrderId[order.id] ?: emptyList())
    }
}
```

**Why it's wrong**: Causes performance issues with large datasets.

### 15. **Loading Entire Aggregates for Queries**

```kotlin
// ❌ Bad - Loading full aggregate for list view
suspend fun getUserList(): List<UserSummaryDto> {
    val users = userRepository.findAll() // Loads everything
    
    return users.map { user ->
        UserSummaryDto(
            id = user.id.value,
            email = user.email.value,
            name = user.name.value
        )
    }
}

// ✅ Good - Using projections
suspend fun getUserList(): List<UserSummaryDto> {
    return userQueryRepository.findAllSummaries() // Loads only needed fields
}
```

**Why it's wrong**: Wastes memory and bandwidth.

## 🔧 How to Avoid These Mistakes

### 1. **Code Reviews**
- Review for domain logic in handlers
- Check for proper event recording
- Verify dependency directions

### 2. **Architecture Tests**
```kotlin
@Test
fun `domain layer should not depend on infrastructure`() {
    ArchRuleDefinition.noClasses()
        .that().resideInPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInPackage("..infrastructure..")
        .check(importedClasses)
}
```

### 3. **Linting Rules**
- Enforce naming conventions
- Check for mutable collections
- Verify Result usage

### 4. **Pair Programming**
- Share knowledge
- Catch mistakes early

### 5. **Documentation**
- Document architectural decisions
- Maintain examples
- Update wiki

## 🔗 Related Topics

- **[Best Practices](Best-Practices.md)** - Recommended approaches
- **[Testing Strategies](Testing-Strategies.md)** - Testing guide
- **[Architecture Overview](Architecture-Overview.md)** - Architectural principles
- **[Error Handling](Error-Handling.md)** - Error handling patterns

---

**Next Steps**: Review [Best Practices](Best-Practices.md) for recommended approaches.
