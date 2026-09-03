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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * 자체 JWT 발급·검증 어댑터. 토큰마다 고유한 jti를 넣고, 리프레시 토큰의 jti는 저장 키로 쓰인다.
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
		RefreshTokenId refreshTokenId = RefreshTokenId.generate();
		Instant refreshExpiresAt = issuedAt.plus(properties.refreshTokenValidity());
		String accessToken = encode(userId, issuedAt, TokenType.ACCESS, properties.accessTokenValidity(), accessTokenId());
		String refreshToken = encode(userId, issuedAt, TokenType.REFRESH, properties.refreshTokenValidity(),
				refreshTokenId.value());
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

	// 액세스 토큰의 jti는 저장하지 않으므로 값 객체 없이 문자열로만 만든다
	private String accessTokenId() {
		return UUID.randomUUID().toString();
	}

	private String encode(Long userId, Instant issuedAt, TokenType tokenType, Duration validity, String tokenId) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(String.valueOf(userId))
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(validity))
				.id(tokenId)
				.claim(TokenType.CLAIM_NAME, tokenType.claimValue())
				.build();
		return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}
}
