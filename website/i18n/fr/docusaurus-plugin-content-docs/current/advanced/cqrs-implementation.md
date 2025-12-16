---
sidebar_position: 1
pagination_next: advanced/transactional-outbox
pagination_prev: architecture/overview
---

# Implémentation CQRS

Comment mettre en œuvre le pattern CQRS (Command Query Responsibility Segregation) avec Structus.

## Qu'est-ce que CQRS ?

CQRS (Command Query Responsibility Segregation) est un pattern qui sépare les opérations de lecture (Queries) des opérations d'écriture (Commands):

- **Commands**: Modifient l'état mais ne retournent pas de données
- **Queries**: Retournent des données mais ne modifient pas l'état

Cette séparation permet d'optimiser indépendamment les chemins de lecture et d'écriture.

## Avantages de CQRS

- **Séparation des préoccupations**: Code plus clair et plus facile à maintenir
- **Optimisation indépendante**: Optimisez les lectures sans affecter les écritures
- **Scalabilité**: Mettez à l'échelle les lectures et écritures séparément
- **Modèles adaptés**: Utilisez des modèles optimisés pour chaque cas d'utilisation
- **Support pour Event Sourcing**: Se combine bien avec l'Event Sourcing

## Implémentation basique

### Côté Commande (Écriture)

Les commandes représentent une intention de modification:

```kotlin
interface Command

data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemDto>
) : Command
```

Les gestionnaires de commandes contiennent la logique pour traiter une commande:

```kotlin
interface CommandHandler<in C : Command, out R> {
    suspend operator fun invoke(command: C): R
}

class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        return runCatching {
            // Créer l'ordre à partir de la commande
            val order = Order.create(CustomerId(command.customerId))
            
            // Ajouter les éléments
            command.items.forEach { itemDto ->
                val item = OrderItem(
                    productId = ProductId(itemDto.productId),
                    quantity = itemDto.quantity,
                    unitPrice = Money(itemDto.price, itemDto.currency)
                )
                order.addItem(item)
            }
            
            // Sauvegarder l'agrégat
            orderRepository.save(order)
            
            // Sauvegarder les événements
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

### Côté Requête (Lecture)

Les requêtes représentent une demande de données:

```kotlin
interface Query

data class GetOrderByIdQuery(val orderId: String) : Query
```

Les gestionnaires de requêtes traitent les requêtes et retournent des données:

```kotlin
interface QueryHandler<in Q : Query, out R> {
    suspend operator fun invoke(query: Q): R
}

class GetOrderByIdQueryHandler(
    private val orderRepository: OrderQueryRepository
) : QueryHandler<GetOrderByIdQuery, OrderDto?> {
    
    override suspend operator fun invoke(query: GetOrderByIdQuery): OrderDto? {
        val orderId = OrderId(query.orderId)
        val order = orderRepository.findById(orderId) ?: return null
        
        return OrderDto(
            id = order.id.value,
            customerId = order.customerId.value,
            status = order.status.name,
            items = order.items.map { item ->
                OrderItemDto(
                    productId = item.productId.value,
                    quantity = item.quantity,
                    price = item.unitPrice.amount,
                    currency = item.unitPrice.currency
                )
            },
            totalAmount = order.totalAmount().amount,
            currency = order.totalAmount().currency,
            createdAt = order.createdAt
        )
    }
}
```

## CQRS avancé avec modèles séparés

Dans une implémentation CQRS plus avancée, on peut utiliser des modèles complètement différents pour la lecture et l'écriture.

### Modèle d'écriture (Domaine)

Le modèle d'écriture est axé sur la logique métier et les règles du domaine:

```kotlin
// Modèle d'écriture - Utilisé par les commandes
class Order(override val id: OrderId) : AggregateRoot<OrderId>() {
    private val _items = mutableListOf<OrderItem>()
    val items: List<OrderItem> get() = _items.toList()
    lateinit var customerId: CustomerId
    var status: OrderStatus = OrderStatus.DRAFT
    
    fun addItem(item: OrderItem) {
        require(status == OrderStatus.DRAFT) { "Ne peut ajouter des articles qu'à une commande brouillon" }
        _items.add(item)
        recordEvent(OrderItemAddedEvent(id.value, item.productId.value))
    }
    
    fun confirm() {
        require(status == OrderStatus.DRAFT) { "Ne peut confirmer qu'une commande brouillon" }
        require(_items.isNotEmpty()) { "Ne peut confirmer une commande vide" }
        status = OrderStatus.CONFIRMED
        recordEvent(OrderConfirmedEvent(id.value))
    }
    
    fun totalAmount(): Money {
        if (_items.isEmpty()) return Money(0.0, "EUR")
        
        val currency = _items.first().unitPrice.currency
        val total = _items.sumOf { it.quantity * it.unitPrice.amount }
        return Money(total, currency)
    }
}
```

### Modèle de lecture

Le modèle de lecture est optimisé pour les requêtes:

```kotlin
// Modèle de lecture - Utilisé par les requêtes
data class OrderReadModel(
    val id: String,
    val customerId: String,
    val customerName: String,  // Dénormalisation pour les performances
    val status: String,
    val items: List<OrderItemReadModel>,
    val totalAmount: Double,
    val currency: String,
    val createdAt: Instant
)

data class OrderItemReadModel(
    val productId: String,
    val productName: String,  // Dénormalisation
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)
```

### Repository séparé pour les requêtes

```kotlin
// Repository optimisé pour les requêtes
interface OrderQueryRepository {
    suspend fun findById(id: String): OrderReadModel?
    suspend fun findByCustomerId(customerId: String): List<OrderReadModel>
    suspend fun findByStatus(status: String): List<OrderReadModel>
    suspend fun search(criteria: OrderSearchCriteria): List<OrderReadModel>
}

