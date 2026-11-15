INSERT INTO notification_event_settings (user_id, type, enabled, created_at, updated_at)
SELECT DISTINCT user_id, 'LIKE', TRUE, LOCALTIMESTAMP, LOCALTIMESTAMP
FROM device_installations
ON CONFLICT (user_id, type) DO NOTHING;
