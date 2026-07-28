package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.application.provided.AuthToken;

public record TokenRefreshResponse(String accessToken, String refreshToken) {

	public static TokenRefreshResponse from(AuthToken token) {
		return new TokenRefreshResponse(token.accessToken(), token.refreshToken());
	}
}
