# Product API

Production-style REST API for managing products and their associated items, implemented with Java 17 and Spring Boot. The application provides CRUD operations, pagination, JWT-based authentication, refresh-token rotation, role-based authorization, validation, centralized error handling, OpenAPI documentation, MySQL persistence, automated tests, and Docker support.

## Features

* Product CRUD operations
* Product item management
* Paginated and sortable product/item listing
* Request validation using Jakarta Bean Validation
* JWT access-token authentication
* Opaque refresh tokens with SHA-256 hashing
* Refresh-token rotation and revocation
* `USER` and `ADMIN` roles
* Role-based method authorization
* BCrypt password hashing
* Stateless Spring Security configuration
* CORS configuration
* Centralized JSON error responses
* Swagger UI / OpenAPI documentation
* MySQL database support
* H2-based test configuration
* Automatic cleanup of expired refresh tokens
* Automatic creation of an initial `ADMIN` account
* Configurable asynchronous task executor
* Spring Boot integration and unit/controller tests
* Multi-stage Docker image
* Docker Compose setup for MySQL + API

---

## Technology Stack

| Technology        | Version / Usage                                                               |
| ----------------- | ----------------------------------------------------------------------------- |
| Java              | 17                                                                            |
| Spring Boot       | 3.5.5                                                                         |
| Gradle            | 8.10.2                                                                        |
| Spring Web        | REST API                                                                      |
| Spring Data JPA   | Persistence                                                                   |
| Hibernate         | ORM                                                                           |
| Spring Security   | Authentication and authorization                                              |
| JJWT              | 0.12.6                                                                        |
| MySQL             | 8.4 in Docker Compose                                                         |
| H2                | Automated tests                                                               |
| Flyway            | MySQL integration dependency; no application migrations are currently present |
| Springdoc OpenAPI | 2.8.9                                                                         |
| Lombok            | Boilerplate reduction                                                         |
| JUnit 5 / Mockito | Testing                                                                       |
| Docker            | Containerization                                                              |

The project uses the Gradle wrapper, so Gradle does not need to be installed separately when running the project locally.

---

## Project Structure

```text
product-api/
├── src/
│   ├── main/
│   │   ├── java/com/example/productapi/
│   │   │   ├── config/
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── DataInitializer.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── SecurityExceptionHandlerConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ItemController.java
│   │   │   │   └── ProductController.java
│   │   │   ├── dto/
│   │   │   │   ├── auth/
│   │   │   │   ├── common/
│   │   │   │   ├── item/
│   │   │   │   └── product/
│   │   │   ├── entity/
│   │   │   │   ├── AppUser.java
│   │   │   │   ├── Item.java
│   │   │   │   ├── Product.java
│   │   │   │   ├── RefreshToken.java
│   │   │   │   └── Role.java
│   │   │   ├── exception/
│   │   │   ├── pagination/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   ├── service/
│   │   │   │   └── implementation/
│   │   │   ├── util/
│   │   │   └── ProductApiApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── schema-reference.sql
│   └── test/
│       ├── java/
│       └── resources/
├── gradle/
│   └── wrapper/
├── .dockerignore
├── .env.example
├── Dockerfile
├── docker-compose.yml
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle
```

Generated directories such as `build/`, `.gradle/`, and IDE metadata are not part of the application source.

---

## Architecture

The application follows a layered architecture:

```text
HTTP Request
     │
     ▼
Controller
     │
     ▼
Service
     │
     ▼
Repository
     │
     ▼
MySQL / H2
```

Security is applied before controller execution:

```text
HTTP Request
     │
     ▼
Spring Security
     │
     ├── Public authentication/OpenAPI endpoints
     │
     └── JWT Authentication Filter
              │
              ▼
        UserDetailsService
              │
              ▼
        SecurityContext
              │
              ▼
          Controller
```

### Controllers

Controllers expose the versioned REST API under `/api/v1`.

* `AuthController` handles registration, login, and refresh-token operations.
* `ProductController` handles product operations and product-specific items.
* `ItemController` handles direct item lookup and paginated item listing.

