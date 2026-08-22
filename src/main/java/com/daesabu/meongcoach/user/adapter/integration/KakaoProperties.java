package com.daesabu.meongcoach.user.adapter.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 카카오 OIDC 연동 설정. 네이티브 SDK 방식이라 서버는 REST 키·시크릿을 보관하지 않고,
 * id_token 서명을 검증할 공개 키 주소와 우리 앱을 가리키는 aud 후보만 가진다.
 * aud는 플랫폼마다 다르므로(네이티브 앱은 네이티브 앱 키, 웹은 REST API 키) 목록으로 받는다.
 * audiences가 비면 모든 로그인이 aud 불일치로 거부되므로 기동 시점에 막는다.
 */
@Validated
@ConfigurationProperties("meongcoach.social.kakao")
public record KakaoProperties(
		@NotBlank String issuer,
		@NotBlank String jwkSetUri,
		@NotEmpty List<@NotBlank String> audiences
) implements OidcProviderProperties {
}
