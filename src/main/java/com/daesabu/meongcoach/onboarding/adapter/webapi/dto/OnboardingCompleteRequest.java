package com.daesabu.meongcoach.onboarding.adapter.webapi.dto;

import com.daesabu.meongcoach.dog.domain.shared.DogRegisterCommand;
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
		// 최대 크기는 dog 모듈의 사용자당 강아지 수 제한(Dogs.MAX_COUNT_PER_USER)과 같은 값이다.
		// 모듈 경계를 넘어 상수를 참조하지 않으므로 값이 바뀌면 함께 고친다
		@NotEmpty @Size(max = 5) @Valid List<DogRequest> dogs) {

	public OnboardingCompleteInfo toInfo() {
		return new OnboardingCompleteInfo(
				nickname,
				birthDate,
				mbti,
				gender,
				profileImageUrl,
				Set.copyOf(emptyIfNull(priorTrainingTopicIds)),
				Set.copyOf(emptyIfNull(trainingGoalTopicIds)),
				dogs.stream().map(DogRequest::toCommand).toList());
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

		// 견종·성별·성격 코드의 enum 변환은 dog 모듈(Dog)이 수행하며, 잘못된 코드는 도메인 예외로 400이 된다
		public DogRegisterCommand toCommand() {
			return new DogRegisterCommand(name, breed, sex, birthDate, weightKg, personalities, profileImageUrl,
					expectation);
		}
	}
}