Controllers use DTOs rather than exposing JPA entities directly.

### Services

Business operations are implemented by:

* `ProductServiceImpl`
* `ItemServiceImpl`
* `AuthServiceImpl`
* `UserDetailsServiceImpl`
* `RefreshTokenCleanupServiceImpl`

Transactions are managed using Spring's `@Transactional`.

### Repositories

Spring Data JPA repositories provide database access:

* `ProductRepository`
* `ItemRepository`
* `AppUserRepository`
* `RefreshTokenRepository`

### Entities

The main persisted entities are:

```text
AppUser
   │
   └── RefreshToken

Product
   │
   └── Item
```

A product can contain multiple items. Each item references exactly one product.

---

## Application Flow

### Product request

1. Client sends a request to `/api/v1/products`.
2. Spring Security checks authentication.
3. `JwtAuthenticationFilter` extracts and validates the bearer token.
4. The authenticated user's authorities are placed into the Spring Security context.
5. `ProductController` validates the request and delegates to `ProductService`.
6. `ProductServiceImpl` performs the business operation.
7. `ProductRepository` persists or retrieves the entity.
8. The entity is converted into a response DTO.
9. The controller returns the HTTP response.

### Authentication flow

```text
Register
   │
   ▼
Validate request
   │
   ▼
Hash password with BCrypt
   │
   ▼
Persist USER
```

```text
Login
   │
   ▼
AuthenticationManager
   │
   ▼
Validate username/password
   │
   ▼
Generate JWT access token
   │
   ├── Generate random refresh token
   │
   └── Store only SHA-256 refresh-token hash
   │
   ▼
Return access + refresh tokens
```

### Refresh-token flow

The refresh endpoint:

1. Hashes the supplied refresh token using SHA-256.
2. Looks up the stored hash using a pessimistic write lock.
3. Rejects revoked or expired tokens.
4. Revokes the current refresh token.
5. Generates a new access token and refresh token.
6. Stores the replacement refresh-token hash.
7. Associates the replacement hash with the old token.

Expired refresh tokens are automatically deleted hourly by the scheduled cleanup service.

---

## Authentication and Authorization

Authentication uses stateless JWT access tokens.

### Access token

The JWT contains:

* Username as the subject
* Issued-at timestamp
* Expiration timestamp
* User roles

Clients must send the token using:

```http
Authorization: Bearer <access-token>
```

### Roles

The application defines two roles:

* `USER`
* `ADMIN`

Authorization is enforced using Spring Security method security.

| Operation         |  USER  |  ADMIN |
| ----------------- | :----: | :----: |
| Register          | Public | Public |
| Login             | Public | Public |
| Refresh token     | Public | Public |
| List products     |   Yes  |   Yes  |
| Get product       |   Yes  |   Yes  |
| Create product    |   Yes  |   Yes  |
| Update product    |   Yes  |   Yes  |
| Delete product    |   No   |   Yes  |
| Add product item  |   Yes  |   Yes  |
| Get product items |   Yes  |   Yes  |
| Get item by ID    |   Yes  |   Yes  |
| List items        |   Yes  |   Yes  |

Authentication endpoints and OpenAPI endpoints are publicly accessible. Product and item APIs require authentication.

---

## Initial Admin Account

`DataInitializer` creates an `ADMIN` account on application startup if an account with username `admin` does not already exist.

The credentials are currently defined directly in `DataInitializer.java` as development seed data.

**Important:** this seeded credential should not be relied upon for a production deployment. Change the implementation or remove the seed account before using the application in a real production environment.

---

## API Endpoints

Base URL:

```text
http://localhost:8080
```

Base API path:

```text
/api/v1
```

### Authentication

#### Register

```http
POST /api/v1/auth/register
Content-Type: application/json
```

Request:

```json
{
  "username": "john",
  "password": "password123"
}
```

Username length must be between 3 and 100 characters. Password length must be between 8 and 100 characters.

Returns `201 Created`.

#### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

Request:

```json
{
  "username": "john",
  "password": "password123"
}
```

