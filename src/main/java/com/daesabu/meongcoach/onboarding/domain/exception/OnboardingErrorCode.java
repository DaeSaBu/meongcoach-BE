package com.daesabu.meongcoach.onboarding.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum OnboardingErrorCode implements ErrorCode {

	ONBOARDING_INVALID_IMAGE_UPLOAD_TARGET(400, "이미지 업로드 대상이 올바르지 않습니다."),
	;

	private final int status;
	private final String message;

	OnboardingErrorCode(int status, String message) {
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
