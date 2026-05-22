-- V3: Email-verification support.
-- The email-verification feature (VerificationToken entity + AppUser.isVerified)
-- shipped without a migration. Under the supabase profile (ddl-auto=validate),
-- Hibernate aborts startup because users.is_verified and the verification_tokens
-- table are absent from the schema. This migration closes that gap.
--
-- IF NOT EXISTS guards keep it safe whether or not an earlier ddl-auto=update run
-- already created these objects on the live database.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS verification_tokens (
    id          UUID DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     UUID NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_verification_tokens_user ON verification_tokens(user_id);
