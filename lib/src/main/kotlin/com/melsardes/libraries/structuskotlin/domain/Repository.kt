/*
 * Copyright (c) 2025 Mel Sardes
 * Licensed under the MIT License
 */

package com.melsardes.libraries.structuskotlin.domain

/**
 * Marker interface for all repository contracts in the domain layer.
 *
 * A **Repository** provides the illusion of an in-memory collection of aggregate roots.
 * It encapsulates the logic for accessing and persisting aggregates, abstracting away
 * the underlying data storage mechanism (database, file system, external API, etc.).
 *
 * ## Key Principles:
 * 1. **Domain Layer Contract**: Repository interfaces are defined in the domain layer
 * 2. **Infrastructure Implementation**: Concrete implementations live in the infrastructure layer
 * 3. **Aggregate-Centric**: One repository per aggregate root (not per entity)
 * 4. **Collection-Like**: Methods should feel like working with an in-memory collection
 * 5. **Persistence Ignorance**: The domain doesn't know about databases, SQL, or ORMs
 *
 * ## Naming Convention:
 * - Interface: `UserRepository`, `OrderRepository` (in domain layer)
 * - Implementation: `UserRepositoryImpl`, `OrderRepositoryImpl` (in infrastructure layer)
 *
 * ## Common Repository Methods:
 * ```kotlin
 * interface UserRepository : Repository {
 *     suspend fun findById(id: UserId): User?
 *     suspend fun findByEmail(email: Email): User?
 *     suspend fun save(user: User)
 *     suspend fun delete(user: User)
 *     suspend fun existsByEmail(email: Email): Boolean
 * }
 *
 * interface OrderRepository : Repository {
 *     suspend fun findById(id: OrderId): Order?
 *     suspend fun findByCustomerId(customerId: CustomerId): List<Order>
 *     suspend fun save(order: Order)
 *     suspend fun findPendingOrders(): List<Order>
 * }
 * ```
 *
 * ## Why Suspend Functions?
 * All repository methods should be `suspend` functions to support:
 * - **Non-blocking I/O**: Database operations don't block threads
 * - **Coroutine Integration**: Seamless integration with Kotlin coroutines
 * - **Reactive Systems**: Compatible with reactive frameworks (R2DBC, etc.)
 * - **Scalability**: Better resource utilization in high-concurrency scenarios
 *
 * ## Repository vs DAO:
 * - **Repository**: Works with domain aggregates, uses domain language, defined in domain layer
 * - **DAO**: Works with database tables, uses technical language, defined in infrastructure layer
 *
 * ## Implementation Guidelines:
 * 1. **Transaction Management**: Repositories should participate in transactions but not manage them
 * 2. **Mapping**: Convert between domain entities and persistence models in the infrastructure layer
 * 3. **Query Methods**: Add specific query methods as needed (e.g., `findByStatus()`, `findExpired()`)
 * 4. **Avoid Generic Repositories**: Create specific repositories for each aggregate with domain-specific methods
 *
 * ## Example Implementation (Infrastructure Layer):
 * ```kotlin
 * class UserRepositoryImpl(
 *     private val r2dbcRepository: UserR2dbcRepository,
 *     private val mapper: UserMapper
 * ) : UserRepository {
 *     override suspend fun findById(id: UserId): User? {
 *         val entity = r2dbcRepository.findById(id.value).awaitSingleOrNull()
 *         return entity?.let { mapper.toDomain(it) }
 *     }
 *
 *     override suspend fun save(user: User) {
 *         val entity = mapper.toPersistence(user)
 *         r2dbcRepository.save(entity).awaitSingle()
 *     }
 * }
 * ```
 *
 * ## Aggregate Root Boundary:
 * - Only aggregate roots should have repositories
 * - Entities within an aggregate are accessed through the aggregate root
 * - If you need a repository for an entity, it might be an aggregate root itself
 *
 * ## Testing:
 * Repository interfaces enable easy testing through:
 * - Mock implementations for unit tests
 * - In-memory implementations for integration tests
 * - Test doubles that simulate various scenarios
 *
 * ## Why Marker Interface?
 * This interface serves as a semantic marker to:
 * - Clearly identify repository contracts in the codebase
 * - Enable architectural validation and static analysis
 * - Provide a common base for potential future extensions
 * - Document the architectural pattern being used
 */
public interface Repository
