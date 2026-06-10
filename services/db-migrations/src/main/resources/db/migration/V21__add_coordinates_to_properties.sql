DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='properties' AND column_name='latitude') THEN
        ALTER TABLE properties ADD COLUMN latitude DECIMAL(10, 8);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='properties' AND column_name='longitude') THEN
        ALTER TABLE properties ADD COLUMN longitude DECIMAL(10, 8);
    END IF;
END $$;
