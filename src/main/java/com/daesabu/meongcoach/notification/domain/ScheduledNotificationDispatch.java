package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.notification.domain.exception.InvalidNotificationDispatchTimeException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "scheduled_notification_dispatches",
		uniqueConstraints = @UniqueConstraint(name = "uk_scheduled_notification_dispatches_scheduled_at", columnNames = "scheduled_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduledNotificationDispatch extends BaseEntity {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final Duration MAX_DELAY = Duration.ofHours(1);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Getter(AccessLevel.NONE)
	@Column(name = "scheduled_at", nullable = false, updatable = false)
	private Instant scheduledAt;

	public static ScheduledNotificationDispatch of(ScheduledNotificationDispatchCommand command, Instant now) {
		NotificationTime time = new NotificationTime(command.hour(), command.minute());
		Instant scheduledAt = command.date().atTime(time.hour(), time.minute()).atZone(SEOUL).toInstant();
		if (scheduledAt.isAfter(now) || scheduledAt.isBefore(now.minus(MAX_DELAY))) {
			throw new InvalidNotificationDispatchTimeException(scheduledAt);
		}
		ScheduledNotificationDispatch dispatch = new ScheduledNotificationDispatch();
		dispatch.scheduledAt = scheduledAt;
		return dispatch;
	}

	public Instant scheduled() {
		return scheduledAt;
	}

	public NotificationTime time() {
		LocalTime local = scheduledAt.atZone(SEOUL).toLocalTime();
		return new NotificationTime(local.getHour(), local.getMinute());
	}
}