Response contains:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

#### Refresh

```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

Request:

```json
{
  "refreshToken": "..."
}
```

A successful refresh rotates the refresh token and returns a new token pair.

---

### Products

#### List products

```http
GET /api/v1/products
```

Optional query parameters:

```text
page
size
sortBy
direction
```

Defaults:

```text
page=0
size=10
sortBy=id
direction=asc
```

Maximum page size is `100`.

Supported product sort fields:

```text
id
productName
createdBy
createdOn
modifiedBy
modifiedOn
```

Example:

```http
GET /api/v1/products?page=0&size=20&sortBy=createdOn&direction=desc
Authorization: Bearer <access-token>
```

Response:

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

#### Get product

```http
GET /api/v1/products/{productId}
Authorization: Bearer <access-token>
```

Returns `200 OK` or `404 Not Found`.

#### Create product

```http
POST /api/v1/products
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "productName": "Example Product"
}
```

Returns `201 Created`.

#### Update product

```http
PUT /api/v1/products/{productId}
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "productName": "Updated Product"
}
```

Returns `200 OK`.

#### Delete product

```http
DELETE /api/v1/products/{productId}
Authorization: Bearer <admin-access-token>
```

Requires `ADMIN`.

Returns `204 No Content`.

---

### Product Items

#### Add item to product

```http
POST /api/v1/products/{productId}/items
Authorization: Bearer <access-token>
Content-Type: application/json
```

Request:

```json
{
  "quantity": 10
}
```

`quantity` must be a positive integer.

Returns `201 Created`.

#### Get items for a product

```http
GET /api/v1/products/{productId}/items
Authorization: Bearer <access-token>
```

Returns all items for the specified product ordered by item ID.

---

### Items

#### Get item

```http
GET /api/v1/items/{itemId}
Authorization: Bearer <access-token>
```

#### List items

```http
GET /api/v1/items?page=0&size=10&sortBy=id&direction=asc
Authorization: Bearer <access-token>
```

Supported item sort fields:

```text
id
product
quantity
```

---

## Error Responses

Application errors use a common JSON structure:

```json
{
  "timestamp": "2026-09-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/products",
  "validationErrors": {
    "productName": "must not be blank"
  }
}
```

The application handles, among others:

* `400 Bad Request`
* `401 Unauthorized`
* `403 Forbidden`
* `404 Not Found`
* `409 Conflict`
* `500 Internal Server Error`

Validation errors are returned through the `validationErrors` field.

---

## OpenAPI / Swagger

OpenAPI is configured through Springdoc.

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

The OpenAPI configuration defines a bearer JWT security scheme named `bearerAuth`.

---

## Database

The application uses MySQL by default.

Default database configuration points to:

```text
jdbc:mysql://localhost:3306/productdb2
```

The application uses Hibernate/JPA to create or update the schema during normal development when `DDL_AUTO=update`.

The main tables created from the JPA entities are:

```text
product
item
app_user
refresh_token
```

Relationships:

```text
product 1 ─────── * item
app_user 1 ────── * refresh_token
```

### Database configuration

The following environment variables are supported:

| Variable      | Purpose                   | Default                                   |
| ------------- | ------------------------- | ----------------------------------------- |
| `DB_URL`      | JDBC connection URL       | MySQL `productdb2` on localhost           |
| `DB_USERNAME` | Database username         | `root`                                    |
| `DB_PASSWORD` | Database password         | Development fallback in `application.yml` |
| `DDL_AUTO`    | Hibernate schema strategy | `update`                                  |
| `SERVER_PORT` | HTTP server port          | `8080`                                    |

### Flyway

The project includes the Flyway MySQL dependency, but there are currently **no Flyway migration scripts** under the project.

`src/main/resources/db/schema-reference.sql` is a reference SQL file and is not the application's migration pipeline.

The production profile sets:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Therefore, a production database must already contain a schema compatible with the JPA entities. The current project does not provide production Flyway migrations to create that schema automatically.

---

## Configuration

Main configuration:

```text
src/main/resources/application.yml
```

Production profile:

```text
src/main/resources/application-prod.yml
```

The application supports:

| Environment variable        | Purpose                      |
| --------------------------- | ---------------------------- |
| `DB_URL`                    | Database JDBC URL            |
| `DB_USERNAME`               | Database username            |
| `DB_PASSWORD`               | Database password            |
| `DDL_AUTO`                  | Hibernate DDL mode           |
| `SERVER_PORT`               | Server port                  |
| `JWT_SECRET`                | JWT signing secret           |
| `JWT_ACCESS_EXPIRATION_MS`  | Access-token lifetime        |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh-token lifetime       |
| `ALLOWED_ORIGINS`           | Comma-separated CORS origins |
| `REQUIRE_HTTPS`             | Enables HTTPS enforcement    |

Current default token lifetimes are:

```text
Access token:  900000 ms   (15 minutes)
Refresh token: 604800000 ms (7 days)
```

The JWT secret must be sufficiently long for the configured HMAC key and should be replaced with a strong randomly generated secret outside development.

### CORS

CORS allowed origins are configured through:

```text
ALLOWED_ORIGINS
```

Multiple origins can be supplied as a comma-separated list.

The default application configuration allows:

```text
http://localhost:3000
http://localhost:8080
```

---

## Local Development

### Prerequisites

Install:

* JDK 17 or newer
* MySQL 8.x

Docker is optional when running the application directly.

### Database

Create the database:

```sql
CREATE DATABASE productdb2;
```

Configure the required database environment variables if the local MySQL credentials differ from the development defaults.

### Run with Gradle

Linux/macOS:

```bash
./gradlew bootRun
```

Windows:

```bat
gradlew.bat bootRun
```

The API starts on:

```text
http://localhost:8080
```

### Build

Linux/macOS:

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

### Run tests

Linux/macOS:

```bash
./gradlew test
```

Windows:

```bat
gradlew.bat test
```

The test configuration uses an in-memory H2 database with MySQL compatibility mode and `create-drop` schema handling.

---

## Docker

The project already contains both a `Dockerfile` and `docker-compose.yml`.

The Docker setup consists of:

```text
MySQL 8.4
    │
    │ productdb2
    ▼
