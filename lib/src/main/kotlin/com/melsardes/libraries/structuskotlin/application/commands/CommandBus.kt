/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.commands

import kotlin.reflect.KClass

/**
 * Interface for a command bus that dispatches commands to their corresponding handlers.
 *
 * The **CommandBus** is a central dispatcher that routes commands to their appropriate handlers.
 * It provides a level of indirection between the presentation layer (controllers, CLI, message listeners)
 * and the application layer (command handlers), enabling:
 * - **Decoupling**: Presentation layer doesn't need to know about specific handlers
 * - **Cross-cutting concerns**: Logging, validation, authorization can be added in one place
 * - **Testing**: Easy to mock or replace for testing
 * - **Flexibility**: Handler implementations can be swapped without changing callers
 *
 * ## Command Bus Pattern:
 * ```
 * ┌──────────────┐                    ┌─────────────┐
 * │ Controller   │───[Command]───────→│ CommandBus  │
 * └──────────────┘                    └─────────────┘
 *                                            │
 *                                            │ dispatch
 *                                            ↓
 *                                     ┌──────────────────┐
 *                                     │ CommandHandler   │
 *                                     └──────────────────┘
 *                                            │
 *                                            ↓
 *                                     ┌──────────────────┐
 *                                     │ Domain Layer     │
 *                                     └──────────────────┘
 * ```
 *
 * ## Usage Example:
 *
 * ### 1. Define Commands and Handlers:
 * ```kotlin
 * data class RegisterUserCommand(
 *     val email: String,
 *     val password: String
 * ) : Command
 *
 * class RegisterUserCommandHandler(
 *     private val userRepository: UserRepository
 * ) : CommandHandler<RegisterUserCommand, UserId> {
 *     override suspend fun handle(command: RegisterUserCommand): UserId {
 *         // Implementation
 *     }
 * }
 * ```
 *
 * ### 2. Register Handlers with the Bus:
 * ```kotlin
 * val commandBus: CommandBus = SimpleCommandBus()
 *
 * commandBus.register(
 *     RegisterUserCommand::class,
 *     registerUserCommandHandler
 * )
 * ```
 *
 * ### 3. Dispatch Commands from Controllers:
 * ```kotlin
 * @RestController
 * class UserController(private val commandBus: CommandBus) {
 *
 *     @PostMapping("/users")
 *     suspend fun registerUser(@RequestBody request: RegisterUserRequest): ResponseEntity<*> {
 *         val command = RegisterUserCommand(
 *             email = request.email,
 *             password = request.password
 *         )
 *
 *         val userId = commandBus.dispatch(command)
 *
 *         return ResponseEntity.created(URI.create("/users/$userId")).build()
 *     }
 * }
 * ```
 *
 * ## Implementation Example:
 * ```kotlin
 * class SimpleCommandBus : CommandBus {
 *     private val handlers = mutableMapOf<KClass<*>, CommandHandler<*, *>>()
 *
 *     override fun <C : Command, R> register(
 *         commandClass: KClass<C>,
 *         handler: CommandHandler<C, R>
 *     ) {
 *         handlers[commandClass] = handler
 *     }
 *
 *     override suspend fun <C : Command, R> dispatch(command: C): R {
 *         @Suppress("UNCHECKED_CAST")
 *         val handler = handlers[command::class] as? CommandHandler<C, R>
 *             ?: throw IllegalStateException("No handler registered for ${command::class.simpleName}")
 *
 *         return handler.handle(command)
 *     }
 * }
 * ```
 *
 * ## Advanced Features:
 *
 * ### 1. Middleware/Interceptors:
 * Add cross-cutting concerns like logging, validation, or authorization:
 * ```kotlin
 * class LoggingCommandBus(
 *     private val delegate: CommandBus,
 *     private val logger: Logger
 * ) : CommandBus {
 *     override suspend fun <C : Command, R> dispatch(command: C): R {
 *         logger.info("Dispatching command: ${command::class.simpleName}")
 *         val startTime = System.currentTimeMillis()
 *
 *         return try {
 *             delegate.dispatch(command).also {
 *                 val duration = System.currentTimeMillis() - startTime
 *                 logger.info("Command completed in ${duration}ms")
 *             }
 *         } catch (e: Exception) {
 *             logger.error("Command failed: ${e.message}", e)
 *             throw e
 *         }
 *     }
 * }
 * ```
 *
 * ### 2. Transaction Management:
 * Wrap command execution in a transaction:
 * ```kotlin
 * class TransactionalCommandBus(
 *     private val delegate: CommandBus,
 *     private val transactionManager: TransactionManager
 * ) : CommandBus {
 *     override suspend fun <C : Command, R> dispatch(command: C): R {
 *         return transactionManager.executeInTransaction {
 *             delegate.dispatch(command)
 *         }
 *     }
 * }
 * ```
 *
 * ### 3. Validation:
 * Validate commands before dispatching:
 * ```kotlin
 * class ValidatingCommandBus(
 *     private val delegate: CommandBus,
 *     private val validator: Validator
 * ) : CommandBus {
 *     override suspend fun <C : Command, R> dispatch(command: C): R {
 *         val violations = validator.validate(command)
 *         if (violations.isNotEmpty()) {
 *             throw ValidationException(violations)
 *         }
 *         return delegate.dispatch(command)
 *     }
 * }
 * ```
 *
 * ## Benefits:
 * 1. **Single Entry Point**: All commands go through one place
 * 2. **Testability**: Easy to mock for testing controllers
 * 3. **Flexibility**: Can add middleware without changing handlers
 * 4. **Decoupling**: Controllers don't depend on specific handlers
 * 5. **Consistency**: Ensures consistent command handling across the application
 *
 * ## When to Use:
 * - ✅ **Use** when you have multiple commands and want a consistent dispatch mechanism
 * - ✅ **Use** when you need cross-cutting concerns (logging, validation, transactions)
 * - ✅ **Use** in larger applications with many commands
 * - ⚠️ **Consider** if it adds value in small applications with few commands
 *
 * ## Alternative: Direct Handler Injection:
 * For simple applications, you might inject handlers directly:
 * ```kotlin
 * class UserController(
 *     private val registerUserHandler: RegisterUserCommandHandler
 * ) {
 *     suspend fun registerUser(request: RegisterUserRequest) {
 *         val command = RegisterUserCommand(request.email, request.password)
 *         return registerUserHandler.handle(command)
 *     }
 * }
 * ```
 *
 * The CommandBus adds value when you need the additional abstraction and flexibility.
 */
