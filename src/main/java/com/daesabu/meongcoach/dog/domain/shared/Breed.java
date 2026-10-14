package com.daesabu.meongcoach.dog.domain.shared;

import com.daesabu.meongcoach.dog.domain.exception.BreedLengthExceededException;
import com.daesabu.meongcoach.dog.domain.exception.BreedRequiredException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;

@Embeddable
public record Breed(@Column(name = "breed", nullable = false, length = Breed.MAX_LENGTH) String value) {

	public static final int MAX_LENGTH = 30;

	public Breed {
		if (value == null || value.isBlank()) {
			throw new BreedRequiredException();
		}
		value = value.strip();
		if (value.length() > MAX_LENGTH) {
			throw new BreedLengthExceededException();
		}
		value = RegisteredBreed.findByLabel(value)
				.map(RegisteredBreed::name)
				.orElse(value);
	}

	public static Breed of(RegisteredBreed registered) {
		return new Breed(registered.name());
	}

	public static Breed from(String value) {
		return new Breed(value);
	}

	public Optional<RegisteredBreed> registered() {
		return RegisteredBreed.find(value);
	}

	public boolean isCustom() {
		return registered().isEmpty();
	}

	public String label() {
		return registered()
				.map(RegisteredBreed::getLabel)
				.orElse(value);
	}
}
