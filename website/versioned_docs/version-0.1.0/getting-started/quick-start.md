---
sidebar_position: 2
pagination_next: getting-started/core-concepts
pagination_prev: getting-started/installation
---

# Quick Start Tutorial

Build your first application with Explicit Architecture in 15 minutes!

## What We'll Build

A user registration system with:
- User aggregate with email validation
- Registration command and handler
- Query to retrieve users
- Domain events for user registration
- Transactional Outbox Pattern

## Prerequisites

- Kotlin 2.2.0+
- JDK 21+
- Library installed (see [Installation Guide](installation))

## Step 1: Define Value Objects

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
        private val EMAIL_REGEX = 
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
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

## Step 2: Create the User Aggregate

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

## Step 3: Define Domain Events

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

## Step 4: Define Repository Interface

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

## Step 5: Create Commands

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

## Step 6: Implement Command Handlers

Handlers orchestrate business logic.

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository

class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(
        command: RegisterUserCommand
    ): Result<UserId> {
        return runCatching {
            // Validate email doesn't exist
            val email = Email(command.email)
            if (userRepository.existsByEmail(email)) {
                throw IllegalStateException(
                    "Email already registered: ${command.email}"
                )
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
```

## Step 7: Create Queries and Handlers

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
    
    override suspend operator fun invoke(
        query: GetUserByIdQuery
    ): UserDto? {
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

## Congratulations! 🎉

You've built a complete application using Explicit Architecture! You've learned:

- ✅ Creating value objects with validation
- ✅ Building aggregate roots with event recording
- ✅ Defining domain events
- ✅ Implementing repositories
- ✅ Creating commands and command handlers
- ✅ Building queries and query handlers
- ✅ Using the Transactional Outbox Pattern

## Next Steps

- **[Core Concepts](core-concepts)** - Deep dive into each concept
- **[Architecture Overview](../architecture/overview)** - Understand the big picture
- **[Transactional Outbox Pattern](../advanced/transactional-outbox)** - Learn about event publishing
- **[Testing Strategies](../best-practices/testing-strategies)** - Test your application

## Tips

1. **Start Small**: Begin with one aggregate and expand
2. **Validate Early**: Put validation in value object constructors
3. **Record Events**: Always record events for significant state changes
4. **Clear Events**: Don't forget to clear events after publishing
5. **Use Result**: Prefer `Result<T>` for explicit error handling
