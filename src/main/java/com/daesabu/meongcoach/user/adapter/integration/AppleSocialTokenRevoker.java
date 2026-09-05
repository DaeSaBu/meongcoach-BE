package com.daesabu.meongcoach.user.adapter.integration;

import com.daesabu.meongcoach.user.adapter.integration.dto.AppleTokenResponse;
import com.daesabu.meongcoach.user.application.required.SocialTokenRevoker;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.exception.AppleAuthorizationCodeRequiredException;
import com.daesabu.meongcoach.user.domain.exception.InvalidAppleAuthorizationCodeException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 탈퇴 시 Sign in with Apple 토큰을 revoke한다(심사 지침 5.1.1(v)).
 * 서버는 Apple 토큰을 보관하지 않으므로 클라이언트가 탈퇴 직전 재인증으로 받은 authorization code를
 * /auth/token으로 refresh_token과 교환한 뒤 그 토큰을 /auth/revoke로 폐기한다. 두 요청 모두 client_secret이
 * 필요한데, Apple은 고정 시크릿 대신 .p8 개인 키로 서명한 ES256 JWT를 요구하므로 요청마다 짧게 만들어 쓴다.
 * 개인 키는 기동 시점에 파싱해 잘못된 설정을 첫 탈퇴 요청이 아니라 배포 시점에 드러낸다.
 */
@Component
public class AppleSocialTokenRevoker implements SocialTokenRevoker {

	private static final String TOKEN_PATH = "/auth/token";
	private static final String REVOKE_PATH = "/auth/revoke";
	private static final String GRANT_TYPE = "authorization_code";
	private static final String TOKEN_TYPE_HINT = "refresh_token";
	// Apple이 허용하는 상한은 6개월이지만 요청마다 새로 만들므로 두 요청을 끝낼 만큼만 유효하면 된다
	private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);
	private static final String PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
	private static final String PEM_FOOTER = "-----END PRIVATE KEY-----";

	private final AppleProperties properties;
	private final RestClient restClient;
	private final ECPrivateKey privateKey;

	public AppleSocialTokenRevoker(AppleProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
		this.privateKey = parsePrivateKey(properties.privateKey());
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.APPLE;
	}

	@Override
	public void revoke(String authorizationCode) {
		// Apple은 서버에 토큰이 없어 코드 없이는 revoke할 수 없으므로 여기서 필수로 요구한다
		if (authorizationCode == null || authorizationCode.isBlank()) {
			throw new AppleAuthorizationCodeRequiredException();
		}
		String clientSecret = createClientSecret();
		String refreshToken = exchangeForRefreshToken(authorizationCode, clientSecret);
		revokeRefreshToken(refreshToken, clientSecret);
	}

	private String exchangeForRefreshToken(String authorizationCode, String clientSecret) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", properties.clientId());
		form.add("client_secret", clientSecret);
		form.add("code", authorizationCode);
		form.add("grant_type", GRANT_TYPE);

		AppleTokenResponse response = post(TOKEN_PATH, form, AppleTokenResponse.class);
		if (response == null || response.refreshToken() == null || response.refreshToken().isBlank()) {
			throw new InvalidAppleAuthorizationCodeException();
		}
		return response.refreshToken();
	}

	private void revokeRefreshToken(String refreshToken, String clientSecret) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", properties.clientId());
		form.add("client_secret", clientSecret);
		form.add("token", refreshToken);
		form.add("token_type_hint", TOKEN_TYPE_HINT);

		post(REVOKE_PATH, form, Void.class);
	}

	private <T> T post(String path, MultiValueMap<String, String> form, Class<T> responseType) {
		try {
			return restClient.post()
					.uri(properties.issuer() + path)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(responseType);
		} catch (RestClientResponseException e) {
			// 4xx는 코드 만료·재사용·다른 앱 발급 등 요청 자체가 거부된 것이라 새 코드로 다시 시도해야 한다
			if (e.getStatusCode().is4xxClientError()) {
				throw new InvalidAppleAuthorizationCodeException();
			}
			throw new SocialProviderUnavailableException();
		} catch (RestClientException e) {
			// 연결·타임아웃 실패다. 코드 무효와 구분해야 클라이언트가 같은 코드로 재시도할 수 있다
			throw new SocialProviderUnavailableException();
		}
	}

	private String createClientSecret() {
		Instant now = Instant.now();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.issuer(properties.teamId())
				.subject(properties.clientId())
				.audience(properties.issuer())
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plus(CLIENT_SECRET_TTL)))
				.build();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
				.keyID(properties.keyId())
				.build();
		SignedJWT jwt = new SignedJWT(header, claims);
		try {
			jwt.sign(new ECDSASigner(privateKey));
		} catch (JOSEException e) {
			throw new IllegalStateException("Apple client_secret 서명에 실패했습니다", e);
		}
		return jwt.serialize();
	}

	// .p8 파일 내용(PKCS#8 PEM)을 그대로 환경 변수로 받으므로 머리말·꼬리말과 줄바꿈(이스케이프된 \n 포함)을 걷어내고 디코딩한다
	private static ECPrivateKey parsePrivateKey(String pem) {
		String encoded = pem
				.replace(PEM_HEADER, "")
				.replace(PEM_FOOTER, "")
				.replace("\\n", "")
				.replaceAll("\\s", "");
		try {
			byte[] der = Base64.getDecoder().decode(encoded);
			return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
		} catch (IllegalArgumentException | InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("Apple 개인 키(APPLE_PRIVATE_KEY)가 PKCS#8 EC 키가 아닙니다", e);
		}
	}
}
