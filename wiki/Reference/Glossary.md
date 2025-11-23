# Glossary

Comprehensive glossary of terms used in Explicit Architecture and this library.

---

## A

### Aggregate
A cluster of domain objects that can be treated as a single unit. An aggregate has a root entity (Aggregate Root) and a boundary that defines what is inside and outside the aggregate.

### Aggregate Root
The main entity within an aggregate that serves as the entry point for all operations. It enforces business invariants and coordinates changes within the aggregate. Only aggregate roots can be obtained directly via repositories.

**Example**: `Order` is an aggregate root that contains `OrderItems`.

### Application Layer
The layer that orchestrates domain logic and coordinates operations. Contains commands, queries, and their handlers. Depends on the domain layer but contains no business logic itself.

---

## B

### BaseDomainEvent
A base class provided by the library for domain events with enhanced metadata including `aggregateType`, `eventVersion`, `causationId`, and `correlationId`.

### Bounded Context
A central pattern in Domain-Driven Design that defines the boundaries within which a particular domain model is defined and applicable. Different bounded contexts can have different models for the same concept.

**Example**: "Customer" in Sales context vs "Customer" in Support context.

### Business Logic
The core rules and operations that define how the business operates. In Explicit Architecture, business logic lives in the domain layer (entities, aggregates, value objects).

---

## C

### Causation ID
An identifier that tracks which command or event caused a particular event. Used for debugging and understanding event chains.

### Clean Architecture
An architectural pattern by Robert C. Martin that emphasizes separation of concerns and dependency inversion. The domain layer is at the center and has no dependencies on outer layers.

### Command
An object representing an intent to change state in the system. Commands are named imperatively (e.g., `RegisterUser`, `PlaceOrder`) and are processed by command handlers.

### Command Bus
A dispatcher that routes commands to their appropriate handlers. Provides a central point for cross-cutting concerns like logging, validation, and transactions.

### Command Handler
A component that executes business logic in response to a command. Orchestrates the flow: load aggregate → call domain method → save aggregate → publish events.

### Command/Query Separation (CQS)
A principle that separates operations that change state (commands) from operations that retrieve data (queries). Commands modify state, queries return data.

### Consistency Boundary
The boundary within which data must be immediately consistent. In DDD, this is typically the aggregate boundary. Between aggregates, eventual consistency is acceptable.

### Correlation ID
An identifier that tracks related events across multiple aggregates or services. Used for distributed tracing and debugging.

### CQRS (Command Query Responsibility Segregation)
An architectural pattern that separates the read model from the write model. Commands use the domain model, queries use optimized read models.

---

## D

### Data Transfer Object (DTO)
An object that carries data between processes or layers. DTOs are simple data structures with no business logic, used in the presentation layer for API requests/responses.

### Domain
The subject area to which the application is applied. The domain is the sphere of knowledge and activity around which the application logic revolves.

### Domain-Driven Design (DDD)
An approach to software development that emphasizes collaboration between technical and domain experts to create a shared understanding of the domain and express it in code.

### Domain Event
An immutable object representing something significant that happened in the domain. Named in past tense (e.g., `UserRegistered`, `OrderPlaced`). Used to trigger side effects and maintain eventual consistency.

### Domain Event Handler
A component that reacts to domain events. Multiple handlers can subscribe to the same event. Handlers should be idempotent.

### Domain Event Publisher
A component that publishes domain events to external systems (message brokers, event buses, etc.). Typically used with the Transactional Outbox Pattern.

### Domain Layer
The core layer containing pure business logic. Includes entities, value objects, aggregates, domain events, and repository interfaces. Has no dependencies on other layers or frameworks.

### Dual-Write Problem
The challenge of atomically writing to two different systems (e.g., database and message broker). Solved by the Transactional Outbox Pattern.

---

## E

### Entity
A domain object with a unique identity that persists over time. Two entities are equal if they have the same ID, regardless of their attributes.

**Example**: `User`, `Order`, `Product`

### Event-Driven Architecture (EDA)
An architectural pattern where components communicate primarily through events. Enables loose coupling and eventual consistency.

### Event Sourcing
A pattern where state changes are stored as a sequence of events rather than just the current state. The current state can be reconstructed by replaying events.

### Eventual Consistency
A consistency model where data will eventually become consistent across all nodes, but may be temporarily inconsistent. Used between aggregates and distributed systems.

### Explicit Architecture
A synthesis of DDD, Clean Architecture, Hexagonal Architecture, CQRS, and EDA. Emphasizes explicit contracts, clear layer separation, and framework independence.

---

## F

### Framework-Agnostic
Code that doesn't depend on any specific framework (Spring, Ktor, etc.). The Structus library is framework-agnostic, working with any Kotlin framework or pure Kotlin.

---

## H

### Handler
A component that processes commands, queries, or events. See Command Handler, Query Handler, Domain Event Handler.

### Hexagonal Architecture (Ports and Adapters)
An architectural pattern that isolates the core business logic from external concerns. The core defines "ports" (interfaces), and external systems provide "adapters" (implementations).

---

## I

### Idempotency
The property that an operation can be applied multiple times without changing the result beyond the initial application. Event handlers should be idempotent.

