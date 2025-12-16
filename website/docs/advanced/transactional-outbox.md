---
sidebar_position: 2
---

# Transactional Outbox Pattern

Learn how to reliably publish domain events using the Transactional Outbox Pattern.

## The Problem

When you need to update a database and publish events, you face a distributed transaction problem:

```kotlin
// ❌ Unreliable approach
suspend fun createOrder(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        val order = Order.create(command.customerId)
        orderRepository.save(order)  // Database operation
        
        // What if this fails? Event is lost!
        eventPublisher.publish(order.domainEvents)  // External system
        
        order.id
    }
}
```

**Problems:**
- If event publishing fails, database changes are committed but events are lost
- If database fails after publishing, events are sent but data is inconsistent
- No atomicity between database and message broker

## The Solution

The **Transactional Outbox Pattern** solves this by:

1. Saving events to a database table in the same transaction as the aggregate
2. Publishing events from the outbox table in a separate process
3. Marking events as published after successful delivery

```
┌─────────────────────────────────────────┐
│         Command Handler                 │
│  1. Save Aggregate                      │
│  2. Save Events to Outbox (same txn)   │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Outbox Publisher                │
│  1. Read unpublished events             │
│  2. Publish to message broker           │
│  3. Mark as published                   │
└─────────────────────────────────────────┘
```

## Implementation

### 1. Define Outbox Repository

```kotlin
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository
import com.melsardes.libraries.structuskotlin.domain.OutboxMessage

interface MessageOutboxRepository : Repository {
    suspend fun save(event: DomainEvent)
    suspend fun findUnpublished(limit: Int): List<OutboxMessage>
    suspend fun markAsPublished(messageId: String)
    suspend fun incrementRetryCount(messageId: String)
    suspend fun deletePublishedOlderThan(olderThanDays: Int): Int
    suspend fun findFailedEvents(maxRetries: Int): List<OutboxMessage>
}
```

### 2. Create Outbox Table

```sql
CREATE TABLE message_outbox (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_unpublished 
ON message_outbox(published_at) 
WHERE published_at IS NULL;

CREATE INDEX idx_outbox_aggregate 
ON message_outbox(aggregate_type, aggregate_id);
```

### 3. Implement Outbox Repository

```kotlin
class OutboxRepositoryImpl(
    private val database: Database
) : MessageOutboxRepository {
    
    override suspend fun save(event: DomainEvent) {
        val message = OutboxMessage.from(event)
        database.execute("""
            INSERT INTO message_outbox (
                id, event_id, event_type, aggregate_type, 
                aggregate_id, payload, occurred_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
            message.id,
            message.eventId,
            message.eventType,
            message.aggregateType,
            message.aggregateId,
            serializeToJson(event),
            message.occurredAt
        )
    }
    
    override suspend fun findUnpublished(limit: Int): List<OutboxMessage> {
        return database.query("""
            SELECT * FROM message_outbox
            WHERE published_at IS NULL
            ORDER BY occurred_at ASC
            LIMIT ?
        """, limit).map { mapToOutboxMessage(it) }
    }
    
    override suspend fun markAsPublished(messageId: String) {
        database.execute("""
            UPDATE message_outbox
            SET published_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """, messageId)
    }
    
    override suspend fun incrementRetryCount(messageId: String) {
        database.execute("""
            UPDATE message_outbox
            SET retry_count = retry_count + 1
            WHERE id = ?
        """, messageId)
    }
}
```

### 4. Use in Command Handler

```kotlin
class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository,
    private val database: Database
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(
        command: CreateOrderCommand
    ): Result<OrderId> {
        return runCatching {
            database.transaction {
                // 1. Create and save aggregate
                val order = Order.create(command.customerId, command.items)
                orderRepository.save(order)
                
                // 2. Save events to outbox (same transaction!)
                order.domainEvents.forEach { event ->
                    outboxRepository.save(event)
                }
                
                // 3. Clear events from aggregate
                order.clearEvents()
                
                order.id
            }
        }
    }
}
```

### 5. Implement Outbox Publisher

```kotlin
class OutboxPublisher(
    private val outboxRepository: MessageOutboxRepository,
    private val eventPublisher: DomainEventPublisher,
    private val maxRetries: Int = 3
) {
    
    suspend fun publishPendingEvents() {
        val messages = outboxRepository.findUnpublished(limit = 100)
        
        messages.forEach { message ->
            try {
                // Publish to external system
                val event = deserializeEvent(message.payload, message.eventType)
                eventPublisher.publish(event)
                
                // Mark as published
                outboxRepository.markAsPublished(message.id)
                
                logger.info("Published event: ${message.eventType} (${message.id})")
                
            } catch (e: Exception) {
                logger.error("Failed to publish event: ${message.id}", e)
                
                // Increment retry count
                outboxRepository.incrementRetryCount(message.id)
                
                // Check if max retries exceeded
                if (message.retryCount + 1 >= maxRetries) {
                    logger.error("Max retries exceeded for event: ${message.id}")
                    // Could move to dead letter queue here
                }
            }
        }
    }
    
    suspend fun cleanupOldEvents(olderThanDays: Int = 30) {
        val deleted = outboxRepository.deletePublishedOlderThan(olderThanDays)
        logger.info("Cleaned up $deleted old published events")
    }
}
```

