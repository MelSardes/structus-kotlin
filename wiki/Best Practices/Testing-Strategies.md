# Testing Strategies

Comprehensive guide to testing applications built with Explicit Architecture.

## 🎯 Testing Pyramid

```
        /\
       /  \      E2E Tests (Few)
      /────\
     /      \    Integration Tests (Some)
    /────────\
   /          \  Unit Tests (Many)
  /────────────\
```

---

## Unit Testing

### Testing Domain Layer

Domain layer tests are **pure unit tests** with no dependencies.

#### Testing Entities

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UserTest {
    
    @Test
    fun `entities with same ID should be equal`() {
        val user1 = User(UserId("123"), Email("john@example.com"), "John")
        val user2 = User(UserId("123"), Email("jane@example.com"), "Jane")
        
        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
    }
    
    @Test
    fun `entities with different IDs should not be equal`() {
        val user1 = User(UserId("123"), Email("john@example.com"), "John")
        val user2 = User(UserId("456"), Email("john@example.com"), "John")
        
        assertNotEquals(user1, user2)
    }
}
```

#### Testing Value Objects

```kotlin
class EmailTest {
    
    @Test
    fun `should create valid email`() {
        val email = Email("john@example.com")
        assertEquals("john@example.com", email.value)
    }
    
    @Test
    fun `should reject invalid email`() {
        assertFailsWith<IllegalArgumentException> {
            Email("invalid-email")
        }
    }
    
    @Test
    fun `value objects with same attributes should be equal`() {
        val email1 = Email("john@example.com")
        val email2 = Email("john@example.com")
        
        assertEquals(email1, email2)
    }
}

class MoneyTest {
    
    @Test
    fun `should add money with same currency`() {
        val money1 = Money(100.0, "USD")
        val money2 = Money(50.0, "USD")
        
        val result = money1 + money2
        
        assertEquals(Money(150.0, "USD"), result)
    }
    
    @Test
    fun `should reject adding different currencies`() {
        val usd = Money(100.0, "USD")
        val eur = Money(50.0, "EUR")
        
        assertFailsWith<IllegalArgumentException> {
            usd + eur
        }
    }
}
```

#### Testing Aggregate Roots

```kotlin
class OrderTest {
    
    @Test
    fun `should place order and record event`() {
        val order = Order(OrderId("123"), OrderStatus.DRAFT)
        val items = listOf(OrderItem("ITEM-1", Money(100.0, "USD")))
        
        order.place(items)
        
        assertEquals(OrderStatus.PLACED, order.status)
        assertEquals(1, order.eventCount())
        assertTrue(order.domainEvents.first() is OrderPlacedEvent)
    }
    
    @Test
    fun `should not place order if not draft`() {
        val order = Order(OrderId("123"), OrderStatus.PLACED)
        val items = listOf(OrderItem("ITEM-1", Money(100.0, "USD")))
        
        assertFailsWith<IllegalArgumentException> {
            order.place(items)
        }
    }
    
    @Test
    fun `should clear events after processing`() {
        val order = Order(OrderId("123"), OrderStatus.DRAFT)
        order.place(listOf(OrderItem("ITEM-1", Money(100.0, "USD"))))
        
        assertEquals(1, order.eventCount())
        
        order.clearEvents()
        
        assertEquals(0, order.eventCount())
        assertFalse(order.hasEvents())
    }
    
    @Test
    fun `should soft delete and restore`() {
        val order = Order(OrderId("123"), OrderStatus.PLACED)
        
        order.softDelete(by = "admin")
        assertTrue(order.isDeleted())
        assertFalse(order.isActive())
        
        order.restore(by = "admin")
        assertFalse(order.isDeleted())
        assertTrue(order.isActive())
    }
}
```

---

### Testing Application Layer

Application layer tests use **mocks** for dependencies.

#### Testing Command Handlers

```kotlin
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class RegisterUserCommandHandlerTest {
    
    private val userRepository = mockk<UserRepository>()
    private val outboxRepository = mockk<MessageOutboxRepository>()
    private val handler = RegisterUserCommandHandler(userRepository, outboxRepository)
    
    @Test
    fun `should register user successfully`() = runTest {
        // Given
        val command = RegisterUserCommand("john@example.com", "John Doe")
        coEvery { userRepository.existsByEmail(any()) } returns false
        coEvery { userRepository.save(any()) } just Runs
        coEvery { outboxRepository.save(any()) } just Runs
        
        // When
        val result = handler(command)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { userRepository.save(any()) }
        coVerify(exactly = 1) { outboxRepository.save(any()) }
    }
    
    @Test
    fun `should fail if email already exists`() = runTest {
        // Given
        val command = RegisterUserCommand("john@example.com", "John Doe")
        coEvery { userRepository.existsByEmail(any()) } returns true
        
        // When
        val result = handler(command)
        
        // Then
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }
    
    @Test
    fun `should save events to outbox`() = runTest {
        // Given
        val command = RegisterUserCommand("john@example.com", "John Doe")
        val capturedEvents = mutableListOf<DomainEvent>()
        
        coEvery { userRepository.existsByEmail(any()) } returns false
        coEvery { userRepository.save(any()) } just Runs
        coEvery { outboxRepository.save(capture(capturedEvents)) } just Runs
        
        // When
        handler(command)
        
        // Then
        assertEquals(1, capturedEvents.size)
        assertTrue(capturedEvents.first() is UserRegisteredEvent)
    }
}
```

#### Testing Query Handlers

```kotlin
class GetUserByIdQueryHandlerTest {
    
