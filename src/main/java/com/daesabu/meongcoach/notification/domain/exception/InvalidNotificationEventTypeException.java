package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationEventTypeException extends DomainException {

	public InvalidNotificationEventTypeException(String type) {
		super(NotificationErrorCode.NOTIFICATION_EVENT_TYPE_INVALID, "지원하지 않는 알림 종류입니다: " + type);
	}
}
