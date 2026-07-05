CREATE TABLE user_device (
  device_id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_token VARCHAR(500) NOT NULL,
  platform VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at TIMESTAMPTZ,
  UNIQUE(device_token)
);

CREATE INDEX idx_user_device_user ON user_device(user_id) WHERE deleted_at IS NULL;

ALTER TABLE attachment ADD COLUMN thumbnail_path VARCHAR(500);
