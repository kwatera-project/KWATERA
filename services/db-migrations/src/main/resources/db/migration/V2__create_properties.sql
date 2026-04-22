CREATE TABLE properties
(
    id          UUID PRIMARY KEY,
    owner_id    UUID                                NOT NULL,
    title       VARCHAR(255)                        NOT NULL,
    description TEXT,
    location    VARCHAR(255)                        NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_properties_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_properties_owner ON properties (owner_id);
CREATE INDEX idx_properties_location ON properties (location);