    private val userRepository = mockk<UserRepository>()
    private val handler = GetUserByIdQueryHandler(userRepository)
    
    @Test
    fun `should return user DTO when user exists`() = runTest {
        // Given
        val userId = UserId("123")
        val user = User(userId, Email("john@example.com"), "John", UserStatus.ACTIVE)
        val query = GetUserByIdQuery("123")
        
        coEvery { userRepository.findById(userId) } returns user
        
        // When
        val result = handler(query)
        
        // Then
        assertNotNull(result)
        assertEquals("123", result.id)
        assertEquals("john@example.com", result.email)
        assertEquals("John", result.name)
    }
    
    @Test
    fun `should return null when user does not exist`() = runTest {
        // Given
        val query = GetUserByIdQuery("999")
        coEvery { userRepository.findById(any()) } returns null
        
        // When
        val result = handler(query)
        
        // Then
        assertNull(result)
    }
}
```

#### Testing Event Handlers

```kotlin
class SendWelcomeEmailHandlerTest {
    
    private val emailService = mockk<EmailService>()
    private val handler = SendWelcomeEmailHandler(emailService)
    
    @Test
    fun `should send welcome email`() = runTest {
        // Given
        val event = UserRegisteredEvent(
            aggregateId = "USER-123",
            email = "john@example.com",
            name = "John Doe"
        )
        coEvery { emailService.send(any(), any(), any()) } just Runs
        
        // When
        handler.handle(event)
        
        // Then
        coVerify {
            emailService.send(
                to = "john@example.com",
                subject = "Welcome!",
                body = any()
            )
        }
    }
    
    @Test
    fun `should be idempotent`() = runTest {
        // Given
        val event = UserRegisteredEvent(
            aggregateId = "USER-123",
            email = "john@example.com",
            name = "John Doe"
        )
        coEvery { emailService.send(any(), any(), any()) } just Runs
        
        // When - handle same event twice
        handler.handle(event)
        handler.handle(event)
        
        // Then - should only send once (idempotent)
        coVerify(exactly = 1) {
            emailService.send(any(), any(), any())
        }
    }
}
```

---

## Integration Testing

Integration tests verify that components work together correctly.

### Testing with In-Memory Implementations

```kotlin
class UserRegistrationIntegrationTest {
    
    private lateinit var userRepository: UserRepository
    private lateinit var outboxRepository: MessageOutboxRepository
    private lateinit var registerHandler: RegisterUserCommandHandler
    private lateinit var getUserHandler: GetUserByIdQueryHandler
    
    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        outboxRepository = InMemoryOutboxRepository()
        registerHandler = RegisterUserCommandHandler(userRepository, outboxRepository)
        getUserHandler = GetUserByIdQueryHandler(userRepository)
    }
    
    @Test
    fun `should register user and retrieve it`() = runTest {
        // Given
        val command = RegisterUserCommand("john@example.com", "John Doe")
        
        // When - Register user
        val result = registerHandler(command)
        
        // Then - Registration successful
        assertTrue(result.isSuccess)
        val userId = result.getOrThrow()
        
        // When - Query user
        val query = GetUserByIdQuery(userId.value)
        val userDto = getUserHandler(query)
        
        // Then - User retrieved
        assertNotNull(userDto)
        assertEquals("john@example.com", userDto.email)
        assertEquals("John Doe", userDto.name)
    }
    
    @Test
    fun `should record events in outbox`() = runTest {
        // Given
        val command = RegisterUserCommand("john@example.com", "John Doe")
        
        // When
        registerHandler(command)
        
        // Then
        val unpublishedEvents = outboxRepository.findUnpublished(10)
        assertEquals(1, unpublishedEvents.size)
        assertEquals("UserRegisteredEvent", unpublishedEvents.first().eventType)
    }
}
```

### Testing with Test Containers

```kotlin
@Testcontainers
class OrderRepositoryIntegrationTest {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }
    
    private lateinit var database: Database
    private lateinit var orderRepository: OrderRepository
    
    @BeforeEach
    fun setup() {
        database = Database.connect(postgres.jdbcUrl, postgres.username, postgres.password)
        orderRepository = PostgresOrderRepository(database)
        
        // Run migrations
        database.migrate()
    }
    
    @Test
    fun `should save and retrieve order`() = runTest {
        // Given
        val order = Order(OrderId("123"), OrderStatus.DRAFT)
        
        // When
        orderRepository.save(order)
        val retrieved = orderRepository.findById(OrderId("123"))
        
        // Then
        assertNotNull(retrieved)
        assertEquals(order.id, retrieved.id)
        assertEquals(order.status, retrieved.status)
    }
}
```

---

## Property-Based Testing

Test properties that should always hold true.

```kotlin
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.checkAll
import io.kotest.property.arbitrary.string

