package com.daesabu.meongcoach.user.adapter.integration;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

/**
 * OIDC id_token 검증 테스트 공용 헬퍼. 제공자 역할의 RSA 키로 토큰을 서명하고,
 * 그 공개 키를 JWKS 응답으로 돌려주는 가짜 제공자 서버를 만든다.
 */
final class IdTokenSigner {

	private final RSAKey providerKey;
	private final RSAKey attackerKey;

	IdTokenSigner() {
		this.providerKey = generate("provider-key");
		this.attackerKey = generate("attacker-key");
	}

	RSAKey providerKey() {
		return providerKey;
	}

	RSAKey attackerKey() {
		return attackerKey;
	}

	/** 제공자 공개 키 JWKS를 성공 응답으로 돌려준다. */
	ResponseCreator jwkSet() {
		return withSuccess(new JWKSet(providerKey.toPublicJWK()).toString(), MediaType.APPLICATION_JSON);
	}

	/** jwkSetUri 호출에 주어진 응답을 돌려주는 RestTemplate과 그 서버를 만든다. */
	Bound bind(String jwkSetUri, ExpectedCount count, ResponseCreator response) {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(count, requestTo(jwkSetUri)).andRespond(response);
		return new Bound(restTemplate, server);
	}

	/** 발급자·주체·aud·발급/만료 시각이 채워진 유효한 클레임 빌더. */
	static JWTClaimsSet.Builder claims(String issuer, String subject, String audience) {
		Instant now = Instant.now();
		return new JWTClaimsSet.Builder()
				.issuer(issuer)
				.subject(subject)
				.audience(audience)
				.issueTime(Date.from(now))
				.expirationTime(Date.from(now.plusSeconds(300)));
	}

	String sign(JWTClaimsSet claims) {
		return sign(providerKey, claims);
	}

	String signByAttacker(JWTClaimsSet claims) {
		return sign(attackerKey, claims);
	}

	private static String sign(RSAKey key, JWTClaimsSet claims) {
		try {
			SignedJWT jwt = new SignedJWT(
					new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
			jwt.sign(new RSASSASigner(key));
			return jwt.serialize();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static RSAKey generate(String keyId) {
		try {
			return new RSAKeyGenerator(2048).keyID(keyId).generate();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	record Bound(RestTemplate restTemplate, MockRestServiceServer server) {
	}
}
