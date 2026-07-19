# chatapps-graph

<!-- sync:description -->
Backend service for a real-time chat application built with Spring Boot, GraphQL, and WebSocket.
Exposes REST + GraphQL APIs for chat messaging, user profiles, group management, and media handling.
<!-- end-sync:description -->

---

## Tech Stack

<!-- sync:tech-stack -->
| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5.8 |
| API | GraphQL (`spring-graphql`) + REST (`spring-web`) |
| Real-time | WebSocket + STOMP |
| Security | Spring Security + OAuth2 Resource Server (Keycloak JWT) |
| Database | PostgreSQL 17 |
| ORM / Migration | Spring Data JPA + Flyway |
| Cache | Redis (Pub/Sub + Cache) |
| Message Queue | RabbitMQ (`spring-amqp`) |
| File Storage | MinIO (S3-compatible) |
| Push Notification | Firebase Admin SDK (FCM) |
| Circuit Breaker | Resilience4J |
| Observability | Micrometer + Prometheus + Spring Actuator |
| HTML Parsing | Jsoup (link preview) |
| Build | Gradle 8 + Gradle Wrapper |
| Native Image | GraalVM Native Build Tools |
| Container | Docker + Docker Compose |
<!-- end-sync:tech-stack -->

---

## Directory Structure

<!-- sync:directory-structure -->
```text
chatapps-graph/
├── src/
│   ├── main/
│   │   ├── java/id/xyz/chatapps_graph/
│   │   │   ├── ChatappsGraphApplication.java       # Entry point
│   │   │   ├── domain/                             # Domain layer (entities, repos, enums)
│   │   │   │   ├── entity/                         # JPA entities (User, Message, Conversation, …)
│   │   │   │   ├── enums/                          # Domain enums
│   │   │   │   ├── exception/                      # Domain exceptions
│   │   │   │   ├── factory/                        # Domain factories
│   │   │   │   └── repository/                     # Spring Data JPA repositories
│   │   │   ├── applications/                       # Application layer (use cases, services)
│   │   │   │   ├── service/                        # Business logic services
│   │   │   │   └── usecase/                        # Use case implementations
│   │   │   ├── framework/                          # Web/API layer (entry points)
│   │   │   │   ├── controller/                     # REST + GraphQL controllers
│   │   │   │   │   ├── BaseApiController.java
│   │   │   │   │   ├── ChatController.java
│   │   │   │   │   ├── ChatWebSocketHandler.java
│   │   │   │   │   ├── GroupController.java
│   │   │   │   │   ├── ProfileController.java
│   │   │   │   │   ├── DeviceController.java
│   │   │   │   │   ├── ExportController.java
│   │   │   │   │   ├── PresenceController.java
│   │   │   │   │   ├── DLQReplayController.java
│   │   │   │   │   └── v2/                         # API v2 versioned controllers
│   │   │   │   ├── dto/                            # Request/Response DTOs (records)
│   │   │   │   └── mapper/                         # DTO ↔ Domain mappers
│   │   │   └── infrastructure/                     # Infrastructure layer (adapters)
│   │   │       ├── config/                         # Spring configuration beans
│   │   │       │   ├── security/                   # Security config (JWT, CORS)
│   │   │       │   ├── rabbitmq/                   # RabbitMQ queue/exchange config
│   │   │       │   ├── redis/                      # Redis config
│   │   │       │   ├── websocket/                  # WebSocket + STOMP config
│   │   │       │   ├── storage/                    # MinIO config
│   │   │       │   ├── properties/                 # @ConfigurationProperties classes
│   │   │       │   └── exception/                  # @ControllerAdvice global handler
│   │   │       ├── aspect/                         # AOP aspects (rate limiting, logging)
│   │   │       ├── constant/                       # SQL query constants, string constants
│   │   │       ├── mapper/                         # Infrastructure mappers
│   │   │       ├── monitoring/                     # Micrometer metrics
│   │   │       ├── notification/                   # FCM notification service
│   │   │       ├── scheduler/                      # @Scheduled tasks (disappearing messages)
│   │   │       ├── service/                        # Infrastructure services (MinIO, email, etc.)
│   │   │       └── utility/                        # Utility classes
│   │   └── resources/
│   │       ├── application.yaml                    # Main config
│   │       ├── application-local.yaml              # Local override config
│   │       ├── graphql/
│   │       │   └── schema.graphqls                 # GraphQL schema definition
│   │       ├── db/migration/                       # Flyway migration scripts (V1.0.x → V1.5.x)
│   │       └── i18n/                               # Locale message files (id, en)
│   └── test/                                       # Unit & integration tests
├── build.gradle                                    # Gradle build config
├── docker-compose.yaml                             # Local infra (Postgres, Redis, RabbitMQ, MinIO, Keycloak)
├── Dockerfile                                      # Production container build
├── Dockerfile.jvm                                  # JVM-based container
└── Dockerfile.native                               # GraalVM native image container
```
<!-- end-sync:directory-structure -->

---

## Development Commands

<!-- sync:dev-commands -->
```bash
# Start all local infrastructure services
docker-compose up -d postgres redis keycloak minio

# Run the application (dev mode with hot reload)
./gradlew bootRun

# Run all tests with coverage report
./gradlew test

# Build fat JAR
./gradlew build

# Build GraalVM native image
./gradlew nativeCompile

# Build Docker image (Cloud Native Buildpacks)
./gradlew bootBuildImage

# Run only infrastructure (no app container)
docker-compose up -d --scale app=0
```
<!-- end-sync:dev-commands -->

## Developer Reference

For full developer guidelines, architectural decisions, and active task details, see
[`project_context.md`](project_context.md).
