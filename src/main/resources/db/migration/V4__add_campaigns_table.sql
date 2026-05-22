-- V4: Add campaigns table for JPA-managed campaign persistence.

CREATE TABLE IF NOT EXISTS campaigns (
    id                   BIGSERIAL PRIMARY KEY,
    title                VARCHAR(255)               NOT NULL,
    description          TEXT                       NOT NULL,
    deadline             DATE,
    target_amount        NUMERIC(19, 2),
    total_raised         NUMERIC(19, 2)              DEFAULT 0,
    status               VARCHAR(50)                NOT NULL DEFAULT 'WAITING',
    creator_id           VARCHAR(255),
    created_at           TIMESTAMP WITHOUT TIME ZONE,
    near_target_notified BOOLEAN                    NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_campaigns_status     ON campaigns(status);
CREATE INDEX IF NOT EXISTS idx_campaigns_creator_id ON campaigns(creator_id);
