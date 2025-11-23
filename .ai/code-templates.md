# Structus Code Templates for AI Agents

Ready-to-use code templates for common scenarios. Copy, customize, and use.

## Template 1: Complete Aggregate with Value Objects

```kotlin
// File: src/main/kotlin/com/example/domain/{aggregate}/{Aggregate}.kt
package com.example.domain.{aggregate}

import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.ValueObject
import com.melsardes.libraries.structuskotlin.domain.Result
import com.melsardes.libraries.structuskotlin.domain.DomainError
import java.time.Instant
import java.util.UUID

// Value Objects
data class {Aggregate}Id(val value: UUID) : ValueObject

data class {ValueObject}(
    val field1: String,
    val field2: Int
) : ValueObject

// Aggregate Root
class {Aggregate}(
    id: {Aggregate}Id,
    val field1: String,
    val field2: {ValueObject},
    val status: {Aggregate}Status,
    val createdAt: Instant,
    val updatedAt: Instant? = null
) : AggregateRoot<{Aggregate}Id>(id) {
    
    enum class {Aggregate}Status {
        DRAFT, ACTIVE, INACTIVE, DELETED
    }
    
    // Business logic method
    fun activate(): Result<{Aggregate}> {
        if (status == {Aggregate}Status.ACTIVE) {
            return Result.Failure(
                DomainError.InvalidOperation("Already active")
            )
        }
        return Result.Success(
            copy(status = {Aggregate}Status.ACTIVE, updatedAt = Instant.now())
        )
    }
    
    // Validation method
    fun validate(): Result<Unit> {
        if (field1.isBlank()) {
            return Result.Failure(
                DomainError.ValidationError("Field1 cannot be blank")
            )
        }
        return Result.Success(Unit)
    }
    
    // Copy method for immutability
    private fun copy(
        id: {Aggregate}Id = this.id,
        field1: String = this.field1,
        field2: {ValueObject} = this.field2,
        status: {Aggregate}Status = this.status,
        createdAt: Instant = this.createdAt,
        updatedAt: Instant? = this.updatedAt
    ) = {Aggregate}(id, field1, field2, status, createdAt, updatedAt)
}
```

**Usage**: Replace `{Aggregate}`, `{ValueObject}`, and field names with your actual names.

---

## Template 2: Command + CommandHandler

```kotlin
// File: src/main/kotlin/com/example/application/commands/Create{Aggregate}Command.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.Command

data class Create{Aggregate}Command(
    val field1: String,
    val field2: String,
    val field3: Int
) : Command

// File: src/main/kotlin/com/example/application/commands/Create{Aggregate}CommandHandler.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import com.melsardes.libraries.structuskotlin.domain.Result
import com.melsardes.libraries.structuskotlin.domain.DomainError
import com.melsardes.libraries.structuskotlin.application.events.DomainEventPublisher
import com.example.domain.{aggregate}.*
import java.time.Instant
import java.util.UUID

class Create{Aggregate}CommandHandler(
    private val repository: {Aggregate}CommandRepository,
    private val eventPublisher: DomainEventPublisher
) : CommandHandler<Create{Aggregate}Command, {Aggregate}Id> {
    
    override suspend fun handle(command: Create{Aggregate}Command): Result<{Aggregate}Id> {
        // 1. Validate input
        if (command.field1.isBlank()) {
            return Result.Failure(
                DomainError.ValidationError("Field1 is required")
            )
        }
        
        // 2. Create domain object
        val id = {Aggregate}Id(UUID.randomUUID())
        val aggregate = {Aggregate}(
            id = id,
            field1 = command.field1,
            field2 = {ValueObject}(command.field2, command.field3),
            status = {Aggregate}.{Aggregate}Status.DRAFT,
            createdAt = Instant.now()
        )
        
        // 3. Validate domain rules
        return when (val validation = aggregate.validate()) {
            is Result.Failure -> validation
            is Result.Success -> {
                // 4. Persist
                when (val saveResult = repository.save(aggregate)) {
                    is Result.Success -> {
                        // 5. Publish event
                        val event = {Aggregate}CreatedEvent(
                            eventId = UUID.randomUUID().toString(),
                            occurredOn = Instant.now(),
                            aggregateId = id.value.toString(),
                            {aggregate}Id = id,
                            field1 = command.field1
                        )
                        eventPublisher.publish(event)
                        Result.Success(id)
                    }
                    is Result.Failure -> saveResult
                }
            }
        }
    }
}
```

