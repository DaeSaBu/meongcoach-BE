package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationHourException extends DomainException {

	public InvalidNotificationHourException(int hour) {
		super(NotificationErrorCode.NOTIFICATION_HOUR_INVALID, "알림 시각은 0에서 23 사이여야 합니다: " + hour);
	}
}
