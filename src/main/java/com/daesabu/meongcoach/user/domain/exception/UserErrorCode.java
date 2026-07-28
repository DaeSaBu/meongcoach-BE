package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

	USER_INVALID_EMAIL(400, "이메일 형식이 올바르지 않습니다."),
	USER_INVALID_MBTI(400, "MBTI 값이 올바르지 않습니다."),
	USER_NOT_FOUND(404, "회원을 찾을 수 없습니다."),
	USER_ALREADY_ONBOARDED(409, "이미 온보딩을 완료한 회원입니다."),
	USER_UNSUPPORTED_SOCIAL_PROVIDER(400, "지원하지 않는 소셜 로그인 제공자입니다."),
	USER_INVALID_SOCIAL_TOKEN(401, "소셜 로그인 토큰이 유효하지 않습니다."),
	USER_SOCIAL_TOKEN_APP_MISMATCH(401, "다른 애플리케이션에서 발급된 소셜 로그인 토큰입니다."),
	USER_INVALID_REFRESH_TOKEN(401, "리프레시 토큰이 유효하지 않습니다."),
	USER_WITHDRAWN(403, "탈퇴한 회원입니다."),
	USER_SOCIAL_PROVIDER_UNAVAILABLE(502, "소셜 로그인 제공자와 통신할 수 없습니다."),
	;

	private final int status;
	private final String message;

	UserErrorCode(int status, String message) {
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
