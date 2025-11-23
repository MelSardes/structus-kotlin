/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.events

/**
 * Interface for publishing domain events to external systems.
 *
 * The **DomainEventPublisher** is responsible for publishing domain events to message brokers,
 * event buses, or other external systems. This interface defines the contract for event publishing,
 * while the actual implementation is provided by the infrastructure layer.
 *
 * ## Key Responsibilities:
 * 1. **Publish** domain events to external systems (message brokers, event buses)
 * 2. **Serialize** events into the appropriate format (JSON, Avro, Protobuf)
 * 3. **Route** events to the correct topics/queues based on event type
 * 4. **Handle** publishing failures (retry, dead letter queue)
 *
 * ## Event Publishing Patterns:
 *
 * ### 1. Direct Publishing (Not Recommended):
 * Publishing events immediately after saving the aggregate can lead to inconsistencies
 * if the database transaction fails after the event is published.
 *
 * ```kotlin
 * // ❌ AVOID: Direct publishing without transactional safety
 * suspend fun handle(command: RegisterUserCommand): UserId {
 *     val user = User.register(command.email, command.name)
 *     userRepository.save(user)
 *
 *     // If this succeeds but the transaction rolls back, we have inconsistency
 *     user.domainEvents.forEach { event ->
 *         eventPublisher.publish(event)
 *     }
 *
 *     return user.id
 * }
 * ```
 *
 * ### 2. Transactional Outbox Pattern (Recommended):
 * Store events in an outbox table within the same transaction as the aggregate.
 * A separate process then publishes events from the outbox.
 *
 * ```kotlin
 * // ✅ RECOMMENDED: Use Transactional Outbox Pattern
 * suspend fun handle(command: RegisterUserCommand): UserId {
 *     return withTransaction {
 *         val user = User.register(command.email, command.name)
 *
 *         // Save aggregate
 *         userRepository.save(user)
 *
 *         // Save events to outbox (same transaction)
 *         user.domainEvents.forEach { event ->
 *             outboxRepository.save(event)
 *         }
 *
 *         user.clearEvents()
 *         user.id
 *     }
 * }
 *
 * // Separate outbox publisher process
 * class OutboxPublisher(
 *     private val outboxRepository: MessageOutboxRepository,
 *     private val eventPublisher: DomainEventPublisher
 * ) {
 *     suspend fun publishPendingEvents() {
 *         val messages = outboxRepository.findUnpublished(limit = 100)
 *
 *         messages.forEach { message ->
 *             try {
 *                 eventPublisher.publish(message.event)
 *                 outboxRepository.markAsPublished(message.id)
 *             } catch (e: Exception) {
 *                 outboxRepository.incrementRetryCount(message.id)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Implementation Examples:
 *
 * ### 1. Kafka Implementation:
 * ```kotlin
 * class KafkaDomainEventPublisher(
 *     private val kafkaTemplate: KafkaTemplate<String, String>,
 *     private val objectMapper: ObjectMapper
 * ) : DomainEventPublisher {
 *
 *     override suspend fun publish(event: DomainEvent) {
 *         val topic = getTopicForEvent(event)
 *         val key = event.aggregateId
 *         val payload = objectMapper.writeValueAsString(event)
 *
 *         kafkaTemplate.send(topic, key, payload).await()
 *     }
 *
 *     override suspend fun publishBatch(events: List<DomainEvent>) {
 *         events.forEach { event ->
 *             publish(event)
 *         }
 *     }
 *
 *     private fun getTopicForEvent(event: DomainEvent): String {
 *         return when (event) {
 *             is UserRegisteredEvent -> "user.registered"
 *             is OrderPlacedEvent -> "order.placed"
 *             else -> "domain.events"
 *         }
 *     }
 * }
 * ```
 *
 * ### 2. RabbitMQ Implementation:
 * ```kotlin
 * class RabbitMQDomainEventPublisher(
 *     private val rabbitTemplate: RabbitTemplate,
 *     private val objectMapper: ObjectMapper
 * ) : DomainEventPublisher {
 *
 *     override suspend fun publish(event: DomainEvent) {
 *         val exchange = "domain.events"
 *         val routingKey = event::class.simpleName ?: "unknown"
 *         val payload = objectMapper.writeValueAsString(event)
 *
 *         rabbitTemplate.convertAndSend(exchange, routingKey, payload)
 *     }
 *
 *     override suspend fun publishBatch(events: List<DomainEvent>) {
 *         events.forEach { event ->
 *             publish(event)
 *         }
 *     }
 * }
 * ```
 *
 * ### 3. In-Memory Implementation (for testing):
 * ```kotlin
 * class InMemoryDomainEventPublisher : DomainEventPublisher {
 *     private val publishedEvents = mutableListOf<DomainEvent>()
 *
 *     override suspend fun publish(event: DomainEvent) {
 *         publishedEvents.add(event)
 *     }
 *
 *     override suspend fun publishBatch(events: List<DomainEvent>) {
 *         publishedEvents.addAll(events)
 *     }
 *
 *     fun getPublishedEvents(): List<DomainEvent> = publishedEvents.toList()
 *
 *     fun clear() {
 *         publishedEvents.clear()
 *     }
 * }
 * ```
 *
 * ## Event Routing:
 * Events can be routed to different destinations based on:
 * - **Event Type**: Different topics/queues for different event types
 * - **Aggregate Type**: Route based on the aggregate that produced the event
 * - **Event Metadata**: Route based on custom metadata (tenant, region, etc.)
 *
 * ```kotlin
 * interface EventRouter {
 *     fun getDestination(event: DomainEvent): EventDestination
 * }
 *
 * data class EventDestination(
 *     val topic: String,
 *     val partitionKey: String? = null
 * )
 *
 * class TypeBasedEventRouter : EventRouter {
 *     override fun getDestination(event: DomainEvent): EventDestination {
 *         return when (event) {
 *             is UserRegisteredEvent -> EventDestination(
 *                 topic = "user.events",
 *                 partitionKey = event.aggregateId
 *             )
 *             is OrderPlacedEvent -> EventDestination(
 *                 topic = "order.events",
 *                 partitionKey = event.aggregateId
 *             )
 *             else -> EventDestination(topic = "domain.events")
 *         }
 *     }
 * }
 * ```
 *
 * ## Event Serialization:
 * Events need to be serialized for transmission:
 * - **JSON**: Human-readable, widely supported
 * - **Avro**: Schema evolution, compact binary format
 * - **Protobuf**: Efficient, strongly typed
 *
 * Include event metadata in the serialized format:
 * ```json
 * {
 *   "eventId": "evt-123",
 *   "eventType": "UserRegisteredEvent",
 *   "occurredAt": "2024-01-15T10:30:00Z",
 *   "aggregateId": "user-456",
 *   "payload": {
 *     "userId": "user-456",
 *     "email": "john@example.com",
 *     "registeredAt": "2024-01-15T10:30:00Z"
 *   }
 * }
 * ```
 *
 * ## Error Handling:
 * Publishing can fail for various reasons:
 * - Network issues
 * - Message broker unavailable
 * - Serialization errors
 * - Authorization failures
 *
 * Strategies for handling failures:
 * 1. **Retry**: Attempt to publish again (with exponential backoff)
 * 2. **Dead Letter Queue**: Move failed events to a DLQ for manual inspection
 * 3. **Circuit Breaker**: Stop publishing if the broker is consistently failing
 * 4. **Monitoring**: Alert on publishing failures
 *
 * ## Idempotency:
 * Event consumers should be idempotent because:
 * - Events may be published multiple times (at-least-once delivery)
 * - Network issues can cause duplicate messages
 * - Retry logic can result in duplicates
 *
 * Include `eventId` in events to enable idempotent processing:
 * ```kotlin
 * class UserEventConsumer(
 *     private val processedEventIds: Set<String>
 * ) {
 *     suspend fun consume(event: UserRegisteredEvent) {
 *         if (processedEventIds.contains(event.eventId)) {
 *             // Already processed, skip
 *             return
 *         }
 *
 *         // Process the event
 *         // ...
 *
 *         // Mark as processed
 *         processedEventIds.add(event.eventId)
 *     }
 * }
 * ```
 *
 * ## Testing:
 * Use an in-memory implementation for testing:
 * ```kotlin
 * @Test
 * fun `should publish user registered event`() = runTest {
 *     val eventPublisher = InMemoryDomainEventPublisher()
 *     val handler = RegisterUserCommandHandler(
 *         userRepository,
 *         passwordHasher,
 *         eventPublisher
 *     )
 *
 *     val command = RegisterUserCommand(
 *         email = "john@example.com",
 *         password = "password123"
 *     )
 *
 *     handler.handle(command)
 *
 *     val publishedEvents = eventPublisher.getPublishedEvents()
 *     assertEquals(1, publishedEvents.size)
 *     assertTrue(publishedEvents[0] is UserRegisteredEvent)
 * }
 * ```
 */
