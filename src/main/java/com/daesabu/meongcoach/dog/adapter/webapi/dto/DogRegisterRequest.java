package com.daesabu.meongcoach.dog.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record DogRegisterRequest(
		@NotBlank @Size(max = 50) String name,
		@NotBlank @Size(max = 30) String breed,
		@NotBlank String sex,
		@PastOrPresent LocalDate birthDate,
		Boolean birthDateEstimated,
		@Positive BigDecimal weightKg,
		Set<String> personalities,
		@Size(max = 512) String profileImageUrl,
		@Size(max = 500) String expectation) {

	public DogRegisterInfo toInfo() {
		return new DogRegisterInfo(name, breed, sex, birthDate, birthDateEstimated, weightKg, personalities, profileImageUrl, expectation);
	}
}
