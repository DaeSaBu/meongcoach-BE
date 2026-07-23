package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RefreshTokenTest {

	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 8, 1, 0, 0);

	@Test
	void issueCreatesActiveToken() {
		RefreshToken token = RefreshToken.issue(1L, "hashed-token", EXPIRES_AT);

		assertThat(token.getUserId()).isEqualTo(1L);
		assertThat(token.getTokenHash()).isEqualTo("hashed-token");
		assertThat(token.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);
		assertThat(token.getExpiresAt()).isEqualTo(EXPIRES_AT);
	}

	@Test
	void revokeChangesStatusToRevoked() {
		RefreshToken token = RefreshToken.issue(1L, "hashed-token", EXPIRES_AT);

		token.revoke();

		assertThat(token.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);
	}

	@Test
	void isExpiredReturnsTrueWhenNowIsAfterExpiry() {
		RefreshToken token = RefreshToken.issue(1L, "hashed-token", EXPIRES_AT);

		assertThat(token.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
	}

	@Test
	void isExpiredReturnsFalseWhenNowIsBeforeExpiry() {
		RefreshToken token = RefreshToken.issue(1L, "hashed-token", EXPIRES_AT);

		assertThat(token.isExpired(EXPIRES_AT.minusSeconds(1))).isFalse();
	}
}
