package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationEventContent(String title, String body, String deepLink) {

	private static final String DEEP_LINK_PREFIX = "meongcoach://";

	public NotificationEventContent {
		if (deepLink != null && !deepLink.startsWith(DEEP_LINK_PREFIX)) {
			throw new InvalidNotificationDeepLinkException();
		}
	}

	public List<PushNotification> toPushNotifications(List<String> fids, NotificationEventType type) {
		return fids.stream().map(fid -> toPushNotification(fid, type)).toList();
	}

	private PushNotification toPushNotification(String fid, NotificationEventType type) {
		Map<String, String> data = new HashMap<>();
		data.put("type", type.name());
		if (deepLink != null) {
			data.put("deepLink", deepLink);
		}
		return new PushNotification(fid, title, body, Map.copyOf(data), UUID.randomUUID().toString());
	}
}
