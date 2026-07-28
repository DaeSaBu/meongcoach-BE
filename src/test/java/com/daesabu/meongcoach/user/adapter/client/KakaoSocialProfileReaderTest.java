package com.daesabu.meongcoach.user.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.daesabu.meongcoach.user.domain.exception.SocialTokenAppMismatchException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

@DisplayName("카카오 id_token 검증 클라이언트")
class KakaoSocialProfileReaderTest {

	private static final String ISSUER = "https://kauth.kakao.com";
	private static final String JWK_SET_URI = "https://kauth.kakao.com/.well-known/jwks.json";
	private static final String NATIVE_APP_KEY = "our-native-app-key";
	private static final String SUBJECT = "3812345678";

	private static final KakaoProperties PROPERTIES =
			new KakaoProperties(ISSUER, JWK_SET_URI, List.of(NATIVE_APP_KEY));

	private static RSAKey kakaoKey;
	private static RSAKey attackerKey;

	private MockRestServiceServer server;
	private KakaoSocialProfileReader reader;

	@BeforeAll
	static void generateKeys() throws Exception {
		kakaoKey = new RSAKeyGenerator(2048).keyID("kakao-key").generate();
		attackerKey = new RSAKeyGenerator(2048).keyID("attacker-key").generate();
	}

	@BeforeEach
	void setUp() {
		reader = readerServing(manyTimes(), jwkSet());
	}

	@Test
	@DisplayName("담당 제공자는 카카오다")
	void providerIsKakao() {
		assertThat(reader.provider()).isEqualTo(SocialProvider.KAKAO);
	}

	@Test
	@DisplayName("유효한 id_token이면 회원 식별자와 이메일을 읽는다")
	void readReturnsProviderIdAndEmail() {
		String idToken = sign(kakaoKey, claims().claim("email", "a@b.com").build());

		SocialAccountLinkCommand command = reader.read(idToken);

		assertThat(command.provider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(command.providerId()).isEqualTo(SUBJECT);
		assertThat(command.email()).isEqualTo("a@b.com");
	}

	@Test
	@DisplayName("이메일 동의를 받지 못하면 이메일 없이 읽는다")
	void readReturnsNullEmailWhenClaimIsAbsent() {
		SocialAccountLinkCommand command = reader.read(sign(kakaoKey, claims().build()));

		assertThat(command.providerId()).isEqualTo(SUBJECT);
		assertThat(command.email()).isNull();
	}

	@Test
	@DisplayName("공개 키를 캐시해 로그인마다 카카오를 호출하지 않는다")
	void readFetchesJwkSetOnlyOnce() {
		reader = readerServing(once(), jwkSet());

		reader.read(sign(kakaoKey, claims().build()));
		reader.read(sign(kakaoKey, claims().build()));

		server.verify();
	}

	@Test
	@DisplayName("다른 앱에 발급된 토큰이면 서명이 유효해도 거부한다")
	void readFailsWhenAudienceDoesNotMatch() {
		String idToken = sign(kakaoKey, claims().audience("attacker-app-key").build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(SocialTokenAppMismatchException.class);
	}

	@Test
	@DisplayName("aud가 없는 토큰도 거부한다")
	void readFailsWhenAudienceIsAbsent() {
		String idToken = sign(kakaoKey, claims().audience((String) null).build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(SocialTokenAppMismatchException.class);
	}

	@Test
	@DisplayName("카카오가 아닌 키로 서명된 토큰은 유효하지 않은 토큰으로 처리한다")
	void readFailsWhenSignedByUnknownKey() {
		String idToken = sign(attackerKey, claims().build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	@DisplayName("발급자가 다른 토큰은 유효하지 않은 토큰으로 처리한다")
	void readFailsWhenIssuerDoesNotMatch() {
		String idToken = sign(kakaoKey, claims().issuer("https://evil.example.com").build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	@DisplayName("만료된 토큰은 유효하지 않은 토큰으로 처리한다")
	void readFailsWhenTokenIsExpired() {
		Instant longAgo = Instant.now().minus(1, ChronoUnit.DAYS);
		String idToken = sign(kakaoKey, claims()
				.issueTime(Date.from(longAgo))
				.expirationTime(Date.from(longAgo.plusSeconds(300)))
				.build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	@DisplayName("JWT 형식이 아니면 유효하지 않은 토큰으로 처리한다")
	void readFailsWhenTokenIsMalformed() {
		assertThatThrownBy(() -> reader.read("not-a-jwt"))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	@DisplayName("공개 키를 가져오지 못하면 토큰 무효와 구분해 처리한다")
	void readFailsWhenJwkSetIsUnavailable() {
		reader = readerServing(manyTimes(), withServerError());
		String idToken = sign(kakaoKey, claims().build());

		assertThatThrownBy(() -> reader.read(idToken))
				.isInstanceOf(SocialProviderUnavailableException.class);
	}

	private KakaoSocialProfileReader readerServing(ExpectedCount count, ResponseCreator response) {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(count, requestTo(JWK_SET_URI)).andRespond(response);
		return new KakaoSocialProfileReader(PROPERTIES, restTemplate);
	}

	private ResponseCreator jwkSet() {
		return withSuccess(new JWKSet(kakaoKey.toPublicJWK()).toString(), MediaType.APPLICATION_JSON);
	}

	private JWTClaimsSet.Builder claims() {
		Instant now = Instant.now();
		return new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.subject(SUBJECT)
				.audience(NATIVE_APP_KEY)
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plusSeconds(300)));
	}

	private String sign(RSAKey key, JWTClaimsSet claims) {
		try {
			SignedJWT jwt = new SignedJWT(
					new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
			jwt.sign(new RSASSASigner(key));
			return jwt.serialize();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