public interface CommandBus {
    /**
     * Registers a command handler for a specific command type.
     *
     * This method associates a command class with its handler, enabling the bus
     * to route commands to the correct handler during dispatch.
     *
     * ## Registration Example:
     * ```kotlin
     * commandBus.register(
     *     RegisterUserCommand::class,
     *     RegisterUserCommandHandler(userRepository, passwordHasher)
     * )
     * ```
     *
     * ## Framework Integration:
     * In frameworks with dependency injection (Spring, Koin, etc.), handlers are
     * typically registered automatically during application startup:
     * ```kotlin
     * @Configuration
     * class CommandBusConfiguration(
     *     private val commandBus: CommandBus,
     *     private val handlers: List<CommandHandler<*, *>>
     * ) {
     *     @PostConstruct
     *     fun registerHandlers() {
     *         handlers.forEach { handler ->
     *             // Register each handler with its command type
     *         }
     *     }
     * }
     * ```
     *
     * @param C The command type
     * @param R The result type
     * @param commandClass The KClass of the command
     * @param handler The handler that processes this command type
     */
    public fun <C : com.melsardes.libraries.structuskotlin.application.commands.Command, R> register(
        commandClass: KClass<C>,
        handler: com.melsardes.libraries.structuskotlin.application.commands.CommandHandler<C, R>
    )

    /**
     * Dispatches a command to its registered handler for execution.
     *
     * This method looks up the appropriate handler for the given command type
     * and delegates the command execution to that handler. It's a `suspend`
     * function to support non-blocking I/O operations.
     *
     * ## Dispatch Flow:
     * 1. Receive command from caller (controller, CLI, etc.)
     * 2. Look up the registered handler for the command type
     * 3. Delegate to the handler's `handle()` method
     * 4. Return the result to the caller
     *
     * ## Usage Example:
     * ```kotlin
     * val command = RegisterUserCommand(
     *     email = "john@example.com",
     *     password = "password123"
     * )
     *
     * val userId = commandBus.dispatch(command)
     * ```
     *
     * ## Error Handling:
     * - Throws `IllegalStateException` if no handler is registered for the command type
     * - Propagates any exceptions thrown by the handler
     * - Consider wrapping in try-catch for error handling at the presentation layer
     *
     * ## Type Safety:
     * The method is type-safe: the return type `R` is inferred from the handler's
     * return type, ensuring compile-time type checking.
     *
     * @param C The command type
     * @param R The result type
     * @param command The command to dispatch
     * @return The result of the command execution
     * @throws IllegalStateException if no handler is registered for the command type
     * @throws Exception any exception thrown by the command handler
     */
    public suspend fun <C : com.melsardes.libraries.structuskotlin.application.commands.Command, R> dispatch(command: C): R
}
