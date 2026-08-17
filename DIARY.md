# Diary of my project (or what I've learnt today):

### What a unit test can and can't prove about `@Transactional`

Wanted my test to prove that if the S3 delete fails, the database delete doesn't persist either
(rollback). Learned that a plain Mockito unit test literally cannot show this: my test builds the
service with `new ArtworkImageService(repository, artworkService, s3StorageService)` directly,
never through Spring, so `@Transactional`'s proxy is never involved at all. So there's no real
transaction, `repository` is just a mock recording which methods were called on
it.

So `verify(repository).delete(artworkImage2)` after a simulated S3 failure only proves that the
code *called* delete before the S3 call blew up, i.e. that the order of operations is right
(which is exactly what makes the real rollback safe once this runs for real, inside Spring). It
doesn't prove anything actually got undone. To really test "nothing gets persisted when S3 fails"
I'd need a proper `@SpringBootTest` integration test with a real database, letting `@Transactional`
actually do its job.

## 12.08 Wiring S3 into the app

### `S3StorageService`

Built a small class whose only job is talking to S3. `uploadFile(MultipartFile)` builds the object
key from a random `UUID` plus the original file extension — not the original filename — because
two different artists both uploading `image.jpg` would otherwise silently overwrite each other's
file:

```java
public String uploadFile(MultipartFile file) {
    var key = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
    s3Client.putObject(
            PutObjectRequest.builder().bucket(bucketName).key(key)
                            .contentType(file.getContentType()).build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
    );
    return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucketName, region, key);
}
```

### Rewiring `ArtworkImageService`

Before this, `addImage` just trusted a `url` string sent by the client. Now the endpoint receives
the actual file (`multipart/form-data`), and the flow is: check the artist owns the artwork, check
the image cap, validate the file, upload it through `S3StorageService`, save the URL it hands
back. Also had to build `ArtworkImageController` itself — it didn't exist before, only the
service/repository layer did.

### Bug: `NoSuchBucketException`

First real upload attempt threw `NoSuchBucketException: The specified bucket does not exist`. The
request was reaching AWS fine (so credentials were working), it just couldn't find a bucket by the
name in my `application.properties`. Turned out the name I originally picked was already taken —
S3 bucket names are unique **globally**, across every AWS account in existence, not just mine — so
when I created the bucket, AWS silently suggested a longer, actually-unique name
(`kunsthaben-artwork-images-<account-id>-<region>-<suffix>`), and that's the one that actually got
created. Just had to update the property to match reality.

### Region question

Asked whether it mattered that I picked `eu-north-1` (Stockholm) while being in Austria. Short
answer: not really, at this scale — maybe 10-20ms extra latency versus Frankfurt, imperceptible
for a school project, and if anything Stockholm tends to be slightly *cheaper* than
`eu-central-1`. Region choice only really matters if my own server (the compute) is *also* running
inside AWS in a specific region — then keeping compute and storage in the same region avoids
cross-region latency/cost. Since my app just runs locally and calls out to S3 over the internet
regardless of where I deploy it, that doesn't apply here.

### "Will I need a new bucket if I deploy this online?"

No — this was a genuine misconception I had. A bucket is reachable over HTTPS from literally
anywhere; region only describes where the bytes physically live, not who's allowed to reach them.
My app can run on my laptop, a VPS, wherever, and keep using the exact same bucket the whole time.
The only case where region-matching is worth optimizing for is deploying the compute itself inside
AWS in a specific region — a deliberate choice, not something required just because the app went
"online."

## 11.08 S3 storage setup: IAM, buckets, credentials and the AWS SDK

### What S3 actually is

S3 (Simple Storage Service) is AWS's object storage. A place to store files
("objects") and get a URL back. A **bucket** is a namespace/container for objects; it's not a real
folder system even though the console displays it that way.

### The AWS Free Plan

New AWS accounts get a **Free Plan**: $100 in credit, usable for 185 days (6 months). The
important part: this is a hard cap, not classic pay-as-you-go. If I ever went over the credit
within that window, AWS doesn't charge me, it just shuts the account down (data kept for 90 days,
so I'd have a chance to upgrade and recover it). That's genuinely different from a normal AWS
account with a card attached and no ceiling, and it's the reason I felt comfortable going ahead
with a real AWS account for a school project instead of something else.

### IAM and policies

**IAM** (Identity and Access Management) is AWS's system for controlling *who* can do *what* on
*which* resources. Instead of using my main/root account credentials in the app (full
account access, no boundaries), I created a separate **IAM user** just for this project.

