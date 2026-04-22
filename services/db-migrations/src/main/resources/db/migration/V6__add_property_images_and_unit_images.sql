CREATE TABLE property_images
(
    id          UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    url         TEXT NOT NULL,
    is_main     BOOLEAN,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_property
        FOREIGN KEY (property_id)
            REFERENCES properties (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_property_images_property_id ON property_images (property_id);

CREATE TABLE unit_images
(
    id         UUID PRIMARY KEY,
    unit_id    UUID NOT NULL,
    url        TEXT NOT NULL,
    is_main    BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_unit
        FOREIGN KEY (unit_id)
            REFERENCES units (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_unit_images_unit_id ON unit_images (unit_id);


ALTER TABLE properties DROP COLUMN image_url;


INSERT INTO property_images (id, property_id, url, is_main)
VALUES
-- Images for Sunset Apartments (Warsaw)
('11111111-1111-1111-1111-111111111111',
 'aaaa1111-1111-1111-1111-111111111111',
 'https://images.pexels.com/photos/34360416/pexels-photo-34360416.jpeg',
 TRUE),

('22222222-2222-2222-2222-222222222222',
 'aaaa1111-1111-1111-1111-111111111111',
 'https://images.pexels.com/photos/34360412/pexels-photo-34360412.jpeg',
 FALSE),

('33333333-3333-3333-3333-333333333333',
 'aaaa1111-1111-1111-1111-111111111111',
 'https://images.pexels.com/photos/34360417/pexels-photo-34360417.jpeg',
 FALSE),

-- Images for Mountain View Lodge (Zakopane)
('44444444-4444-4444-4444-444444444444',
 'bbbb2222-2222-2222-2222-222222222222',
 'https://images.pexels.com/photos/6552568/pexels-photo-6552568.jpeg',
 TRUE),

('55555555-5555-5555-5555-555555555555',
 'bbbb2222-2222-2222-2222-222222222222',
 'https://images.pexels.com/photos/11789386/pexels-photo-11789386.jpeg',
 FALSE) ON CONFLICT (id) DO NOTHING;

INSERT INTO unit_images (id, unit_id, url, is_main)
VALUES
-- Studio 1 (Warsaw)
('11111111-1111-1111-1111-111111111111',
 '11111111-1111-1111-1111-222222222222',
 'https://images.pexels.com/photos/19846366/pexels-photo-19846366.jpeg',
 TRUE),

('22222222-2222-2222-2222-222222222222',
 '11111111-1111-1111-1111-222222222222',
 'https://images.pexels.com/photos/19846387/pexels-photo-19846387.jpeg',
 FALSE),

-- Apartment Deluxe (Warsaw)
('33333333-3333-3333-3333-333333333333',
 '22222222-2222-2222-2222-333333333333',
 'https://images.pexels.com/photos/19916704/pexels-photo-19916704.jpeg',
 TRUE),

('44444444-4444-4444-4444-444444444444',
 '22222222-2222-2222-2222-333333333333',
 'https://images.pexels.com/photos/19916700/pexels-photo-19916700.jpeg',
 FALSE),

-- Room 1 (Zakopane)
('55555555-5555-5555-5555-555555555555',
 '33333333-3333-3333-3333-444444444444',
 'https://images.pexels.com/photos/30708768/pexels-photo-30708768.jpeg',
 TRUE),

-- Family Suite (Zakopane)
('66666666-6666-6666-6666-666666666666',
 '44444444-4444-4444-4444-555555555555',
 'https://images.pexels.com/photos/7598367/pexels-photo-7598367.jpeg',
 TRUE),

('77777777-7777-7777-7777-777777777777',
 '44444444-4444-4444-4444-555555555555',
 'https://images.pexels.com/photos/7598361/pexels-photo-7598361.jpeg',
 FALSE) ON CONFLICT (id) DO NOTHING;