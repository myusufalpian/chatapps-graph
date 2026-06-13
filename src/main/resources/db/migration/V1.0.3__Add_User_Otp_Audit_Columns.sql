ALTER TABLE user_otp ALTER COLUMN otp_code TYPE VARCHAR(64);
ALTER TABLE user_otp ADD COLUMN user_id BIGINT REFERENCES users(user_id);
ALTER TABLE user_otp ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'SIGN_IN';
ALTER TABLE user_otp ADD COLUMN verified_at TIMESTAMPTZ;

CREATE INDEX idx_user_otp_audit ON user_otp(user_id, purpose, created_at DESC);
