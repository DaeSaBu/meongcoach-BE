package com.daesabu.meongcoach.user.adapter.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 애플 OIDC 연동 설정. 로그인은 Sign in with Apple 네이티브 방식이라 id_token 서명을 검증할 공개 키 주소와
 * 우리 앱을 가리키는 aud 후보만 쓴다. aud는 플랫폼마다 다르므로(iOS 네이티브는 앱 번들 ID, 웹·안드로이드는 Services ID) 목록으로 받는다.
 * audiences가 비면 모든 로그인이 aud 불일치로 거부되므로 기동 시점에 막는다.
 * 탈퇴 시 토큰 revoke는 서버가 Apple에 직접 요청하므로 client_secret(ES256 JWT)을 만들 팀 ID·키 ID·개인 키(.p8)와
 * authorization code가 발급된 앱의 client_id가 필요하다. 하나라도 비면 revoke 요청이 전부 실패하므로 역시 기동 시점에 막는다.
 */
@Validated
@ConfigurationProperties("meongcoach.social.apple")
public record AppleProperties(
		@NotBlank String issuer,
		@NotBlank String jwkSetUri,
		@NotEmpty List<@NotBlank String> audiences,
		@NotBlank String clientId,
		@NotBlank String teamId,
		@NotBlank String keyId,
		@NotBlank String privateKey
) implements OidcProviderProperties {
}
