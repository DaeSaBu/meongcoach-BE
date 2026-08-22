package com.daesabu.meongcoach.user.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.daesabu.meongcoach.user.domain.exception.SocialTokenAppMismatchException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;

class OidcIdTokenVerifierTest {

	private static final String ISSUER = "https://provider.example.com";
	private static final String JWK_SET_URI = "https://provider.example.com/.well-known/jwks.json";
	private static final String OUR_APP = "our-app-id";
	private static final String SUBJECT = "3812345678";

	private record TestProperties(String issuer, String jwkSetUri, List<String> audiences)
			implements OidcProviderProperties {
	}

	private static final TestProperties PROPERTIES = new TestProperties(ISSUER, JWK_SET_URI, List.of(OUR_APP));

	private static IdTokenSigner signer;

	private MockRestServiceServer server;
	private OidcIdTokenVerifier verifier;

	@BeforeAll
	static void generateKeys() {
		signer = new IdTokenSigner();
	}

	@BeforeEach
	void setUp() {
		verifier = verifierServing(manyTimes(), signer.jwkSet());
	}

	@Test
	void 유효한_id_token이면_검증된_토큰을_돌려준다() {
		Jwt idToken = verifier.verify(signer.sign(claims().claim("email", "a@b.com").build()));

		assertThat(idToken.getSubject()).isEqualTo(SUBJECT);
		assertThat(idToken.getClaimAsString("email")).isEqualTo("a@b.com");
	}

	@Test
	void 공개_키를_캐시해_검증마다_제공자를_호출하지_않는다() {
		verifier = verifierServing(once(), signer.jwkSet());

		verifier.verify(signer.sign(claims().build()));
		verifier.verify(signer.sign(claims().build()));

		server.verify();
	}

	@Test
	void 다른_앱에_발급된_토큰이면_서명이_유효해도_거부한다() {
		String idToken = signer.sign(claims().audience("attacker-app-id").build());

		assertThatThrownBy(() -> verifier.verify(idToken))
				.isInstanceOf(SocialTokenAppMismatchException.class);
	}

	@Test
	void aud가_없는_토큰도_거부한다() {
		String idToken = signer.sign(claims().audience((String) null).build());

		assertThatThrownBy(() -> verifier.verify(idToken))
				.isInstanceOf(SocialTokenAppMismatchException.class);
	}

	@Test
	void sub가_없는_토큰은_유효하지_않은_토큰으로_처리한다() {
		String idToken = signer.sign(claims().subject(null).build());

		assertThatThrownBy(() -> verifier.verify(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	void 제공자가_아닌_키로_서명된_토큰은_유효하지_않은_토큰으로_처리한다() {
		String idToken = signer.signByAttacker(claims().build());

		assertThatThrownBy(() -> verifier.verify(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	void 발급자가_다른_토큰은_유효하지_않은_토큰으로_처리한다() {
		String idToken = signer.sign(claims().issuer("https://evil.example.com").build());

		assertThatThrownBy(() -> verifier.verify(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	void 만료된_토큰은_유효하지_않은_토큰으로_처리한다() {
		Instant longAgo = Instant.now().minus(1, ChronoUnit.DAYS);
		String idToken = signer.sign(claims()
				.issueTime(Date.from(longAgo))
				.expirationTime(Date.from(longAgo.plusSeconds(300)))
				.build());

		assertThatThrownBy(() -> verifier.verify(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	void JWT_형식이_아니면_유효하지_않은_토큰으로_처리한다() {
		assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	void 공개_키를_가져오지_못하면_토큰_무효와_구분해_처리한다() {
		verifier = verifierServing(manyTimes(), withServerError());
		String idToken = signer.sign(claims().build());

		assertThatThrownBy(() -> verifier.verify(idToken))
				.isInstanceOf(SocialProviderUnavailableException.class);
	}

	private OidcIdTokenVerifier verifierServing(ExpectedCount count, ResponseCreator response) {
		IdTokenSigner.Bound bound = signer.bind(JWK_SET_URI, count, response);
		server = bound.server();
		return new OidcIdTokenVerifier(PROPERTIES, bound.restTemplate());
	}

	private JWTClaimsSet.Builder claims() {
		return IdTokenSigner.claims(ISSUER, SUBJECT, OUR_APP);
	}
}
