-- Add profile_photo column to users table
ALTER TABLE users ADD COLUMN profile_photo TEXT;

-- Table: Contact
CREATE TABLE contact (
    contact_id BIGSERIAL PRIMARY KEY,
    contact_uuid VARCHAR(64) DEFAULT gen_random_uuid()::text,
    owner_user_id BIGINT NOT NULL REFERENCES users(user_id),
    contact_user_id BIGINT NOT NULL REFERENCES users(user_id),
    display_name VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT now(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    UNIQUE(owner_user_id, contact_user_id)
);

CREATE INDEX idx_contact_owner ON contact(owner_user_id);
CREATE INDEX idx_users_phone_status ON users(user_phone, user_status);
