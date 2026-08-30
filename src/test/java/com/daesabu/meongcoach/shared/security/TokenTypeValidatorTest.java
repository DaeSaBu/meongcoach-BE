package com.daesabu.meongcoach.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenTypeValidatorTest {

	@Test
	void 기대한_용도의_토큰은_검증을_통과한다() {
		Jwt jwt = jwtWithClaims(Map.of(TokenType.CLAIM_NAME, TokenType.ACCESS.claimValue()));

		assertThat(new TokenTypeValidator(TokenType.ACCESS).validate(jwt).hasErrors()).isFalse();
	}

	@Test
	void 리프레시_토큰은_액세스_토큰_자리에서_거부된다() {
		Jwt jwt = jwtWithClaims(Map.of(TokenType.CLAIM_NAME, TokenType.REFRESH.claimValue()));

		assertThat(new TokenTypeValidator(TokenType.ACCESS).validate(jwt).hasErrors()).isTrue();
	}

	@Test
	void 용도_클레임이_없으면_거부된다() {
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
