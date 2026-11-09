CREATE TABLE "support_reply_receipts" (
    "message_id" VARCHAR(200) PRIMARY KEY,
    "received_at" TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO "notification_event_settings" ("user_id", "type", "enabled", "created_at", "updated_at")
SELECT DISTINCT d."user_id", 'SUPPORT_REPLY', TRUE, NOW(), NOW()
FROM "device_installations" d
ON CONFLICT ("user_id", "type") DO NOTHING;
