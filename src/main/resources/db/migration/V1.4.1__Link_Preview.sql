CREATE TABLE IF NOT EXISTS link_preview (
  preview_id BIGSERIAL PRIMARY KEY,
  url VARCHAR(2048) NOT NULL,
  url_hash VARCHAR(64) NOT NULL,
  title VARCHAR(500),
  description VARCHAR(2000),
  image_url VARCHAR(2048),
  site_name VARCHAR(200),
  fetched_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_link_preview_url UNIQUE (url)
);

CREATE INDEX IF NOT EXISTS idx_link_preview_url_hash ON link_preview(url_hash);

ALTER TABLE message 
ADD COLUMN IF NOT EXISTS preview_id BIGINT DEFAULT NULL;

ALTER TABLE message 
ADD CONSTRAINT fk_message_preview 
FOREIGN KEY (preview_id) REFERENCES link_preview(preview_id);
