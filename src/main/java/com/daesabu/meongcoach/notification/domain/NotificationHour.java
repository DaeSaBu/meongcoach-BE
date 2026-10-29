package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.notification.domain.exception.NotAllowedNotificationHourException;
import java.util.List;
import java.util.Map;

public final class NotificationHour {

	public static final int MIN = 0;
	public static final int MAX = 23;

	public static final List<Integer> ALLOWED_HOURS = List.of(8, 11, 17);

	private static final Map<Integer, Boolean> DEFAULT_ENABLED = Map.of(8, false, 11, false, 17, true);

	private NotificationHour() {
	}

	public static void validate(int hour) {
		if (hour < MIN || hour > MAX) {
			throw new InvalidNotificationHourException(hour);
		}
	}

	public static void validateAllowed(int hour) {
		if (!ALLOWED_HOURS.contains(hour)) {
			throw new NotAllowedNotificationHourException(hour);
		}
	}

	public static boolean isDefaultEnabled(int hour) {
		return DEFAULT_ENABLED.getOrDefault(hour, false);
	}
}
