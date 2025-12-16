---
sidebar_position: 1
pagination_next: reference/api-overview
pagination_prev: advanced/transactional-outbox
---

# Bonnes pratiques

Suivez ces directives pour tirer le meilleur parti de Structus et maintenir une architecture propre.

## Couche domaine

### Objets valeur

✅ **À faire**:
- Rendre les objets valeur immuables
- Valider dans le constructeur
- Utiliser des data classes
- Implémenter des opérations significatives

```kotlin
data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Email invalide" }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}
```

❌ **À éviter**:
- Rendre les objets valeur mutables
- Ignorer la validation
- Mettre la logique métier en dehors des objets valeur

### Agrégats racines

✅ **À faire**:
- Garder les agrégats petits
- Faire respecter les invariants
- Enregistrer les événements de domaine
- Utiliser des méthodes factory

```kotlin
class Order(override val id: OrderId) : AggregateRoot<OrderId>() {
    
    fun addItem(item: OrderItem) {
        require(status == OrderStatus.DRAFT) { "Impossible de modifier une commande confirmée" }
        items.add(item)
        recordEvent(ItemAddedEvent(id.value, item.productId))
    }
    
    companion object {
        fun create(customerId: String): Order {
            val order = Order(OrderId(UUID.randomUUID().toString()))
            order.recordEvent(OrderCreatedEvent(order.id.value, customerId))
            return order
        }
    }
}
```

❌ **À éviter**:
- Créer des agrégats dieux
- Ignorer l'enregistrement d'événements
- Permettre des transitions d'état invalides
- Exposer des collections mutables

### Repositories

✅ **À faire**:
- Définir les interfaces dans la couche domaine
- Utiliser une API semblable à une collection
- Retourner des objets du domaine
- Utiliser des fonctions suspend

```kotlin
interface OrderRepository : Repository {
    suspend fun findById(id: OrderId): Order?
    suspend fun save(order: Order)
    suspend fun delete(id: OrderId)
}
```

❌ **À éviter**:
- Mettre l'implémentation dans la couche domaine
- Retourner des DTOs depuis les repositories
- Utiliser des opérations bloquantes
- Exposer des détails de base de données

## Couche application

### Commandes

✅ **À faire**:
- Valider dans le constructeur
- Utiliser des noms descriptifs
- Les garder simples
- Les rendre immuables

```kotlin
data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemDto>
) : Command {
    init {
        require(customerId.isNotBlank()) { "ID client requis" }
        require(items.isNotEmpty()) { "La commande doit avoir des articles" }
    }
}
```

❌ **À éviter**:
- Mettre la logique métier dans les commandes
- Utiliser des noms de commande génériques
- Rendre les commandes mutables
- Ignorer la validation

### Gestionnaires de commandes

✅ **À faire**:
- Suivre la responsabilité unique
- Utiliser le type Result
- Effacer les événements après publication
- Gérer les erreurs avec élégance

```kotlin
class CreateOrderHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        return runCatching {
            val order = Order.create(command.customerId)
            command.items.forEach { order.addItem(it.toDomain()) }
            
            orderRepository.save(order)
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

❌ **À éviter**:
- Mettre la logique de domaine dans les gestionnaires
- Oublier d'effacer les événements
- Avaler les exceptions
- Mélanger plusieurs responsabilités

### Requêtes

✅ **À faire**:
- Garder les requêtes simples
- Utiliser des DTOs spécifiques
- Optimiser pour la lecture
- Contourner le modèle de domaine quand approprié

```kotlin
data class GetOrderQuery(val orderId: String) : Query

