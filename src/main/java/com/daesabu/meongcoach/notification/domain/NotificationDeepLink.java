package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDeepLinkException;

public record NotificationDeepLink(String value) {

	private static final String PREFIX = "meongcoach://";

	public NotificationDeepLink {
		if (value != null && !value.startsWith(PREFIX)) {
			throw new InvalidNotificationDeepLinkException();
		}
	}
}
