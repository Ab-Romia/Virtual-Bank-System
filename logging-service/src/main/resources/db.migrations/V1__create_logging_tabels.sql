
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS log_entries (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   message TEXT NOT NULL,  -- Stores the escaped JSON request/response
   message_type VARCHAR(50) NOT NULL, -- 'Request' or 'Response'
   date_time TIMESTAMP WITH TIME ZONE NOT NULL,
   source_service VARCHAR(100), -- Name of the service that generated the log
   source_endpoint VARCHAR(255), -- Endpoint that generated the log
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   app_name VARCHAR(50) -- 'PORTAL' or 'MOBILE'
);


-- Create index on message_type for filtering queries
CREATE INDEX idx_log_entries_message_type ON log_entries(message_type);

-- Create index on date_time for time-based queries
CREATE INDEX idx_log_entries_date_time ON log_entries(date_time);

-- Create index on source_service for service-based filtering
CREATE INDEX idx_log_entries_source_service ON log_entries(source_service);

-- Create a view for simplified querying of recent logs
CREATE OR REPLACE VIEW recent_logs AS
SELECT id, message_type, source_service, source_endpoint, date_time
FROM log_entries
ORDER BY date_time DESC
LIMIT 1000;

-- Grant privileges (assuming you'll use the same postgres user)
GRANT ALL PRIVILEGES ON DATABASE virtual_bank_logging_service TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;