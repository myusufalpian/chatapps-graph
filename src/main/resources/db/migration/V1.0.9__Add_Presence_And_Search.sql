ALTER TABLE users ADD COLUMN last_seen_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN presence_visibility VARCHAR(20) NOT NULL DEFAULT 'EVERYONE';

ALTER TABLE message ADD COLUMN search_vector TSVECTOR;
CREATE INDEX idx_message_search ON message USING GIN(search_vector);

CREATE OR REPLACE FUNCTION message_search_vector_trigger() RETURNS trigger AS $$
BEGIN
  NEW.search_vector := to_tsvector('simple', COALESCE(NEW.content, ''));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER message_search_update BEFORE INSERT OR UPDATE OF content
  ON message FOR EACH ROW EXECUTE FUNCTION message_search_vector_trigger();

UPDATE message SET search_vector = to_tsvector('simple', COALESCE(content, ''));
