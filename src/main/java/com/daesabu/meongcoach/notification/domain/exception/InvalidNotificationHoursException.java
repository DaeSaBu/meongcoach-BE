package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationHoursException extends DomainException {

	public InvalidNotificationHoursException() {
		super(NotificationErrorCode.NOTIFICATION_HOURS_INVALID);
	}
}
