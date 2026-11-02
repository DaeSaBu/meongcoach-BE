package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public record NotificationSlot(LocalDate date, NotificationTime time) {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final Duration MAX_DELAY = Duration.ofHours(1);

	public Instant scheduledAt() {
		return date.atTime(time.hour(), time.minute()).atZone(SEOUL).toInstant();
	}

	public void validateDispatchable(Instant now) {
		Instant scheduledAt = scheduledAt();
		if (scheduledAt.isAfter(now) || scheduledAt.isBefore(now.minus(MAX_DELAY))) {
			throw new InvalidNotificationDispatchTimeException(scheduledAt);
		}
	}
}
