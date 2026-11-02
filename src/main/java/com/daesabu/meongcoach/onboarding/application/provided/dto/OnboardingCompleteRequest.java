package com.daesabu.meongcoach.onboarding.application.provided.dto;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record OnboardingCompleteRequest(
		@NotEmpty @Size(max = 1) List<@NotNull @Valid DogRequest> dogs) {

	public record DogRequest(
			@NotBlank @Size(max = 12) String name,
			@NotBlank @Size(max = 30) String breed,
			@NotBlank String sex,
			@NotNull @Past LocalDate birthDate,
			Set<String> personalities,
			@Size(max = 512) String profileImageUrl) {

		public DogRegisterInfo toInfo() {
			return new DogRegisterInfo(name, breed, sex, birthDate, null, personalities, profileImageUrl,
					null);
		}
	}
}
