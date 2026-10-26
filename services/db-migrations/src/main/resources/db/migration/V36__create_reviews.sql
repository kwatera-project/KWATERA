CREATE TABLE reviews
(
    id             UUID PRIMARY KEY,
    reservation_id UUID                                NOT NULL,
    author_id      UUID                                NOT NULL,
    rating         INTEGER                             NOT NULL,
    comment        TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reviews_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES reservations (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_reviews_author
        FOREIGN KEY (author_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_reviews_reservation ON reviews (reservation_id);
CREATE INDEX idx_reviews_author ON reviews (author_id);
CREATE INDEX idx_reviews_rating ON reviews (rating);