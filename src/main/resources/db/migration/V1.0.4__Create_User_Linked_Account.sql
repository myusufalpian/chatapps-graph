CREATE TABLE user_linked_account (
  linked_account_id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(user_id),
  provider VARCHAR(20) NOT NULL,
  provider_sub VARCHAR(255) NOT NULL,
  provider_email VARCHAR(100),
  linked_at TIMESTAMPTZ DEFAULT now(),
  CONSTRAINT uq_provider_sub UNIQUE(provider, provider_sub)
);

CREATE INDEX idx_linked_account_user ON user_linked_account(user_id);
