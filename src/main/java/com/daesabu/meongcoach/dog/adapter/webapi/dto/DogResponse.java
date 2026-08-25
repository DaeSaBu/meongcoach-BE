package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 강아지 프로필 응답. 견종·성격은 수정 화면의 선택값(code)과 표시 화면의 이름(label)을 함께 내린다.
 * 나이 미상 강아지는 birthDate·age가 null이며, 미등록 이미지·기대 사항은 빈 문자열이다.
 */
public record DogResponse(Long dogId, String name, BreedResponse breed, DogSex sex, LocalDate birthDate, Integer age,
		BigDecimal weightKg, DogStatus status, String profileImageUrl, String expectation,
		List<PersonalityResponse> personalities) {

	public static DogResponse from(Dog dog) {
		// Set이라 순서가 없으므로 enum 선언 순(온보딩 표시 순)으로 정렬해 안정적으로 내린다
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
			return new BreedResponse(breed.name(), breed.getLabel());
		}
	}

	public record PersonalityResponse(String code, String label) {

		public static PersonalityResponse from(Personality personality) {
			return new PersonalityResponse(personality.name(), personality.getLabel());
		}
	}
}