public interface DomainEventPublisher {
    /**
     * Publishes a single domain event to external systems.
     *
     * This method serializes the event and sends it to the appropriate destination
     * (message broker, event bus, etc.). The actual implementation is provided by
     * the infrastructure layer.
     *
     * ## Usage:
     * ```kotlin
     * val event = UserRegisteredEvent(
     *     aggregateId = user.id.value,
     *     userId = user.id.value,
     *     email = user.email.value,
     *     registeredAt = Instant.now()
     * )
     *
     * eventPublisher.publish(event)
     * ```
     *
     * ## Error Handling:
     * - Throws an exception if publishing fails
     * - Implementation should handle retries and error logging
     * - Consider using the Transactional Outbox pattern to ensure reliability
     *
     * ## Idempotency:
     * Event consumers should be idempotent because this method may publish
     * the same event multiple times in case of retries.
     *
     * @param event The domain event to publish
     * @throws Exception if the event cannot be published
     */
    public suspend fun publish(event: com.melsardes.libraries.structuskotlin.domain.events.DomainEvent)

    /**
     * Publishes multiple domain events in a batch.
     *
     * This method is useful for publishing multiple events from the same aggregate
     * or transaction. Implementations may optimize batch publishing by sending
     * multiple events in a single network call.
     *
     * ## Usage:
     * ```kotlin
     * val events = listOf(
     *     UserRegisteredEvent(...),
     *     WelcomeEmailSentEvent(...)
     * )
     *
     * eventPublisher.publishBatch(events)
     * ```
     *
     * ## Atomicity:
     * Implementations should strive for all-or-nothing semantics:
     * - Either all events are published successfully
     * - Or none are published (and an exception is thrown)
     *
     * However, due to the distributed nature of message brokers, true atomicity
     * may not be achievable. Consider using the Transactional Outbox pattern
     * for guaranteed delivery.
     *
     * @param events The list of domain events to publish
     * @throws Exception if any event cannot be published
     */
    public suspend fun publishBatch(events: List<com.melsardes.libraries.structuskotlin.domain.events.DomainEvent>)
}
