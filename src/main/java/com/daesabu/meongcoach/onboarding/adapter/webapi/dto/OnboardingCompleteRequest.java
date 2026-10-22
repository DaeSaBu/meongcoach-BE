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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record OnboardingCompleteRequest(
		@NotBlank @Size(max = 12) String nickname,
		@Size(max = 100) List<@NotNull @Positive Long> priorTrainingTopicIds,
		@Size(max = 100) List<@NotNull @Positive Long> trainingGoalTopicIds,
		@NotEmpty @Size(max = 1) List<@NotNull @Valid DogRequest> dogs) {

	public OnboardingCompleteInfo toInfo() {
		return new OnboardingCompleteInfo(
				nickname,
				null,
				null,
				Set.copyOf(emptyIfNull(priorTrainingTopicIds)),
				Set.copyOf(emptyIfNull(trainingGoalTopicIds)),
				dogs.stream().map(DogRequest::toInfo).toList());
	}

	private static <T> List<T> emptyIfNull(List<T> values) {
		return Objects.requireNonNullElse(values, List.of());
	}

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
