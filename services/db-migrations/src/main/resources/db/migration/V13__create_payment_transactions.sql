CREATE TABLE payment_transactions
(
    id                UUID PRIMARY KEY,

    settlement_id     UUID        NOT NULL,
    unit_id           UUID        NOT NULL,

    status            VARCHAR(20) NOT NULL,

    type              VARCHAR(50) NOT NULL,

    description       TEXT NOT NULL,

    quantity          NUMERIC(12, 2) NOT NULL,
    unit_price        NUMERIC(12, 2) NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,

    stripe_session_id VARCHAR(255) NOT NULL UNIQUE,

    failure_reason    TEXT,

    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_transactions_settlement_item
        FOREIGN KEY (settlement_id)
            REFERENCES settlements (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_payment_transactions_settlement_id ON payment_transactions (settlement_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions (status);

INSERT INTO payment_transactions (
    id,
    settlement_id,
    unit_id,
    status,
    type,
    description,
    quantity,
    unit_price,
    amount,
    stripe_session_id,
    failure_reason,
    created_at
)
VALUES

-- 1 SETTLEMENT 1 → ACCOMMODATION (PAID)
(gen_random_uuid(),
 'aaaaaaaa-aaaa-aaaa-aaaa-111111111111',
 '11111111-1111-1111-1111-222222222222',
 'SUCCESS',
 'ACCOMMODATION',
 'Accommodation fee payment',
 1,
 1000.00,
 1000.00,
 'cs_test_111_paid',
 NULL,
 '2025-01-15 15:00:00'),

-- WATER item → SUCCESS
(gen_random_uuid(),
 'aaaaaaaa-aaaa-aaaa-aaaa-111111111111',
 '11111111-1111-1111-1111-222222222222',
 'SUCCESS',
 'WATER',
 'Water usage payment',
 8,
 5.00,
 40.00,
 'cs_test_111_water',
 NULL,
 '2025-01-15 15:05:00'),

-- 2 SETTLEMENT 2 → ACCOMMODATION (PAID)
(gen_random_uuid(),
 'aaaaaaaa-aaaa-aaaa-aaaa-222222222222',
 '22222222-2222-2222-2222-333333333333',
 'SUCCESS',
 'ACCOMMODATION',
 'Accommodation fee payment',
 1,
 1400.00,
 1400.00,
 'cs_test_222_paid',
 NULL,
 '2025-01-10 12:00:00'),

-- 3 SETTLEMENT 3 → FAILED PAYMENT (DRAFT settlement)
(gen_random_uuid(),
 'aaaaaaaa-aaaa-aaaa-aaaa-333333333333',
 '33333333-3333-3333-3333-444444444444',
 'FAILED',
 'ACCOMMODATION',
 'Accommodation fee payment',
 1,
 900.00,
 900.00,
 'cs_test_333_failed',
 'Card declined',
 '2025-01-05 10:00:00'),

-- 4 SETTLEMENT 4 → FAILED (cancelled)
(gen_random_uuid(),
 'aaaaaaaa-aaaa-aaaa-aaaa-444444444444',
 '44444444-4444-4444-4444-555555555555',
 'FAILED',
 'ACCOMMODATION',
 'Accommodation fee payment',
 1,
 1200.00,
 1200.00,
 'cs_test_444_cancelled',
 'Cancelled by user',
 '2025-01-02 09:00:00'),

-- 5 SETTLEMENT 5 → SUCCESS
(gen_random_uuid(),
 'aaaaaaaa-aaaa-aaaa-aaaa-555555555555',
 '11111111-1111-1111-1111-222222222222',
 'SUCCESS',
 'ACCOMMODATION',
 'Accommodation fee payment',
 1,
 1000.00,
 1000.00,
 'cs_test_555_paid',
 NULL,
 '2025-01-20 18:00:00')

    ON CONFLICT (id) DO NOTHING;

