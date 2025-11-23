# Error Handling

Comprehensive guide to error handling in Structus using Kotlin's `Result` type and domain exceptions.

## 🎯 Error Handling Philosophy

Structus promotes **explicit error handling** using:
1. **Kotlin `Result` type** - For expected failures
2. **Domain exceptions** - For exceptional conditions
3. **Type-safe errors** - Compile-time safety

## 📦 Using Kotlin Result Type

### Basic Usage

```kotlin
// Command handler returns Result
class CreateUserCommandHandler : CommandHandler<CreateUserCommand, Result<UserId>> {
    
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            // Business logic that might fail
            val email = Email(command.email) // Throws if invalid
            val user = User.create(email, command.name)
            repository.save(user)
            user.id
        }
    }
}
```

### Handling Results

```kotlin
// In controller or application service
val result = commandHandler(createUserCommand)

result.fold(
    onSuccess = { userId ->
        // Success path
        ResponseEntity.created(URI("/users/${userId.value}"))
            .body(UserCreatedResponse(userId.value))
    },
    onFailure = { error ->
        // Error path
        when (error) {
            is ValidationException -> ResponseEntity.badRequest()
                .body(ErrorResponse(error.message))
            is EntityAlreadyExistsException -> ResponseEntity.status(409)
                .body(ErrorResponse(error.message))
            else -> ResponseEntity.internalServerError()
                .body(ErrorResponse("Internal error"))
        }
    }
)
```

## 🚨 Domain Exceptions

### Built-in Exception Types

Structus doesn't provide built-in exceptions - you define them in your domain:

```kotlin
// Domain exceptions
sealed class DomainException(message: String) : Exception(message)

class EntityNotFoundException(
    message: String,
    val entityType: String,
    val entityId: String
) : DomainException(message)

class EntityAlreadyExistsException(
    message: String,
    val entityType: String,
    val identifier: String
) : DomainException(message)

class ValidationException(
    message: String,
    val field: String? = null,
    val violations: List<String> = emptyList()
) : DomainException(message)

class BusinessRuleViolationException(
    message: String,
    val rule: String
) : DomainException(message)

class ConcurrencyException(
    message: String,
    val expectedVersion: Int,
    val actualVersion: Int
) : DomainException(message)
```

### When to Use Exceptions

Use exceptions for **exceptional conditions**:

```kotlin
class User(
    override val id: UserId,
    var email: Email,
    var status: UserStatus
) : AggregateRoot<UserId>() {
    
    fun activate() {
        // Business rule violation - exceptional condition
        if (status == UserStatus.DELETED) {
            throw BusinessRuleViolationException(
                message = "Cannot activate deleted user",
                rule = "USER_ACTIVATION"
            )
        }
        
        status = UserStatus.ACTIVE
        recordEvent(UserActivatedEvent(id.value))
    }
}
```

## 🎨 Error Handling Patterns

### Pattern 1: Result with Custom Error Types

```kotlin
// Define error types
sealed interface UserError {
    data class NotFound(val userId: String) : UserError
    data class EmailAlreadyExists(val email: String) : UserError
    data class InvalidEmail(val email: String) : UserError
}

// Use in handler
class CreateUserCommandHandler : 
    CommandHandler<CreateUserCommand, Result<UserId>> {
    
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            // Validate email
            if (!isValidEmail(command.email)) {
                throw ValidationException(
                    "Invalid email format",
                    field = "email"
                )
            }
            
            // Check uniqueness
            if (repository.existsByEmail(command.email)) {
                throw EntityAlreadyExistsException(
                    "User with email already exists",
                    entityType = "User",
                    identifier = command.email
                )
            }
            
            val user = User.create(Email(command.email), command.name)
            repository.save(user)
            user.id
        }
    }
}
```

### Pattern 2: Railway-Oriented Programming

```kotlin
// Extension functions for Result chaining
suspend fun <T, R> Result<T>.andThen(
    block: suspend (T) -> Result<R>
): Result<R> {
    return fold(
        onSuccess = { block(it) },
        onFailure = { Result.failure(it) }
    )
}

// Usage
suspend fun registerUser(command: CreateUserCommand): Result<UserId> {
    return validateEmail(command.email)
        .andThen { email -> checkEmailUniqueness(email) }
        .andThen { email -> createUser(email, command.name) }
        .andThen { user -> saveUser(user) }
        .andThen { user -> publishEvents(user) }
}
```

### Pattern 3: Validation Before Execution

```kotlin
// Value object with validation
data class Email(val value: String) : ValueObject {
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(value.matches(EMAIL_REGEX)) { "Invalid email format: $value" }
        require(value.length <= 255) { "Email too long" }
    }
    
    companion object {
        private val EMAIL_REGEX = 
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    }
}

// Safe factory method
fun Email.Companion.createOrNull(value: String): Email? {
    return try {
        Email(value)
    } catch (e: IllegalArgumentException) {
        null
    }
}

// Usage in handler
val email = Email.createOrNull(command.email)
    ?: return Result.failure(ValidationException("Invalid email"))
```

## 🏗️ Layer-Specific Error Handling

### Domain Layer

```kotlin
// Domain layer throws exceptions for business rule violations
class Order(
    override val id: OrderId,
    var status: OrderStatus,
    val items: MutableList<OrderItem>
) : AggregateRoot<OrderId>() {
    
    fun cancel() {
        // Business rule: Can't cancel shipped orders
        if (status == OrderStatus.SHIPPED) {
            throw BusinessRuleViolationException(
                message = "Cannot cancel shipped order",
                rule = "ORDER_CANCELLATION"
            )
        }
        
        status = OrderStatus.CANCELLED
        recordEvent(OrderCancelledEvent(id.value))
    }
    
    fun addItem(item: OrderItem) {
        // Business rule: Max 100 items
        if (items.size >= 100) {
            throw BusinessRuleViolationException(
                message = "Order cannot have more than 100 items",
                rule = "MAX_ORDER_ITEMS"
            )
        }
        
        items.add(item)
        recordEvent(OrderItemAddedEvent(id.value, item.productId))
    }
}
```

