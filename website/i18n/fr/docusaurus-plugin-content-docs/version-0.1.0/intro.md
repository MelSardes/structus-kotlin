---
sidebar_position: 1
slug: /intro
---

# Introduction à Structus

Bienvenue sur **Structus** - une bibliothèque Kotlin JVM pure fournissant les blocs de construction fondamentaux pour implémenter l'**Architecture Explicite**.

## Qu'est-ce que Structus ?

Structus est un **noyau partagé** pour les projets à grande échelle qui synthétise :

- 🏛️ **Conception Pilotée par le Domaine (DDD)**
- 📝 **Séparation Commande/Requête (CQS)**
- 📡 **Architecture Orientée Événements (EDA)**

Il définit des interfaces et des classes de base pour tous les concepts métier de base et les modèles architecturaux tout en restant complètement **indépendant des frameworks**.

## Fonctionnalités Clés

- 🚀 **Kotlin Pur** : Aucune dépendance de framework (Spring, Ktor, Micronaut, etc.)
- 🔄 **Prêt pour les Coroutines** : Toutes les opérations I/O utilisent des fonctions suspend
- 📦 **Dépendances Minimales** : Seulement Kotlin stdlib + kotlinx-coroutines-core
- 📚 **Documentation Complète** : Chaque composant inclut KDoc et des exemples
- 🏗️ **Indépendant des Frameworks** : Fonctionne avec n'importe quel framework ou Kotlin pur
- 🎨 **Architecture Propre** : Applique une séparation appropriée des couches et des dépendances

## Pourquoi l'Architecture Explicite ?

L'Architecture Explicite vous aide à :

1. **Séparer les Préoccupations** : Frontières claires entre domaine, application et infrastructure
2. **Testabilité** : Facile de tester la logique métier de manière isolée
3. **Flexibilité** : Changer de frameworks ou de bases de données sans réécrire la logique métier
4. **Maintenabilité** : Le code est organisé et prévisible
5. **Évolutivité** : L'architecture évolue avec votre équipe et votre base de code

## Couches d'Architecture

```
┌─────────────────────────────────────────┐
│         Couche Présentation             │
│    (Contrôleurs, DTOs, APIs REST)       │
└─────────────────────────────────────────┘
              ↓ dépend de
┌─────────────────────────────────────────┐
│        Couche Application               │
│  (Commandes, Requêtes, Handlers, Events)│
└─────────────────────────────────────────┘
              ↓ dépend de
┌─────────────────────────────────────────┐
│          Couche Domaine                 │
│  (Entités, Agrégats, Objets Valeur)    │
└─────────────────────────────────────────┘
              ↑ implémenté par
┌─────────────────────────────────────────┐
│      Couche Infrastructure              │
│  (Base de données, APIs externes, etc.) │
└─────────────────────────────────────────┘
```

## Exemple Rapide

Voici un aperçu du code Structus :

```kotlin
// Couche Domaine - Logique métier pure
class User(
    override val id: UserId,
    var email: Email,
    var name: String
) : AggregateRoot<UserId>() {
    
    fun activate() {
        status = UserStatus.ACTIVE
        recordEvent(UserActivatedEvent(id.value))
    }
}

// Couche Application - Cas d'utilisation
class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<RegisterUserCommand, Result<UserId>> {
    
    override suspend operator fun invoke(command: RegisterUserCommand): Result<UserId> {
        return runCatching {
            val user = User.create(Email(command.email), command.name)
            userRepository.save(user)
            
            // Pattern Transactional Outbox
            user.domainEvents.forEach { outboxRepository.save(it) }
            user.clearEvents()
            
            user.id
        }
    }
}
```

## Prochaines Étapes

Prêt à commencer ? Voici ce qu'il faut faire ensuite :

1. **[Guide d'Installation](getting-started/installation)** - Configurez Structus dans votre projet
2. **[Tutoriel de Démarrage Rapide](getting-started/quick-start)** - Créez votre première application en 15 minutes
3. **[Concepts de Base](getting-started/core-concepts)** - Comprenez les concepts fondamentaux
4. **[Vue d'Ensemble de l'Architecture](architecture/overview)** - Plongez dans l'architecture

## Communauté & Support

- 💬 [Discussions GitHub](https://github.com/melsardes/structus-kotlin/discussions)
- 🐛 [Suivi des Problèmes](https://github.com/melsardes/structus-kotlin/issues)
- ⭐ [Star sur GitHub](https://github.com/melsardes/structus-kotlin)

---

**Créé par [Mel Sardes](https://github.com/melsardes) • Fait avec ❤️ pour la communauté Kotlin**
