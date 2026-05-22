CREATE TABLE unit_settlement_items
(
    id             UUID PRIMARY KEY,
    unit_id        UUID                                NOT NULL,
    settlement_item_type   VARCHAR(50)                         NOT NULL, -- settlementItem type
    price_per_unit NUMERIC(10, 2)                      NOT NULL,
    measurement_unit      VARCHAR(20),                                  -- kWh, m3, liter
    billing_type   VARCHAR(20)                         NOT NULL, -- INCLUDED, PER_USAGE, FIXED
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_utility_price
        CHECK (price_per_unit >= 0),

    CONSTRAINT fk_unit_settlement_items_unit
        FOREIGN KEY (unit_id)
            REFERENCES units (id)
            ON DELETE CASCADE

);

CREATE INDEX idx_unit_settlement_items_unit_id ON unit_settlement_items (unit_id);
CREATE INDEX idx_unit_settlement_items_settlement_item_type ON unit_settlement_items (settlement_item_type);

INSERT INTO unit_settlement_items (
    id,
    unit_id,
    settlement_item_type,
    price_per_unit,
    measurement_unit,
    billing_type
)
VALUES
-- Studio 1 (Warsaw)
(
    gen_random_uuid(),
    '11111111-1111-1111-1111-222222222222',
    'WATER',
    18.50,
    'M3',
    'PER_USAGE'
),

-- Apartment Deluxe (Warsaw)
(
    gen_random_uuid(),
    '22222222-2222-2222-2222-333333333333',
    'WATER',
    18.50,
    'M3',
    'PER_USAGE'
),

-- Room 1 (Zakopane)
(
    gen_random_uuid(),
    '33333333-3333-3333-3333-444444444444',
    'WATER',
    19.50,
    'M3',
    'PER_USAGE'
),

-- Family Suite (Zakopane)
(
    gen_random_uuid(),
    '44444444-4444-4444-4444-555555555555',
    'WATER',
    19.50,
    'M3',
    'PER_USAGE'
)
    ON CONFLICT DO NOTHING;

-- Correct inconsistency in seed data
DELETE FROM settlement_items
WHERE settlement_id IN (
                        'aaaaaaaa-aaaa-aaaa-aaaa-333333333333',
                        'aaaaaaaa-aaaa-aaaa-aaaa-444444444444'
    );