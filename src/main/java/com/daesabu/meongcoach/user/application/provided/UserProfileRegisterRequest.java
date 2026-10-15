package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import java.time.LocalDate;
import java.util.Set;

public record UserProfileRegisterRequest(String nickname, String profileImageUrl, LocalDate birthDate, String mbti,
                                         String gender, Set<Long> priorTrainingTopicIds,
                                         Set<Long> trainingGoalTopicIds) {

	public UserProfileCreateCommand toCommand() {
		return new UserProfileCreateCommand(nickname, profileImageUrl, birthDate, mbti, gender, priorTrainingTopicIds,
				trainingGoalTopicIds);
	}
}
