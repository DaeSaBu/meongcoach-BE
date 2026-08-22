package com.daesabu.meongcoach.user.adapter.integration;

import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.daesabu.meongcoach.user.domain.exception.SocialTokenAppMismatchException;
import java.util.Collections;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestOperations;

/**
 * 앱이 제공자 SDK로 받은 OIDC id_token을 서버가 검증하는 공통 로직. 제공자별 리더가 하나씩 들고 쓴다.
 * 서명·발급자·만료는 JWKS 기반 디코더가, "우리 앱에 발급된 토큰인지"는 aud 대조가 담당한다.
 * 공개 키는 디코더가 캐시하므로 로그인마다 제공자를 호출하지 않는다.
 * 클레임을 회원 정보로 옮기는 일은 제공자별 차이가 있을 수 있어 리더에 남기고, 여기서는 검증된 토큰만 돌려준다.
 */
final class OidcIdTokenVerifier {

	private final JwtDecoder decoder;
	private final List<String> audiences;

	OidcIdTokenVerifier(OidcProviderProperties properties, RestOperations restOperations) {
		this.decoder = buildDecoder(properties, restOperations);
		this.audiences = List.copyOf(properties.audiences());
	}

	Jwt verify(String credential) {
		Jwt idToken = decode(credential);

		// 서명이 유효해도 다른 앱에 발급된 토큰이면 해당 제공자 사용자로 로그인할 수 있으므로 반드시 대조한다
		List<String> tokenAudiences = idToken.getAudience();
		if (tokenAudiences == null || Collections.disjoint(tokenAudiences, audiences)) {
			throw new SocialTokenAppMismatchException();
		}

		if (idToken.getSubject() == null) {
			throw new InvalidSocialTokenException();
		}
		return idToken;
	}

	private Jwt decode(String credential) {
		try {
			return decoder.decode(credential);
		} catch (BadJwtException e) {
			// 형식·서명·발급자·만료 위반은 토큰 자체가 잘못된 것이다
			throw new InvalidSocialTokenException();
		} catch (JwtException e) {
			// 공개 키를 가져오지 못한 경우다. 토큰 무효와 구분해야 클라이언트가 재시도할 수 있다
			throw new SocialProviderUnavailableException();
		}
	}

	private static JwtDecoder buildDecoder(OidcProviderProperties properties, RestOperations restOperations) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
				.jwsAlgorithm(SignatureAlgorithm.RS256)
				.restOperations(restOperations)
				.build();
		// aud는 예외 코드를 따로 구분해야 해서 검증기에 넣지 않고 verify()에서 대조한다
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(properties.issuer())
		));
		return decoder;
	}
}
