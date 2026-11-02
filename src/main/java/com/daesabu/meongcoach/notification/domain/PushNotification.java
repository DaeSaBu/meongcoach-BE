package com.daesabu.meongcoach.notification.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PushNotification(String fid, String title, String body, Map<String, String> data, String collapseKey) {

	public static PushNotification ofEvent(String fid, NotificationEventType type, String title, String body, NotificationDeepLink deepLink) {
		Map<String, String> data = new HashMap<>();
		data.put("type", type.name());
		if (deepLink.value() != null) {
			data.put("deepLink", deepLink.value());
		}
		return new PushNotification(fid, title, body, Map.copyOf(data), UUID.randomUUID().toString());
	}
}
