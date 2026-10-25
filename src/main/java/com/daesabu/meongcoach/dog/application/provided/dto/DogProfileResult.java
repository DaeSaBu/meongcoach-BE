package com.daesabu.meongcoach.dog.application.provided.dto;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DogProfileResult(Long id, String name, Breed breed, LocalDate birthDate, BigDecimal weightKg) {

	public static DogProfileResult from(Dog dog) {
		return new DogProfileResult(dog.getId(), dog.getName(), dog.getBreed(), dog.getBirthDate(), dog.getWeightKg());
	}
}
