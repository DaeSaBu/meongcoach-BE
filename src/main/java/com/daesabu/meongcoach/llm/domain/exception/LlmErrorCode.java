package com.daesabu.meongcoach.llm.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum LlmErrorCode implements ErrorCode {

	AI_CONTENT_GENERATION_FAILED(502, "AI 콘텐츠 생성에 실패했습니다.");

	private final int status;
	private final String message;

	LlmErrorCode(int status, String message) {
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