A **policy** is a JSON document you attach to that user, listing exactly which actions are
allowed, on exactly which resources. Mine only grants `s3:PutObject`/`s3:DeleteObject`, scoped to
this one bucket's ARN:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::YOUR-BUCKET-NAME/*"
    }
  ]
}
```

This is the "least privilege" idea: if these specific credentials ever leaked, the damage is
capped at "someone can write/delete objects in this one bucket," not "someone owns my AWS
account."

### Creating the bucket

Picked a region (`eu-north-1`, Stockholm) and a name. Left "Block all public access" on at the
bucket level, and instead added a narrow bucket policy allowing public *read-only* access
(`s3:GetObject` only) — so uploaded images can be viewed via a plain URL in a browser, without
opening up writing/listing/deleting to the whole internet.

### Saving credentials without putting them in the repo

Instead of pasting the access key/secret into `application.properties` (where they could easily
end up committed to git by accident), I saved them as a **named profile** in `~/.aws/credentials`:

```
[kunsthaben]
aws_access_key_id = ...
aws_secret_access_key = ...
```

and pointed the Java code at that profile by name, so the actual secret values never touch the
repo at all:

```java
S3Client.builder().

region(Region.of(region)).

credentialsProvider(ProfileCredentialsProvider.create("kunsthaben")).

build();
```

### pom.xml dependencies

Needed the AWS SDK for Java v2. Added their BOM first (Bill of Materials, a shared version list,
so I don't have to manually pick a compatible version for every single AWS artifact), then the
actual `s3` artifact with no version needed since the BOM supplies it:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.29.10</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### What `S3Client` is

`S3Client` is the actual object my code uses to talk to S3 — every `putObject`/`deleteObject` call
goes through it. Built it once as a Spring `@Bean` in a small `S3Config` class (region + the
profile credentials provider above), so the whole app reuses one client instead of creating a new
one per request.

## 07.08 Concurrency: why `@Transactional` doesn't prevent race conditions

`ArtworkImageService.addImage`'s cap check (`MAX_IMAGES_PER_ARTWORK`) is a classic
check-then-act race: two concurrent requests for the same artwork can each read
`count = 9`, both pass the check before either commits, and both insert — ending up
with 11 images even though the limit is 10.
Although `@Transactional` gives atomicity (all your own writes commit together or not at all) and isolation from
uncommitted changes of other transactions, it does not make
concurrent calls to the same method run one at a time. Two transactions can each read
"count = 9" at the same instant, because neither has committed yet when the other reads.

### Pessimistic write

Pessimistic locking on the `Artwork` row before the count check, e.g. a repository
method annotated `@Lock(LockModeType.PESSIMISTIC_WRITE)`, would force a second
concurrent `addImage` call for the *same* artwork to block until the first transaction
commits (other artworks are unaffected, since the lock is per-row).
I'm skipping this fix for later.

## 06.08 Pagination and Specification

### Pagination

I had `getAllCards()` returning a plain `List<ArtworkCardResponse>`, which means every call was
fetching and returning *everything* that matched, no matter how many rows. That's fine for a demo
with 3 artworks, but the homepage grows with the whole platform's artwork count forever, so I
needed a way to ask for just one "page" of results at a time.

Spring Data already has this built in:

- `Pageable` describes *which* chunk I want (page number, page size, sorting). I build one with
  `PageRequest.of(page, size, sort)`.
- `Page<T>` is what comes back: the items, and also totals (`totalElements`,
  `totalPages`) and whether there's a next page.

Just by adding a `Pageable` parameter to my own repository methods, Spring Data handles the
`LIMIT`/`OFFSET` translation to SQL for me:

```java
Page<Artwork> findAllByDeletedAtIsNullAndSold(boolean sold, Pageable pageable);
```

Worth mentioning: returning `Page<T>` costs **two queries**:
`SELECT ... LIMIT ... OFFSET ...` for the content, plus a separate `SELECT COUNT(*)` to compute the totals. If I don't
need the totals, `Slice<T>` is a cheaper
alternative that skips the count query entirely.

### @PageableDefault

Without `@PageableDefault`, a plain `GET /artwork` with no query params would
come back completely unsorted (Spring Data's own default), so I annotated the controller
parameter to keep newest-first as the default behavior once I removed the hardcoded
`OrderByCreatedAtDesc` from the repository method name:

```java

