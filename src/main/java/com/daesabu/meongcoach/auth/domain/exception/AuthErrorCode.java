package com.daesabu.meongcoach.auth.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

/**
 * 상수 이름이 곧 클라이언트 분기용 에러 코드라, user 모듈에서 분리한 뒤에도 기존 USER_ 접두어를 유지한다.
 */
public enum AuthErrorCode implements ErrorCode {

	USER_INVALID_EMAIL(400, "이메일 형식이 올바르지 않습니다."),
	USER_UNSUPPORTED_SOCIAL_PROVIDER(400, "지원하지 않는 소셜 로그인 제공자입니다."),
	USER_INVALID_SOCIAL_TOKEN(401, "소셜 로그인 토큰이 유효하지 않습니다."),
	USER_SOCIAL_EMAIL_REQUIRED(400, "소셜 계정의 이메일 제공 동의가 필요합니다."),
	USER_SOCIAL_TOKEN_APP_MISMATCH(401, "다른 애플리케이션에서 발급된 소셜 로그인 토큰입니다."),
	USER_INVALID_REFRESH_TOKEN(401, "리프레시 토큰이 유효하지 않습니다."),
	USER_INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
	USER_WITHDRAWN(403, "탈퇴한 회원입니다."),
	USER_SOCIAL_PROVIDER_UNAVAILABLE(502, "소셜 로그인 제공자와 통신할 수 없습니다."),
	USER_APPLE_AUTHORIZATION_CODE_REQUIRED(400, "Apple 계정 탈퇴에는 Apple 인가 코드가 필요합니다."),
	USER_INVALID_APPLE_AUTHORIZATION_CODE(400, "Apple 인가 코드가 유효하지 않거나 만료되었습니다."),
	;

	private final int status;
	private final String message;

	AuthErrorCode(int status, String message) {
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
