package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.domain.User;

public record UserMeResponse(boolean needsOnboarding) {

	public static UserMeResponse from(User user) {
		return new UserMeResponse(user.isOnboarding());
	}
}
