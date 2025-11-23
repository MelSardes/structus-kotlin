# CQRS Implementation

Command Query Responsibility Segregation (CQRS) is a pattern that separates read and write operations into different models.

## 🎯 What is CQRS?

CQRS separates the responsibility of handling **commands** (writes) from **queries** (reads):

- **Commands**: Change state, return minimal data (success/failure)
- **Queries**: Read data, never modify state, can be optimized differently

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│                  (Controllers/Handlers)                  │
└────────────┬─────────────────────────────┬──────────────┘
             │                             │
    ┌────────▼────────┐         ┌─────────▼────────┐
    │  Command Side   │         │   Query Side     │
    │   (Write Model) │         │  (Read Model)    │
    └────────┬────────┘         └─────────┬────────┘
             │                             │
    ┌────────▼────────┐         ┌─────────▼────────┐
    │ Command Handler │         │  Query Handler   │
    │   + Domain      │         │  + Projections   │
    └────────┬────────┘         └─────────┬────────┘
             │                             │
    ┌────────▼────────┐         ┌─────────▼────────┐
    │  Write Database │         │  Read Database   │
    │  (Normalized)   │         │ (Denormalized)   │
    └─────────────────┘         └──────────────────┘
```

## 📝 Command Side (Write Model)

### Command Definition

```kotlin
data class CreateUserCommand(
    val email: String,
    val name: String,
    val role: String
) : Command

data class UpdateUserEmailCommand(
    val userId: String,
    val newEmail: String
) : Command
```

### Command Handler

```kotlin
class CreateUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateUserCommand, Result<UserId>> {
    
    override suspend fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            // 1. Validate
            val email = Email(command.email)
            val name = UserName(command.name)
            
            // 2. Create aggregate
            val user = User.create(
                email = email,
                name = name,
                role = UserRole.valueOf(command.role)
            )
            
            // 3. Save to write database
            userRepository.save(user)
            
            // 4. Publish events via outbox
            user.domainEvents.forEach { event ->
                outboxRepository.save(OutboxMessage.from(event))
            }
            user.clearEvents()
            
            user.id
        }
    }
}
```

### Write Model Characteristics

- **Normalized**: Proper domain model with aggregates
- **Business Logic**: Rich domain behavior
- **Consistency**: Strong consistency within aggregate boundaries
- **Events**: Publishes domain events for eventual consistency

## 🔍 Query Side (Read Model)

### Query Definition

```kotlin
data class GetUserByIdQuery(
    val userId: String
) : Query

data class SearchUsersQuery(
    val searchTerm: String?,
    val role: String?,
    val page: Int = 0,
    val size: Int = 20
) : Query
```

### Query Handler

```kotlin
class GetUserByIdQueryHandler(
    private val queryRepository: UserQueryRepository
) : QueryHandler<GetUserByIdQuery, Result<UserDto>> {
    
    override suspend fun invoke(query: GetUserByIdQuery): Result<UserDto> {
        return runCatching {
            queryRepository.findById(UserId(query.userId))
                ?: throw EntityNotFoundException("User not found: ${query.userId}")
        }
    }
}
```

### Read Model Characteristics

- **Denormalized**: Optimized for queries (DTOs, projections)
- **No Business Logic**: Simple data retrieval
- **Eventually Consistent**: Updated via domain events
- **Multiple Views**: Different projections for different use cases

## 🔄 Synchronization via Events

### Event Handler Updates Read Model

```kotlin
class UserCreatedEventHandler(
    private val userProjectionRepository: UserProjectionRepository
) : DomainEventHandler<UserCreatedEvent> {
    
    override suspend fun handle(event: UserCreatedEvent) {
        val projection = UserProjection(
            id = event.aggregateId,
            email = event.email,
            name = event.name,
            role = event.role,
            status = "ACTIVE",
            createdAt = event.occurredAt
        )
        
        userProjectionRepository.save(projection)
    }
}
```

## 🎨 Implementation Patterns

### Pattern 1: Same Database, Different Tables

**Write Side**: `users` table (normalized)  
**Read Side**: `user_projections` table (denormalized)

```kotlin
// Write Model
data class UserTable(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val version: Int
)

// Read Model
data class UserProjection(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: String,
    val status: String,
    val lastLoginAt: Instant?,
    val createdAt: Instant
)
```

### Pattern 2: Different Databases

**Write Side**: PostgreSQL (transactional)  
**Read Side**: MongoDB/Elasticsearch (optimized for queries)

```kotlin
// Write Repository (PostgreSQL)
class UserCommandRepository : UserRepository {
    override suspend fun save(user: User) {
        // Save to PostgreSQL
    }
}

