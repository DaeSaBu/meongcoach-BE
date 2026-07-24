package com.daesabu.meongcoach.dog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DogTest {

	private Dog registerDog() {
		return Dog.register(1L, "초코", "푸들", DogSex.MALE, LocalDate.of(2024, 3, 1),
				new BigDecimal("4.50"), DogSize.SMALL);
	}

	@Test
	void registerCreatesSelectedDog() {
		Dog dog = registerDog();

		assertThat(dog.getUserId()).isEqualTo(1L);
		assertThat(dog.getName()).isEqualTo("초코");
		assertThat(dog.getBreed()).isEqualTo("푸들");
		assertThat(dog.getSex()).isEqualTo(DogSex.MALE);
		assertThat(dog.getBirthDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(dog.getWeightKg()).isEqualByComparingTo("4.50");
		assertThat(dog.getSize()).isEqualTo(DogSize.SMALL);
		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}

	@Test
	void changeProfileImageReplacesImageUrl() {
		Dog dog = registerDog();

		dog.changeProfileImage("https://cdn.meongcoach.com/dog.png");

		assertThat(dog.getProfileImageUrl()).isEqualTo("https://cdn.meongcoach.com/dog.png");
	}

	@Test
	void getAgeCalculatesFromBirthDate() {
		Dog dog = Dog.register(1L, "초코", "푸들", DogSex.MALE, LocalDate.now().minusYears(3),
				new BigDecimal("4.50"), DogSize.SMALL);

		assertThat(dog.getAge()).isEqualTo(3);
	}

	@Test
	void getAgeReturnsNullWhenBirthDateIsNull() {
		Dog dog = Dog.register(1L, "초코", "푸들", DogSex.MALE, null,
				new BigDecimal("4.50"), DogSize.SMALL);

		assertThat(dog.getAge()).isNull();
	}

	@Test
	void unselectChangesStatusToUnselected() {
		Dog dog = registerDog();

		dog.unselect();

		assertThat(dog.getStatus()).isEqualTo(DogStatus.UNSELECTED);
	}

	@Test
	void selectChangesStatusToSelected() {
		Dog dog = registerDog();
		dog.unselect();

		dog.select();

		assertThat(dog.getStatus()).isEqualTo(DogStatus.SELECTED);
	}
}
