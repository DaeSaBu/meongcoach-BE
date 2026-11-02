package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
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

	@Column(name = "scheduled_at", nullable = false, updatable = false)
	private Instant scheduledAt;

	public static ScheduledNotificationDispatch complete(Instant scheduledAt) {
		ScheduledNotificationDispatch dispatch = new ScheduledNotificationDispatch();
		dispatch.scheduledAt = requireNonNull(scheduledAt);
		return dispatch;
	}
}
