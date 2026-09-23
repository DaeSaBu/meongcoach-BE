package com.daesabu.meongcoach.auth.domain;

import java.time.LocalDateTime;

// userId는 토큰 쌍의 주인이다. 로그인 응답에 회원 상태를 덧붙여야 하는 호출자가 이 값으로 회원을 조회한다
public record AuthToken(Long userId, String accessToken, String refreshToken, RefreshTokenId refreshTokenId,
                        LocalDateTime refreshTokenExpiresAt) {
}
