-- Schema cho database bank (Level 1)
-- Cách dùng: psql -U postgres -d bank -f schema.sql
-- (Tạo database trước: CREATE DATABASE bank;)

CREATE TABLE IF NOT EXISTS customer (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255),
    location    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS account (
    id                  BIGSERIAL PRIMARY KEY,
    account_number      VARCHAR(50) NOT NULL UNIQUE,
    balance             NUMERIC(19,2) NOT NULL,
    customer_id         BIGINT NOT NULL REFERENCES customer(id),
    transaction_limit   NUMERIC(19,2) NOT NULL,
    opened_date         DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT NOT NULL REFERENCES account(id),
    type        VARCHAR(20) NOT NULL,   -- DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
    amount      NUMERIC(19,2) NOT NULL,
    fee         NUMERIC(19,2) NOT NULL,
    location    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_account_customer_id ON account(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);
