ALTER TABLE attachment
    ADD COLUMN IF NOT EXISTS voice_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS voice_waveform JSONB,
    ADD COLUMN IF NOT EXISTS voice_codec VARCHAR(50),
    ADD COLUMN IF NOT EXISTS voice_bitrate INTEGER,
    ADD COLUMN IF NOT EXISTS voice_sample_rate INTEGER,
    ADD COLUMN IF NOT EXISTS voice_channel_count SMALLINT,
    ADD COLUMN IF NOT EXISTS metadata_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN IF NOT EXISTS metadata_error TEXT,
    ADD COLUMN IF NOT EXISTS metadata_processed_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_attachment_metadata_status
    ON attachment(metadata_status);

CREATE TABLE IF NOT EXISTS export_jobs (
    export_job_id BIGSERIAL PRIMARY KEY,
    export_uuid UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    status VARCHAR(20) NOT NULL,
    format VARCHAR(10) NOT NULL DEFAULT 'JSON',
    snapshot_at TIMESTAMPTZ NOT NULL,
    storage_path VARCHAR(1024),
    file_size BIGINT,
    expires_at TIMESTAMPTZ NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error_code VARCHAR(100),
    last_error_message TEXT,
    processing_started_at TIMESTAMPTZ,
    lease_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_export_active_user
    ON export_jobs(user_id)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX IF NOT EXISTS idx_export_jobs_cleanup
    ON export_jobs(status, expires_at);

CREATE TABLE IF NOT EXISTS dlq_replay_audits (
    audit_id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT NOT NULL REFERENCES users(user_id),
    task_id VARCHAR(255) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(255),
    replayed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
