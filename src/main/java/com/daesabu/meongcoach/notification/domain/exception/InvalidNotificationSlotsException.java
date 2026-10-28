package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationSlotsException extends DomainException {

	public InvalidNotificationSlotsException() {
		super(NotificationErrorCode.NOTIFICATION_SLOTS_INVALID);
	}
}
