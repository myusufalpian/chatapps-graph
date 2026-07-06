-- Sprint 4: Message Edit Feature

ALTER TABLE message ADD COLUMN IF NOT EXISTS edited_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS message_edit_history (
  history_id BIGSERIAL PRIMARY KEY,
  message_id BIGINT NOT NULL REFERENCES message(message_id) ON DELETE CASCADE,
  original_content TEXT NOT NULL,
  edited_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_message_edit_history_message ON message_edit_history(message_id);