### 6. Schedule Publisher

```kotlin
// Using Kotlin coroutines
class OutboxScheduler(
    private val outboxPublisher: OutboxPublisher
) {
    
    fun start(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                try {
                    outboxPublisher.publishPendingEvents()
                } catch (e: Exception) {
                    logger.error("Error in outbox publisher", e)
                }
                delay(5000) // Poll every 5 seconds
            }
        }
        
        // Cleanup job
        scope.launch {
            while (isActive) {
                try {
                    outboxPublisher.cleanupOldEvents()
                } catch (e: Exception) {
                    logger.error("Error cleaning up events", e)
                }
                delay(24.hours) // Daily cleanup
            }
        }
    }
}

// In your application startup
fun main() {
    val scope = CoroutineScope(Dispatchers.Default)
    val scheduler = OutboxScheduler(outboxPublisher)
    scheduler.start(scope)
}
```

## Advanced Features

### Dead Letter Queue

Handle events that fail repeatedly:

```kotlin
suspend fun handleFailedEvents() {
    val failedEvents = outboxRepository.findFailedEvents(maxRetries = 3)
    
    failedEvents.forEach { message ->
        // Move to dead letter queue
        deadLetterRepository.save(message)
        
        // Delete from outbox
        outboxRepository.delete(message.id)
        
        // Alert monitoring system
        alerting.sendAlert("Failed event: ${message.eventType}")
    }
}
```

### Event Ordering

Ensure events are published in order per aggregate:

```kotlin
override suspend fun findUnpublished(limit: Int): List<OutboxMessage> {
    return database.query("""
        SELECT * FROM message_outbox
        WHERE published_at IS NULL
        ORDER BY aggregate_id, occurred_at ASC
        LIMIT ?
    """, limit).map { mapToOutboxMessage(it) }
}
```

### Idempotent Publishing

Make event publishing idempotent:

```kotlin
data class OrderCreatedEvent(
    override val eventId: String,  // Use as idempotency key
    override val aggregateId: String,
    val customerId: String
) : BaseDomainEvent(...)

// In event consumer
suspend fun handleOrderCreated(event: OrderCreatedEvent) {
    if (processedEvents.contains(event.eventId)) {
        logger.info("Event already processed: ${event.eventId}")
        return
    }
    
    // Process event
    processOrder(event)
    
    // Mark as processed
    processedEvents.add(event.eventId)
}
```

## Benefits

✅ **Atomicity**: Events and data changes are atomic
✅ **Reliability**: Events are never lost
✅ **Consistency**: Database and events stay in sync
✅ **Retry Logic**: Failed publishes are retried
✅ **Monitoring**: Track unpublished events

## Trade-offs

⚠️ **Eventual Consistency**: Events are published asynchronously
⚠️ **Complexity**: Additional infrastructure needed
⚠️ **Storage**: Outbox table grows over time
⚠️ **Polling**: Publisher polls the database

## Best Practices

### ✅ Do

- Use transactions for aggregate + outbox saves
- Clear events after saving to outbox
- Implement retry logic with backoff
- Monitor unpublished event count
- Clean up old published events
- Handle failed events (dead letter queue)

### ❌ Don't

- Publish events directly from command handlers
- Forget to clear events from aggregates
- Skip error handling in publisher
- Let outbox table grow indefinitely
- Ignore failed events

## Testing

### Test Command Handler

```kotlin
@Test
fun `should save events to outbox`() = runTest {
    val command = CreateOrderCommand("CUST-1", items)
    val handler = CreateOrderCommandHandler(mockRepo, mockOutbox, mockDb)
    
    val result = handler(command)
    
    assertTrue(result.isSuccess)
    verify(mockOutbox).save(any<OrderCreatedEvent>())
}
```

### Test Outbox Publisher

```kotlin
@Test
fun `should publish unpublished events`() = runTest {
    val message = OutboxMessage(...)
    whenever(mockOutbox.findUnpublished(100)).thenReturn(listOf(message))
    
    outboxPublisher.publishPendingEvents()
    
    verify(mockEventPublisher).publish(any())
    verify(mockOutbox).markAsPublished(message.id)
}
```

## Next Steps

- **[Event-Driven Architecture](event-driven)** - Building with events
- **[Error Handling](error-handling)** - Handling failures
- **[Testing Strategies](../best-practices/testing-strategies)** - Testing patterns
