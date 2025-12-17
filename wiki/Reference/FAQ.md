# Frequently Asked Questions (FAQ)

## General Questions

### What is Explicit Architecture?

Explicit Architecture is a synthesis of Domain-Driven Design (DDD), Clean Architecture, Hexagonal Architecture, CQRS, and Event-Driven Architecture. It provides clear guidelines for building maintainable, testable, and scalable applications.

### Why use this library?

- **Framework-Agnostic**: Works with Spring, Ktor, Micronaut, or pure Kotlin
- **Minimal Dependencies**: Only Kotlin stdlib and coroutines
- **Enforces Best Practices**: Clear separation of concerns and dependency rules
- **Production-Ready**: Battle-tested patterns like Transactional Outbox
- **Comprehensive Documentation**: Every component is well-documented

### Is this library production-ready?

Yes! Version 0.1.0 is stable and includes all core components needed for production applications. It's being used in real-world projects.

---

## Architecture Questions

### When should I use an Entity vs a Value Object?

**Use Entity when**:
- Identity matters (e.g., User, Order, Product)
- Object has a lifecycle (created, updated, deleted)
- Needs to be tracked over time

**Use Value Object when**:
- Attributes define the object (e.g., Email, Money, Address)
- Immutable and replaceable
- No lifecycle or identity

### How do I know what should be an Aggregate Root?

An Aggregate Root should:
- Be a consistency boundary
- Enforce business invariants
- Be the entry point for all operations on related entities
- Have a clear identity

**Example**: `Order` is an aggregate root that contains `OrderItems`. You don't access `OrderItem` directly; you go through `Order`.

### Should every entity be an Aggregate Root?

No! Only entities that serve as consistency boundaries should be Aggregate Roots. Child entities within an aggregate are just regular entities.

### How big should an Aggregate be?

Keep aggregates **small**. They should contain only what needs to be consistent within a single transaction. Use eventual consistency between aggregates.

**Rule of thumb**: If you're loading more than 3-4 related entities, consider splitting into multiple aggregates.

---

## Implementation Questions

### Do I need to use CommandBus?

No, it's optional. You can inject handlers directly:

```kotlin
// With CommandBus
val result = commandBus.dispatch(RegisterUserCommand(...))

// Without CommandBus (direct injection)
val result = registerUserHandler(RegisterUserCommand(...))
```

**Use CommandBus when**:
- You need middleware (logging, validation, transactions)
- You want centralized command routing
- You're building a plugin system

**Skip CommandBus when**:
- Simple applications
- Direct injection is clearer
- Framework handles routing (e.g., Spring)

### How do I handle transactions?

Transactions are managed in the **infrastructure layer**:

```kotlin
class TransactionalCommandHandler<C : Command, R>(
    private val delegate: CommandHandler<C, R>,
    private val transactionManager: TransactionManager
) : CommandHandler<C, R> {
    
    override suspend operator fun invoke(command: C): R {
        return transactionManager.withTransaction {
            delegate(command)
        }
    }
}
```

