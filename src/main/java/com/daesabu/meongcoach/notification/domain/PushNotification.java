package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PushNotification(String fid, String title, String body, Map<String, String> data, String collapseKey) {

	private static final String DEEP_LINK_PREFIX = "meongcoach://";

	public static List<PushNotification> ofEvent(List<String> fids, NotificationEventType type, String title, String body, String deepLink) {
		if (deepLink != null && !deepLink.startsWith(DEEP_LINK_PREFIX)) {
			throw new InvalidNotificationDeepLinkException();
		}
		Map<String, String> data = new HashMap<>();
		data.put("type", type.name());
		if (deepLink != null) {
			data.put("deepLink", deepLink);
		}
		Map<String, String> eventData = Map.copyOf(data);
		return fids.stream()
				.map(fid -> new PushNotification(fid, title, body, eventData, UUID.randomUUID().toString()))
				.toList();
	}
}
