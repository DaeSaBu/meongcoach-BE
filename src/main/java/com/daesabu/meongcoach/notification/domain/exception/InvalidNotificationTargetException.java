package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationTargetException extends DomainException {

	public InvalidNotificationTargetException() {
		super(NotificationErrorCode.NOTIFICATION_TARGET_INVALID);
	}
}
