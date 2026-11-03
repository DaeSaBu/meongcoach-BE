ALTER TABLE "notification_dispatches" RENAME TO "scheduled_notification_dispatches";
ALTER TABLE "scheduled_notification_dispatches" ADD COLUMN "scheduled_at" TIMESTAMPTZ;

UPDATE "scheduled_notification_dispatches"
SET "scheduled_at" = date_trunc('minute', regexp_replace("dispatch_key", ':[0-9]+$', '')::TIMESTAMPTZ);

DELETE FROM "scheduled_notification_dispatches" a
USING "scheduled_notification_dispatches" b
WHERE a."scheduled_at" = b."scheduled_at" AND a."id" > b."id";

ALTER TABLE "scheduled_notification_dispatches" ALTER COLUMN "scheduled_at" SET NOT NULL;
ALTER TABLE "scheduled_notification_dispatches" DROP COLUMN "dispatch_key";
ALTER TABLE "scheduled_notification_dispatches"
    ADD CONSTRAINT "uk_scheduled_notification_dispatches_scheduled_at" UNIQUE ("scheduled_at");
