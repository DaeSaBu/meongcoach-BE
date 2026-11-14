package com.daesabu.meongcoach.community.application.provided.dto;

import com.daesabu.meongcoach.dog.application.provided.PublicDogResult;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;

public record AuthorDogResult(String name, Breed breed, DogSex sex, Integer age, String profileImageUrl) {

	public static AuthorDogResult from(PublicDogResult dog) {
		return new AuthorDogResult(dog.name(), dog.breed(), dog.sex(), dog.age(), dog.profileImageUrl());
	}
}
