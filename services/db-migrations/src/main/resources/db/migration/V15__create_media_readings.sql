CREATE TABLE media_readings
(
    id                       UUID PRIMARY KEY,

    settlement_item_id       UUID           NOT NULL,

    utility_type             VARCHAR(50)    NOT NULL,

    initial_reading          NUMERIC(12, 6) NOT NULL,

    initial_confidence_score NUMERIC(12, 6) NOT NULL,

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

    reading_source           VARCHAR(50)    NOT NULL DEFAULT 'OCR',
    reading_status           VARCHAR(50)    NOT NULL DEFAULT 'PENDING',

    created_at               TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,

    CHECK (
        final_reading IS NULL
            OR final_reading >= initial_reading
        ),

    CONSTRAINT fk_media_readings_settlement_item
        FOREIGN KEY (settlement_item_id)
            REFERENCES settlement_items (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_media_readings_settlement_item_id ON media_readings (settlement_item_id);
CREATE INDEX idx_media_readings_reading_status ON media_readings (reading_status);

INSERT INTO media_readings (id,
                            settlement_item_id,
                            utility_type,
                            initial_reading,
                            initial_confidence_score,
                            final_reading,
                            final_confidence_score,
                            unit_price,
                            reading_source,
                            reading_status)
VALUES (gen_random_uuid(),
        'bbbbbbbb-bbbb-bbbb-bbbb-111111111112',
        'WATER',
        100.000000,
        0.982145,
        108.000000,
        0.997531,
        5.00,
        'OCR',
        'AUTO_APPROVED');