---

## Template 3: Query + QueryHandler

```kotlin
// File: src/main/kotlin/com/example/application/queries/Get{Aggregate}ByIdQuery.kt
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.Query
import com.example.domain.{aggregate}.{Aggregate}Id

data class Get{Aggregate}ByIdQuery(
    val id: {Aggregate}Id
) : Query<{Aggregate}Dto>

// DTO for query result
data class {Aggregate}Dto(
    val id: String,
    val field1: String,
    val field2: String,
    val field3: Int,
    val status: String,
    val createdAt: String,
    val updatedAt: String?
)

// File: src/main/kotlin/com/example/application/queries/Get{Aggregate}ByIdQueryHandler.kt
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler
import com.melsardes.libraries.structuskotlin.domain.Result
import com.example.domain.{aggregate}.{Aggregate}QueryRepository

class Get{Aggregate}ByIdQueryHandler(
    private val repository: {Aggregate}QueryRepository
) : QueryHandler<Get{Aggregate}ByIdQuery, {Aggregate}Dto> {
    
    override suspend fun handle(query: Get{Aggregate}ByIdQuery): Result<{Aggregate}Dto> {
        return when (val result = repository.findById(query.id)) {
            is Result.Success -> {
                val aggregate = result.value
                Result.Success(
                    {Aggregate}Dto(
                        id = aggregate.id.value.toString(),
                        field1 = aggregate.field1,
                        field2 = aggregate.field2.field1,
                        field3 = aggregate.field2.field2,
                        status = aggregate.status.name,
                        createdAt = aggregate.createdAt.toString(),
                        updatedAt = aggregate.updatedAt?.toString()
                    )
                )
            }
            is Result.Failure -> result
        }
    }
}
```

---

## Template 4: Repository Interfaces

```kotlin
// File: src/main/kotlin/com/example/domain/{aggregate}/{Aggregate}Repository.kt
package com.example.domain.{aggregate}

import com.melsardes.libraries.structuskotlin.domain.CommandRepository
import com.melsardes.libraries.structuskotlin.domain.QueryRepository
import com.melsardes.libraries.structuskotlin.domain.Result

// Command Repository (Write operations)
interface {Aggregate}CommandRepository : CommandRepository<{Aggregate}, {Aggregate}Id> {
    suspend fun save(aggregate: {Aggregate}): Result<Unit>
    suspend fun update(aggregate: {Aggregate}): Result<Unit>
    suspend fun delete(id: {Aggregate}Id): Result<Unit>
    suspend fun existsByField(field: String): Result<Boolean>
}

// Query Repository (Read operations)
interface {Aggregate}QueryRepository : QueryRepository<{Aggregate}, {Aggregate}Id> {
    suspend fun findById(id: {Aggregate}Id): Result<{Aggregate}>
    suspend fun findAll(limit: Int = 100, offset: Int = 0): Result<List<{Aggregate}>>
    suspend fun findByStatus(status: {Aggregate}.{Aggregate}Status): Result<List<{Aggregate}>>
    suspend fun findByField(field: String): Result<List<{Aggregate}>>
    suspend fun count(): Result<Long>
}
```

---

## Template 5: Domain Events

```kotlin
// File: src/main/kotlin/com/example/domain/{aggregate}/{Aggregate}Events.kt
package com.example.domain.{aggregate}

import com.melsardes.libraries.structuskotlin.domain.events.DomainEvent
import java.time.Instant

data class {Aggregate}CreatedEvent(
    override val eventId: String,
    override val occurredOn: Instant,
    override val aggregateId: String,
    val {aggregate}Id: {Aggregate}Id,
    val field1: String
) : DomainEvent {
    override val eventType: String = "{Aggregate}Created"
}

data class {Aggregate}UpdatedEvent(
    override val eventId: String,
    override val occurredOn: Instant,
    override val aggregateId: String,
    val {aggregate}Id: {Aggregate}Id,
    val updatedFields: Map<String, Any>
) : DomainEvent {
    override val eventType: String = "{Aggregate}Updated"
}

data class {Aggregate}DeletedEvent(
    override val eventId: String,
    override val occurredOn: Instant,
    override val aggregateId: String,
    val {aggregate}Id: {Aggregate}Id
) : DomainEvent {
    override val eventType: String = "{Aggregate}Deleted"
}
```

