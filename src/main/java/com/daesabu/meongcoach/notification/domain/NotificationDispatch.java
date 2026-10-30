package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification_dispatches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDispatch {
	@Id
	@Column(length = 64)
	private String id;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public static NotificationDispatch complete(String key, Instant now) {
		NotificationDispatch dispatch = new NotificationDispatch();
		dispatch.id = requireNonNull(key);
		dispatch.createdAt = requireNonNull(now);
		return dispatch;
	}
}
