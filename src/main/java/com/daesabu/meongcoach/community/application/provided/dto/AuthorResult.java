package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.dog.application.provided.SelectedDogResult;

public record AuthorResult(Long userId, String dogName, String profileImageUrl) {

	public static AuthorResult from(SelectedDogResult selectedDog) {
		return new AuthorResult(selectedDog.userId(), selectedDog.name(), selectedDog.profileImageUrl());
	}
}
