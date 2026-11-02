package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationHourException;
import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationMinuteException;
import com.daesabu.meongcoach.notification.domain.exception.NotAllowedNotificationHourException;
import java.util.List;
import java.util.Set;

public record NotificationTime(int hour, int minute) {

	private static final int MAX_HOUR = 23;
	private static final int MAX_MINUTE = 59;

	public static final List<NotificationTime> ALLOWED = List.of(
			new NotificationTime(8, 0), new NotificationTime(11, 0), new NotificationTime(17, 0));

	private static final Set<NotificationTime> DEFAULT_ENABLED = Set.of(new NotificationTime(17, 0));

	public NotificationTime {
		if (hour < 0 || hour > MAX_HOUR) {
			throw new InvalidNotificationHourException(hour);
		}
		if (minute < 0 || minute > MAX_MINUTE) {
			throw new InvalidNotificationMinuteException(minute);
		}
	}

	public void validateAllowed() {
		if (!ALLOWED.contains(this)) {
			throw new NotAllowedNotificationHourException(hour, minute);
		}
	}

	public boolean isDefaultEnabled() {
		return DEFAULT_ENABLED.contains(this);
	}
}
