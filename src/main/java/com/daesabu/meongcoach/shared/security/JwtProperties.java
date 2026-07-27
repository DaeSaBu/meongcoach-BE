package com.daesabu.meongcoach.shared.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 자체 JWT 발급·검증 설정. secret은 HS256이 요구하는 최소 길이(32바이트)를 만족해야 하며,
 * 미달이면 애플리케이션 기동 시점에 실패한다.
 */
@Validated
@ConfigurationProperties("meongcoach.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@Size(min = 32) String secret,
		Duration accessTokenValidity,
		Duration refreshTokenValidity
) {

	private static final String ALGORITHM = "HmacSHA256";

	public SecretKey secretKey() {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
	}
}