class EmailPropertyTest : StringSpec({
    
    "valid emails should be accepted" {
        val validEmails = listOf(
            "test@example.com",
            "user.name@example.com",
            "user+tag@example.co.uk"
        )
        
        validEmails.forEach { email ->
            Email(email).value shouldBe email
        }
    }
    
    "entities with same ID should always be equal" {
        checkAll<String> { id ->
            val entity1 = User(UserId(id), Email("test@example.com"), "Test")
            val entity2 = User(UserId(id), Email("other@example.com"), "Other")
            
            entity1 shouldBe entity2
            entity1.hashCode() shouldBe entity2.hashCode()
        }
    }
})
```

---

## End-to-End Testing

Test the entire application flow.

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserRegistrationE2ETest {
    
    @Autowired
    private lateinit var restTemplate: TestRestTemplate
    
    @Test
    fun `should register user via API`() {
        // Given
        val request = RegisterUserRequest(
            email = "john@example.com",
            name = "John Doe"
        )
        
        // When
        val response = restTemplate.postForEntity(
            "/api/users",
            request,
            UserResponse::class.java
        )
        
        // Then
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body?.userId)
        
        // Verify user can be retrieved
        val userId = response.body!!.userId
        val getResponse = restTemplate.getForEntity(
            "/api/users/$userId",
            UserDto::class.java
        )
        
        assertEquals(HttpStatus.OK, getResponse.statusCode)
        assertEquals("john@example.com", getResponse.body?.email)
    }
}
```

---

## Testing Best Practices

### 1. Test Naming

Use descriptive test names:

```kotlin
// ❌ Bad
@Test
fun test1() { }

// ✅ Good
@Test
fun `should register user successfully when email is valid`() { }
```

### 2. Arrange-Act-Assert (AAA)

Structure tests clearly:

```kotlin
@Test
fun `should place order`() {
    // Arrange (Given)
    val order = Order(OrderId("123"), OrderStatus.DRAFT)
    val items = listOf(OrderItem("ITEM-1", Money(100.0, "USD")))
    
    // Act (When)
    order.place(items)
    
    // Assert (Then)
    assertEquals(OrderStatus.PLACED, order.status)
    assertTrue(order.hasEvents())
}
```

### 3. Test One Thing

Each test should verify one behavior:

```kotlin
// ❌ Bad - Testing multiple things
@Test
fun testOrder() {
    order.place(items)
    assertEquals(OrderStatus.PLACED, order.status)
    
    order.cancel()
    assertEquals(OrderStatus.CANCELLED, order.status)
}

// ✅ Good - Separate tests
@Test
fun `should place order`() {
    order.place(items)
    assertEquals(OrderStatus.PLACED, order.status)
}

@Test
fun `should cancel order`() {
    order.cancel()
    assertEquals(OrderStatus.CANCELLED, order.status)
}
```

### 4. Use Test Fixtures

Create reusable test data:

```kotlin
object UserMother {
    fun active() = User(
        id = UserId(UUID.randomUUID().toString()),
        email = Email("test@example.com"),
        name = "Test User",
        status = UserStatus.ACTIVE
    )
    
    fun pending() = active().copy(status = UserStatus.PENDING)
    
    fun withEmail(email: String) = active().copy(email = Email(email))
}

// Usage
@Test
fun `should activate pending user`() {
    val user = UserMother.pending()
    user.activate()
    assertEquals(UserStatus.ACTIVE, user.status)
}
```

### 5. Mock Only External Dependencies

```kotlin
// ✅ Good - Mock external dependencies
val emailService = mockk<EmailService>()
val handler = SendWelcomeEmailHandler(emailService)

// ❌ Bad - Don't mock domain objects
val order = mockk<Order>()  // Don't do this!
```

---

## Test Coverage

Aim for high coverage on critical paths:

- **Domain Layer**: 90%+ (pure logic, easy to test)
- **Application Layer**: 80%+ (orchestration)
- **Infrastructure Layer**: 60%+ (integration tests)
- **Presentation Layer**: 70%+ (E2E tests)

```bash
# Run tests with coverage
./gradlew test jacocoTestReport

# View coverage report
open lib/build/reports/jacoco/test/html/index.html
```

---

## 🚀 Next Steps

- **[Core Concepts](Core-Concepts.md)** - Understanding what to test
- **[Architecture Overview](Architecture-Overview.md)** - Testing at each layer
- **[Common Mistakes](Common-Mistakes.md)** - Avoid testing pitfalls
