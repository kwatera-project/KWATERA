CREATE TABLE review_summaries
(
      id                   UUID PRIMARY KEY,
      property_id          UUID REFERENCES properties (id) ON DELETE CASCADE,
      unit_id              UUID REFERENCES units (id) ON DELETE CASCADE,
      ai_summary           TEXT                                NOT NULL,
      input_fingerprint    VARCHAR(255)                        NOT NULL, -- Hash used to detect changes in reviews
      generation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
      created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
      updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP

      CONSTRAINT chk_review_target CHECK (
          (property_id IS NOT NULL AND unit_id IS NULL) OR
          (property_id IS NULL AND unit_id IS NOT NULL)
      )
);

CREATE INDEX idx_review_summaries_fingerprint ON review_summaries (input_fingerprint);

CREATE UNIQUE INDEX idx_unique_property_summary
    ON review_summaries (property_id)
    WHERE property_id IS NOT NULL;

CREATE UNIQUE INDEX idx_unique_unit_summary
    ON review_summaries (unit_id)
    WHERE unit_id IS NOT NULL;