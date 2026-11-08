package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.dog.application.provided.SelectedDogResult;

public record AuthorResult(Long userId, String dogName, String profileImageUrl, boolean expert) {

	public AuthorResult(Long userId, String dogName, String profileImageUrl) {
		this(userId, dogName, profileImageUrl, false);
	}

	public static AuthorResult of(SelectedDogResult selectedDog, boolean expert) {
		return new AuthorResult(selectedDog.userId(), selectedDog.name(), selectedDog.profileImageUrl(), expert);
	}
}
