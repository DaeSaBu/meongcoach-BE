package com.daesabu.meongcoach.auth.adapter.webapi.legacy;

import jakarta.validation.constraints.NotBlank;

/**
 * 구 클라이언트의 소셜 로그인 본문. 제공자는 경로 변수로 오므로 토큰만 담는다.
 * 요청 DTO는 application/provided/dto에 두는 것이 규칙이지만, 이 record는 구 계약 형태를 신 SocialLoginRequest로 옮기기 위한
 * 호환 계층 전용이라 삭제 단위를 맞추려 여기에 둔다.
 */
public record LegacySocialLoginRequest(@NotBlank String token) {
}