Product API
    │
    ▼
Port 8080
```

### Docker Compose

For local containerized development:

```bash
docker compose up --build
```

The Compose configuration starts:

* `product-api-mysql`
* `product-api`

The API waits for MySQL's health check before starting.

API:

```text
http://localhost:8080
```

MySQL:

```text
localhost:3306
```

### Environment file for Compose

Copy:

```text
.env.example
```

to:

```text
.env
```

and replace the placeholder credentials/secrets.

The Compose file supports:

```text
DB_USERNAME
DB_PASSWORD
MYSQL_ROOT_PASSWORD
JWT_SECRET
ALLOWED_ORIGINS
```

Compose supplies the API container with the MySQL hostname `mysql`, so the API uses a container-to-container JDBC URL rather than `localhost`.

The Compose configuration currently sets:

```text
DDL_AUTO=update
```

which is appropriate for the included development-oriented Docker setup but should not be treated as a production schema-management strategy.

### Stop containers

```bash
docker compose down
```

To remove the persisted MySQL volume as well:

```bash
docker compose down -v
```

The MySQL data is stored in the named Docker volume:

```text
mysql_data
```

---

## Docker Image

The existing Dockerfile uses a multi-stage build:

```text
Gradle + JDK 17
      │
      ├── compile
      ├── test/build lifecycle as requested by Gradle task
      └── bootJar
             │
             ▼
Eclipse Temurin JRE 17
             │
             ▼
        product-api
```

The runtime image contains the generated Spring Boot JAR rather than the complete source tree or Gradle environment.

The container exposes:

```text
8080
```

The application is started with:

```text
java -jar app.jar
```

The Dockerfile is consistent with the current Gradle project and generated artifact name and does not require modification.

---

## Production Profile

The project includes:

```text
application-prod.yml
```

Activate it with the standard Spring profile mechanism, for example:

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

The production profile changes:

```text
ddl-auto=validate
REQUIRE_HTTPS=true
```

HTTPS enforcement is therefore expected when the production profile is active.

The application also has:

```yaml
server:
  forward-headers-strategy: framework
