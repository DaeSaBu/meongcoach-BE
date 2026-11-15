ALTER TABLE support_conversations ADD COLUMN last_message_at TIMESTAMP DEFAULT LOCALTIMESTAMP NOT NULL;
UPDATE support_conversations SET last_message_at = updated_at;
DROP INDEX support_conversations_activity;
CREATE INDEX support_conversations_activity ON support_conversations (last_message_at DESC, id DESC);
