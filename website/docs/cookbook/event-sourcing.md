---
sidebar_position: 2
---

# Event Sourcing with Structus

This guide shows you how to implement event sourcing using Structus.

## What is Event Sourcing?

Event sourcing is an architectural pattern where the state of your application is determined by a sequence of events. Instead of storing the current state, you store the history of actions that led to that state.

## Basic Event Sourcing Implementation

### 1. Define Your Events

First, define the events that can occur in your domain:

```kotlin
sealed class AccountEvent : DomainEvent {
    abstract val accountId: String
    
    data class AccountCreatedEvent(
        override val accountId: String,
        val customerName: String,
        val initialBalance: BigDecimal,
        override val occurredAt: Instant = Instant.now()
    ) : AccountEvent()
    
    data class MoneyDepositedEvent(
        override val accountId: String,
        val amount: BigDecimal,
        override val occurredAt: Instant = Instant.now()
    ) : AccountEvent()
    
    data class MoneyWithdrawnEvent(
        override val accountId: String,
        val amount: BigDecimal,
        override val occurredAt: Instant = Instant.now()
    ) : AccountEvent()
}
```

### 2. Create an Event-Sourced Aggregate

```kotlin
class Account private constructor(
    override val id: AccountId
) : AggregateRoot<AccountId>() {
    private var customerName: String = ""
    private var balance: BigDecimal = BigDecimal.ZERO
    private var isActive: Boolean = true
    
    // Apply methods to update state based on events
    fun apply(event: AccountEvent): Account {
        when (event) {
            is AccountEvent.AccountCreatedEvent -> {
                customerName = event.customerName
                balance = event.initialBalance
            }
            is AccountEvent.MoneyDepositedEvent -> {
                balance += event.amount
            }
            is AccountEvent.MoneyWithdrawnEvent -> {
                require(balance >= event.amount) { "Insufficient funds" }
                balance -= event.amount
            }
        }
        return this
    }
    
    // Command methods that generate events
    fun deposit(amount: BigDecimal) {
        require(isActive) { "Cannot deposit to inactive account" }
        require(amount > BigDecimal.ZERO) { "Deposit amount must be positive" }
        
        val event = AccountEvent.MoneyDepositedEvent(id.value, amount)
        apply(event)
        recordEvent(event)
    }
    
    fun withdraw(amount: BigDecimal) {
        require(isActive) { "Cannot withdraw from inactive account" }
        require(amount > BigDecimal.ZERO) { "Withdrawal amount must be positive" }
        require(balance >= amount) { "Insufficient funds" }
        
        val event = AccountEvent.MoneyWithdrawnEvent(id.value, amount)
        apply(event)
        recordEvent(event)
    }
    
    companion object {
        fun create(id: AccountId, customerName: String, initialBalance: BigDecimal = BigDecimal.ZERO): Account {
            require(initialBalance >= BigDecimal.ZERO) { "Initial balance cannot be negative" }
            
            val account = Account(id)
            val event = AccountEvent.AccountCreatedEvent(id.value, customerName, initialBalance)
            account.apply(event)
            account.recordEvent(event)
            return account
        }
        
        // Reconstruct account from event history
        fun fromEvents(id: AccountId, events: List<AccountEvent>): Account {
            val account = Account(id)
            events.forEach { event -> account.apply(event) }
            return account
        }
    }
}
```

### 3. Implement Event Storage Repository

```kotlin
interface EventStore {
    suspend fun saveEvents(aggregateId: String, events: List<DomainEvent>, expectedVersion: Int)
    suspend fun getEvents(aggregateId: String): List<DomainEvent>
}

class EventStoreAccountRepository(
    private val eventStore: EventStore
) : AccountRepository {
    
    override suspend fun save(account: Account): Result<Unit> {
        return runCatching {
            val events = account.domainEvents
            eventStore.saveEvents(account.id.value, events, account.version)
            account.clearEvents()
        }
    }
    
    override suspend fun findById(id: AccountId): Result<Account?> {
        return runCatching {
            val events = eventStore.getEvents(id.value)
            if (events.isEmpty()) {
                return Result.success(null)
            }
            
            val accountEvents = events.filterIsInstance<AccountEvent>()
            val account = Account.fromEvents(id, accountEvents)
            Result.success(account)
        }
    }
}
```

## Database Implementation

Here's how you might implement the event store with a PostgreSQL database:

