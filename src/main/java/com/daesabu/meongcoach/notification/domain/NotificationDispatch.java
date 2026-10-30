package com.daesabu.meongcoach.notification.domain;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private NotificationDispatchStatus status;

	@Version
	@Column(nullable = false)
	private Long version;

	public static NotificationDispatch start(String key, Instant now) {
		NotificationDispatch dispatch = new NotificationDispatch();
		dispatch.id = requireNonNull(key);
		dispatch.createdAt = requireNonNull(now);
		dispatch.status = NotificationDispatchStatus.RUNNING;
		return dispatch;
	}

	public boolean retry() {
		if (status != NotificationDispatchStatus.FAILED) {
			return false;
		}
		status = NotificationDispatchStatus.RUNNING;
		return true;
	}

	public void complete() {
		Assert.state(status == NotificationDispatchStatus.RUNNING, "진행 중인 발송만 완료할 수 있습니다.");
		status = NotificationDispatchStatus.COMPLETED;
	}

	public void fail() {
		Assert.state(status == NotificationDispatchStatus.RUNNING, "진행 중인 발송만 실패할 수 있습니다.");
		status = NotificationDispatchStatus.FAILED;
	}
}
