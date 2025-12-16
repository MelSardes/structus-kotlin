---
sidebar_position: 1
---

# Spring Boot Integration

This guide shows you how to integrate Structus with Spring Boot.

## Overview

Spring Boot is a popular framework for building Java and Kotlin applications. Structus can be seamlessly integrated with Spring Boot to provide a clean architecture foundation for your application.

## Setup

### Dependencies

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    // Structus
    implementation("com.github.melsardes:structus-kotlin:0.1.0")
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
}
```

### Coroutines Configuration

Configure Spring to work with Kotlin coroutines:

```kotlin
@Configuration
class CoroutineConfig {
    @Bean
    fun coroutineDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    @Bean
    fun coroutineScope(coroutineDispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(coroutineDispatcher + SupervisorJob())
}
```

## Domain Layer

Create your domain entities and repositories:

```kotlin
data class ProductId(val value: String) : ValueObject

class Product private constructor(
    override val id: ProductId,
    private var name: String,
    private var price: Money,
    private var active: Boolean = true
) : AggregateRoot<ProductId>() {
    // Properties and methods
    
    companion object {
        fun create(id: ProductId, name: String, price: Money): Product {
            // Create product and record events
            val product = Product(id, name, price)
            product.recordEvent(ProductCreatedEvent(id.value, name, price.amount))
            return product
        }
    }
}

interface ProductRepository : Repository {
    suspend fun findById(id: ProductId): Product?
    suspend fun save(product: Product)
}
```

## Application Layer

Create command and query handlers:

```kotlin
data class CreateProductCommand(val name: String, val price: BigDecimal) : Command

@Component
class CreateProductCommandHandler(
    private val productRepository: ProductRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateProductCommand, Result<ProductId>> {
    
    override suspend operator fun invoke(command: CreateProductCommand): Result<ProductId> {
        return runCatching {
            val id = ProductId(UUID.randomUUID().toString())
            val price = Money(command.price, Currency.getInstance("USD"))
            
            val product = Product.create(id, command.name, price)
            productRepository.save(product)
            
            // Save events to outbox
            product.domainEvents.forEach { outboxRepository.save(it) }
            product.clearEvents()
            
            id
        }
    }
}
```

## Infrastructure Layer

Implement the repositories:

```kotlin
@Repository
class ProductRepositoryImpl(private val jdbcTemplate: JdbcTemplate) : ProductRepository {
    
    override suspend fun findById(id: ProductId): Product? = withContext(Dispatchers.IO) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT id, name, price, currency, active FROM products WHERE id = ?",
                { rs, _ -> mapToProduct(rs) },
                id.value
            )
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }
    
    override suspend fun save(product: Product) = withContext(Dispatchers.IO) {
        // Implementation details
    }
    
    private fun mapToProduct(rs: ResultSet): Product {
        // Mapping logic
    }
}
```

## Presentation Layer

Create REST controllers:

```kotlin
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val createProductHandler: CreateProductCommandHandler,
    private val getProductHandler: GetProductQueryHandler
) {
    
    @PostMapping
    suspend fun createProduct(@RequestBody request: CreateProductRequestDto): ResponseEntity<Any> {
        val command = CreateProductCommand(name = request.name, price = request.price)
        
        return when (val result = createProductHandler(command)) {
            is Result.Success -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapOf("id" to result.value.value))
            is Result.Failure -> ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to (result.error.message ?: "Unknown error")))
        }
    }
    
    @GetMapping("/{id}")
    suspend fun getProduct(@PathVariable id: String): ResponseEntity<Any> {
        // Implementation
    }
}
```

## Event Publishing

Set up a scheduled task to publish events from the outbox:

```kotlin
@Component
class OutboxPublisher(
    private val outboxRepository: MessageOutboxRepository,
    private val eventPublisher: DomainEventPublisher
) {
    
    @Scheduled(fixedDelay = 1000)
    fun publishPendingEvents() = runBlocking {
        val messages = outboxRepository.findUnpublished(100)
        
        messages.forEach { message ->
            try {
                eventPublisher.publish(message.event)
                outboxRepository.markAsPublished(message.id)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
}
```

## Database Schema

```sql
CREATE TABLE products (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE message_outbox (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_data TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT false,
    timestamp TIMESTAMP NOT NULL
);
```

## Testing

Create integration tests:

```kotlin
@SpringBootTest
class ProductIntegrationTest {
    
    @Autowired
    private lateinit var productController: ProductController
    
    @Test
    fun `should create and retrieve a product`() = runBlocking {
        // Test implementation
    }
}
```

## Example Project

Check out our [complete example project](https://github.com/melsardes/structus-kotlin-examples/spring-boot-sample) on GitHub to see a full Spring Boot integration with Structus.

## Next Steps

Explore the [Ktor Integration](ktor) guide to see how to use Structus with Ktor.