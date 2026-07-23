package com.daesabu.meongcoach.dog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DogPersonalityTest {

	@Test
	void assignConnectsDogAndPersonality() {
		Dog dog = Dog.register(1L, "초코", "푸들", DogSex.FEMALE, LocalDate.of(2024, 3, 1),
				new BigDecimal("4.50"), DogSize.SMALL);
		Personality personality = Personality.create("활발함");

		DogPersonality dogPersonality = DogPersonality.assign(dog, personality);

		assertThat(dogPersonality.getDog()).isEqualTo(dog);
		assertThat(dogPersonality.getPersonality()).isEqualTo(personality);
		assertThat(personality.getName()).isEqualTo("활발함");
	}
}
