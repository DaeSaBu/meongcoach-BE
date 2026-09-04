package com.daesabu.meongcoach.user.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 로그인 요청. 이메일 형식 검증은 도메인 값 객체(Email)가 맡으므로 여기서는 공백만 막는다.
 */
public record LocalLoginRequest(@NotBlank String email, @NotBlank String password) {
}
