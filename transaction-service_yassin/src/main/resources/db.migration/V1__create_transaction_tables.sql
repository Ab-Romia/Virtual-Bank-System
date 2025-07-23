 -- CREATE DATABASE virtual_bank_transaction_service;
 CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
 -- Create transaction_status enum type
 CREATE TYPE transaction_status AS ENUM ('INITIATED', 'SUCCESS', 'FAILED');

 -- Create transaction_type enum type
 CREATE TYPE transaction_type AS ENUM ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL');

 -- Create transactions table
 CREATE TABLE transactions (
                               transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               from_account_id UUID,
                               to_account_id UUID,
                               amount DECIMAL(19, 2) NOT NULL,
                               description VARCHAR(255),
                               status transaction_status NOT NULL,
                               transaction_type transaction_type NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
 );

 -- Create index on from_account_id for faster queries
 CREATE INDEX idx_transactions_from_account_id ON transactions(from_account_id);

 -- Create index on to_account_id for faster queries
 CREATE INDEX idx_transactions_to_account_id ON transactions(to_account_id);

 -- Create a function to update the updated_at timestamp
 CREATE OR REPLACE FUNCTION update_updated_at_column()
     RETURNS TRIGGER AS $$
 BEGIN
     NEW.updated_at = CURRENT_TIMESTAMP;
     RETURN NEW;
 END;
 $$ LANGUAGE 'plpgsql';

-- Create a trigger to automatically update updated_at
 CREATE TRIGGER update_transactions_updated_at
     BEFORE UPDATE ON transactions
     FOR EACH ROW
 EXECUTE FUNCTION update_updated_at_column();

 -- Create a view for retrieving all transactions related to an account
 CREATE OR REPLACE VIEW account_transactions AS
 SELECT
     transaction_id,
     from_account_id AS account_id,
     -amount AS amount,  -- Negative for outgoing transactions
     description,
     status,
     transaction_type,
     created_at,
     updated_at
 FROM
     transactions
 WHERE
     from_account_id IS NOT NULL
 UNION ALL
 SELECT
     transaction_id,
     to_account_id AS account_id,
     amount,  -- Positive for incoming transactions
     description,
     status,
     transaction_type,
     created_at,
    updated_at
 FROM
     transactions
 WHERE
    to_account_id IS NOT NULL;
-- Drop the current table
 DROP TABLE IF EXISTS transactions CASCADE;

 -- Create transactions table without enum types
 CREATE TABLE transactions (
                               transaction_id UUID PRIMARY KEY,
                               from_account_id UUID,
                               to_account_id UUID,
                               amount DECIMAL(19, 2) NOT NULL,
                               description VARCHAR(255),
                               status VARCHAR(50) NOT NULL,
                               transaction_type VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
 );