@GetMapping("/artwork")
Page<ArtworkCardResponse> getAllCards(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return service.getAllCards(pageable);
}
```

### PageImpl

`Page<T>` is just an **interface** — it only declares what a page can *do*
(`getContent()`, `getTotalElements()`, `getTotalPages()`, `hasNext()`, ...). `PageImpl<T>` is Spring Data's own concrete
implementation of that interface.
When I mock the repository
(`when(repository.findAllByDeletedAtIsNullAndSold(...)).thenReturn(...)`), Mockito needs a real
object to hand back, not just an interface.

It has two constructors worth knowing:

- `new PageImpl<>(List.of(...))`: treats the given list as the
  whole result (one page). Good enough since my tests only
  care about verifying the mapped content came through correctly, not exercising real pagination
  math.
- `new PageImpl<>(content, pageable, total)`: the fuller form, letting me simulate "this is page
  2 of 5, out of 47 results total," if I ever need to test pagination boundaries specifically
  (`hasNext()`, `getTotalPages()`), which I'm not doing at the moment.

### Fixing this warning:

```
For a stable JSON structure, please use Spring Data's PagedModel (globally via @EnableSpringDataWebSupport(
pageSerializationMode = VIA_DTO)) or Spring HATEOAS and Spring Data's PagedResourcesAssembler as documented
in https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.pageables.
```

As per this blogpost (Coding Steve): https://stevenpg.com/posts/spring-data-page-impl-serialization-warning/

This warning appears when you’re returning Page<T> objects directly from your REST controllers, and Spring is warning
you that the JSON structure might change between versions.

and the fix:

```java

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
}
```

This configuration tells Spring to serialize Page objects using a stable DTO structure instead of the internal PageImpl
structure.

With this configuration, your existing controller methods work exactly the same, but the JSON output will use Spring’s
stable PagedModel format:

```JSON
{
  "content": [
    ...
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### I have no clue:

how to test the content of a endpoint that returns a Page object. For now, I'll just test
the status as the response of the content is actually tested at service.

### Specification

The other big thing today was `Specification<Artwork>` for the filters page. The core idea: one
small method per filter criterion, each returning either a real condition or `null` when that
filter wasn't provided:

```java
public static Specification<Artwork> hasMinPrice(Double minPrice) {
    if (minPrice == null) return Specification.unrestricted();
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
}
```

**Correction (10.08):** I originally wrote `return null;` here, believing
`Specification.where(x).and(y)` treated a `null` Specification as "skip this one". That was true
in older Spring Data versions, but this project is on Spring Data JPA 4.1.0, where
`Specification.and(other)` now does `Assert.notNull(other, "Other specification must not be
null")` — passing `null` throws instead of being skipped. Any filter request leaving a field unset
(so its `hasX` method returned `null`) blew up with `Other specification must not be null`. The
fix is the new `Specification.unrestricted()` static factory (added in 4.0), a no-op specification
meant exactly for this: it always returns a `null` *predicate*, which composition does still treat
as "doesn't contribute" — the null-tolerance moved from the `Specification` reference itself to
the `Predicate` it produces.

`Specification.where(x).and(y)` is what lets me chain a dozen optional filters together and only
the ones actually provided end up in the final `WHERE` clause:

```
Specification.where(isNotDeleted())
        .and(isNotSold())
        .and(hasMinPrice(filter.getMinPrice()))
        .and(hasCategoryIn(filter.getCategoryIds()))
// ...
```

### Case-insensitive search without `ILIKE`

Postgres's `ILIKE` is for case-insensitive search, but plain JPA's
`CriteriaBuilder` doesn't have an `ilike()` method as `ILIKE` isn't standard SQL, it's
Postgres-only. In this case, lowercasing must be done from both sides:

```java
private static Specification<Artwork> matchesKeyword(String word) {
    var pattern = "%" + word.toLowerCase() + "%";
    return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("title")), pattern),
            cb.like(cb.lower(root.get("description")), pattern)
    );
}
```

### Multiple keyword search

A single `LIKE '%red house%'` requires that exact phrase, in that exact order, to appear as one
substring, so what I decided to do for now is to split on whitespace and require each word independently to
appear in title or description, ANDed together:

```
return Arrays.stream(keywords.trim().split("\\s+"))
        .map(ArtworkSpecifications::matchesKeyword)
         .reduce(Specification::and)
         .orElse(null);
```

This solution is not ideal and I would like to implement a filter using PostgreSQL Text Search Types
`tsvector` and `tsquery`. This is much more involved so I'll leave it for later.

### The service side

```java

@Transactional(readOnly = true)
public Page<ArtworkCardResponse> search(ArtworkFilter filter, Pageable pageable) {
    return repository.findAll(ArtworkSpecifications.build(filter), pageable)
                     .map(ArtworkCardResponse::from);
}
```

### The controller side

```java

@GetMapping("/artwork/search")
Page<ArtworkCardResponse> search(
        @ModelAttribute ArtworkFilter filter,
        @PageableDefault(size = 20, sort = "createdAt", direction =
                Sort.Direction.DESC) Pageable pageable) {
    return service.search(filter, pageable);
}
```

`@ModelAttribute` tells Spring: "take all the matching query parameters from this GET request and populate them onto
this object's fields, one by one."
Concretely, with a request like:

`GET /artwork/search?keywords=house&minPrice=50&categoryIds=1&categoryIds=3&framed=true`

Spring creates a new ArtworkFilter (using its no-arg constructor), then for each query param, it calls the matching
setter:
setKeywords("house"), setMinPrice(50.0), setCategoryIds(List.of(1L, 3L)) (repeated params automatically collect into a
List), setFramed(true).
Any field with no matching query param is just left at its default (null, since every field in ArtworkFilter is a
boxed/nullable type), which is exactly what     
ArtworkSpecifications.build(...) needs: null means "this filter wasn't provided, skip it."

### Testing filters

I'm not testing the search method of the service (the one that filters) as it would involve mocking the db with
@DataJpaTest. I'm postponing this for later.

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
artwork.getImages().

add(newImage);
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

