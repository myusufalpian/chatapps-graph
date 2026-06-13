-- Drop old tables
DROP TABLE IF EXISTS chat_detail;
DROP TABLE IF EXISTS chat_user;
DROP TABLE IF EXISTS attachment;

-- Conversation
CREATE TABLE conversation (
    conversation_id BIGSERIAL PRIMARY KEY,
    conversation_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
    conversation_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Conversation participants
CREATE TABLE conversation_participant (
    participant_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversation(conversation_id),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    joined_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT uq_conversation_user UNIQUE(conversation_id, user_id)
);
CREATE INDEX idx_participant_user ON conversation_participant(user_id);

-- Attachment
CREATE TABLE attachment (
    attachment_id BIGSERIAL PRIMARY KEY,
    attachment_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
    uploader_id BIGINT NOT NULL REFERENCES users(user_id),
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    attachment_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Message
CREATE TABLE message (
    message_id BIGSERIAL PRIMARY KEY,
    message_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
    conversation_id BIGINT NOT NULL REFERENCES conversation(conversation_id),
    sender_id BIGINT NOT NULL REFERENCES users(user_id),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content TEXT,
    attachment_id BIGINT REFERENCES attachment(attachment_id),
    reply_to_message_id BIGINT REFERENCES message(message_id),
    message_status INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_message_conversation_cursor ON message(conversation_id, created_at, message_id);

-- Message receipt
CREATE TABLE message_receipt (
    receipt_id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES message(message_id),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    status INTEGER NOT NULL DEFAULT 0,
    is_deleted_for_me BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMPTZ,
    CONSTRAINT uq_receipt_message_user UNIQUE(message_id, user_id)
);
CREATE INDEX idx_receipt_user_status ON message_receipt(user_id, status);