```

which supports deployments behind a TLS-terminating reverse proxy/load balancer when the appropriate forwarded protocol headers are supplied.

A production deployment should use:

* A strong external JWT secret
* Strong database credentials
* HTTPS
* A pre-created/managed database schema
* A proper migration strategy
* Removal/replacement of the development admin seed credentials
* A production-grade reverse proxy or load balancer where appropriate

---

## Testing

The project contains tests covering:

* Product controller behavior
* Item controller behavior
* Authentication controller behavior
* Product service behavior
* Item service behavior
* Authentication service behavior
* User details service behavior
* JWT service behavior
* JWT authentication filter behavior
* Security configuration
* Security exception handling
* Async configuration
* Global exception handling
* Entity/DTO behavior
* Spring Boot integration behavior

The test environment uses:

```text
H2
JUnit 5
Mockito
Spring Boot Test
Spring Security Test
```

Run the complete suite with:

```bash
./gradlew test
```

or on Windows:

```bat
gradlew.bat test
```

---

## Important Operational Notes

### Development database schema

Development uses Hibernate's `ddl-auto=update` by default.

Do not assume this is suitable for production schema management.

### Production schema

The `prod` profile uses `validate`, and the project currently has no Flyway migration scripts. A production database therefore needs to be provisioned separately with a schema matching the JPA entities.

### Secrets

Do not commit real:

* JWT secrets
* Database passwords
* Production credentials

The `.env` file is ignored by Docker and should remain outside source control.

### Refresh tokens

Only SHA-256 hashes of refresh tokens are persisted. The original opaque refresh-token values are returned to clients but are not stored directly in the database.

Successful refresh requests revoke the existing token and issue a replacement.

### Expired-token cleanup

Expired refresh tokens are deleted by a scheduled task that runs hourly.

### Data initialization

An administrator is automatically seeded at startup when the `admin` username does not exist. This is development-oriented behavior and should be reviewed before production deployment.

---

## Troubleshooting

### Application cannot connect to MySQL

Verify:

```text
MySQL is running
Database productdb2 exists
DB_URL is correct
DB_USERNAME is correct
DB_PASSWORD is correct
```

For Docker Compose, the API must use the MySQL service hostname:

```text
mysql
```

rather than:

```text
localhost
```

### Port 8080 already in use

Change:

```text
SERVER_PORT
```

when running the application directly, or change the Docker/Compose port mapping when running containers.

### JWT errors

Verify that:

* `JWT_SECRET` is present and sufficiently long.
* The same secret is used for token generation and validation.
* The access token has not expired.
* The request contains `Authorization: Bearer <token>`.

### 401 Unauthorized

Check that the endpoint requires authentication and that the request contains a valid bearer token.

### 403 Forbidden

The user is authenticated but does not have the required role. For example, deleting a product requires `ADMIN`.

### Production startup failure

If the `prod` profile is active, Hibernate uses:

```text
ddl-auto=validate
```

A fresh database without the required tables will therefore fail validation. Provision the schema before starting the application.

---

## Useful Commands

### Run application

```bash
./gradlew bootRun
```

### Build application

```bash
./gradlew build
```

### Run tests

```bash
./gradlew test
```

### Start Docker environment

```bash
docker compose up --build
```

### Stop Docker environment

```bash
docker compose down
```

### Stop Docker environment and remove database volume

```bash
docker compose down -v
```

For Windows, use `gradlew.bat` instead of `./gradlew`.

---

## Summary

This project is a layered Spring Boot Product API with:

* RESTful product and item APIs
* DTO-based API contracts
* Spring Data JPA persistence
* MySQL runtime database
* H2 test database
* JWT authentication
* Refresh-token rotation
* USER/ADMIN authorization
* Centralized exception handling
* Pagination and sorting
* OpenAPI documentation
* Automated testing
* Docker and Docker Compose support
