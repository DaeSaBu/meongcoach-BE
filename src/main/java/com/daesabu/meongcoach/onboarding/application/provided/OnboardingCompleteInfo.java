package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record OnboardingCompleteInfo(String nickname, LocalDate birthDate, String mbti, String gender,
                                     String profileImageUrl, Set<Long> priorTrainingTopicIds,
                                     Set<Long> trainingGoalTopicIds, List<DogRegisterInfo> dogs) {
}
