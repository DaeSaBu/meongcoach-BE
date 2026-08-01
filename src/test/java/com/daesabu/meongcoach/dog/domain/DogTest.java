package com.daesabu.meongcoach.dog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dog 도메인")
class DogTest {

	private Dog registerDog() {
		return registerDog(LocalDate.of(2024, 3, 1));
	}

	private Dog registerDog(LocalDate birthDate) {
		return Dog.register(new DogRegisterCommand(1L, "초코", Breed.POODLE, DogSex.MALE, birthDate,
				new BigDecimal("4.50")));
	}

	@Test
	@DisplayName("등록하면 SELECTED 상태의 강아지가 생성된다")
	void registerCreatesSelectedDog() {
		Dog dog = registerDog();

		assertThat(dog.getUserId()).isEqualTo(1L);
		assertThat(dog.getName()).isEqualTo("초코");
		assertThat(dog.getBreed()).isEqualTo(Breed.POODLE);
		assertThat(dog.getSex()).isEqualTo(DogSex.MALE);
		assertThat(dog.getBirthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(dog.getWeightKg()).isEqualByComparingTo("4.50");
		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	@DisplayName("등록 직후에는 성격이 비어 있다")
	void registerCreatesDogWithoutPersonalities() {
		Dog dog = registerDog();

		assertThat(dog.getPersonalities()).isEmpty();
	}

	@Test
	@DisplayName("성격을 변경하면 기존 성격이 교체된다")
	void changePersonalitiesReplacesPersonalities() {
		Dog dog = registerDog();
		dog.changePersonalities(Set.of(Personality.TIMID));

		dog.changePersonalities(Set.of(Personality.LIVELY, Personality.FRIENDLY));

		assertThat(dog.getPersonalities())
				.containsExactlyInAnyOrder(Personality.LIVELY, Personality.FRIENDLY);
	}

	@Test
	@DisplayName("프로필 이미지를 변경하면 이미지 URL이 교체된다")
	void changeProfileImageReplacesImageUrl() {
		Dog dog = registerDog();

		dog.changeProfileImage("https://cdn.meongcoach.com/dog.png");

		assertThat(dog.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/dog.png");
	}

	@Test
	@DisplayName("생년월일로 나이를 계산한다")
	void getAgeCalculatesFromBirthDate() {
		Dog dog = registerDog(LocalDate.now().minusYears(3));

		assertThat(dog.getAge()).isEqualTo(3);
	}

	@Test
	@DisplayName("생년월일이 없으면 나이는 null을 반환한다")
	void getAgeReturnsNullWhenBirthDateIsNull() {
		Dog dog = registerDog(null);

		assertThat(dog.getAge()).isNull();
	}

	@Test
	@DisplayName("선택 해제하면 상태가 UNSELECTED로 변경된다")
	void unselectChangesStatusToUnselected() {
		Dog dog = registerDog();

		dog.unselect();

		assertThat(dog.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	@DisplayName("선택하면 상태가 SELECTED로 변경된다")
	void selectChangesStatusToSelected() {
		Dog dog = registerDog();
		dog.unselect();

		dog.select();

		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}
}
