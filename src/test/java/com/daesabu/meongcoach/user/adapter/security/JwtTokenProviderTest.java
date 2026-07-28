package com.daesabu.meongcoach.user.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.shared.security.JwtProperties;
import com.daesabu.meongcoach.shared.security.TokenType;
import com.daesabu.meongcoach.shared.security.TokenTypeValidator;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@DisplayName("JWT 토큰 발급 어댑터")
class JwtTokenProviderTest {

	private static final String ISSUER = "meongcoach";
	private static final String SECRET = "meongcoach-test-only-jwt-secret-key-32b";
	private static final String OTHER_SECRET = "another-service-jwt-secret-key-32bytes!";
	private static final Long USER_ID = 42L;

	@Test
	@DisplayName("발급하면 액세스·리프레시 토큰이 각자의 용도로 만들어진다")
	void issueCreatesAccessAndRefreshTokens() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));

		AuthToken token = provider.issue(USER_ID);

		Jwt access = decoder(SECRET, TokenType.ACCESS).decode(token.accessToken());
		Jwt refresh = decoder(SECRET, TokenType.REFRESH).decode(token.refreshToken());
		assertThat(access.getSubject()).isEqualTo(String.valueOf(USER_ID));
		assertThat(access.getClaimAsString("iss")).isEqualTo(ISSUER);
		assertThat(access.getClaimAsString(TokenType.CLAIM_NAME)).isEqualTo("access");
		assertThat(refresh.getClaimAsString(TokenType.CLAIM_NAME)).isEqualTo("refresh");
		assertThat(access.getId()).isNotEqualTo(refresh.getId());
	}

	@Test
	@DisplayName("리프레시 토큰에서 회원 식별자를 꺼낸다")
	void extractUserIdReturnsSubject() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		AuthToken token = provider.issue(USER_ID);

		assertThat(provider.extractUserId(token.refreshToken())).isEqualTo(USER_ID);
	}

	@Test
	@DisplayName("액세스 토큰을 리프레시 토큰 자리에 제출하면 실패한다")
	void extractUserIdFailsWhenAccessTokenIsSubmitted() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		AuthToken token = provider.issue(USER_ID);

		assertThatThrownBy(() -> provider.extractUserId(token.accessToken()))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("다른 키로 서명된 토큰은 거부된다")
	void extractUserIdFailsWhenSignedWithAnotherKey() {
		AuthToken forged = tokenProvider(OTHER_SECRET, Duration.ofHours(1), Duration.ofDays(14)).issue(USER_ID);
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));

		assertThatThrownBy(() -> provider.extractUserId(forged.refreshToken()))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("만료된 리프레시 토큰은 거부된다")
	void extractUserIdFailsWhenTokenIsExpired() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		String expired = expiredRefreshToken();

		assertThatThrownBy(() -> provider.extractUserId(expired))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	@DisplayName("검증 실패 응답에 토큰 값이 노출되지 않는다")
	void extractUserIdFailureHidesTokenValue() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		String tampered = provider.issue(USER_ID).refreshToken() + "tampered";

		assertThatThrownBy(() -> provider.extractUserId(tampered))
				.isInstanceOf(InvalidRefreshTokenException.class)
				.hasMessageNotContaining(tampered);
	}

	// Jwt는 expiresAt이 issuedAt보다 앞서면 인코딩 자체를 거부하므로, 과거 시각으로 직접 만든다
	private String expiredRefreshToken() {
		JwtProperties properties = new JwtProperties(ISSUER, SECRET, Duration.ofHours(1), Duration.ofDays(14));
		Instant issuedAt = Instant.now().minus(Duration.ofDays(15));
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(String.valueOf(USER_ID))
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(Duration.ofMinutes(1)))
				.claim(TokenType.CLAIM_NAME, TokenType.REFRESH.claimValue())
				.build();
		return NimbusJwtEncoder.withSecretKey(properties.secretKey()).build()
				.encode(JwtEncoderParameters.from(claims))
				.getTokenValue();
	}

	private JwtTokenProvider tokenProvider(String secret, Duration accessValidity, Duration refreshValidity) {
		JwtProperties properties = new JwtProperties(ISSUER, secret, accessValidity, refreshValidity);
		JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(properties.secretKey()).build();
		return new JwtTokenProvider(encoder, decoder(secret, TokenType.REFRESH), properties);
	}

	private JwtDecoder decoder(String secret, TokenType tokenType) {
		SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(ISSUER),
				new TokenTypeValidator(tokenType)
		));
		return decoder;
	}
}
