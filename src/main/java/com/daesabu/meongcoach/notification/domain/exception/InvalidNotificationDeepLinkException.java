package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationDeepLinkException extends DomainException {

	public InvalidNotificationDeepLinkException() {
		super(NotificationErrorCode.NOTIFICATION_DEEP_LINK_INVALID);
	}
}
