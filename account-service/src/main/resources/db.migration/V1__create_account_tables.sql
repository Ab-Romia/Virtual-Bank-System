CREATE DATABASE virtual_bank_account_service;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Account table
CREATE TABLE accounts (
                          account_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          user_id UUID NOT NULL,
                          account_number VARCHAR(10) UNIQUE NOT NULL,
                          account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('SAVINGS', 'CHECKING', 'MONEY_MARKET', 'CERTIFICATE')),
                          balance DECIMAL(18, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
                          status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'FROZEN')),
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          last_transaction_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Transaction history table (for scheduled job to check last activity)
CREATE TABLE account_transactions (
                                      transaction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      from_account_id UUID REFERENCES accounts(account_id),
                                      to_account_id UUID REFERENCES accounts(account_id),
                                      amount DECIMAL(18, 2) NOT NULL,
                                      transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
                                      status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      description TEXT
);

-- Indexes
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_last_transaction ON accounts(last_transaction_at);
CREATE INDEX idx_transactions_from_account ON account_transactions(from_account_id);
CREATE INDEX idx_transactions_to_account ON account_transactions(to_account_id);
CREATE INDEX idx_transactions_created_at ON account_transactions(created_at);

-- Update timestamp trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Views
CREATE VIEW active_accounts AS
SELECT
    account_id,
    user_id,
    account_number,
    account_type,
    balance,
    status,
    created_at,
    updated_at,
    last_transaction_at
FROM accounts
WHERE status = 'ACTIVE';

-- Function to generate unique account number
CREATE OR REPLACE FUNCTION generate_account_number()
    RETURNS VARCHAR(10) AS $$
DECLARE
new_account_number VARCHAR(10);
    is_unique BOOLEAN := FALSE;
BEGIN
    WHILE NOT is_unique LOOP
        new_account_number := LPAD(FLOOR(RANDOM() * 10000000000)::TEXT, 10, '0');

SELECT COUNT(*) = 0 INTO is_unique
FROM accounts
WHERE account_number = new_account_number;
END LOOP;

RETURN new_account_number;
END;
$$ LANGUAGE plpgsql;

-- Function to find stale accounts
CREATE OR REPLACE FUNCTION find_stale_accounts(hours_threshold INT)
    RETURNS TABLE (account_id UUID) AS $$
BEGIN
RETURN QUERY
SELECT a.account_id
FROM accounts a
WHERE a.status = 'ACTIVE'
  AND a.last_transaction_at < (CURRENT_TIMESTAMP - (hours_threshold || ' hours')::INTERVAL);
END;
$$ LANGUAGE plpgsql;

-- Sample data for testing
INSERT INTO accounts (account_id, user_id, account_number, account_type, balance, status, created_at, updated_at)
VALUES
    (uuid_generate_v4(), 'c969754c-22a3-4242-b10f-1430946ee6e7', '1234567890', 'SAVINGS', 100.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'c969754c-22a3-4242-b10f-1430946ee6e7', '0987654321', 'CHECKING', 500.50, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);