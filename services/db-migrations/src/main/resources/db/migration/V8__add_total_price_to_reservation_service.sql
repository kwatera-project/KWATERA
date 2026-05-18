ALTER TABLE reservations
    ADD COLUMN price_per_night_snapshot NUMERIC(10, 2),
    ADD COLUMN total_price NUMERIC(10,2);

UPDATE reservations
SET price_per_night_snapshot = 200.00,
    total_price              = 1000.00
WHERE id = '00000000-0000-0000-0000-111111111111';

UPDATE reservations
SET price_per_night_snapshot = 350.00,
    total_price              = 1400.00
WHERE id = '00000000-0000-0000-0000-222222222222';

UPDATE reservations
SET price_per_night_snapshot = 180.00,
    total_price              = 900.00
WHERE id = '00000000-0000-0000-0000-333333333333';

UPDATE reservations
SET price_per_night_snapshot = 400.00,
    total_price              = 1200.00
WHERE id = '00000000-0000-0000-0000-444444444444';

UPDATE reservations
SET price_per_night_snapshot = 200.00,
    total_price              = 1000.00
WHERE id = '00000000-0000-0000-0000-555555555555';