---
sidebar_position: 3
pagination_next: architecture/overview
pagination_prev: getting-started/quick-start
---

# Concepts fondamentaux

Cette page explique les principes fondamentaux utilisés dans Structus et comment ils s'intègrent dans une architecture propre.

## Domain-Driven Design (DDD)

Le Domain-Driven Design est une approche qui place le domaine métier au centre du développement logiciel. Structus fournit les blocs de construction pour implémenter le DDD en Kotlin.

### Entité vs Objet Valeur

#### Entité

Une entité est un objet avec une identité qui persiste à travers le temps, même si ses attributs changent.

```kotlin
abstract class Entity<ID : Any>(open val id: ID)
```

**Caractéristiques des entités**:
- Identité unique (généralement via un ID)
- Peut être modifiée au fil du temps
- Égalité basée sur l'identité, pas sur les attributs

**Exemple**:
```kotlin
class User(override val id: UserId, var email: Email) : Entity<UserId>(id)
```

#### Objet Valeur

Un objet valeur est immuable et n'a pas d'identité. Il est défini entièrement par ses attributs.

```kotlin
interface ValueObject
```

**Caractéristiques des objets valeur**:
- Immuables (généralement des data classes)
- Pas d'identité distincte
- Égalité basée sur les attributs
- Validation dans le constructeur

**Exemple**:
```kotlin
data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Email invalide" }
    }
}
```

### Agrégat Racine

Un agrégat est un groupe d'objets traités comme une unité. L'agrégat racine est le point d'entrée de l'agrégat.

```kotlin
abstract class AggregateRoot<ID : Any> : Entity<ID> {
    private val _domainEvents = mutableListOf<DomainEvent>()
    val domainEvents: List<DomainEvent> get() = _domainEvents

    fun recordEvent(event: DomainEvent) {
        _domainEvents.add(event)
    }

    fun clearEvents() {
        _domainEvents.clear()
    }
}
```

**Caractéristiques des agrégats racine**:
- Maintien des invariants métier
- Point d'entrée pour toutes les modifications
- Enregistrement des événements de domaine
- Frontière de transaction

**Exemple**:
```kotlin
class Order(override val id: OrderId) : AggregateRoot<OrderId>() {
    private val _items = mutableListOf<OrderItem>()
    val items: List<OrderItem> get() = _items
    
    fun addItem(item: OrderItem) {
        _items.add(item)
        recordEvent(OrderItemAddedEvent(id.value, item.productId))
    }
}
```

### Événements de domaine

Les événements de domaine représentent quelque chose qui s'est produit dans le domaine.

```kotlin
interface DomainEvent {
    val eventId: String
    val occurredOn: Instant
    val aggregateId: String
    val eventType: String
}
```

**Caractéristiques des événements de domaine**:
- Immuables
- Décrivent un fait passé
- Contiennent toutes les informations pertinentes
- Nommés au passé (Created, Updated, Deleted)

**Exemple**:
```kotlin
data class OrderCreatedEvent(
    override val aggregateId: String,
    val customerId: String,
    override val occurredOn: Instant = Instant.now(),
    override val eventId: String = UUID.randomUUID().toString(),
    override val eventType: String = "OrderCreated"
) : DomainEvent
```

### Repository (Dépôt)

Un repository encapsule la logique pour récupérer et stocker des agrégats.

```kotlin
interface Repository<T : AggregateRoot<ID>, ID : Any>

interface CommandRepository<T : AggregateRoot<ID>, ID : Any> : Repository<T, ID>
interface QueryRepository<T : AggregateRoot<ID>, ID : Any> : Repository<T, ID>
```

**Caractéristiques des repositories**:
- Interface définie dans la couche domaine
- Implémentation dans la couche infrastructure
- Ressemble à une collection d'objets
- Cache les détails de la persistance

**Exemple**:
```kotlin
interface OrderRepository : Repository {
    suspend fun findById(id: OrderId): Order?
    suspend fun save(order: Order)
    suspend fun delete(id: OrderId)
}
```

## Command Query Separation (CQS)

CQS sépare les opérations qui modifient l'état (commandes) de celles qui lisent l'état (requêtes).

### Commande vs Requête (CQS)

#### Commande

Une commande est une intention de changer l'état du système.

```kotlin
interface Command
```

**Caractéristiques des commandes**:
- Modifie l'état
- Pas de retour de données (sauf confirmation)
- Nommée à l'impératif (CreateOrder, UpdateUser)
- Validée avant traitement

