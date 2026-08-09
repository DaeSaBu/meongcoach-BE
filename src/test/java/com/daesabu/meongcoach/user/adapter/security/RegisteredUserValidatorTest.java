package com.daesabu.meongcoach.user.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("회원 존재 검증기")
class RegisteredUserValidatorTest {

	private static final Long USER_ID = 42L;

	@Test
	@DisplayName("등록된 회원의 토큰은 통과시킨다")
	void acceptsTokenOfRegisteredUser() {
		OAuth2TokenValidatorResult result = validate(userId -> true, String.valueOf(USER_ID));

		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	@DisplayName("등록되지 않은 회원의 토큰은 거부한다")
	void rejectsTokenOfUnregisteredUser() {
		OAuth2TokenValidatorResult result = validate(userId -> false, String.valueOf(USER_ID));

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	@DisplayName("sub가 회원 ID 형식이 아니면 거부한다")
	void rejectsTokenWithNonNumericSubject() {
		OAuth2TokenValidatorResult result = validate(userId -> true, "not-a-user-id");

		assertThat(result.hasErrors()).isTrue();
	}

	// 인증 실패 응답에 실리므로 회원 ID가 존재하는지 알려주는 단서를 담으면 안 된다
	@Test
	@DisplayName("거부 사유에 회원 ID를 담지 않는다")
	void rejectionDescriptionHidesUserId() {
		OAuth2TokenValidatorResult result = validate(userId -> false, String.valueOf(USER_ID));

		assertThat(result.getErrors())
				.allSatisfy(error -> assertThat(error.getDescription()).doesNotContain(String.valueOf(USER_ID)));
	}

	private OAuth2TokenValidatorResult validate(RegisteredUserChecker checker, String subject) {
		return new RegisteredUserValidator(checker).validate(jwtWithSubject(subject));
	}

	private Jwt jwtWithSubject(String subject) {
		Instant issuedAt = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(subject)
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(1, ChronoUnit.HOURS))
				.build();
	}
}
