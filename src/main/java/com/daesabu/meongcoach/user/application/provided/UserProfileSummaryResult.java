package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;

public record UserProfileSummaryResult(Long userId, String nickname, String profileImageUrl) {

	public static UserProfileSummaryResult from(User user) {
		UserProfile profile = user.getProfile();
		return new UserProfileSummaryResult(user.getId(), profile.getNickname(), profile.getProfileImageUrl());
	}
}
