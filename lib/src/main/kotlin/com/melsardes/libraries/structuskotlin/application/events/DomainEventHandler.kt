/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.events

/**
 * Interface for handling domain events.
 *
 * A **DomainEventHandler** processes domain events to perform side effects such as:
 * - Updating read models (CQRS projections)
 * - Sending notifications (emails, push notifications)
 * - Integrating with external systems
 * - Triggering workflows or sagas
 * - Updating analytics or reporting databases
 *
 * ## Key Characteristics:
 * - **Asynchronous**: Handlers process events asynchronously after the main transaction
 * - **Idempotent**: Handlers should be idempotent (safe to process the same event multiple times)
 * - **Independent**: Each handler operates independently and doesn't affect the main transaction
 * - **Side Effects Only**: Handlers should not modify the write model (aggregates)
 *
 * ## Handler Pattern:
 * Each event type can have multiple handlers. This creates a one-to-many relationship:
 * - `UserRegisteredEvent` → `SendWelcomeEmailHandler`, `CreateUserProfileHandler`, `UpdateAnalyticsHandler`
 * - `OrderPlacedEvent` → `SendOrderConfirmationHandler`, `UpdateInventoryHandler`, `NotifyWarehouseHandler`
 *
 * ## Why Suspend Functions?
 * The `handle` method is a `suspend` function to support:
 * - **Non-blocking I/O**: External API calls don't block threads
 * - **Coroutine Integration**: Seamless integration with Kotlin coroutines
 * - **Scalability**: Better resource utilization in high-concurrency scenarios
 *
 * ## Usage Example:
 * ```kotlin
 * class SendWelcomeEmailHandler(
 *     private val emailService: EmailService,
 *     private val userRepository: UserRepository
 * ) : DomainEventHandler<UserRegisteredEvent> {
 *
 *     override suspend fun handle(event: UserRegisteredEvent) {
 *         val user = userRepository.findById(event.userId) ?: return
 *
 *         emailService.send(
 *             to = user.email,
 *             subject = "Welcome!",
 *             body = "Welcome to our platform, ${user.name}!"
 *         )
 *     }
 * }
 * ```
 *
 * ## Event Dispatcher:
 * Handlers are registered with an event dispatcher, which is responsible for routing events
 * to the correct handlers based on the event type.
 *
 * ### Event Handler Registration:
 * ```kotlin
 * // In your DI configuration
 * eventDispatcher.register(UserRegisteredEvent::class, sendWelcomeEmailHandler)
 * eventDispatcher.register(UserRegisteredEvent::class, updateUserProjectionHandler)
 * eventDispatcher.register(OrderPlacedEvent::class, notifyAdminHandler)
 * ```
 *
 * ### Event Dispatcher Example:
 * ```kotlin
 * class SimpleEventDispatcher {
 *     private val handlers = mutableMapOf<KClass<out DomainEvent>, MutableList<DomainEventHandler<*>>>()
 *
 *     fun <E : DomainEvent> register(eventClass: KClass<E>, handler: DomainEventHandler<E>) {
 *         handlers.getOrPut(eventClass) { mutableListOf() }.add(handler)
 *     }
 *
 *     suspend fun dispatch(event: DomainEvent) {
 *         handlers[event::class]?.forEach { handler ->
 *             try {
 *                 @Suppress("UNCHECKED_CAST")
 *                 (handler as DomainEventHandler<DomainEvent>).handle(event)
 *             } catch (e: Exception) {
 *                 // logger.error("Handler failed for event ${event.eventId}", e)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Idempotency:
 * Handlers MUST be idempotent because events may be delivered multiple times:
 * ```kotlin
 * class UpdateUserProjectionHandler(
 *     private val projectionRepository: UserProjectionRepository,
 *     private val processedEventRepository: ProcessedEventRepository
 * ) : DomainEventHandler<UserRegisteredEvent> {
 *
 *     override suspend fun handle(event: UserRegisteredEvent) {
 *         // Check if already processed
 *         if (processedEventRepository.exists(event.eventId)) {
 *             return // Already processed, skip
 *         }
 *
 *         // Process the event
 *         projectionRepository.insert(UserProjection.from(event))
 *
 *         // Mark as processed
 *         processedEventRepository.save(event.eventId)
 *     }
 * }
 * ```
 *
 * ## Error Handling:
 * Handlers should handle errors gracefully:
 * ```kotlin
 * override suspend fun handle(event: UserRegisteredEvent) {
 *     try {
 *         emailService.send(...)
 *     } catch (e: EmailServiceException) {
 *         // Log error but don't fail the entire event processing
 *         // logger.error("Failed to send welcome email", e)
 *         // Optionally: Store in dead letter queue for retry
 *         // deadLetterQueue.add(event, e)
 *     }
 * }
 * ```
 *
 * ## Testing:
 * Event handlers are easy to test in isolation:
 * ```kotlin
 * @Test
 * fun `should send welcome email when user registered`() = runTest {
 *     // Given
 *     val event = UserRegisteredEvent(
 *         userId = "user-123",
 *         email = "john@example.com",
 *         registeredAt = Instant.now()
 *     )
 *     val emailService = mockk<EmailService>()
 *     val handler = SendWelcomeEmailHandler(emailService, userRepository)
 *
 *     coEvery { emailService.send(any(), any(), any()) } just Runs
 *
 *     // When
 *     handler.handle(event)
 *
 *     // Then
 *     coVerify {
 *         emailService.send(
 *             to = "john@example.com",
 *             subject = "Welcome!",
 *             body = any()
 *         )
 *     }
 * }
 * ```
 *
 * ## Handler Composition:
 * Handlers should focus on a single responsibility:
 * - ✅ **DO**: Handle one specific side effect
 * - ✅ **DO**: Be idempotent
 * - ✅ **DO**: Handle errors gracefully
 * - ✅ **DO**: Log important operations
 * - ❌ **DON'T**: Modify aggregates (use commands instead)
 * - ❌ **DON'T**: Call other handlers directly
 * - ❌ **DON'T**: Perform long-running synchronous operations
 * - ❌ **DON'T**: Throw exceptions for expected failures
 *
 * @param E The type of domain event this handler processes
 */
public interface DomainEventHandler<in E : com.melsardes.libraries.structuskotlin.domain.events.DomainEvent> {
    /**
     * Handles a domain event.
     *
     * This method processes the event to perform side effects such as updating
     * read models, sending notifications, or integrating with external systems.
     *
     * This is a `suspend` function to support non-blocking I/O operations and
     * integration with Kotlin coroutines.
     *
     * ## Idempotency Requirement:
     * This method MUST be idempotent. It should produce the same result when
     * called multiple times with the same event, as events may be delivered
     * more than once due to retries or at-least-once delivery guarantees.
     *
     * ## Error Handling:
     * - Handle expected errors gracefully (log and continue)
     * - Let unexpected errors propagate for framework handling
     * - Consider using a dead letter queue for failed events
     *
     * ## Transaction Boundary:
     * Event handlers typically run outside the main transaction. If you need
     * transactional behavior, manage it within the handler.
     *
     * @param event The domain event to handle
     */
    public suspend fun handle(event: E)
}
