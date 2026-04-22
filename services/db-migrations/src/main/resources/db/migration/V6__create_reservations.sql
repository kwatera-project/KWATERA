CREATE TABLE reservations
(
    id         UUID PRIMARY KEY,
    user_id    UUID                                NOT NULL,
    unit_id    UUID                                NOT NULL,
    start_date DATE                                NOT NULL,
    end_date   DATE                                NOT NULL,
    status     VARCHAR(50)                         NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservations_user FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_reservations_unit
        FOREIGN KEY (unit_id)
            REFERENCES units (id)
            ON DELETE CASCADE,

    CONSTRAINT check_dates
        CHECK (end_date > start_date)

);

CREATE INDEX idx_res_user ON reservations (user_id);
CREATE INDEX idx_res_unit ON reservations (unit_id);