ALTER TABLE support_messages
    ADD COLUMN media_type VARCHAR(20),
    ADD COLUMN media_object_key VARCHAR(512),
    DROP CONSTRAINT support_messages_body_check,
    ADD CONSTRAINT support_messages_content_check
        CHECK (length(trim(body)) > 0 OR media_object_key IS NOT NULL),
    ADD CONSTRAINT support_messages_attachment_check
        CHECK ((media_type IS NULL AND media_object_key IS NULL)
            OR (media_type IS NOT NULL AND media_type IN ('IMAGE', 'VIDEO')
                AND media_object_key IS NOT NULL AND length(trim(media_object_key)) > 0));
