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