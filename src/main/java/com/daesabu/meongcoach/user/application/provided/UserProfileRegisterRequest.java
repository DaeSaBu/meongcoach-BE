package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import java.util.Set;

public record UserProfileRegisterRequest(String nickname, String profileImageUrl, String mbti,
                                         Set<Long> priorTrainingTopicIds, Set<Long> trainingGoalTopicIds) {

	public UserProfileCreateCommand toCommand() {
		return new UserProfileCreateCommand(nickname, profileImageUrl, mbti, priorTrainingTopicIds,
				trainingGoalTopicIds);
	}
}
