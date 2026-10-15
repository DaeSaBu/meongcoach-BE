package com.daesabu.meongcoach.user.domain;

import java.time.LocalDate;
import java.util.Set;

public record UserProfileCreateCommand(String nickname, String profileImageUrl, LocalDate birthDate, String mbti,
                                       String gender, Set<Long> priorTrainingTopicIds,
                                       Set<Long> trainingGoalTopicIds) {
}
