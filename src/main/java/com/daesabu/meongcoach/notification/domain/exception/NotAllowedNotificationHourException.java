package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class NotAllowedNotificationHourException extends DomainException {

	public NotAllowedNotificationHourException(int hour, int minute) {
		super(NotificationErrorCode.NOTIFICATION_HOUR_NOT_ALLOWED, "허용하지 않는 알림 시각입니다: %d:%02d".formatted(hour, minute));
	}
}
