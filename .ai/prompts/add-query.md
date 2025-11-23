# AI Prompt: Add a Query and Handler

Use this prompt template when you need to add a new query to read data from your domain.

## Prompt Template

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) and have an existing aggregate called [AggregateName].

Please help me create a query to [describe what data to retrieve] with the following requirements:

**Data to Retrieve:**
- [Describe what data should be returned]
- [List the input parameters/filters]
- [Specify the output format/DTO structure]

**Query Requirements:**
- [Describe filtering criteria]
- [Mention sorting requirements]
- [Specify pagination if needed]
- [List any aggregations or calculations]

**Performance Considerations:**
- [Mention if this is a frequently called query]
- [Specify if partial data is acceptable]
- [Note any caching requirements]

Please follow these Structus conventions:
1. Create query in package: com.example.application.queries
2. Query should implement Query<ResultType> interface
3. Handler should implement QueryHandler<Query, ResultType>
4. Handler should be a suspend function
5. Return Result<DTO> (not domain objects directly)
6. Use QueryRepository (read-only)
7. Never modify state in a query
8. Create DTOs for query results

Also create:
- Query data class
- QueryHandler class
- Result DTO(s)
- Update QueryRepository interface if new methods are needed
```

## Example Usage

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) and have an existing aggregate called Order.

Please help me create a query to retrieve order details by ID with the following requirements:

**Data to Retrieve:**
- Complete order information including all order lines
- Input: order ID
- Output: OrderDto with nested OrderLineDto objects

**Query Requirements:**
- Find order by ID
- Include all order lines
- Include calculated total amount
- Return 404 if order not found

**Performance Considerations:**
- This is a frequently called query
- Full order details are needed (no partial data)
- Consider caching at application level

Please follow these Structus conventions:
1. Create query in package: com.example.application.queries
2. Query should implement Query<ResultType> interface
3. Handler should implement QueryHandler<Query, ResultType>
4. Handler should be a suspend function
5. Return Result<DTO> (not domain objects directly)
6. Use QueryRepository (read-only)
7. Never modify state in a query
8. Create DTOs for query results

Also create:
- GetOrderByIdQuery data class
- GetOrderByIdQueryHandler class
- OrderDto and OrderLineDto classes
- Update OrderQueryRepository if needed
```

## What the AI Should Generate

1. **Query Class**
   ```kotlin
   data class [Action][Aggregate]Query(...) : Query<[ResultType]>
   ```

2. **Query Handler**
   ```kotlin
   class [Action][Aggregate]QueryHandler(
       private val repository: [Aggregate]QueryRepository
   ) : QueryHandler<[Action][Aggregate]Query, [ResultType]> {
       override suspend fun handle(query: [Action][Aggregate]Query): Result<[ResultType]> {
           // 1. Validate input
           // 2. Query repository
           // 3. Transform to DTO
           // 4. Return result
       }
   }
   ```

3. **Result DTO(s)**
   ```kotlin
   data class [Aggregate]Dto(...)
   ```

## Query Types

### Single Item Queries
- Input: ID or unique identifier
- Output: Single DTO
- Example: `GetOrderByIdQuery` → `Result<OrderDto>`

### List Queries
- Input: Filters, pagination
- Output: List of DTOs
- Example: `ListOrdersQuery` → `Result<List<OrderDto>>`

### Paginated Queries
- Input: Filters, limit, offset
- Output: Paginated result with metadata
- Example: `ListOrdersQuery` → `Result<OrderListResult>`

### Aggregation Queries
- Input: Filters, grouping criteria
- Output: Aggregated data
- Example: `GetOrderStatisticsQuery` → `Result<OrderStatistics>`

## DTO Design Guidelines

1. **Flat Structure**: Avoid deep nesting when possible
2. **String IDs**: Convert UUIDs to strings
3. **Formatted Dates**: Convert Instant to ISO-8601 strings
4. **Calculated Fields**: Include computed values
5. **No Domain Logic**: DTOs are pure data containers

## Related Prompts

- [Create Aggregate](./create-aggregate.md) - Before adding queries
- [Add Command](./add-command.md) - For write operations
- [Implement Repository](./implement-repository.md) - For query implementations

## Reference

- [Library Overview](../library-overview.md)
- [Usage Patterns](../usage-patterns.md) - Pattern 3
- [Code Templates](../code-templates.md) - Templates 3, 7
