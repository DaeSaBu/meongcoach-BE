package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.Dog;

public record SelectedDogResult(Long userId, String name, String profileImageUrl) {

	public static SelectedDogResult from(Dog dog) {
		return new SelectedDogResult(dog.getUserId(), dog.getName(), dog.getProfileImageUrl());
	}
}