### Application Layer

```kotlin
// Application layer catches exceptions and returns Result
class CancelOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CancelOrderCommand, Result<Unit>> {
    
    override suspend fun invoke(command: CancelOrderCommand): Result<Unit> {
        return runCatching {
            // Find order
            val order = orderRepository.findById(OrderId(command.orderId))
                ?: throw EntityNotFoundException(
                    message = "Order not found",
                    entityType = "Order",
                    entityId = command.orderId
                )
            
            // Cancel (may throw BusinessRuleViolationException)
            order.cancel()
            
            // Save
            orderRepository.save(order)
            
            // Publish events
            order.domainEvents.forEach { event ->
                outboxRepository.save(OutboxMessage.from(event))
            }
            order.clearEvents()
        }
    }
}
```

### Presentation Layer

```kotlin
// Controller maps exceptions to HTTP responses
@RestController
class OrderController(
    private val cancelOrderHandler: CancelOrderCommandHandler
) {
    
    @PostMapping("/orders/{orderId}/cancel")
    suspend fun cancelOrder(@PathVariable orderId: String): ResponseEntity<*> {
        val result = cancelOrderHandler(CancelOrderCommand(orderId))
        
        return result.fold(
            onSuccess = {
                ResponseEntity.ok(SuccessResponse("Order cancelled"))
            },
            onFailure = { error ->
                when (error) {
                    is EntityNotFoundException -> 
                        ResponseEntity.status(404).body(
                            ErrorResponse(
                                code = "ORDER_NOT_FOUND",
                                message = error.message ?: "Order not found"
                            )
                        )
                    
                    is BusinessRuleViolationException -> 
                        ResponseEntity.status(422).body(
                            ErrorResponse(
                                code = error.rule,
                                message = error.message ?: "Business rule violation"
                            )
                        )
                    
                    else -> 
                        ResponseEntity.status(500).body(
                            ErrorResponse(
                                code = "INTERNAL_ERROR",
                                message = "An unexpected error occurred"
                            )
                        )
                }
            }
        )
    }
}
```

## 📊 Error Response DTOs

```kotlin
// Standard error response
data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, Any>? = null
)

// Validation error response
data class ValidationErrorResponse(
    val code: String = "VALIDATION_ERROR",
    val message: String,
    val violations: List<FieldViolation>,
    val timestamp: Instant = Instant.now()
)

data class FieldViolation(
    val field: String,
    val message: String,
    val rejectedValue: Any? = null
)
```

## 🔍 Logging Errors

```kotlin
class CreateUserCommandHandler(
    private val repository: UserRepository,
    private val logger: Logger = LoggerFactory.getLogger(CreateUserCommandHandler::class.java)
) : CommandHandler<CreateUserCommand, Result<UserId>> {
    
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        logger.info("Creating user with email: ${command.email}")
        
        return runCatching {
            val user = User.create(Email(command.email), command.name)
            repository.save(user)
            
            logger.info("User created successfully: ${user.id.value}")
            user.id
        }.onFailure { error ->
            when (error) {
                is ValidationException -> 
                    logger.warn("Validation failed for user creation: ${error.message}")
                is EntityAlreadyExistsException -> 
                    logger.warn("User already exists: ${error.identifier}")
                else -> 
                    logger.error("Unexpected error creating user", error)
            }
        }
    }
}
```

## ✅ Best Practices

### 1. **Use Result for Expected Failures**
```kotlin
// ✅ Good - Expected failure
fun findUser(id: UserId): Result<User>

// ❌ Bad - Exception for expected case
fun findUser(id: UserId): User // throws if not found
```

### 2. **Use Exceptions for Exceptional Conditions**
```kotlin
// ✅ Good - Business rule violation
fun cancel() {
    if (status == SHIPPED) {
        throw BusinessRuleViolationException("Cannot cancel shipped order")
    }
}

// ❌ Bad - Using Result for business rules
fun cancel(): Result<Unit> {
    if (status == SHIPPED) {
        return Result.failure(Exception("Cannot cancel"))
    }
}
```

### 3. **Validate Early**
```kotlin
// ✅ Good - Validate in value object constructor
data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email" }
    }
}

// ❌ Bad - Validate later
data class Email(val value: String) : ValueObject
fun validate(email: Email): Boolean = email.value.matches(EMAIL_REGEX)
```

### 4. **Provide Context in Errors**
```kotlin
// ✅ Good - Rich context
throw EntityNotFoundException(
    message = "User not found with id: $userId",
    entityType = "User",
    entityId = userId
)

// ❌ Bad - Generic message
throw Exception("Not found")
```

### 5. **Don't Swallow Exceptions**
```kotlin
// ✅ Good - Propagate or handle properly
try {
    repository.save(user)
} catch (e: Exception) {
    logger.error("Failed to save user", e)
    throw e
}

// ❌ Bad - Silent failure
try {
    repository.save(user)
} catch (e: Exception) {
    // Do nothing
}
```

## 🔗 Related Topics

- **[Core Concepts](Core-Concepts.md)** - Commands and Queries
- **[Testing Strategies](Testing-Strategies.md)** - Testing error cases
- **[API Reference](API-Reference.md)** - Result type usage

---

**Next Steps**: Learn about [Testing Strategies](Testing-Strategies.md) to test error scenarios.
