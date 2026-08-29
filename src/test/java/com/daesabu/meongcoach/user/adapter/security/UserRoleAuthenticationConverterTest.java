package com.daesabu.meongcoach.user.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class UserRoleAuthenticationConverterTest {

	private static final Long USER_ID = 42L;

	@Test
	void 정회원_토큰에는_ROLE_MEMBER_권한을_부여한다() {
		AbstractAuthenticationToken authentication = convert(AuthorityRole.MEMBER, String.valueOf(USER_ID));

		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_MEMBER");
	}

	@Test
	void 온보딩_회원_토큰에는_ROLE_ONBOARDING_MEMBER_권한을_부여한다() {
		AbstractAuthenticationToken authentication = convert(AuthorityRole.ONBOARDING_MEMBER, String.valueOf(USER_ID));

		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ONBOARDING_MEMBER");
	}

	// CurrentUserIdArgumentResolver가 인증 주체 이름을 회원 ID로 해석하므로 sub가 유지되어야 한다
	@Test
	void 인증_주체_이름은_토큰의_sub를_그대로_쓴다() {
		AbstractAuthenticationToken authentication = convert(AuthorityRole.MEMBER, String.valueOf(USER_ID));

		assertThat(authentication.getName()).isEqualTo(String.valueOf(USER_ID));
	}

	// AuthenticationException 계열이 아니면 401 대신 500이 되므로 예외 타입 자체가 회귀 가드다
	@Test
	void 등록되지_않은_회원의_토큰은_인증_예외로_거부한다() {
		assertThatThrownBy(() -> convert(null, String.valueOf(USER_ID)))
				.isInstanceOf(InvalidBearerTokenException.class)
				.isInstanceOf(AuthenticationException.class);
	}

	@Test
	void sub가_회원_ID_형식이_아니면_인증_예외로_거부한다() {
		assertThatThrownBy(() -> convert(AuthorityRole.MEMBER, "not-a-user-id"))
				.isInstanceOf(InvalidBearerTokenException.class);
	}

	// 인증 실패 응답에 실리므로 회원 ID가 존재하는지 알려주는 단서를 담으면 안 된다
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
			public boolean isRegistered(Long userId) {
				return role != null;
			}

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
