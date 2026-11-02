package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Embeddable
public record NotificationSlot(@Column(name = "scheduled_at", nullable = false, updatable = false) Instant scheduledAt) {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final Duration MAX_DELAY = Duration.ofHours(1);

	public static NotificationSlot of(LocalDate date, NotificationTime time) {
		return new NotificationSlot(date.atTime(time.hour(), time.minute()).atZone(SEOUL).toInstant());
	}

	public NotificationTime time() {
		LocalTime local = scheduledAt.atZone(SEOUL).toLocalTime();
		return new NotificationTime(local.getHour(), local.getMinute());
	}

	public void validateDispatchable(Instant now) {
		if (scheduledAt.isAfter(now) || scheduledAt.isBefore(now.minus(MAX_DELAY))) {
			throw new InvalidNotificationDispatchTimeException(scheduledAt);
		}
	}
}
