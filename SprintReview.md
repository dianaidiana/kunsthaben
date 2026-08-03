# Sprint reviews

Prepare a short (~ 5-10 min) presentation that shows what you’ve been working on since the last Sprint Review. Include
the following:

- What have you been working on, in terms of finished features (present them in your running project)?
- What have you been working on, in terms of code (show in code)?
- What are the most important (technical) things you learned?
- What are you struggling with?
- Which parts of the roadmap do you think you have completed?
- Which parts do you feel you have not yet completed in this sprint, and would like to continue to work on?
- Did you work on your application documents (blog post/portfolio)? What did you accomplish there?
- What are you going to work on next?

## August 3th:

- What have you been working on, in terms of code (show in code)?

Dividing the project in packages for each feature containing:

1. Entities
2. Repository
3. When sensitive data or risk of JSON recursion, corresponding DTO classes
3. Service
4. Controller

For each Controller, a SpringBootTest that test each endpoint and for service a test with mock repository (only when
service has logic to be tested).

- What have you been working on, in terms of finished features (present them in your running project)?

### Features finished (subject to changes):

#### Classification: category, media, support

These are pre-defined and are inserted through the DatabaseInitializer (the definition is yet not finished).

#### Category:

`GET /category` gets all the categories

`GET /category/{id}` gets category by id

`GET /category/code/{code}` gets category by code

#### Media:

`GET /media` gets all the media

`GET /media/{id}` gets media by id

`GET /media/code/{code}` gets media by code

`GET /media/category/{categoryId}` gets all the media for a corresponding category id

#### Support

`GET /support` gets all the supports

`GET /support/{id}` gets support by id

`GET /support/code/{code}` gets support by code

`GET /support/category/{categoryId}` gets all the supports for a corresponding category id

#### Users:

`POST /user/register` registers a new user

`GET /user/{id}` gets user by id

`PUT /user/{id}` updates an existing user

`DELETE /user/{id}` deletes a user

#### User favorite artists:

`POST /user/{userId}/favorite-artist/{artistId}` follows an artist

`DELETE /user/{userId}/favorite-artist/{artistId}` unfollows an artist

`GET /user/{userId}/favorite-artist` lists the artists a user follows

- What are the most important (technical) things you learned?

    - **DTOs**: needed when an entity has sensitive data, a recursion risk, or
      needs multiple response shapes.
    - **Validation**: combining DB `CHECK` constraints and Java-level validation:
        - validating factory (`Frame`/`Dimensions`) so an invalid object can't even be constructed, not
          just rejected on save.
        - Spring validation vs JPA: on request's DTO and Entities. Has two separate trigger points: the DTO validation
          on
          the controller we need to invoke it by annotation `@Valid` and on the Entities it is automatically handled by
          JPA.
        - Error handling: both throw different errors so you must be careful with exception handling.
    - **Bean Validation has two separate trigger points**: `@Valid` on a DTO in the controller, vs.
      automatic validation on `@Entity` classes at save time via Hibernate/JPA, with no `@Valid` needed.
    - **Centralized exception handling**: one `@RestControllerAdvice`, and exceptions named after HTTP
      semantics (`NotFoundException`, `ConflictException`, `ForbiddenException`).
    - **Testing with Mockito**: controller tests verify the HTTP contract with the service mocked;
      service tests verify real logic in isolation; `ArgumentCaptor` for inspecting what a mock was
      actually called with (e.g. verifying passwords get hashed).
    - **JPA specifics**: composite keys via `@EmbeddedId`, and `@Transactional` enabling Hibernate's
      automatic "dirty-checking" so an update doesn't need an explicit `save()` call. (dirty-checking: This concept
      enables
      Hibernate to detect changes in the state of entities automatically, allowing it to update entities automatically.)


- What are you struggling with?

Not a struggle but still many concepts are unclear and need to ask multiple times the same questions, there's a lot of
new information to learn.

Git branching (only using Main. not sure how to organize myself).

- Which parts of the roadmap do you think you have completed?
- Did you work on your application documents (blog post/portfolio)? What did you accomplish there?

I have two old projects I did with typescript/React that I'm trying to polish and make them showable.

- What are you going to work on next?
  I want to finish everything related to Artwork:
    - Standard CRUD operations
    - Filtering: I want to learn about JPA Specification API in order to chain multiple filters together
    - I have questions about uploading images (for the moment I just receive url of where it is saved)

create github issue
tags for issues
sprint 1 is done

packages all lower case
object storage
local servers
amazon s3