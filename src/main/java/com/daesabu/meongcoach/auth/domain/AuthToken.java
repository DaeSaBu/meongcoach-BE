package com.daesabu.meongcoach.auth.domain;

import java.time.LocalDateTime;

public record AuthToken(String accessToken, String refreshToken, RefreshTokenId refreshTokenId,
                        LocalDateTime refreshTokenExpiresAt) {
}
