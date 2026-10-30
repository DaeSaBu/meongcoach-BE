package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserRole;

public record UserMeResponse(Long userId, UserRole role, boolean needsOnboarding, ProfileResponse profile) {

	public static UserMeResponse from(User user) {
		UserProfile profile = user.getProfile();
		if (profile == null) {
			return new UserMeResponse(user.getId(), user.getRole(), user.isOnboarding(), null);
		}
		return new UserMeResponse(user.getId(), user.getRole(), user.isOnboarding(), ProfileResponse.from(profile));
	}

	public record ProfileResponse(String nickname, String profileImageUrl) {

		public static ProfileResponse from(UserProfile profile) {
			return new ProfileResponse(profile.getNickname(), profile.getProfileImageUrl());
		}
	}
}
