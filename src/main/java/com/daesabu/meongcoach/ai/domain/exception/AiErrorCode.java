package com.daesabu.meongcoach.ai.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum AiErrorCode implements ErrorCode {

	AI_REPORT_NOT_FOUND(404, "AI 리포트를 찾을 수 없습니다."),
	AI_REPORT_TRIAL_EXCEEDED(403, "AI 리포트 무료 체험 횟수를 모두 사용했습니다.");

	private final int status;
	private final String message;

	AiErrorCode(int status, String message) {
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
