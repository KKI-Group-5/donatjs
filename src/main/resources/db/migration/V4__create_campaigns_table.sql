-- V4: Persist campaign management data.
-- Campaigns were previously stored only in JVM memory, which loses data on
-- Cloud Run restarts and across scaled instances. This table gives campaign
-- creation, moderation, donations, and deadline automation durable state.

CREATE TABLE IF NOT EXISTS campaigns (
    id                   BIGSERIAL PRIMARY KEY,
    title                VARCHAR(255) NOT NULL,
    description          TEXT NOT NULL,
    deadline             DATE,
    target_amount        NUMERIC(19, 2) NOT NULL,
    total_raised         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status               VARCHAR(32) NOT NULL,
    creator_id           VARCHAR(255),
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    near_target_notified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_campaigns_status ON campaigns(status);
CREATE INDEX IF NOT EXISTS idx_campaigns_creator_id ON campaigns(creator_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_deadline_status ON campaigns(deadline, status);
