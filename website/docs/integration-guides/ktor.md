---
sidebar_position: 2
---

# Ktor Integration

This guide demonstrates how to integrate Structus with Ktor, a lightweight and flexible Kotlin web framework.

## Overview

Ktor is an asynchronous web framework built from the ground up to leverage Kotlin's language features like coroutines. It's an excellent match for Structus since both are designed with Kotlin-first principles.

## Setup

### Dependencies

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    // Structus
    implementation("com.github.melsardes:structus-kotlin:0.1.0")
    
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-jackson:2.3.7")
    
    // Database (using Exposed)
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.45.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

## Project Structure

Following clean architecture principles:

```
src/
├── main/
│   ├── kotlin/
│   │   ├── com/example/
│   │   │   ├── domain/          # Domain layer (entities, repositories)
│   │   │   ├── application/     # Application layer (commands, queries)
│   │   │   ├── infrastructure/  # Infrastructure layer (DB, external services)
│   │   │   ├── presentation/    # Presentation layer (Ktor routes)
│   │   │   └── Application.kt   # Main application entry point
```

## Domain Layer

Create your domain entities and repositories:

```kotlin
// domain/Product.kt
data class ProductId(val value: String) : ValueObject

class Product private constructor(
    override val id: ProductId,
    private var name: String,
    private var price: Money
) : AggregateRoot<ProductId>() {

    val name: String get() = name
    val price: Money get() = price
    
    fun updateDetails(name: String, price: Money) {
        this.name = name
        this.price = price
        recordEvent(ProductUpdatedEvent(id.value, name, price.amount))
    }
    
    companion object {
        fun create(id: ProductId, name: String, price: Money): Product {
            val product = Product(id, name, price)
            product.recordEvent(ProductCreatedEvent(id.value, name, price.amount))
            return product
        }
    }
}

// domain/ProductRepository.kt
interface ProductRepository : Repository {
    suspend fun findById(id: ProductId): Product?
    suspend fun save(product: Product)
    suspend fun delete(id: ProductId)
    suspend fun findAll(): List<Product>
}
```

## Application Layer

Create command and query handlers:

```kotlin
// application/commands/CreateProductCommand.kt
data class CreateProductCommand(
    val name: String,
    val price: BigDecimal,
    val currency: String
) : Command

// application/commands/CreateProductCommandHandler.kt
class CreateProductCommandHandler(
    private val productRepository: ProductRepository,
    private val outboxRepository: MessageOutboxRepository
) : CommandHandler<CreateProductCommand, Result<ProductId>> {
    
    override suspend operator fun invoke(command: CreateProductCommand): Result<ProductId> {
        return runCatching {
            val id = ProductId(UUID.randomUUID().toString())
            val price = Money(command.price, Currency.getInstance(command.currency))
            
            val product = Product.create(id, command.name, price)
            productRepository.save(product)
            
            product.domainEvents.forEach { outboxRepository.save(it) }
            product.clearEvents()
            
            id
        }
    }
}

// application/queries/GetProductQuery.kt
data class GetProductQuery(val id: String) : Query

data class ProductDto(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val currency: String
)

// application/queries/GetProductQueryHandler.kt
class GetProductQueryHandler(
    private val productRepository: ProductRepository
) : QueryHandler<GetProductQuery, ProductDto?> {
    
    override suspend operator fun invoke(query: GetProductQuery): ProductDto? {
        val product = productRepository.findById(ProductId(query.id)) ?: return null
        
        return ProductDto(
            id = product.id.value,
            name = product.name,
            price = product.price.amount,
            currency = product.price.currency.currencyCode
        )
    }
}
```

## Infrastructure Layer

Implement repositories using Exposed:

