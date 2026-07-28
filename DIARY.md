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

## 22.07: Validation and composite keys

### Validate in DB AND in Java level

The same as happened with Frame I should do it with the dimensions of the artwork, so that I can't create an artwork
with negative dimensions, even though I already had a CHECK constraint at db layer.

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

The general rule is: any group of fields guarded by a CHECK constraint that expresses a positivity/consistency invariant
is a candidate for a small      
validating @Embeddable value object. Where it doesn't apply: things like Media/Support codes or Chat's buyer_id <>
seller_id check, those are either simple single-column constraints already fully enforced by the DB, or      
relational checks across separate entities that don't naturally collapse into one embeddable value object. Don't reach
for this pattern everywhere — it earns its keep specifically when a CHECK constraint is describing "these
2–4 fields together form one coherent, occasionally-invalid concept," which is true of both Frame and Dimensions, but
wouldn't be true of, say, title/price (each independently valid, no cross-field relationship).

### The primary key class [for a composite key] must be serializable.

By adding implements Serializable, you are just fulfilling a technical requirement of the JPA specification. It
ensures
that Hibernate can safely move your composite keys around in memory, caches, or across networks without breaking the
object.
Rule of thumb: Every time you create an @Embeddable class to be used as an @EmbeddedId, always add implements
Serializable.

## 24.07 how to handle exceptions in the controller

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

[//]: # (### what is the N+1 problem??)
Option A — fetch, mutate, explicit
save():

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

Option B — @Transactional, no explicit save() at all:

```java

@Transactional
public UserResponse update(Long id, UserUpdateRequest request) {
    var user = repository.findById(id)
                         .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));

    user.setCity(request.getCity());
    user.setPostcode(request.getPostcode());
    // ... same setters, no repository.save(user) call needed                                                                                                                                                                    

    return new UserResponse(/* ... */);
}
```

Why option B works without calling save(): this is exactly what @Transactional is for, and it's genuinely worth
understanding rather than treating as magic. Without @Transactional on the service method, each individual       
repository call (findById) runs in its own short-lived transaction managed internally by Spring Data — by the time it
returns, that transaction (and the Hibernate session backing it) has already closed, so the User object you
get back is detached: nothing is watching it anymore, and mutating its fields afterward does nothing until you
explicitly save() it again (that's Option A).

With @Transactional on the whole method, one single transaction spans the entire method body — findById returns a
managed entity that Hibernate is actively tracking for the duration. Any field you mutate on it is             
automatically detected ("dirty checking"), and Hibernate issues the UPDATE on its own when the transaction commits at
the end of the method — no save() call needed at all.
