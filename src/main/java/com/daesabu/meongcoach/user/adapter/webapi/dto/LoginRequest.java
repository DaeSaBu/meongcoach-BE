package com.daesabu.meongcoach.user.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청. token은 앱이 제공자 SDK로 받은 자격증명으로, 카카오·구글·애플 모두 ID 토큰(OIDC)을 담는다.
 */
public record LoginRequest(@NotBlank String token) {
}
