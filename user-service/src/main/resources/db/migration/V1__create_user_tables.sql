CREATE TABLE users (
                       user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE user_sessions (
                               session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                               session_token VARCHAR(255) UNIQUE NOT NULL,
                               expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token ON user_sessions(session_token);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

CREATE TABLE user_login_attempts (
                                     attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     username VARCHAR(50) NOT NULL,
                                     ip_address INET,
                                     user_agent TEXT,
                                     success BOOLEAN NOT NULL,
                                     attempted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     failure_reason VARCHAR(255)
);

CREATE INDEX idx_login_attempts_username ON user_login_attempts(username);
CREATE INDEX idx_login_attempts_attempted_at ON user_login_attempts(attempted_at);
CREATE INDEX idx_login_attempts_ip_address ON user_login_attempts(ip_address);

CREATE TABLE user_profile_audit (
                                    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                    field_name VARCHAR(100) NOT NULL,
                                    old_value TEXT,
                                    new_value TEXT,
                                    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    changed_by VARCHAR(100)
);

CREATE INDEX idx_user_profile_audit_user_id ON user_profile_audit(user_id);
CREATE INDEX idx_user_profile_audit_changed_at ON user_profile_audit(changed_at);

INSERT INTO users (username, email, password_hash, first_name, last_name) VALUES
    ('john.doe', 'john.doe@example.com', '$2a$10$N.kmUiGKvK8kAFmNJPHEoOjOCBIgVkgWKQGcwFWjf1nzJcD8bNlv6', 'John', 'Doe');

CREATE VIEW active_users AS
SELECT
    user_id,
    username,
    email,
    first_name,
    last_name,
    created_at
FROM users
WHERE is_active = TRUE;

CREATE OR REPLACE FUNCTION get_user_by_credential(credential VARCHAR)
    RETURNS TABLE (
                      user_id UUID,
                      username VARCHAR(50),
                      email VARCHAR(255),
                      password_hash VARCHAR(255),
                      first_name VARCHAR(100),
                      last_name VARCHAR(100),
                      created_at TIMESTAMP WITH TIME ZONE,
                      is_active BOOLEAN
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT
            u.user_id,
            u.username,
            u.email,
            u.password_hash,
            u.first_name,
            u.last_name,
            u.created_at,
            u.is_active
        FROM users u
        WHERE u.username = credential OR u.email = credential;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION check_user_exists(check_username VARCHAR, check_email VARCHAR)
    RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM users
        WHERE username = check_username OR email = check_email
    );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
    RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_sessions
    WHERE expires_at < CURRENT_TIMESTAMP;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
