package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class NotificationDeliveryException extends DomainException {
	public NotificationDeliveryException(Throwable cause) {
		super(NotificationErrorCode.NOTIFICATION_DELIVERY_FAILED,
				NotificationErrorCode.NOTIFICATION_DELIVERY_FAILED.message(), cause);
	}
}
