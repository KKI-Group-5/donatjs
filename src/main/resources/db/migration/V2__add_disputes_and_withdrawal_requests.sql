-- V2: Add disputes and withdrawal_requests tables introduced by the donation management module.

CREATE TABLE IF NOT EXISTS disputes (
    id          UUID        DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    user_id     UUID        NOT NULL,
    reason      VARCHAR(1000) NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    admin_notes VARCHAR(1000),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_disputes_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS withdrawal_requests (
    id           BIGSERIAL PRIMARY KEY,
    donation_id  BIGINT        NOT NULL,
    user_id      VARCHAR(255)  NOT NULL,
    status       VARCHAR(50)   NOT NULL,
    reason       TEXT,
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_withdrawal_donation ON withdrawal_requests(donation_id);
CREATE INDEX IF NOT EXISTS idx_withdrawal_user     ON withdrawal_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_disputes_user       ON disputes(user_id);
