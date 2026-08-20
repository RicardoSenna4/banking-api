CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_number INT NOT NULL,
    client_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    withdraw_fee DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_client FOREIGN KEY (client_id) REFERENCES clients (id)
);

CREATE INDEX idx_accounts_client ON accounts (client_id);
CREATE INDEX idx_accounts_type ON accounts (type);
