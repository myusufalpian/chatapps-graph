ALTER TABLE "group" ADD COLUMN avatar_path VARCHAR(500);
ALTER TABLE "group" ADD COLUMN allow_member_add BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE "group" ADD COLUMN conversation_id BIGINT REFERENCES conversation(conversation_id);

ALTER TABLE conversation ADD COLUMN group_id BIGINT REFERENCES "group"(group_id);

CREATE INDEX idx_group_conversation ON "group"(conversation_id) WHERE is_active = 1;
CREATE INDEX idx_conversation_group ON conversation(group_id) WHERE group_id IS NOT NULL;
