CREATE TABLE revoked_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jti        VARCHAR(36) UNIQUE NOT NULL,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    revoked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    reason     VARCHAR(50)
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);
CREATE INDEX idx_revoked_tokens_user_id ON revoked_tokens (user_id);
