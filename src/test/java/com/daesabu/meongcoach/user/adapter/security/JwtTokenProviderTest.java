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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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

class JwtTokenProviderTest {

	private static final String ISSUER = "meongcoach";
	private static final String SECRET = "meongcoach-test-only-jwt-secret-key-32b";
	private static final String OTHER_SECRET = "another-service-jwt-secret-key-32bytes!";
	private static final Long USER_ID = 42L;

	@Test
	void 발급하면_액세스_토큰과_리프레시_토큰이_각자의_용도로_만들어진다() {
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
	void 발급한_리프레시_토큰의_jti와_만료_시각을_함께_돌려준다() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));

		AuthToken token = provider.issue(USER_ID);

		Jwt refresh = decoder(SECRET, TokenType.REFRESH).decode(token.refreshToken());
		LocalDateTime expiresAt = LocalDateTime.ofInstant(refresh.getExpiresAt(), ZoneId.systemDefault());
		assertThat(token.refreshTokenId().value()).isEqualTo(refresh.getId());
		// JWT exp는 초 단위라 발급 시각의 나노초를 버리고 비교한다
		assertThat(token.refreshTokenExpiresAt().truncatedTo(ChronoUnit.SECONDS)).isEqualTo(expiresAt);
	}

	@Test
	void 리프레시_토큰에서_jti를_꺼낸다() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		AuthToken token = provider.issue(USER_ID);

		assertThat(provider.extractTokenId(token.refreshToken())).isEqualTo(token.refreshTokenId());
	}

	@Test
	void 액세스_토큰을_리프레시_토큰_자리에_제출하면_실패한다() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		AuthToken token = provider.issue(USER_ID);

		assertThatThrownBy(() -> provider.extractTokenId(token.accessToken()))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 다른_키로_서명된_토큰은_거부된다() {
		AuthToken forged = tokenProvider(OTHER_SECRET, Duration.ofHours(1), Duration.ofDays(14)).issue(USER_ID);
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));

		assertThatThrownBy(() -> provider.extractTokenId(forged.refreshToken()))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 만료된_리프레시_토큰은_거부된다() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		String expired = expiredRefreshToken();

		assertThatThrownBy(() -> provider.extractTokenId(expired))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 검증_실패_응답에_토큰_값이_노출되지_않는다() {
		JwtTokenProvider provider = tokenProvider(SECRET, Duration.ofHours(1), Duration.ofDays(14));
		String tampered = provider.issue(USER_ID).refreshToken() + "tampered";

		assertThatThrownBy(() -> provider.extractTokenId(tampered))
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
