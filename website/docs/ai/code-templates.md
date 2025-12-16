---
sidebar_position: 4
---

# Code Templates

Ready-to-use code templates for common tasks. Replace `{Aggregate}` with your entity name.

## Template 1: Aggregate Root

```kotlin
// Domain Layer
package com.example.domain.{aggregate}

import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import com.melsardes.libraries.structuskotlin.domain.ValueObject
import java.util.UUID

// Value Objects
data class {Aggregate}Id(val value: UUID) : ValueObject

data class {ValueObject}(
    val field1: String,
    val field2: Int
) : ValueObject

// Aggregate Root
class {Aggregate}(
    override val id: {Aggregate}Id,
    val field1: String,
    val field2: {ValueObject},
    val status: {Aggregate}Status
) : AggregateRoot<{Aggregate}Id>() {
    
    enum class {Aggregate}Status {
        DRAFT, ACTIVE, INACTIVE
    }
    
    fun activate() {
        require(status == {Aggregate}Status.DRAFT) {
            "Can only activate draft {aggregate}"
        }
        recordEvent({Aggregate}ActivatedEvent(
            aggregateId = id.value.toString(),
            {aggregate}Id = id
        ))
    }
    
    companion object {
        fun create(field1: String, field2: {ValueObject}): {Aggregate} {
            val aggregate = {Aggregate}(
                id = {Aggregate}Id(UUID.randomUUID()),
                field1 = field1,
                field2 = field2,
                status = {Aggregate}Status.DRAFT
            )
            aggregate.recordEvent({Aggregate}CreatedEvent(
                aggregateId = aggregate.id.value.toString(),
                {aggregate}Id = aggregate.id,
                field1 = field1
            ))
            return aggregate
        }
    }
}
```

## Template 2: Command + Handler

```kotlin
// Application Layer
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.Command
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler

data class Create{Aggregate}Command(
    val field1: String,
    val field2: String,
    val field3: Int
) : Command

class Create{Aggregate}CommandHandler(
    private val repository: {Aggregate}Repository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<Create{Aggregate}Command, Result<{Aggregate}Id>> {
    
    override suspend operator fun invoke(
        command: Create{Aggregate}Command
    ): Result<{Aggregate}Id> {
        return runCatching {
            // 1. Create aggregate
            val aggregate = {Aggregate}.create(
                field1 = command.field1,
                field2 = {ValueObject}(command.field2, command.field3)
            )
            
            // 2. Save aggregate
            repository.save(aggregate)
            
            // 3. Save events to outbox
            aggregate.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // 4. Clear events
            aggregate.clearEvents()
            
            aggregate.id
        }
    }
}
```

## Template 3: Query + Handler

```kotlin
// Application Layer
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.Query
import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler

data class Get{Aggregate}ByIdQuery(
    val id: String
) : Query

data class {Aggregate}Dto(
    val id: String,
    val field1: String,
    val field2: String,
    val status: String
)

class Get{Aggregate}ByIdQueryHandler(
    private val repository: {Aggregate}Repository
) : QueryHandler<Get{Aggregate}ByIdQuery, {Aggregate}Dto?> {
    
    override suspend operator fun invoke(
        query: Get{Aggregate}ByIdQuery
    ): {Aggregate}Dto? {
        val aggregate = repository.findById({Aggregate}Id(UUID.fromString(query.id)))
        return aggregate?.let {
            {Aggregate}Dto(
                id = it.id.value.toString(),
                field1 = it.field1,
                field2 = it.field2.field1,
                status = it.status.name
            )
        }
    }
}
```

## Template 4: Repository Interface

```kotlin
// Domain Layer
package com.example.domain.{aggregate}

import com.melsardes.libraries.structuskotlin.domain.Repository

interface {Aggregate}Repository : Repository {
    suspend fun findById(id: {Aggregate}Id): {Aggregate}?
    suspend fun save(aggregate: {Aggregate})
    suspend fun delete(id: {Aggregate}Id)
    suspend fun findByStatus(status: {Aggregate}.{Aggregate}Status): List<{Aggregate}>
}
```

## Template 5: Domain Events

```kotlin
// Domain Layer
package com.example.domain.{aggregate}

import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent

data class {Aggregate}CreatedEvent(
    override val aggregateId: String,
    val {aggregate}Id: {Aggregate}Id,
    val field1: String
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "{Aggregate}",
    eventVersion = 1
)

data class {Aggregate}ActivatedEvent(
    override val aggregateId: String,
    val {aggregate}Id: {Aggregate}Id
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "{Aggregate}",
    eventVersion = 1
)
```

## Template 6: Repository Implementation

```kotlin
// Infrastructure Layer
package com.example.infrastructure.persistence

import com.example.domain.{aggregate}.*

class {Aggregate}RepositoryImpl(
    private val database: Database
) : {Aggregate}Repository {
    
    override suspend fun findById(id: {Aggregate}Id): {Aggregate}? {
        return database.query(
            "SELECT * FROM {aggregates} WHERE id = ?",
            id.value
        )?.let { mapToDomain(it) }
    }
    
    override suspend fun save(aggregate: {Aggregate}) {
        database.execute("""
            INSERT INTO {aggregates} (id, field1, field2_field1, field2_field2, status)
            VALUES (?, ?, ?, ?, ?)
        """,
            aggregate.id.value,
            aggregate.field1,
            aggregate.field2.field1,
            aggregate.field2.field2,
            aggregate.status.name
        )
    }
    
    override suspend fun delete(id: {Aggregate}Id) {
        database.execute(
            "DELETE FROM {aggregates} WHERE id = ?",
            id.value
        )
    }
    
    override suspend fun findByStatus(
        status: {Aggregate}.{Aggregate}Status
    ): List<{Aggregate}> {
        return database.query(
            "SELECT * FROM {aggregates} WHERE status = ?",
            status.name
        ).map { mapToDomain(it) }
    }
    
    private fun mapToDomain(row: DatabaseRow): {Aggregate} {
        return {Aggregate}(
            id = {Aggregate}Id(UUID.fromString(row.getString("id"))),
            field1 = row.getString("field1"),
            field2 = {ValueObject}(
                field1 = row.getString("field2_field1"),
                field2 = row.getInt("field2_field2")
            ),
            status = {Aggregate}.{Aggregate}Status.valueOf(row.getString("status"))
        )
    }
}
```

## Quick Reference

| Template | Use When |
|----------|----------|
| Aggregate Root | Creating new entity with identity |
| Command + Handler | Implementing write operation |
| Query + Handler | Implementing read operation |
| Repository Interface | Defining persistence contract |
| Domain Events | Notifying of state changes |
| Repository Implementation | Implementing persistence |

## Usage Instructions

1. **Replace placeholders**: `{Aggregate}`, `{aggregate}`, `{ValueObject}`
2. **Customize fields**: Add/remove fields as needed
3. **Add business logic**: Implement domain methods
4. **Follow conventions**: Keep naming consistent

## 📚 Related

- **[Usage Patterns](usage-patterns)** - Implementation patterns
- **[AI Prompts](prompts)** - Prompt templates
- **[Library Overview](library-overview)** - Core concepts

---

**Tip**: Start with the Aggregate Root template and build from there!
