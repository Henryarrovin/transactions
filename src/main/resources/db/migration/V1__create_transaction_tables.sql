-- Wallets

CREATE TABLE IF NOT EXISTS wallets (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    VARCHAR(255) NOT NULL UNIQUE,
    balance    BIGINT       NOT NULL DEFAULT 0,
    currency   VARCHAR(10)  NOT NULL DEFAULT 'INR',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Transactions

CREATE TYPE transaction_type AS ENUM ('payment', 'refund', 'transfer');
CREATE TYPE transaction_status AS ENUM ('pending', 'completed', 'failed', 'reversed');

CREATE TABLE IF NOT EXISTS transactions (
    id                  UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             VARCHAR(255)       NOT NULL,
    type                transaction_type   NOT NULL,
    status              transaction_status NOT NULL DEFAULT 'pending',
    amount              BIGINT             NOT NULL,
    currency            VARCHAR(10)        NOT NULL DEFAULT 'INR',
    provider_order_id   VARCHAR(255),
    provider_payment_id VARCHAR(255),
    provider_refund_id  VARCHAR(255),
    reference_id        VARCHAR(255),
    description         TEXT,
    metadata            JSONB,
    created_at          TIMESTAMP          NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP          NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user_id             ON transactions(user_id);
CREATE INDEX idx_transactions_provider_payment_id ON transactions(provider_payment_id);
CREATE INDEX idx_transactions_provider_order_id   ON transactions(provider_order_id);
CREATE INDEX idx_transactions_reference_id        ON transactions(reference_id);
CREATE INDEX idx_transactions_created_at          ON transactions(created_at);

-- ── Ledger Entries ────────────────────────────────────────────────────────────

CREATE TYPE entry_type AS ENUM ('debit', 'credit');

CREATE TABLE IF NOT EXISTS ledger_entries (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID         NOT NULL REFERENCES transactions(id),
    user_id        VARCHAR(255) NOT NULL,
    type           entry_type   NOT NULL,
    amount         BIGINT       NOT NULL,
    currency       VARCHAR(10)  NOT NULL DEFAULT 'INR',
    balance        BIGINT       NOT NULL,
    description    TEXT         NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_user_id        ON ledger_entries(user_id);
CREATE INDEX idx_ledger_created_at     ON ledger_entries(created_at);

-- ── Statements ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS statements (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           VARCHAR(255) NOT NULL,
    month             INTEGER      NOT NULL,
    year              INTEGER      NOT NULL,
    opening_balance   BIGINT       NOT NULL DEFAULT 0,
    closing_balance   BIGINT       NOT NULL DEFAULT 0,
    total_credits     BIGINT       NOT NULL DEFAULT 0,
    total_debits      BIGINT       NOT NULL DEFAULT 0,
    transaction_count INTEGER      NOT NULL DEFAULT 0,
    generated_at      TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, month, year)
);

CREATE INDEX idx_statements_user_id ON statements(user_id);

-- ── Reconciliation Records ────────────────────────────────────────────────────

CREATE TYPE reconciliation_status AS ENUM ('matched', 'mismatched', 'missing');

CREATE TABLE IF NOT EXISTS reconciliation_records (
    id                  UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_payment_id VARCHAR(255)          NOT NULL,
    provider_order_id   VARCHAR(255),
    expected_amount     BIGINT                NOT NULL,
    actual_amount       BIGINT                NOT NULL,
    status              reconciliation_status NOT NULL,
    notes               TEXT,
    reconciled_at       TIMESTAMP             NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP             NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reconciliation_provider_payment_id ON reconciliation_records(provider_payment_id);
