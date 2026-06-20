ALTER TABLE properties
    DROP COLUMN IF EXISTS property_type;

UPDATE units
SET unit_type = 'ENTIRE_APARTMENT', amenities = '["WiFi","Elevator","Air Conditioning"]'::jsonb
WHERE id IN (
             '11111111-1111-1111-1111-222222222222',
             '22222222-2222-2222-2222-333333333333'
    );

UPDATE units
SET unit_type = 'PRIVATE_ROOM_IN_HOME', amenities = '["WiFi","Shared Kitchen","Parking"]'::jsonb
WHERE id = '33333333-3333-3333-3333-444444444444';

UPDATE units
SET unit_type = 'ENTIRE_COTTAGE', amenities = '["WiFi","Sauna","Fireplace","Shared Kitchen","Parking"]'::jsonb
WHERE id = '44444444-4444-4444-4444-555555555555';

UPDATE properties
SET amenities = '["WiFi","Elevator","Air Conditioning"]'::jsonb
WHERE id = 'aaaa1111-1111-1111-1111-111111111111';

UPDATE properties
SET amenities = '["WiFi","Sauna","Fireplace","Shared Kitchen","Parking"]'::jsonb
WHERE id = 'bbbb2222-2222-2222-2222-222222222222';