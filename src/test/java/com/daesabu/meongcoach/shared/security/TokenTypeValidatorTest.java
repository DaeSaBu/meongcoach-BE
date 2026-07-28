package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("토큰 용도 검증")
class TokenTypeValidatorTest {

	@Test
	@DisplayName("기대한 용도의 토큰은 검증을 통과한다")
	void validateSucceedsWhenTokenTypeMatches() {
		Jwt jwt = jwtWithClaims(Map.of(TokenType.CLAIM_NAME, TokenType.ACCESS.claimValue()));

		assertThat(new TokenTypeValidator(TokenType.ACCESS).validate(jwt).hasErrors()).isFalse();
	}

	@Test
	@DisplayName("리프레시 토큰은 액세스 토큰 자리에서 거부된다")
	void validateFailsWhenRefreshTokenIsUsedAsAccessToken() {
		Jwt jwt = jwtWithClaims(Map.of(TokenType.CLAIM_NAME, TokenType.REFRESH.claimValue()));

		assertThat(new TokenTypeValidator(TokenType.ACCESS).validate(jwt).hasErrors()).isTrue();
	}

	@Test
	@DisplayName("용도 클레임이 없으면 거부된다")
	void validateFailsWhenClaimIsMissing() {
		Jwt jwt = jwtWithClaims(Map.of("sub", "1"));

		assertThat(new TokenTypeValidator(TokenType.ACCESS).validate(jwt).hasErrors()).isTrue();
	}

	private Jwt jwtWithClaims(Map<String, Object> claims) {
		Jwt.Builder builder = Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60));
		claims.forEach(builder::claim);
		return builder.build();
	}
}
