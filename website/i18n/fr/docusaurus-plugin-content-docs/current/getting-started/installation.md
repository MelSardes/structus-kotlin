---
sidebar_position: 1
pagination_next: getting-started/quick-start
pagination_prev: about
---

# Guide d'installation

Ce guide vous aidera à ajouter **Structus** à votre projet Kotlin.

## Prérequis

- JDK 11 ou supérieur
- Kotlin 1.6.0 ou supérieur
- Gradle ou Maven

## Installation via Gradle

Ajouter la dépendance au fichier `build.gradle` ou `build.gradle.kts`:

```kotlin
// build.gradle.kts (Kotlin DSL)
repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.melsardes:structus-kotlin:0.1.0")
}
```

```groovy
// build.gradle (Groovy DSL)
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.melsardes:structus-kotlin:0.1.0'
}
```

## Installation via Maven

```xml
<!-- pom.xml -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.melsardes</groupId>
        <artifactId>structus-kotlin</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

## Construire depuis la source

Si vous préférez construire la bibliothèque depuis la source:

```bash
# Cloner le dépôt
git clone https://github.com/melsardes/structus-kotlin.git

# Naviguer vers le répertoire
cd structus-kotlin

# Construire le projet
./gradlew build

# Installer dans votre dépôt local Maven
./gradlew publishToMavenLocal
```

## Vérification de l'installation

Créez un simple fichier Kotlin pour vérifier que Structus est correctement installé:

```kotlin
import com.melsardes.libraries.structuskotlin.domain.Entity
import com.melsardes.libraries.structuskotlin.domain.ValueObject

fun main() {
    // Créer une classe de test pour s'assurer que les imports fonctionnent
    data class TestId(val value: String) : ValueObject
    class TestEntity(override val id: TestId) : Entity<TestId>()
    
    val entity = TestEntity(TestId("test-id"))
    println("Entité créée avec l'id: ${entity.id.value}")
}
```

Si le code compile et s'exécute sans erreur, vous avez correctement installé Structus!

## Modules inclus

La bibliothèque comprend les modules suivants:

1. **Domain** - Blocs fondamentaux pour le DDD (entités, objets-valeurs, événements)
2. **Application** - Modèles CQRS et gestionnaires pour les commandes et requêtes
3. **Infrastructure** - Implémentations optionnelles pour diverses technologies

## Configuration avec les frameworks

### Spring Boot

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import org.springframework.stereotype.Service

@Service
class MyCommandHandler(
    private val repository: MyRepository
) : CommandHandler<MyCommand, Result<MyEntity>> {
    // ...
}
```

### Ktor

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler
import io.ktor.server.application.*

// Dans votre module Ktor
fun Application.configureHandlers() {
    val handler = MyCommandHandler(repository)
    // ...
}
```

### Kotlin pur

```kotlin
import com.melsardes.libraries.structuskotlin.application.commands.CommandHandler

// Créer des instances manuellement
val repository = MyRepositoryImpl()
val handler = MyCommandHandler(repository)
```

## Dépendances optionnelles

Selon votre cas d'utilisation, vous pourriez avoir besoin d'ajouter ces dépendances supplémentaires:

- **kotlinx-coroutines-core** - Pour la programmation asynchrone
- **kotlinx-serialization** - Pour la sérialisation/désérialisation d'objets
- **slf4j-api** - Pour la journalisation (logging)

## Résolution des problèmes

### Conflits de version

Si vous rencontrez des conflits de version avec Kotlin ou les coroutines, forcez une version spécifique:

```kotlin
// build.gradle.kts
configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}
```

### JDK incompatible

Structus nécessite Java 11 ou supérieur. Vérifiez votre version JDK:

```bash
java -version
```

## Ressources supplémentaires

- [Tutoriel de démarrage rapide](quick-start)
- [Concepts fondamentaux](core-concepts)
- [Référence API](../reference/api-overview)
- [GitHub du projet](https://github.com/melsardes/structus-kotlin)

## Prochaines étapes

Une fois Structus installé, passez au [Tutoriel de démarrage rapide](quick-start) pour construire votre première application.