CREATE TABLE notification_dispatches (
    id VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL
);
