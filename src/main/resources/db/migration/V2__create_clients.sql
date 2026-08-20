CREATE TABLE clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cpf VARCHAR(14) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT uq_clients_cpf UNIQUE (cpf)
);

CREATE INDEX idx_clients_cpf ON clients (cpf);
CREATE INDEX idx_clients_status ON clients (status);
