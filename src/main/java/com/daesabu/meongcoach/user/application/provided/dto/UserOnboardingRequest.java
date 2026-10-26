package com.daesabu.meongcoach.user.application.provided.dto;

import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record UserOnboardingRequest(
		@Size(max = 50) String nickname,
		@Size(max = 512) String profileImageUrl,
		@Size(max = 4) String mbti,
		@Size(max = 100) List<@NotNull @Positive Long> priorTrainingTopicIds,
		@Size(max = 100) List<@NotNull @Positive Long> trainingGoalTopicIds) {

	public UserProfileCreateCommand toCommand() {
		return new UserProfileCreateCommand(Objects.requireNonNullElse(nickname, ""), profileImageUrl, mbti,
				Set.copyOf(Objects.requireNonNullElse(priorTrainingTopicIds, List.of())),
				Set.copyOf(Objects.requireNonNullElse(trainingGoalTopicIds, List.of())));
	}
}
