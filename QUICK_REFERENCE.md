# Structus - Quick Reference

## 📦 Package Structure

```
com.melsardes.libraries.structuskotlin
├── domain/                           # Pure business logic
│   ├── Entity<ID>                    # Identity-based objects
│   ├── ValueObject                   # Attribute-based immutable objects
│   ├── AggregateRoot<ID>             # Consistency boundary with events
│   ├── Repository                    # Persistence contract
│   ├── MessageOutboxRepository       # Transactional Outbox Pattern
│   └── events/
│       └── DomainEvent               # Something that happened
│
└── application/                      # Use cases and orchestration
    ├── commands/                     # Write operations (CQS)
    │   ├── Command                   # Intent to change state
    │   ├── CommandHandler<C, R>      # Executes business logic
    │   └── CommandBus                # Dispatches commands
    │
    ├── queries/                      # Read operations (CQS)
    │   ├── Query                     # Request for data
    │   └── QueryHandler<Q, R>        # Retrieves data
    │
    └── events/                       # Event publishing
        └── DomainEventPublisher      # Publishes events externally
```

## 🎯 Core Concepts

### Entity vs Value Object

| Aspect | Entity | Value Object |
|--------|--------|--------------|
| **Identity** | Has unique ID | No identity |
| **Equality** | By ID | By attributes |
| **Mutability** | Mutable | Immutable |
| **Example** | User, Order | Email, Money, Address |

### Command vs Query (CQS)

| Aspect | Command | Query |
|--------|---------|-------|
| **Purpose** | Change state | Retrieve data |
| **Naming** | Imperative (RegisterUser) | Question (GetUserById) |
| **Return** | ID or success/failure | Data (DTO) |
| **Side Effects** | Yes | No |

## 🔧 Common Patterns

### 1. Creating an Aggregate

```kotlin
class User(
    override val id: UserId,
    var email: Email,
    var status: UserStatus
) : AggregateRoot<UserId>() {
    
    fun activate() {
        require(status == UserStatus.PENDING) { "User must be pending" }
        status = UserStatus.ACTIVE
        
        recordEvent(UserActivatedEvent(
            aggregateId = id.value,
            activatedAt = Instant.now()
        ))
    }
}
```

### 2. Command Handler Pattern

```kotlin
class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, UserId> {
    
    override suspend fun handle(command: RegisterUserCommand): UserId {
        // 1. Validate
        require(!userRepository.existsByEmail(command.email)) {
            "Email already exists"
        }
        
        // 2. Create aggregate
        val user = User.create(command.email, command.name)
        
        // 3. Save aggregate
        userRepository.save(user)
        
        // 4. Save events to outbox (same transaction)
        user.domainEvents.forEach { outboxRepository.save(it) }
        
        // 5. Clear events
        user.clearEvents()
        
        return user.id
    }
}
```

### 3. Query Handler Pattern

```kotlin
class GetUserByIdQueryHandler(
    private val userRepository: UserRepository
) : QueryHandler<GetUserByIdQuery, UserDto?> {
    
    override suspend fun handle(query: GetUserByIdQuery): UserDto? {
        return userRepository.findById(query.userId)?.toDto()
    }
}
```

### 4. Transactional Outbox Pattern

