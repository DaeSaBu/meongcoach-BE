package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.Dog;

public record FirstDogResult(Long userId, String name, String profileImageUrl) {

	public static FirstDogResult from(Dog dog) {
		return new FirstDogResult(dog.getUserId(), dog.getName(), dog.getProfileImageUrl());
	}
}
