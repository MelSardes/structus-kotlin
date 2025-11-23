# Quick Start Tutorial

Build your first application with Explicit Architecture in 15 minutes! This tutorial will guide you through creating a simple user registration system.

## 🎯 What We'll Build

A user registration system with:
- User aggregate with email validation
- Registration command and handler
- Query to retrieve users
- Domain events for user registration
- Transactional Outbox Pattern

## 📋 Prerequisites

- Kotlin 2.2.0+
- JDK 21+
- Library installed (see [Installation Guide](Installation-Guide.md))

## 🚀 Step 1: Define Value Objects

Value objects are immutable and self-validating.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.ValueObject

// Email value object with validation
data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { 
            "Invalid email format: $value" 
        }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}

// User ID value object
data class UserId(val value: String) : ValueObject

// User status enum
enum class UserStatus {
    PENDING,
    ACTIVE,
    SUSPENDED
}
```

## 🏗️ Step 2: Create the User Aggregate

The aggregate root manages state and records events.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent
import java.util.UUID

class User(
    override val id: UserId,
    var email: Email,
    var name: String,
    var status: UserStatus
) : AggregateRoot<UserId>() {
    
    fun activate() {
        require(status == UserStatus.PENDING) { 
            "User must be pending to activate" 
        }
        
        status = UserStatus.ACTIVE
        
        recordEvent(UserActivatedEvent(
            aggregateId = id.value,
            userId = id.value,
            email = email.value
        ))
    }
    
    companion object {
        fun create(email: Email, name: String): User {
            val user = User(
                id = UserId(UUID.randomUUID().toString()),
                email = email,
                name = name,
                status = UserStatus.PENDING
            )
            
            user.recordEvent(UserRegisteredEvent(
                aggregateId = user.id.value,
                userId = user.id.value,
                email = email.value,
                name = name
            ))
            
            return user
        }
    }
}
```

## 📢 Step 3: Define Domain Events

Events capture what happened in the domain.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

data class UserRegisteredEvent(
    override val aggregateId: String,
    val userId: String,
    val email: String,
    val name: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1
)

data class UserActivatedEvent(
    override val aggregateId: String,
    val userId: String,
    val email: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1
)
```

## 🗄️ Step 4: Define Repository Interface

The repository interface lives in the domain layer.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.Repository

interface UserRepository : Repository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun save(user: User)
    suspend fun existsByEmail(email: Email): Boolean
}
```

## 💾 Step 5: Implement Repository (Infrastructure)

Create an in-memory implementation for this tutorial.

```kotlin
class InMemoryUserRepository : UserRepository {
    private val users = mutableMapOf<UserId, User>()
    
    override suspend fun findById(id: UserId): User? {
        return users[id]
    }
    
    override suspend fun findByEmail(email: Email): User? {
        return users.values.find { it.email == email }
    }
    
    override suspend fun save(user: User) {
        users[user.id] = user
    }
    
    override suspend fun existsByEmail(email: Email): Boolean {
        return users.values.any { it.email == email }
    }
}
```

## 📝 Step 6: Create Commands

Commands represent intent to change state.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.Command

data class RegisterUserCommand(
    val email: String,
    val name: String
) : Command {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
    }
}

data class ActivateUserCommand(
    val userId: String
) : Command {
    init {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
    }
}
```

## ⚙️ Step 7: Implement Command Handlers

Handlers orchestrate business logic.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository

class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
        return runCatching {
            // Validate email doesn't exist
            val email = Email(command.email)
            if (userRepository.existsByEmail(email)) {
                throw IllegalStateException("Email already registered: ${command.email}")
            }
            
            // Create user
            val user = User.create(email, command.name)
            
            // Save user
            userRepository.save(user)
            
            // Save events to outbox (Transactional Outbox Pattern)
            user.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // Clear events
            user.clearEvents()
            
            user.id
        }
    }
}

class ActivateUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<ActivateUserCommand, Result<Unit>> {
    
    override suspend operator fun invoke(command: ActivateUserCommand): Result<Unit> {
        return runCatching {
            val user = userRepository.findById(UserId(command.userId))
                ?: throw IllegalArgumentException("User not found: ${command.userId}")
            
            user.activate()
            userRepository.save(user)
            
            // Save events to outbox
            user.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            user.clearEvents()
        }
    }
}
```

## 🔍 Step 8: Create Queries and Handlers

Queries retrieve data without side effects.

```kotlin
import com.melsardes.libraries.structuskotlin.application.queries.Query
import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler

// Query
data class GetUserByIdQuery(
    val userId: String
) : Query

// DTO
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val status: String
)

// Query Handler
class GetUserByIdQueryHandler(
    private val userRepository: UserRepository
) : QueryHandler<GetUserByIdQuery, UserDto?> {
    
    override suspend operator fun invoke(query: GetUserByIdQuery): UserDto? {
        val user = userRepository.findById(UserId(query.userId))
        return user?.let {
            UserDto(
                id = it.id.value,
                email = it.email.value,
                name = it.name,
                status = it.status.name
            )
        }
    }
}
```

## 🔄 Step 9: Implement Outbox Repository

For this tutorial, we'll use an in-memory implementation.

