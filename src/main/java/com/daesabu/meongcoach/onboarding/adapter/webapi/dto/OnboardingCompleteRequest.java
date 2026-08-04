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
import java.util.Objects;
import java.util.Set;

public record OnboardingCompleteRequest(
		@NotBlank @Size(max = 50) String nickname,
		@Past LocalDate birthDate,
		@NotBlank String mbti,
		@NotBlank String gender,
		@Size(max = 512) String profileImageUrl,
		@Size(max = 100) List<@NotNull @Positive Long> priorTrainingTopicIds,
		@Size(max = 100) List<@NotNull @Positive Long> trainingGoalTopicIds,
		@NotEmpty @Valid List<DogRequest> dogs) {

	public OnboardingCompleteInfo toInfo() {
		return new OnboardingCompleteInfo(
				nickname,
				birthDate,
				mbti,
				gender,
				profileImageUrl,
				Set.copyOf(emptyIfNull(priorTrainingTopicIds)),
				Set.copyOf(emptyIfNull(trainingGoalTopicIds)),
				dogs.stream().map(DogRequest::toInfo).toList());
	}

	private static <T> List<T> emptyIfNull(List<T> values) {
		return Objects.requireNonNullElse(values, List.of());
	}

	public record DogRequest(
			@NotBlank @Size(max = 50) String name,
			@NotBlank @Size(max = 30) String breed,
			@NotBlank String sex,
			@Past LocalDate birthDate,
			@NotNull @Positive BigDecimal weightKg,
			Set<String> personalities,
			@Size(max = 512) String profileImageUrl,
			@Size(max = 500) String expectation) {

		public DogRegisterInfo toInfo() {
			return new DogRegisterInfo(name, breed, sex, birthDate, weightKg, personalities, profileImageUrl,
					expectation);
		}
	}
}
