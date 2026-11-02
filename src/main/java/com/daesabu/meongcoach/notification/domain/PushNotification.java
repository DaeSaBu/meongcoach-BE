package com.daesabu.meongcoach.notification.domain;

import java.util.HashMap;
import java.util.Map;

public record PushNotification(
		String fid,
		String title,
		String body,
		NotificationEventType type,
		Integer hour,
		NotificationDeepLink deepLink,
		String collapseKey
) {

	public Map<String, String> data() {
		Map<String, String> data = new HashMap<>();
		if (type != null) {
			data.put("type", type.name());
		}
		if (hour != null) {
			data.put("hour", String.valueOf(hour));
		}
		if (deepLink.value() != null) {
			data.put("deepLink", deepLink.value());
		}
		return Map.copyOf(data);
	}
}
