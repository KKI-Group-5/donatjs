-- V1: Baseline schema — documents the full DB state when Flyway was introduced.
-- This migration is NOT run on the existing Supabase DB because
-- flyway.baseline-on-migrate=true baselines it at this version.
-- New environments (staging, CI with Postgres) start here.

CREATE TABLE IF NOT EXISTS users (
    id              UUID    DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255),
    name            VARCHAR(255),
    bio             VARCHAR(255),
    date_of_birth   DATE,
    rejected_donation_count  INTEGER NOT NULL DEFAULT 0,
    rejected_campaign_count  INTEGER NOT NULL DEFAULT 0,
    is_suspended             BOOLEAN NOT NULL DEFAULT false,
    flagged_for_review       BOOLEAN NOT NULL DEFAULT false,
    fraud_activity_count     INTEGER NOT NULL DEFAULT 0,
    flagged                  BOOLEAN NOT NULL DEFAULT false,
    suspended                BOOLEAN NOT NULL DEFAULT false,
    admin                    BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS wallets (
    id      VARCHAR(255) NOT NULL PRIMARY KEY,
    balance DOUBLE PRECISION,
    user_id VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS transactions (
    id          VARCHAR(255) NOT NULL PRIMARY KEY,
    amount      DOUBLE PRECISION,
    description VARCHAR(255),
    timestamp   TIMESTAMP WITHOUT TIME ZONE,
    type        VARCHAR(255),
    wallet_id   VARCHAR(255) NOT NULL,
    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE TABLE IF NOT EXISTS donations (
    id             BIGSERIAL PRIMARY KEY,
    amount         BIGINT,
    campaign_id    BIGINT,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    fee            BIGINT,
    notes          TEXT,
    payment_method VARCHAR(255),
    status         VARCHAR(255),
    total_amount   BIGINT,
    type           VARCHAR(255),
    updated_at     TIMESTAMP WITHOUT TIME ZONE,
    user_id        VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS saved_campaigns (
    id                  BIGSERIAL PRIMARY KEY,
    campaign_id         VARCHAR(255),
    campaign_image_url  VARCHAR(255),
    campaign_organizer  VARCHAR(255),
    campaign_title      VARCHAR(255) NOT NULL,
    saved_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    CONSTRAINT uk_saved_campaigns_user_campaign UNIQUE (user_id, campaign_id)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    amount          BIGINT,
    campaign_id     BIGINT,
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    frequency       VARCHAR(255),
    next_debit_date DATE,
    status          VARCHAR(255),
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    user_id         VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS admin_notifications (
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    message    TEXT NOT NULL,
    read       BOOLEAN NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_name  VARCHAR(255)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_donation_user              ON donations(user_id);
CREATE INDEX IF NOT EXISTS idx_donation_campaign          ON donations(campaign_id);
CREATE INDEX IF NOT EXISTS idx_transactions_wallet_id     ON transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id      ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_campaign_id  ON subscriptions(campaign_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status_next  ON subscriptions(status, next_debit_date);
