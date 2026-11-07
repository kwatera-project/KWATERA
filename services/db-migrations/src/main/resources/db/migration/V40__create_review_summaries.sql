CREATE TABLE review_summaries
(
      id                   UUID PRIMARY KEY,
      ai_summary           TEXT                                NOT NULL,
      input_fingerprint    VARCHAR(255)                        NOT NULL, -- Hash used to detect changes in reviews
      generation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
      created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
      updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_review_summaries_fingerprint ON review_summaries (input_fingerprint);