Or use framework-specific annotations (Spring's `@Transactional`).

### Should I use Result<T> or throw exceptions?

**Use `Result<T>` for**:
- Expected, recoverable errors (validation, business rules)
- Explicit error handling
- Functional programming style

**Use exceptions for**:
- Unexpected errors (database connection failure)
- Infrastructure failures
- Programming errors

**Example**:
```kotlin
// Result for business errors
fun registerUser(email: String): Result<UserId> {
    return runCatching {
        if (existsByEmail(email)) {
            throw EmailAlreadyExistsException(email)
        }
        // ...
    }
}

// Exception for infrastructure errors
suspend fun save(user: User) {
    // Database connection failure throws exception
    database.execute("INSERT INTO users ...")
}
```

### How do I test my code?

See [Testing Strategies](Testing-Strategies.md) for comprehensive examples. Quick overview:

**Domain Layer** (pure unit tests):
```kotlin
@Test
fun `should record event when user is activated`() {
    val user = User.create(Email("test@example.com"), "Test")
    
    user.activate()
    
    assertEquals(UserStatus.ACTIVE, user.status)
    assertTrue(user.hasEvents())
}
```

**Application Layer** (with mocks):
```kotlin
@Test
fun `should register user successfully`() = runTest {
    val userRepository = mockk<UserRepository>()
    coEvery { userRepository.existsByEmail(any()) } returns false
    coEvery { userRepository.save(any()) } just Runs
    
    val handler = RegisterUserCommandHandler(userRepository, outboxRepository)
    val result = handler(RegisterUserCommand("test@example.com", "Test"))
    
    assertTrue(result.isSuccess)
}
```

---

## Domain Events Questions

### When should I record a domain event?

Record an event when:
- Something significant happened in the domain
- Other parts of the system need to react
- You need an audit trail
- State changed in a meaningful way

**Examples**: `UserRegistered`, `OrderPlaced`, `PaymentProcessed`

### Should I clear events after publishing?

**Yes, always!** After publishing events, call `aggregate.clearEvents()`:

```kotlin
// Save events
user.domainEvents.forEach { outboxRepository.save(it) }

// Clear events (important!)
user.clearEvents()
```

Otherwise, events will be published multiple times.

### Can I publish events synchronously?

**Not recommended**. Use the Transactional Outbox Pattern for reliability:

```kotlin
// ❌ Don't do this
orderRepository.save(order)
eventPublisher.publish(order.domainEvents)  // Can fail!

// ✅ Do this
orderRepository.save(order)
order.domainEvents.forEach { outboxRepository.save(it) }  // Same transaction
order.clearEvents()
```

### How do I handle event versioning?

Use the `eventVersion` field in `BaseDomainEvent`:

```kotlin
// Version 1
data class UserRegisteredEvent(
    override val aggregateId: String,
    val email: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1  // Version 1
)

// Version 2 (added name field)
data class UserRegisteredEventV2(
    override val aggregateId: String,
    val email: String,
    val name: String  // New field
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 2  // Version 2
)
```

Consumers handle both versions:
```kotlin
when (event.eventVersion) {
    1 -> handleV1(event as UserRegisteredEvent)
    2 -> handleV2(event as UserRegisteredEventV2)
}
```

---

## CQRS Questions

### Do I need separate databases for reads and writes?

No, it's optional. Start with a single database:

**Level 1** (Single DB, same model):
```
Commands → Domain Model → Database ← Queries
```

**Level 2** (Single DB, separate models):
```
Commands → Domain Model → Database
                          ↓
                    Read Model ← Queries
```

**Level 3** (Separate DBs):
```
Commands → Domain Model → Write DB
                          ↓ Events
                    Read DB ← Queries
```

Start simple, evolve as needed.

### Can queries modify data?

**No!** Queries should be read-only. If you need to modify data, use a command.

```kotlin
// ❌ Don't do this
class GetUserQueryHandler : QueryHandler<GetUserQuery, UserDto> {
    override suspend operator fun invoke(query: GetUserQuery): UserDto {
        val user = userRepository.findById(query.userId)
        user.updateLastAccessed()  // ❌ Modifying state in a query!
        return user.toDto()
    }
}

// ✅ Do this
class GetUserQueryHandler : QueryHandler<GetUserQuery, UserDto> {
    override suspend operator fun invoke(query: GetUserQuery): UserDto {
        val user = userRepository.findById(query.userId)
        return user.toDto()  // ✅ Read-only
    }
}
```

---

## Integration Questions

### Can I use this with Spring Boot?

Yes! See [Spring Boot Integration](Spring-Boot-Integration.md). Quick example:

```kotlin
@Configuration
class ApplicationConfig {
    
    @Bean
    fun registerUserHandler(
        userRepository: UserRepository,
        outboxRepository: MessageOutboxRepository
    ) = RegisterUserCommandHandler(userRepository, outboxRepository)
}

@RestController
class UserController(
    private val registerUserHandler: RegisterUserCommandHandler
) {
    @PostMapping("/users")
    suspend fun register(@RequestBody request: RegisterRequest) =
        registerUserHandler(request.toCommand())
}
```

### Can I use this with Ktor?

Yes! See [Ktor Integration](Ktor-Integration.md). Quick example:

```kotlin
fun Application.module() {
    val userRepository = PostgresUserRepository(database)
    val registerHandler = RegisterUserCommandHandler(userRepository, outboxRepository)
    
    routing {
        post("/users") {
            val request = call.receive<RegisterRequest>()
            val result = registerHandler(request.toCommand())
            
            result.fold(
                onSuccess = { call.respond(HttpStatusCode.Created, it) },
                onFailure = { call.respond(HttpStatusCode.BadRequest, it.message) }
            )
        }
    }
}
```

### Does this work with reactive frameworks?

Yes! The library uses suspend functions, which work with:
- Spring WebFlux
- Ktor (coroutines-based)
- R2DBC (reactive database)
- Reactive message brokers

---

## Performance Questions

### Is this architecture slow?

No! Common misconceptions:

**Myth**: "More layers = slower"
**Reality**: Layer separation has negligible overhead. Database and network calls dominate performance.

**Myth**: "CQRS requires separate databases"
**Reality**: Start with a single database. Separate only when needed.

**Myth**: "Events add latency"
**Reality**: Transactional Outbox publishes asynchronously. No added latency to the request.

### How do I optimize queries?

- Use projections (select only needed fields)
- Add database indexes
- Cache frequently accessed data
- Use read replicas for queries
- Denormalize read models if needed

```kotlin
// ❌ Slow: Load full aggregate
val user = userRepository.findById(userId)
return UserSummaryDto(user.id, user.name)

// ✅ Fast: Direct SQL projection
val summary = database.query(
    "SELECT id, name FROM users WHERE id = ?",
    userId
)
```

---

## Migration Questions

### Can I migrate an existing application?

Yes! Migrate incrementally:

1. **Start with one aggregate** (e.g., User)
2. **Extract domain logic** from services into aggregates
3. **Add command handlers** around existing code
4. **Introduce events** for new features
5. **Refactor queries** to use query handlers
6. **Repeat** for other aggregates

### Do I need to rewrite everything?

No! Use the **Strangler Fig Pattern**:
- New features use Explicit Architecture
- Old code remains unchanged
- Gradually migrate old code
- Both coexist during transition

---

## Troubleshooting

### "Could not find structus-kotlin"

Ensure you have the correct repository configured. See [Installation Guide](Installation-Guide.md).

### Events are published multiple times

You forgot to call `aggregate.clearEvents()` after publishing.

### Aggregate has no events

You forgot to call `recordEvent()` in your domain methods.

### Transaction not working

Ensure your command handler is wrapped in a transaction. Check your infrastructure layer.

---

## Contributing

### How can I contribute?

See [Contributing Guide](Contributing-Guide.md). We welcome:
- Bug reports
- Feature requests
- Documentation improvements
- Code contributions

### Where do I report bugs?

Open an issue on [GitHub Issues](https://github.com/structus-io/structus-kotlin/issues).

---

## Additional Resources

- **[Quick Start Tutorial](Quick-Start-Tutorial.md)** - Build your first app
- **[Core Concepts](Core-Concepts.md)** - Understand the fundamentals
- **[Architecture Overview](Architecture-Overview.md)** - Big picture view
- **[Testing Strategies](Testing-Strategies.md)** - Test your code
- **[Glossary](Glossary.md)** - Terms and definitions
