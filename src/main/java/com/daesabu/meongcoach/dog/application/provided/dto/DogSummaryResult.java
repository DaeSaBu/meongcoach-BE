package com.daesabu.meongcoach.dog.application.provided.dto;

import com.daesabu.meongcoach.dog.domain.Dog;

public record DogSummaryResult(Long id, String name) {

	public static DogSummaryResult from(Dog dog) {
		return new DogSummaryResult(dog.getId(), dog.getName());
	}
}
