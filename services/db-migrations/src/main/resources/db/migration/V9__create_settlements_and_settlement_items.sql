CREATE TABLE settlements
(
    id                   UUID PRIMARY KEY,
    reservation_id       UUID           NOT NULL,
    status               VARCHAR(50)    NOT NULL,
    accommodation_amount NUMERIC(12, 2) NOT NULL,
    utilities_amount     NUMERIC(12, 2) NOT NULL,
    deposit_amount       NUMERIC(12, 2) NOT NULL,
    total_amount         NUMERIC(12, 2) NOT NULL,
    amount_paid          NUMERIC(12, 2) NOT NULL,
    balance_due          NUMERIC(12, 2) NOT NULL,
    issued_at            TIMESTAMP,
    paid_at              TIMESTAMP,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_settlement_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES reservations (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_settlement_reservation_id ON settlements (reservation_id);
CREATE INDEX idx_settlement_status ON settlements (status);

-- DESCRIPTION:
-- accommodation_amount	- the cost specifically related to the rent.
-- utilities_amount	- the cost of services like water or electricity.
-- deposit_amount - the security deposit required for the reservation.
-- total_amount	- the sum of all charges (accommodation + utilities + deposit).
-- amount_paid - the total amount the customer has already paid.
-- balance_due - the remaining amount that still needs to be paid (total − paid).
-- issued_at - the date and time when the settlement was officially issued.
-- paid_at - the date and time when the full balance was settled.

CREATE TABLE settlement_items
(
    id            UUID PRIMARY KEY,
    settlement_id UUID           NOT NULL,
    type          VARCHAR(50)    NOT NULL,
    description   TEXT,
    quantity      NUMERIC(10, 2) NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_settlement_item_settlement
        FOREIGN KEY (settlement_id)
            REFERENCES settlements (id)
            ON DELETE CASCADE
);


CREATE INDEX idx_settlement_item_settlement_id ON settlement_items (settlement_id);


INSERT INTO settlements (id,
                         reservation_id,
                         status,
                         accommodation_amount,
                         utilities_amount,
                         deposit_amount,
                         total_amount,
                         amount_paid,
                         balance_due,
                         issued_at,
                         paid_at)
VALUES

-- 1 COMPLETED -> PAID
('aaaaaaaa-aaaa-aaaa-aaaa-111111111111',
 '00000000-0000-0000-0000-111111111111',
 'PAID',
 1000.00,
 40.00,
 0.00,
 1040.00,
 1040.00,
 0.00,
 '2025-01-15 10:00:00',
 '2025-01-15 15:00:00'),

-- 2 CONFIRMED -> PAID_ACCOMMODATION
('aaaaaaaa-aaaa-aaaa-aaaa-222222222222',
 '00000000-0000-0000-0000-222222222222',
 'PAID_ACCOMMODATION',
 1400.00,
 0.00,
 0.00,
 1400.00,
 1400.00,
 0.00,
 NULL,
 NULL),

-- 3 PENDING -> DRAFT
('aaaaaaaa-aaaa-aaaa-aaaa-333333333333',
 '00000000-0000-0000-0000-333333333333',
 'DRAFT',
 900.00,
 0.00,
 0.00,
 900.00,
 0.00,
 900.00,
 NULL,
 NULL),

-- 4 CANCELLED -> CANCELLED
('aaaaaaaa-aaaa-aaaa-aaaa-444444444444',
 '00000000-0000-0000-0000-444444444444',
 'CANCELLED',
 1200.00,
 0.00,
 0.00,
 1200.00,
 0.00,
 0.00,
 NULL,
 NULL),

-- 5 CONFIRMED -> PAID_ACCOMMODATION
('aaaaaaaa-aaaa-aaaa-aaaa-555555555555',
 '00000000-0000-0000-0000-555555555555',
 'PAID_ACCOMMODATION',
 1000.00,
 0.00,
 0.00,
 1000.00,
 1000.00,
 0.00,
 NULL,
 NULL) ON CONFLICT (id) DO NOTHING;


INSERT INTO settlement_items (id,
                              settlement_id,
                              type,
                              description,
                              quantity,
                              unit_price,
                              amount)
VALUES

-- settlement 1
('bbbbbbbb-bbbb-bbbb-bbbb-111111111111',
 'aaaaaaaa-aaaa-aaaa-aaaa-111111111111',
 'ACCOMMODATION',
 'Accommodation fee',
 1,
 1000.00,
 1000.00),

('bbbbbbbb-bbbb-bbbb-bbbb-111111111112',
 'aaaaaaaa-aaaa-aaaa-aaaa-111111111111',
 'WATER',
 'Water usage',
 8,
 5.00,
 40.00),

-- settlement 2
('bbbbbbbb-bbbb-bbbb-bbbb-222222222221',
 'aaaaaaaa-aaaa-aaaa-aaaa-222222222222',
 'ACCOMMODATION',
 'Accommodation fee',
 1,
 1400.00,
 1400.00),

-- settlement 3
('bbbbbbbb-bbbb-bbbb-bbbb-333333333331',
 'aaaaaaaa-aaaa-aaaa-aaaa-333333333333',
 'ACCOMMODATION',
 'Accommodation fee',
 1,
 900.00,
 900.00),

-- settlement 4
('bbbbbbbb-bbbb-bbbb-bbbb-444444444441',
 'aaaaaaaa-aaaa-aaaa-aaaa-444444444444',
 'ACCOMMODATION',
 'Accommodation fee',
 1,
 1200.00,
 1200.00),

-- settlement 5
('bbbbbbbb-bbbb-bbbb-bbbb-555555555551',
 'aaaaaaaa-aaaa-aaaa-aaaa-555555555555',
 'ACCOMMODATION',
 'Accommodation fee',
 1,
 1000.00,
 1000.00) ON CONFLICT (id) DO NOTHING;






