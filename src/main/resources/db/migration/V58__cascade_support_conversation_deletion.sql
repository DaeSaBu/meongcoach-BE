ALTER TABLE support_messages
    DROP CONSTRAINT support_messages_conversation_id_fkey,
    ADD CONSTRAINT support_messages_conversation_id_fkey
        FOREIGN KEY (conversation_id) REFERENCES support_conversations(id) ON DELETE CASCADE;
