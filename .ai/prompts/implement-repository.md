# AI Prompt: Implement Repository

Use this prompt template when you need to implement repository interfaces for persistence.

## Prompt Template

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) and have defined repository interfaces for [AggregateName].

Please help me implement these repositories using [Spring Data R2DBC / Exposed / JDBC / etc.] with the following requirements:

**Technology Stack:**
- Framework: [Spring Boot / Ktor / Micronaut / etc.]
- Database: [PostgreSQL / MySQL / MongoDB / etc.]
- Persistence: [R2DBC / JDBC / Exposed / etc.]

**Repository Interfaces to Implement:**
- [List the repository interfaces from domain layer]

**Database Schema:**
- Table name: [table_name]
- Columns: [list columns and types]
- Indexes: [list any indexes needed]
- Relationships: [describe any foreign keys]

**Special Requirements:**
- [Mention any complex queries needed]
- [Describe transaction requirements]
- [Note any performance optimizations]

Please follow these conventions:
1. Create implementations in package: com.example.infrastructure.persistence
2. Implement the repository interfaces from domain layer
3. Use suspend functions for all operations
4. Return Result<T> wrapping success or failure
5. Handle database exceptions and convert to DomainError
6. Use transactions where appropriate
7. Map between domain objects and database entities

Also create:
- Database entity classes (if needed)
- Mapper functions between domain and database entities
- Spring Data repository interfaces (if using Spring)
- Migration scripts (if needed)
```

## Example Usage

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) and have defined repository interfaces for Order.

Please help me implement these repositories using Spring Data R2DBC with the following requirements:

**Technology Stack:**
- Framework: Spring Boot 3.2
- Database: PostgreSQL 15
- Persistence: Spring Data R2DBC with Kotlin Coroutines

**Repository Interfaces to Implement:**
- OrderCommandRepository (save, update, delete)
- OrderQueryRepository (findById, findByCustomerId, findByStatus, findAll, count)

**Database Schema:**
- Table name: orders
- Columns:
  - id (UUID, primary key)
  - customer_id (VARCHAR)
  - status (VARCHAR)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)
- Table name: order_lines
- Columns:
  - id (UUID, primary key)
  - order_id (UUID, foreign key)
  - product_id (VARCHAR)
  - quantity (INT)
  - unit_price (DECIMAL)
  - currency (VARCHAR)

**Special Requirements:**
- Use transactions for save/update operations
- Join order_lines when fetching orders
- Support pagination for findAll
- Handle optimistic locking with version field

Please follow these conventions:
1. Create implementations in package: com.example.infrastructure.persistence
2. Implement the repository interfaces from domain layer
3. Use suspend functions for all operations
4. Return Result<T> wrapping success or failure
5. Handle database exceptions and convert to DomainError
6. Use transactions where appropriate
7. Map between domain objects and database entities

Also create:
- OrderTable and OrderLineTable entity classes
- Mapper functions (toDomain, toTable)
- Spring Data R2dbcRepository interfaces
- Flyway migration script
```

## What the AI Should Generate

1. **Database Entity Classes**
   ```kotlin
   @Table("orders")
   data class OrderTable(
       @Id val id: UUID,
       val customerId: String,
       val status: String,
       val createdAt: Instant,
       val updatedAt: Instant?
   )
   ```

2. **Spring Data Repository Interface** (if using Spring)
   ```kotlin
   interface OrderR2dbcRepository : CoroutineCrudRepository<OrderTable, UUID> {
       suspend fun findByCustomerId(customerId: String): Flow<OrderTable>
       suspend fun findByStatus(status: String): Flow<OrderTable>
   }
   ```

3. **Repository Implementation**
   ```kotlin
   @Repository
   class OrderCommandRepositoryImpl(
       private val r2dbcRepository: OrderR2dbcRepository,
       private val orderLineRepository: OrderLineR2dbcRepository
   ) : OrderCommandRepository {
       
       @Transactional
       override suspend fun save(order: Order): Result<Unit> {
           return try {
               val orderTable = order.toTable()
               r2dbcRepository.save(orderTable)
               
               val orderLines = order.lines.map { it.toTable(order.id) }
               orderLineRepository.saveAll(orderLines).collect()
               
               Result.Success(Unit)
           } catch (e: Exception) {
               Result.Failure(DomainError.PersistenceError(e.message ?: "Save failed"))
           }
       }
   }
   ```

4. **Mapper Functions**
   ```kotlin
   fun Order.toTable() = OrderTable(
       id = id.value,
       customerId = customerId,
       status = status.name,
       createdAt = createdAt,
       updatedAt = updatedAt
   )
   
   fun OrderTable.toDomain(lines: List<OrderLine>) = Order(
       id = OrderId(id),
       customerId = customerId,
       lines = lines,
       status = Order.OrderStatus.valueOf(status),
       createdAt = createdAt,
       updatedAt = updatedAt
   )
   ```

5. **Migration Script** (Flyway/Liquibase)
   ```sql
   CREATE TABLE orders (
       id UUID PRIMARY KEY,
       customer_id VARCHAR(255) NOT NULL,
       status VARCHAR(50) NOT NULL,
       created_at TIMESTAMP NOT NULL,
       updated_at TIMESTAMP
   );
   
   CREATE TABLE order_lines (
       id UUID PRIMARY KEY,
       order_id UUID NOT NULL REFERENCES orders(id),
       product_id VARCHAR(255) NOT NULL,
       quantity INT NOT NULL,
       unit_price DECIMAL(19, 4) NOT NULL,
       currency VARCHAR(3) NOT NULL
   );
   
   CREATE INDEX idx_orders_customer_id ON orders(customer_id);
   CREATE INDEX idx_orders_status ON orders(status);
   CREATE INDEX idx_order_lines_order_id ON order_lines(order_id);
   ```

## Technology-Specific Patterns

### Spring Data R2DBC
- Use `CoroutineCrudRepository`
- Use `@Transactional` for transactions
- Use `Flow<T>` for streaming results
- Handle `DataAccessException`

### Exposed (JetBrains)
- Define table objects extending `Table`
- Use `transaction { }` blocks
- Use `ResultRow.toDomain()` mappers
- Handle `SQLException`

### JDBC (Synchronous)
- Use `JdbcTemplate` or similar
- Wrap in `withContext(Dispatchers.IO)`
- Use `PreparedStatement` for queries
- Handle `SQLException`

## Error Handling

Map database exceptions to domain errors:

```kotlin
try {
    // Database operation
} catch (e: DataIntegrityViolationException) {
    Result.Failure(DomainError.EntityAlreadyExists("Duplicate key"))
} catch (e: DataAccessException) {
    Result.Failure(DomainError.PersistenceError(e.message ?: "Database error"))
} catch (e: Exception) {
    Result.Failure(DomainError.UnexpectedError(e.message ?: "Unexpected error"))
}
```

## Related Prompts

- [Create Aggregate](./create-aggregate.md) - Define domain first
- [Add Command](./add-command.md) - Commands use repositories
- [Add Query](./add-query.md) - Queries use repositories

## Reference

- [Library Overview](../library-overview.md)
- [Usage Patterns](../usage-patterns.md) - Pattern 4
- [Integration Guide](../integration-guide.md) - Framework-specific examples
