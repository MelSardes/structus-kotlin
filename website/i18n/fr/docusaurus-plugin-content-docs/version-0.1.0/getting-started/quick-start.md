---
sidebar_position: 2
pagination_next: getting-started/core-concepts
pagination_prev: getting-started/installation
---

# Tutoriel de démarrage rapide

Ce tutoriel vous guidera à travers la création d'un système d'inscription d'utilisateurs complet en utilisant Structus. Vous apprendrez à définir des objets valeur, des agrégats, des événements de domaine et des gestionnaires de commandes.

## Prérequis

- Connaissance de base de Kotlin
- Structus installé dans votre projet (voir le [Guide d'installation](installation))
- IDE configuré (IntelliJ IDEA recommandé)

## Vue d'ensemble

Nous allons construire un système d'inscription d'utilisateurs comprenant:

1. Définition d'objets valeur (Email, Password)
2. Création d'un agrégat utilisateur
3. Mise en place d'événements de domaine
4. Implémentation de commandes et gestionnaires
5. Définition de requêtes pour la lecture

## Étape 1: Définir les objets valeur

Les objets valeur sont des objets immuables identifiés par leurs attributs, pas par leur identité. Commençons par définir quelques objets valeur pour notre système d'inscription:

```kotlin
// src/main/kotlin/com/example/domain/user/UserValueObjects.kt
package com.example.domain.user

import com.melsardes.libraries.structuskotlin.domain.ValueObject

data class UserId(val value: String) : ValueObject

data class Email(val value: String) : ValueObject {
    init {
        require(value.matches(EMAIL_REGEX)) { "Format d'email invalide" }
    }
    
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}

data class Password(private val value: String) : ValueObject {
    init {
        require(value.length >= 8) { "Le mot de passe doit contenir au moins 8 caractères" }
        require(value.contains(Regex("[A-Z]"))) { "Le mot de passe doit contenir au moins une lettre majuscule" }
        require(value.contains(Regex("[0-9]"))) { "Le mot de passe doit contenir au moins un chiffre" }
    }
    
    fun matches(rawPassword: String): Boolean {
        // Dans un environnement de production, utilisez un algorithme sécurisé
        // comme bcrypt pour la comparaison
        return value == rawPassword
    }
    
    // Ne pas exposer la valeur brute, mais fournir un hachage pour le stockage
    fun hashedValue(): String {
        // Utilisez un algorithme de hachage sécurisé dans un environnement de production
        return value.hashCode().toString()
    }
}
```

## Étape 2: Créer l'agrégat utilisateur

Un agrégat est un cluster d'objets de domaine traités comme une unité:

```kotlin
// src/main/kotlin/com/example/domain/user/User.kt
package com.example.domain.user

import com.melsardes.libraries.structuskotlin.domain.AggregateRoot
import java.time.Instant
import java.util.UUID

class User private constructor(
    override val id: UserId,
    val email: Email,
    val password: Password,
    val status: UserStatus = UserStatus.PENDING_ACTIVATION,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null
) : AggregateRoot<UserId>() {
    
    enum class UserStatus {
        PENDING_ACTIVATION,
        ACTIVE,
        LOCKED,
        DELETED
    }
    
    fun activate() {
        if (status != UserStatus.PENDING_ACTIVATION) {
            throw IllegalStateException("Seuls les utilisateurs en attente peuvent être activés")
        }
        
        val event = UserActivatedEvent(
            aggregateId = id.value,
            email = email.value,
            occurredAt = Instant.now()
        )
        
        // Enregistrer l'événement pour publication ultérieure
        recordEvent(event)
        
        // Modifier l'état
        status = UserStatus.ACTIVE
        updatedAt = Instant.now()
    }
    
    companion object {
        fun create(email: Email, password: Password): User {
            val id = UserId(UUID.randomUUID().toString())
            val user = User(id, email, password)
            
            // Création d'un événement de domaine
            val event = UserCreatedEvent(
                aggregateId = id.value,
                email = email.value,
                occurredAt = Instant.now()
            )
            
            // Enregistrer l'événement pour publication ultérieure
            user.recordEvent(event)
            
            return user
        }
    }
}
```

## Étape 3: Définir les événements de domaine

Les événements de domaine représentent quelque chose qui s'est passé dans le domaine:

```kotlin
// src/main/kotlin/com/example/domain/user/UserEvents.kt
package com.example.domain.user

import com.melsardes.libraries.structuskotlin.domain.events.BaseDomainEvent
import java.time.Instant

data class UserCreatedEvent(
    override val aggregateId: String,
    val email: String,
    override val occurredAt: Instant
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1
)

data class UserActivatedEvent(
    override val aggregateId: String,
    val email: String,
    override val occurredAt: Instant
) : BaseDomainEvent(
    aggregateId = aggregateId,
    aggregateType = "User",
    eventVersion = 1
)
```

## Étape 4: Définir les interfaces de repository

Les repositories fournissent des méthodes pour persister et retrouver les agrégats:

```kotlin
// src/main/kotlin/com/example/domain/user/UserRepository.kt
package com.example.domain.user

import com.melsardes.libraries.structuskotlin.domain.Repository

interface UserRepository : Repository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun save(user: User)
    suspend fun delete(id: UserId)
}
```

## Étape 5: Créer la commande d'inscription

Les commandes représentent une intention de modifier l'état:

```kotlin
// src/main/kotlin/com/example/application/commands/RegisterUserCommand.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.Command

data class RegisterUserCommand(
    val email: String,
    val password: String
) : Command
```

## Étape 6: Implémenter le gestionnaire de commandes

Le gestionnaire de commandes contient la logique pour traiter la commande:

```kotlin
// src/main/kotlin/com/example/application/commands/RegisterUserCommandHandler.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository
import com.example.domain.user.*

class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
        return runCatching {
            // 1. Validation
            val email = Email(command.email)
            val password = Password(command.password)
            
            // 2. Vérifier si l'email existe déjà
            userRepository.findByEmail(email)?.let {
                return Result.failure(IllegalStateException("Un utilisateur avec cet email existe déjà"))
            }
            
            // 3. Créer l'agrégat
            val user = User.create(email, password)
            
            // 4. Persister l'agrégat
            userRepository.save(user)
            
            // 5. Sauvegarder les événements dans l'outbox
            user.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // 6. Effacer les événements de l'agrégat
            user.clearEvents()
            
            // 7. Retourner l'ID
            user.id
        }
    }
}
```

## Étape 7: Créer la commande d'activation

```kotlin
// src/main/kotlin/com/example/application/commands/ActivateUserCommand.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.Command

data class ActivateUserCommand(
    val userId: String
) : Command
```

## Étape 8: Implémenter le gestionnaire d'activation

```kotlin
// src/main/kotlin/com/example/application/commands/ActivateUserCommandHandler.kt
package com.example.application.commands

import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import com.melsardes.libraries.structuskotlin.domain.MessageOutboxRepository
import com.example.domain.user.*

class ActivateUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<ActivateUserCommand, Result<Unit>> {
    
    override suspend operator fun invoke(command: ActivateUserCommand): Result<Unit> {
        return runCatching {
            // 1. Trouver l'utilisateur
            val userId = UserId(command.userId)
            val user = userRepository.findById(userId)
                ?: return Result.failure(NoSuchElementException("Utilisateur non trouvé"))
            
            // 2. Activer l'utilisateur
            user.activate()
            
            // 3. Persister l'agrégat modifié
            userRepository.save(user)
            
            // 4. Sauvegarder les événements dans l'outbox
            user.domainEvents.forEach { event ->
                outboxRepository.save(event)
            }
            
            // 5. Effacer les événements de l'agrégat
            user.clearEvents()
        }
    }
}
```

## Étape 9: Implémenter une requête pour obtenir l'utilisateur

```kotlin
// src/main/kotlin/com/example/application/queries/GetUserByIdQuery.kt
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.Query

data class GetUserByIdQuery(
    val userId: String
) : Query
```

## Étape 10: Créer un DTO pour la réponse

```kotlin
// src/main/kotlin/com/example/application/queries/UserDto.kt
package com.example.application.queries

import java.time.Instant

data class UserDto(
    val id: String,
    val email: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant?
)
```

## Étape 11: Implémenter le gestionnaire de requêtes

```kotlin
// src/main/kotlin/com/example/application/queries/GetUserByIdQueryHandler.kt
package com.example.application.queries

import com.melsardes.libraries.structuskotlin.application.queries.QueryHandler
import com.example.domain.user.*

class GetUserByIdQueryHandler(
    private val userRepository: UserRepository
) : QueryHandler<GetUserByIdQuery, UserDto?> {
    
    override suspend operator fun invoke(query: GetUserByIdQuery): UserDto? {
        val userId = UserId(query.userId)
        val user = userRepository.findById(userId) ?: return null
        
        return UserDto(
            id = user.id.value,
            email = user.email.value,
            status = user.status.name,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}
```

## Utilisation du système

Avec tous ces éléments en place, vous pouvez maintenant utiliser le système d'inscription:

```kotlin
// Exemple d'utilisation
suspend fun main() {
    // Initialiser les repositories et handlers
    val userRepository = UserRepositoryImpl()
    val outboxRepository = MessageOutboxRepositoryImpl()
    
    val registerHandler = RegisterUserCommandHandler(userRepository, outboxRepository)
    val activateHandler = ActivateUserCommandHandler(userRepository, outboxRepository)
    val getUserHandler = GetUserByIdQueryHandler(userRepository)
    
    // Inscription d'un utilisateur
    val registerCommand = RegisterUserCommand(
        email = "user@example.com",
        password = "Password123"
    )
    
    val result = registerHandler(registerCommand)
    
    when (result) {
        is Result.Success -> {
            val userId = result.value.value
            println("Utilisateur inscrit avec l'ID: $userId")
            
            // Activation de l'utilisateur
            val activateCommand = ActivateUserCommand(userId)
            activateHandler(activateCommand)
            
            // Obtention des détails de l'utilisateur
            val query = GetUserByIdQuery(userId)
            val userDto = getUserHandler(query)
            
            userDto?.let {
                println("Utilisateur: ${it.email}, Status: ${it.status}")
            }
        }
        is Result.Failure -> {
            println("Erreur lors de l'inscription: ${result.error.message}")
        }
    }
}
```

## Conclusion

Dans ce tutoriel, vous avez appris à:

- Définir des objets valeur pour encapsuler les règles de validation
- Créer des agrégats qui protègent leurs invariants
- Utiliser des événements de domaine pour notifier les changements d'état
- Implémenter des commandes et des gestionnaires pour modifier l'état
- Créer des requêtes pour lire les données

C'est une architecture CQRS (Command Query Responsibility Segregation) de base qui sépare clairement les opérations de lecture et d'écriture. Ce modèle vous aide à construire des applications maintenables et évolutives.

## Prochaines étapes

- [Concepts de base](core-concepts) - Comprendre les concepts fondamentaux en détail
- [Vue d'ensemble de l'architecture](../architecture/overview) - Explorer l'architecture complète
- [Implémentation CQRS](../advanced/cqrs-implementation) - Approfondir CQRS