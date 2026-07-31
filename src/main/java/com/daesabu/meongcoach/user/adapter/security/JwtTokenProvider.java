package com.daesabu.meongcoach.user.adapter.security;

import com.daesabu.meongcoach.shared.security.JwtProperties;
import com.daesabu.meongcoach.shared.security.TokenType;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * 자체 JWT 발급·검증 어댑터. 향후 거부 목록을 붙일 수 있도록 토큰마다 고유한 jti를 넣는다.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

	private final JwtEncoder encoder;
	private final JwtDecoder refreshTokenDecoder;
	private final JwtProperties properties;

	public JwtTokenProvider(JwtEncoder encoder, @Qualifier("refreshTokenDecoder") JwtDecoder refreshTokenDecoder,
	                        JwtProperties properties) {
		this.encoder = encoder;
		this.refreshTokenDecoder = refreshTokenDecoder;
		this.properties = properties;
	}

	@Override
	public AuthToken issue(Long userId) {
		Instant issuedAt = Instant.now();
		return new AuthToken(
				encode(userId, issuedAt, TokenType.ACCESS, properties.accessTokenValidity()),
				encode(userId, issuedAt, TokenType.REFRESH, properties.refreshTokenValidity())
		);
	}

	@Override
	public Long extractUserId(String refreshToken) {
		try {
			return Long.valueOf(refreshTokenDecoder.decode(refreshToken).getSubject());
		} catch (JwtException | NumberFormatException e) {
			// 예외 detail은 응답에 노출되므로 토큰 값이나 원인 메시지를 담지 않는다
			throw new InvalidRefreshTokenException();
		}
	}

	private String encode(Long userId, Instant issuedAt, TokenType tokenType, Duration validity) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(String.valueOf(userId))
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(validity))
				.id(UUID.randomUUID().toString())
				.claim(TokenType.CLAIM_NAME, tokenType.claimValue())
				.build();
		return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
}
