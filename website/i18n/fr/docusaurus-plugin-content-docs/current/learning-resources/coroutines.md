---
sidebar_position: 1
---

# Guide des Coroutines Kotlin

Les Coroutines Kotlin sont une partie fondamentale des capacités de traitement asynchrone de Structus. Ce guide fournit une vue d'ensemble du fonctionnement des coroutines et de leur utilisation dans Structus.

## Bases des Coroutines

Les coroutines sont la solution de Kotlin pour la programmation asynchrone. Elles vous permettent d'écrire du code asynchrone dans un style séquentiel, le rendant plus facile à comprendre et à maintenir.

### Fonctions Suspendues

Les fonctions suspendues sont au cœur des coroutines. Ces fonctions peuvent mettre en pause leur exécution et reprendre plus tard, permettant des opérations non bloquantes.

```kotlin
// Définition d'une fonction suspendue
suspend fun fetchData(): Data {
    // Cette fonction peut être mise en pause et reprise
    return networkService.getData()
}
```

### Constructeurs de Coroutines

Les constructeurs de coroutines sont utilisés pour démarrer de nouvelles coroutines. Les plus courants sont:

```kotlin
// launch: démarre une nouvelle coroutine sans retourner de résultat
suspend fun main() = coroutineScope {
    launch {
        delay(1000)
        println("Monde des Coroutines Kotlin !")
    }
    println("Bonjour")
}

// async: démarre une coroutine qui retourne un résultat
suspend fun fetchTwoValues() = coroutineScope {
    val valueOne = async { getValue1() }
    val valueTwo = async { getValue2() }
    
    // Attendre les deux résultats
    valueOne.await() + valueTwo.await()
}
```

## Utilisation des Coroutines dans Structus

Structus utilise largement les coroutines pour assurer des opérations non bloquantes, en particulier dans les repositories et les gestionnaires de commandes/requêtes.

### Repositories

Toutes les méthodes de repository dans Structus sont des fonctions suspendues:

```kotlin
interface OrderRepository : Repository {
    suspend fun findById(id: OrderId): Order?
    suspend fun save(order: Order)
    suspend fun delete(id: OrderId)
}
```

### Gestionnaires de Commandes

Les gestionnaires de commandes utilisent le modificateur `suspend` pour permettre le traitement asynchrone:

```kotlin
class CreateOrderCommandHandler(
    private val orderRepository: OrderRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateOrderCommand, Result<OrderId>> {
    
    override suspend operator fun invoke(command: CreateOrderCommand): Result<OrderId> {
        return runCatching {
            val order = Order.create(command.customerId)
            
            orderRepository.save(order)
            order.domainEvents.forEach { outboxRepository.save(it) }
            order.clearEvents()
            
            order.id
        }
    }
}
```

## Coroutines et Outbox Transactionnel

Le pattern Outbox Transactionnel de Structus s'appuie fortement sur les coroutines pour une publication fiable des événements:

```kotlin
class OutboxPublisher(
    private val outboxRepository: MessageOutboxRepository,
    private val eventPublisher: DomainEventPublisher
) {
    
    suspend fun publishPendingEvents() {
        val messages = outboxRepository.findUnpublished(limit = 100)
        
        messages.forEach { message ->
            try {
                val event = deserializeEvent(message.payload, message.eventType)
                eventPublisher.publish(event)
                outboxRepository.markAsPublished(message.id)
            } catch (e: Exception) {
                outboxRepository.incrementRetryCount(message.id)
            }
        }
    }
}
```

## Bonnes Pratiques

### Utiliser la Concurrence Structurée

Utilisez toujours la concurrence structurée pour garantir que toutes les coroutines sont correctement gérées:

```kotlin
suspend fun fetchData() = coroutineScope {
    // Toutes les coroutines démarrées dans ce scope seront terminées
    // avant que la fonction ne retourne
    val result1 = async { api.fetchFirstPart() }
    val result2 = async { api.fetchSecondPart() }
    
    combineResults(result1.await(), result2.await())
}
```

### Gérer les Exceptions Correctement

Utilisez `runCatching` ou des blocs try-catch pour gérer les exceptions dans les coroutines:

```kotlin
suspend fun safeApiCall() = runCatching {
    api.fetchData()
}.getOrElse { error ->
    // Gérer l'erreur
    logger.error(error)
    defaultValue
}
```

### Choisir le Bon Dispatcher

Sélectionnez un dispatcher approprié en fonction du travail effectué:

- **Dispatchers.IO**: Pour les travaux liés aux E/S (réseau, disque)
- **Dispatchers.Default**: Pour les travaux intensifs en CPU
- **Dispatchers.Main**: Pour les opérations UI (Android)

```kotlin
withContext(Dispatchers.IO) {
    // Opérations E/S (réseau, base de données)
}
```

## Ressources Additionnelles

- [Guide Officiel des Coroutines Kotlin](https://kotlinlang.org/docs/coroutines-guide.html)
- [Coroutines Kotlin par l'Exemple](https://kotlinlang.org/docs/coroutines-basics.html)
- [Dépôt GitHub](https://github.com/kotlin/kotlinx.coroutines)