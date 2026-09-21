package com.daesabu.meongcoach.auth.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청. 제공자는 문자열로 받아 도메인(SocialProvider.from)이 변환한다 — enum으로 받으면 대소문자를 구분하고
 * 잘못된 값이 우리 에러 코드 없이 역직렬화 오류로 끝난다.
 */
public record SocialLoginRequest(
		@NotBlank String socialProvider,
		@NotBlank String idToken
) {
}