**Example**: Processing the same event twice should have the same effect as processing it once.

### Identity
The unique identifier of an entity that distinguishes it from other entities. Identity persists over the entity's lifetime.

### Immutability
The property that an object's state cannot be modified after creation. Value objects should be immutable.

### Infrastructure Layer
The layer containing technical implementation details: database access, external API clients, message brokers, etc. Implements interfaces defined in the domain layer.

### Invariant
A business rule that must always be true. Aggregates enforce invariants within their consistency boundary.

**Example**: "An order's total must equal the sum of its items."

---

## L

### Lifecycle
The stages an entity goes through from creation to deletion. The library provides lifecycle methods: `markAsCreated`, `markAsUpdated`, `softDelete`, `restore`.

---

## M

### Message Outbox
A database table that stores domain events before they're published to external systems. Part of the Transactional Outbox Pattern.

### Message Outbox Repository
A repository interface for managing the message outbox. Provides methods to save, find, and mark events as published.

---

## O

### Optimistic Locking
A concurrency control technique where conflicts are detected at commit time rather than preventing them upfront. Uses version numbers to detect concurrent modifications.

### Outbox Message
A wrapper object containing a domain event and metadata for the Transactional Outbox Pattern.

### Outbox Publisher
A background process that reads unpublished events from the outbox and publishes them to external systems.

---

## P

### Persistence Ignorance
The principle that domain objects should not know how they're persisted. Repository interfaces live in the domain layer, implementations in the infrastructure layer.

### Presentation Layer
The outermost layer that handles external communication (HTTP, CLI, messaging). Contains controllers, request/response DTOs, and input validation.

### Projection
A read-optimized view of data. In CQRS, projections are built from events and used for queries.

---

## Q

### Query
An object representing a request for data. Queries are named as questions (e.g., `GetUserById`, `FindActiveOrders`) and are processed by query handlers.

### Query Handler
A component that retrieves data in response to a query. Query handlers can bypass the domain model for performance, using direct SQL or read models.

---

## R

### Read Model
A denormalized, query-optimized data structure used in CQRS. Updated by event handlers and queried directly without loading domain aggregates.

### Repository
An abstraction for data persistence that provides a collection-like interface. Repository interfaces live in the domain layer, implementations in the infrastructure layer.

### Result Type
A type that explicitly represents success or failure. The library uses Kotlin's standard `Result<T>` type for explicit error handling.

---

## S

### Saga
A pattern for managing long-running transactions across multiple aggregates or services. Coordinates a sequence of local transactions with compensating actions for failures.

### Separation of Concerns
The principle that different concerns (business logic, persistence, presentation) should be separated into distinct components or layers.

### Side Effect
An observable change outside a function's scope (database write, API call, event publication). Commands have side effects, queries should not.

### Soft Delete
Marking an entity as deleted without physically removing it from the database. The library provides `softDelete()` and `restore()` methods.

### Specification Pattern
A pattern for encapsulating business rules as reusable, composable objects. Useful for complex validation and query logic.

### Suspend Function
A Kotlin coroutine function that can be suspended and resumed. All I/O operations in the library use suspend functions for async/non-blocking execution.

---

## T

### Transactional Outbox Pattern
A pattern that ensures atomic writes to both a database and a message broker by storing events in the database first, then publishing them asynchronously.

### Transaction
A unit of work that either completely succeeds or completely fails. In Explicit Architecture, transactions are managed in the infrastructure layer.

---

## U

### Ubiquitous Language
A common language shared by developers and domain experts. The domain model should reflect this language.

### Unit of Work
A pattern that maintains a list of objects affected by a business transaction and coordinates writing changes. Often combined with repositories.

---

## V

### Value Object
An immutable object defined by its attributes rather than identity. Two value objects are equal if all their attributes are equal.

**Examples**: `Email`, `Money`, `Address`, `DateRange`

### Version
A number used for optimistic locking to detect concurrent modifications. The library provides `incrementVersion()` on aggregate roots.

---

## W

### Write Model
The domain model used for commands in CQRS. Optimized for enforcing business rules and maintaining consistency.

---

## Acronyms

- **ADR**: Architecture Decision Record
- **API**: Application Programming Interface
- **CDC**: Change Data Capture
- **CLI**: Command Line Interface
- **CQRS**: Command Query Responsibility Segregation
- **CQS**: Command/Query Separation
- **DDD**: Domain-Driven Design
- **DTO**: Data Transfer Object
- **EDA**: Event-Driven Architecture
- **E2E**: End-to-End
- **HTTP**: Hypertext Transfer Protocol
- **ID**: Identifier
- **JSON**: JavaScript Object Notation
- **ORM**: Object-Relational Mapping
- **REST**: Representational State Transfer
- **SQL**: Structured Query Language
- **UUID**: Universally Unique Identifier

---

## 📚 See Also

- **[Core Concepts](Core-Concepts.md)** - Understanding the fundamentals
- **[Architecture Overview](Architecture-Overview.md)** - How it all fits together
- **[FAQ](FAQ.md)** - Common questions
- **[API Reference](API-Reference.md)** - Complete API documentation
