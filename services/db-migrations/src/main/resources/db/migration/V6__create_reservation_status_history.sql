CREATE TABLE reservation_status_history
(
    id             UUID PRIMARY KEY,
    reservation_id UUID                                NOT NULL,
    old_status     VARCHAR(50),
    new_status     VARCHAR(50)                         NOT NULL,
    changed_by     UUID                                NOT NULL,
    changed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_status_history_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES reservations (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_status_history_user
        FOREIGN KEY (changed_by)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_status_history_res ON reservation_status_history (reservation_id);