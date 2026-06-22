CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    role       VARCHAR(50)                         NOT NULL,
    username   VARCHAR(50)                         NOT NULL UNIQUE,
    email      VARCHAR(100)                        NOT NULL UNIQUE,
    password   VARCHAR(255)                        NOT NULL,
    first_name VARCHAR(100), 
    last_name VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);

CREATE TABLE properties
(
    id          UUID PRIMARY KEY,
    owner_id    UUID                                NOT NULL,
    title       VARCHAR(255)                        NOT NULL,
    description TEXT,
    city    VARCHAR(255)                        NOT NULL,
    country VARCHAR(255) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    street VARCHAR(255) NOT NULL,
    street_number VARCHAR(50) NOT NULL,
    latitude DECIMAL(12, 8) NOT NULL,
    longitude DECIMAL(12, 8) NOT NULL,
    amenities jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_properties_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_properties_owner ON properties (owner_id);
CREATE INDEX idx_properties_location ON properties (location);

CREATE TABLE units
(
    id              UUID PRIMARY KEY,
    property_id     UUID                                NOT NULL,
    name            VARCHAR(255)                        NOT NULL,
    description     TEXT,
    price_per_night DECIMAL(10, 2)                      NOT NULL,
    capacity        INT                                 NOT NULL,
    unit_type VARCHAR(255) NOT NULL,
    unit_number VARCHAR(50) NOT NULL,
    floor INTEGER NOT NULL,
    bedrooms INTEGER NOT NULL DEFAULT 0, 
    beds     INTEGER NOT NULL DEFAULT 0,
    amenities jsonb DEFAULT '[]'::jsonb NOT NULL,
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

CREATE TABLE reservations
(
    id         UUID PRIMARY KEY,
    user_id    UUID                                NOT NULL,
    unit_id    UUID                                NOT NULL,
    start_date DATE                                NOT NULL,
    end_date   DATE                                NOT NULL,
    status     VARCHAR(50)                         NOT NULL,
    price_per_night_snapshot NUMERIC(10, 2), 
    total_price NUMERIC(10,2), 
    guest_message TEXT,
    payment_currency VARCHAR(3) DEFAULT 'PLN' NOT NULL,
    payment_exchange_rate NUMERIC(19, 4) DEFAULT 1.0000 NOT NULL,
    guest_email VARCHAR(255),
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
        CHECK (end_date >= start_date)

);

CREATE INDEX idx_res_user ON reservations (user_id);
CREATE INDEX idx_res_unit ON reservations (unit_id);

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

CREATE TABLE settlements
(
    id                   UUID PRIMARY KEY,
    reservation_id       UUID UNIQUE    NOT NULL,
    status               VARCHAR(50)    NOT NULL,
    accommodation_amount NUMERIC(12, 2) NOT NULL,
    utilities_amount     NUMERIC(12, 2) NOT NULL,
    deposit_amount       NUMERIC(12, 2) NOT NULL,
    discount_amount      NUMERIC(12, 2) NOT NULL,
    total_amount         NUMERIC(12, 2) NOT NULL,
    amount_paid          NUMERIC(12, 2) NOT NULL,
    balance_due          NUMERIC(12, 2) NOT NULL,
    issued_at            TIMESTAMP,
    paid_at              TIMESTAMP,
    invoice_requested BOOLEAN NOT NULL DEFAULT FALSE,
    invoice_pdf_path VARCHAR(255),
    company_name VARCHAR(255), 
    tax_id VARCHAR(50), 
    company_address VARCHAR(255),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_settlement_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES reservations (id)
            ON DELETE
                CASCADE
);

CREATE INDEX idx_settlement_reservation_id ON settlements (reservation_id);
CREATE INDEX idx_settlement_status ON settlements (status);

CREATE TABLE settlement_items
(
    id            UUID PRIMARY KEY,
    settlement_id UUID           NOT NULL,
    type          VARCHAR(50)    NOT NULL,
    description   TEXT,
    quantity      NUMERIC(10, 2) NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_settlement_item_settlement
        FOREIGN KEY (settlement_id)
            REFERENCES settlements (id)
            ON DELETE CASCADE
);


CREATE INDEX idx_settlement_item_settlement_id ON settlement_items (settlement_id);

CREATE TABLE unit_settlement_items
(
    id             UUID PRIMARY KEY,
    unit_id        UUID                                NOT NULL,
    settlement_item_type   VARCHAR(50)                         NOT NULL, 
    price_per_unit NUMERIC(10, 2)                      NOT NULL,
    measurement_unit      VARCHAR(20),                                
    billing_type   VARCHAR(20)                         NOT NULL, 
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT check_utility_price
        CHECK (price_per_unit >= 0),

    CONSTRAINT fk_unit_settlement_items_unit
        FOREIGN KEY (unit_id)
            REFERENCES units (id)
            ON DELETE CASCADE

);

CREATE INDEX idx_unit_settlement_items_unit_id ON unit_settlement_items (unit_id);
CREATE INDEX idx_unit_settlement_items_settlement_item_type ON unit_settlement_items (settlement_item_type);

CREATE TABLE payment_transactions
(
    id                UUID PRIMARY KEY,

    settlement_id     UUID        NOT NULL,
    unit_id           UUID        NOT NULL,

    status            VARCHAR(20) NOT NULL,

    type              VARCHAR(50) NOT NULL,

    description       TEXT NOT NULL,

    quantity          NUMERIC(12, 2) NOT NULL,
    unit_price        NUMERIC(12, 2) NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL,

    stripe_session_id VARCHAR(255) NOT NULL UNIQUE,
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,


    failure_reason    TEXT,

    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_transactions_settlement_item
        FOREIGN KEY (settlement_id)
            REFERENCES settlements (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_payment_transactions_settlement_id ON payment_transactions (settlement_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions (status);

CREATE TABLE media_readings
(
    id                       UUID PRIMARY KEY,

    settlement_id       UUID           NOT NULL,

    utility_type             VARCHAR(50)    NOT NULL,

    initial_reading          NUMERIC(12, 6),

    initial_reading_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    final_reading_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    initial_reading_source VARCHAR(50),

    final_reading_source VARCHAR(50),

    initial_confidence_score NUMERIC(12, 6),

    final_reading            NUMERIC(12, 6),

    final_confidence_score   NUMERIC(12, 6),

    consumption_difference   NUMERIC(12, 6)
        GENERATED ALWAYS AS (
            CASE
                WHEN final_reading IS NOT NULL
                    THEN final_reading - initial_reading
                ELSE NULL
                END
            ) STORED,

    unit_price               NUMERIC(12, 2) NOT NULL,

    calculated_cost          NUMERIC(12, 2)
        GENERATED ALWAYS AS (
            CASE
                WHEN final_reading IS NOT NULL
                    THEN (final_reading - initial_reading) * unit_price
                ELSE NULL
                END
            ) STORED,

    created_at               TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,

    CHECK (
final_reading IS NULL
OR initial_reading IS NULL
OR final_reading >= initial_reading
),


    CONSTRAINT fk_media_readings_settlement_item
        FOREIGN KEY (settlement_id)
            REFERENCES settlements (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_media_readings_settlement_id ON media_readings (settlement_id);
CREATE INDEX idx_media_readings_reading_status ON media_readings (reading_status);

CREATE TABLE media_reading_upload_attempts (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   media_reading_id UUID NOT NULL REFERENCES media_readings(id) ON DELETE CASCADE,
   meter_image BYTEA,
   ocr_value VARCHAR(50),
   confidence_score NUMERIC(5, 4),
   status VARCHAR(50) NOT NULL
       CHECK (
           status IN (
                      'PENDING',
                      'AUTO_APPROVED',
                      'REQUEST_REUPLOAD',
                      'REQUEST_MANUAL_REVIEW',
                      'MANUALLY_APPROVED'
               )
           ),
   reading_type VARCHAR(20) NOT NULL
       CHECK (reading_type IN ('INITIAL', 'FINAL')),
   attempted_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_upload_attempts_media_reading_id ON media_reading_upload_attempts(media_reading_id);

CREATE TABLE system_events
(
    id            UUID PRIMARY KEY,
    timestamp     TIMESTAMP   NOT NULL,
    action_type   VARCHAR(80) NOT NULL,
    actor_user_id UUID,
    entity_type   VARCHAR(80),
    entity_id     UUID,
    details       TEXT
);

CREATE INDEX idx_system_events_timestamp ON system_events (timestamp);
CREATE INDEX idx_system_events_action_type ON system_events (action_type);
CREATE INDEX idx_system_events_actor_user_id ON system_events (actor_user_id);

CREATE TABLE newsletter_subscribers (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL,
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP
);

CREATE INDEX idx_newsletter_subscribers_email ON newsletter_subscribers (email);
CREATE INDEX idx_newsletter_subscribers_token ON newsletter_subscribers (token);

CREATE TABLE password_reset_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens (token);