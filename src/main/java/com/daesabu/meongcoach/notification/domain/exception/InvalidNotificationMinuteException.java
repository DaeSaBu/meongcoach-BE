package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationMinuteException extends DomainException {

	public InvalidNotificationMinuteException(int minute) {
		super(NotificationErrorCode.NOTIFICATION_MINUTE_INVALID, "알림 분은 0에서 59 사이여야 합니다: " + minute);
	}
}
