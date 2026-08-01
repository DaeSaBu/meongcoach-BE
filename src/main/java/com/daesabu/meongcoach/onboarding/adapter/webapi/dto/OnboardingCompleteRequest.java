package com.daesabu.meongcoach.onboarding.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingCompleteInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record OnboardingCompleteRequest(
		@NotBlank @Size(max = 50) String nickname,
		@Past LocalDate birthDate,
		String mbti,
		String gender,
		@Size(max = 512) String profileImageUrl,
		@NotEmpty @Valid List<DogRequest> dogs) {

	public OnboardingCompleteInfo toInfo() {
		return new OnboardingCompleteInfo(nickname, birthDate, mbti, gender, profileImageUrl,
				dogs.stream().map(DogRequest::toInfo).toList());
	}

	public record DogRequest(
			@NotBlank @Size(max = 50) String name,
			@NotBlank @Size(max = 30) String breed,
			@NotBlank String sex,
			@Past LocalDate birthDate,
			@NotNull @Positive BigDecimal weightKg,
			Set<String> personalities,
			@Size(max = 512) String profileImageUrl) {

		public DogRegisterInfo toInfo() {
			return new DogRegisterInfo(name, breed, sex, birthDate, weightKg, personalities, profileImageUrl, null);
		}
	}
}
