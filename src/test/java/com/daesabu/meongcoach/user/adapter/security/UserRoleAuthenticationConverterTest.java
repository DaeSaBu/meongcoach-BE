package com.daesabu.meongcoach.user.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

@DisplayName("회원 역할 부여 컨버터")
class UserRoleAuthenticationConverterTest {

	private static final Long USER_ID = 42L;

	@Test
	@DisplayName("정회원 토큰에는 ROLE_MEMBER 권한을 부여한다")
	void grantsMemberRoleAuthority() {
		AbstractAuthenticationToken authentication = convert(AuthorityRole.MEMBER, String.valueOf(USER_ID));

		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_MEMBER");
	}

	@Test
	@DisplayName("온보딩 회원 토큰에는 ROLE_ONBOARDING_MEMBER 권한을 부여한다")
	void grantsOnboardingMemberRoleAuthority() {
		AbstractAuthenticationToken authentication = convert(AuthorityRole.ONBOARDING_MEMBER, String.valueOf(USER_ID));

		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_ONBOARDING_MEMBER");
	}

	// CurrentUserIdArgumentResolver가 인증 주체 이름을 회원 ID로 해석하므로 sub가 유지되어야 한다
	@Test
	@DisplayName("인증 주체 이름은 토큰의 sub를 그대로 쓴다")
	void keepsSubjectAsAuthenticationName() {
		AbstractAuthenticationToken authentication = convert(AuthorityRole.MEMBER, String.valueOf(USER_ID));

		assertThat(authentication.getName()).isEqualTo(String.valueOf(USER_ID));
	}

	// AuthenticationException 계열이 아니면 401 대신 500이 되므로 예외 타입 자체가 회귀 가드다
	@Test
	@DisplayName("등록되지 않은 회원의 토큰은 인증 예외로 거부한다")
	void rejectsTokenOfUnregisteredUser() {
		assertThatThrownBy(() -> convert(null, String.valueOf(USER_ID)))
				.isInstanceOf(InvalidBearerTokenException.class)
				.isInstanceOf(AuthenticationException.class);
	}

	@Test
	@DisplayName("sub가 회원 ID 형식이 아니면 인증 예외로 거부한다")
	void rejectsTokenWithNonNumericSubject() {
		assertThatThrownBy(() -> convert(AuthorityRole.MEMBER, "not-a-user-id"))
				.isInstanceOf(InvalidBearerTokenException.class);
	}

	// 인증 실패 응답에 실리므로 회원 ID가 존재하는지 알려주는 단서를 담으면 안 된다
	@Test
	@DisplayName("거부 사유에 회원 ID를 담지 않는다")
	void rejectionMessageHidesUserId() {
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
