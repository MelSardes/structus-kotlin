/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.application.commands

/**
 * Marker interface for all commands in the application layer.
 *
 * A **Command** represents an intention to change the state of the system. Commands are
 * part of the **Command/Query Separation (CQS)** pattern, where commands handle writes
 * (state changes) and queries handle reads (data retrieval).
 *
 * ## Key Characteristics:
 * - **Imperative naming**: Commands are named as actions (e.g., `RegisterUser`, `PlaceOrder`, `CancelSubscription`)
 * - **Write operations**: Commands modify system state
 * - **Intent-revealing**: Command names should clearly express the user's intention
 * - **Immutable**: Commands should be immutable data structures
 * - **Validation**: Commands can include basic structural validation
 *
 * ## Command vs Query:
 * - **Command**: Changes state, may return a simple result (ID, success/failure), named as verbs
 * - **Query**: Reads data, returns data, never changes state, named as questions
 *
 * ## Naming Convention:
 * Commands should be named in the imperative form, describing the action to be performed:
 * - `RegisterUserCommand`
 * - `PlaceOrderCommand`
 * - `UpdateProfileCommand`
 * - `CancelSubscriptionCommand`
 * - `ApprovePaymentCommand`
 *
 * ## Usage Example:
 * ```kotlin
 * data class RegisterUserCommand(
 *     val email: String,
 *     val password: String,
 *     val firstName: String,
 *     val lastName: String
 * ) : Command {
 *     init {
 *         require(email.isNotBlank()) { "Email cannot be blank" }
 *         require(password.length >= 8) { "Password must be at least 8 characters" }
 *     }
 * }
 *
 * data class PlaceOrderCommand(
 *     val customerId: String,
 *     val items: List<OrderItemDto>,
 *     val shippingAddress: AddressDto
 * ) : Command {
 *     init {
 *         require(items.isNotEmpty()) { "Order must have at least one item" }
 *     }
 * }
 *
 * data class UpdateProfileCommand(
 *     val userId: String,
 *     val firstName: String?,
 *     val lastName: String?,
 *     val phoneNumber: String?
 * ) : Command
 * ```
 *
 * ## Command Structure:
 * Commands are typically implemented as Kotlin data classes containing:
 * 1. **Identifiers**: IDs of entities to operate on
 * 2. **Data**: Information needed to perform the operation
 * 3. **Validation**: Basic structural validation in the `init` block
 *
 * ## Command Flow:
 * ```
 * ┌──────────────┐      ┌─────────────┐      ┌──────────────────┐
 * │ Presentation │─────→│ CommandBus  │─────→│ CommandHandler   │
 * │   Layer      │      │             │      │                  │
 * └──────────────┘      └─────────────┘      └──────────────────┘
 *                                                      │
 *                                                      ↓
 *                                             ┌─────────────────┐
 *                                             │ Domain Layer    │
 *                                             │ (Aggregate)     │
 *                                             └─────────────────┘
 * ```
 *
 * ## Validation Strategy:
 * Commands should include basic structural validation (not null, not empty, format checks).
 * Complex business rules should be validated in the domain layer:
 *
 * - **Command Validation** (Structural):
 *   - Required fields are present
 *   - Data formats are correct (email format, phone format)
 *   - Basic constraints (min/max length, positive numbers)
 *
 * - **Domain Validation** (Business Rules):
 *   - User email is unique
 *   - Order total matches item prices
 *   - Account has sufficient balance
 *   - Aggregate invariants are maintained
 *
 * ## Return Values:
 * Commands typically return:
 * - **Success**: The ID of the created/modified entity, or a simple success indicator
 * - **Failure**: An error or exception indicating what went wrong
 *
 * Consider using `kotlin.Result<T>` for explicit success/failure handling:
 * ```kotlin
 * // Define a custom exception for a business rule violation
 * class EmailAlreadyExistsException(val email: String) : Exception("Email '$email' already exists")
 *
 * // The handler can return a Result
 * override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
 *     if (userRepository.existsByEmail(command.email)) {
 *         return Result.failure(EmailAlreadyExistsException(command.email))
 *     }
 *     // ...
 *     return Result.success(user.id)
 * }
 * ```
 *
 * ## Command Handler:
 * Each command is processed by a corresponding CommandHandler:
 * ```kotlin
 * class RegisterUserCommandHandler(
 *     private val userRepository: UserRepository,
 *     private val passwordHasher: PasswordHasher
 * ) : CommandHandler<RegisterUserCommand, UserId> {
 *     override suspend operator fun invoke(command: RegisterUserCommand): UserId {
 *         // 1. Create domain entity
 *         val user = User.register(
 *             email = Email(command.email),
 *             password = passwordHasher.hash(command.password),
 *             name = Name(command.firstName, command.lastName)
 *         )
 *
 *         // 2. Save to repository
 *         userRepository.save(user)
 *
 *         // 3. Publish events (via outbox)
 *         // ...
 *
 *         // 4. Return result
 *         return user.id
 *     }
 * }
 * ```
 *
 * ## Testing:
 * Commands are easy to test because they are simple data structures:
 * ```kotlin
 * @Test
 * fun `should reject command with blank email`() {
 *     assertThrows<IllegalArgumentException> {
 *         RegisterUserCommand(
 *             email = "",
 *             password = "password123",
 *             firstName = "John",
 *             lastName = "Doe"
 *         )
 *     }
 * }
 * ```
 *
 * ## Why Marker Interface?
 * This interface serves as a semantic marker to:
 * - Clearly identify commands in the codebase
 * - Enable type-safe command bus implementations
 * - Support architectural validation and static analysis
 * - Provide a common base for potential future extensions (e.g., command metadata)
 */
public interface Command
