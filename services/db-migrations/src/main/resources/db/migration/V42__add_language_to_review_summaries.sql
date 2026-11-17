ALTER TABLE review_summaries
    ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'en';

DROP INDEX idx_unique_property_summary;
DROP INDEX idx_unique_unit_summary;

CREATE UNIQUE INDEX idx_unique_property_summary_language
    ON review_summaries (property_id, language)
    WHERE property_id IS NOT NULL;

CREATE UNIQUE INDEX idx_unique_unit_summary_language
    ON review_summaries (unit_id, language)
    WHERE unit_id IS NOT NULL;