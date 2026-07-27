package com.daesabu.meongcoach.user.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 앱이 제공자 SDK로 받은 자격증명. 카카오는 액세스 토큰, 구글·애플은 ID 토큰을 담는다.
 */
public record SocialLoginRequest(@NotBlank String token) {
}
