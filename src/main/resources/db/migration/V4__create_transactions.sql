CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    moment DATETIME(6) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    source_account_number INT,
    target_account_number INT,
    account_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

CREATE INDEX idx_transactions_account ON transactions (account_id);
CREATE INDEX idx_transactions_type ON transactions (type);
CREATE INDEX idx_transactions_moment ON transactions (moment);
