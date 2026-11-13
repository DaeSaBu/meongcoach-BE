package com.daesabu.meongcoach.community.adapter.webapi.dto;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorDogResult;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import java.util.Optional;

public record AuthorDogResponse(DogResponse dog) {

	public static AuthorDogResponse from(Optional<AuthorDogResult> dog) {
		return new AuthorDogResponse(dog.map(DogResponse::from).orElse(null));
	}

	public record DogResponse(String name, BreedResponse breed, DogSex sex, Integer age, String profileImageUrl) {

		public static DogResponse from(AuthorDogResult dog) {
			return new DogResponse(dog.name(), BreedResponse.from(dog.breed()), dog.sex(), dog.age(),
					dog.profileImageUrl());
		}
	}

	public record BreedResponse(String code, String label) {

		public static BreedResponse from(Breed breed) {
			return new BreedResponse(breed.value(), breed.label());
		}
	}
}
