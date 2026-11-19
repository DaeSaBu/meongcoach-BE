UPDATE support_conversations c
SET closed_at = LOCALTIMESTAMP,
    updated_at = LOCALTIMESTAMP
WHERE c.closed_at IS NULL
  AND c.deleted_at IS NULL
  AND c.created_at < LOCALTIMESTAMP - INTERVAL '1 hour'
  AND NOT EXISTS (SELECT 1 FROM support_messages m WHERE m.conversation_id = c.id);
