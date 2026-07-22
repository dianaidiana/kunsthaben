# Diary of my project (or what I've learnt today):

## 21.07: Normalization overkill & making illegal states irrepresentable

while planning my schema, I created this table:

```sql 
CREATE TABLE IF NOT EXISTS artworks
(
    id
    SERIAL
    PRIMARY
    KEY,
    artist_id
    INTEGER
    NOT
    NULL
    REFERENCES
    users
(
    id
) ON DELETE CASCADE,
    title TEXT NOT NULL,
    price NUMERIC
(
    12,
    2
) NOT NULL,
    year INTEGER NOT NULL,
    description TEXT NOT NULL,
    city TEXT NOT NULL,
    postcode TEXT NOT NULL,
    dim_x NUMERIC
(
    8,
    2
) NOT NULL,
    dim_y NUMERIC
(
    8,
    2
) NOT NULL,
    dim_z NUMERIC
(
    8,
    2
),
    framed BOOLEAN NOT NULL,
    dim_frame_x NUMERIC
(
    8,
    2
),
    dim_frame_y NUMERIC
(
    8,
    2
),
    dim_frame_z NUMERIC
(
    8,
    2
), -- is this violating the make illegal states irrepresentable rule?
    category_id INTEGER REFERENCES categories
(
    id
)
  ON DELETE SET NULL,
    medium_id INTEGER REFERENCES media
(
    id
)
  ON DELETE SET NULL,
    support_id INTEGER REFERENCES supports
(
    id
)
  ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    sold_at TIMESTAMPTZ,
    reserved BOOLEAN DEFAULT false
    );
```

but I came up with the question: is this violating the make illegal states irrepresentable rule?

```sql 
framed
BOOLEAN        NOT NULL,
dim_frame_x NUMERIC(8, 2),
dim_frame_y NUMERIC(8, 2),
dim_frame_z NUMERIC(8, 2), 
```

what happens if the artwork is framed but no dimensions are given? or if it is unframed but there are frames dimensions
given?
Should I instead create a separate table for framed dimensions and provide an nullable foreign key to the artworks
table?

The short answer is: **Keep them in the same table, but use `CHECK` constraints.**

In database design, moving a few optional columns to a separate table just to avoid `NULLs` is often considered **"
Normalization Overkill."** It makes your queries slower (requires more joins) and your Java code more complex without
providing much benefit.

However, your concern about **"making illegal states irrepresentable"** is a very valid and high-quality engineering
thought.

Here is how you handle it effectively:

### 1. The "PostgreSQL Way" (Constraints)

Instead of a separate table, use a `CHECK` constraint. This forces the database to reject "illegal states" (like having
frame dimensions when `framed` is false, or having a frame with no dimensions).

You can add this to your `CREATE TABLE` statement:

```sql
CONSTRAINT check_frame_logic CHECK (
    (framed = FALSE AND dim_frame_x IS NULL AND dim_frame_y IS NULL AND dim_frame_z IS NULL)
    OR
    (framed = TRUE AND dim_frame_x IS NOT NULL AND dim_frame_y IS NOT NULL)
    -- dim_frame_z can stay nullable even if framed, as some frames are very thin
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

This way, in your Java logic, you can do things like `if (artwork.getFrame().isFramed())`, keeping your code organized
while keeping the database efficient.

---

### When *should* you use a separate table?

You should only move these to a separate table if:

1. **Multiple Frames:** If an artwork could have many different interchangeable frames.
2. **Reusable Frames:** If you have a stock of physical frames that you assign to different artworks over time (
   Inventory management).
3. **Frame Metadata:** If you need to store 10+ details about the frame (Material, Color, Manufacturer, Weight, Glass
   type, etc.).

### Summary Recommendation:

Since you are likely just recording dimensions for shipping or display:

1. **Keep them in the `artworks` table.**
2. **Add the `CHECK` constraint** to the SQL.
3. **Use `@Embeddable`** in your Spring Boot app.

This gives you the "Clean Code" of a separate table with the "Performance" and "Safety" of a single table.

## 22.07: questions to ask:

should dimensions be enclosed in their own class?
should artwork also have a factory with validation (eg the dimensions should be positive in object layer too).
all validations should be done in java layer AND dba layer?
what is the N+1 problem??
"The primary key class [for a composite key] must be serializable."
By adding implements Serializable, you are just fulfilling a technical requirement of the JPA specification. It ensures
that Hibernate can safely move your composite keys around in memory, caches, or across networks without breaking the
object.
Rule of thumb: Every time you create an @Embeddable class to be used as an @EmbeddedId, always add implements
Serializable.