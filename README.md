# Kunsthaben

Backend API for Kunsthaben, a platform where artists list, browse, and favorite artworks, and message each other about
them.

## Stack

- **Java 21**, **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Security)
- **PostgreSQL** as the primary datastore
- **JWT** (jjwt) for authentication, delivered via an HTTP-only cookie rather than a bearer header
- **AWS S3** for artwork image storage
- **Lombok** to cut down entity/DTO boilerplate
- **JUnit 5 + Mockito** for unit tests, `@SpringBootTest`/`@WebMvcTest` for integration tests
- **Maven** (wrapper included, no local install required)

## Architecture

Each business domain is a self-contained package under `io.everyonecodes.project_module`, following the same layering:

- **Entity**: the JPA-mapped domain object
- **Repository**: a `JpaRepository` interface, with extra derived-query methods where needed
- **Service**: business logic and orchestration; the only layer allowed to touch repositories directly
- **Controller**: the REST endpoints; maps DTOs in and out, never exposes entities
- **DTOs**: request/response shapes, kept separate from entities to avoid leaking fields (e.g. password hashes) and to
  sidestep bidirectional-relationship serialization loops

### Modules

| Package          | Responsibility                                                                     |
|------------------|------------------------------------------------------------------------------------|
| `auth`           | Login, JWT issuance/validation, auth cookie handling, CSRF, Spring Security config |
| `users`          | User accounts and profiles                                                         |
| `artworks`       | Artwork CRUD, ownership checks, filtering/search                                   |
| `artworkimages`  | Artwork image upload and metadata, backed by S3                                    |
| `classification` | Categories, media, and support types used to classify artworks                     |
| `chats`          | Conversations and messages between users                                           |
| `favorites`      | User's favorite artists and favorite artworks                                      |
| `storage`        | S3 client configuration and upload/delete operations                               |
| `exceptions`     | Centralized exception types and a global `@ControllerAdvice` handler               |

Authentication uses stateless JWTs: the server holds no session, and each request is verified from the token's signature
alone. The token is delivered in an HTTP-only.
A companion `/auth/csrf` endpoint issues a CSRF token, required for cookie-based auth to stay safe against cross-site
requests.

## Project layout

```
src/main/java/io/everyonecodes/project_module/   application code, one package per domain
src/main/resources/application.properties        configuration (datasource, JWT, S3, uploads)
src/test/java/...                                 unit and integration tests, mirroring main packages
```

## Running locally

1. Start a local PostgreSQL instance and create the `kunsthaben_db` database (or adjust `spring.datasource.*` in
   `application.properties`).
2. Export a `JWT_SECRET` environment variable (used to sign tokens).
3. Run the app:

   ```
   ./mvnw spring-boot:run
   ```

The schema is recreated on every start (`ddl-auto=create-drop`), so this is a development setup, not a production one.

## Tests

```
./mvnw test
```