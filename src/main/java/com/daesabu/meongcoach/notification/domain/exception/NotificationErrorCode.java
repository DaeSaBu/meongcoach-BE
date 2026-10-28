package com.daesabu.meongcoach.notification.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum NotificationErrorCode implements ErrorCode {

	NOTIFICATION_PLATFORM_INVALID(400, "지원하지 않는 기기 플랫폼입니다."),
	NOTIFICATION_SLOT_INVALID(400, "알 수 없는 알림 시간대입니다."),
	NOTIFICATION_SLOTS_INVALID(400, "아침·점심·저녁 알림 설정을 각각 한 번씩 보내야 합니다."),
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
