ALTER TABLE resources
    ADD COLUMN tags jsonb DEFAULT '[]'::jsonb;