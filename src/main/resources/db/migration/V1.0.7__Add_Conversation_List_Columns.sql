ALTER TABLE conversation_participant ADD COLUMN last_message_at TIMESTAMPTZ;
ALTER TABLE conversation_participant ADD COLUMN last_message_preview TEXT;
ALTER TABLE conversation_participant ADD COLUMN last_message_type VARCHAR(20);
ALTER TABLE conversation_participant ADD COLUMN unread_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE conversation_participant ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE conversation_participant ADD COLUMN pinned_at TIMESTAMPTZ;
ALTER TABLE conversation_participant ADD COLUMN is_archived BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE conversation_participant ADD COLUMN is_muted BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_participant_list ON conversation_participant(user_id, is_archived, is_pinned, last_message_at DESC);