```kotlin
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository
import com.melsardes.libraries.structuskotlin.domain.OutboxMessage
import com.melsardes.libraries.structuskotlin.domain.events.DomainEvent

class InMemoryOutboxRepository : MessageOutboxRepository {
    private val messages = mutableListOf<OutboxMessage>()
    
    override suspend fun save(event: DomainEvent) {
        messages.add(OutboxMessage.from(event))
    }
    
    override suspend fun findUnpublished(limit: Int): List<OutboxMessage> {
        return messages.filter { !it.isPublished() }.take(limit)
    }
    
    override suspend fun markAsPublished(messageId: String) {
        messages.find { it.id == messageId }?.let {
            messages[messages.indexOf(it)] = it.copy(
                publishedAt = kotlin.time.Clock.System.now()
            )
        }
    }
    
    override suspend fun incrementRetryCount(messageId: String) {
        messages.find { it.id == messageId }?.let {
            messages[messages.indexOf(it)] = it.copy(
                retryCount = it.retryCount + 1
            )
        }
    }
    
    override suspend fun deletePublishedOlderThan(olderThanDays: Int): Int {
        val cutoff = kotlin.time.Clock.System.now() - kotlin.time.Duration.parse("${olderThanDays}d")
        val toDelete = messages.filter { 
            it.isPublished() && it.publishedAt!! < cutoff 
        }
        messages.removeAll(toDelete)
        return toDelete.size
    }
    
    override suspend fun findFailedEvents(maxRetries: Int): List<OutboxMessage> {
        return messages.filter { it.hasExceededRetries(maxRetries) }
    }
}
```

## 🎮 Step 10: Put It All Together

Now let's use everything we've built!

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Setup repositories
    val userRepository = InMemoryUserRepository()
    val outboxRepository = InMemoryOutboxRepository()
    
    // Setup handlers
    val registerHandler = RegisterUserCommandHandler(userRepository, outboxRepository)
    val activateHandler = ActivateUserCommandHandler(userRepository, outboxRepository)
    val getUserHandler = GetUserByIdQueryHandler(userRepository)
    
    // Register a user
    println("=== Registering User ===")
    val registerCommand = RegisterUserCommand(
        email = "john.doe@example.com",
        name = "John Doe"
    )
    
    val result = registerHandler(registerCommand)
    result.fold(
        onSuccess = { userId ->
            println("✅ User registered successfully: ${userId.value}")
            
            // Query the user
            println("\n=== Querying User ===")
            val query = GetUserByIdQuery(userId.value)
            val userDto = getUserHandler(query)
            println("User: $userDto")
            
            // Activate the user
            println("\n=== Activating User ===")
            val activateCommand = ActivateUserCommand(userId.value)
            activateHandler(activateCommand).fold(
                onSuccess = { 
                    println("✅ User activated successfully")
                    
                    // Query again
                    val updatedUser = getUserHandler(query)
                    println("Updated User: $updatedUser")
                },
                onFailure = { error ->
                    println("❌ Activation failed: ${error.message}")
                }
            )
            
            // Check outbox events
            println("\n=== Outbox Events ===")
            val unpublishedEvents = outboxRepository.findUnpublished(10)
            println("Unpublished events: ${unpublishedEvents.size}")
            unpublishedEvents.forEach { message ->
                println("- Event: ${message.eventType} (ID: ${message.id})")
            }
        },
        onFailure = { error ->
            println("❌ Registration failed: ${error.message}")
        }
    )
    
    // Try to register with same email (should fail)
    println("\n=== Attempting Duplicate Registration ===")
    val duplicateResult = registerHandler(registerCommand)
    duplicateResult.fold(
        onSuccess = { println("✅ Unexpected success") },
        onFailure = { error -> println("❌ Expected failure: ${error.message}") }
    )
}
```

## 📊 Expected Output

```
=== Registering User ===
✅ User registered successfully: 550e8400-e29b-41d4-a716-446655440000

=== Querying User ===
User: UserDto(id=550e8400-e29b-41d4-a716-446655440000, email=john.doe@example.com, name=John Doe, status=PENDING)

=== Activating User ===
✅ User activated successfully
Updated User: UserDto(id=550e8400-e29b-41d4-a716-446655440000, email=john.doe@example.com, name=John Doe, status=ACTIVE)

=== Outbox Events ===
Unpublished events: 2
- Event: UserRegisteredEvent (ID: 123e4567-e89b-12d3-a456-426614174000)
- Event: UserActivatedEvent (ID: 123e4567-e89b-12d3-a456-426614174001)

=== Attempting Duplicate Registration ===
❌ Expected failure: Email already registered: john.doe@example.com
```

## 🎉 Congratulations!

You've built a complete application using Explicit Architecture! You've learned:

- ✅ Creating value objects with validation
- ✅ Building aggregate roots with event recording
- ✅ Defining domain events
- ✅ Implementing repositories
- ✅ Creating commands and command handlers
- ✅ Building queries and query handlers
- ✅ Using the Transactional Outbox Pattern

## 🚀 Next Steps

- **[Core Concepts](Core-Concepts.md)** - Deep dive into each concept
- **[Architecture Overview](Architecture-Overview.md)** - Understand the big picture
- **[Transactional Outbox Pattern](Transactional-Outbox-Pattern.md)** - Learn about event publishing
- **[Testing Strategies](Testing-Strategies.md)** - Test your application
- **[Spring Boot Integration](Spring-Boot-Integration.md)** - Use with Spring Boot

## 💡 Tips

1. **Start Small**: Begin with one aggregate and expand
2. **Validate Early**: Put validation in value object constructors
3. **Record Events**: Always record events for significant state changes
4. **Clear Events**: Don't forget to clear events after publishing
5. **Use Result**: Prefer `Result<T>` for explicit error handling