```kotlin
// infrastructure/persistence/ProductTable.kt
object ProductTable : Table("products") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val price = decimal("price", 19, 4)
    val currency = varchar("currency", 3)
    
    override val primaryKey = PrimaryKey(id)
}

// infrastructure/persistence/ProductRepositoryImpl.kt
class ProductRepositoryImpl : ProductRepository {
    
    override suspend fun findById(id: ProductId): Product? = dbQuery {
        ProductTable.select { ProductTable.id eq id.value }
            .singleOrNull()
            ?.let { mapToProduct(it) }
    }
    
    override suspend fun save(product: Product) = dbQuery {
        val exists = ProductTable.select { ProductTable.id eq product.id.value }.count() > 0
        
        if (exists) {
            ProductTable.update({ ProductTable.id eq product.id.value }) {
                it[name] = product.name
                it[price] = product.price.amount
                it[currency] = product.price.currency.currencyCode
            }
        } else {
            ProductTable.insert {
                it[id] = product.id.value
                it[name] = product.name
                it[price] = product.price.amount
                it[currency] = product.price.currency.currencyCode
            }
        }
    }
    
    override suspend fun delete(id: ProductId) = dbQuery {
        ProductTable.deleteWhere { ProductTable.id eq id.value }
    }
    
    override suspend fun findAll(): List<Product> = dbQuery {
        ProductTable.selectAll().map { mapToProduct(it) }
    }
    
    private fun mapToProduct(row: ResultRow): Product {
        val id = ProductId(row[ProductTable.id])
        val name = row[ProductTable.name]
        val price = row[ProductTable.price]
        val currency = Currency.getInstance(row[ProductTable.currency])
        
        // Use reflection or a special factory method
        return ProductFactory.reconstruct(id, name, Money(price, currency))
    }
    
    private suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}

// infrastructure/persistence/OutboxTable.kt
object OutboxTable : Table("message_outbox") {
    val id = varchar("id", 36)
    val eventType = varchar("event_type", 255)
    val aggregateId = varchar("aggregate_id", 255)
    val eventData = text("event_data")
    val published = bool("published")
    val timestamp = timestamp("timestamp")
    val publishedAt = timestamp("published_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

// infrastructure/persistence/MessageOutboxRepositoryImpl.kt
class MessageOutboxRepositoryImpl(
    private val objectMapper: ObjectMapper
) : MessageOutboxRepository {
    
    override suspend fun save(event: DomainEvent) = dbQuery {
        val eventId = UUID.randomUUID().toString()
        val eventType = event.javaClass.name
        val aggregateId = event.aggregateId
        val eventData = objectMapper.writeValueAsString(event)
        
        OutboxTable.insert {
            it[id] = eventId
            it[OutboxTable.eventType] = eventType
            it[OutboxTable.aggregateId] = aggregateId
            it[OutboxTable.eventData] = eventData
            it[published] = false
            it[timestamp] = LocalDateTime.now()
        }
    }
    
    // Other methods omitted
}
```

## Presentation Layer

Set up Ktor routes to expose your application:

```kotlin
// presentation/Routes.kt
fun Application.configureRouting(
    createProductHandler: CreateProductCommandHandler,
    getProductHandler: GetProductQueryHandler
) {
    routing {
        route("/api/v1") {
            route("/products") {
                post {
                    val request = call.receive<CreateProductRequestDto>()
                    
                    val command = CreateProductCommand(
                        name = request.name,
                        price = request.price,
                        currency = request.currency
                    )
                    
                    when (val result = createProductHandler(command)) {
                        is Result.Success -> {
                            call.respond(
                                HttpStatusCode.Created,
                                mapOf("id" to result.value.value)
                            )
                        }
                        is Result.Failure -> {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to (result.error.message ?: "Unknown error"))
                            )
                        }
                    }
                }
                
                get("/{id}") {
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, "Missing id parameter")
                        return@get
                    }
                    
                    val query = GetProductQuery(id)
                    val product = getProductHandler(query)
                    
                    if (product != null) {
                        call.respond(product)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}

// presentation/DTOs.kt
@Serializable
data class CreateProductRequestDto(
    val name: String,
    val price: BigDecimal,
    val currency: String
)
```

## Main Application

Create the main application entry point:

```kotlin
// Application.kt
fun main() {
    embeddedServer(Netty, port = 8080) {
        configureSerialization()
        configureDatabase()
        
        // Setup DI
        val objectMapper = jacksonObjectMapper()
        
        val productRepository = ProductRepositoryImpl()
        val outboxRepository = MessageOutboxRepositoryImpl(objectMapper)
        
        val createProductHandler = CreateProductCommandHandler(productRepository, outboxRepository)
        val getProductHandler = GetProductQueryHandler(productRepository)
        
        // Configure routes
        configureRouting(createProductHandler, getProductHandler)
        
        // Start outbox processor
        launch {
            val outboxProcessor = OutboxProcessor(outboxRepository)
            outboxProcessor.start()
        }
    }.start(wait = true)
}

// Configure Jackson serialization
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(KotlinModule.Builder().build())
        }
    }
}

// Configure database
fun Application.configureDatabase() {
    Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver"
    )
    
    transaction {
        SchemaUtils.create(ProductTable, OutboxTable)
    }
}
```

## Event Processing

Create an outbox processor to publish events:

```kotlin
// infrastructure/events/OutboxProcessor.kt
class OutboxProcessor(
    private val outboxRepository: MessageOutboxRepository,
    private val kafkaProducer: KafkaProducer<String, String> = createKafkaProducer()
) {
    private val logger = LoggerFactory.getLogger(OutboxProcessor::class.java)
    private val objectMapper = jacksonObjectMapper()
    
    suspend fun start() {
        while (true) {
            try {
                val messages = outboxRepository.findUnpublished(100)
                
                messages.forEach { message ->
                    try {
                        // Publish to Kafka
                        val record = ProducerRecord(
                            "domain-events",
                            message.aggregateId,
                            objectMapper.writeValueAsString(message.event)
                        )
                        
                        kafkaProducer.send(record).get()
                        
                        // Mark as published
                        outboxRepository.markAsPublished(message.id)
                    } catch (e: Exception) {
                        logger.error("Failed to publish event ${message.id}", e)
                        outboxRepository.incrementRetryCount(message.id)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error processing outbox", e)
            }
            
            delay(1000)
        }
    }
    
    private companion object {
        private fun createKafkaProducer(): KafkaProducer<String, String> {
            val props = Properties()
            props["bootstrap.servers"] = "localhost:9092"
            props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
            props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
            return KafkaProducer(props)
        }
    }
}
```

## Testing

Create tests for your application:

```kotlin
// ApplicationTest.kt
class ApplicationTest {
    private val testEngine = TestApplicationEngine(createTestEnvironment())
    private val productRepository = mockk<ProductRepository>()
    private val outboxRepository = mockk<MessageOutboxRepository>()
    
    @BeforeTest
    fun setup() {
        testEngine.start(wait = false)
        testEngine.application.apply {
            configureSerialization()
            configureRouting(
                createProductHandler = CreateProductCommandHandler(
                    productRepository, outboxRepository
                ),
                getProductHandler = GetProductQueryHandler(
                    productRepository
                )
            )
        }
    }
    
    @AfterTest
    fun teardown() {
        testEngine.stop(0L, 0L)
    }
    
    @Test
    fun `test create product`() = runBlocking {
        // Setup mocks
        coEvery { productRepository.save(any()) } just Runs
        coEvery { outboxRepository.save(any()) } just Runs
        
        // Perform test
        with(testEngine) {
            handleRequest(HttpMethod.Post, "/api/v1/products") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"name":"Test Product","price":19.99,"currency":"USD"}""")
            }.apply {
                assertEquals(HttpStatusCode.Created, response.status())
                assertNotNull(response.content)
            }
        }
        
        // Verify
        coVerify { productRepository.save(any()) }
        coVerify { outboxRepository.save(any()) }
    }
}
```

## Full Example

Check out our [complete Ktor example](https://github.com/melsardes/structus-kotlin-examples/ktor-sample) on GitHub to see a full integration with Structus.

## Next Steps

Now that you've integrated Structus with Ktor, explore our [PostgreSQL Integration](postgres) guide to learn how to use Structus with PostgreSQL.