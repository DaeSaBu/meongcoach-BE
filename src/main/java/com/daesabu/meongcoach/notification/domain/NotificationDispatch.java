package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

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
@Table(name = "notification_dispatches",
		uniqueConstraints = @UniqueConstraint(name = "uk_notification_dispatches_dispatch_key", columnNames = "dispatch_key"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDispatch {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "dispatch_key", nullable = false, updatable = false, length = 64)
	private String dispatchKey;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public static NotificationDispatch complete(String key, Instant now) {
		NotificationDispatch dispatch = new NotificationDispatch();
		dispatch.dispatchKey = requireNonNull(key);
		dispatch.createdAt = requireNonNull(now);
		return dispatch;
	}
}
