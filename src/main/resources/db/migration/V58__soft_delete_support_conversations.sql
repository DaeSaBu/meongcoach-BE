ALTER TABLE support_conversations
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN deleted_at TIMESTAMPTZ;

UPDATE support_conversations c
SET created_at = LEAST(c.updated_at,
    COALESCE((SELECT MIN(m.created_at) FROM support_messages m WHERE m.conversation_id = c.id), c.updated_at));

ALTER TABLE support_conversations
    ALTER COLUMN created_at SET NOT NULL,
    DROP CONSTRAINT uk_support_conversations_member;

CREATE UNIQUE INDEX uk_support_conversations_active_member
    ON support_conversations (member_id) WHERE deleted_at IS NULL;
