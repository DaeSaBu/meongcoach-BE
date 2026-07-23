package com.daesabu.meongcoach.shared.exception;

/**
 * 모듈별 에러 코드 enum이 구현하는 계약. 도메인 계층이 구현하므로 Spring 타입(HttpStatus 등)에 의존하지 않는다.
 */
public interface ErrorCode {

	/**
	 * 클라이언트 분기용 코드. {모듈}_{원인} 형식의 UPPER_SNAKE_CASE. (예: USER_DUPLICATE_EMAIL)
	 */
	String code();

	/**
	 * 기본 에러 메시지. Problem Details 응답의 detail로 사용된다.
	 */
	String message();

	/**
	 * HTTP 상태 코드 숫자. (예: 404)
	 */
	int status();
}
