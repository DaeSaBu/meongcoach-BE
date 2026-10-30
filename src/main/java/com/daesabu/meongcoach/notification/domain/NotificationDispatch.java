package com.daesabu.meongcoach.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_dispatches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDispatch {
	@Id
	@Column(length = 64)
	private String id;

	@Column(nullable = false)
	private Instant createdAt;
}
