ALTER TABLE reservations
    ADD COLUMN cancelled_at TIMESTAMP;

UPDATE reservations r
SET cancelled_at = cancellation.changed_at
FROM (
    SELECT reservation_id, MAX(changed_at) AS changed_at
    FROM reservation_status_history
    WHERE new_status = 'CANCELLED'
    GROUP BY reservation_id
) cancellation
WHERE r.id = cancellation.reservation_id
  AND r.status = 'CANCELLED';

CREATE TABLE conversations
(
    id              UUID PRIMARY KEY,
    reservation_id  UUID                                NOT NULL,
    guest_user_id   UUID                                NOT NULL,
    owner_user_id   UUID                                NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_message_at TIMESTAMP,

    CONSTRAINT uq_conversations_reservation UNIQUE (reservation_id),

    CONSTRAINT fk_conversations_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES reservations (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_conversations_guest
        FOREIGN KEY (guest_user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_conversations_owner
        FOREIGN KEY (owner_user_id)
            REFERENCES users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_conversations_guest ON conversations (guest_user_id);
CREATE INDEX idx_conversations_owner ON conversations (owner_user_id);
