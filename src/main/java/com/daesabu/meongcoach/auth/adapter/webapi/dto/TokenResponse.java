package com.daesabu.meongcoach.auth.adapter.webapi.dto;

import com.daesabu.meongcoach.auth.domain.AuthToken;

public record TokenResponse(String accessToken, String refreshToken) {

	public static TokenResponse from(AuthToken token) {
		return new TokenResponse(token.accessToken(), token.refreshToken());
	}
}
