package com.daesabu.meongcoach.user.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Apple 토큰 엔드포인트(/auth/token) 응답. revoke에 쓸 refresh_token만 읽고 access_token·id_token 등은 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppleTokenResponse(@JsonProperty("refresh_token") String refreshToken) {
}
