ALTER TABLE reservations
    ADD COLUMN payment_currency VARCHAR(3) DEFAULT 'PLN' NOT NULL,
    ADD COLUMN payment_exchange_rate NUMERIC(19, 4) DEFAULT 1.0000 NOT NULL;
