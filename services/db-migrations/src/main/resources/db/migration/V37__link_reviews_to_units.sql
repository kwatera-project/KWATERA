DO $$
BEGIN
    IF EXISTS (
        SELECT reservation_id
        FROM reviews
        GROUP BY reservation_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Duplicate reviews exist. Resolve them before applying V37.';
END IF;

    IF EXISTS (
        SELECT 1
        FROM reviews review
        JOIN reservations reservation
          ON reservation.id = review.reservation_id
        WHERE review.author_id <> reservation.user_id
           OR reservation.status <> 'COMPLETED'
    ) THEN
        RAISE EXCEPTION
            'Existing reviews do not satisfy verified-review rules. Resolve them before applying V37.';
END IF;
END
$$;

ALTER TABLE reviews
    ADD COLUMN unit_id UUID;

UPDATE reviews review
SET unit_id = reservation.unit_id
    FROM reservations reservation
WHERE reservation.id = review.reservation_id;

ALTER TABLE reviews
    ALTER COLUMN unit_id SET NOT NULL;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_unit
        FOREIGN KEY (unit_id)
            REFERENCES units (id)
            ON DELETE CASCADE;

ALTER TABLE reviews
    ADD CONSTRAINT uq_reviews_reservation
        UNIQUE (reservation_id);

ALTER TABLE reviews
    ADD CONSTRAINT check_reviews_rating
        CHECK (rating BETWEEN 1 AND 5);

CREATE INDEX idx_reviews_unit_created
    ON reviews (unit_id, created_at DESC, id DESC);