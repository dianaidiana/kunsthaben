# Diary of my project (or what I've learnt today):

## 21.07: Normalization overkill & making illegal states irrepresentable

While planning my database schema, I created this table:

```postgresql
CREATE TABLE IF NOT EXISTS artworks
(
    id          SERIAL PRIMARY KEY,
    artist_id   INTEGER        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       TEXT           NOT NULL,
    price       NUMERIC(12, 2) NOT NULL,
    year        INTEGER        NOT NULL,
    description TEXT           NOT NULL,
    city        TEXT           NOT NULL,
    postcode    TEXT           NOT NULL,
    dim_x       NUMERIC(8, 2)  NOT NULL,
    dim_y       NUMERIC(8, 2)  NOT NULL,
    dim_z       NUMERIC(8, 2),
    framed      BOOLEAN        NOT NULL,
    dim_frame_x NUMERIC(8, 2),
    dim_frame_y NUMERIC(8, 2),
    dim_frame_z NUMERIC(8, 2),
    category_id INTEGER        REFERENCES categories (id) ON DELETE SET NULL,
    medium_id   INTEGER        REFERENCES media (id) ON DELETE SET NULL,
    support_id  INTEGER        REFERENCES supports (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMPTZ,
    sold_at     TIMESTAMPTZ,
    reserved    BOOLEAN     DEFAULT false
);
```

I came up with the question: is this violating the make illegal states irrepresentable rule?

Looking at this section of the table:

```postgresql
framed BOOLEAN NOT NULL,
dim_frame_x NUMERIC(8, 2),
dim_frame_y NUMERIC(8, 2),
dim_frame_z NUMERIC(8, 2),
```

what happens if the artwork is framed but no dimensions are given? or if it is unframed but there are frames dimensions
given?
Should I instead create a separate table for framed dimensions and provide a nullable foreign key to the artworks
table?

The short answer is: **Keep them in the same table, but use `CHECK` constraints.**

In database design, moving a few optional columns to a separate table just to avoid `NULLs` is often considered **"
Normalization Overkill."** It makes your queries slower (requires more joins) and your Java code more complex without
providing much benefit.

How to handle **"making illegal states irrepresentable"** effectively:

### 1. The "PostgreSQL Way" (Constraints)

Instead of a separate table, use a `CHECK` constraint. This forces the database to reject "illegal states" (like having
frame dimensions when `framed` is false, or having a frame with no dimensions).

You can add this to your `CREATE TABLE` statement:

```sql
CONSTRAINT check_frame_logic CHECK (
    (framed = FALSE AND dim_frame_x IS NULL AND dim_frame_y IS NULL AND dim_frame_z IS NULL)
    OR
    (framed = TRUE AND dim_frame_x IS NOT NULL AND dim_frame_y IS NOT NULL)
    -- dim_frame_z can stay nullable even if framed
)
```

**Why this is better than a separate table:**

* **Performance:** You get the data in one fetch (no JOIN needed).
* **Safety:** The database literally won't let you save a "broken" artwork record.
* **Atomicity:** You don't have to worry about a transaction failing halfway between the `artworks` table and a `frames`
  table.

---

### 2. The "Spring Boot Way" (`@Embeddable`)

If you want your **Java code** to be cleaner (and follow the "illegal states" principle in your objects), you can use
JPA's `@Embeddable`.

This allows you to have a `Frame` object in Java, but keeps the database table **flat**.

**The Java Class:**

```java

@Embeddable
@Getter
@Setter
public class Frame {
    private boolean framed;
    private Double dimFrameX;
    private Double dimFrameY;
    private Double dimFrameZ;
}
```

**The Entity:**

```java
public class Artwork {
    // ... other fields ...

    @Embedded
    private Frame frame;
}
```

In this way you can make your Constructors protected/private

```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
```

and force the creation of Frames to be done by controlled factories:

```java
public static Frame framed(double dimX, double dimY, Double dimZ) {
    if (dimX <= 0 || dimY <= 0 || (dimZ != null && dimZ <= 0)) {
        throw new IllegalArgumentException("Frame dimensions must be positive");
    }
    return new Frame(true, dimX, dimY, dimZ);
}

public static Frame unframed() {
    return new Frame(false, null, null, null);
}
```

