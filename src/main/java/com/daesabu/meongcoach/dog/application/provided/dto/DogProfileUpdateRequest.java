package com.daesabu.meongcoach.dog.application.provided.dto;

import com.daesabu.meongcoach.dog.domain.DogProfileUpdateCommand;
import com.daesabu.meongcoach.dog.domain.DogSex;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record DogProfileUpdateRequest(
		@NotBlank @Size(max = 50) String name,
		@NotBlank @Size(max = 30) String breed,
		@NotBlank String sex,
		@Past LocalDate birthDate,
		@NotNull @Positive BigDecimal weightKg,
		Set<String> personalities,
		@Size(max = 512) String profileImageUrl,
		@Size(max = 500) String expectation) {

	public DogProfileUpdateCommand toCommand() {
		Breed parsedBreed = Breed.from(breed);
		DogSex parsedSex = DogSex.from(sex);
		Set<Personality> parsedPersonalities = Personality.fromCodes(personalities);
		return new DogProfileUpdateCommand(name, parsedBreed, parsedSex, birthDate, weightKg, parsedPersonalities,
				profileImageUrl, expectation);
	}
}
