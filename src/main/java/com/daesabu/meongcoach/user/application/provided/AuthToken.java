package com.daesabu.meongcoach.user.application.provided;

/**
 * 우리 서비스가 발급한 토큰 쌍. 리프레시 토큰은 저장하지 않고 서명으로만 검증한다.
 */
public record AuthToken(String accessToken, String refreshToken) {
}
