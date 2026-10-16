package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import java.util.List;
import java.util.Set;

public record OnboardingCompleteInfo(String nickname, String mbti, String profileImageUrl,
                                     Set<Long> priorTrainingTopicIds, Set<Long> trainingGoalTopicIds,
                                     List<DogRegisterInfo> dogs) {
}
