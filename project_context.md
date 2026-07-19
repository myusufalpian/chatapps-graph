# Project Context: chatapps-graph

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

## Core Directory Structure

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

---

## Key Domain Entities

| Entity | Description |
|---|---|
| `User` | App user, linked to Keycloak via `userUuid` |
| `UserOtp` | OTP record for phone verification |
| `UserDevice` | FCM device token per user |
| `UserLinkedAccount` | OAuth2 / social linked accounts |
| `Conversation` | A chat thread (direct or group) |
| `ConversationParticipant` | Junction: user ↔ conversation |
| `Message` | A single message in a conversation |
| `MessageReceipt` | Delivery/read receipt per message per user |
| `MessageReaction` | Emoji reactions on messages |
| `MessageEditHistory` | Audit log for message edits |
| `Attachment` | File/media metadata linked to a message |
| `LinkPreview` | Cached Open Graph preview for URLs |
| `Group` | Group chat metadata |
| `GroupMember` | Group membership with roles (OWNER/ADMIN/MEMBER) |
| `ExportJob` | Async chat export job status |
| `DlqReplayAudit` | Dead-letter queue replay audit |
| `MstAbout` | Master data for profile "About" options |

---

## GraphQL Schema Overview

```graphql
type Query {
    myProfile: Profile!
    userProfile(userUuid: String!): Profile!
    contacts: [ContactEntry!]!
}

type Mutation {
    updateMyFullName(fullName: String!): Profile!
    updateMyStatus(aboutDesc: String!): Profile!
    updateMyProfilePhoto(photoUrl: String!): Profile!
    updateContactDisplayName(contactUserUuid: String!, displayName: String!): ContactEntry!
    syncContacts(phoneNumbers: [String!]!): [ContactEntry!]!
}
```

---

## Database Migration History (Flyway)

| Version | Description |
|---|---|
| V1.0.2 | Initial schema — users, OTP, messages |
| V1.0.3 | Contact & profile photo + OTP audit columns |
| V1.0.4 | User linked accounts |
| V1.0.5 | Chat schema redesign |
| V1.0.6 | Hide read receipt flag |
| V1.0.7 | Conversation list columns |
| V1.0.8 | Message reactions & forwarding |
| V1.0.9 | Presence & message search |
| V1.1.0 | User device (FCM) & thumbnails |
| V1.2.0 | Group chat management |
| V1.3.0 | Message edit features |
| V1.4.0 | Disappearing messages |
| V1.4.1 | Link preview |
| V1.5.0 | Voice message metadata & export jobs |

---

## Sprint Status

<!-- sync:sprint-status -->
| Sprint | Features | Status |
|---|---|---|
| Sprint 1 | Typing Indicator, Reactions, Forwarding, Rate Limiting | ✅ Done |
| Sprint 2A | Online/Last Seen, Message Search | ✅ Done |
| Sprint 2B | FCM Push Notifications, Media Thumbnail | ✅ Done |
| Sprint 3 | Group Chat Management (OWNER/ADMIN/MEMBER) | ✅ Done |
| Sprint 4 | Message Edit, Read Receipt Privacy, i18n | ✅ Done |
| Sprint 5 | Message Delivery Status (Double Tick) | ✅ Done |
| Sprint 6 | Disappearing Messages, Link Preview | ✅ Done |
| Sprint 7 | WebSocket Multi-Instance (Redis Pub/Sub), RabbitMQ, API Versioning | ✅ Done |
| Sprint 8 | Voice Metadata, Chat Export, Analytics/Monitoring, E2EE | 🚧 In Progress |
<!-- end-sync:sprint-status -->

## Active Tasks (Sprint 8)

- [ ] **13. Voice Message Metadata** — waveform data extraction & storage
- [ ] **19. Message Backup/Export** — JSON/PDF export, archive to MinIO
- [ ] **21. Analytics & Monitoring** — Micrometer, Prometheus, Grafana dashboards
- [ ] **20. End-to-End Encryption (E2EE)** — Signal Protocol design & implementation

---

## Architect Review & Issues Backlog

Hasil code review menyeluruh tersedia di dua file:

| File | Deskripsi |
|---|---|
| [`ARCHITECT_REVIEW.md`](ARCHITECT_REVIEW.md) | Laporan lengkap: critical issues, code smells, miss implementasi, dan rekomendasi fitur |
| [`ISSUES_BACKLOG.md`](ISSUES_BACKLOG.md) | Checklist actionable dengan referensi file & baris yang perlu diperbaiki |

Ringkasan temuan:
- **6 Critical** — N+1 receipt insert, @Value violation, God Class, raw SQL inline, double-serialize Redis, memory risk scheduler
- **9 Code Smells** — duplicate logic, non-final constant, emoji di log, rate limit bypass di WebSocket
- **7 Miss Implementasi** — `hasMore` bug, silent WebSocket auth, missing FK/index, ambiguous About schema
- **8 Fitur Backlog** — User Blocking, Idempotency Key, Message Pinning, @Mention, Circuit Breaker wirings

## Architecture Decisions

| Decision | Rationale | Upgrade Path |
|---|---|---|
| No Eureka / Service Mesh | Single-service monolith is sufficient; service discovery adds overhead. | Extract microservices and add Spring Cloud Gateway if traffic demands horizontal domain split. |
| Redis Pub/Sub (not Kafka) for WebSocket fan-out | Lower ops complexity for current single-topic broadcast. | Migrate to Kafka with dedicated topics per conversation if message volume exceeds Redis throughput. |
| RabbitMQ for async tasks (FCM, compression, link preview) | Simpler topic model for fire-and-forget tasks. | Upgrade to Kafka Streams if event replay or exactly-once delivery becomes a requirement. |
| `@ConfigurationProperties` only (no `@Value`) | Centralized, type-safe config binding; reduces scattered `@Value` coupling. | No upgrade needed; this is the target state. |
| Constructor injection only (no `@Autowired`) | Explicit dependencies; testable without Spring context. | No upgrade needed. |
| Flyway for migrations (not Liquibase) | SQL-first, minimal config; no XML/YAML overhead. | Switch to Liquibase if cross-database portability or rollback tracking becomes required. |
| JPA Projections for read queries | Avoids loading full entity graphs for list/search endpoints; reduces N+1 risk. | No upgrade needed; extend projection interfaces as new fields are required. |
| GraalVM Native Image support configured | Enables lightweight container deployments on resource-constrained environments. | Ceiling: reflection-heavy libraries may need manual GraalVM hints. |
