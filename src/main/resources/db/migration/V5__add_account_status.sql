ALTER TABLE accounts
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_accounts_status ON accounts (status);
