package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum NotificationErrorCode implements ErrorCode {

	NOTIFICATION_PLATFORM_INVALID(400, "지원하지 않는 기기 플랫폼입니다."),
	NOTIFICATION_HOUR_INVALID(400, "알림 시각은 0에서 23 사이여야 합니다."),
	NOTIFICATION_HOUR_NOT_ALLOWED(400, "8시·11시·17시만 설정할 수 있습니다."),
	NOTIFICATION_DELIVERY_FAILED(502, "알림 발송에 실패했습니다."),
	;

	private final int status;
	private final String message;

	NotificationErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public int status() {
		return status;
	}
}
