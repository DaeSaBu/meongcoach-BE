ALTER TABLE support_conversations
    ADD COLUMN deleted_at TIMESTAMP;

DROP INDEX uk_support_conversations_active_member;

CREATE UNIQUE INDEX uk_support_conversations_active_member
    ON support_conversations (member_id) WHERE closed_at IS NULL AND deleted_at IS NULL;
