package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidDevicePlatformException extends DomainException {

	public InvalidDevicePlatformException(String platform) {
		super(NotificationErrorCode.NOTIFICATION_PLATFORM_INVALID, "지원하지 않는 기기 플랫폼입니다: " + platform);
	}
}
