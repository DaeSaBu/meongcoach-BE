package com.daesabu.meongcoach.user.domain;

import java.util.Set;

public record UserProfileCreateCommand(String nickname, String profileImageUrl, String mbti,
                                       Set<Long> priorTrainingTopicIds, Set<Long> trainingGoalTopicIds) {
}
