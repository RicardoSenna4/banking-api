ALTER TABLE clients ADD COLUMN user_id BIGINT NULL;
ALTER TABLE clients ADD CONSTRAINT fk_clients_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE clients ADD CONSTRAINT uq_clients_user UNIQUE (user_id);
CREATE INDEX idx_clients_user ON clients (user_id);
