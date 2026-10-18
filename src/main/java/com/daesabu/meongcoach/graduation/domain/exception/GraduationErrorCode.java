package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum GraduationErrorCode implements ErrorCode {

	GRADUATION_TOPIC_NOT_COMPLETED(403, "토픽의 모든 커리큘럼을 완료해야 실습 인증을 할 수 있습니다."),
	GRADUATION_ALREADY_APPROVED(409, "이미 수료한 토픽입니다."),
	GRADUATION_SUBMISSION_CONFLICT(409, "같은 토픽의 실습 인증 제출이 동시에 처리되었습니다. 다시 시도해 주세요."),
	GRADUATION_INVALID_VIDEO_KEY(400, "실습 인증 영상의 객체 키가 올바르지 않습니다.");

	private final int status;
	private final String message;

	GraduationErrorCode(int status, String message) {
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
