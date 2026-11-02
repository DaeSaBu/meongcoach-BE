package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;
import java.time.Instant;

public class InvalidNotificationDispatchTimeException extends DomainException {

	public InvalidNotificationDispatchTimeException(Instant scheduledAt) {
		super(NotificationErrorCode.NOTIFICATION_DISPATCH_TIME_INVALID, "발송할 수 없는 예약 시각입니다: " + scheduledAt);
	}
}
