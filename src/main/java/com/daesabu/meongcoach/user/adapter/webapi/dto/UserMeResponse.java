package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;

public record UserMeResponse(Long userId, boolean needsOnboarding, ProfileResponse profile) {

	public static UserMeResponse from(User user) {
		UserProfile profile = user.getProfile();
		if (profile == null) {
			return new UserMeResponse(user.getId(), user.isOnboarding(), null);
		}
		return new UserMeResponse(user.getId(), user.isOnboarding(), ProfileResponse.from(profile));
	}

	public record ProfileResponse(String nickname, String profileImageUrl) {

		public static ProfileResponse from(UserProfile profile) {
			return new ProfileResponse(profile.getNickname(), profile.getProfileImageUrl());
		}
	}
}
