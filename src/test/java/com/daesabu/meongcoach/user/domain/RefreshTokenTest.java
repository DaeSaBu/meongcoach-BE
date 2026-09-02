package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

	private static final String TOKEN_ID = "0f8fad5b-d9cb-469f-a165-70867728950e";
	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 9, 16, 12, 0);

	@Test
	void 발급하면_회원과_jti와_만료_시각이_담기고_무효화되지_않은_상태다() {
		User user = User.registerOnboardingMember();

		RefreshToken token = RefreshToken.issue(user, TOKEN_ID, EXPIRES_AT);

		assertThat(token.getUser()).isEqualTo(user);
		assertThat(token.getTokenId()).isEqualTo(TOKEN_ID);
		assertThat(token.getExpiresAt()).isEqualTo(EXPIRES_AT);
		assertThat(token.getRevokedAt()).isNull();
	}

	@Test
	void 무효화하면_revokedAt이_기록된다() {
		RefreshToken token = RefreshToken.issue(User.registerOnboardingMember(), TOKEN_ID, EXPIRES_AT);

		token.revoke();

		assertThat(token.getRevokedAt()).isNotNull();
	}

	@Test
	void 이미_무효화된_토큰을_다시_무효화해도_처음_시각이_유지된다() {
		RefreshToken token = RefreshToken.issue(User.registerOnboardingMember(), TOKEN_ID, EXPIRES_AT);
		token.revoke();
		LocalDateTime firstRevokedAt = token.getRevokedAt();

		token.revoke();

		assertThat(token.getRevokedAt()).isEqualTo(firstRevokedAt);
	}

	@Test
	void 무효화된_토큰은_사용할_수_없다() {
		RefreshToken token = RefreshToken.issue(User.registerOnboardingMember(), TOKEN_ID, EXPIRES_AT);
		token.revoke();

		boolean usable = token.isUsable(EXPIRES_AT.minusDays(1));

		assertThat(usable).isFalse();
	}

	@Test
	void 만료_시각이_지난_토큰은_사용할_수_없다() {
		RefreshToken token = RefreshToken.issue(User.registerOnboardingMember(), TOKEN_ID, EXPIRES_AT);

		boolean usable = token.isUsable(EXPIRES_AT.plusSeconds(1));

		assertThat(usable).isFalse();
	}

	@Test
	void 만료_전이고_무효화되지_않은_토큰은_사용할_수_있다() {
		RefreshToken token = RefreshToken.issue(User.registerOnboardingMember(), TOKEN_ID, EXPIRES_AT);

		boolean usable = token.isUsable(EXPIRES_AT.minusDays(1));

		assertThat(usable).isTrue();
	}
}
