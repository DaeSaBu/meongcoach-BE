package com.daesabu.meongcoach.auth.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;

import com.daesabu.meongcoach.auth.domain.Email;
import com.daesabu.meongcoach.auth.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import com.daesabu.meongcoach.auth.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.auth.domain.exception.SocialEmailRequiredException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 검증 규칙 자체는 OidcIdTokenVerifierTest가 맡는다. 여기서는 애플 설정이 검증기에 배선되고
 * 검증된 토큰의 클레임이 회원 연결 명령으로 옮겨지는지만 본다.
 */
class AppleSocialProfileReaderTest {

	private static final String ISSUER = "https://appleid.apple.com";
	private static final String JWK_SET_URI = "https://appleid.apple.com/auth/keys";
	private static final String BUNDLE_ID = "com.daesabu.meongcoach";
	private static final String SUBJECT = "001234.abcdef0123456789abcdef0123456789.1234";

	// revoke 설정은 이 리더가 쓰지 않으므로 형식만 맞춘 값을 둔다
	private static final AppleProperties PROPERTIES = new AppleProperties(ISSUER, JWK_SET_URI, List.of(BUNDLE_ID),
			BUNDLE_ID, "TEAMID0001", "KEYID00001", "unused-private-key");

	private static IdTokenSigner signer;

	private AppleSocialProfileReader reader;

	@BeforeAll
	static void generateKeys() {
		signer = new IdTokenSigner();
	}

	@BeforeEach
	void setUp() {
		IdTokenSigner.Bound bound = signer.bind(JWK_SET_URI, manyTimes(), signer.jwkSet());
		reader = new AppleSocialProfileReader(PROPERTIES, bound.restTemplate());
	}

	@Test
	void 담당_제공자는_애플이다() {
		assertThat(reader.provider()).isEqualTo(SocialProvider.APPLE);
	}

	@Test
	void 유효한_id_token이면_회원_식별자와_이메일을_읽는다() {
		String idToken = signer.sign(claims().claim("email", "abc123@privaterelay.appleid.com").build());

		SocialAccountLinkCommand command = reader.read(idToken);

		assertThat(command.provider()).isEqualTo(SocialProvider.APPLE);
		assertThat(command.providerId()).isEqualTo(SUBJECT);
		assertThat(command.email()).isEqualTo(new Email("abc123@privaterelay.appleid.com"));
	}

	@Test
	void 이메일_클레임이_없으면_SocialEmailRequiredException을_던진다() {
		String idToken = signer.sign(claims().build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(SocialEmailRequiredException.class);
	}

	@Test
	void 애플이_아닌_키로_서명된_토큰은_유효하지_않은_토큰으로_처리한다() {
		String idToken = signer.signByAttacker(claims().build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	private JWTClaimsSet.Builder claims() {
		return IdTokenSigner.claims(ISSUER, SUBJECT, BUNDLE_ID);
	}
}