class GetOrderHandler(
    private val database: Database
) : QueryHandler<GetOrderQuery, OrderDto?> {
    
    override suspend operator fun invoke(query: GetOrderQuery): OrderDto? {
        return database.queryForObject(
            "SELECT * FROM orders WHERE id = ?",
            query.orderId
        )?.let { mapToDto(it) }
    }
}
```

❌ **À éviter**:
- Modifier l'état dans les requêtes
- Utiliser le modèle de domaine pour les requêtes
- Retourner des entités depuis les requêtes
- Sur-compliquer les modèles de lecture

## Couche infrastructure

### Implémentation du Repository

✅ **À faire**:
- Mapper entre domaine et modèles de persistance
- Gérer les transactions correctement
- Utiliser les fonctionnalités de base de données appropriées
- Journaliser les erreurs

```kotlin
class OrderRepositoryImpl(
    private val database: Database
) : OrderRepository {
    
    override suspend fun save(order: Order) {
        database.transaction {
            val model = order.toPersistenceModel()
            execute("INSERT INTO orders (...) VALUES (...)", model)
        }
    }
    
    override suspend fun findById(id: OrderId): Order? {
        return database.queryForObject(
            "SELECT * FROM orders WHERE id = ?",
            id.value
        )?.toDomainModel()
    }
}
```

❌ **À éviter**:
- Exposer les détails de base de données au domaine
- Ignorer la gestion des erreurs
- Oublier les transactions
- Retourner des modèles de persistance

## Gestion des événements

### Événements de domaine

✅ **À faire**:
- Utiliser des noms au passé
- Inclure toutes les données pertinentes
- Rendre les événements immuables
- Versionner vos événements

```kotlin
data class OrderCreatedEvent(
    override val aggregateId: String,
    val customerId: String,
    val createdAt: Instant
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "Order",
    eventVersion = 1
)
```

❌ **À éviter**:
- Utiliser le présent
- Inclure des données mutables
- Oublier le versionnement d'événements
- Mettre la logique métier dans les événements

### Outbox transactionnel

✅ **À faire**:
- Sauvegarder les événements dans la même transaction
- Effacer les événements après sauvegarde
- Gérer les échecs de publication
- Implémenter une logique de nouvelle tentative

```kotlin
override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        database.transaction {
            val order = Order.create(command.customerId)
            orderRepository.save(order)
            
            // Sauvegarder dans l'outbox dans la même transaction
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

❌ **À éviter**:
- Publier les événements directement
- Oublier d'effacer les événements
- Ignorer la logique de nouvelle tentative
- Ignorer les échecs de publication

## Tests

### Tests unitaires

✅ **À faire**:
- Tester la logique métier isolément
- Utiliser des mocks pour les dépendances
- Tester les cas limites
- Garder les tests simples

```kotlin
@Test
fun `devrait créer une commande avec des articles valides`() = runTest {
    val command = CreateOrderCommand("CUST-1", listOf(item))
    val handler = CreateOrderHandler(mockRepo, mockOutbox)
    
    val result = handler(command)
    
    assertTrue(result.isSuccess)
    verify(mockRepo).save(any())
}
```

❌ **À éviter**:
- Tester le code de framework
- Utiliser des bases de données réelles dans les tests unitaires
- Tester plusieurs choses à la fois
- Ignorer les cas d'erreur

## Directives générales

### Conventions de nommage

- **Commandes**: Impératif (CreateOrder, CancelOrder)
- **Requêtes**: Question (GetOrderById, FindActiveOrders)
- **Événements**: Passé (OrderCreated, OrderCancelled)
- **Gestionnaires**: CommandName + Handler (CreateOrderHandler)

### Gestion des erreurs

✅ **À faire**:
- Utiliser le type Result pour les erreurs attendues
- Utiliser des exceptions pour les erreurs inattendues
- Fournir des messages d'erreur significatifs
- Journaliser les erreurs de manière appropriée

❌ **À éviter**:
- Utiliser des exceptions pour le flux de contrôle
- Avaler les exceptions
- Retourner null pour les erreurs
- Utiliser des messages d'erreur génériques

### Dépendances

✅ **À faire**:
- Injecter les dépendances via le constructeur
- Dépendre des interfaces, pas des implémentations
- Garder les dépendances minimales
- Utiliser l'injection de dépendance

❌ **À éviter**:
- Utiliser des localisateurs de services
- Créer des dépendances à l'intérieur des classes
- Dépendre d'implémentations concrètes
- Avoir des dépendances circulaires

## Performance

### Conseils d'optimisation

1. **Utiliser des projections**: Créer des modèles de lecture optimisés
2. **Mettre en cache les requêtes**: Mettre en cache les données fréquemment accédées
3. **Opérations par lots**: Traiter les événements par lots
4. **Indexer judicieusement**: Ajouter des index de base de données pour les requêtes
5. **Surveiller**: Suivre les métriques de performance

### Pièges courants

❌ Éviter:
- Problèmes de requête N+1
- Charger des agrégats entiers pour les requêtes
- Publication d'événements synchrone
- Index de base de données manquants
- Sur-extraction de données