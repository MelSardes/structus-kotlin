# Transactional Outbox Pattern

The Transactional Outbox Pattern solves the **dual-write problem** - ensuring atomic writes to both a database and a message broker.

## 🎯 The Problem: Dual-Write

When you need to:
1. Save data to a database
2. Publish an event to a message broker (Kafka, RabbitMQ, etc.)

You face a consistency challenge:

### ❌ Naive Approach (Broken)

```kotlin
suspend fun createOrder(command: CreateOrderCommand): OrderId {
    // Write #1: Database
    val order = Order.create(command.items)
    orderRepository.save(order)
    
    // Write #2: Message Broker
    eventPublisher.publish(OrderCreatedEvent(order.id))
    
    return order.id
}
```

**Problems**:
- Database succeeds, message broker fails → **Lost event**
- Message broker succeeds, database fails → **Phantom event**
- No atomicity between two systems

## ✅ The Solution: Transactional Outbox

Store events in the **same database** as your aggregate, then publish them asynchronously.

```
┌─────────────────────────────────────────────────┐
│         Single Database Transaction             │
│                                                 │
│  1. Save Aggregate → orders table              │
│  2. Save Events → outbox_messages table        │
│                                                 │
└─────────────────────────────────────────────────┘
                    ↓
         Background Process (separate)
                    ↓
         3. Read unpublished events
         4. Publish to message broker
         5. Mark as published
```

## 🏗️ Implementation

### Step 1: Define the Outbox Repository

The library provides `MessageOutboxRepository`:

```kotlin
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository
import com.melsardes.libraries.structuskotlin.domain.OutboxMessage
import com.melsardes.libraries.structuskotlin.domain.events.DomainEvent

interface MessageOutboxRepository : Repository {
    // Save event to outbox
    suspend fun save(event: DomainEvent)
    
    // Find unpublished events
    suspend fun findUnpublished(limit: Int): List<OutboxMessage>
    
    // Mark event as published
    suspend fun markAsPublished(messageId: String)
    
    // Increment retry count on failure
    suspend fun incrementRetryCount(messageId: String)
    
    // Cleanup old published events
    suspend fun deletePublishedOlderThan(olderThanDays: Int): Int
    
    // Find events that exceeded retry limit
    suspend fun findFailedEvents(maxRetries: Int): List<OutboxMessage>
}
```

### Step 2: Create Database Schema

```sql
CREATE TABLE outbox_messages (
    id VARCHAR(255) PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_unpublished (published_at, created_at),
    INDEX idx_aggregate (aggregate_id, aggregate_type)
);
```

### Step 3: Implement the Repository

```kotlin
class PostgresOutboxRepository(
    private val database: Database
) : MessageOutboxRepository {
    
    override suspend fun save(event: DomainEvent) {
        val message = OutboxMessage.from(event)
        
        database.execute(
            """
            INSERT INTO outbox_messages 
            (id, event_id, event_type, aggregate_id, aggregate_type, 
             payload, occurred_at, retry_count)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, 0)
            """,
            message.id,
            message.eventId,
            message.eventType,
            message.aggregateId,
            message.aggregateType,
            serializeEvent(event),
            message.occurredAt
        )
    }
    
    override suspend fun findUnpublished(limit: Int): List<OutboxMessage> {
        return database.query(
            """
            SELECT * FROM outbox_messages 
            WHERE published_at IS NULL 
            ORDER BY created_at ASC 
            LIMIT ?
            """,
            limit
        ).map { mapToOutboxMessage(it) }
    }
    
    override suspend fun markAsPublished(messageId: String) {
        database.execute(
            """
            UPDATE outbox_messages 
            SET published_at = NOW() 
            WHERE id = ?
            """,
            messageId
        )
    }
    
    override suspend fun incrementRetryCount(messageId: String) {
        database.execute(
            """
            UPDATE outbox_messages 
            SET retry_count = retry_count + 1 
            WHERE id = ?
            """,
            messageId
        )
    }
    
    override suspend fun deletePublishedOlderThan(olderThanDays: Int): Int {
        return database.execute(
            """
            DELETE FROM outbox_messages 
            WHERE published_at IS NOT NULL 
            AND published_at < NOW() - INTERVAL '$olderThanDays days'
            """
        )
    }
    
    override suspend fun findFailedEvents(maxRetries: Int): List<OutboxMessage> {
        return database.query(
            """
            SELECT * FROM outbox_messages 
            WHERE published_at IS NULL 
            AND retry_count >= ?
            """,
            maxRetries
        ).map { mapToOutboxMessage(it) }
    }
}
```

### Step 4: Use in Command Handlers

```kotlin
class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        return runCatching {
            // Start transaction
            withTransaction {
                // 1. Execute domain logic
                val order = Order.create(
                    customerId = command.customerId,
                    items = command.items
                )
                
                // 2. Save aggregate (database write #1)
                orderRepository.save(order)
                
                // 3. Save events to outbox (database write #2)
                // BOTH writes in the SAME transaction!
                order.domainEvents.forEach { event ->
                    outboxRepository.save(event)
                }
                
                // 4. Clear events from aggregate
                order.clearEvents()
                
                order.id
            }
        }
    }
}
```

