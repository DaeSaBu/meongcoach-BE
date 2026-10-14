package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DogResponse(Long dogId, String name, BreedResponse breed, DogSex sex, LocalDate birthDate, Integer age,
		BigDecimal weightKg, DogStatus status, String profileImageUrl, String expectation,
		List<PersonalityResponse> personalities) {

	public static DogResponse from(Dog dog) {
		List<PersonalityResponse> personalities = dog.getPersonalities().stream()
				.sorted()
				.map(PersonalityResponse::from)
				.toList();
		return new DogResponse(dog.getId(), dog.getName(), BreedResponse.from(dog.getBreed()), dog.getSex(),
				dog.getBirthDate(), dog.getAge(), dog.getWeightKg(), dog.getStatus(), dog.getProfileImageUrl(),
				dog.getExpectation(), personalities);
	}

	public record BreedResponse(String code, String label) {

		public static BreedResponse from(Breed breed) {
			return new BreedResponse(breed.value(), breed.label());
		}
	}

	public record PersonalityResponse(String code, String label) {

		public static PersonalityResponse from(Personality personality) {
			return new PersonalityResponse(personality.name(), personality.getLabel());
		}
	}
}
