package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.dog.application.provided.FirstDogResult;

public record AuthorResult(Long userId, String dogName, String profileImageUrl) {

	public static AuthorResult from(FirstDogResult firstDog) {
		return new AuthorResult(firstDog.userId(), firstDog.name(), firstDog.profileImageUrl());
	}
}
