---
sidebar_position: 1
---

# Kotlin Coroutines Guide

Kotlin Coroutines are a fundamental part of Structus's asynchronous processing capabilities. This guide provides an overview of how coroutines work and how they're used within Structus.

## Basics of Coroutines

Coroutines are Kotlin's solution for asynchronous programming. They enable you to write asynchronous code in a sequential style, making it easier to understand and maintain.

### Suspending Functions

Suspending functions are at the core of coroutines. These functions can pause their execution and resume later, allowing for non-blocking operations.

```kotlin
// Defining a suspending function
suspend fun fetchData(): Data {
    // This function can be paused and resumed
    return networkService.getData()
}
```

### Coroutine Builders

Coroutine builders are used to start new coroutines. The most common ones are:

```kotlin
// launch: starts a new coroutine without returning a result
suspend fun main() = coroutineScope {
    launch {
        delay(1000)
        println("Kotlin Coroutines World!")
    }
    println("Hello")
}

// async: starts a coroutine that returns a result
suspend fun fetchTwoValues() = coroutineScope {
    val valueOne = async { getValue1() }
    val valueTwo = async { getValue2() }
    
    // Await both results
    valueOne.await() + valueTwo.await()
}
```

## Using Coroutines in Structus

Structus uses coroutines extensively to ensure non-blocking operations, especially in repositories and command/query handlers.

### Repositories

All repository methods in Structus are suspending functions:

```kotlin
interface OrderRepository : Repository {
    suspend fun findById(id: OrderId): Order?
    suspend fun save(order: Order)
    suspend fun delete(id: OrderId)
}
```

### Command Handlers

Command handlers use the `suspend` modifier to enable asynchronous processing:

```kotlin
class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        return runCatching {
            val order = Order.create(command.customerId)
            
            orderRepository.save(order)
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

## Coroutines and Transactional Outbox

Structus's Transactional Outbox pattern relies heavily on coroutines for reliable event publishing:

```kotlin
class OutboxPublisher(
    private val outboxRepository: MessageOutboxRepository,
    private val eventPublisher: DomainEventPublisher
) {
    
    suspend fun publishPendingEvents() {
        val messages = outboxRepository.findUnpublished(limit = 100)
        
        messages.forEach { message ->
            try {
                val event = deserializeEvent(message.payload, message.eventType)
                eventPublisher.publish(event)
                outboxRepository.markAsPublished(message.id)
            } catch (e: Exception) {
                outboxRepository.incrementRetryCount(message.id)
            }
        }
    }
}
```

## Best Practices

### Use Structured Concurrency

Always use structured concurrency to ensure all coroutines are properly managed:

```kotlin
suspend fun fetchData() = coroutineScope {
    // All coroutines started in this scope will be completed
    // before the function returns
    val result1 = async { api.fetchFirstPart() }
    val result2 = async { api.fetchSecondPart() }
    
    combineResults(result1.await(), result2.await())
}
```

### Handle Exceptions Properly

Use `runCatching` or try-catch blocks to handle exceptions in coroutines:

```kotlin
suspend fun safeApiCall() = runCatching {
    api.fetchData()
}.getOrElse { error ->
    // Handle error
    logger.error(error)
    defaultValue
}
```

### Choose the Right Dispatcher

Select an appropriate dispatcher based on the work being done:

- **Dispatchers.IO**: For IO-bound work (network, disk)
- **Dispatchers.Default**: For CPU-intensive work
- **Dispatchers.Main**: For UI operations (Android)

```kotlin
withContext(Dispatchers.IO) {
    // IO operations (network, database)
}
```

## Additional Resources

- [Kotlin Coroutines Official Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Kotlin Coroutines by Example](https://kotlinlang.org/docs/coroutines-basics.html)
- [GitHub Repository](https://github.com/kotlin/kotlinx.coroutines)