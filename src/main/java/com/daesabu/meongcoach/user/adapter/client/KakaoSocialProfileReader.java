package com.daesabu.meongcoach.user.adapter.client;

import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.daesabu.meongcoach.user.domain.exception.SocialTokenAppMismatchException;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

/**
 * 앱이 카카오 SDK로 받은 OIDC id_token을 서버가 검증한다.
 * 서명·발급자·만료는 JWKS 기반 디코더가, "우리 앱에 발급된 토큰인지"는 aud 대조가 담당한다.
 * 공개 키는 디코더가 캐시하므로 로그인마다 카카오를 호출하지 않는다.
 * 토큰은 로그인 시점에만 쓰고 저장하지 않으며, 이후 인가는 우리 JWT로만 한다.
 */
@Component
public class KakaoSocialProfileReader implements SocialProfileReader {

	private static final String EMAIL_CLAIM = "email";

	private final JwtDecoder decoder;
	private final List<String> audiences;

	// 생성자가 둘이라 주입 대상을 명시한다
	@Autowired
	public KakaoSocialProfileReader(KakaoProperties properties, RestTemplateBuilder restTemplateBuilder) {
		this(properties, restTemplateBuilder.build());
	}

	// JWKS 응답을 테스트에서 가로챌 수 있도록 RestOperations를 직접 받는 통로를 둔다
	KakaoSocialProfileReader(KakaoProperties properties, RestOperations restOperations) {
		this.decoder = buildDecoder(properties, restOperations);
		this.audiences = List.copyOf(properties.audiences());
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.KAKAO;
	}

	@Override
	public SocialAccountLinkCommand read(String credential) {
		Jwt idToken = decode(credential);

		// 서명이 유효해도 다른 앱에 발급된 토큰이면 해당 카카오 사용자로 로그인할 수 있으므로 반드시 대조한다
		List<String> tokenAudiences = idToken.getAudience();
		if (tokenAudiences == null || Collections.disjoint(tokenAudiences, audiences)) {
			throw new SocialTokenAppMismatchException();
		}

		String providerId = idToken.getSubject();
		if (providerId == null) {
			throw new InvalidSocialTokenException();
		}

		return new SocialAccountLinkCommand(SocialProvider.KAKAO, providerId, idToken.getClaimAsString(EMAIL_CLAIM));
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

	private static JwtDecoder buildDecoder(KakaoProperties properties, RestOperations restOperations) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
				.jwsAlgorithm(SignatureAlgorithm.RS256)
				.restOperations(restOperations)
				.build();
		// aud는 예외 코드를 따로 구분해야 해서 검증기에 넣지 않고 read()에서 대조한다
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(properties.issuer())
		));
		return decoder;
	}
}
