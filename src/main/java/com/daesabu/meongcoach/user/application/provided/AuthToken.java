package com.daesabu.meongcoach.user.application.provided;

import java.time.LocalDateTime;

/**
 * 우리 서비스가 발급한 토큰 쌍. 리프레시 토큰은 원문이 아니라 jti(refreshTokenId)와 만료 시각을 저장해 두고,
 * 재발급 시 저장된 토큰인지 확인한다.
 */
public record AuthToken(String accessToken, String refreshToken, String refreshTokenId,
                        LocalDateTime refreshTokenExpiresAt) {
}