## 22.07: More on making illegal states irrepresentable and Composite keys

### Make Java object instances valid

The same as I did with Frame I should do it with the dimensions of the artwork, so that I can't create an artwork
with negative dimensions, even though I already had a CHECK constraint at db layer. There's a real window between
construction and the eventual save where an Artwork with a negative dimension is a perfectly valid Java object that any
code can read, pass around, or compute with. The bug only surfaces if and when something happens to persist it, which
might be much
later, in a different method, possibly never (if this Artwork is only ever used in memory for some calculation and never
saved).

That's why I created an Dimensions class which I also embed in Artwork and has a factory that validates the integrity of
the dimensions:

```java
public class Dimensions {

    @Column(name = "dim_x", columnDefinition = "NUMERIC(8,2) CHECK (dim_x > 0)", nullable = false)
    private double x;

    @Column(name = "dim_y", columnDefinition = "NUMERIC(8,2) CHECK (dim_y > 0)", nullable = false)
    private double y;

    @Column(name = "dim_z", columnDefinition = "NUMERIC(8,2) CHECK (dim_z > 0)")
    private Double z;

    public static Dimensions of(double x, double y, Double z) {
        if (x <= 0 || y <= 0 || (z != null && z <= 0)) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        return new Dimensions(x, y, z);
    }
}
```

### The primary key class [for a composite key] must be serializable.

By adding implements Serializable, you are just fulfilling a technical requirement of the JPA specification. It
ensures that Hibernate can safely move your composite keys around in memory, caches, or across networks without breaking
the
object. Rule of thumb: Every time you create an @Embeddable class to be used as an @EmbeddedId, always add implements
Serializable.

## 24.07 How to handle exceptions of the API

```java

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
    }
}
```

```java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

Exception propagation mechanics: nothing "catches" an exception in your controller/service, it's ordinary Java stack
unwinding (only possible because every exception  
here is unchecked) all the way out of your code, and only then does Spring's DispatcherServlet catch it and dispatch to
a matching @ExceptionHandler.

## 27.07 DTOs and validation

### What is a DTO

A DTO (Data Transfer Object) is a plain class that defines exactly what an HTTP request or response body must
contain, separate from the `@Entity` class. The
A DTO is needed when an entity has a sensitive field, a bidirectional
relationship that risks Jackson recursion, needs more than one response shape, or is a join-table
entity (in which case I reuse the *other side's* DTO instead of inventing a new one).

For example in the case `users` case:

```java
public class UserRegisterRequest {

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private final String name;

    @Email(message = ErrorMessages.EMAIL_INVALID)
    private final String email;

    @NotBlank(message = ErrorMessages.PASSWORD_REQUIRED)
    private String password;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;
}
```

```java
public class UserUpdateRequest {

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;

    @URL(message = ErrorMessages.BANNER_URL_INVALID)
    private String bannerUrl;

    @URL(message = ErrorMessages.AVATAR_URL_INVALID)
    private String avatarUrl;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;

    private String about;
}
```

```java
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String bannerUrl;
    private String avatarUrl;
    private String city;
    private String postcode;
    private String about;
    private OffsetDateTime createdAt;
}
```

`UserResponse` deliberately has no `passwordHash` field. That's the whole reason it exists
instead of just returning `User` directly.

**DTOs I chose to skip**: `Category`, `Media`, `Support`. These are flat, nothing sensitive, and
nothing else has a back-reference to them (no recursion risk), so the controller just returns the
entity directly.

### Adding spring-boot-starter-validation

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### @Valid vs JPA automatic validation

On the DTO, validation only runs because the controller says so:

```java

@PostMapping("/user/register")
UserResponse register(@Valid @RequestBody UserRegisterRequest userRequest) {
    return userService.register(userRequest);
}
```

Without `@Valid` here, `@NotBlank`/`@Email` on `UserRegisterRequest` would have no effect.

On the `@Entity`, the exact same kind of annotations get checked automatically, with **no**
`@Valid` anywhere:

```java

