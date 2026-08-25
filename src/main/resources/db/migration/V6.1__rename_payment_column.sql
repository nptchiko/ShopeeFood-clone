ALTER TABLE revoked_tokens
    DROP CONSTRAINT revoked_tokens_user_id_fkey;

CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

ALTER TABLE payments
    RENAME COLUMN party_name TO payment_provider;

DROP EXTENSION IF EXISTS postgis CASCADE;

ALTER TABLE user_otps
    DROP COLUMN verified_at;

ALTER TABLE user_otps
    ALTER COLUMN otp TYPE VARCHAR(6) USING (otp::VARCHAR(6));

ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(50) USING (role::VARCHAR(50));