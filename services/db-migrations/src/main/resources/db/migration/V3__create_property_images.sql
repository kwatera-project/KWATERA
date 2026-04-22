CREATE TABLE property_images
(
    id          UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    url         TEXT NOT NULL,
    is_main     BOOLEAN,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_property
        FOREIGN KEY (property_id)
            REFERENCES properties (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_property_images_property_id ON property_images (property_id);