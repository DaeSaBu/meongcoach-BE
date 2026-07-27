package com.daesabu.meongcoach.user.adapter.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 GET /v1/user/access_token_info 응답.
 * app_id를 돌려주는 유일한 엔드포인트라 회원 식별자도 여기에서 얻는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoAccessTokenInfoResponse(Long id, @JsonProperty("app_id") Long appId) {
}
