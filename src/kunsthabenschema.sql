CREATE TABLE IF NOT EXISTS users
(
    id            SERIAL PRIMARY KEY,
    name          TEXT NOT NULL,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    banner_url    TEXT,
    avatar_url    TEXT,
    city          TEXT NOT NULL,
    postcode      TEXT NOT NULL,
    about         TEXT,
    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_favorite_artists
(
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    artist_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, artist_id),
    CHECK (user_id <> artist_id)
);

CREATE TABLE IF NOT EXISTS categories
(
    id   SERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    code TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS media
(
    id          SERIAL PRIMARY KEY,
    name        TEXT    NOT NULL,
    category_id INTEGER NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    code        TEXT    NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS supports
(
    id          SERIAL PRIMARY KEY,
    name        TEXT    NOT NULL,
    category_id INTEGER NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    code        TEXT    NOT NULL UNIQUE
);

--  THIS MIGHT CHANGE IN THE FUTURE:
--  heriarchical relationship vs many-to-many approach:
--  remove category_id from media and supports
--  then we would need the following two tables to make the relation
--  this would avoid repetition in media and supports (e.g. paper is used for painting and drawing, etc...)
-- CREATE TABLE category_media
-- (
--     category_id INTEGER REFERENCES categories (id) ON DELETE CASCADE,
--     media_id    INTEGER REFERENCES media (id) ON DELETE CASCADE,
--     PRIMARY KEY (category_id, media_id)
-- );
--
-- CREATE TABLE category_supports
-- (
--     category_id INTEGER REFERENCES categories (id) ON DELETE CASCADE,
--     support_id  INTEGER REFERENCES supports (id) ON DELETE CASCADE,
--     PRIMARY KEY (category_id, support_id)
-- );

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
    reserved    BOOLEAN     DEFAULT false,
    CONSTRAINT check_framed CHECK (
        (framed = FALSE AND dim_frame_x IS NULL AND dim_frame_y IS NULL AND dim_frame_z IS NULL)
            OR
        (framed = TRUE AND dim_frame_x IS NOT NULL AND dim_frame_y IS NOT NULL)
        -- dim_frame_z can be null even if framed
        )
);

CREATE TABLE IF NOT EXISTS artwork_images
(
    id         SERIAL PRIMARY KEY,
    artwork_id INTEGER NOT NULL REFERENCES artworks (id) ON DELETE CASCADE,
    url        TEXT    NOT NULL,
    sort_order INTEGER NOT NULL,
    UNIQUE (artwork_id, sort_order)
);

CREATE TABLE IF NOT EXISTS user_favorite_artworks
(
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    artwork_id INTEGER NOT NULL REFERENCES artworks (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, artwork_id)
);

CREATE TABLE IF NOT EXISTS chats
(
    id         SERIAL PRIMARY KEY,
    artwork_id INTEGER NOT NULL REFERENCES artworks (id) ON DELETE CASCADE,
    buyer_id   INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    seller_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    active     BOOLEAN     DEFAULT true,
    CHECK (buyer_id <> seller_id),
    UNIQUE (artwork_id, buyer_id)
);

CREATE TABLE IF NOT EXISTS messages
(
    id         SERIAL PRIMARY KEY,
    chat_id    INTEGER NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    sender_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content    TEXT    NOT NULL,
    read       BOOLEAN     DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);


-- SOME INSERTIONS:
-- CATEGORIES
INSERT INTO categories (name, code)
VALUES ('Painting', 'CAT_PAINTING'),
       ('Drawing', 'CAT_DRAWING')
ON CONFLICT (code) DO NOTHING;
--        ('Printmaking', 'CAT_PRINTMAKING'),
--        ('Sculpture', 'CAT_SCULPTURE'),
--        ('Ceramics', 'CAT_CERAMICS'),
--        ('Textile Art', 'CAT_TEXTILE_ART'),
--        ('Collage', 'CAT_COLLAGE');

-- MEDIA FOR CAT_PAINTING
WITH cat AS (SELECT id FROM categories WHERE code = 'CAT_PAINTING')
INSERT
INTO media (name, category_id, code)
SELECT val.name, cat.id, val.code
FROM cat,
     (VALUES ('Oil', 'MED_PAINT_OIL'),
             ('Acrylic', 'MED_PAINT_ACRYLIC'),
             ('Watercolor', 'MED_PAINT_WATERCOLOR'),
             ('Gouache', 'MED_PAINT_GOUACHE'),
             ('Mixed media', 'MED_PAINT_MIXED_MEDIA')) AS val(name, code)
ON CONFLICT (code) DO NOTHING;

-- MEDIA FOR CAT_DRAWING
WITH cat AS (SELECT id FROM categories WHERE code = 'CAT_DRAWING')
INSERT
INTO media (name, category_id, code)
SELECT val.name, cat.id, val.code
FROM cat,
     (VALUES ('Charcoal', 'MED_DRAW_CHARCOAL'),
             ('Graphite', 'MED_DRAW_GRAPHITE'),
             ('Ink', 'MED_DRAW_INK'),
             ('Pastel', 'MED_DRAW_PASTEL'),
             ('Oil pastel', 'MED_DRAW_OIL_PASTEL')) AS val(name, code)
ON CONFLICT (code) DO NOTHING;

-- SUPPORTS FOR CAT_PAINTING
WITH cat AS (SELECT id FROM categories WHERE code = 'CAT_PAINTING')
INSERT
INTO supports (name, category_id, code)
SELECT val.name, cat.id, val.code
FROM cat,
     (VALUES ('Canvas', 'SUP_PAINT_CANVAS'),
             ('Wood Panel', 'SUP_PAINT_WOOD'),
             ('Linen', 'SUP_PAINT_LINEN'),
             ('Paper', 'SUP_PAINT_PAPER')) AS val(name, code)
ON CONFLICT (code) DO NOTHING;

-- SUPPORTS FOR CAT_DRAWING
WITH cat AS (SELECT id FROM categories WHERE code = 'CAT_DRAWING')
INSERT
INTO supports (name, category_id, code)
SELECT val.name, cat.id, val.code
FROM cat,
     (VALUES ('Paper', 'SUP_DRAW_PAPER'),
             ('Cardboard', 'SUP_DRAW_CARDBOARD'),
             ('Vellum', 'SUP_DRAW_VELLUM')) AS val(name, code)
ON CONFLICT (code) DO NOTHING;

