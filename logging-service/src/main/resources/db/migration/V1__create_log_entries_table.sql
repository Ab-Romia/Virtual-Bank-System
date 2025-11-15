CREATE TABLE log_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message TEXT NOT NULL,
    message_type VARCHAR(255) NOT NULL,
    date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    source_service VARCHAR(255),
    source_endpoint VARCHAR(255),
    app_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_log_entries_message_type ON log_entries(message_type);
CREATE INDEX idx_log_entries_date_time ON log_entries(date_time);
CREATE INDEX idx_log_entries_source_service ON log_entries(source_service);
CREATE INDEX idx_log_entries_created_at ON log_entries(created_at);
