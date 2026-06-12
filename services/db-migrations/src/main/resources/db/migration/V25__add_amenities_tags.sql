ALTER TABLE properties ADD COLUMN amenities jsonb DEFAULT '[]'::jsonb NOT NULL;
ALTER TABLE units ADD COLUMN amenities jsonb DEFAULT '[]'::jsonb NOT NULL;
