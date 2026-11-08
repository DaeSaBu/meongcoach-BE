package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.dog.application.provided.SelectedDogResult;
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;

public record AuthorResult(Long userId, String name, String profileImageUrl, boolean expert) {

	public AuthorResult(Long userId, String name, String profileImageUrl) {
		this(userId, name, profileImageUrl, false);
	}

	public static AuthorResult from(SelectedDogResult selectedDog) {
		return new AuthorResult(selectedDog.userId(), selectedDog.name(), selectedDog.profileImageUrl(), false);
	}

	public static AuthorResult fromExpert(UserProfileSummaryResult profile) {
		return new AuthorResult(profile.userId(), profile.nickname(), profile.profileImageUrl(), true);
	}
}
