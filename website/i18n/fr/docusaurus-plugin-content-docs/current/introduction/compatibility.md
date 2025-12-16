---
sidebar_position: 2
---

# Compatibilité

Cette page fournit des informations sur la compatibilité entre les versions de Structus et diverses dépendances.

## Compatibilité Kotlin

| Version Structus | Version Kotlin | Notes                                  |
|------------------|---------------|----------------------------------------|
| 0.1.0            | 1.8.0+        | Nécessite Kotlin 1.8.0 ou supérieur    |
| 0.2.0 (à venir)  | 1.9.0+        | Utilisera les fonctionnalités de Kotlin 1.9 |

## Compatibilité JDK

| Version Structus | Version JDK | Notes                                   |
|------------------|-------------|------------------------------------------|
| 0.1.0            | JDK 11+     | Compatible avec JDK 11 à JDK 21         |
| 0.2.0 (à venir)  | JDK 11+     | Compatible avec JDK 11 à JDK 21         |

## Compatibilité Coroutines

| Version Structus | Version Coroutines | Notes                               |
|------------------|-------------------|--------------------------------------|
| 0.1.0            | 1.6.4+            | Nécessite kotlinx.coroutines 1.6.4+   |
| 0.2.0 (à venir)  | 1.7.0+            | Utilisera les nouvelles fonctionnalités des coroutines |

## Compatibilité Spring Boot

Bien que Structus soit agnostique en matière de framework, voici la compatibilité avec Spring Boot pour référence:

| Version Structus | Version Spring Boot | Notes                             |
|------------------|---------------------|-----------------------------------|
| 0.1.0            | 2.7.x, 3.x         | Compatible avec les deux versions   |
| 0.2.0 (à venir)  | 3.x                | Se concentrera sur Spring Boot 3   |

## Compatibilité Ktor

| Version Structus | Version Ktor | Notes                                   |
|------------------|--------------|------------------------------------------|
| 0.1.0            | 2.x          | Compatible avec Ktor 2.x                |
| 0.2.0 (à venir)  | 2.x          | Continuera à supporter Ktor 2.x         |

## Compatibilité Gradle

| Version Structus | Version Gradle | Notes                                |
|------------------|----------------|--------------------------------------|
| 0.1.0            | 7.0+           | Compatible avec Gradle 7.0 et supérieur |
| 0.2.0 (à venir)  | 7.3+           | Nécessitera Gradle 7.3 et supérieur   |

## Compatibilité Maven

| Version Structus | Version Maven | Notes                                |
|------------------|--------------|---------------------------------------|
| 0.1.0            | 3.6+         | Compatible avec Maven 3.6 et supérieur |
| 0.2.0 (à venir)  | 3.6+         | Continuera à supporter Maven 3.6+     |

## Compatibilité des Systèmes d'Exploitation

Structus est compatible avec tout système d'exploitation qui prend en charge la JVM :

- Windows
- macOS
- Linux
- Tout autre OS avec support JVM

## Support Multiplateforme

Actuellement, Structus est conçu uniquement pour JVM. Le support multiplateforme est dans notre feuille de route:

| Version Structus | Plateformes    | Notes                                   |
|------------------|--------------|------------------------------------------|
| 0.1.0            | JVM uniquement | Pas de support multiplateforme          |
| 0.2.0 (à venir)  | JVM uniquement | Pas de support multiplateforme          |
| Versions futures | JVM, JS, Native | Prévu pour les versions futures         |

## Support IDE

Structus est développé en tenant compte du support IDE:

| IDE             | Niveau de support | Notes                                   |
|-----------------|--------------|------------------------------------------|
| IntelliJ IDEA   | Complet      | IDE recommandé pour le développement     |
| Android Studio  | Complet      | Basé sur IntelliJ IDEA                   |
| Eclipse         | Partiel      | Fonctionnalités de base supportées       |
| VS Code         | Partiel      | Avec l'extension Kotlin                  |

## Fonctionnalités Expérimentales

Les fonctionnalités marquées comme expérimentales dans Structus n'ont aucune garantie de compatibilité entre les versions et peuvent changer sans préavis.

## Migration

Lors de la mise à niveau entre les versions de Structus, veuillez consulter notre [Guide de Migration](../migration-guide) pour des instructions détaillées.