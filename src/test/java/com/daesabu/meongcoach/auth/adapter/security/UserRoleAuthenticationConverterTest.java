package com.daesabu.meongcoach.auth.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class UserRoleAuthenticationConverterTest {

	private static final Long USER_ID = 42L;

	@ParameterizedTest
	@CsvSource({
			"USER, ROLE_USER",
			"ONBOARDING_USER, ROLE_ONBOARDING_USER",
			"ADMIN, ROLE_ADMIN",
			"EXPERT, ROLE_EXPERT",
			"TRAINER, ROLE_TRAINER"
	})
	void 회원_역할에_해당하는_권한을_부여한다(AuthorityRole role, String authority) {
		AbstractAuthenticationToken authentication = convert(role, String.valueOf(USER_ID));

		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly(authority);
	}

	@Test
	void 인증_주체_이름은_토큰의_sub를_그대로_쓴다() {
		AbstractAuthenticationToken authentication = convert(AuthorityRole.USER, String.valueOf(USER_ID));

		assertThat(authentication.getName()).isEqualTo(String.valueOf(USER_ID));
	}

	@Test
	void 등록되지_않은_회원의_토큰은_인증_예외로_거부한다() {
		assertThatThrownBy(() -> convert(null, String.valueOf(USER_ID)))
				.isInstanceOf(InvalidBearerTokenException.class)
				.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void sub가_회원_ID_형식이_아니면_인증_예외로_거부한다() {
		assertThatThrownBy(() -> convert(AuthorityRole.USER, "not-a-user-id"))
				.isInstanceOf(InvalidBearerTokenException.class);
	}

	@Test
	void 거부_사유에_회원_ID를_담지_않는다() {
		assertThatThrownBy(() -> convert(null, String.valueOf(USER_ID)))
				.hasMessageNotContaining(String.valueOf(USER_ID));
	}

	private AbstractAuthenticationToken convert(AuthorityRole role, String subject) {
		return new UserRoleAuthenticationConverter(checkerWithRole(role)).convert(jwtWithSubject(subject));
	}

	private RegisteredUserChecker checkerWithRole(AuthorityRole role) {
		return new RegisteredUserChecker() {
			@Override
			public Optional<AuthorityRole> findRole(Long userId) {
				return Optional.ofNullable(role);
			}
		};
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
