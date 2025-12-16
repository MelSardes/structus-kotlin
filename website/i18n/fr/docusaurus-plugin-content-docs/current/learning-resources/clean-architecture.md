---
sidebar_position: 2
---

# Guide de l'Architecture Propre

L'Architecture Propre (Clean Architecture) est le principe fondateur de la conception de Structus. Ce guide explique les concepts d'architecture propre et leur mise en œuvre dans Structus.

## Qu'est-ce que l'Architecture Propre ?

L'Architecture Propre est une philosophie de conception logicielle introduite par Robert C. Martin (Uncle Bob) qui sépare les préoccupations en couches concentriques, avec des dépendances pointant vers l'intérieur. Cela garantit que la logique métier reste indépendante des frameworks, des bases de données et de l'interface utilisateur.

## Principes Fondamentaux

1. **Indépendance des Frameworks**: L'architecture ne dépend pas de l'existence de bibliothèques ou de frameworks
2. **Testabilité**: Les règles métier peuvent être testées sans interface utilisateur, base de données ou éléments externes
3. **Indépendance de l'Interface Utilisateur**: L'interface utilisateur peut changer sans modifier le système
4. **Indépendance de la Base de Données**: Les règles métier ne sont pas liées à une base de données spécifique
5. **Indépendance des Agents Externes**: Les règles métier ne connaissent pas le monde extérieur

## Les Couches de l'Architecture Propre

L'Architecture Propre organise le code en couches concentriques:

```
┌─────────────────────────────────────────┐
│         Frameworks & Drivers            │
│  (Web, UI, Base de données, Interfaces externes)│
└─────────────────────────────────────────┘
              ↓ dépend de
┌─────────────────────────────────────────┐
│         Adaptateurs d'Interface         │
│  (Contrôleurs, Présentateurs, Passerelles)│
└─────────────────────────────────────────┘
              ↓ dépend de
┌─────────────────────────────────────────┐
│        Règles Métier d'Application      │
│  (Cas d'Utilisation, Services d'Application)│
└─────────────────────────────────────────┘
              ↓ dépend de
┌─────────────────────────────────────────┐
│        Règles Métier d'Entreprise       │
│  (Entités, Objets Valeur)               │
└─────────────────────────────────────────┘
```

### 1. Règles Métier d'Entreprise (Entités)

La couche la plus interne contient les entités métier qui encapsulent les règles les plus générales et de haut niveau.

```kotlin
// Exemple d'entité de domaine Structus
class User private constructor(
    override val id: UserId,
    val email: Email,
    val status: UserStatus
) : AggregateRoot<UserId>() {
    
    fun activate() {
        require(status == UserStatus.PENDING) { "Seuls les utilisateurs en attente peuvent être activés" }
        // Logique métier ici
    }
    
    companion object {
        fun create(email: Email): User {
            val user = User(UserId(UUID.randomUUID()), email, UserStatus.PENDING)
            user.recordEvent(UserCreatedEvent(user.id.value, email.value))
            return user
        }
    }
}
```

### 2. Règles Métier d'Application (Cas d'Utilisation)

Cette couche contient les règles métier spécifiques à l'application qui orchestrent le flux de données vers et depuis les entités.

```kotlin
// Exemple de gestionnaire de commande Structus
class CreateUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: CreateUserCommand): Result<UserId> {
        return runCatching {
            val email = Email(command.email)
            val user = User.create(email)
            
            userRepository.save(user)
            user.domainEvents.forEach { outboxRepository.save(it) }
            user.clearEvents()
            
            user.id
        }
    }
}
```

### 3. Adaptateurs d'Interface

Les adaptateurs convertissent les données entre les cas d'utilisation/entités et les agents externes tels que la base de données ou le web.

```kotlin
// Exemple d'implémentation de repository Structus
class UserRepositoryImpl(
    private val database: Database
) : UserRepository {
    
    override suspend fun save(user: User) {
        database.execute(
            "INSERT INTO users (id, email, status) VALUES (?, ?, ?)",
            user.id.value,
            user.email.value,
            user.status.name
        )
    }
    
    override suspend fun findById(id: UserId): User? {
        return database.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            id.value
        )?.let { mapToDomain(it) }
    }
    
    private fun mapToDomain(row: DatabaseRow): User {
        // Code de mappage
    }
}
```

### 4. Frameworks & Drivers

La couche la plus externe est composée de frameworks et d'outils tels que la base de données, le framework web, etc.

```kotlin
// Exemple de contrôleur Spring Boot
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val createUserHandler: CreateUserCommandHandler,
    private val getUserHandler: GetUserByIdQueryHandler
) {
    
    @PostMapping
    suspend fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<Any> {
        val command = CreateUserCommand(request.email)
        
        return when (val result = createUserHandler(command)) {
            is Result.Success -> ResponseEntity.status(HttpStatus.CREATED)
                .body(mapOf("id" to result.value.value))
            is Result.Failure -> ResponseEntity.badRequest()
                .body(mapOf("error" to result.error.message))
        }
    }
}
```

## La Règle de Dépendance

La règle fondamentale de l'Architecture Propre est que les dépendances pointent toujours vers l'intérieur. Les cercles intérieurs ne savent rien des cercles extérieurs.

Dans Structus, cela est réalisé par:

1. **Interfaces définies dans les couches internes**
2. **Implémentations dans les couches externes**
3. **Injection de dépendance** pour tout câbler ensemble

## Avantages dans Structus

Structus exploite l'Architecture Propre pour fournir:

- **Indépendance de Framework**: Votre logique métier fonctionne quel que soit le framework que vous utilisez (Spring, Ktor, etc.)
- **Testabilité**: Les couches domaine et application peuvent être testées isolément
- **Maintenabilité**: Les changements dans les systèmes externes n'affectent pas votre logique métier principale
- **Flexibilité**: Facilité pour remplacer des composants d'infrastructure

## Ressources Additionnelles

- [Architecture Propre par Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [L'Architecture Propre (Uncle Bob)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Architecture Propre avec Kotlin](https://proandroiddev.com/clean-architecture-with-kotlin-multiplatform-6d5f2b5e0bba)