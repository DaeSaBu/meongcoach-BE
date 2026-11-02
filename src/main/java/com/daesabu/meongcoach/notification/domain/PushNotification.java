package com.daesabu.meongcoach.notification.domain;

public record PushNotification(
		String fid,
		String title,
		String body,
		NotificationEventType type,
		Integer hour,
		NotificationDeepLink deepLink,
		String collapseKey
) {
}
