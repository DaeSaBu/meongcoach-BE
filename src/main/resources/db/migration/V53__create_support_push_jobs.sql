CREATE TABLE support_push_jobs (
 message_id BIGINT PRIMARY KEY REFERENCES support_messages(id),
 user_id BIGINT NOT NULL REFERENCES users(id),
 state VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (state IN ('PENDING','PROCESSING','SENT','DEAD')),
 attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 8),
 available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 lease_token UUID,
 lease_until TIMESTAMPTZ,
 last_error VARCHAR(200),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 finished_at TIMESTAMPTZ,
 CHECK ((state='PROCESSING') = (lease_token IS NOT NULL AND lease_until IS NOT NULL))
);
CREATE INDEX support_push_jobs_pending ON support_push_jobs(available_at,message_id) WHERE state='PENDING';
CREATE INDEX support_push_jobs_expired ON support_push_jobs(lease_until,message_id) WHERE state='PROCESSING';