// Query Repository (MongoDB)
class UserQueryRepository {
    suspend fun search(criteria: SearchCriteria): List<UserDto> {
        // Query MongoDB with full-text search
    }
}
```

### Pattern 3: Event Sourcing + CQRS

Commands create events, queries read from projections built from events.

```kotlin
// Event Store (Write Side)
class UserEventStore {
    suspend fun append(userId: UserId, events: List<DomainEvent>) {
        // Append events to event stream
    }
}

// Projection (Read Side)
class UserProjectionBuilder : DomainEventHandler<DomainEvent> {
    override suspend fun handle(event: DomainEvent) {
        when (event) {
            is UserCreatedEvent -> createProjection(event)
            is UserEmailChangedEvent -> updateEmail(event)
            is UserDeletedEvent -> markDeleted(event)
        }
    }
}
```

## ✅ Benefits

### 1. **Optimized Performance**
- Write model: Optimized for consistency and business logic
- Read model: Optimized for query performance

### 2. **Scalability**
- Scale read and write sides independently
- Use different databases for different workloads

### 3. **Flexibility**
- Multiple read models for different use cases
- Change read model without affecting write model

### 4. **Simplified Queries**
- No complex joins
- Denormalized data ready to serve

### 5. **Better Security**
- Separate permissions for reads and writes
- Read-only replicas for queries

## ⚠️ Considerations

### 1. **Eventual Consistency**
- Read model may lag behind write model
- Need to handle stale data in UI

```kotlin
// Return command result with version
data class CommandResult(
    val userId: UserId,
    val version: Int
)

// Client can poll or use WebSocket for updates
```

### 2. **Increased Complexity**
- Two models to maintain
- Event handlers for synchronization
- More infrastructure

### 3. **Data Duplication**
- Same data in write and read models
- Storage overhead

### 4. **Debugging Challenges**
- Need to track events
- Monitor synchronization lag

## 🛠️ Implementation with Structus

### Step 1: Define Commands and Queries

```kotlin
// Commands
sealed interface UserCommand : Command
data class CreateUserCommand(...) : UserCommand
data class UpdateUserCommand(...) : UserCommand

// Queries
sealed interface UserQuery : Query
data class GetUserQuery(...) : UserQuery
data class SearchUsersQuery(...) : UserQuery
```

### Step 2: Implement Handlers

```kotlin
// Command Handler (uses domain model)
class CreateUserCommandHandler(
    private val repository: UserRepository,
    private val outbox: MessageOutboxRepository
) : CommandHandler<CreateUserCommand, Result<UserId>>

// Query Handler (uses projections)
class SearchUsersQueryHandler(
    private val queryRepo: UserQueryRepository
) : QueryHandler<SearchUsersQuery, Result<List<UserDto>>>
```

### Step 3: Synchronize via Events

```kotlin
// Event handler updates read model
class UserEventProjector(
    private val projectionRepo: UserProjectionRepository
) : DomainEventHandler<UserEvent> {
    
    override suspend fun handle(event: UserEvent) {
        when (event) {
            is UserCreatedEvent -> createProjection(event)
            is UserUpdatedEvent -> updateProjection(event)
        }
    }
}
```

### Step 4: Wire Everything Together

```kotlin
// In your application setup
val commandBus = CommandBusImpl().apply {
    register(CreateUserCommand::class, createUserHandler)
    register(UpdateUserCommand::class, updateUserHandler)
}

val eventPublisher = DomainEventPublisherImpl().apply {
    subscribe(UserCreatedEvent::class, userEventProjector)
    subscribe(UserUpdatedEvent::class, userEventProjector)
}
```

## 📊 When to Use CQRS

### ✅ Good Fit
- High read-to-write ratio (10:1 or higher)
- Complex domain logic on write side
- Need different query optimizations
- Scalability requirements differ for reads/writes
- Multiple client types with different data needs

### ❌ Not Recommended
- Simple CRUD applications
- Low traffic applications
- Team unfamiliar with pattern
- Tight consistency requirements everywhere

## 🔗 Related Topics

- **[Core Concepts](Core-Concepts.md)** - Commands and Queries
- **[Architecture Overview](Architecture-Overview.md)** - CQRS in context
- **[Event Sourcing](Event-Sourcing.md)** - CQRS with Event Sourcing
- **[Transactional Outbox Pattern](Transactional-Outbox-Pattern.md)** - Event publishing

---

**Next Steps**: Explore [Event Sourcing](Event-Sourcing.md) for a complete event-driven architecture.
