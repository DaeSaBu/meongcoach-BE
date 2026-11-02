ALTER TABLE "notification_preferences" RENAME TO "notification_schedules";
ALTER TABLE "notification_schedules" RENAME CONSTRAINT "ck_notification_preferences_hour" TO "ck_notification_schedules_hour";
ALTER TABLE "notification_schedules" RENAME CONSTRAINT "uk_notification_preferences_user_id_hour" TO "uk_notification_schedules_user_id_hour";
ALTER INDEX "idx_notification_preferences_hour_enabled" RENAME TO "idx_notification_schedules_hour_enabled";
