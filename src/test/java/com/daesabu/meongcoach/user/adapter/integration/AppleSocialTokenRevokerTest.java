package com.daesabu.meongcoach.user.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.exception.AppleAuthorizationCodeRequiredException;
import com.daesabu.meongcoach.user.domain.exception.InvalidAppleAuthorizationCodeException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

/**
 * HTTP 호출은 MockRestServiceServer로 가로채고, Apple 문서가 요구하는 두 요청(/auth/token → /auth/revoke)의
 * 순서·폼 파라미터·client_secret 구성과 실패 원인별 예외 번역을 확인한다.
 */
class AppleSocialTokenRevokerTest {

	private static final String ISSUER = "https://appleid.apple.com";
	private static final String TOKEN_URL = ISSUER + "/auth/token";
	private static final String REVOKE_URL = ISSUER + "/auth/revoke";
	private static final String CLIENT_ID = "com.daesabu.meongcoach";
	private static final String TEAM_ID = "TEAMID0001";
	private static final String KEY_ID = "KEYID00001";
	private static final String AUTHORIZATION_CODE = "c1a2b3.0.abcd.efgh";
	private static final String REFRESH_TOKEN = "r1a2b3.0.abcd.efgh";
	private static final String TOKEN_RESPONSE = """
			{"access_token":"a1","token_type":"Bearer","expires_in":3600,"refresh_token":"%s","id_token":"x.y.z"}
			""".formatted(REFRESH_TOKEN);

	private static KeyPair keyPair;
	private static AppleProperties properties;

	private MockRestServiceServer server;
	private AppleSocialTokenRevoker revoker;

	@BeforeAll
	static void generateKey() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp256r1"));
		keyPair = generator.generateKeyPair();
		properties = properties(toPem(keyPair));
	}

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		revoker = new AppleSocialTokenRevoker(properties, builder);
	}

	@Test
	void 담당_제공자는_애플이다() {
		assertThat(revoker.provider()).isEqualTo(SocialProvider.APPLE);
	}

	@Test
	void 인가_코드를_refresh_token으로_교환한_뒤_그_토큰을_revoke한다() {
		server.expect(requestTo(TOKEN_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().formDataContains(Map.of(
						"client_id", CLIENT_ID,
						"code", AUTHORIZATION_CODE,
						"grant_type", "authorization_code")))
				.andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
		server.expect(requestTo(REVOKE_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().formDataContains(Map.of(
						"client_id", CLIENT_ID,
						"token", REFRESH_TOKEN,
						"token_type_hint", "refresh_token")))
				.andRespond(withSuccess());

		revoker.revoke(AUTHORIZATION_CODE);

		server.verify();
	}

	@Test
	void client_secret은_팀_ID가_발급하고_키_ID로_서명한_ES256_JWT다() throws Exception {
		Map<String, String> tokenForm = new HashMap<>();
		server.expect(requestTo(TOKEN_URL))
				.andExpect(captureForm(tokenForm))
				.andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
		server.expect(requestTo(REVOKE_URL)).andRespond(withSuccess());

		revoker.revoke(AUTHORIZATION_CODE);

		SignedJWT clientSecret = SignedJWT.parse(tokenForm.get("client_secret"));
		JWTClaimsSet claims = clientSecret.getJWTClaimsSet();
		assertThat(clientSecret.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
		assertThat(clientSecret.getHeader().getKeyID()).isEqualTo(KEY_ID);
		assertThat(claims.getIssuer()).isEqualTo(TEAM_ID);
		assertThat(claims.getSubject()).isEqualTo(CLIENT_ID);
		assertThat(claims.getAudience()).containsExactly(ISSUER);
		assertThat(claims.getExpirationTime()).isAfter(new Date());
		assertThat(clientSecret.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic()))).isTrue();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", " "})
	void 인가_코드가_없으면_코드_필수_예외를_던지고_Apple을_호출하지_않는다(String authorizationCode) {
		assertThatThrownBy(() -> revoker.revoke(authorizationCode))
				.isInstanceOf(AppleAuthorizationCodeRequiredException.class);
		server.verify();
	}

	@Test
	void Apple이_인가_코드를_거부하면_코드_무효_예외를_던진다() {
		server.expect(requestTo(TOKEN_URL))
				.andRespond(withBadRequest().body("{\"error\":\"invalid_grant\"}").contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> revoker.revoke(AUTHORIZATION_CODE))
				.isInstanceOf(InvalidAppleAuthorizationCodeException.class);
		server.verify();
	}

	@Test
	void 토큰_응답에_refresh_token이_없으면_코드_무효_예외를_던진다() {
		server.expect(requestTo(TOKEN_URL))
				.andRespond(withSuccess("{\"access_token\":\"a1\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> revoker.revoke(AUTHORIZATION_CODE))
				.isInstanceOf(InvalidAppleAuthorizationCodeException.class);
		server.verify();
	}

	@Test
	void revoke_요청이_거부돼도_코드_무효_예외를_던진다() {
		server.expect(requestTo(TOKEN_URL))
				.andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
		server.expect(requestTo(REVOKE_URL))
				.andRespond(withBadRequest().body("{\"error\":\"invalid_client\"}").contentType(MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> revoker.revoke(AUTHORIZATION_CODE))
				.isInstanceOf(InvalidAppleAuthorizationCodeException.class);
		server.verify();
	}

	@Test
	void Apple_서버_오류면_제공자_통신_실패_예외를_던진다() {
		server.expect(requestTo(TOKEN_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> revoker.revoke(AUTHORIZATION_CODE))
				.isInstanceOf(SocialProviderUnavailableException.class);
	}

	@Test
	void 연결_자체가_실패하면_제공자_통신_실패_예외를_던진다() {
		server.expect(requestTo(TOKEN_URL)).andRespond(withException(new IOException("connection reset")));

		assertThatThrownBy(() -> revoker.revoke(AUTHORIZATION_CODE))
				.isInstanceOf(SocialProviderUnavailableException.class);
	}

	@Test
	void 이스케이프된_줄바꿈이_섞인_PEM도_읽는다() {
		String escaped = toPem(keyPair).replace("\n", "\\n");

		assertThat(new AppleSocialTokenRevoker(properties(escaped), RestClient.builder())).isNotNull();
	}

	@Test
	void 개인_키가_PKCS8_EC_키가_아니면_생성_시점에_실패한다() {
		AppleProperties broken = properties("-----BEGIN PRIVATE KEY-----\nbm90LWEta2V5\n-----END PRIVATE KEY-----");

		assertThatThrownBy(() -> new AppleSocialTokenRevoker(broken, RestClient.builder()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("APPLE_PRIVATE_KEY");
	}

	private static AppleProperties properties(String privateKeyPem) {
		return new AppleProperties(ISSUER, ISSUER + "/auth/keys", List.of(CLIENT_ID),
				CLIENT_ID, TEAM_ID, KEY_ID, privateKeyPem);
	}

	private static String toPem(KeyPair keyPair) {
		String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
				.encodeToString(keyPair.getPrivate().getEncoded());
		return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
	}

	// 폼 본문을 파싱해 넘긴 맵에 담는다. client_secret처럼 요청마다 달라지는 값을 꺼내 보기 위해서다
	private static RequestMatcher captureForm(Map<String, String> target) {
		return request -> {
			String body = ((MockClientHttpRequest) request).getBodyAsString();
			Arrays.stream(body.split("&"))
					.map(pair -> pair.split("=", 2))
					.forEach(pair -> target.put(decode(pair[0]), decode(pair[1])));
		};
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}
}
