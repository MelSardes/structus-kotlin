---
sidebar_position: 1
pagination_next: advanced/cqrs-implementation
pagination_prev: getting-started/core-concepts
---

# Vue d'ensemble de l'architecture

Structus implémente une Architecture Explicite, qui est une synthèse de plusieurs patterns architecturaux complémentaires.

## Architecture Explicite

L'Architecture Explicite combine plusieurs concepts populaires:

- Architecture Hexagonale (Ports & Adaptateurs)
- Architecture en Oignon
- Architecture Propre (Clean Architecture)
- Domain-Driven Design (DDD)
- CQRS (Command Query Responsibility Segregation)
- Architecture Orientée Événements (EDA)

Ce qui en résulte est un système avec des frontières claires entre les couches et une séparation des préoccupations.

## Structure des couches

Structus organise le code en quatre couches principales:

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

### Règle de dépendance

La règle fondamentale est que les dépendances ne peuvent pointer que vers l'intérieur. Les couches intérieures ne savent rien des couches extérieures.

## Couche Domaine

C'est le cœur de votre application. Elle contient:

- **Entités**: Objets avec identité (User, Order)
- **Objets Valeur**: Objets immuables sans identité (Email, Address)
- **Agrégats**: Grappes d'objets avec une racine (OrderAggregate)
- **Événements de domaine**: Faits passés dans le domaine (OrderCreated)
- **Interfaces Repository**: Contrats pour la persistance
- **Services de domaine**: Logique qui ne rentre pas dans les entités

La couche domaine est **pure** - elle ne dépend d'aucun framework ni bibliothèque externe.

```kotlin
// Exemple de couche domaine
package com.example.domain.order

data class OrderId(val value: String) : ValueObject

class Order(override val id: OrderId) : AggregateRoot<OrderId>() {
    private val _items = mutableListOf<OrderItem>()
    val items: List<OrderItem> = _items
    
    fun addItem(item: OrderItem) {
        _items.add(item)
        recordEvent(OrderItemAddedEvent(id.value, item.productId))
    }
}

interface OrderRepository : Repository {
    suspend fun findById(id: OrderId): Order?
    suspend fun save(order: Order)
    suspend fun delete(id: OrderId)
}
```

## Couche Application

La couche application coordonne les flux de travail de haut niveau. Elle contient:

- **Commandes**: Demandes de modifications (CreateOrderCommand)
- **Gestionnaires de commandes**: Traitement des commandes (CreateOrderCommandHandler)
- **Requêtes**: Demandes de données (GetOrderQuery)
- **Gestionnaires de requêtes**: Traitement des requêtes (GetOrderQueryHandler)
- **DTOs internes**: Structures de données pour la couche application

La couche application coordonne les flux de travail mais délègue la logique métier à la couche domaine.

```kotlin
// Exemple de couche application
package com.example.application.commands

data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemDto>
) : Command

class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        return runCatching {
            // Vérifier que le client existe
            val customerId = CustomerId(command.customerId)
            val customer = customerRepository.findById(customerId)
                ?: return Result.failure(NoSuchElementException("Client non trouvé"))
                
            // Créer l'ordre
            val order = Order.create(customerId)
            
            // Ajouter les éléments
            command.items.forEach { itemDto ->
                val item = OrderItem(
                    productId = itemDto.productId,
                    quantity = itemDto.quantity,
                    price = Money(itemDto.price, itemDto.currency)
                )
                order.addItem(item)
            }
            
            // Sauvegarder l'ordre
            orderRepository.save(order)
            
            // Sauvegarder les événements dans l'outbox
            order.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // Effacer les événements
            order.clearEvents()
            
            order.id
        }
    }
}
```

## Couche Présentation

La couche présentation est responsable de l'interaction avec le monde extérieur. Elle contient:

- **Contrôleurs**: Points d'entrée HTTP ou autres
- **DTOs**: Structures de données pour l'API
- **Convertisseurs**: Mappers entre DTOs et commandes/requêtes
- **Validation**: Règles de validation d'entrée

Cette couche est mince et délègue tout travail à la couche application.

```kotlin
// Exemple de couche présentation
package com.example.presentation.controllers

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val createOrderHandler: CreateOrderCommandHandler,
    private val getOrderHandler: GetOrderQueryHandler
) {
    
    @PostMapping
    suspend fun createOrder(@RequestBody request: CreateOrderRequestDto): ResponseEntity<Any> {
        val command = CreateOrderCommand(
            customerId = request.customerId,
            items = request.items.map { it.toDto() }
        )
        
        return when (val result = createOrderHandler(command)) {
            is Result.Success -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapOf("id" to result.value.value))
            is Result.Failure -> ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to result.error.message))
        }
    }
    
    @GetMapping("/{id}")
    suspend fun getOrder(@PathVariable id: String): ResponseEntity<Any> {
        val query = GetOrderQuery(id)
        
        return when (val result = getOrderHandler(query)) {
            null -> ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build()
            else -> ResponseEntity.ok(result)
        }
    }
}
```

## Couche Infrastructure

La couche infrastructure fournit les détails techniques. Elle contient:

- **Implémentations Repository**: Accès à la base de données
- **Client API externes**: Communication avec d'autres systèmes
- **Configuration**: Configuration de framework
- **Services techniques**: Logging, caching, etc.

Cette couche est la seule à dépendre de frameworks spécifiques.

```kotlin
// Exemple de couche infrastructure
package com.example.infrastructure.persistence

@Repository
class OrderRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate
) : OrderRepository {
    
    override suspend fun findById(id: OrderId): Order? {
        // Implémentation JDBC
    }
    
    override suspend fun save(order: Order) {
        // Implémentation JDBC
    }
    
    override suspend fun delete(id: OrderId) {
        // Implémentation JDBC
    }
}
```

## Inversion de dépendance

Pour que la couche domaine reste pure, nous utilisons l'inversion de dépendance:

1. La couche domaine définit des interfaces (OrderRepository)
2. La couche infrastructure implémente ces interfaces (OrderRepositoryImpl)
3. La couche application utilise les interfaces, pas les implémentations

Cela permet à la couche domaine de rester indépendante des détails techniques.

## Communication entre couches

La communication entre les couches se fait via:

- **Commandes & Requêtes**: Pour les appels entrants
- **DTOs**: Pour transférer des données entre couches
- **Événements de domaine**: Pour la communication asynchrone

## Bonnes pratiques

- **Gardez le domaine pur**: Pas de frameworks ou bibliothèques externes
- **Validation dans les objets valeur**: Encapsulez les règles métier
- **Événements pour les effets secondaires**: Utilisez des événements au lieu d'appels directs
- **Repositories pour la persistance**: Cachez les détails de la base de données
- **CQRS pour la séparation**: Gardez la lecture et l'écriture séparées

## Prochaines étapes

- [Implémentation CQRS](../advanced/cqrs-implementation) - Plus de détails sur CQRS
- [Pattern Outbox Transactionnel](../advanced/transactional-outbox) - Garantir la livraison des événements
- [Bonnes pratiques](../best-practices/guidelines) - Guide des bonnes pratiques Structus