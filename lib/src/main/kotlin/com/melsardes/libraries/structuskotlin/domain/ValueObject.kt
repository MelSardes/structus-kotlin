/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.domain

/**
 * Marker interface for Value Objects in the domain model.
 *
 * A **Value Object** is a domain concept that is defined entirely by its attributes,
 * with no conceptual identity. Two value objects are considered equal if all their
 * attributes are equal.
 *
 * ## Key Characteristics:
 * - **Attribute-based equality**: Value objects are compared by their attributes, not by identity.
 * - **Immutable**: Value objects should be immutable. Once created, their state cannot change.
 * - **Replaceable**: If you need to "change" a value object, you create a new instance.
 * - **Side-effect free**: Methods on value objects should not have side effects.
 *
 * ## Implementation Guidelines:
 * 1. Use Kotlin `data class` for automatic `equals()`, `hashCode()`, and `copy()` implementation
 * 2. Make all properties `val` (immutable)
 * 3. Validate invariants in the constructor or factory method
 * 4. Provide meaningful domain methods that return new instances
 *
 * ## Usage Example:
 * ```kotlin
 * data class Money(
 *     val amount: BigDecimal,
 *     val currency: Currency
 * ) : ValueObject {
 *     init {
 *         require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }
 *     }
 *
 *     fun add(other: Money): Money {
 *         require(currency == other.currency) { "Cannot add different currencies" }
 *         return copy(amount = amount + other.amount)
 *     }
 * }
 *
 * data class Email(val value: String) : ValueObject {
 *     init {
 *         require(value.matches(EMAIL_REGEX)) { "Invalid email format" }
 *     }
 *
 *     companion object {
 *         private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
 *     }
 * }
 * ```
 *
 * ## Why Marker Interface?
 * This interface serves as a semantic marker to clearly identify value objects in the codebase.
 * It doesn't define any methods because Kotlin's `data class` already provides the necessary
 * equality and immutability semantics. The interface helps with:
 * - Code documentation and readability
 * - Static analysis and architectural validation
 * - Potential future extensions (e.g., serialization markers)
 *
 * ## Value Object vs Entity:
 * - **Entity**: Has identity, mutable, compared by ID (e.g., User, Order)
 * - **Value Object**: No identity, immutable, compared by attributes (e.g., Money, Address, Email)
 */
public interface ValueObject