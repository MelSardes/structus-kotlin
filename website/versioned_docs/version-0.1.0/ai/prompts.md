---
sidebar_position: 5
---

# AI Prompts

Pre-written prompt templates for common tasks. Copy and customize these prompts when working with AI assistants.

## Prompt 1: Create Aggregate

```markdown
Using the Structus library (com.melsardes.libraries.structuskotlin), create a new aggregate for [ENTITY_NAME] with the following requirements:

**Domain Requirements:**
- Identity: [ID_TYPE] (e.g., UUID, String)
- Properties:
  - [property1]: [type]
  - [property2]: [type]
  - status: [STATUS_ENUM_VALUES]
- Business rules:
  - [rule 1]
  - [rule 2]
- Domain events:
  - [Event1]Created
  - [Event1]Updated

**Implementation:**
1. Create value objects for complex types
2. Extend AggregateRoot<ID>
3. Add business logic methods
4. Record domain events for state changes
5. Use immutable properties

Follow the patterns from the Structus documentation.
```

## Prompt 2: Add Command

```markdown
Using the Structus library, implement a command to [ACTION] a [ENTITY_NAME]:

**Command Requirements:**
- Command name: [Action][Entity]Command
- Input parameters:
  - [param1]: [type]
  - [param2]: [type]
- Validation rules:
  - [rule 1]
  - [rule 2]

**Handler Requirements:**
- Validate input
- Load/create aggregate
- Execute business logic
- Save to repository
- Publish events using Transactional Outbox Pattern
- Return Result<[RETURN_TYPE]>

Use suspend functions and follow CQRS patterns.
```

## Prompt 3: Add Query

```markdown
Using the Structus library, implement a query to retrieve [DATA_DESCRIPTION]:

**Query Requirements:**
- Query name: [Get/Find/List][Entity][Criteria]Query
- Input parameters:
  - [param1]: [type]
  - [param2]: [type]
- Return type: [DTO_NAME]

**Handler Requirements:**
- Query repository (read-only)
- Return DTO, not domain object
- Optimize for reading
- No state changes
- Use suspend function

Follow CQRS query patterns.
```

## Prompt 4: Implement Repository

```markdown
Using the Structus library, implement a repository for [ENTITY_NAME]:

**Repository Requirements:**
- Interface in domain layer
- Implementation in infrastructure layer
- Methods needed:
  - findById
  - save
  - delete
  - [custom query methods]

**Implementation:**
- All methods are suspend functions
- Map between domain and persistence models
- Handle transactions properly
- Return domain objects, not persistence models

Use [DATABASE_TYPE] for persistence.
```

## Prompt 5: Add Domain Event

```markdown
Using the Structus library, create a domain event for [EVENT_DESCRIPTION]:

**Event Requirements:**
- Event name: [Entity][Action]Event (past tense)
- Properties:
  - aggregateId: String
  - [property1]: [type]
  - [property2]: [type]
- Event version: 1

**Implementation:**
- Extend BaseDomainEvent
- Use immutable data class
- Include all relevant information
- Use past tense naming

The event should be published when [TRIGGER_CONDITION].
```

## Prompt 6: Implement CQRS Feature

```markdown
Using the Structus library, implement a complete CQRS feature for [FEATURE_NAME]:

**Requirements:**
- Write side (Command):
  - Command: [Action][Entity]Command
  - Handler: [Action][Entity]CommandHandler
  - Repository: [Entity]Repository
  - Events: [Entity][Action]Event
  
- Read side (Query):
  - Query: [Get/List][Entity]Query
  - Handler: [Get/List][Entity]QueryHandler
  - DTO: [Entity]Dto

**Business Rules:**
- [rule 1]
- [rule 2]

**Implementation:**
- Separate read and write models
- Use Transactional Outbox Pattern
- Follow clean architecture layers
- Return Result&lt;T&gt; from handlers

Provide complete implementation with all files.
```

## Prompt 7: Add Validation

```markdown
Using the Structus library, add validation to [ENTITY_NAME] for:

**Validation Rules:**
- [field1]: [validation rule]
- [field2]: [validation rule]
- Business invariants:
  - [invariant 1]
  - [invariant 2]

**Implementation:**
- Add validation in value object constructors
- Add validate() method to aggregate
- Return Result<Unit> from validation
- Use DomainError for failures

Validation should happen before state changes.
```

## Prompt 8: Integrate with Framework

```markdown
Using the Structus library, integrate with [FRAMEWORK_NAME]:

**Integration Requirements:**
- Framework: [Spring Boot / Ktor / etc.]
- Endpoints needed:
  - POST /[resource] - Create
  - GET /[resource]/{id} - Get by ID
  - PUT /[resource]/{id} - Update
  - DELETE /[resource]/{id} - Delete

**Implementation:**
- Create controllers/routes
- Map DTOs to commands/queries
- Handle Result&lt;T&gt; responses
- Return appropriate HTTP status codes
- Use dependency injection

Keep domain layer pure, framework code only in presentation layer.
```

## Usage Tips

### For Developers

1. **Copy the prompt** that matches your task
2. **Fill in the placeholders** with your specific requirements
3. **Paste to your AI assistant** (ChatGPT, Claude, Copilot, etc.)
4. **Review the generated code** against Structus patterns
5. **Iterate if needed** by providing more context

### For AI Assistants

When you receive these prompts:

1. **Read the context** from Library Overview and Usage Patterns
2. **Follow the templates** from Code Templates
3. **Apply the patterns** correctly
4. **Generate complete code** with all necessary imports
5. **Explain your choices** briefly

## Example Interaction

**Developer:**
```markdown
Using the Structus library, create a new aggregate for Order with:
- Identity: OrderId (UUID)
- Properties: customerId (String), items (List<OrderItem>), status (DRAFT/CONFIRMED/SHIPPED)
- Business rules: Can only confirm draft orders, must have at least one item
- Events: OrderCreated, OrderConfirmed
```

**AI Response:**
```kotlin
// [Generated code following Structus patterns]
```

## 📚 Related

- **[Library Overview](library-overview)** - Understand the architecture
- **[Usage Patterns](usage-patterns)** - See correct patterns
- **[Code Templates](code-templates)** - Ready-to-use templates

---

**Tip**: The more specific your prompt, the better the AI-generated code!
