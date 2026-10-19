package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfile;

public record UserProfileSummaryResult(Long userId, String nickname, String profileImageUrl) {

	public static UserProfileSummaryResult from(UserProfile profile) {
		return new UserProfileSummaryResult(profile.getUserId(), profile.getNickname(), profile.getProfileImageUrl());
	}
}
