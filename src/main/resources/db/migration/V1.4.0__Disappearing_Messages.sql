ALTER TABLE conversation 
ADD COLUMN IF NOT EXISTS disappearing_ttl INTEGER DEFAULT NULL;

COMMENT ON COLUMN conversation.disappearing_ttl IS 'TTL in hours: 24, 168, 720, or NULL for off';

ALTER TABLE message
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ DEFAULT NULL;
