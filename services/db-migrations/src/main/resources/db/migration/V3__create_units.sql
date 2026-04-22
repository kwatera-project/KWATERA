CREATE TABLE units
(
    id              UUID PRIMARY KEY,
    property_id     UUID                                NOT NULL,
    name            VARCHAR(255)                        NOT NULL,
    description     TEXT,
    price_per_night DECIMAL(10, 2)                      NOT NULL,
    capacity        INT                                 NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_units_property
        FOREIGN KEY (property_id)
            REFERENCES properties (id)
            ON DELETE CASCADE,

    CONSTRAINT check_capacity
        CHECK (capacity > 0),

    CONSTRAINT check_price_per_night
        CHECK (price_per_night > 0)

);

CREATE INDEX idx_units_property ON units (property_id);