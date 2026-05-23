ALTER TABLE payment_transactions
ADD COLUMN stripe_event_id VARCHAR(255);

UPDATE payment_transactions
SET stripe_event_id = concat('legacy_evt_', id)
WHERE stripe_event_id IS NULL;

ALTER TABLE payment_transactions
ADD CONSTRAINT uk_stripe_event_id UNIQUE (stripe_event_id);

ALTER TABLE payment_transactions
ALTER COLUMN stripe_event_id SET NOT NULL;