package com.daesabu.meongcoach.user.adapter.security;

import com.daesabu.meongcoach.shared.security.JwtProperties;
import com.daesabu.meongcoach.shared.security.TokenType;
import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * 자체 JWT 발급·검증 어댑터. 리프레시 토큰에만 고유한 jti를 넣어 저장 키로 쓴다.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

	private static final String EMAIL_CLAIM = "email";

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
	public AuthToken issue(Long userId, String email) {
		Instant issuedAt = Instant.now();
		RefreshTokenId refreshTokenId = RefreshTokenId.generate();
		Instant refreshExpiresAt = issuedAt.plus(properties.refreshTokenValidity());
		JwtClaimsSet.Builder accessClaimsBuilder = claims(userId, issuedAt, TokenType.ACCESS,
				properties.accessTokenValidity());
		// email은 클라이언트가 분석 도구에 넘기는 값이라 수명이 1시간인 액세스 토큰에만 싣는다.
		// 14일을 사는 리프레시 토큰에 넣으면 얻는 것 없이 개인정보 노출 창만 길어진다
		addEmail(accessClaimsBuilder, email);
		JwtClaimsSet accessClaims = accessClaimsBuilder.build();
		// jti는 저장 이력 조회 키라 리프레시 토큰에만 넣는다. 액세스 토큰은 어디에도 저장·조회하지 않는다
		JwtClaimsSet refreshClaims = claims(userId, issuedAt, TokenType.REFRESH, properties.refreshTokenValidity())
				.id(refreshTokenId.value())
				.build();
		String accessToken = encode(accessClaims);
		String refreshToken = encode(refreshClaims);
		// 엔티티의 시각 컬럼이 시스템 존 LocalDateTime이므로 같은 존으로 변환한다
		LocalDateTime refreshTokenExpiresAt = LocalDateTime.ofInstant(refreshExpiresAt, ZoneId.systemDefault());
		return new AuthToken(accessToken, refreshToken, refreshTokenId, refreshTokenExpiresAt);
	}

	@Override
	public RefreshTokenId extractTokenId(String refreshToken) {
		String tokenId = decodeTokenId(refreshToken);
		// 형식이 깨진 jti는 값 객체 생성에서 같은 예외로 거부된다
		return new RefreshTokenId(tokenId);
	}

	private String decodeTokenId(String refreshToken) {
		try {
			return refreshTokenDecoder.decode(refreshToken).getId();
		} catch (JwtException e) {
			// 예외 detail은 응답에 노출되므로 토큰 값이나 원인 메시지를 담지 않는다
			throw new InvalidRefreshTokenException();
		}
	}

	private JwtClaimsSet.Builder claims(Long userId, Instant issuedAt, TokenType tokenType, Duration validity) {
		return JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(String.valueOf(userId))
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(validity))
				.claim(TokenType.CLAIM_NAME, tokenType.claimValue());
	}

	private void addEmail(JwtClaimsSet.Builder claims, String email) {
		if (email == null || email.isBlank()) {
			return;
		}
		claims.claim(EMAIL_CLAIM, email);
	}

	private String encode(JwtClaimsSet claims) {
		return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
}
