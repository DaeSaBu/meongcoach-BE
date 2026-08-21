package com.daesabu.meongcoach.shared.exception;

/**
 * 모든 도메인 예외의 최상위 타입. 직접 인스턴스화하지 않고 각 모듈의 구체 예외가 상속한다.
 */
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

	// 외부 연동 어댑터가 인프라 예외를 도메인 예외로 번역할 때 원인을 보존한다
	protected DomainException(ErrorCode errorCode, String detail, Throwable cause) {
		super(detail, cause);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