**Exemple**:
```kotlin
data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemDto>
) : Command
```

#### Requête

Une requête est une demande de données sans effet secondaire.

```kotlin
interface Query
```

**Caractéristiques des requêtes**:
- Lecture seule
- Retourne des données
- Pas d'effet secondaire
- Optimisée pour la lecture

**Exemple**:
```kotlin
data class GetOrderByIdQuery(val orderId: String) : Query
```

### Gestionnaires de commandes

Les gestionnaires de commandes contiennent la logique pour traiter une commande.

```kotlin
interface CommandHandler<C : Command, R> {
    suspend operator fun invoke(command: C): R
}
```

**Caractéristiques des gestionnaires de commandes**:
- Traitent une seule commande
- Contiennent la logique d'application
- Orchestrent les opérations de domaine
- Gèrent la persistance et les événements

**Exemple**:
```kotlin
class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        // Logique d'implémentation
    }
}
```

### Gestionnaires de requêtes

Les gestionnaires de requêtes traitent les requêtes et retournent des données.

```kotlin
interface QueryHandler<Q : Query, R> {
    suspend operator fun invoke(query: Q): R
}
```

**Caractéristiques des gestionnaires de requêtes**:
- Lecture seule
- Optimisés pour la performance
- Peuvent contourner le modèle de domaine
- Retournent des DTOs

**Exemple**:
```kotlin
class GetOrderByIdQueryHandler(
    private val orderRepository: OrderQueryRepository
) : QueryHandler<GetOrderByIdQuery, OrderDto?> {
    override suspend operator fun invoke(query: GetOrderByIdQuery): OrderDto? {
        // Logique d'implémentation
    }
}
```

## Event Publishing (Publication d'événements)

Les événements de domaine doivent être publiés de manière fiable.

### Message Outbox (Boîte de messages sortants)

Le pattern Outbox transactionnel garantit une publication fiable des événements.

```kotlin
interface MessageOutboxRepository : Repository {
    suspend fun save(event: DomainEvent)
    suspend fun findUnpublished(limit: Int): List<OutboxMessage>
    suspend fun markAsPublished(messageId: String)
}
```

**Caractéristiques du pattern Outbox**:
- Atomicité avec les changements de domaine
- Assurance de livraison des événements
- Cohérence éventuelle
- Transactions isolées

**Exemple**:
```kotlin
override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        database.transaction {
            // 1. Créer et sauvegarder l'agrégat
            val order = Order.create(...)
            orderRepository.save(order)
            
            // 2. Sauvegarder les événements dans l'outbox (même transaction)
            order.domainEvents.forEach { outboxRepository.save(it) }
            
            // 3. Effacer les événements de l'agrégat
            order.clearEvents()
            
            order.id
        }
    }
}
```

### Publication d'événements

Les événements sont publiés de manière asynchrone depuis l'outbox.

```kotlin
interface DomainEventPublisher {
    suspend fun publish(event: DomainEvent)
}
```

**Caractéristiques de la publication d'événements**:
- Asynchrone
- Idempotente
- Gestion des erreurs et réessais
- Livraison au moins une fois

## Architecture en couches

Structus encourage une architecture en couches propre:

```
┌─────────────────────────────────────────┐
│         Couche Présentation             │
│  (Contrôleurs, DTOs, APIs REST, etc.)   │
└─────────────────────────────────────────┘
              ↓ dépend de
┌─────────────────────────────────────────┐
│        Couche Application               │
│  (Commandes, Requêtes, Gestionnaires)   │
└─────────────────────────────────────────┘
              ↓ dépend de
┌─────────────────────────────────────────┐
│          Couche Domaine                 │
│  (Entités, Objets Valeur, Repositories) │
└─────────────────────────────────────────┘
              ↑ implémente
┌─────────────────────────────────────────┐
│      Couche Infrastructure              │
│  (Persistance, API externes, etc.)      │
└─────────────────────────────────────────┘
```

### Règles de dépendance

1. La couche domaine ne dépend de rien d'autre
2. La couche application dépend uniquement du domaine
3. La couche présentation dépend de l'application et du domaine
4. La couche infrastructure dépend de tout, mais est utilisée indirectement

Ces règles maintiennent la couche domaine pure et testable.

## Prochaines étapes

- [Architecture](../architecture/overview) - Vue d'ensemble de l'architecture
- [Implémentation CQRS](../advanced/cqrs-implementation) - Détails sur l'implémentation CQRS
- [Pattern Outbox Transactionnel](../advanced/transactional-outbox) - Fiabilité des événements