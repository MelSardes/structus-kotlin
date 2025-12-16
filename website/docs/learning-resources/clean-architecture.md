---
sidebar_position: 2
---

# Clean Architecture Guide

Clean Architecture is the foundational principle behind Structus's design. This guide explains clean architecture concepts and how they're implemented in Structus.

## What is Clean Architecture?

Clean Architecture is a software design philosophy introduced by Robert C. Martin (Uncle Bob) that separates concerns into concentric layers, with dependencies pointing inward. This ensures that business logic remains independent of frameworks, databases, and UI.

## Core Principles

1. **Independence from Frameworks**: The architecture doesn't depend on the existence of libraries or frameworks
2. **Testability**: Business rules can be tested without UI, database, or external elements
3. **Independence from UI**: The UI can change without changing the system
4. **Independence from Database**: Business rules aren't bound to a specific database
5. **Independence from External Agency**: Business rules don't know about the outside world

## The Clean Architecture Layers

Clean Architecture organizes code into concentric layers:

```
┌─────────────────────────────────────────┐
│         Frameworks & Drivers            │
│  (Web, UI, Database, External Interfaces)│
└─────────────────────────────────────────┘
              ↓ depends on
┌─────────────────────────────────────────┐
│         Interface Adapters              │
│  (Controllers, Presenters, Gateways)     │
└─────────────────────────────────────────┘
              ↓ depends on
┌─────────────────────────────────────────┐
│        Application Business Rules       │
│  (Use Cases, Application Services)      │
└─────────────────────────────────────────┘
              ↓ depends on
┌─────────────────────────────────────────┐
│        Enterprise Business Rules        │
│  (Entities, Value Objects)              │
└─────────────────────────────────────────┘
```

### 1. Enterprise Business Rules (Entities)

The innermost layer contains business entities that encapsulate the most general and high-level rules.

```kotlin
// Structus domain entity example
class User private constructor(
    override val id: UserId,
    val email: Email,
    val status: UserStatus
) : AggregateRoot<UserId>() {
    
    fun activate() {
        require(status == UserStatus.PENDING) { "Only pending users can be activated" }
        // Business logic here
    }
    
    companion object {
        fun create(email: Email): User {
            val user = User(UserId(UUID.randomUUID()), email, UserStatus.PENDING)
            user.recordEvent(UserCreatedEvent(user.id.value, email.value))
            return user
        }
    }
}
```

### 2. Application Business Rules (Use Cases)

This layer contains application-specific business rules that orchestrate the flow of data to and from entities.

```kotlin
// Structus command handler example
class CreateUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            val email = Email(command.email)
            val user = User.create(email)
            
            userRepository.save(user)
            user.domainEvents.forEach { outboxRepository.save(it) }
            user.clearEvents()
            
            user.id
        }
    }
}
```

### 3. Interface Adapters

Adapters convert data between the use cases/entities and external agencies such as the database or web.

```kotlin
// Structus repository implementation example
class UserRepositoryImpl(
    private val database: Database
) : UserRepository {
    
    override suspend fun save(user: User) {
        database.execute(
            "INSERT INTO users (id, email, status) VALUES (?, ?, ?)",
            user.id.value,
            user.email.value,
            user.status.name
        )
    }
    
    override suspend fun findById(id: UserId): User? {
        return database.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            id.value
        )?.let { mapToDomain(it) }
    }
    
    private fun mapToDomain(row: DatabaseRow): User {
        // Mapping code
    }
}
```

### 4. Frameworks & Drivers

The outermost layer consists of frameworks and tools such as the database, the web framework, etc.

```kotlin
// Spring Boot controller example
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val createUserHandler: CreateUserCommandHandler,
    private val getUserHandler: GetUserByIdQueryHandler
) {
    
    @PostMapping
    suspend fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<Any> {
        val command = CreateUserCommand(request.email)
        
        return when (val result = createUserHandler(command)) {
            is Result.Success -> ResponseEntity.status(HttpStatus.CREATED)
                .body(mapOf("id" to result.value.value))
            is Result.Failure -> ResponseEntity.badRequest()
                .body(mapOf("error" to result.error.message))
        }
    }
}
```

## The Dependency Rule

The fundamental rule of Clean Architecture is that dependencies always point inward. Inner circles know nothing about outer circles.

In Structus, this is achieved through:

1. **Interfaces defined in inner layers**
2. **Implementations in outer layers**
3. **Dependency Injection** to wire everything together

## Benefits in Structus

Structus leverages Clean Architecture to provide:

- **Framework Independence**: Your business logic works regardless of the framework you use (Spring, Ktor, etc.)
- **Testability**: Domain and application layers can be tested in isolation
- **Maintainability**: Changes to external systems don't affect your core business logic
- **Flexibility**: Easy to swap out infrastructure components

## Additional Resources

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [The Clean Architecture (Uncle Bob)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Clean Architecture with Kotlin](https://proandroiddev.com/clean-architecture-with-kotlin-multiplatform-6d5f2b5e0bba)