# AI Prompt: Add a Command and Handler

Use this prompt template when you need to add a new command to change state in your domain.

## Prompt Template

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) and have an existing aggregate called [AggregateName].

Please help me create a command to [describe the operation] with the following requirements:

**Operation:**
- [Describe what this command should do]
- [List the input parameters needed]
- [Describe the expected outcome]

**Business Rules:**
- [List validation rules]
- [List business constraints]
- [Describe error conditions]

**Side Effects:**
- [Describe what should be persisted]
- [List domain events that should be published]
- [Mention any external systems to notify]

Please follow these Structus conventions:
1. Create command in package: com.example.application.commands
2. Command should implement Command interface
3. Handler should implement CommandHandler<Command, ResultType>
4. Handler should be a suspend function
5. Return Result<T> (Success or Failure)
6. Validate inputs before creating domain objects
7. Publish domain events after successful persistence
8. Use repository interfaces from domain layer

Also create:
- Command data class
- CommandHandler class
- Domain event if needed
- Update repository interface if new methods are needed
```

## Example Usage

```
I'm using the Structus library (com.melsardes.libraries.structuskotlin) and have an existing aggregate called Order.

Please help me create a command to confirm an order with the following requirements:

**Operation:**
- Confirm an existing order (change status from DRAFT to CONFIRMED)
- Input: order ID
- Output: success or failure

**Business Rules:**
- Order must exist
- Order must be in DRAFT status
- Order must have at least one order line
- Order total must be greater than zero

**Side Effects:**
- Update order status to CONFIRMED
- Set confirmed timestamp
- Publish OrderConfirmedEvent
- Send confirmation email (mention in event)

Please follow these Structus conventions:
1. Create command in package: com.example.application.commands
2. Command should implement Command interface
3. Handler should implement CommandHandler<Command, ResultType>
4. Handler should be a suspend function
5. Return Result<T> (Success or Failure)
6. Validate inputs before creating domain objects
7. Publish domain events after successful persistence
8. Use repository interfaces from domain layer

Also create:
- ConfirmOrderCommand data class
- ConfirmOrderCommandHandler class
- OrderConfirmedEvent domain event
- Update OrderCommandRepository if needed
```

## What the AI Should Generate

1. **Command Class**
   ```kotlin
   data class [Action][Aggregate]Command(...) : Command
   ```

2. **Command Handler**
   ```kotlin
   class [Action][Aggregate]CommandHandler(
       private val commandRepository: [Aggregate]CommandRepository,
       private val queryRepository: [Aggregate]QueryRepository,
       private val eventPublisher: DomainEventPublisher
   ) : CommandHandler<[Action][Aggregate]Command, [ResultType]> {
       override suspend fun handle(command: [Action][Aggregate]Command): Result<[ResultType]> {
           // 1. Validate input
           // 2. Fetch/create domain object
           // 3. Apply business logic
           // 4. Persist changes
           // 5. Publish events
           // 6. Return result
       }
   }
   ```

3. **Domain Event** (if needed)
   ```kotlin
   data class [Aggregate][Action]Event(...) : DomainEvent
   ```

## Command Types

### Create Commands
- Return: Aggregate ID
- Example: `CreateOrderCommand` → `Result<OrderId>`

### Update Commands
- Return: Unit or updated aggregate ID
- Example: `ConfirmOrderCommand` → `Result<Unit>`

### Delete Commands
- Return: Unit
- Example: `CancelOrderCommand` → `Result<Unit>`

## Related Prompts

- [Create Aggregate](./create-aggregate.md) - Before adding commands
- [Add Query](./add-query.md) - For read operations
- [Add Event](./add-event.md) - For event handling

## Reference

- [Library Overview](../library-overview.md)
- [Usage Patterns](../usage-patterns.md) - Pattern 2
- [Code Templates](../code-templates.md) - Templates 2, 6
