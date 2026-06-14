CREATE TABLE message_reaction (
  reaction_id BIGSERIAL PRIMARY KEY,
  message_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  emoji VARCHAR(10) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(message_id, user_id)
);

CREATE INDEX idx_reaction_message ON message_reaction(message_id);

ALTER TABLE message ADD COLUMN forwarded_from_id BIGINT;