---

## Template 6: Update Command + Handler

```kotlin
// File: src/main/kotlin/com/example/application/commands/Update{Aggregate}Command.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.Command
import com.example.domain.{aggregate}.{Aggregate}Id

data class Update{Aggregate}Command(
    val id: {Aggregate}Id,
    val field1: String?,
    val field2: String?,
    val field3: Int?
) : Command

// File: src/main/kotlin/com/example/application/commands/Update{Aggregate}CommandHandler.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import com.melsardes.libraries.structuskotlin.domain.Result
import com.melsardes.libraries.structuskotlin.domain.DomainError
import com.melsardes.libraries.structuskotlin.application.events.DomainEventPublisher
import com.example.domain.{aggregate}.*
import java.time.Instant
import java.util.UUID

class Update{Aggregate}CommandHandler(
    private val commandRepository: {Aggregate}CommandRepository,
    private val queryRepository: {Aggregate}QueryRepository,
    private val eventPublisher: DomainEventPublisher
) : CommandHandler<Update{Aggregate}Command, Unit> {
    
    override suspend fun handle(command: Update{Aggregate}Command): Result<Unit> {
        // 1. Fetch existing aggregate
        val existingResult = queryRepository.findById(command.id)
        if (existingResult is Result.Failure) {
            return Result.Failure(
                DomainError.EntityNotFound("{Aggregate} not found")
            )
        }
        
        val existing = (existingResult as Result.Success).value
        
        // 2. Apply updates
        val updated = existing.copy(
            field1 = command.field1 ?: existing.field1,
            field2 = if (command.field2 != null || command.field3 != null) {
                {ValueObject}(
                    field1 = command.field2 ?: existing.field2.field1,
                    field2 = command.field3 ?: existing.field2.field2
                )
            } else existing.field2,
            updatedAt = Instant.now()
        )
        
        // 3. Validate
        return when (val validation = updated.validate()) {
            is Result.Failure -> validation
            is Result.Success -> {
                // 4. Persist
                when (val updateResult = commandRepository.update(updated)) {
                    is Result.Success -> {
                        // 5. Publish event
                        val event = {Aggregate}UpdatedEvent(
                            eventId = UUID.randomUUID().toString(),
                            occurredOn = Instant.now(),
                            aggregateId = command.id.value.toString(),
                            {aggregate}Id = command.id,
                            updatedFields = buildMap {
                                command.field1?.let { put("field1", it) }
                                command.field2?.let { put("field2", it) }
                                command.field3?.let { put("field3", it) }
                            }
                        )
                        eventPublisher.publish(event)
                        Result.Success(Unit)
                    }
                    is Result.Failure -> updateResult
                }
            }
        }
    }
}
```

---

## Template 7: List Query with Pagination

```kotlin
// File: src/main/kotlin/com/example/application/queries/List{Aggregate}sQuery.kt
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.Query

data class List{Aggregate}sQuery(
    val status: String? = null,
    val limit: Int = 20,
    val offset: Int = 0
) : Query<{Aggregate}ListResult>

data class {Aggregate}ListResult(
    val items: List<{Aggregate}Dto>,
    val total: Long,
    val limit: Int,
    val offset: Int
)

// File: src/main/kotlin/com/example/application/queries/List{Aggregate}sQueryHandler.kt
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler
import com.melsardes.libraries.structuskotlin.domain.Result
import com.example.domain.{aggregate}.*

class List{Aggregate}sQueryHandler(
    private val repository: {Aggregate}QueryRepository
) : QueryHandler<List{Aggregate}sQuery, {Aggregate}ListResult> {
    
    override suspend fun handle(query: List{Aggregate}sQuery): Result<{Aggregate}ListResult> {
        // Get items
        val itemsResult = if (query.status != null) {
            val status = {Aggregate}.{Aggregate}Status.valueOf(query.status)
            repository.findByStatus(status)
        } else {
            repository.findAll(query.limit, query.offset)
        }
        
        // Get total count
        val countResult = repository.count()
        
        return when {
            itemsResult is Result.Failure -> itemsResult
            countResult is Result.Failure -> countResult
            else -> {
                val items = (itemsResult as Result.Success).value
                val total = (countResult as Result.Success).value
                
                Result.Success(
                    {Aggregate}ListResult(
                        items = items.map { it.toDto() },
                        total = total,
                        limit = query.limit,
                        offset = query.offset
                    )
                )
            }
        }
    }
    
    private fun {Aggregate}.toDto() = {Aggregate}Dto(
        id = id.value.toString(),
        field1 = field1,
        field2 = field2.field1,
        field3 = field2.field2,
        status = status.name,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt?.toString()
    )
}
```

