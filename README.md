# Product API — Spring Boot + Gradle

Production-style REST API assignment implemented with **Java 17, Spring Boot, Gradle, Spring Data JPA/Hibernate, MySQL, Spring Security/JWT, refresh-token rotation, JUnit 5, Mockito, H2, OpenAPI and Docker Compose**.

## Architecture

`Controller → Service → Repository → MySQL`

- **Controller:** versioned REST resources, validation, HTTP semantics and API DTOs.
- **Service:** existing product business rules and transactions.
- **Repository:** Spring Data JPA persistence.
- **DTO:** request/response models isolate the API contract from JPA entities. Pagination is returned through `PageResponse<T>` instead of exposing Spring Data `Page`.
- **Security:** stateless JWT access tokens + persisted, SHA-256 hashed refresh tokens with rotation.
- **Exception layer:** one JSON error contract for application, validation and security errors.
- **Async:** mutation audit is dispatched through a bounded Spring task executor.

## API

### Authentication

- `POST /api/v1/auth/register` — creates USER
- `POST /api/v1/auth/login` — returns access + refresh tokens
- `POST /api/v1/auth/refresh` — rotates refresh token and returns a new token pair

### Products

- `GET /api/v1/products?page=0&size=20&sortBy=id&direction=asc`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products` — USER/ADMIN
- `PUT /api/v1/products/{id}` — USER/ADMIN
- `DELETE /api/v1/products/{id}` — ADMIN only
- `GET /api/v1/products/{id}/items`
- `POST /api/v1/products/{id}/items` — USER/ADMIN

All protected endpoints require `Authorization: Bearer <access-token>`.

### Pagination contract

The collection endpoint accepts `page`, `size` (1–100), `sortBy` and `direction`. The controller converts the internal Spring Data `Page<ProductResponse>` into the public `PageResponse<ProductResponse>` DTO:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Supported sort fields: `id`, `productName`, `createdBy`, `createdOn`, `modifiedBy`, `modifiedOn`.

## Database

Core assignment tables are represented by the existing JPA entities:

- `product`: id, product_name, created_by, created_on, modified_by, modified_on
- `item`: id, product_id, quantity

Security adds `app_user` and `refresh_token` tables. Existing indexes cover product audit fields, the item foreign key, usernames, refresh-token hashes and expiry.

For production, activate the `prod` profile and use a proper schema migration pipeline before switching `ddl-auto` to `validate`.

## Configuration

Secrets and database credentials are supplied through environment variables; they are not required to be committed to source control. Copy `.env.example` to `.env` for local Docker use and replace the values.

Important variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (use a strong random value of at least 32 bytes)
- `JWT_ACCESS_EXPIRATION_MS`
- `JWT_REFRESH_EXPIRATION_MS`
- `ALLOWED_ORIGINS`
- `REQUIRE_HTTPS`

## Run locally

Prerequisites: JDK 17+ and MySQL.

```bash
./gradlew bootRun
```

Or start MySQL + API with Docker:

```bash
docker compose up --build
```

API: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Test

```bash
./gradlew test
```

Tests use H2 and include Mockito service tests, controller tests and Spring Boot integration tests.

## HTTPS

Local development uses HTTP. The `prod` profile sets `REQUIRE_HTTPS=true` and `ddl-auto=validate`. Run production behind a TLS-terminating reverse proxy/load balancer and forward `X-Forwarded-Proto` so Spring Security can enforce HTTPS correctly.

## Refresh-token rotation

Refresh tokens are random opaque values. Only SHA-256 hashes are stored in the database. A successful refresh revokes the presented token and persists a replacement token hash. Expired refresh tokens are cleaned hourly.

A revoked or expired refresh token cannot be used again. For higher-security deployments, add token-family reuse detection and revoke the entire token family when reuse is detected.
