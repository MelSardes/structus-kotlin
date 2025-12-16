---
sidebar_position: 2
pagination_next: best-practices/guidelines
pagination_prev: advanced/cqrs-implementation
---

# Pattern Outbox Transactionnel

Apprenez à publier des événements de domaine de manière fiable avec le Pattern Outbox Transactionnel.

## Le problème

Lorsque vous devez mettre à jour une base de données et publier des événements, vous faites face à un problème de transaction distribuée:

```kotlin
// ❌ Approche peu fiable
suspend fun createOrder(command: CreateOrderCommand): Result<OrderId> {
    return runCatching {
        val order = Order.create(command.customerId)
        orderRepository.save(order)  // Opération de base de données
        
        // Et si cela échoue? L'événement est perdu!
        eventPublisher.publish(order.domainEvents)  // Système externe
        
        order.id
    }
}
```

**Problèmes**:
- Si la publication d'événement échoue, les modifications de la base de données sont validées mais les événements sont perdus
- Si la base de données échoue après la publication, les événements sont envoyés mais les données sont incohérentes
- Pas d'atomicité entre la base de données et le courtier de messages

## La solution

Le **Pattern Outbox Transactionnel** résout ce problème en:

1. Sauvegardant les événements dans une table de base de données dans la même transaction que l'agrégat
2. Publiant les événements depuis la table outbox dans un processus séparé
3. Marquant les événements comme publiés après une livraison réussie

```
┌─────────────────────────────────────────┐
│     Gestionnaire de commandes           │
│  1. Sauvegarder l'agrégat               │
│  2. Sauvegarder les événements dans     │
│     l'outbox (même transaction)        │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│     Éditeur de l'Outbox                 │
│  1. Lire les événements non publiés     │
│  2. Publier vers le broker de messages  │
│  3. Marquer comme publiés               │
└─────────────────────────────────────────┘
```

## Implémentation

### 1. Définir le repository Outbox

```kotlin
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository
import com.melsardes.libraries.structuskotlin.domain.OutboxMessage

interface MessageOutboxRepository : Repository {
    suspend fun save(event: DomainEvent)
    suspend fun findUnpublished(limit: Int): List<OutboxMessage>
    suspend fun markAsPublished(messageId: String)
    suspend fun incrementRetryCount(messageId: String)
    suspend fun deletePublishedOlderThan(olderThanDays: Int): Int
    suspend fun findFailedEvents(maxRetries: Int): List<OutboxMessage>
}
```

### 2. Créer la table Outbox

```sql
CREATE TABLE message_outbox (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3. Utiliser dans le gestionnaire de commandes

```kotlin
class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository,
    private val database: Database
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(
        command: CreateOrderCommand
    ): Result<OrderId> {
        return runCatching {
            database.transaction {
                // 1. Créer et sauvegarder l'agrégat
                val order = Order.create(command.customerId, command.items)
                orderRepository.save(order)
                
                // 2. Sauvegarder les événements dans l'outbox (même transaction!)
                order.domainEvents.forEach { event ->
                    outboxRepository.save(event)
                }
                
                // 3. Effacer les événements de l'agrégat
                order.clearEvents()
                
                order.id
            }
        }
    }
}
```

### 4. Implémenter le Publisher de l'Outbox

```kotlin
class OutboxPublisher(
    private val outboxRepository: MessageOutboxRepository,
    private val eventPublisher: DomainEventPublisher,
    private val maxRetries: Int = 3
) {
    
    suspend fun publishPendingEvents() {
        val messages = outboxRepository.findUnpublished(limit = 100)
        
        messages.forEach { message ->
            try {
                // Publier vers le système externe
                val event = deserializeEvent(message.payload, message.eventType)
                eventPublisher.publish(event)
                
                // Marquer comme publié
                outboxRepository.markAsPublished(message.id)
                
            } catch (e: Exception) {
                // Incrémenter le compteur de tentatives
                outboxRepository.incrementRetryCount(message.id)
            }
        }
    }
}
```

### 5. Planifier le Publisher

```kotlin
// Utilisation des coroutines Kotlin
class OutboxScheduler(
    private val outboxPublisher: OutboxPublisher
) {
    
    fun start(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                try {
                    outboxPublisher.publishPendingEvents()
                } catch (e: Exception) {
                    logger.error("Erreur dans le publisher de l'outbox", e)
                }
                delay(5000) // Sonder toutes les 5 secondes
            }
        }
    }
}
```

## Avantages

✅ **Atomicité**: Les événements et les changements de données sont atomiques
✅ **Fiabilité**: Les événements ne sont jamais perdus
✅ **Cohérence**: La base de données et les événements restent synchronisés
✅ **Logique de nouvelle tentative**: Les publications échouées sont réessayées
✅ **Surveillance**: Suivi des événements non publiés

## Compromis

⚠️ **Cohérence éventuelle**: Les événements sont publiés de façon asynchrone
⚠️ **Complexité**: Infrastructure additionnelle nécessaire
⚠️ **Stockage**: La table outbox grandit avec le temps
⚠️ **Sondage**: Le publisher sonde la base de données

## Bonnes pratiques

### ✅ À faire

- Utiliser des transactions pour les sauvegardes d'agrégat + outbox
- Effacer les événements après sauvegarde dans l'outbox
- Implémenter une logique de nouvelle tentative avec backoff
- Surveiller le nombre d'événements non publiés
- Nettoyer les anciens événements publiés
- Gérer les événements échoués (file d'attente des lettres mortes)

### ❌ À éviter

- Publier des événements directement depuis les gestionnaires de commandes
- Oublier d'effacer les événements des agrégats
- Ignorer la gestion des erreurs dans le publisher
- Laisser la table outbox croître indéfiniment
- Ignorer les événements échoués

## Prochaines étapes

- **[Architecture Orientée Événements](event-driven)** - Construire avec des événements
- **[Gestion des erreurs](error-handling)** - Gérer les échecs
- **[Stratégies de test](../best-practices/testing-strategies)** - Modèles de test