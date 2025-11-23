/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.commands

/**
 * Interface for command handlers that process commands and execute business logic.
 *
 * A **CommandHandler** is responsible for orchestrating the execution of a specific command.
 * It coordinates between the domain layer (aggregates, entities, value objects) and the
 * infrastructure layer (repositories, external services) to fulfill the command's intent.
 *
 * ## Key Responsibilities:
 * 1. **Validate** the command (business rule validation)
 * 2. **Load** required aggregates from repositories
 * 3. **Execute** domain logic on aggregates
 * 4. **Persist** changes back to repositories
 * 5. **Publish** domain events (if applicable)
 *
 * ## Example:
 * ```kotlin
 * class CreateUserCommandHandler(
 *     private val userRepository: UserRepository
 * ) : CommandHandler<CreateUserCommand, UserId> {
 *
 *     override suspend operator fun invoke(command: CreateUserCommand): UserId {
 *         // Validate
 *         require(command.email.isNotBlank()) { "Email is required" }
 *
 *         // Create aggregate
 *         val user = User.create(command.email, command.name)
 *
 *         // Persist
 *         userRepository.save(user)
 *
 *         return user.id
 *     }
 * }
 *
 * // Usage: handler(command) instead of handler.handle(command)
 * val userId = createUserHandler(CreateUserCommand("john@example.com", "John"))
 * ```
 *
 * ## Handler Pattern:
 * Each command has exactly one handler. This creates a clear one-to-one mapping:
 * - `RegisterUserCommand` → `RegisterUserCommandHandler`
 * - `PlaceOrderCommand` → `PlaceOrderCommandHandler`
 * - `CancelSubscriptionCommand` → `CancelSubscriptionCommandHandler`
 *
 * ## Why Suspend Functions?
 * The `invoke` method is a `suspend` function to support:
 * - **Non-blocking I/O**: Repository calls don't block threads
 * - **Coroutine Integration**: Seamless integration with Kotlin coroutines
 * - **Scalability**: Better resource utilization in high-concurrency scenarios
 * - **Reactive Systems**: Compatible with reactive frameworks
 *
 * ## Usage Example:
 * ```kotlin
 * class RegisterUserCommandHandler(
 *     private val userRepository: UserRepository,
 *     private val passwordHasher: PasswordHasher,
 *     private val outboxRepository: MessageOutboxRepository
 * ) : CommandHandler<RegisterUserCommand, UserId> {
 *
 *     override suspend operator fun invoke(command: RegisterUserCommand): UserId {
 *         // 1. Check business rules
 *         val emailExists = userRepository.existsByEmail(Email(command.email))
 *         if (emailExists) {
 *             throw EmailAlreadyExistsException(command.email)
 *         }
 *
 *         // 2. Create domain entity
 *         val hashedPassword = passwordHasher.hash(command.password)
 *         val user = User.register(
 *             email = Email(command.email),
 *             password = Password(hashedPassword),
 *             name = Name(command.firstName, command.lastName)
 *         )
 *
 *         // 3. Save aggregate (within transaction)
 *         userRepository.save(user)
 *
 *         // 4. Save events to outbox (same transaction)
 *         user.domainEvents.forEach { event ->
 *             outboxRepository.save(event)
 *         }
 *
 *         // 5. Clear events from aggregate
 *         user.clearEvents()
 *
 *         // 6. Return result
 *         return user.id
 *     }
 * }
 * ```
 *
 * ## Handler Orchestration Flow:
 * ```
 * ┌────────────────────────────────────────────────────────────┐
 * │ CommandHandler.invoke(command)                             │
 * │                                                            │
 * │  1. Load Aggregate(s)                                      │
 * │     ↓                                                      │
 * │  2. Execute Domain Logic                                   │
 * │     ↓                                                      │
 * │  3. Save Aggregate(s)                                      │
 * │     ↓                                                      │
 * │  4. Save Events to Outbox (same transaction)               │
 * │     ↓                                                      │
 * │  5. Clear Events from Aggregate                            │
 * │     ↓                                                      │
 * │  6. Return Result                                          │
 * │                                                            │
 * └────────────────────────────────────────────────────────────┘
 * ```
 *
 * ## Transaction Management:
 * Command handlers should execute within a transaction boundary. The transaction typically:
 * - Starts before the handler is called
 * - Commits after the handler completes successfully
 * - Rolls back if an exception is thrown
 *
 * Transaction management is usually handled by the infrastructure layer (e.g., Spring's @Transactional):
 * ```kotlin
 * @Transactional
 * suspend fun executeCommand(command: Command): Result<*> {
 *     return commandHandler(command)
 * }
 * ```
 *
 * ## Error Handling:
 * Handlers can communicate errors in several ways:
 *
 * 1. **Exceptions** (for exceptional cases):
 * ```kotlin
 * override suspend operator fun invoke(command: RegisterUserCommand): UserId {
 *     if (userRepository.existsByEmail(command.email)) {
 *         throw EmailAlreadyExistsException(command.email)
 *     }
 *     // ...
 * }
 * ```
 *
 * 2. **Result Type** (for expected failures):
 * ```kotlin
 * // Define a custom exception
 * class EmailAlreadyExistsException(val email: String) : Exception("Email '$email' already exists")
 *
 * override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
 *     if (userRepository.existsByEmail(command.email)) {
 *         return Result.failure(EmailAlreadyExistsException(command.email))
 *     }
 *     // ...
 *     return Result.success(user.id)
 * }
 * ```
 *
 * ## Handler Composition:
 * Handlers should focus on orchestration, not business logic:
 * - ✅ **DO**: Call domain methods that encapsulate business rules
 * - ✅ **DO**: Coordinate between repositories and aggregates
 * - ✅ **DO**: Handle event publishing through the outbox
 * - ❌ **DON'T**: Implement business logic directly in the handler
 * - ❌ **DON'T**: Manipulate aggregate state directly (use domain methods)
 * - ❌ **DON'T**: Call other handlers directly (use the CommandBus if needed)
 *
 * ## Testing:
 * Command handlers are integration-tested with mocked repositories:
 * ```kotlin
 * @Test
 * fun `should register user successfully`() = runTest {
 *     // Given
 *     val command = RegisterUserCommand(
 *         email = "john@example.com",
 *         password = "password123",
 *         firstName = "John",
 *         lastName = "Doe"
 *     )
 *     val userRepository = mockk<UserRepository>()
 *     val passwordHasher = mockk<PasswordHasher>()
 *     val handler = RegisterUserCommandHandler(userRepository, passwordHasher)
 *
 *     coEvery { userRepository.existsByEmail(any()) } returns false
 *     coEvery { passwordHasher.hash(any()) } returns "hashed_password"
 *     coEvery { userRepository.save(any()) } just Runs
 *
 *     // When
 *     val userId = handler(command)
 *
 *     // Then
 *     assertNotNull(userId)
 *     coVerify { userRepository.save(any()) }
 * }
 * ```
 *
 * ## Dependency Injection:
 * Handlers typically receive their dependencies through constructor injection:
 * - Repositories (for data access)
 * - Domain services (for complex business logic)
 * - Infrastructure services (for external integrations)
 * - Outbox repository (for event publishing)
 *
 * ## Handler Registration:
 * Handlers are typically registered with the CommandBus during application startup:
 * ```kotlin
 * // In your DI configuration
 * commandBus.register(RegisterUserCommand::class, registerUserCommandHandler)
 * commandBus.register(PlaceOrderCommand::class, placeOrderCommandHandler)
 * ```
 *
 * @param C The type of command this handler processes
 * @param R The type of result this handler returns
 */
public interface CommandHandler<in C : com.melsardes.libraries.structuskotlin.application.commands.Command, out R> {
    /**
     * Handles the execution of a command.
     * This operator function allows calling the handler as: `handler(command)`
     *
     * This method orchestrates the command execution by:
     * 1. Loading necessary aggregates from repositories
     * 2. Executing domain logic through aggregate methods
     * 3. Saving modified aggregates
     * 4. Publishing domain events via the outbox
     * 5. Returning a result
     *
     * This is a `suspend` function to support non-blocking I/O operations and
     * integration with Kotlin coroutines.
     *
     * ## Exception Handling:
     * - Throw domain-specific exceptions for business rule violations
     * - Let infrastructure exceptions propagate for framework handling
     * - Consider using a Result type for expected failure cases
     *
     * ## Transaction Boundary:
     * This method should execute within a transaction. The transaction is typically
     * managed by the infrastructure layer (e.g., Spring's @Transactional).
     *
     * @param command The command to handle
     * @return The result of the command execution
     * @throws Exception if the command cannot be executed
     */
    public suspend operator fun invoke(command: C): R
}
