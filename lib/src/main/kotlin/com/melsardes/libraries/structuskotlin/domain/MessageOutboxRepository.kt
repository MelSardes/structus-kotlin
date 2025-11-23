/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.domain

/**
 * Repository interface for the Transactional Outbox Pattern.
 *
 * The **Transactional Outbox Pattern** is a reliable way to publish domain events in distributed
 * systems. Instead of directly publishing events to a message broker (which could fail), events
 * are first stored in an "outbox" table within the same database transaction as the aggregate.
 * A separate process then reads from the outbox and publishes events to the message broker.
 *
 * ## The Problem It Solves:
 * Without the outbox pattern, you face a dual-write problem:
 * 1. Save aggregate to database (succeeds)
 * 2. Publish event to message broker (fails)
 * Result: Inconsistent state - aggregate saved but event not published
 *
 * ## How It Works:
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Transaction Boundary                                        │
 * │                                                             │
 * │  1. Save Aggregate → Database                              │
 * │  2. Save Event → Outbox Table (same DB, same transaction)  │
 * │                                                             │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Separate Process (Outbox Publisher)                         │
 * │                                                             │
 * │  3. Poll Outbox Table                                       │
 * │  4. Publish Events → Message Broker                         │
 * │  5. Mark as Published or Delete                             │
 * │                                                             │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 *
 * ## Benefits:
 * - **Atomicity**: Event storage and aggregate persistence happen in the same transaction
 * - **Reliability**: Events are guaranteed to be published (at-least-once delivery)
 * - **Resilience**: System can recover from message broker failures
 * - **Ordering**: Events can be published in the order they were created
 *
 * ## Usage Example:
 * ```kotlin
 * // In the command handler (Application Layer)
 * suspend fun handle(command: RegisterUserCommand): Result<UserId> {
 *     return runCatching {
 *         withTransaction { // Hypothetical transaction block
 *             // 1. Execute domain logic
 *             val user = User.register(command.email, command.name)
 *
 *             // 2. Save aggregate
 *             userRepository.save(user)
 *
 *             // 3. Save events to outbox (same transaction)
 *             user.domainEvents.forEach { event ->
 *                 messageOutboxRepository.save(OutboxMessage.from(event))
 *             }
 *
 *             // 4. Clear events from aggregate
 *             user.clearEvents()
 *
 *             user.id
 *         }
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
 *                 // Retry logic or dead letter queue
 *                 outboxRepository.incrementRetryCount(message.id)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Outbox Message Structure:
 * An outbox message typically contains:
 * - **id**: Unique identifier for the outbox message
 * - **eventId**: The ID of the domain event
 * - **eventType**: The fully qualified class name of the event
 * - **payload**: Serialized event data (JSON, Avro, etc.)
 * - **aggregateId**: The ID of the aggregate that produced the event
 * - **createdAt**: When the message was created
 * - **publishedAt**: When the message was successfully published (nullable)
 * - **retryCount**: Number of publication attempts
 *
 * ## Implementation Considerations:
 * 1. **Idempotency**: Event consumers should handle duplicate events gracefully
 * 2. **Ordering**: Consider using aggregate ID for partitioning to maintain order per aggregate
 * 3. **Cleanup**: Implement a strategy to clean up old published messages
 * 4. **Monitoring**: Track outbox size, publish lag, and failure rates
 * 5. **Dead Letter Queue**: Handle events that fail repeatedly
 *
 * ## Database Table Example:
 * ```sql
 * CREATE TABLE message_outbox (
 *     id UUID PRIMARY KEY,
 *     event_id VARCHAR(255) NOT NULL,
 *     event_type VARCHAR(500) NOT NULL,
 *     payload JSONB NOT NULL,
 *     aggregate_id VARCHAR(255) NOT NULL,
 *     created_at TIMESTAMP NOT NULL,
 *     published_at TIMESTAMP,
 *     retry_count INT DEFAULT 0,
 *     INDEX idx_unpublished (published_at, created_at) WHERE published_at IS NULL
 * );
 * ```
 */
public interface MessageOutboxRepository : com.melsardes.libraries.structuskotlin.domain.Repository {
    /**
     * Saves a domain event to the outbox table.
     *
     * This method should be called within the same transaction as the aggregate save operation.
     * The event is serialized and stored in the outbox table, ready to be published by a
     * separate outbox publisher process.
     *
     * @param event The domain event to save to the outbox
     */
    public suspend fun save(event: com.melsardes.libraries.structuskotlin.domain.events.DomainEvent)

    /**
     * Retrieves unpublished events from the outbox, ordered by creation time.
     *
     * This method is typically called by the outbox publisher process to fetch events
     * that need to be published to the message broker. Events are returned in the order
     * they were created to maintain event ordering.
     *
     * @param limit Maximum number of events to retrieve
     * @return List of unpublished outbox messages
     */
    public suspend fun findUnpublished(limit: Int): List<com.melsardes.libraries.structuskotlin.domain.OutboxMessage>

    /**
     * Marks an outbox message as successfully published.
     *
     * This method is called after an event has been successfully published to the message
     * broker. It updates the `publishedAt` timestamp to indicate the event has been processed.
     *
     * @param messageId The ID of the outbox message to mark as published
     */
    public suspend fun markAsPublished(messageId: String)

    /**
     * Increments the retry count for a failed publication attempt.
     *
     * When publishing an event fails, this method is called to track the number of
     * retry attempts. This information can be used to implement retry strategies
     * and move events to a dead letter queue after too many failures.
     *
     * @param messageId The ID of the outbox message to update
     */
    public suspend fun incrementRetryCount(messageId: String)
}

/**
 * Represents a message stored in the outbox table.
 *
 * This is a data class that wraps a domain event along with metadata needed for
 * the outbox pattern implementation. It's typically used by the infrastructure layer
 * when implementing the MessageOutboxRepository.
 *
 * @property id Unique identifier for this outbox message
 * @property event The domain event to be published
 * @property createdAt When this message was created
 * @property publishedAt When this message was successfully published (null if not yet published)
 * @property retryCount Number of times publication has been attempted
 */
public data class OutboxMessage(
    val id: String,
    val event: com.melsardes.libraries.structuskotlin.domain.events.DomainEvent,
    val createdAt: kotlin.time.Instant,
    val publishedAt: kotlin.time.Instant? = null,
    val retryCount: Int = 0
) {
    /**
     * Checks if this message has been published.
     */
    public fun isPublished(): Boolean = publishedAt != null

    /**
     * Checks if this message has exceeded the maximum retry count.
     */
    public fun hasExceededRetries(maxRetries: Int): Boolean = retryCount >= maxRetries

    public companion object {
        /**
         * Creates an OutboxMessage from a domain event.
         *
         * This factory method is useful for creating outbox messages in command handlers.
         *
         * @param event The domain event to wrap
         * @return A new OutboxMessage instance
         */
        public fun from(event: com.melsardes.libraries.structuskotlin.domain.events.DomainEvent): OutboxMessage {
            return OutboxMessage(
                id = java.util.UUID.randomUUID().toString(),
                event = event,
                createdAt = kotlin.time.Clock.System.now()
            )
        }
    }
}
