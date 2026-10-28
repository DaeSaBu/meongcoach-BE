package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidNotificationSlotException extends DomainException {

	public InvalidNotificationSlotException(String slot) {
		super(NotificationErrorCode.NOTIFICATION_SLOT_INVALID, "알 수 없는 알림 시간대입니다: " + slot);
	}
}