```kotlin
class PostgresEventStore(private val database: Database) : EventStore {
    
    override suspend fun saveEvents(
        aggregateId: String,
        events: List<DomainEvent>,
        expectedVersion: Int
    ) {
        database.transaction {
            // Check expected version
            val currentVersion = getCurrentVersion(aggregateId)
            if (currentVersion != expectedVersion) {
                throw ConcurrencyException(
                    "Expected version $expectedVersion but got $currentVersion"
                )
            }
            
            // Save each event
            events.forEachIndexed { index, event ->
                val version = expectedVersion + index + 1
                insertEvent(aggregateId, event, version)
            }
        }
    }
    
    override suspend fun getEvents(aggregateId: String): List<DomainEvent> {
        return database.query("""
            SELECT event_type, event_data, version
            FROM event_store
            WHERE aggregate_id = ?
            ORDER BY version ASC
        """, aggregateId) { rows ->
            rows.map { row ->
                val eventType = row.getString("event_type")
                val eventData = row.getString("event_data")
                deserializeEvent(eventType, eventData)
            }
        }
    }
    
    private suspend fun getCurrentVersion(aggregateId: String): Int {
        return database.queryForObject("""
            SELECT COALESCE(MAX(version), 0) as version
            FROM event_store
            WHERE aggregate_id = ?
        """, aggregateId) { row ->
            row.getInt("version")
        } ?: 0
    }
    
    private suspend fun insertEvent(
        aggregateId: String,
        event: DomainEvent,
        version: Int
    ) {
        val eventType = event.javaClass.name
        val eventData = serializeEvent(event)
        
        database.execute("""
            INSERT INTO event_store (
                event_id, 
                aggregate_id, 
                event_type, 
                event_data, 
                version,
                timestamp
            ) VALUES (?, ?, ?, ?, ?, ?)
        """, UUID.randomUUID().toString(), aggregateId, eventType, eventData, version, Instant.now())
    }
    
    private fun serializeEvent(event: DomainEvent): String {
        // Use a JSON serializer (Jackson, Kotlinx.serialization, etc.)
        return objectMapper.writeValueAsString(event)
    }
    
    private fun deserializeEvent(eventType: String, eventData: String): DomainEvent {
        val eventClass = Class.forName(eventType)
        return objectMapper.readValue(eventData, eventClass) as DomainEvent
    }
}
```

## Event Projections

Projections transform the event stream into a queryable model:

```kotlin
class AccountBalanceProjection(private val database: Database) {
    
    suspend fun handleEvent(event: DomainEvent) {
        when (event) {
            is AccountEvent.AccountCreatedEvent -> {
                database.execute("""
                    INSERT INTO account_balances (
                        account_id, 
                        customer_name, 
                        balance, 
                        last_updated
                    ) VALUES (?, ?, ?, ?)
                """, event.accountId, event.customerName, event.initialBalance, event.occurredAt)
            }
            is AccountEvent.MoneyDepositedEvent -> {
                database.execute("""
                    UPDATE account_balances
                    SET balance = balance + ?, last_updated = ?
                    WHERE account_id = ?
                """, event.amount, event.occurredAt, event.accountId)
            }
            is AccountEvent.MoneyWithdrawnEvent -> {
                database.execute("""
                    UPDATE account_balances
                    SET balance = balance - ?, last_updated = ?
                    WHERE account_id = ?
                """, event.amount, event.occurredAt, event.accountId)
            }
        }
    }
}
```

## Event Processor

To process events and update projections:

```kotlin
class EventProcessor(
    private val eventStore: EventStore,
    private val projections: List<Any>,
    private val database: Database
) {
    
    suspend fun processEvents() {
        val lastProcessedId = getLastProcessedId()
        val newEvents = getNewEvents(lastProcessedId)
        
        database.transaction {
            newEvents.forEach { event ->
                projections.forEach { projection ->
                    callHandleMethod(projection, event)
                }
                updateLastProcessedId(event.id)
            }
        }
    }
    
    private fun callHandleMethod(projection: Any, event: DomainEvent) {
        val method = projection.javaClass.methods.find {
            it.name == "handleEvent" && it.parameterCount == 1 &&
            it.parameterTypes[0].isAssignableFrom(event.javaClass)
        }
        
        method?.invoke(projection, event)
    }
    
    // Other helper methods
}
```

## Best Practices

1. **Version your events**: As your system evolves, your events will change. Include version numbers in your events.

2. **Keep events immutable**: Once stored, events should never change.

3. **Consider snapshot aggregates**: For aggregates with many events, periodically save snapshots to improve loading performance.

4. **Use event streams for auditing**: Event sourcing naturally provides a complete audit log of all changes.

5. **Separate event definitions from storage details**: Keep your domain events clean and free from persistence concerns.

## Next Steps

Once you've implemented event sourcing, explore the [Error Handling](error-handling) guide to learn how to handle errors in an event-sourced system.