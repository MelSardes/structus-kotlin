# AI Prompt: Create a New Aggregate

Use this prompt template when you need to create a new aggregate in your domain.

## Prompt Template

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) to build a [describe your application].

Please help me create a new aggregate called [AggregateName] with the following requirements:

**Domain Requirements:**
- [Describe the business entity and its purpose]
- [List the key attributes/fields needed]
- [Describe any value objects that should be created]
- [List the business rules and invariants]

**Behavior:**
- [List the operations this aggregate should support]
- [Describe state transitions if applicable]
- [Mention any validation rules]

**Events:**
- [List domain events that should be published]

Please follow these Structus conventions:
1. Create the aggregate in package: com.example.domain.[aggregate]
2. Extend AggregateRoot<[Aggregate]Id>
3. Create value objects for complex types
4. Use immutable properties (val, not var)
5. Return Result<T> for operations that can fail
6. Include validation methods
7. Define domain events for state changes

Also create:
- Value object for the aggregate ID
- Any other value objects needed
- Domain events for key state changes
- Repository interfaces (CommandRepository and QueryRepository)
```

## Example Usage

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) to build an e-commerce platform.

Please help me create a new aggregate called Order with the following requirements:

**Domain Requirements:**
- Represents a customer order in the system
- Must have: order ID, customer ID, list of order lines, status, timestamps
- Order lines should be value objects containing: product ID, quantity, unit price
- Must track order total amount

**Behavior:**
- Create order in DRAFT status
- Confirm order (DRAFT → CONFIRMED)
- Ship order (CONFIRMED → SHIPPED)
- Deliver order (SHIPPED → DELIVERED)
- Cancel order (DRAFT or CONFIRMED → CANCELLED)
- Calculate total amount

**Events:**
- OrderCreated when order is first created
- OrderConfirmed when order is confirmed
- OrderShipped when order is shipped
- OrderDelivered when order is delivered
- OrderCancelled when order is cancelled

Please follow these Structus conventions:
1. Create the aggregate in package: com.example.domain.order
2. Extend AggregateRoot<OrderId>
3. Create value objects for complex types
4. Use immutable properties (val, not var)
5. Return Result<T> for operations that can fail
6. Include validation methods
7. Define domain events for state changes

Also create:
- Value object for the aggregate ID
- OrderLine value object
- Money value object
- Domain events for all state transitions
- Repository interfaces (OrderCommandRepository and OrderQueryRepository)
```

## What the AI Should Generate

1. **Aggregate Root Class**
   - Extends `AggregateRoot<[Aggregate]Id>`
   - Immutable properties
   - Business logic methods returning `Result<T>`
   - Validation methods

2. **Value Objects**
   - ID value object
   - Any complex type value objects
   - All implementing `ValueObject` interface

3. **Domain Events**
   - One event per significant state change
   - Implementing `DomainEvent` interface
   - Past tense naming

4. **Repository Interfaces**
   - Command repository for writes
   - Query repository for reads
   - All methods returning `Result<T>`
   - All methods as `suspend` functions

## Related Prompts

- [Add Command](./add-command.md) - After creating aggregate
- [Add Query](./add-query.md) - After creating aggregate
- [Implement Repository](./implement-repository.md) - After defining interfaces

## Reference

- [Library Overview](../library-overview.md)
- [Usage Patterns](../usage-patterns.md)
- [Code Templates].(./code-templates.md) - Template 1
