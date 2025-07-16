-- CREATE DATABASE virtual_bank_user_service;
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE TABLE users (
--                        user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--                        username VARCHAR(50) UNIQUE NOT NULL,
--                        email VARCHAR(255) UNIQUE NOT NULL,
--                        password_hash VARCHAR(255) NOT NULL,
--                        first_name VARCHAR(100) NOT NULL,
--                        last_name VARCHAR(100) NOT NULL,
--                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
--                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
--                        is_active BOOLEAN DEFAULT TRUE
-- );

-- CREATE INDEX idx_users_username ON users(username);
-- CREATE INDEX idx_users_email ON users(email);
-- CREATE INDEX idx_users_created_at ON users(created_at);

-- CREATE OR REPLACE FUNCTION update_updated_at_column()
--     RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.updated_at = CURRENT_TIMESTAMP;
--     RETURN NEW;
-- END;
-- $$ language 'plpgsql';
--
-- CREATE TRIGGER update_users_updated_at
--     BEFORE UPDATE ON users
--     FOR EACH ROW
-- EXECUTE FUNCTION update_updated_at_column();

-- CREATE TABLE user_sessions (
--                                session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--                                user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
--                                session_token VARCHAR(255) UNIQUE NOT NULL,
--                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
--                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
--                                is_active BOOLEAN DEFAULT TRUE
-- );

-- CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
-- CREATE INDEX idx_user_sessions_token ON user_sessions(session_token);
-- CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

