<div align="center">
  <img src="./structus-logo.svg" alt="Structus Logo" width="200"/>
  
  # Structus - Kotlin Architecture Toolkit

  [![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0+-blue.svg)](https://kotlinlang.org)
  [![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
  [![Version](https://img.shields.io/badge/Version-0.1.0-orange.svg)](https://github.com/structus-io/structus-kotlin)
  [![AI Agent Friendly](https://img.shields.io/badge/AI%20Agent-Friendly-purple.svg)](.ai/README.md)

  ![Structus Banner](./structus-banner.png)
</div>

A **pure Kotlin JVM library** providing the foundational building blocks for implementing **Explicit Architecture**—a synthesis of **Domain-Driven Design (DDD)**, **Command/Query Separation (CQS)**, and **Event-Driven Architecture (EDA)**.

## 🎯 Purpose

This library serves as a **shared kernel** for large-scale projects, defining the interfaces and base classes for all core business concepts and architectural patterns. It enforces clean architecture principles while remaining completely **framework-agnostic**.

## ✨ Key Features

- 🚀 **Pure Kotlin**: No framework dependencies (Spring, Ktor, Micronaut, etc.)
- 🔄 **Coroutine-Ready**: All I/O operations use suspend functions
- 📦 **Minimal Dependencies**: Only Kotlin stdlib + kotlinx-coroutines-core
- 📚 **Comprehensive Documentation**: Every component includes KDoc and examples
- 🏗️ **Framework-Agnostic**: Works with any framework or pure Kotlin
- 🎨 **Clean Architecture**: Enforces proper layer separation and dependencies

## 📦 Installation

### Building from Source

Since the library is not yet published to a public repository, you'll need to build and install it locally:

#### 1. Clone and Build

```bash
git clone https://github.com/structus-io/structus-kotlin.git
cd structus-kotlin
./gradlew build publishToMavenLocal
```

This will install the library to your local Maven repository (`~/.m2/repository`).

#### 2. Add to Your Project

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    mavenLocal()  // Add local Maven repository
    mavenCentral()
}

dependencies {
    implementation("com.melsardes.libraries:structus-kotlin:0.1.0")
}
```

**Gradle (Groovy)**

```groovy
repositories {
    mavenLocal()  // Add local Maven repository
    mavenCentral()
}

dependencies {
    implementation 'com.melsardes.libraries:structus-kotlin:0.1.0'
}
```

**Maven**

```xml
<dependency>
    <groupId>com.melsardes.libraries</groupId>
    <artifactId>structus-kotlin</artifactId>
    <version>0.1.0</version>
</dependency>
```

> **Note**: Maven automatically checks the local repository (`~/.m2/repository`) before remote repositories.

## 🏛️ Architecture Components

### Domain Layer (`com.melsardes.libraries.structuskotlin.domain`)

#### Core Building Blocks

```kotlin
// Entity: Identity-based domain objects
abstract class Entity<ID> {
    abstract val id: ID
    // equals/hashCode based on ID
}

// Value Object: Attribute-based immutable objects
interface ValueObject

// Aggregate Root: Consistency boundary with event management and lifecycle
abstract class AggregateRoot<ID> : Entity<ID>() {
    val domainEvents: List<DomainEvent>
    protected fun recordEvent(event: DomainEvent)
    fun clearEvents()
    
    // Lifecycle management
    internal fun markAsCreated(by: String, at: kotlin.time.Instant = Clock.System.now())
    internal fun markAsUpdated(by: String, at: kotlin.time.Instant = Clock.System.now())
    fun softDelete(by: String, at: kotlin.time.Instant = Clock.System.now())
    fun restore(by: String, at: kotlin.time.Instant = Clock.System.now())
    fun isDeleted(): Boolean
    fun isActive(): Boolean
    internal fun incrementVersion()
}

// Repository: Persistence contract
interface Repository
```

#### Events

```kotlin
// Domain Event: Something that happened
interface DomainEvent {
    val eventId: String
    val occurredAt: kotlin.time.Instant  // Uses Kotlin multiplatform time API
    val aggregateId: String
}

// Transactional Outbox Pattern
interface MessageOutboxRepository : Repository {
    suspend fun save(event: DomainEvent)
    suspend fun findUnpublished(limit: Int): List<OutboxMessage>
    suspend fun markAsPublished(messageId: String)
    suspend fun incrementRetryCount(messageId: String)
}
```

### Application Layer - Commands (`com.melsardes.libraries.structuskotlin.application.commands`)

```kotlin
// Command: Intent to change state
interface Command

// Command Handler: Executes business logic (uses invoke operator)
interface CommandHandler<in C : Command, out R> {
    suspend operator fun invoke(command: C): R
}

// Command Bus: Dispatches commands to handlers
interface CommandBus {
    fun <C : Command, R> register(commandClass: KClass<C>, handler: CommandHandler<C, R>)
    suspend fun <C : Command, R> dispatch(command: C): R
}
```

### Application Layer - Queries (`com.melsardes.libraries.structuskotlin.application.queries`)

```kotlin
// Query: Request for data
interface Query

// Query Handler: Retrieves data (uses invoke operator)
interface QueryHandler<in Q : Query, out R> {
    suspend operator fun invoke(query: Q): R
}
```

### Application Layer - Events (`com.melsardes.libraries.structuskotlin.application.events`)

```kotlin
// Domain Event Publisher: Publishes events to external systems
interface DomainEventPublisher {
    suspend fun publish(event: DomainEvent)
    suspend fun publishBatch(events: List<DomainEvent>)
}
```

## 🚀 Quick Start

### 1. Define Your Domain Model

```kotlin
// Value Object
data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}

// Entity ID
data class UserId(val value: String) : ValueObject

// Aggregate Root
class User(
    override val id: UserId,
    var email: Email,
    var name: String,
    var status: UserStatus
) : AggregateRoot<UserId>() {
    
    fun register(email: Email, name: String) {
        this.email = email
        this.name = name
        this.status = UserStatus.ACTIVE
        
        recordEvent(UserRegisteredEvent(
            aggregateId = id.value,
            userId = id.value,
            email = email.value,
            registeredAt = kotlin.time.Clock.System.now()
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
            user.register(email, name)
            return user
        }
    }
}

// Domain Event
data class UserRegisteredEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: kotlin.time.Instant = kotlin.time.Clock.System.now(),
    override val aggregateId: String,
    val userId: String,
    val email: String,
    val registeredAt: kotlin.time.Instant
) : DomainEvent

// Repository Interface
interface UserRepository : Repository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun save(user: User)
    suspend fun existsByEmail(email: Email): Boolean
}
```

### 2. Define Commands and Handlers

```kotlin
// Command
data class RegisterUserCommand(
    val email: String,
    val name: String
) : Command {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
    }
}

// Command Handler
class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
        return runCatching {
            // Check if email already exists
            if (userRepository.existsByEmail(Email(command.email))) {
                throw IllegalStateException("Email already exists")
            }
            
            // Create user
            val user = User.create(
                email = Email(command.email),
                name = command.name
            )
            
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

### 3. Define Queries and Handlers

```kotlin
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

### 4. Use in Your Application

```kotlin
// In your controller/endpoint
class UserController(
    private val commandBus: CommandBus,
    private val getUserByIdHandler: GetUserByIdQueryHandler
) {
    
    suspend fun registerUser(request: RegisterUserRequest): UserResponse {
        val command = RegisterUserCommand(
            email = request.email,
            name = request.name
        )
        
        val result = commandBus.dispatch(command)
        
        return result.fold(
            onSuccess = { userId -> UserResponse(userId = userId.value) },
            onFailure = { throw it }
        )
    }
    
    suspend fun getUser(userId: String): UserDto? {
        val query = GetUserByIdQuery(userId)
        return getUserByIdHandler(query)  // Invoke operator
    }
}
```

## 📖 Documentation

- **[📚 Official Documentation Website](https://structus-io.github.io/structus-kotlin/)** - Complete guides, tutorials, and API reference
- **[GUIDE.md](GUIDE.md)**: Comprehensive guide on project structure and conventions
- **[ASSESSMENT.md](ASSESSMENT.md)**: Implementation checklist and improvement suggestions
- **API Documentation**: Generated KDoc available in the library

## 🏗️ Project Structure

```
lib/src/main/kotlin/com/melsardes/libraries/structuskotlin/
├── domain/
│   ├── Entity.kt                    # Base entity class
│   ├── ValueObject.kt               # Value object marker
│   ├── AggregateRoot.kt             # Aggregate root with events & lifecycle
│   ├── Repository.kt                # Repository marker
│   ├── MessageOutboxRepository.kt   # Outbox pattern support
│   ├── Result.kt                    # Result type for error handling
│   └── events/
│       ├── DomainEvent.kt           # Domain event interface
│       └── BaseDomainEvent.kt       # Base event implementation
├── application/
│   ├── commands/
│   │   ├── Command.kt               # Command marker
│   │   ├── CommandHandler.kt        # Command handler (invoke operator)
│   │   └── CommandBus.kt            # Command bus interface
│   ├── queries/
│   │   ├── Query.kt                 # Query marker
│   │   └── QueryHandler.kt          # Query handler (invoke operator)
│   └── events/
│       ├── DomainEventPublisher.kt  # Event publisher interface
│       └── DomainEventHandler.kt    # Event handler interface
```

## 🎯 Design Principles

### 1. Dependency Rule
Layers can only depend on layers beneath them:
- **Domain** → Nothing (pure business logic)
- **Application** → Domain
- **Infrastructure** → Domain + Application
- **Presentation** → Application

### 2. Framework Independence
The library has no framework dependencies, making it usable with:
- Spring Boot
- Ktor
- Micronaut
- Quarkus
- Pure Kotlin applications

### 3. Testability
All interfaces enable easy testing through:
- Mock implementations
- In-memory implementations
- Test doubles

### 4. Explicit Over Implicit
- No magic or hidden behavior
- Clear contracts through interfaces
- Explicit error handling

## 🔧 Advanced Patterns

### Transactional Outbox Pattern

```kotlin
suspend fun invoke(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        withTransaction {
            // 1. Execute domain logic
            val order = Order.create(command.customerId, command.items)
            
            // 2. Save aggregate
            orderRepository.save(order)
            
            // 3. Save events to outbox (same transaction)
            order.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // 4. Clear events
            order.clearEvents()
            
            order.id
        }
    }
}

// Separate process publishes events
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

### CQRS with Separate Read Models

```kotlin
// Write side: Use domain model
class CreateUserHandler : CommandHandler<CreateUserCommand, Result<UserId>> {
    override suspend operator fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            val user = User.create(command.email, command.name)
            userRepository.save(user)
            user.id
        }
    }
}

// Read side: Use optimized read model
class GetUserHandler : QueryHandler<GetUserQuery, UserDto?> {
    override suspend operator fun invoke(query: GetUserQuery): UserDto? {
        // Direct database query, bypassing domain model
        return jdbcTemplate.queryForObject(
            "SELECT id, email, name FROM users WHERE id = ?",
            UserDto::class.java,
            query.userId
        )
    }
}
```

## 🤖 AI Agent Support

**Structus is AI-agent-friendly!** We provide comprehensive resources to help AI coding assistants (GitHub Copilot, Cursor, Claude, ChatGPT, etc.) understand and properly use this library.

### Quick Start for AI Agents

Point your AI assistant to the `.ai/` directory for:

- **[Library Overview](.ai/library-overview.md)** - Core concepts and architecture
- **[Usage Patterns](.ai/usage-patterns.md)** - Correct patterns and anti-patterns
- **[Code Templates](.ai/code-templates.md)** - Ready-to-use code templates
- **[Prompt Templates](.ai/prompts/)** - Pre-written prompts for common tasks

### Example AI Prompt

```markdown
I'm using the Structus library (com.melsardes.libraries.structuskotlin) to build an e-commerce platform.

Please read these files to understand the architecture:
1. .ai/library-overview.md - Core concepts
2. .ai/usage-patterns.md - Implementation patterns
3. .ai/code-templates.md - Code templates

Then help me create a new Order aggregate with the following requirements:
[describe your requirements here]
```

### For Developers

To maximize AI assistance when using Structus:

1. **Share Context**: Reference `.ai/` files when asking AI for help
2. **Use Prompt Templates**: Copy from `.ai/prompts/` and customize
3. **Follow Patterns**: AI agents trained on `.ai/usage-patterns.md` will generate better code
4. **Leverage Templates**: Point AI to `.ai/code-templates.md` for boilerplate

See [.ai/README.md](.ai/README.md) for complete documentation.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

This library is inspired by:
- **[Explicit Architecture](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)** by [Herberto Graça](https://herbertograca.com/10-2/)
- **Domain-Driven Design** by Eric Evans
- **Implementing Domain-Driven Design** by Vaughn Vernon
- **Clean Architecture** by Robert C. Martin
- **CQRS** by Greg Young
- **Event Sourcing** patterns

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/structus-io/structus-kotlin/issues)
- **Discussions**: [GitHub Discussions](https://github.com/structus-io/structus-kotlin/discussions)
- **Documentation**: [Getting Started Guide](GETTING_STARTED.md)

---

**Made with ❤️ for the Kotlin community**
