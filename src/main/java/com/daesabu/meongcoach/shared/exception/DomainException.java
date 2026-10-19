package com.daesabu.meongcoach.shared.exception;

import java.util.Map;

public abstract class DomainException extends RuntimeException {

	private final ErrorCode errorCode;

	protected DomainException(ErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	protected DomainException(ErrorCode errorCode, String detail) {
		super(detail);
		this.errorCode = errorCode;
	}

	protected DomainException(ErrorCode errorCode, String detail, Throwable cause) {
		super(detail, cause);
		this.errorCode = errorCode;
	}

	public Map<String, Object> getProperties() {
		return Map.of();
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
