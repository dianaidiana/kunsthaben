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

## API endpoints

### Auth

| Method | Path           | Description                                    |
|--------|----------------|-------------------------------------------------|
| GET    | `/auth/csrf`   | Issue a CSRF token, required before login/logout |
| POST   | `/auth/login`  | Authenticate and attach the JWT auth cookie      |
| POST   | `/auth/logout` | Clear the auth cookie                            |

### Users

| Method | Path                  | Description                                    |
|--------|-----------------------|--------------------------------------------------|
| POST   | `/user/register`      | Register a new user and log them in              |
| GET    | `/user/{id}`          | Get a user's profile                             |
| PUT    | `/user`               | Update the caller's own profile                  |
| PUT    | `/user/avatar`        | Upload/replace the caller's avatar image         |
| PUT    | `/user/banner`        | Upload/replace the caller's banner image         |
| DELETE | `/user/avatar`        | Remove the caller's avatar image                 |
| DELETE | `/user/banner`        | Remove the caller's banner image                 |
| DELETE | `/user`               | Delete the caller's own account                  |

### Artworks

| Method | Path                                | Description                                        |
|--------|---------------------------------------|-------------------------------------------------------|
| GET    | `/artwork/{id}`                       | Get artwork details                                    |
| GET    | `/artwork`                            | List artwork cards (paged)                             |
| GET    | `/artwork/search`                     | Search/filter artwork cards (paged)                    |
| GET    | `/user/{artistId}/artwork`            | List an artist's unsold artworks (paged)               |
| GET    | `/user/{artistId}/artwork/sold`       | List an artist's sold artworks (paged)                 |
| POST   | `/artwork`                            | Create an artwork for the caller                       |
| PUT    | `/artwork/{artworkId}`                | Update an artwork owned by the caller                  |
| DELETE | `/artwork/{artworkId}`                | Delete an artwork owned by the caller                  |
| PATCH  | `/artwork/{artworkId}/reserved`       | Mark the caller's artwork reserved/unreserved          |
| PATCH  | `/artwork/{artworkId}/sold`           | Mark the caller's artwork sold/unsold                  |

### Artwork images

| Method | Path                                       | Description                                    |
|--------|-----------------------------------------------|----------------------------------------------------|
| POST   | `/artwork/{artworkId}/images`                 | Upload an image for the caller's artwork            |
| PUT    | `/artwork/{artworkId}/images/reorder`         | Reorder the caller's artwork's images               |
| DELETE | `/artwork/{artworkId}/images/{imageId}`       | Delete an image from the caller's artwork            |

### Chats

| Method | Path                          | Description                                  |
|--------|-------------------------------|-------------------------------------------------|
| POST   | `/artwork/{artworkId}/chat`   | Start a chat about an artwork with its first message |
| GET    | `/chat`                       | List the caller's chats, newest activity first (paged) |
| GET    | `/chat/{chatId}/message`      | List messages in a chat, newest first (paged)    |
| POST   | `/chat/{chatId}/message`      | Send a message in a chat                         |
| PATCH  | `/chat/{chatId}/message/read` | Mark a chat's messages as read                   |

### Favorites

| Method | Path                             | Description                              |
|--------|-------------------------------------|---------------------------------------------|
| POST   | `/favorite-artist/{artistId}`       | Add an artist to the caller's favorites      |
| DELETE | `/favorite-artist/{artistId}`       | Remove an artist from the caller's favorites |
| GET    | `/favorite-artist`                  | List the caller's favorite artists           |

### Classification (category, media, support)

| Method | Path                              | Description                              |
|--------|-------------------------------------|---------------------------------------------|
| GET    | `/category`                         | List all categories                          |
| GET    | `/category/{id}`                    | Get a category by id                         |
| GET    | `/category/code/{code}`             | Get a category by code                       |
| GET    | `/media`                            | List all media types                         |
| GET    | `/media/{id}`                       | Get a media type by id                       |
| GET    | `/media/code/{code}`                | Get a media type by code                     |
| GET    | `/media/category/{categoryId}`      | List media types belonging to a category     |
| GET    | `/support`                          | List all support types                       |
| GET    | `/support/{id}`                    | Get a support type by id                     |
| GET    | `/support/code/{code}`             | Get a support type by code                   |
| GET    | `/support/category/{categoryId}`   | List support types belonging to a category   |

Aside from registration and the public GETs, these endpoints require authentication. Where an endpoint acts on the
caller's own resource (their profile, their artwork, their favorites), the id comes from `AuthPrincipal`, not from the
URL, so there is no separate ownership check to perform at that point. Where an id in the path names a different
resource, such as `artworkId` on the image endpoints, the service layer looks it up and throws a 403 if the caller
does not own it.

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