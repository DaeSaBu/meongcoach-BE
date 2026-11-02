package com.daesabu.meongcoach.notification.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "scheduled_notification_dispatches",
		uniqueConstraints = @UniqueConstraint(name = "uk_scheduled_notification_dispatches_scheduled_at", columnNames = "scheduled_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduledNotificationDispatch extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Embedded
	private NotificationSlot slot;

	public static ScheduledNotificationDispatch of(LocalDate date, int hour, int minute, Instant now) {
		NotificationSlot slot = NotificationSlot.of(date, new NotificationTime(hour, minute));
		slot.validateDispatchable(now);
		ScheduledNotificationDispatch dispatch = new ScheduledNotificationDispatch();
		dispatch.slot = slot;
		return dispatch;
	}

	public Instant scheduled() {
		return slot.scheduledAt();
	}

	public NotificationTime time() {
		return slot.time();
	}

	public PushNotification toPushNotification(String fid) {
		return slot.toPushNotification(fid);
	}
}
