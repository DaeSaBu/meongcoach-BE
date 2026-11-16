package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DogFixture {

	public static Dog selected(Long userId) {
		Dog dog = unselected(userId);
		dog.select();
		return dog;
	}

	public static Dog unselected(Long userId) {
		DogRegisterCommand command = new DogRegisterCommand(userId, "보리", Breed.of(RegisteredBreed.POODLE),
				DogSex.MALE, LocalDate.of(2024, 3, 1), new BigDecimal("4.50"), null, null);
		return Dog.register(command);
	}
}
