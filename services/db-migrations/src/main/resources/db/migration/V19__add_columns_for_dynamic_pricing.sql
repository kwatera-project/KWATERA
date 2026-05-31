ALTER TABLE properties
    RENAME COLUMN location TO city;

ALTER TABLE properties
    ADD COLUMN country VARCHAR(255),
    ADD COLUMN postal_code VARCHAR(20),
    ADD COLUMN street VARCHAR(255),
    ADD COLUMN street_number VARCHAR(50),
    ADD COLUMN latitude DECIMAL(12, 8),
    ADD COLUMN longitude DECIMAL(12, 8);

UPDATE properties
SET country       = 'Poland',
    postal_code   = '00-864',
    street        = 'Krochmalna',
    street_number = '61',
    latitude      = 52.235363,
    longitude     = 20.985603
WHERE id = 'aaaa1111-1111-1111-1111-111111111111';

UPDATE properties
SET country       = 'Poland',
    postal_code   = '34-500',
    street        = 'Oberconiówka',
    street_number = '7',
    latitude      = 49.29765319824219,
    longitude     = 19.991104125976562
WHERE id = 'bbbb2222-2222-2222-2222-222222222222';

ALTER TABLE units
    ADD COLUMN unit_type VARCHAR(255),
    ADD COLUMN unit_number VARCHAR(50),
    ADD COLUMN floor INTEGER;

UPDATE units
SET unit_type   = 'ENTIRE_APARTMENT',
    unit_number = '17K',
    floor       = 7
WHERE id = '11111111-1111-1111-1111-222222222222';

UPDATE units
SET unit_type   = 'ENTIRE_APARTMENT',
    unit_number = '19K',
    floor       = 7
WHERE id = '22222222-2222-2222-2222-333333333333';

UPDATE units
SET unit_type   = 'PRIVATE_ROOM_IN_HOME',
    unit_number = '10',
    floor       = 1
WHERE id = '33333333-3333-3333-3333-444444444444';

UPDATE units
SET unit_type   = 'ENTIRE_GUEST_SUITE',
    unit_number = '10',
    floor       = 0
WHERE id = '44444444-4444-4444-4444-555555555555';


ALTER TABLE properties
    ALTER COLUMN country SET NOT NULL;

ALTER TABLE properties
    ALTER COLUMN postal_code SET NOT NULL;

ALTER TABLE properties
    ALTER COLUMN street SET NOT NULL;

ALTER TABLE properties
    ALTER COLUMN street_number SET NOT NULL;

ALTER TABLE properties
    ALTER COLUMN latitude SET NOT NULL;

ALTER TABLE properties
    ALTER COLUMN longitude SET NOT NULL;


ALTER TABLE units
    ALTER COLUMN unit_type SET NOT NULL;

ALTER TABLE units
    ALTER COLUMN unit_number SET NOT NULL;

ALTER TABLE units
    ALTER COLUMN floor SET NOT NULL;

