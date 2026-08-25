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
 * 앱이 Sign in with Apple로 받은 identityToken(OIDC id_token)을 검증하고 회원 식별자·이메일을 읽는다.
 * 검증 자체는 {@link OidcIdTokenVerifier}가 맡고, 토큰은 로그인 시점에만 쓰고 저장하지 않는다.
 * 이메일은 사용자가 숨기기를 고르면 비공개 릴레이 주소이고 동의하지 않으면 없다.
 */
@Component
public class AppleSocialProfileReader implements SocialProfileReader {

	private static final String EMAIL_CLAIM = "email";

	private final OidcIdTokenVerifier verifier;

	// 생성자가 둘이라 주입 대상을 명시한다
	@Autowired
	public AppleSocialProfileReader(AppleProperties properties, RestTemplateBuilder restTemplateBuilder) {
		this(properties, restTemplateBuilder.build());
	}

	// JWKS 응답을 테스트에서 가로챌 수 있도록 RestOperations를 직접 받는 통로를 둔다
	AppleSocialProfileReader(AppleProperties properties, RestOperations restOperations) {
		this.verifier = new OidcIdTokenVerifier(properties, restOperations);
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.APPLE;
	}

	@Override
	public SocialAccountLinkCommand read(String credential) {
		Jwt idToken = verifier.verify(credential);
		return new SocialAccountLinkCommand(SocialProvider.APPLE, idToken.getSubject(),
				idToken.getClaimAsString(EMAIL_CLAIM));
	}
}
