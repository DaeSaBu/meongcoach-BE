package com.daesabu.meongcoach.notification.domain.shared;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationSlotException;
import java.util.Locale;

public enum NotificationSlot {
	MORNING(false),
	NOON(false),
	EVENING(true),
	;

	private final boolean defaultEnabled;

	NotificationSlot(boolean defaultEnabled) {
		this.defaultEnabled = defaultEnabled;
	}

	public static NotificationSlot from(String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidNotificationSlotException(value);
		}
		try {
			return valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new InvalidNotificationSlotException(value);
		}
	}

	public boolean isDefaultEnabled() {
		return defaultEnabled;
	}
}