// Implémentation avec accès direct à la base de données
class OrderQueryRepositoryImpl(private val jdbcTemplate: JdbcTemplate) : OrderQueryRepository {
    override suspend fun findById(id: String): OrderReadModel? {
        return jdbcTemplate.queryForObject("""
            SELECT o.id, o.customer_id, c.name as customer_name, o.status, o.created_at,
                   o.total_amount, o.currency
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            WHERE o.id = ?
        """, id) { rs, _ ->
            val orderId = rs.getString("id")
            val items = getOrderItems(orderId)
            
            OrderReadModel(
                id = orderId,
                customerId = rs.getString("customer_id"),
                customerName = rs.getString("customer_name"),
                status = rs.getString("status"),
                items = items,
                totalAmount = rs.getDouble("total_amount"),
                currency = rs.getString("currency"),
                createdAt = rs.getTimestamp("created_at").toInstant()
            )
        }
    }
    
    private suspend fun getOrderItems(orderId: String): List<OrderItemReadModel> {
        // Requête SQL pour obtenir les éléments
    }
}
```

## Synchronisation des modèles

La synchronisation entre les modèles d'écriture et de lecture se fait généralement via des événements:

1. Un gestionnaire de commandes modifie le modèle d'écriture
2. Des événements de domaine sont générés
3. Des projections écoutent ces événements et mettent à jour le modèle de lecture

```kotlin
class OrderProjection(private val database: Database) {
    suspend fun on(event: OrderCreatedEvent) {
        database.execute("""
            INSERT INTO order_read_model (
                id, customer_id, status, total_amount, currency, created_at
            ) VALUES (?, ?, ?, ?, ?, ?)
        """, 
            event.orderId,
            event.customerId,
            "DRAFT",
            0.0,
            "EUR",
            event.occurredAt
        )
    }
    
    suspend fun on(event: OrderItemAddedEvent) {
        // Mise à jour du modèle de lecture quand un article est ajouté
    }
    
    suspend fun on(event: OrderConfirmedEvent) {
        // Mise à jour du modèle de lecture quand une commande est confirmée
    }
}
```

## Gestion des transactions

Dans un système CQRS, la gestion des transactions est importante:

- **Commandes**: Utilisez une consistance forte (transactions ACID)
- **Requêtes**: Peuvent utiliser une consistance éventuelle
- **Événements**: Assurez une publication fiable via le pattern Outbox Transactionnel

```kotlin
// Transaction côté commande
override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        database.transaction {  // Transaction ACID
            val order = Order.create(command.customerId)
            orderRepository.save(order)
            
            // Sauvegarde dans l'outbox en même transaction
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}

// Requête sans transaction (lecture seule)
override suspend operator fun invoke(query: GetOrderByIdQuery): OrderDto? {
    return orderQueryRepository.findById(query.orderId)
}
```

## Considérations de performances

CQRS permet d'optimiser indépendamment la lecture et l'écriture:

- **Côté écriture**: Focalisé sur la cohérence et l'intégrité des données
- **Côté lecture**: Optimisé pour la performance
  - Dénormalisation des données
  - Caching agressif
  - Vues matérialisées
  - Interrogation directe de la base de données

## Tests

CQRS facilite les tests car les commandes et requêtes sont testées séparément:

```kotlin
class CreateOrderCommandHandlerTest {
    @Test
    fun `devrait créer une commande avec succès`() = runTest {
        // Arrange
        val command = CreateOrderCommand("customer-1", listOf(item))
        val mockRepo = mock<OrderRepository>()
        val mockOutbox = mock<MessageOutboxRepository>()
        val handler = CreateOrderCommandHandler(mockRepo, mockOutbox)
        
        // Act
        val result = handler(command)
        
        // Assert
        assertTrue(result.isSuccess)
        verify(mockRepo).save(any())
        verify(mockOutbox, times(2)).save(any())
    }
}

class GetOrderByIdQueryHandlerTest {
    @Test
    fun `devrait retourner les détails de commande`() = runTest {
        // Arrange
        val query = GetOrderByIdQuery("order-1")
        val mockRepo = mock<OrderQueryRepository>()
        val handler = GetOrderByIdQueryHandler(mockRepo)
        
        // Configurer le mock pour retourner un modèle de lecture
        whenever(mockRepo.findById("order-1")).thenReturn(orderReadModel)
        
        // Act
        val result = handler(query)
        
        // Assert
        assertNotNull(result)
        assertEquals("order-1", result?.id)
    }
}
```

## Modèles hybrides

Pour les systèmes plus petits, un modèle CQRS complet peut être excessif. Vous pouvez utiliser un modèle hybride:

```kotlin
// Repository commun avec méthodes séparées
interface OrderRepository : Repository {
    // Méthodes de commande
    suspend fun save(order: Order)
    suspend fun delete(orderId: OrderId)
    
    // Méthodes de requête
    suspend fun findById(orderId: OrderId): Order?
    suspend fun findAll(): List<Order>
    suspend fun findByCustomer(customerId: CustomerId): List<Order>
}
```

## Résumé

L'implémentation CQRS avec Structus:

1. **Séparer clairement** les commandes et les requêtes
2. **Utiliser des DTOs** pour les transferts de données
3. **Retourner `Result<T>`** depuis les gestionnaires de commandes
4. **Considérer des modèles séparés** pour les systèmes plus grands
5. **Synchroniser via des événements** en utilisant le pattern Outbox

## Prochaines étapes

- [Pattern Outbox Transactionnel](transactional-outbox) - Comment publier des événements de manière fiable
- [Bonnes pratiques](../best-practices/guidelines) - Meilleures pratiques pour utiliser Structus