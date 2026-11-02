ALTER TABLE "notification_schedules" ADD COLUMN "minute" SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE "notification_schedules" ALTER COLUMN "minute" DROP DEFAULT;
ALTER TABLE "notification_schedules" ADD CONSTRAINT "ck_notification_schedules_minute" CHECK ("minute" BETWEEN 0 AND 59);

ALTER TABLE "notification_schedules" DROP CONSTRAINT "uk_notification_schedules_user_id_hour";
ALTER TABLE "notification_schedules" ADD CONSTRAINT "uk_notification_schedules_user_id_hour_minute" UNIQUE ("user_id", "hour", "minute");

DROP INDEX "idx_notification_schedules_hour_enabled";
CREATE INDEX "idx_notification_schedules_hour_minute_enabled" ON "notification_schedules" ("hour", "minute", "enabled");
