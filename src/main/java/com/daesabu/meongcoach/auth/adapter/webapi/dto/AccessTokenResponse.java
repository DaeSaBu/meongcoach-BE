package com.daesabu.meongcoach.auth.adapter.webapi.dto;

import com.daesabu.meongcoach.auth.domain.AuthToken;

public record AccessTokenResponse(String accessToken) {

	public static AccessTokenResponse from(AuthToken token) {
		return new AccessTokenResponse(token.accessToken());
	}
}
