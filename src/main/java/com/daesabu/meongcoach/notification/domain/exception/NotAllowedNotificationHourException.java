package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class NotAllowedNotificationHourException extends DomainException {

	public NotAllowedNotificationHourException(int hour) {
		super(NotificationErrorCode.NOTIFICATION_HOUR_NOT_ALLOWED, "8시·11시·17시만 설정할 수 있습니다: " + hour);
	}
}
