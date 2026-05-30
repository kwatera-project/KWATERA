DROP TABLE IF EXISTS media_readings;

CREATE TABLE media_readings (
                                id UUID NOT NULL PRIMARY KEY,
                                settlement_id UUID NOT NULL,
                                utility_type VARCHAR(50) NOT NULL,

                                initial_reading NUMERIC(12, 6) NOT NULL,
                                initial_confidence_score NUMERIC(12, 6) NOT NULL,
                                initial_reading_source VARCHAR(50) NOT NULL,
                                initial_reading_status VARCHAR(50) NOT NULL,

                                final_reading NUMERIC(12, 6),
                                final_confidence_score NUMERIC(12, 6),
                                final_reading_source VARCHAR(50),
                                final_reading_status VARCHAR(50),

                                consumption_difference NUMERIC(12, 6)
                                    GENERATED ALWAYS AS (final_reading - initial_reading),

                                unit_price NUMERIC(12, 2) NOT NULL,

                                calculated_cost NUMERIC(12, 2)
                                    GENERATED ALWAYS AS ((final_reading - initial_reading) * unit_price),

                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                CONSTRAINT media_readings_final_gte_initial CHECK (
                                    final_reading IS NULL OR final_reading >= initial_reading
                                    )
);