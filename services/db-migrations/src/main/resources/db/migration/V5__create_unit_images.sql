CREATE TABLE unit_images
(
    id         UUID PRIMARY KEY,
    unit_id    UUID NOT NULL,
    url        TEXT NOT NULL,
    is_main    BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_unit
        FOREIGN KEY (unit_id)
            REFERENCES units (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_unit_images_unit_id ON unit_images (unit_id);