### Step 5: Create Outbox Publisher

A **separate background process** publishes events from the outbox:

```kotlin
import com.melsardes.libraries.structuskotlin.application.events.DomainEventPublisher
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

class OutboxPublisher(
    private val outboxRepository: MessageOutboxRepository,
    private val eventPublisher: DomainEventPublisher,
    private val maxRetries: Int = 5,
    private val batchSize: Int = 100
) {
    
    private var job: Job? = null
    
    fun start(scope: CoroutineScope) {
        job = scope.launch {
            while (isActive) {
                try {
                    publishPendingEvents()
                } catch (e: Exception) {
                    // Log error
                    println("Error publishing events: ${e.message}")
                }
                
                delay(5.seconds)  // Poll every 5 seconds
            }
        }
    }
    
    fun stop() {
        job?.cancel()
    }
    
    suspend fun publishPendingEvents() {
        val messages = outboxRepository.findUnpublished(limit = batchSize)
        
        messages.forEach { message ->
            try {
                // Publish to message broker
                eventPublisher.publish(message.event)
                
                // Mark as published
                outboxRepository.markAsPublished(message.id)
                
                println("✅ Published event: ${message.eventType} (${message.id})")
            } catch (e: Exception) {
                // Increment retry count
                outboxRepository.incrementRetryCount(message.id)
                
                println("❌ Failed to publish event: ${message.eventType} (${message.id})")
                
                // Check if exceeded max retries
                if (message.hasExceededRetries(maxRetries)) {
                    println("⚠️ Event exceeded max retries: ${message.id}")
                    // Could move to dead letter queue or alert
                }
            }
        }
    }
    
    suspend fun cleanupOldEvents(olderThanDays: Int = 7) {
        val deleted = outboxRepository.deletePublishedOlderThan(olderThanDays)
        println("🧹 Cleaned up $deleted old published events")
    }
}
```

### Step 6: Wire Everything Together

```kotlin
// In your application startup
fun main() = runBlocking {
    // Setup repositories
    val database = Database.connect("jdbc:postgresql://localhost/mydb")
    val orderRepository = PostgresOrderRepository(database)
    val outboxRepository = PostgresOutboxRepository(database)
    
    // Setup event publisher (Kafka, RabbitMQ, etc.)
    val eventPublisher = KafkaDomainEventPublisher(kafkaProducer)
    
    // Start outbox publisher
    val outboxPublisher = OutboxPublisher(
        outboxRepository = outboxRepository,
        eventPublisher = eventPublisher,
        maxRetries = 5,
        batchSize = 100
    )
    outboxPublisher.start(this)
    
    // Schedule cleanup job (daily)
    launch {
        while (isActive) {
            delay(24.hours)
            outboxPublisher.cleanupOldEvents(olderThanDays = 7)
        }
    }
    
    // Your application runs...
    
    // Shutdown
    outboxPublisher.stop()
}
```

## 🎯 Benefits

### ✅ Atomicity
- Both database writes happen in a single transaction
- Either both succeed or both fail
- No lost or phantom events

### ✅ Reliability
- Events are persisted before publishing
- Automatic retry on failure
- Dead letter queue for failed events

### ✅ Ordering
- Events published in the order they were created
- Per-aggregate ordering guaranteed

### ✅ At-Least-Once Delivery
- Events will be published at least once
- Consumers should be idempotent

## ⚠️ Considerations

### Idempotency

Consumers must handle duplicate events:

```kotlin
class OrderCreatedEventHandler : DomainEventHandler<OrderCreatedEvent> {
    
    override suspend fun handle(event: OrderCreatedEvent) {
        // Check if already processed
        if (processedEvents.contains(event.eventId)) {
            println("Event already processed: ${event.eventId}")
            return
        }
        
        // Process event
        sendConfirmationEmail(event.customerId, event.orderId)
        
        // Mark as processed
        processedEvents.add(event.eventId)
    }
}
```

### Polling Interval

Balance between latency and database load:
- **Fast polling** (1-5 seconds): Lower latency, higher load
- **Slow polling** (30-60 seconds): Higher latency, lower load

### Cleanup Strategy

Remove old published events to prevent table growth:
- Daily cleanup of events older than 7 days
- Or use database partitioning by date

### Monitoring

Track key metrics:
- Unpublished event count
- Publish latency
- Retry count
- Failed events

## 🔄 Alternative: Change Data Capture (CDC)

For high-throughput systems, consider CDC tools like Debezium:

```
Database → CDC (Debezium) → Kafka → Consumers
```

**Pros**:
- No polling overhead
- Real-time event streaming
- No application code changes

**Cons**:
- Additional infrastructure
- More complex setup
- Database-specific

## 📚 Further Reading

- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Event-Driven Architecture](Event-Sourcing.md)
- [Domain Events](Domain-Events.md)
- [CQRS Implementation](CQRS-Implementation.md)

## 🚀 Next Steps

- Implement the outbox repository for your database
- Create the background publisher
- Add monitoring and alerting
- Test failure scenarios