```kotlin
// In command handler (write side)
suspend fun handle(command: CreateOrderCommand): OrderId {
    return withTransaction {
        val order = Order.create(command.items)
        orderRepository.save(order)
        
        // Save events to outbox (same transaction)
        order.domainEvents.forEach { outboxRepository.save(it) }
        order.clearEvents()
        
        order.id
    }
}

// Separate outbox publisher (background process)
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

## 📋 Checklist for New Features

### Domain Layer
- [ ] Define Value Objects (immutable, validated)
- [ ] Define Entity IDs (typically Value Objects)
- [ ] Create Aggregate Root (extends `AggregateRoot<ID>`)
- [ ] Define Domain Events (past tense, immutable)
- [ ] Create Repository Interface (extends `Repository`)
- [ ] Implement domain methods that `recordEvent()`

### Application Layer - Commands
- [ ] Create Command (data class, validation in init)
- [ ] Create CommandHandler (implements `CommandHandler<C, R>`)
- [ ] Register handler with CommandBus
- [ ] Handle events via Transactional Outbox

### Application Layer - Queries
- [ ] Create Query (data class with filters/pagination)
- [ ] Create DTOs for response
- [ ] Create QueryHandler (implements `QueryHandler<Q, R>`)
- [ ] Optimize for read performance (direct SQL, caching)

### Infrastructure Layer
- [ ] Implement Repository (persistence logic)
- [ ] Implement MessageOutboxRepository
- [ ] Implement DomainEventPublisher (Kafka, RabbitMQ, etc.)
- [ ] Create persistence models/tables
- [ ] Add mappers (domain ↔ persistence)

### Presentation Layer
- [ ] Create request/response DTOs
- [ ] Create controller/endpoint
- [ ] Map requests to Commands/Queries
- [ ] Dispatch via CommandBus or call handlers directly
- [ ] Handle errors and return appropriate responses

## 🚨 Common Mistakes to Avoid

### ❌ DON'T
- Don't put business logic in handlers (put it in aggregates)
- Don't call repositories from domain entities
- Don't publish events directly (use Transactional Outbox)
- Don't modify state in query handlers
- Don't skip event clearing after publishing
- Don't use entities for DTOs (create separate DTOs)

### ✅ DO
- Put business logic in aggregate methods
- Use repositories only in handlers
- Use Transactional Outbox Pattern for events
- Keep query handlers read-only
- Clear events after publishing
- Create separate DTOs for API responses

## 🎨 Naming Conventions

### Commands
- `RegisterUserCommand`
- `PlaceOrderCommand`
- `UpdateProfileCommand`
- `CancelSubscriptionCommand`

### Queries
- `GetUserByIdQuery`
- `FindActiveOrdersQuery`
- `SearchProductsQuery`
- `ListCustomersQuery`

### Events
- `UserRegisteredEvent`
- `OrderPlacedEvent`
- `ProfileUpdatedEvent`
- `SubscriptionCancelledEvent`

### Handlers
- `RegisterUserCommandHandler`
- `GetUserByIdQueryHandler`

### Repositories
- Interface: `UserRepository`
- Implementation: `UserRepositoryImpl`

## 🔍 Testing Patterns

### Unit Test - Aggregate
```kotlin
@Test
fun `should activate pending user`() {
    val user = User.create(Email("test@example.com"), "Test User")
    
    user.activate()
    
    assertEquals(UserStatus.ACTIVE, user.status)
    assertEquals(1, user.eventCount())
    assertTrue(user.domainEvents[0] is UserActivatedEvent)
}
```

### Unit Test - Command Handler
```kotlin
@Test
fun `should register user successfully`() = runTest {
    val command = RegisterUserCommand("test@example.com", "Test")
    val userRepository = mockk<UserRepository>()
    val outboxRepository = mockk<MessageOutboxRepository>()
    
    coEvery { userRepository.existsByEmail(any()) } returns false
    coEvery { userRepository.save(any()) } just Runs
    coEvery { outboxRepository.save(any()) } just Runs
    
    val handler = RegisterUserCommandHandler(userRepository, outboxRepository)
    val userId = handler.handle(command)
    
    assertNotNull(userId)
    coVerify { userRepository.save(any()) }
    coVerify { outboxRepository.save(any()) }
}
```

### Unit Test - Query Handler
```kotlin
@Test
fun `should retrieve user by ID`() = runTest {
    val query = GetUserByIdQuery("user-123")
    val expectedUser = UserDto("user-123", "test@example.com", "Test")
    val userRepository = mockk<UserRepository>()
    
    coEvery { userRepository.findById(any()) } returns expectedUser
    
    val handler = GetUserByIdQueryHandler(userRepository)
    val result = handler.handle(query)
    
    assertEquals(expectedUser, result)
}
```

## 📚 Further Reading

- **GUIDE.md**: Detailed project structure and conventions
- **ASSESSMENT.md**: Implementation checklist and improvements
- **README.md**: Full library documentation with examples

## 🔗 Quick Links

- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
