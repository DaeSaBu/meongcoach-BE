package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationEventTypeException;
import java.util.Arrays;

public enum NotificationEventType {
	AI_ANALYSIS,
	OPERATOR_MESSAGE,
	COMMENT;

	public static NotificationEventType from(String value) {
		return Arrays.stream(values())
				.filter(type -> type.name().equals(value))
				.findFirst()
				.orElseThrow(() -> new InvalidNotificationEventTypeException(value));
	}
}
