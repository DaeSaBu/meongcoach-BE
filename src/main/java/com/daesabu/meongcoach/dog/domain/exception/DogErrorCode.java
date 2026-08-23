package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum DogErrorCode implements ErrorCode {

	DOG_INVALID_BREED(400, "강아지 견종이 올바르지 않습니다."),
	DOG_INVALID_SEX(400, "강아지 성별이 올바르지 않습니다."),
	DOG_INVALID_PERSONALITY(400, "강아지 성격이 올바르지 않습니다."),
	DOG_NOT_FOUND(404, "강아지를 찾을 수 없습니다."),
	DOG_LAST_DOG_NOT_DELETABLE(409, "마지막 강아지는 삭제할 수 없습니다."),
	;

	private final int status;
	private final String message;

	DogErrorCode(int status, String message) {
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
