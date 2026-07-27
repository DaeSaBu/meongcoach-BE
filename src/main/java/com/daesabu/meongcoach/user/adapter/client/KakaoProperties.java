package com.daesabu.meongcoach.user.adapter.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 카카오 연동 설정. 네이티브 SDK 방식이라 서버는 REST 키·시크릿을 보관하지 않고,
 * 토큰이 우리 앱에서 발급된 것인지 대조할 앱 ID만 가진다.
 */
@ConfigurationProperties("meongcoach.social.kakao")
public record KakaoProperties(String apiBaseUrl, Long appId) {
}
