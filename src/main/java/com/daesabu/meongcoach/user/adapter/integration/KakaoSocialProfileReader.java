package com.daesabu.meongcoach.user.adapter.integration;

import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;

/**
 * 앱이 카카오 SDK로 받은 OIDC id_token을 검증하고 회원 식별자·이메일을 읽는다.
 * 검증 자체는 {@link OidcIdTokenVerifier}가 맡고, 토큰은 로그인 시점에만 쓰고 저장하지 않는다.
 */
@Component
public class KakaoSocialProfileReader implements SocialProfileReader {

	private static final String EMAIL_CLAIM = "email";

	private final OidcIdTokenVerifier verifier;

	// 생성자가 둘이라 주입 대상을 명시한다
	@Autowired
	public KakaoSocialProfileReader(KakaoProperties properties, RestTemplateBuilder restTemplateBuilder) {
		this(properties, restTemplateBuilder.build());
	}

	// JWKS 응답을 테스트에서 가로챌 수 있도록 RestOperations를 직접 받는 통로를 둔다
	KakaoSocialProfileReader(KakaoProperties properties, RestOperations restOperations) {
		this.verifier = new OidcIdTokenVerifier(properties, restOperations);
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.KAKAO;
	}

	@Override
	public SocialAccountLinkCommand read(String credential) {
		Jwt idToken = verifier.verify(credential);
		return new SocialAccountLinkCommand(SocialProvider.KAKAO, idToken.getSubject(),
				idToken.getClaimAsString(EMAIL_CLAIM));
	}
}
