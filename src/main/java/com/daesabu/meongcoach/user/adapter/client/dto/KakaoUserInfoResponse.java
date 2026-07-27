package com.daesabu.meongcoach.user.adapter.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 GET /v2/user/me 응답. 이메일은 동의 항목 미승인·미동의 시 내려오지 않으므로
 * kakao_account 자체가 없을 수 있다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(@JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record KakaoAccount(String email) {
	}

	public String resolveEmail() {
		if (kakaoAccount == null) {
			return null;
		}
		return kakaoAccount.email();
	}
}