---

## Template 8: Spring Boot Controller Integration

```kotlin
// File: src/main/kotlin/com/example/presentation/controllers/{Aggregate}Controller.kt
package com.example.presentation.controllers

import com.example.application.commands.*
import com.example.application.queries.*
import com.example.domain.{aggregate}.{Aggregate}Id
import com.melsardes.libraries.structuskotlin.domain.Result
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/{aggregates}")
class {Aggregate}Controller(
    private val createHandler: Create{Aggregate}CommandHandler,
    private val updateHandler: Update{Aggregate}CommandHandler,
    private val getByIdHandler: Get{Aggregate}ByIdQueryHandler,
    private val listHandler: List{Aggregate}sQueryHandler
) {
    
    @PostMapping
    suspend fun create(@RequestBody request: Create{Aggregate}Request): ResponseEntity<Any> {
        val command = Create{Aggregate}Command(
            field1 = request.field1,
            field2 = request.field2,
            field3 = request.field3
        )
        
        return when (val result = createHandler.handle(command)) {
            is Result.Success -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapOf("id" to result.value.value.toString()))
            is Result.Failure -> ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.error.message))
        }
    }
    
    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: String): ResponseEntity<Any> {
        val query = Get{Aggregate}ByIdQuery({Aggregate}Id(UUID.fromString(id)))
        
        return when (val result = getByIdHandler.handle(query)) {
            is Result.Success -> ResponseEntity.ok(result.value)
            is Result.Failure -> ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to result.error.message))
        }
    }
    
    @GetMapping
    suspend fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<Any> {
        val query = List{Aggregate}sQuery(status, limit, offset)
        
        return when (val result = listHandler.handle(query)) {
            is Result.Success -> ResponseEntity.ok(result.value)
            is Result.Failure -> ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to result.error.message))
        }
    }
    
    @PutMapping("/{id}")
    suspend fun update(
        @PathVariable id: String,
        @RequestBody request: Update{Aggregate}Request
    ): ResponseEntity<Any> {
        val command = Update{Aggregate}Command(
            id = {Aggregate}Id(UUID.fromString(id)),
            field1 = request.field1,
            field2 = request.field2,
            field3 = request.field3
        )
        
        return when (val result = updateHandler.handle(command)) {
            is Result.Success -> ResponseEntity.noContent().build()
            is Result.Failure -> ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.error.message))
        }
    }
}

// Request DTOs
data class Create{Aggregate}Request(
    val field1: String,
    val field2: String,
    val field3: Int
)

data class Update{Aggregate}Request(
    val field1: String?,
    val field2: String?,
    val field3: Int?
)
```

---

## Quick Reference: File Locations

| Component | Package | File Location |
|-----------|---------|---------------|
| Aggregate | `domain.{aggregate}` | `domain/{aggregate}/{Aggregate}.kt` |
| Value Objects | `domain.{aggregate}` | `domain/{aggregate}/{Aggregate}.kt` |
| Repository Interfaces | `domain.{aggregate}` | `domain/{aggregate}/{Aggregate}Repository.kt` |
| Domain Events | `domain.{aggregate}` | `domain/{aggregate}/{Aggregate}Events.kt` |
| Commands | `application.commands` | `application/commands/Create{Aggregate}Command.kt` |
| Command Handlers | `application.commands` | `application/commands/Create{Aggregate}CommandHandler.kt` |
| Queries | `application.queries` | `application/queries/Get{Aggregate}ByIdQuery.kt` |
| Query Handlers | `application.queries` | `application/queries/Get{Aggregate}ByIdQueryHandler.kt` |
| Controllers | `presentation.controllers` | `presentation/controllers/{Aggregate}Controller.kt` |
| Repository Impls | `infrastructure.persistence` | `infrastructure/persistence/{Aggregate}RepositoryImpl.kt` |

---

**Remember**: These are templates. Always customize them to fit your specific domain and requirements!
