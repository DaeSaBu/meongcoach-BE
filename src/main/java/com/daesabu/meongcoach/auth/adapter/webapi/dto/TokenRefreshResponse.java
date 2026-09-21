package com.daesabu.meongcoach.auth.adapter.webapi.dto;

import com.daesabu.meongcoach.auth.application.provided.AuthToken;

public record TokenRefreshResponse(String accessToken, String refreshToken) {

	public static TokenRefreshResponse from(AuthToken token) {
		return new TokenRefreshResponse(token.accessToken(), token.refreshToken());
	}
}
