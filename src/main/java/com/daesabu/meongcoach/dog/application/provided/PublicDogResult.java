package com.daesabu.meongcoach.dog.application.provided;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;

public record PublicDogResult(String name, Breed breed, DogSex sex, Integer age, String profileImageUrl) {

	public static PublicDogResult from(Dog dog) {
		return new PublicDogResult(dog.getName(), dog.getBreed(), dog.getSex(), dog.getAge(), dog.getProfileImageUrl());
	}
}
