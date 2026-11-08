ALTER TABLE review_summaries
    ADD COLUMN review_ids UUID[] NOT NULL DEFAULT '{}';