@Entity
@Table(name = "users")
public class User {
    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;
    // ...
}
```

This works because Bean Validation integration is part of the JPA spec itself. Hibernate checks
for a Bean Validation provider (Hibernate Validator, which the dependency above brings in) and,
if present, validates the entity automatically right before every insert/update.

### Error handling for validation

There are two different Errors that will be thrown after validation. `MethodArgumentNotValidException.class`
is thrown by Spring MVC itself. It carries a BindingResult with one FieldError per failed constraint
(field name + the message from the annotation, e.g. @NotBlank(message = "...")).

```java

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleSpringMvcValidation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult().getFieldErrors()
     .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
}
```

`jakarta.validation.ConstraintViolationException.class` is the Entity validation failure (thrown by Hibernate/JPA)

```java 

@ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
public ResponseEntity<String> handleEntityValidation(jakarta.validation.ConstraintViolationException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
}
```

## 28.07 @Transactional annotation

There are these two ways of doing update:

### fetching, setting and saving:

```java
public UserResponse update(Long id, UserUpdateRequest
        request) {
    var user = repository.findById(
                                 id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));

    user.setCity(request.getCity());
    user.setPostcode(request.getPostcode());
    user.setBannerUrl(request.getBannerUrl());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setAbout(request.getAbout());

    var savedUser = repository.save(user);
    return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
            savedUser.getBannerUrl(), savedUser.getAvatarUrl(), savedUser.getCity(),
            savedUser.getPostcode(), savedUser.getAbout(), savedUser.getCreatedAt());

}
```

### using @Transactional, no explicit save():

```java

@Transactional
public UserResponse update(Long id, UserUpdateRequest request) {
    var user = repository.findById(id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));

    user.setName(request.getName());
    user.setCity(request.getCity());
    user.setPostcode(request.getPostcode());
    user.setAvatarUrl(request.getAvatarUrl());
    user.setBannerUrl(request.getBannerUrl());
    user.setAbout(request.getAbout());

    return new UserResponse(user.getId(), user.getName(), user.getEmail(),
            user.getBannerUrl(), user.getAvatarUrl(), user.getCity(),
            user.getPostcode(), user.getAbout(), user.getCreatedAt());
}
```

Without @Transactional on the service method, each individual
repository call (findById) runs in its own short-lived transaction managed internally by Spring Data. By the time it
returns, that transaction (and the Hibernate session backing it) has already closed, so the User object you
get back is detached: mutating its fields afterward does nothing until you
explicitly save() it again.

With @Transactional on the whole method, one single transaction spans the entire method body, findById returns a
managed entity that Hibernate is actively tracking for the duration. Any field you mutate on it is             
automatically detected ("dirty checking"), and Hibernate issues the UPDATE on its own when the transaction commits at
the end of the method. No save() call needed at all.

## 29.07 Testing with Mockito

### Controller test vs service test

Controller test (`UserControllerTest`) verifies
the whole HTTP contract, not the service's internal logic:

```java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class UserControllerTest {

    @MockitoBean
    UserService service;

    @Autowired
    RestTestClient client;
```

Service tests have no Spring context at all, just Mockito. Verifies the service's
own logic in isolation, with no DB and no HTTP involved:

```java

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    UserService service;

    @Mock
    UserRepository repository;

    @BeforeEach
    void setup() {
        service = new UserService(repository);
    }
```

### `when(...)` — stub what a mock returns

```java
when(repository.findByEmail("bob@ross.com")).

thenReturn(Optional.empty());
```

Only matches calls whose arguments are `.equals()` to what was stubbed. This is why request DTOs
without `@EqualsAndHashCode` can break a `when(mock.method(exactObject))` stub if the
real call uses a different (but equal-looking) instance. Using `any()`/`eq()` fixes it. any() tells Mockito "match this
argument no matter what its value is, don't even try .equals().

### `verify(...)` — assert a mock was actually called

For void methods, there's no return value to assert on, so `verify` is the only way to confirm
something happened:

```java
service.delete(1L);

verify(repository).

delete(user);
```

`never()` proves the opposite: that something did *not* happen.

```java
assertThrows(NotFoundException .class, () ->service.

delete(1L));

verify(repository, never()).

delete(any());
```

### `ArgumentCaptor` — inspect what a mock was actually called with

`when`/`verify` only let you control or confirm that a call happened, `ArgumentCaptor` lets you
grab the real object a mock was called with. I needed this to verify password hashing, since the
returned `UserResponse` doesn't expose `passwordHash`. I had to capture the actual `User` passed to
`save(...)`:

```java
void registerSuccessfully() {

    var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123", "Vienna", "1020");
    when(repository.findByEmail("bob@ross.com")).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> {
        User savedUser = invocation.getArgument(0);
        savedUser.setId(1L);
        savedUser.setCreatedAt(createdAt);
        return savedUser;
    });

    var result = service.register(request);

    var userCaptor = ArgumentCaptor.forClass(User.class);
    verify(repository).save(userCaptor.capture());
    var savedUser = userCaptor.getValue();

    assertNotEquals("password123", savedUser.getPasswordHash());
    assertTrue(encoder.matches("password123", savedUser.getPasswordHash()));

    var expectedUserResponse = new UserResponse(1L, "Bob Ross", "bob@ross.com", null, null, "Vienna", "1020", null, createdAt);
    assertEquals(expectedUserResponse, result);
}
```

## 05.08 Relationships: owning side, and lazy loading

### 1. Only one side of a relationship is "real" to Hibernate

I have this in `Artwork`:

```java

@OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("sortOrder ASC")
private List<ArtworkImage> images = new ArrayList<>();
```

and this in `ArtworkImage`:

```java

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "artwork_id", nullable = false)
private Artwork artwork;
```

I thought since I can navigate both ways in Java (`artwork.getImages()` and
`image.getArtwork()`), both sides were "equal". They're not. In the actual database there is only
**one** column that stores this relationship: `artwork_id`, sitting on the `artwork_images` table.
`artworks` has no column pointing back at all.

So Hibernate needs to know which of my two Java fields it should actually trust when saving. That's
exactly what `@JoinColumn` vs `mappedBy` mean:

- `@JoinColumn` on `ArtworkImage.artwork` → this is the **owning side**. Hibernate writes to
  `artwork_id` based on *this* field.
- `mappedBy = "artwork"` on `Artwork.images` → the **inverse side**. It's just a convenience for
  reading (`SELECT * FROM artwork_images WHERE artwork_id = ?` under the hood), Hibernate ignores it
  when saving.

Which means if I ever write code like this:

```java
artwork.getImages().add(newImage);
```

it does nothing to persist the relationship, because I never touched `newImage.getArtwork()`.
Hibernate would try to insert the image with `artwork_id = NULL` and the DB would reject it
(`nullable = false`).

The fix is to always keep both sides in sync, and the standard way to not forget is a small helper
method on the parent entity:

```java
public void addImage(ArtworkImage image) {
    images.add(image);
    image.setArtwork(this);
}
```

### 2. Lazy collections need an open DB session to be read

`@OneToMany` collections are lazy by default (unlike `@ManyToOne`, which is eager by default). 
Hibernate doesn't put a real list in `artwork.images`,
it puts a placeholder that says "run a query the first time someone actually touches me". For that
trick to work, the database session (Hibernate calls it the "persistence context") has to still be
open at that exact moment. If it's already closed, you get a `LazyInitializationException`.

My `getDetailById`/`getAllCards` methods call `.getImages()` (indirectly, through
`ArtworkDetailResponse.from(...)`/`ArtworkCardResponse.from(...)`) without being `@Transactional`
at all, so by that logic they should already be broken. They're not, because Spring Boot has
`spring.jpa.open-in-view=true` **on by default**, which keeps the session open for the whole HTTP
request instead of closing it right after the method returns. So it "just works", invisibly,
because of a setting I never touched or even knew existed.

If `open-in-view` ever gets turned off, all my reads that touch lazy
collections would suddenly break, for a reason that would be very hard to trace back to one config
line. So the "honest" fix is to say directly, on the method itself, that it needs a session:

```java

@Transactional(readOnly = true)
public Optional<ArtworkDetailResponse> getDetailById(Long id) {
    return repository.findByIdAndDeletedAtIsNull(id)
                     .map(ArtworkDetailResponse::from);
}
```

I already had `@Transactional` imported in `ArtworkService`, but
from `jakarta.transaction`, not Spring's. `readOnly` doesn't exist on that one at all — it's a
Spring-only attribute. Had to switch the import to
`org.springframework.transaction.annotation.Transactional` for `readOnly = true` to even compile.

