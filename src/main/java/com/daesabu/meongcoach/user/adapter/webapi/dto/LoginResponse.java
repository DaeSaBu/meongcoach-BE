package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.application.provided.LoginResult;

public record LoginResponse(String accessToken, String refreshToken, boolean needsOnboarding) {

	public static LoginResponse from(LoginResult result) {
		return new LoginResponse(result.token().accessToken(), result.token().refreshToken(),
				result.needsOnboarding());
	}
}
