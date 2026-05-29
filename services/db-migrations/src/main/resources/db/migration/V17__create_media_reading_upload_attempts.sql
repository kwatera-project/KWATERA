ALTER TABLE media_readings
    ALTER COLUMN initial_reading DROP NOT NULL,
ALTER COLUMN initial_confidence_score DROP NOT NULL;

ALTER TABLE media_readings
DROP CONSTRAINT IF EXISTS media_readings_check;

ALTER TABLE media_readings
    ADD CONSTRAINT media_readings_check
        CHECK (
            final_reading IS NULL
                OR initial_reading IS NULL
                OR final_reading >= initial_reading
            );

ALTER TABLE media_readings
    ADD COLUMN initial_reading_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN final_reading_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN initial_reading_source VARCHAR(50),
    ADD COLUMN final_reading_source VARCHAR(50);

ALTER TABLE media_readings
DROP COLUMN IF EXISTS reading_status,
    DROP COLUMN IF EXISTS reading_source;

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

CREATE INDEX idx_upload_attempts_media_reading_id
    ON media_reading_upload_attempts(media_reading_id);